const mongoose = require('mongoose');

const userSchema = new mongoose.Schema(
  {
    phone: {
      type: String,
      required: true,
      unique: true,
      trim: true,
      index: true
    },
    displayName: {
      type: String,
      trim: true,
      default: ''
    },
    avatarMediaId: {
      type: String,
      default: null
    },
    statusText: {
      type: String,
      default: 'Available'
    },
    locale: {
      type: String,
      enum: ['vi', 'en'],
      default: 'vi'
    },
    accountStatus: {
      type: String,
      enum: ['active', 'locked'],
      default: 'active',
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

module.exports = mongoose.model('User', userSchema, 'users');
