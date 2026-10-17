const express = require('express');
const router = express.Router();
const userController = require('../controllers/user.controller');
const { authenticateToken, requireRole } = require('../../../../shared');

// Health check
router.get('/health', (req, res) => {
  res.json({ status: 'ok', service: 'user-service' });
});

// Internal endpoint called by Auth Service during login/verify
router.post('/internal/upsert', userController.upsertUserInternal);

// Authenticated user routes
router.get('/me', authenticateToken, userController.getMe);
router.put('/me', authenticateToken, userController.updateProfile);
router.get('/search', authenticateToken, userController.searchByPhone);
router.post('/device', authenticateToken, userController.updateDevice);

// Admin-only account lock/unlock
router.patch('/:userId/status', authenticateToken, requireRole('admin'), userController.setAccountStatus);

module.exports = router;
