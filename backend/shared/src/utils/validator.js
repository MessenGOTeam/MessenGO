/**
 * International E.164 phone number standard validator.
 * Must start with '+' followed by country code and subscriber number (max 15 digits total).
 * Example: +84912345678
 * @param {string} phone 
 * @returns {boolean}
 */
function isValidE164(phone) {
  if (typeof phone !== 'string') return false;
  const e164Regex = /^\+[1-9]\d{1,14}$/;
  return e164Regex.test(phone.trim());
}

/**
 * Validate 6-digit OTP code.
 * @param {string} otp 
 * @returns {boolean}
 */
function isValidOTP(otp) {
  if (typeof otp !== 'string') return false;
  return /^\d{6}$/.test(otp.trim());
}

module.exports = {
  isValidE164,
  isValidOTP
};
