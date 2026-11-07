const Message = require('../../models/Message');
const Conversation = require('../../models/Conversation');
const socketManager = require('../socketManager');
const { getRedisClient } = require('../../../../../shared');

class MessageHandler {
  /**
   * Handle incoming WebSocket message events.
   * Format: { event: "msg.send" | "msg.delivered_ack" | "msg.read_receipt" | "msg.typing", data: { ... } }
   */
  async handleEvent(ws, rawData) {
    let packet;
    try {
      packet = typeof rawData === 'string' ? JSON.parse(rawData) : JSON.parse(rawData.toString());
    } catch (err) {
      this.sendError(ws, 'INVALID_JSON', 'Tin nhắn gửi lên không đúng định dạng JSON');
      return;
    }

    const { event, data } = packet || {};
    if (!event) {
      this.sendError(ws, 'MISSING_EVENT', 'Trường event là bắt buộc');
      return;
    }

    switch (event) {
      case 'msg.send':
        await this.handleMsgSend(ws, data);
        break;

      case 'msg.delivered_ack':
        await this.handleDeliveredAck(ws, data);
        break;

      case 'msg.read_receipt':
        await this.handleReadReceipt(ws, data);
        break;

      case 'msg.typing':
        await this.handleTyping(ws, data);
        break;

      case 'ping':
        ws.send(JSON.stringify({ event: 'pong', data: { timestamp: Date.now() } }));
        break;

      default:
        this.sendError(ws, 'UNKNOWN_EVENT', `Không hỗ trợ event '${event}'`);
    }
  }

  /**
   * Step 5 -> 9 & 12 (Sequence Diagram 3):
   * msg.send -> Deduplication -> Insert MESSAGE -> Update CONVERSATION -> msg.ack -> XADD Redis Streams
   */
  async handleMsgSend(ws, data) {
    const senderId = ws.userId;
    const { clientMsgId, convId, content, type = 'text', mediaId = null } = data || {};

    if (!clientMsgId || !convId || !content || !content.trim()) {
      this.sendError(ws, 'INVALID_PAYLOAD', 'clientMsgId, convId và content không được để trống');
      return;
    }

    try {
      // Step 6: Deduplication check by clientMsgId in MongoDB
      const existingMessage = await Message.findOne({
        conversationId: convId,
        clientMsgId
      });

      if (existingMessage) {
        // Resend msg.ack immediately without incrementing seq
        ws.send(
          JSON.stringify({
            event: 'msg.ack',
            data: {
              clientMsgId: existingMessage.clientMsgId,
              msgId: existingMessage.id,
              seq: existingMessage.seq,
              status: existingMessage.status,
              convId,
              createdAt: existingMessage.createdAt,
              isDuplicate: true
            }
          })
        );
        return;
      }

      // Check conversation membership
      const conversation = await Conversation.findOne({
        _id: convId,
        members: senderId
      });

      if (!conversation) {
        this.sendError(ws, 'CONVERSATION_NOT_FOUND', 'Cuộc trò chuyện không tồn tại hoặc bạn không tham gia');
        return;
      }

      const recipientId = conversation.members.find((m) => m !== senderId);
      const nextSeq = conversation.lastSeq + 1;
      const now = new Date();

      // Step 7: Insert MESSAGE (seq, status: "sent")
      const message = await Message.create({
        conversationId: convId,
        clientMsgId,
        senderId,
        recipientId,
        content: content.trim(),
        type,
        mediaId,
        seq: nextSeq,
        status: 'sent',
        createdAt: now
      });

      // Step 8: Update CONVERSATION (lastMsgId, lastSeq, hasMessages)
      conversation.lastSeq = nextSeq;
      conversation.messageCount += 1;
      conversation.hasMessages = true;
      conversation.lastMessageAt = now;
      conversation.lastMessage = {
        id: message.id,
        clientMsgId,
        senderId,
        text: content.trim(),
        type,
        seq: nextSeq,
        createdAt: now
      };

      if (recipientId) {
        const recipientSetting = conversation.memberSettings.find((m) => m.userId === recipientId);
        if (recipientSetting) {
          recipientSetting.unreadCount = (recipientSetting.unreadCount || 0) + 1;
        }
      }

      await conversation.save();

      // Step 9: WS ACK: msg.ack { clientMsgId, seq, status: "sent" } back to Sender
      ws.send(
        JSON.stringify({
          event: 'msg.ack',
          data: {
            clientMsgId,
            msgId: message.id,
            seq: nextSeq,
            status: 'sent',
            convId,
            createdAt: now.getTime()
          }
        })
      );

      // Step 12: XADD stream:message.created { convId, msgId, seq, receiverId } into Redis Streams
      try {
        const redis = getRedisClient();
        if (redis && (redis.status === 'ready' || redis.status === 'connect')) {
          await redis.xadd(
            'stream:message.created',
            '*',
            'convId',
            String(convId),
            'clientMsgId',
            String(clientMsgId),
            'msgId',
            String(message.id),
            'seq',
            String(nextSeq),
            'senderId',
            String(senderId),
            'receiverId',
            String(recipientId || ''),
            'content',
            String(content.trim()),
            'type',
            String(type),
            'createdAt',
            String(now.getTime())
          );
        } else {
          // If Redis is in local test mode or offline, fallback directly to active sockets delivery
          if (recipientId && socketManager.isUserOnline(recipientId)) {
            socketManager.sendToUser(recipientId, 'msg.receive', {
              convId,
              clientMsgId,
              msgId: message.id,
              seq: nextSeq,
              senderId,
              content: content.trim(),
              type,
              createdAt: now.getTime()
            });
          }
        }
      } catch (redisErr) {
        console.warn('[MessageHandler] Redis XADD warning:', redisErr.message);
        // Direct delivery fallback
        if (recipientId && socketManager.isUserOnline(recipientId)) {
          socketManager.sendToUser(recipientId, 'msg.receive', {
            convId,
            clientMsgId,
            msgId: message.id,
            seq: nextSeq,
            senderId,
            content: content.trim(),
            type,
            createdAt: now.getTime()
          });
        }
      }
    } catch (error) {
      console.error('[MessageHandler] msg.send error:', error);
      this.sendError(ws, 'SERVER_ERROR', error.message);
    }
  }

