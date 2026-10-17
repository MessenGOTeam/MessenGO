const express = require('express');
const router = express.Router();
const authController = require('../controllers/auth.controller');

// Health check
router.get('/health', (req, res) => {
  res.json({ status: 'ok', service: 'auth-service' });
});

// Sprint 1 Auth Flows
router.post('/otp/request', (req, res, next) => authController.requestOtp(req, res, next));
router.post('/otp/verify', (req, res, next) => authController.verifyOtp(req, res, next));
router.post('/token/refresh', (req, res, next) => authController.refreshToken(req, res, next));
router.post('/logout', (req, res, next) => authController.logout(req, res, next));

module.exports = router;
