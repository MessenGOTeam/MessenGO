const { getRedisClient } = require('../../../../shared');
const socketManager = require('../socket/socketManager');

const STREAM_NAME = 'stream:message.created';
const GROUP_NAME = 'group:message_delivery';

class RedisStreamsConsumer {
  constructor() {
    this.isRunning = false;
    this.consumerId = `consumer_${process.pid}_${Math.random().toString(36).substring(2, 7)}`;
    this.redisClient = null;
  }

  /**
   * Start the Redis Streams Consumer Group worker.
   */
  async start() {
    try {
      this.redisClient = getRedisClient();
      if (!this.redisClient) {
        console.warn('[Redis Consumer] Redis client not available, consumer disabled');
        return;
      }

      if (this.redisClient.status === 'wait') {
        try {
          await this.redisClient.connect();
        } catch (e) {
          console.warn('[Redis Consumer] Redis connect warning:', e.message);
        }
      }

      // 1. Ensure Stream and Consumer Group exist
      try {
        await this.redisClient.xgroup('CREATE', STREAM_NAME, GROUP_NAME, '$', 'MKSTREAM');
        console.log(`[Redis Consumer] Created consumer group: ${GROUP_NAME} on stream: ${STREAM_NAME}`);
      } catch (err) {
        if (!err.message.includes('BUSYGROUP')) {
          console.warn('[Redis Consumer] xgroup create warning:', err.message);
        }
      }

      this.isRunning = true;
      console.log(`[Redis Consumer] Started ${this.consumerId} listening to ${STREAM_NAME}`);

      // Run non-blocking poll loop
      this.poll();
    } catch (err) {
      console.warn('[Redis Consumer] Start error:', err.message);
    }
  }

  async poll() {
    while (this.isRunning) {
      try {
        if (!this.redisClient || this.redisClient.status !== 'ready') {
          await new Promise((r) => setTimeout(r, 1000));
          continue;
        }

        // XREADGROUP GROUP group:message_delivery <consumer> BLOCK 2000 COUNT 10 STREAMS stream:message.created >
        const results = await this.redisClient.xreadgroup(
          'GROUP',
          GROUP_NAME,
          this.consumerId,
          'BLOCK',
          2000,
          'COUNT',
          10,
          'STREAMS',
          STREAM_NAME,
          '>'
        );

        if (results && results.length > 0) {
          for (const streamEntry of results) {
            const [, messages] = streamEntry;
            for (const [id, rawFields] of messages) {
              await this.processMessage(id, rawFields);
            }
          }
        }
      } catch (err) {
        if (this.isRunning) {
          // If Redis disconnected temporarily, backoff 1s
          await new Promise((r) => setTimeout(r, 1000));
        }
      }
    }
  }

  /**
   * Process a single message event from Redis Streams.
   * Step 13 & 14 in Sequence Diagram 3.
   */
  async processMessage(streamId, rawFields) {
    try {
      // Convert flat array [key, val, key, val] to object
      const fields = {};
      for (let i = 0; i < rawFields.length; i += 2) {
        fields[rawFields[i]] = rawFields[i + 1];
      }

      const {
        convId,
        msgId,
        clientMsgId,
        seq,
        senderId,
        receiverId,
        content,
        type = 'text',
        createdAt
      } = fields;

      if (!receiverId) {
        // Acknowledge invalid stream entry
        await this.redisClient.xack(STREAM_NAME, GROUP_NAME, streamId);
        return;
      }

      // Step 14: Check if Recipient is ONLINE via WebSocket
      const isOnline = socketManager.isUserOnline(receiverId);

      if (isOnline) {
        // WS Push: msg.receive { seq, content, senderId, convId, msgId }
        socketManager.sendToUser(receiverId, 'msg.receive', {
          convId,
          msgId,
          clientMsgId,
          seq: Number(seq),
          senderId,
          content,
          type,
          createdAt: Number(createdAt) || Date.now()
        });
      } else {
        // Step 20: Recipient is OFFLINE -> trigger fallback Push Notification
        // In full system: publish to stream:notification.push or call Notification Service
        // console.log(`[Redis Consumer] Recipient ${receiverId} is OFFLINE. Triggering FCM fallback notification.`);
      }

      // Acknowledge message in Consumer Group
      await this.redisClient.xack(STREAM_NAME, GROUP_NAME, streamId);
    } catch (err) {
      console.warn(`[Redis Consumer] Error processing stream message ${streamId}:`, err.message);
    }
  }

  stop() {
    this.isRunning = false;
  }
}

module.exports = new RedisStreamsConsumer();
