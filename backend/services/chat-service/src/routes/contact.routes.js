const express = require('express');
const router = express.Router();
const contactController = require('../controllers/contact.controller');
const { authenticateToken } = require('../../../../shared');

// Health check
router.get('/health', (req, res) => {
  res.json({ status: 'ok', service: 'chat-service:contacts' });
});

// POST /api/v1/contacts/request - Send friend request (FR-CON-02, Seq 2 Diagram)
router.post('/request', authenticateToken, (req, res, next) => {
  contactController.requestContact(req, res, next);
});

// POST /api/v1/contacts/respond - Accept/Reject friend request (FR-CON-03, Seq 2 Diagram)
router.post('/respond', authenticateToken, (req, res, next) => {
  contactController.respondContact(req, res, next);
});

// POST /api/v1/contacts/block - Block user (FR-CON-04: messages_only, messages_and_calls, all)
router.post('/block', authenticateToken, (req, res, next) => {
  contactController.blockContact(req, res, next);
});

// POST /api/v1/contacts/unblock - Unblock user
router.post('/unblock', authenticateToken, (req, res, next) => {
  contactController.unblockContact(req, res, next);
});

// GET /api/v1/contacts - Get contacts list (?type=friends|incoming|outgoing|blocked)
router.get('/', authenticateToken, (req, res, next) => {
  contactController.getContacts(req, res, next);
});

module.exports = router;
