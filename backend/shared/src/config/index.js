const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, '../../../../.env') });

module.exports = {
  env: process.env.NODE_ENV || 'development',
  gatewayPort: parseInt(process.env.GATEWAY_PORT || '80', 10),
  mongo: {
    host: process.env.MONGO_HOST || 'localhost',
    port: parseInt(process.env.MONGO_PORT || '27017', 10),
    rootUser: process.env.MONGO_ROOT_USER || 'admin',
    rootPassword: process.env.MONGO_ROOT_PASSWORD || 'messengo_admin_pass',
    uri: process.env.MONGO_URI || 'mongodb://admin:messengo_admin_pass@localhost:27017'
  },
  redis: {
    host: process.env.REDIS_HOST || 'localhost',
    port: parseInt(process.env.REDIS_PORT || '6379', 10)
  },
  jwt: {
    accessSecret: process.env.JWT_ACCESS_SECRET || 'messengo_access_jwt_super_secret_key_2026',
    refreshSecret: process.env.JWT_REFRESH_SECRET || 'messengo_refresh_jwt_super_secret_key_2026',
    accessExpiry: process.env.JWT_ACCESS_EXPIRY || '15m',
    refreshExpiry: process.env.JWT_REFRESH_EXPIRY || '30d'
  },
  otp: {
    expirySeconds: parseInt(process.env.OTP_EXPIRY_SECONDS || '300', 10), // 5 minutes per SRS
    mockEnabled: process.env.OTP_MOCK_ENABLED !== 'false',
    deliveryMethod: process.env.OTP_DELIVERY_METHOD || 'email' // 'email' | 'mock' | 'sms'
  },
  smtp: {
    host: process.env.SMTP_HOST || 'smtp.gmail.com',
    port: parseInt(process.env.SMTP_PORT || '587', 10),
    secure: process.env.SMTP_SECURE === 'true',
    user: process.env.SMTP_USER || '',
    pass: process.env.SMTP_PASS || '',
    from: process.env.SMTP_FROM || '"MessenGO Security" <noreply@messengo.com>'
  },
  ports: {
    auth: parseInt(process.env.AUTH_SERVICE_PORT || '3001', 10),
    user: parseInt(process.env.USER_SERVICE_PORT || '3002', 10),
    chat: parseInt(process.env.CHAT_SERVICE_PORT || '3003', 10),
    message: parseInt(process.env.MESSAGE_SERVICE_PORT || '3004', 10),
    media: parseInt(process.env.MEDIA_SERVICE_PORT || '3005', 10),
    call: parseInt(process.env.CALL_SERVICE_PORT || '3006', 10),
    utility: parseInt(process.env.UTILITY_SERVICE_PORT || '3007', 10),
    search: parseInt(process.env.SEARCH_SERVICE_PORT || '3008', 10),
    notification: parseInt(process.env.NOTIFICATION_SERVICE_PORT || '3009', 10)
  }
};
