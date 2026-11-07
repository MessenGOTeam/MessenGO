const express = require('express');
const router = express.Router();
const chatController = require('../controllers/chat.controller');
const { authenticateToken } = require('../../../../shared');

// All chat routes require authentication
router.use(authenticateToken);

// POST /api/v1/chats - Create or get 1-1 conversation (with unique pairKey)
router.post('/', (req, res, next) => {
  chatController.getOrCreateConversation(req, res, next);
});

// GET /api/v1/chats - List active conversations (empty ones filtered out)
router.get('/', (req, res, next) => {
  chatController.getConversations(req, res, next);
});

// POST /api/v1/chats/read - Mark messages as read (Seq 3 Diagram)
router.post('/read', (req, res, next) => {
  chatController.markAsRead(req, res, next);
});

// GET /api/v1/chats/:chatId - Get conversation details
router.get('/:chatId', (req, res, next) => {
  chatController.getConversationById(req, res, next);
});

// POST /api/v1/chats/:chatId/messages - Send message via HTTP
router.post('/:chatId/messages', (req, res, next) => {
  chatController.sendMessage(req, res, next);
});

// POST /api/v1/chats/:chatId/read - Mark messages as read by chatId
router.post('/:chatId/read', (req, res, next) => {
  chatController.markAsRead(req, res, next);
});

module.exports = router;

