const { sendError } = require('../utils/response');

/**
 * Global Express error handling middleware.
 */
function errorHandler(err, req, res, next) {
  console.error('[Error caught in middleware]:', err.stack || err);
  const status = err.statusCode || err.status || 500;
  const message = err.message || 'Internal Server Error';
  return sendError(res, message, status, err.details || null);
}

module.exports = {
  errorHandler
};
