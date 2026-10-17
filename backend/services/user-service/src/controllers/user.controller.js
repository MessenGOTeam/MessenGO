const User = require('../models/User');
const Device = require('../models/Device');
const { sendSuccess, sendError, isValidE164 } = require('../../../../shared');

/**
 * Internal upsert called by Auth Service after successful OTP verification.
 * Creates user if not existing, and registers/updates device info.
 */
async function upsertUserInternal(req, res, next) {
  try {
    const { phone, deviceId, deviceName, fcmToken, appVersion } = req.body;

    if (!phone || !isValidE164(phone)) {
      return sendError(res, 'Valid E.164 phone number required', 400);
    }
    if (!deviceId) {
      return sendError(res, 'deviceId is required', 400);
    }

    let user = await User.findOne({ phone });
    let isNewUser = false;

    if (!user) {
      user = await User.create({
        phone,
        displayName: `User ${phone.slice(-4)}`,
        accountStatus: 'active'
      });
      isNewUser = true;
    } else if (user.accountStatus === 'locked') {
      return sendError(res, 'Account is locked. Contact administrator.', 403, { code: 'ACCOUNT_LOCKED' });
    }

    // Upsert Device info (FR-AUTH-04)
    const device = await Device.findOneAndUpdate(
      { userId: user._id, deviceId },
      {
        deviceName: deviceName || 'Android Device',
        fcmToken: fcmToken || null,
        appVersion: appVersion || '1.0.0',
        lastActiveAt: new Date()
      },
      { new: true, upsert: true }
    );

    return sendSuccess(
      res,
      {
        user: user.toJSON(),
        device: device.toJSON(),
        isNewUser
      },
      'User synchronized successfully',
      isNewUser ? 201 : 200
    );
  } catch (error) {
    next(error);
  }
}

/**
 * Get profile of the current authenticated user.
 */
async function getMe(req, res, next) {
  try {
    const user = await User.findById(req.user.userId);
    if (!user) {
      return sendError(res, 'User not found', 404);
    }
    return sendSuccess(res, { user: user.toJSON() }, 'Profile fetched successfully');
  } catch (error) {
    next(error);
  }
}

/**
 * Update current user profile (FR-AUTH-05).
 */
async function updateProfile(req, res, next) {
  try {
    const { displayName, statusText, locale, avatarMediaId } = req.body;
    const updates = {};

    if (displayName !== undefined) updates.displayName = displayName.trim();
    if (statusText !== undefined) updates.statusText = statusText.trim();
    if (locale !== undefined) {
      if (!['vi', 'en'].includes(locale)) {
        return sendError(res, "Locale must be 'vi' or 'en'", 400);
      }
      updates.locale = locale;
    }
    if (avatarMediaId !== undefined) updates.avatarMediaId = avatarMediaId;

    const user = await User.findByIdAndUpdate(req.user.userId, updates, {
      new: true,
      runValidators: true
    });

    if (!user) {
      return sendError(res, 'User not found', 404);
    }

    return sendSuccess(res, { user: user.toJSON() }, 'Profile updated successfully');
  } catch (error) {
    next(error);
  }
}

/**
 * Search user by exact phone number (FR-CON-01).
 */
async function searchByPhone(req, res, next) {
  try {
    const { phone } = req.query;
    if (!phone || !isValidE164(phone)) {
      return sendError(res, 'A valid E.164 phone number query is required', 400);
    }

    const user = await User.findOne({ phone, accountStatus: 'active' }).select(
      'displayName phone avatarMediaId statusText'
    );

    if (!user) {
      return sendError(res, 'User not found', 404, { code: 'USER_NOT_FOUND' });
    }

    return sendSuccess(res, { user: user.toJSON() }, 'User found');
  } catch (error) {
    next(error);
  }
}

/**
 * Update device info or FCM token (FR-AUTH-04).
 */
async function updateDevice(req, res, next) {
  try {
    const { deviceId, deviceName, fcmToken, appVersion } = req.body;
    if (!deviceId) {
      return sendError(res, 'deviceId is required', 400);
    }

    const device = await Device.findOneAndUpdate(
      { userId: req.user.userId, deviceId },
      {
        deviceName: deviceName || 'Android Device',
        fcmToken: fcmToken || null,
        appVersion: appVersion || '1.0.0',
        lastActiveAt: new Date()
      },
      { new: true, upsert: true }
    );

    return sendSuccess(res, { device: device.toJSON() }, 'Device updated successfully');
  } catch (error) {
    next(error);
  }
}

/**
 * Lock/unlock account (FR-ADM-01 - Admin only).
 */
async function setAccountStatus(req, res, next) {
  try {
    const { userId } = req.params;
    const { status } = req.body;

    if (!['active', 'locked'].includes(status)) {
      return sendError(res, "Status must be 'active' or 'locked'", 400);
    }

    const user = await User.findByIdAndUpdate(userId, { accountStatus: status }, { new: true });
    if (!user) {
      return sendError(res, 'User not found', 404);
    }

    return sendSuccess(res, { user: user.toJSON() }, `Account status updated to ${status}`);
  } catch (error) {
    next(error);
  }
}

module.exports = {
  upsertUserInternal,
  getMe,
  updateProfile,
  searchByPhone,
  updateDevice,
  setAccountStatus
};
