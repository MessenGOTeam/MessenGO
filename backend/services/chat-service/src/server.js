const app = require('./app');
const { config, connectMongo } = require('../../../shared');

const PORT = config.ports.chat || 3003;
const DB_NAME = process.env.CHAT_DB_NAME || 'messengo_identity';

async function startServer() {
  await connectMongo(DB_NAME);

  app.listen(PORT, () => {
    console.log(`MessenGO Chat Service is running on port ${PORT}`);
  });
}

// Only start when run directly
if (require.main === module) {
  startServer();
}

module.exports = { startServer };
