const assert = require('assert');
const { isValidE164, isValidOTP, signAccessToken, verifyAccessToken, hashToken, config } = require('../shared');
const otpService = require('../services/auth-service/src/services/otp.service');
const tokenService = require('../services/auth-service/src/services/token.service');

async function runSprint1Tests() {
  console.log('--- Starting Sprint 1 Member A Automated Tests ---\n');

  // Test 1: Phone validator
  console.log('1. Testing E.164 Phone Validation:');
  assert.strictEqual(isValidE164('+84912345678'), true, 'Valid VN phone should pass');
  assert.strictEqual(isValidE164('+14155552671'), true, 'Valid US phone should pass');
  assert.strictEqual(isValidE164('0912345678'), false, 'Non-E.164 without plus should fail');
  assert.strictEqual(isValidE164('abcd'), false, 'Letters should fail');
  console.log('   ✓ E.164 format validator passed.');

  // Test 2: OTP validator
  console.log('2. Testing OTP Validator:');
  assert.strictEqual(isValidOTP('889922'), true, '6-digit OTP should pass');
  assert.strictEqual(isValidOTP('12345'), false, '5-digit OTP should fail');
  assert.strictEqual(isValidOTP('1234567'), false, '7-digit OTP should fail');
  assert.strictEqual(isValidOTP('12345a'), false, 'Non-digit OTP should fail');
  console.log('   ✓ 6-digit OTP validator passed.');

  // Test 3: JWT signing & verification
  console.log('3. Testing JWT Access Token Signing & Verification:');
  const tokenPayload = { userId: 'u_test_101', phone: '+84912345678', roles: ['user'] };
  const accessToken = signAccessToken(tokenPayload);
  assert.ok(accessToken && typeof accessToken === 'string', 'Should return token string');
  const decoded = verifyAccessToken(accessToken);
  assert.strictEqual(decoded.userId, 'u_test_101');
  assert.strictEqual(decoded.phone, '+84912345678');
  assert.deepStrictEqual(decoded.roles, ['user']);
  console.log('   ✓ JWT Access Token sign & verify passed.');

  // Test 4: OTP Request & Brute Force Lockout
  console.log('4. Testing OTP Request & Brute-Force Lockout (FR-AUTH-02):');
  const testPhone = `+84988${Math.floor(100000 + Math.random() * 900000)}`;
  const reqResult = await otpService.requestOtp(testPhone);
  assert.strictEqual(reqResult.phone, testPhone);
  assert.strictEqual(reqResult.expiresInSeconds, config.otp.expirySeconds);
  assert.ok(reqResult.mockOtp, 'Mock OTP should be present in dev mode');
  console.log(`   ✓ OTP requested for ${testPhone}, received code: ${reqResult.mockOtp}`);

  // Test 3 wrong attempts
  console.log('   Testing 3 wrong attempts lockout:');
  for (let attempt = 1; attempt <= 2; attempt++) {
    try {
      await otpService.verifyOtp(testPhone, '000000');
      assert.fail(`Attempt ${attempt} should have thrown`);
    } catch (err) {
      assert.strictEqual(err.statusCode, 400, `Attempt ${attempt} should return 400`);
      assert.strictEqual(err.message, 'Invalid OTP code');
      console.log(`   - Wrong attempt ${attempt} correctly rejected with 400`);
    }
  }

  // 3rd wrong attempt triggers 429
  try {
    await otpService.verifyOtp(testPhone, '000000');
    assert.fail('3rd wrong attempt should have thrown 429');
  } catch (err) {
    assert.strictEqual(err.statusCode, 429, '3rd attempt must return 429 Too Many Requests');
    assert.ok(
      err.message.includes('wrong code 3 times') && err.message.includes('15 minutes'),
      `Error message matches AC-01.2: "${err.message}"`
    );
    console.log('   ✓ 3rd wrong attempt locked out with 429 (15-min lockout).');
  }

  // Test 5: Successful OTP Verification on fresh number
  console.log('5. Testing Successful OTP Verification (AC-01.1):');
  const freshPhone = `+84977${Math.floor(100000 + Math.random() * 900000)}`;
  const freshOtpRes = await otpService.requestOtp(freshPhone);
  const correctOtp = freshOtpRes.mockOtp;
  const verified = await otpService.verifyOtp(freshPhone, correctOtp);
  assert.strictEqual(verified, true, 'Correct OTP should verify');
  console.log('   ✓ Correct OTP verified successfully.');

  // Test 6: Option A Real Email OTP Dispatch
  console.log('6. Testing Option A Email OTP Dispatch:');
  const emailPhone = `+84966${Math.floor(100000 + Math.random() * 900000)}`;
  const testEmail = 'tester@messengo.local';
  const emailOtpRes = await otpService.requestOtp(emailPhone, testEmail);
  assert.strictEqual(emailOtpRes.phone, emailPhone);
  assert.strictEqual(emailOtpRes.email, testEmail);
  assert.ok(emailOtpRes.mockOtp, 'OTP generated');
  console.log(`   ✓ Real Email OTP dispatch triggered for ${testEmail} (Phone: ${emailPhone})`);

  console.log('\n=============================================');
  console.log('🎉 ALL SPRINT 1 CORE AUTH TESTS PASSED!');
  console.log('=============================================\n');
  process.exit(0);
}

runSprint1Tests().catch((err) => {
  console.error('Test failed with error:', err);
  process.exit(1);
});
