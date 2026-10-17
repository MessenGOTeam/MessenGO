const axios = require('axios');
const otpService = require('../services/otp.service');
const tokenService = require('../services/token.service');
const { sendSuccess, sendError, isValidE164, isValidOTP, config } = require('../../../../shared');

const USER_SERVICE_URL = process.env.USER_SERVICE_INTERNAL_URL || `http://localhost:${config.ports.user}`;

class AuthController {
  /**
   * POST /api/v1/auth/otp/request
   * FR-AUTH-01 & FR-AUTH-02
   */
  async requestOtp(req, res, next) {
    try {
      const { phone, email } = req.body;
      if (!phone || !isValidE164(phone)) {
        return sendError(
          res,
          'Invalid phone number format. Must be E.164 standard (e.g. +84912345678)',
          400,
          { code: 'INVALID_PHONE_FORMAT' }
        );
      }

      if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
        return sendError(res, 'Invalid email address format', 400, { code: 'INVALID_EMAIL_FORMAT' });
      }

      const result = await otpService.requestOtp(phone, email ? email.trim() : null);
      return sendSuccess(res, result, email ? `OTP sent successfully to ${email}` : 'OTP sent successfully');
    } catch (error) {
      next(error);
    }
  }

  /**
   * POST /api/v1/auth/otp/verify
   * FR-AUTH-03 & FR-AUTH-04 (Seq1 / Diagram 1)
   */
  async verifyOtp(req, res, next) {
    try {
      const { phone, otp, deviceId, deviceName, fcmToken, appVersion } = req.body;

      if (!phone || !isValidE164(phone)) {
        return sendError(res, 'Valid E.164 phone number required', 400);
      }
      if (!otp || !isValidOTP(otp)) {
        return sendError(res, 'A 6-digit OTP code is required', 400);
      }
      if (!deviceId) {
        return sendError(res, 'deviceId is required to bind session', 400);
      }

      // 1. Verify OTP with brute force counter
      await otpService.verifyOtp(phone, otp);

      // 2. Synchronize user & device profile with User Service
      let userData = null;
      try {
        const userSvcResponse = await axios.post(`${USER_SERVICE_URL}/internal/upsert`, {
          phone,
          deviceId,
          deviceName,
          fcmToken,
          appVersion
        });
        userData = userSvcResponse.data.data;
      } catch (err) {
        console.warn('[Auth Controller] Could not contact User Service directly via HTTP:', err.message);
        // Fallback: create mock ID if running standalone without user-service
        userData = {
          user: { id: `usr_${Date.now()}`, phone, displayName: `User ${phone.slice(-4)}` }
        };
      }

      const userId = userData.user.id;

      // 3. Issue JWT Access & Refresh tokens (FR-AUTH-03)
      const tokens = await tokenService.issueTokens(userId, phone, deviceId);

      return sendSuccess(
        res,
        {
          ...tokens,
          user: userData.user,
          device: userData.device
        },
        'Authentication successful'
      );
    } catch (error) {
      next(error);
    }
  }

  /**
   * POST /api/v1/auth/token/refresh
   * FR-AUTH-07
   */
  async refreshToken(req, res, next) {
    try {
      const { refreshToken, deviceId } = req.body;
      if (!refreshToken) {
        return sendError(res, 'refreshToken is required', 400);
      }

      const result = await tokenService.refreshAccessToken(refreshToken, deviceId);
      return sendSuccess(res, result, 'Access token refreshed successfully');
    } catch (error) {
      next(error);
    }
  }

  /**
   * POST /api/v1/auth/logout
   * FR-AUTH-06
   */
  async logout(req, res, next) {
    try {
      const { refreshToken } = req.body;
      await tokenService.revokeToken(refreshToken);
      return sendSuccess(res, null, 'Logged out successfully');
    } catch (error) {
      next(error);
    }
  }
}

module.exports = new AuthController();
