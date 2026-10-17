const nodemailer = require('nodemailer');
const { config } = require('../../../../shared');

class EmailService {
  constructor() {
    this.transporter = null;
    this._initTransporter();
  }

  _initTransporter() {
    const smtpConfig = config.smtp;

    // Real SMTP configuration (e.g. Gmail SMTP, SendGrid, etc.)
    if (smtpConfig.user && smtpConfig.pass) {
      this.transporter = nodemailer.createTransport({
        host: smtpConfig.host,
        port: smtpConfig.port,
        secure: smtpConfig.secure, // true for 465, false for 587
        auth: {
          user: smtpConfig.user,
          pass: smtpConfig.pass
        }
      });
      console.log(`[Email Service] Configured real SMTP transport using ${smtpConfig.host}:${smtpConfig.port} (${smtpConfig.user})`);
    } else {
      // Local development fallback (MailHog or Mock logger)
      this.transporter = nodemailer.createTransport({
        host: process.env.MAILHOG_HOST || 'localhost',
        port: parseInt(process.env.MAILHOG_SMTP_PORT || '1025', 10),
        ignoreTLS: true
      });
      console.log('[Email Service] Real SMTP credentials (SMTP_USER/SMTP_PASS) not set in .env. Falling back to local SMTP/MailHog on port 1025.');
    }
  }

  /**
   * Send 6-digit OTP verification code to recipient email.
   * @param {string} toEmail - Recipient email address
   * @param {string} otpCode - 6-digit OTP code
   * @param {string} phone - Target phone number being verified
   * @returns {Promise<object>}
   */
  async sendOtpEmail(toEmail, otpCode, phone) {
    if (!toEmail) {
      throw new Error('Recipient email is required to send verification code');
    }

    const expiryMinutes = Math.round((config.otp.expirySeconds || 180) / 60);

    const htmlContent = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <title>MessenGO Verification Code</title>
      <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 20px; }
        .card { max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 12px; border: 1px solid #e2e8f0; padding: 32px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }
        .logo { font-size: 24px; font-weight: 800; color: #2563eb; margin-bottom: 24px; letter-spacing: -0.5px; }
        .title { font-size: 20px; font-weight: 700; color: #0f172a; margin-bottom: 12px; }
        .desc { font-size: 15px; color: #475569; line-height: 1.6; margin-bottom: 24px; }
        .otp-box { background: #eff6ff; border: 2px dashed #93c5fd; border-radius: 8px; padding: 18px; text-align: center; margin: 24px 0; }
        .otp-code { font-size: 34px; font-weight: 800; letter-spacing: 8px; color: #1d4ed8; font-family: monospace; }
        .meta { font-size: 13px; color: #64748b; margin-top: 24px; border-top: 1px solid #f1f5f9; padding-top: 16px; }
        .warning { color: #dc2626; font-weight: 600; }
      </style>
    </head>
    <body>
      <div class="card">
        <div class="logo">📱 MessenGO</div>
        <div class="title">Verify your account</div>
        <p class="desc">
          You are verifying the mobile number <strong>${phone}</strong> on MessenGO. Use the verification code below to complete your sign-in:
        </p>
        <div class="otp-box">
          <div class="otp-code">${otpCode}</div>
        </div>
        <p class="desc">
          This verification code is valid for <strong>${expiryMinutes} minutes</strong> and can only be used once.
        </p>
        <div class="meta">
          <p class="warning">⚠️ Never share this code with anyone. MessenGO support will never ask for your verification code.</p>
          <p>© 2026 MessenGO Team. Secure real-time mobile messaging.</p>
        </div>
      </div>
    </body>
    </html>
    `;

    const mailOptions = {
      from: config.smtp.from,
      to: toEmail,
      subject: `[MessenGO] Your Verification Code is ${otpCode}`,
      text: `Your MessenGO verification code for ${phone} is ${otpCode}. It expires in ${expiryMinutes} minutes.`,
      html: htmlContent
    };

    try {
      const info = await this.transporter.sendMail(mailOptions);
      console.log(`[Email Service] OTP email successfully dispatched to ${toEmail} (MessageId: ${info.messageId})`);
      return {
        sent: true,
        recipient: toEmail,
        messageId: info.messageId
      };
    } catch (err) {
      console.warn(`[Email Service] Failed to deliver email to ${toEmail}: ${err.message}`);
      // Return gracefully so dev flow is not blocked if offline
      return {
        sent: false,
        recipient: toEmail,
        error: err.message
      };
    }
  }
}

module.exports = new EmailService();
