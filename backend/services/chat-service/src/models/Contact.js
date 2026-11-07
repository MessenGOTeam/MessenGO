const mongoose = require('mongoose');

const contactSchema = new mongoose.Schema(
  {
    ownerId: {
      type: String,
      required: true,
      index: true
    },
    contactUserId: {
      type: String,
      required: true,
      index: true
    },
    contactPhone: {
      type: String,
      trim: true,
      index: true
    },
    alias: {
      type: String,
      trim: true,
      default: null
    },
    message: {
      type: String,
      trim: true,
      default: null
    },
    status: {
      type: String,
      enum: ['pending', 'accepted', 'rejected', 'blocked'],
      default: 'pending',
      index: true
    },
    blockType: {
      type: String,
      enum: ['messages_only', 'messages_and_calls', 'all', null],
      default: null,
      index: true
    },
    blockedAt: {
      type: Date,
      default: null
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

// Each user can have only one contact entry per target user
contactSchema.index({ ownerId: 1, contactUserId: 1 }, { unique: true });

module.exports = mongoose.models.Contact || mongoose.model('Contact', contactSchema, 'contacts');