  /**
   * Step 16 -> 17 (Sequence Diagram 3):
   * Recipient sends msg.delivered_ack { seq, convId } -> Update status="delivered" -> Push msg.status_update to Sender
   */
  async handleDeliveredAck(ws, data) {
    const recipientId = ws.userId;
    const { convId, seq, msgId } = data || {};

    if (!convId || seq === undefined) return;

    try {
      // Update message in DB
      const message = await Message.findOneAndUpdate(
        { conversationId: convId, seq: Number(seq) },
        { status: 'delivered' },
        { new: true }
      );

      if (message) {
        // Step 17: Push msg.status_update to Sender (User A)
        socketManager.sendToUser(message.senderId, 'msg.status_update', {
          convId,
          seq: Number(seq),
          msgId: message.id,
          status: 'delivered'
        });
      }
    } catch (err) {
      console.warn('[MessageHandler] delivered_ack error:', err.message);
    }
  }

  /**
   * Step 21 -> 24 (Sequence Diagram 3):
   * User B views chat -> msg.read_receipt { convId, lastReadSeq } -> atomic update -> push msg.read_receipt to Sender
   */
  async handleReadReceipt(ws, data) {
    const readerId = ws.userId;
    const { convId, lastReadSeq } = data || {};

    if (!convId || lastReadSeq === undefined) return;

    try {
      const parsedSeq = Number(lastReadSeq);

      // Step 23: Atomic update on Conversation memberSettings
      const conversation = await Conversation.findOne({
        _id: convId,
        members: readerId
      });

      if (!conversation) return;

      const mySetting = conversation.memberSettings.find((m) => m.userId === readerId);
      if (mySetting) {
        mySetting.lastReadSeq = Math.max(mySetting.lastReadSeq || 0, parsedSeq);
        mySetting.unreadCount = 0;
      }

      await conversation.save();

      // Update message status in MongoDB to 'seen' for all messages seq <= parsedSeq sent to readerId
      await Message.updateMany(
        {
          conversationId: convId,
          recipientId: readerId,
          seq: { $lte: parsedSeq },
          status: { $ne: 'seen' }
        },
        { status: 'seen' }
      );

      // Step 24: Push msg.read_receipt to Sender (User A)
      const senderId = conversation.members.find((m) => m !== readerId);
      if (senderId) {
        socketManager.sendToUser(senderId, 'msg.read_receipt', {
          convId,
          readerId,
          lastReadSeq: parsedSeq
        });
      }
    } catch (err) {
      console.warn('[MessageHandler] read_receipt error:', err.message);
    }
  }

  /**
   * Typing indicator
   */
  async handleTyping(ws, data) {
    const currentUserId = ws.userId;
    const { convId, isTyping } = data || {};

    if (!convId) return;

    try {
      const conversation = await Conversation.findOne({
        _id: convId,
        members: currentUserId
      });
      if (!conversation) return;

      const peerId = conversation.members.find((m) => m !== currentUserId);
      if (peerId) {
        socketManager.sendToUser(peerId, 'msg.typing', {
          convId,
          userId: currentUserId,
          isTyping: Boolean(isTyping)
        });
      }
    } catch (e) {}
  }

  sendError(ws, code, message) {
    if (ws.readyState === ws.OPEN) {
      ws.send(
        JSON.stringify({
          event: 'error',
          data: { code, message }
        })
      );
    }
  }
}

module.exports = new MessageHandler();
