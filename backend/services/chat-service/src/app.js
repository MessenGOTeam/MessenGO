const express = require('express');
const cors = require('cors');
const contactRoutes = require('./routes/contact.routes');
const chatRoutes = require('./routes/chat.routes');
const { errorHandler } = require('../../../shared');

const app = express();

app.use(cors());
app.use(express.json());

// Routes
app.use('/api/v1/contacts', contactRoutes);
app.use('/contacts', contactRoutes);

app.use('/api/v1/chats', chatRoutes);
app.use('/chats', chatRoutes);

// Global Error Handler
app.use(errorHandler);

module.exports = app;
