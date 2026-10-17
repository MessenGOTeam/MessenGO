const express = require('express');
const cors = require('cors');
const authRoutes = require('./routes/auth.routes');
const { errorHandler } = require('../../shared');

const app = express();

app.use(cors());
app.use(express.json());

// Mount for both direct service access and Nginx gateway pathing
app.use('/api/v1/auth', authRoutes);
app.use('/', authRoutes);

app.use(errorHandler);

module.exports = app;
