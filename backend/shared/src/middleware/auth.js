const { verifyAccessToken } = require('../auth/jwt');
const { sendError } = require('../utils/response');

/**
 * Express middleware to authenticate Bearer Access Token.
 */
function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  if (!authHeader) {
    return sendError(res, 'Authentication token required', 401);
  }

  const parts = authHeader.split(' ');
  if (parts.length !== 2 || parts[0] !== 'Bearer') {
    return sendError(res, 'Invalid authorization header format. Expected Bearer <token>', 401);
  }

  const token = parts[1];
  try {
    const decoded = verifyAccessToken(token);
    req.user = decoded;
    next();
  } catch (err) {
    if (err.name === 'TokenExpiredError') {
      return sendError(res, 'Access token has expired', 401, { code: 'TOKEN_EXPIRED' });
    }
    return sendError(res, 'Invalid or malformed access token', 403, { code: 'INVALID_TOKEN' });
  }
}

/**
 * Role-based authorization middleware.
 * @param  {...string} allowedRoles 
 */
function requireRole(...allowedRoles) {
  return (req, res, next) => {
    if (!req.user || !req.user.roles) {
      return sendError(res, 'Forbidden: insufficient role privileges', 403);
    }
    const hasRole = req.user.roles.some((r) => allowedRoles.includes(r));
    if (!hasRole) {
      return sendError(res, 'Forbidden: required role missing', 403);
    }
    next();
  };
}

module.exports = {
  authenticateToken,
  requireRole
};
