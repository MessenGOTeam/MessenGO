const mongoose = require('mongoose');

const memberSettingSchema = new mongoose.Schema(
  {
    userId: { type: String, required: true },
    lastReadSeq: { type: Number, default: 0 },
    unreadCount: { type: Number, default: 0 },
    isPinned: { type: Boolean, default: false },
    isMuted: { type: Boolean, default: false }
  },
  { _id: false }
);

const conversationSchema = new mongoose.Schema(
  {
    pairKey: {
      type: String,
      unique: true,
      sparse: true,
      index: true
    },
    type: {
      type: String,
      enum: ['direct', 'group'],
      default: 'direct',
      index: true
    },
    title: {
      type: String,
      trim: true,
      default: null
    },
    avatarMediaId: {
      type: String,
      default: null
    },
    members: [
      {
        type: String,
        required: true,
        index: true
      }
    ],
    memberSettings: [memberSettingSchema],
    lastMessage: {
      id: { type: String, default: null },
      clientMsgId: { type: String, default: null },
      senderId: { type: String, default: null },
      text: { type: String, default: null },
      type: { type: String, default: 'text' },
      seq: { type: Number, default: 0 },
      createdAt: { type: Date, default: null }
    },
    lastMessageAt: {
      type: Date,
      default: null,
      index: true
    },
    lastSeq: {
      type: Number,
      default: 0
    },
    messageCount: {
      type: Number,
      default: 0
    },
    hasMessages: {
      type: Boolean,
      default: false,
      index: true
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

conversationSchema.statics.generatePairKey = function (id1, id2) {
  if (!id1 || !id2) return null;
  return [String(id1), String(id2)].sort().join('_');
};

module.exports =
  mongoose.models.Conversation || mongoose.model('Conversation', conversationSchema, 'conversations');
