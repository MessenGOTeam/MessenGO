const { getRedisClient, config } = require('../../../../shared');
const emailService = require('./email.service');

// In-memory fallback map if Redis is temporarily offline during unit testing/local dev
const memoryStore = new Map();

class OtpService {
  constructor() {
    this.otpTtlSeconds = config.otp.expirySeconds || 300; // 5 mins
    this.lockTtlSeconds = 900; // 15 mins
    this.maxAttempts = 3;
  }

  async _getRedis() {
    try {
      const client = getRedisClient();
      if (client.status === 'ready' || client.status === 'connect') {
        return client;
      }
      await client.connect().catch(() => {});
      if (client.status === 'ready') return client;
    } catch (e) {
      // Redis offline
    }
    return null;
  }

  /**
   * Request OTP generation for phone number.
   * Checks brute force lockout first.
   */
  async requestOtp(phone, email = null) {
    const redis = await this._getRedis();
    const tryKey = `otp_try:${phone}`;
    const otpKey = `otp:${phone}`;

    // 1. Check brute force attempts
    let attempts = 0;
    if (redis) {
      const val = await redis.get(tryKey);
      attempts = val ? parseInt(val, 10) : 0;
    } else {
      const val = memoryStore.get(tryKey);
      if (val && val.expiresAt > Date.now()) {
        attempts = val.count;
      }
    }

    if (attempts >= this.maxAttempts) {
      const err = new Error('You entered the wrong code 3 times. Please try again in 15 minutes');
      err.statusCode = 429;
      err.code = 'TOO_MANY_ATTEMPTS';
      throw err;
    }

    // 2. Generate random 6-digit OTP (e.g. 889922)
    const code = Math.floor(100000 + Math.random() * 900000).toString();

    // 3. Save OTP in Redis with TTL
    if (redis) {
      await redis.set(otpKey, code, 'EX', this.otpTtlSeconds);
    } else {
      memoryStore.set(otpKey, {
        code,
        expiresAt: Date.now() + this.otpTtlSeconds * 1000
      });
    }

    console.log(`[OTP Service] Generated OTP for ${phone}: [${code}] (TTL: ${this.otpTtlSeconds}s)`);

    // 4. Dispatch Real OTP Email (Option A)
    let emailResult = null;
    if (email) {
      emailResult = await emailService.sendOtpEmail(email, code, phone);
    }

    return {
      phone,
      email: email || undefined,
      emailDispatched: emailResult ? emailResult.sent : false,
      expiresInSeconds: this.otpTtlSeconds,
      // For dev/demo convenience (per SRS section 2.7 / 9.1 open issue 6)
      mockOtp: config.otp.mockEnabled ? code : undefined
    };
  }

  /**
   * Verify entered OTP.
   * Handles attempt counting and invalidation.
   */
  async verifyOtp(phone, inputOtp) {
    const redis = await this._getRedis();
    const tryKey = `otp_try:${phone}`;
    const otpKey = `otp:${phone}`;

    // 1. Check if locked out
    let attempts = 0;
    if (redis) {
      const val = await redis.get(tryKey);
      attempts = val ? parseInt(val, 10) : 0;
    } else {
      const val = memoryStore.get(tryKey);
      if (val && val.expiresAt > Date.now()) {
        attempts = val.count;
      }
    }

    if (attempts >= this.maxAttempts) {
      const err = new Error('You entered the wrong code 3 times. Please try again in 15 minutes');
      err.statusCode = 429;
      err.code = 'TOO_MANY_ATTEMPTS';
      throw err;
    }

    // 2. Fetch stored OTP
    let storedOtp = null;
    if (redis) {
      storedOtp = await redis.get(otpKey);
    } else {
      const item = memoryStore.get(otpKey);
      if (item && item.expiresAt > Date.now()) {
        storedOtp = item.code;
      }
    }

    // 3. Handle expired or missing OTP
    if (!storedOtp) {
      const err = new Error('The OTP has expired');
      err.statusCode = 400;
      err.code = 'OTP_EXPIRED';
      throw err;
    }

    // 4. Validate matching OTP
    if (storedOtp !== inputOtp.trim()) {
      // Increment failed attempts
      if (redis) {
        const newCount = await redis.incr(tryKey);
        if (newCount === 1) {
          await redis.expire(tryKey, this.lockTtlSeconds);
        }
        attempts = newCount;
      } else {
        attempts += 1;
        memoryStore.set(tryKey, {
          count: attempts,
          expiresAt: Date.now() + this.lockTtlSeconds * 1000
        });
      }

      if (attempts >= this.maxAttempts) {
        const err = new Error('You entered the wrong code 3 times. Please try again in 15 minutes');
        err.statusCode = 429;
        err.code = 'TOO_MANY_ATTEMPTS';
        throw err;
      }

      const err = new Error('Invalid OTP code');
      err.statusCode = 400;
      err.code = 'INVALID_OTP';
      throw err;
    }

    // 5. Successful verification: clean up keys (single-use OTP)
    if (redis) {
      await redis.del(otpKey);
      await redis.del(tryKey);
    } else {
      memoryStore.delete(otpKey);
      memoryStore.delete(tryKey);
    }

    return true;
  }
}

module.exports = new OtpService();
