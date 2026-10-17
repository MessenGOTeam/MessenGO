const mongoose = require('mongoose');
const config = require('../config');

/**
 * Connect to service-specific MongoDB database.
 * Supports Database-per-Service pattern.
 * @param {string} dbName - Target database name (e.g. messengo_auth, messengo_identity)
 * @returns {Promise<mongoose.Connection>}
 */
async function connectMongo(dbName) {
  try {
    const baseUri = config.mongo.uri.replace(/\/[^/?]+(\?|$)/, '/');
    const uri = `${baseUri.replace(/\/$/, '')}/${dbName}?authSource=admin`;

    const conn = await mongoose.connect(uri, {
      serverSelectionTimeoutMS: 5000
    });
    console.log(`[MongoDB] Connected successfully to database: ${dbName}`);
    return conn;
  } catch (error) {
    console.error(`[MongoDB] Connection error to ${dbName}:`, error.message);
    // Don't crash immediately in dev if DB is starting up
    return null;
  }
}

module.exports = {
  connectMongo,
  mongoose
};
