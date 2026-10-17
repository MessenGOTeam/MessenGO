const mongoose = require('mongoose');

const roleAssignmentSchema = new mongoose.Schema(
  {
    userId: {
      type: String,
      required: true,
      index: true
    },
    role: {
      type: String,
      enum: ['user', 'admin'],
      default: 'user'
    }
  },
  {
    timestamps: true
  }
);

roleAssignmentSchema.index({ userId: 1, role: 1 }, { unique: true });

module.exports = mongoose.model('RoleAssignment', roleAssignmentSchema, 'role_assignments');
