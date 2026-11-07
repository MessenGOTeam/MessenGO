const mongoose = require('mongoose');

const messageSchema = new mongoose.Schema(
  {
    conversationId: {
      type: String,
      required: true,
      index: true
    },
    clientMsgId: {
      type: String,
      required: true,
      index: true
    },
    senderId: {
      type: String,
      required: true,
      index: true
    },
    recipientId: {
      type: String,
      index: true
    },
    content: {
      type: String,
      required: true,
      trim: true
    },
    type: {
      type: String,
      enum: ['text', 'image', 'video', 'file', 'call_log'],
      default: 'text'
    },
    mediaId: {
      type: String,
      default: null
    },
    seq: {
      type: Number,
      required: true,
      index: true
    },
    status: {
      type: String,
      enum: ['pending', 'sent', 'delivered', 'seen'],
      default: 'sent'
    }
  },
  {
    timestamps: true,
    toJSON: {
      transform(doc, ret) {
        ret.id = ret._id.toString();
        delete ret._id;
        delete ret.__v;
        return ret;
      }
    }
  }
);

messageSchema.index({ conversationId: 1, seq: 1 }, { unique: true });
messageSchema.index({ conversationId: 1, clientMsgId: 1 }, { unique: true });

module.exports = mongoose.models.Message || mongoose.model('Message', messageSchema, 'messages');

