const http = require('http');
const app = require('./app');
const { createWebSocketServer } = require('./socket/wsServer');
const redisConsumer = require('./streams/redisConsumer');
const { config, connectMongo } = require('../../../shared');

const PORT = config.ports.message || 3004;
const DB_NAME = process.env.MESSAGE_DB_NAME || 'messengo_identity';

async function startServer() {
  // Connect to MongoDB
  await connectMongo(DB_NAME);

  const server = http.createServer(app);

  // Initialize WebSocket Server on /ws/messages
  const wss = createWebSocketServer(server);

  // Start Redis Streams Consumer Group
  await redisConsumer.start();

  server.listen(PORT, () => {
    console.log(`[Message Service] Real-time WebSocket Server running on port ${PORT}`);
    console.log(`[Message Service] WebSocket endpoint: ws://localhost:${PORT}/ws/messages`);
  });

  return { server, wss, redisConsumer };
}

if (require.main === module) {
  startServer();
}

module.exports = { startServer };
