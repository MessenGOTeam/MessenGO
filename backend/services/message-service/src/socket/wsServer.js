const { WebSocketServer } = require('ws');
const url = require('url');
const { verifyAccessToken } = require('../../../../shared');
const socketManager = require('./socketManager');
const messageHandler = require('./handlers/messageHandler');

/**
 * Initialize WebSocket Server for MessenGO Real-time messaging (/ws/messages).
 * @param {import('http').Server} httpServer
 */
function createWebSocketServer(httpServer) {
  const wss = new WebSocketServer({ noServer: true });

  httpServer.on('upgrade', (request, socket, head) => {
    let parsedUrl;
    try {
      parsedUrl = new URL(request.url, `http://${request.headers.host || 'localhost'}`);
    } catch {
      socket.write('HTTP/1.1 400 Bad Request\r\n\r\n');
      socket.destroy();
      return;
    }

    const pathname = parsedUrl.pathname;

    // Support both /ws/messages and / or /ws
    if (!pathname.startsWith('/ws/messages') && pathname !== '/' && pathname !== '/ws') {
      socket.write('HTTP/1.1 404 Not Found\r\n\r\n');
      socket.destroy();
      return;
    }

    // 1. Extract JWT token from Query Param, Authorization header, or sec-websocket-protocol
    let token = parsedUrl.searchParams.get('token');

    if (!token && request.headers.authorization) {
      const parts = request.headers.authorization.split(' ');
      if (parts.length === 2 && parts[0].toLowerCase() === 'bearer') {
        token = parts[1];
      }
    }

    if (!token && request.headers['sec-websocket-protocol']) {
      // Sometimes clients pass token in protocol header
      const protocols = request.headers['sec-websocket-protocol'].split(',').map((p) => p.trim());
      if (protocols.length > 0 && protocols[0].length > 20) {
        token = protocols[0];
      }
    }

    if (!token) {
      socket.write('HTTP/1.1 401 Unauthorized\r\n\r\n');
      socket.destroy();
      return;
    }

    // 2. Verify JWT Access Token
    let decoded;
    try {
      decoded = verifyAccessToken(token);
    } catch (err) {
      socket.write('HTTP/1.1 401 Unauthorized\r\n\r\n');
      socket.destroy();
      return;
    }

    if (!decoded || !decoded.userId) {
      socket.write('HTTP/1.1 401 Unauthorized\r\n\r\n');
      socket.destroy();
      return;
    }

    // Handshake successful -> complete WebSocket upgrade
    wss.handleUpgrade(request, socket, head, (ws) => {
      ws.userId = decoded.userId;
      ws.phone = decoded.phone;
      ws.roles = decoded.roles || ['user'];
      wss.emit('connection', ws, request);
    });
  });

  wss.on('connection', (ws) => {
    ws.isAlive = true;
    socketManager.addSocket(ws.userId, ws);

    // Heartbeat pong
    ws.on('pong', () => {
      ws.isAlive = true;
    });

    // Send connection established welcome event
    ws.send(
      JSON.stringify({
        event: 'connected',
        data: {
          userId: ws.userId,
          message: 'Kết nối WebSocket MessenGO thành công',
          timestamp: Date.now()
        }
      })
    );

    // Handle incoming messages
    ws.on('message', async (rawData) => {
      await messageHandler.handleEvent(ws, rawData);
    });

    // Handle close and errors
    ws.on('close', () => {
      socketManager.removeSocket(ws.userId, ws);
    });

    ws.on('error', (err) => {
      console.warn(`[WSS] Socket error for user ${ws.userId}:`, err.message);
      socketManager.removeSocket(ws.userId, ws);
    });
  });

  // Heartbeat interval to cleanup dead connections (every 30s)
  const pingInterval = setInterval(() => {
    for (const ws of wss.clients) {
      if (ws.isAlive === false) {
        socketManager.removeSocket(ws.userId, ws);
        return ws.terminate();
      }
      ws.isAlive = false;
      ws.ping();
    }
  }, 30000);

  wss.on('close', () => {
    clearInterval(pingInterval);
  });

  return wss;
}

module.exports = { createWebSocketServer };
