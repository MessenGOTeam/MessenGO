const WebSocket = require('ws');

class SocketManager {
  constructor() {
    // Map<userId, Set<WebSocket>>
    this.activeSockets = new Map();
  }

  /**
   * Register a new socket connection for a user.
   */
  addSocket(userId, ws) {
    if (!userId || !ws) return;
    if (!this.activeSockets.has(userId)) {
      this.activeSockets.set(userId, new Set());
    }
    this.activeSockets.get(userId).add(ws);
    // console.log(`[SocketManager] User ${userId} connected (Total sockets for user: ${this.activeSockets.get(userId).size})`);
  }

  /**
   * Remove a socket connection on disconnect.
   */
  removeSocket(userId, ws) {
    if (!userId || !ws) return;
    const userSockets = this.activeSockets.get(userId);
    if (userSockets) {
      userSockets.delete(ws);
      if (userSockets.size === 0) {
        this.activeSockets.delete(userId);
      }
    }
    // console.log(`[SocketManager] User ${userId} disconnected`);
  }

  /**
   * Check if user currently has at least one active WebSocket connection.
   */
  isUserOnline(userId) {
    const sockets = this.activeSockets.get(userId);
    return Boolean(sockets && sockets.size > 0);
  }

  /**
   * Push an event with data to all active sockets of a target user.
   * @param {string} userId - Recipient user ID
   * @param {string} event - Event name (e.g. 'msg.receive', 'msg.status_update')
   * @param {object} data - Payload
   * @returns {number} Number of active sockets the event was delivered to
   */
  sendToUser(userId, event, data) {
    const sockets = this.activeSockets.get(userId);
    if (!sockets || sockets.size === 0) {
      return 0;
    }

    const payload = JSON.stringify({ event, data });
    let deliveredCount = 0;

    for (const ws of sockets) {
      if (ws.readyState === WebSocket.OPEN) {
        try {
          ws.send(payload);
          deliveredCount++;
        } catch (err) {
          console.warn(`[SocketManager] Failed to send to socket of user ${userId}:`, err.message);
        }
      }
    }

    return deliveredCount;
  }

  /**
   * Send to multiple users (e.g. conversation members)
   */
  sendToUsers(userIds, event, data, excludeUserId = null) {
    let count = 0;
    for (const uid of userIds) {
      if (excludeUserId && uid === excludeUserId) continue;
      count += this.sendToUser(uid, event, data);
    }
    return count;
  }

  getOnlineUserCount() {
    return this.activeSockets.size;
  }
}

module.exports = new SocketManager();

