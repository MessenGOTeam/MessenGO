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
    let rawUri = config.mongo.uri;
    if (rawUri.includes('@mongodb:')) {
      try {
        const dns = require('dns');
        await dns.promises.lookup('mongodb');
      } catch {
        rawUri = rawUri.replace('@mongodb:', '@localhost:');
      }
    }

    let uri;
    try {
      const parsedUrl = new URL(rawUri);
      parsedUrl.pathname = `/${dbName}`;
      parsedUrl.searchParams.set('authSource', 'admin');
      uri = parsedUrl.toString();
    } catch {
      const questionIdx = rawUri.indexOf('?');
      const base = questionIdx === -1 ? rawUri : rawUri.slice(0, questionIdx);
      uri = `${base.replace(/\/+$/, '')}/${dbName}?authSource=admin`;
    }

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
