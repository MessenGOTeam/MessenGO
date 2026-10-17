const Redis = require('ioredis');
const config = require('../config');

let redisClient = null;

/**
 * Get or initialize singleton Redis client.
 * @returns {Redis}
 */
function getRedisClient() {
  if (!redisClient) {
    redisClient = new Redis({
      host: config.redis.host,
      port: config.redis.port,
      lazyConnect: true,
      maxRetriesPerRequest: 1,
      enableOfflineQueue: false,
      retryStrategy(times) {
        if (times > 2) return null; // Stop retrying if host unreachable
        return 500;
      }
    });

    redisClient.on('connect', () => {
      console.log(`[Redis] Connected to ${config.redis.host}:${config.redis.port}`);
    });

    redisClient.on('error', (err) => {
      console.warn(`[Redis] Connection warning: ${err.message}`);
    });
  }

  return redisClient;
}

module.exports = {
  getRedisClient,
  Redis
};
