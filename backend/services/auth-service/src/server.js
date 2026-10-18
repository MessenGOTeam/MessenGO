const app = require('./app');
const { config, connectMongo } = require('../../shared');

const PORT = config.ports.auth || 3001;
const DB_NAME = 'messengo_auth';

async function startServer() {
  await connectMongo(DB_NAME);

  app.listen(PORT, () => {
    console.log(`[Auth Service] Listening on port ${PORT}`);
  });
}

startServer();
