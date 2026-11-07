const express = require('express');
const cors = require('cors');
const { errorHandler } = require('../../../shared');

const app = express();

app.use(cors());
app.use(express.json());

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'message-service',
    timestamp: new Date().toISOString()
  });
});

app.get('/api/v1/messages/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'message-service',
    timestamp: new Date().toISOString()
  });
});

app.use(errorHandler);

module.exports = app;
