const express = require('express');
const cors = require('cors');
const userRoutes = require('./routes/user.routes');
const { errorHandler } = require('../../shared');

const app = express();

app.use(cors());
app.use(express.json());

// Support both direct microservice calls and Nginx gateway pathing
app.use('/api/v1/users', userRoutes);
app.use('/', userRoutes);

app.use(errorHandler);

module.exports = app;
