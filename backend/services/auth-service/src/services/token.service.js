const crypto = require('crypto');
const RefreshToken = require('../models/RefreshToken');
const RoleAssignment = require('../models/RoleAssignment');
const {
  signAccessToken,
  signRefreshToken,
  verifyRefreshToken,
  hashToken
} = require('../../../../shared');

class TokenService {
  /**
   * Get roles assigned to a user (defaults to ['user']).
   */
  async getUserRoles(userId) {
    const assignments = await RoleAssignment.find({ userId });
    if (!assignments || assignments.length === 0) {
      return ['user'];
    }
    return assignments.map((a) => a.role);
  }

  /**
   * Issue new Access Token and Refresh Token pair.
   */
  async issueTokens(userId, phone, deviceId) {
    const roles = await this.getUserRoles(userId);

    const accessToken = signAccessToken({
      userId,
      phone,
      roles
    });

    // Generate random opaque refresh token string and hash it
    const rawRefreshToken = signRefreshToken({ userId, deviceId });
    const tokenHash = hashToken(rawRefreshToken);

    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + 30); // 30 days per SRS FR-AUTH-03

    // Store in AuthDB
    await RefreshToken.create({
      tokenHash,
      userId,
      deviceId,
      expiresAt,
      isRevoked: false
    });

    return {
      accessToken,
      refreshToken: rawRefreshToken,
      expiresIn: 900 // 15 minutes in seconds
    };
  }

  /**
   * Refresh access token using a valid Refresh Token (FR-AUTH-07).
   */
  async refreshAccessToken(rawRefreshToken, currentDeviceId) {
    let decoded;
    try {
      decoded = verifyRefreshToken(rawRefreshToken);
    } catch (e) {
      const err = new Error('Invalid or expired refresh token');
      err.statusCode = 401;
      throw err;
    }

    const tokenHash = hashToken(rawRefreshToken);
    const tokenDoc = await RefreshToken.findOne({ tokenHash });

    if (!tokenDoc) {
      const err = new Error('Refresh token not found or revoked');
      err.statusCode = 401;
      throw err;
    }

    if (tokenDoc.isRevoked) {
      const err = new Error('Refresh token has been revoked');
      err.statusCode = 401;
      throw err;
    }

    if (new Date() > tokenDoc.expiresAt) {
      const err = new Error('Refresh token has expired');
      err.statusCode = 401;
      throw err;
    }

    if (currentDeviceId && tokenDoc.deviceId !== currentDeviceId) {
      const err = new Error('Token does not match current device');
      err.statusCode = 403;
      throw err;
    }

    const roles = await this.getUserRoles(tokenDoc.userId);
    const newAccessToken = signAccessToken({
      userId: tokenDoc.userId,
      roles
    });

    return {
      accessToken: newAccessToken,
      expiresIn: 900
    };
  }

  /**
   * Revoke token on logout (FR-AUTH-06).
   */
  async revokeToken(rawRefreshToken) {
    if (!rawRefreshToken) return false;
    const tokenHash = hashToken(rawRefreshToken);
    await RefreshToken.updateOne({ tokenHash }, { isRevoked: true });
    return true;
  }
}

module.exports = new TokenService();
