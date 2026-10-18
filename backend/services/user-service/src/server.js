const app = require('./app');
const { config, connectMongo } = require('../../shared');

const PORT = config.ports.user || 3002;
const DB_NAME = 'messengo_identity';

async function startServer() {
  await connectMongo(DB_NAME);

  app.listen(PORT, () => {
    console.log(`[User Service] Listening on port ${PORT}`);
  });
}

startServer();
