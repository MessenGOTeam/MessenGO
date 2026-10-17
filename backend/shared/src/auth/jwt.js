const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const config = require('../config');

/**
 * Generate SHA-256 hash for secure refresh token storage (per NFR-04).
 * @param {string} token 
 * @returns {string} hex digest
 */
function hashToken(token) {
  return crypto.createHash('sha256').update(token).digest('hex');
}

/**
 * Sign 15-minute JWT Access Token containing userId, phone, and roles.
 * @param {object} payload - { userId, phone, roles }
 * @returns {string}
 */
function signAccessToken(payload) {
  return jwt.sign(payload, config.jwt.accessSecret, {
    expiresIn: config.jwt.accessExpiry
  });
}

/**
 * Sign 30-day Refresh Token containing userId and deviceId.
 * @param {object} payload - { userId, deviceId }
 * @returns {string}
 */
function signRefreshToken(payload) {
  return jwt.sign(payload, config.jwt.refreshSecret, {
    expiresIn: config.jwt.refreshExpiry
  });
}

/**
 * Verify JWT Access Token.
 * @param {string} token 
 * @returns {object} decoded payload
 */
function verifyAccessToken(token) {
  return jwt.verify(token, config.jwt.accessSecret);
}

/**
 * Verify JWT Refresh Token.
 * @param {string} token 
 * @returns {object} decoded payload
 */
function verifyRefreshToken(token) {
  return jwt.verify(token, config.jwt.refreshSecret);
}

module.exports = {
  hashToken,
  signAccessToken,
  signRefreshToken,
  verifyAccessToken,
  verifyRefreshToken
};
