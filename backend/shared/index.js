const config = require('./src/config');
const { connectMongo, mongoose } = require('./src/database/mongo');
const { getRedisClient, Redis } = require('./src/database/redis');
const {
  hashToken,
  signAccessToken,
  signRefreshToken,
  verifyAccessToken,
  verifyRefreshToken
} = require('./src/auth/jwt');
const { authenticateToken, requireRole } = require('./src/middleware/auth');
const { errorHandler } = require('./src/middleware/error');
const { sendSuccess, sendError } = require('./src/utils/response');
const { isValidE164, isValidOTP } = require('./src/utils/validator');

module.exports = {
  config,
  connectMongo,
  mongoose,
  getRedisClient,
  Redis,
  hashToken,
  signAccessToken,
  signRefreshToken,
  verifyAccessToken,
  verifyRefreshToken,
  authenticateToken,
  requireRole,
  errorHandler,
  sendSuccess,
  sendError,
  isValidE164,
  isValidOTP
};
