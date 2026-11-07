const axios = require('axios');
const Contact = require('../models/Contact');
const User = require('../models/User');
const { sendSuccess, sendError, isValidE164, config } = require('../../../../shared');

const USER_SERVICE_URL = process.env.USER_SERVICE_INTERNAL_URL || `http://localhost:${config.ports.user}`;

class ContactController {
  /**
   * POST /api/v1/contacts/request
   * Send friend request (FR-CON-02, Seq 2 Diagram).
   * Supports: peerId, contactUserId, contactPhone, message, alias, acceptExisting
   */
  async requestContact(req, res, next) {
    try {
      const requesterId = req.user.userId;
      const requesterPhone = req.user.phone;
      const {
        peerId,
        contactUserId,
        contactPhone,
        alias,
        message,
        acceptExisting
      } = req.body;

      const rawTargetId = peerId || contactUserId;
      if (!contactPhone && !rawTargetId) {
        return sendError(res, 'Vui lòng cung cấp peerId, contactUserId hoặc contactPhone', 400);
      }

      let targetUserId = rawTargetId;
      let targetPhone = contactPhone;
      let targetProfile = null;

      // 1. Resolve target user if phone is provided
      if (contactPhone) {
        if (!isValidE164(contactPhone)) {
          return sendError(res, 'Số điện thoại không đúng định dạng (VD: +84912345678)', 400);
        }

        if (contactPhone === requesterPhone) {
          return sendError(res, 'Không thể gửi lời mời kết bạn cho chính mình', 400, {
            code: 'CANNOT_FRIEND_SELF'
          });
        }

        // Direct DB lookup in IdentityDB first
        try {
          const directUser = await User.findOne({ phone: contactPhone, accountStatus: 'active' });
          if (directUser) {
            targetProfile = directUser.toJSON();
            targetUserId = directUser.id;
          }
        } catch (e) {}

        // Fallback to User Service via HTTP
        if (!targetProfile && USER_SERVICE_URL) {
          try {
            const userResponse = await axios.get(
              `${USER_SERVICE_URL}/search?phone=${encodeURIComponent(contactPhone)}`,
              {
                headers: { Authorization: req.headers.authorization },
                timeout: 3000
              }
            );

            if (userResponse.data?.data?.user) {
              targetProfile = userResponse.data.data.user;
              targetUserId = targetProfile.id;
            }
          } catch (err) {
            if (err.response?.status === 404) {
              return sendError(res, 'Người dùng không tồn tại hoặc không thể tìm thấy', 404, {
                code: 'USER_NOT_FOUND'
              });
            }
          }
        }
      } else if (targetUserId) {
        try {
          const directUser = await User.findById(targetUserId);
          if (directUser) {
            targetProfile = directUser.toJSON();
            targetPhone = directUser.phone;
          }
        } catch (e) {}
      }

      // If user profile was not found in DB or mock test IDs
      if (!targetUserId) {
        if (rawTargetId) {
          targetUserId = rawTargetId;
        } else {
          return sendError(res, 'Người dùng không tồn tại hoặc không thể tìm thấy', 404, {
            code: 'USER_NOT_FOUND'
          });
        }
      }

      // Check self-friending by ID
      if (targetUserId === requesterId) {
        return sendError(res, 'Không thể gửi lời mời kết bạn cho chính mình', 400, {
          code: 'CANNOT_FRIEND_SELF'
        });
      }

      // 2. Check blocks and existing contact relationships
      const incoming = await Contact.findOne({ ownerId: targetUserId, contactUserId: requesterId });
      const outgoing = await Contact.findOne({ ownerId: requesterId, contactUserId: targetUserId });

      // RULE 1: If target has blocked requester with ANY blockType (messages_only, messages_and_calls, all):
      // Target is completely invisible to requester -> return 404 USER_NOT_FOUND!
      if (incoming?.status === 'blocked') {
        return sendError(res, 'Người dùng không tồn tại hoặc không thể tìm thấy', 404, {
          code: 'USER_NOT_FOUND'
        });
      }

      // RULE 2: If requester has blocked target -> 400 YOU_HAVE_BLOCKED_USER
      if (outgoing?.status === 'blocked') {
        return sendError(res, 'Bạn đang chặn người này. Vui lòng bỏ chặn trước khi gửi lời mời kết bạn.', 400, {
          code: 'YOU_HAVE_BLOCKED_USER'
        });
      }

      // RULE 3: Already friends
      if (outgoing?.status === 'accepted' || incoming?.status === 'accepted') {
        return sendError(res, 'Hai bạn đã là bạn bè của nhau rồi', 400, {
          code: 'ALREADY_FRIENDS'
        });
      }

      // RULE 4: Already sent pending request
      if (outgoing?.status === 'pending') {
        return sendError(res, 'Lời mời kết bạn đã được gửi trước đó và đang chờ phản hồi', 400, {
          code: 'REQUEST_ALREADY_PENDING'
        });
      }

      // RULE 5: Mutual request (Target already sent a pending request to Requester)
      if (incoming?.status === 'pending') {
        // If user tapped Accept on the popup (or acceptExisting: true)
        if (acceptExisting === true) {
          incoming.status = 'accepted';
          await incoming.save();

          const acceptedContact = await Contact.findOneAndUpdate(
            { ownerId: requesterId, contactUserId: targetUserId },
            {
              contactPhone: targetPhone || incoming.contactPhone,
              alias: alias ? alias.trim() : null,
              message: message ? message.trim() : null,
              status: 'accepted'
            },
            { new: true, upsert: true }
          );

          return sendSuccess(
            res,
            {
              contact: acceptedContact.toJSON(),
              targetUser: targetProfile,
              status: 'accepted',
              isMutualAccept: true
            },
            'Đã chấp nhận lời mời kết bạn thành công!',
            200
          );
        }

        // Do NOT silently auto-accept! Return 409 Conflict with details for popup dialog
        return sendError(
          res,
          'Người này đã gửi lời mời kết bạn cho bạn trước đó',
          409,
          {
            code: 'INCOMING_REQUEST_PENDING',
            incomingRequest: {
              id: incoming.id,
              senderId: targetUserId,
              senderPhone: targetPhone || incoming.contactPhone,
              senderName: targetProfile?.displayName || 'Người dùng MessenGO',
              senderAvatar: targetProfile?.avatarMediaId || null,
              message: incoming.message || '',
              createdAt: incoming.createdAt
            }
          }
        );
      }

      // Fresh pending request
      const newContact = await Contact.findOneAndUpdate(
        { ownerId: requesterId, contactUserId: targetUserId },
        {
          contactPhone: targetPhone,
          alias: alias ? alias.trim() : null,
          message: message ? message.trim() : null,
          status: 'pending'
        },
        { new: true, upsert: true }
      );

      return sendSuccess(
        res,
        {
          contact: newContact.toJSON(),
          targetUser: targetProfile,
          status: 'pending'
        },
        'Gửi lời mời kết bạn thành công',
        201
      );
    } catch (error) {
      next(error);
    }
  }

  /**
   * POST /api/v1/contacts/respond
   * Accept or Reject a friend request (FR-CON-03, Seq 2 Diagram).
   * Body: { peerId, contactUserId, action: 'accept' | 'reject', alias }
   */
  async respondContact(req, res, next) {
    try {
      const currentUserId = req.user.userId;
      const { peerId, contactUserId, action, alias } = req.body;
      const senderId = peerId || contactUserId;

      if (!senderId) {
        return sendError(res, 'Vui lòng cung cấp peerId hoặc contactUserId', 400);
      }
      if (!action || !['accept', 'reject'].includes(action)) {
        return sendError(res, "Hành động (action) phải là 'accept' hoặc 'reject'", 400, {
          code: 'INVALID_ACTION'
        });
      }

      // Find pending incoming request from senderId
      const incomingReq = await Contact.findOne({
        ownerId: senderId,
        contactUserId: currentUserId,
        status: 'pending'
      });

      if (!incomingReq) {
        return sendError(res, 'Không tìm thấy lời mời kết bạn đang chờ phản hồi', 404, {
          code: 'NO_PENDING_REQUEST'
        });
      }

      if (action === 'accept') {
        // 1. Update sender's contact status to accepted
        incomingReq.status = 'accepted';
        await incomingReq.save();

        // 2. Upsert recipient's contact record to accepted (Bidirectional relationship)
        const recipientContact = await Contact.findOneAndUpdate(
          { ownerId: currentUserId, contactUserId: senderId },
          {
            contactPhone: incomingReq.contactPhone,
            alias: alias ? alias.trim() : null,
            status: 'accepted'
          },
          { new: true, upsert: true }
        );

        return sendSuccess(
          res,
          {
            status: 'accepted',
            peerId: senderId,
            contact: recipientContact.toJSON()
          },
          'Đã chấp nhận lời mời kết bạn',
          200
        );
      } else {
        // action === 'reject'
        incomingReq.status = 'rejected';
        await incomingReq.save();

        return sendSuccess(
          res,
          {
            status: 'rejected',
            peerId: senderId
          },
          'Đã từ chối lời mời kết bạn',
          200
        );
      }
    } catch (error) {
      next(error);
    }
  }

  /**
   * POST /api/v1/contacts/block
   * Block a user with specific block type (FR-CON-04).
   * Body: { peerId, contactUserId, blockType: 'messages_only' | 'messages_and_calls' | 'all' }
   */
  async blockContact(req, res, next) {
    try {
      const currentUserId = req.user.userId;
      const { peerId, contactUserId, blockType } = req.body;
      const targetUserId = peerId || contactUserId;

      if (!targetUserId) {
        return sendError(res, 'Vui lòng cung cấp peerId hoặc contactUserId', 400);
      }

      if (targetUserId === currentUserId) {
        return sendError(res, 'Không thể tự chặn chính mình', 400, {
          code: 'CANNOT_BLOCK_SELF'
        });
      }

      const validBlockTypes = ['messages_only', 'messages_and_calls', 'all'];
      const resolvedBlockType = blockType || 'all';

      if (!validBlockTypes.includes(resolvedBlockType)) {
        return sendError(
          res,
          "Mức độ chặn (blockType) phải là 'messages_only', 'messages_and_calls', hoặc 'all'",
          400,
          { code: 'INVALID_BLOCK_TYPE' }
        );
      }

      // Upsert block record
      const blockedContact = await Contact.findOneAndUpdate(
        { ownerId: currentUserId, contactUserId: targetUserId },
        {
          status: 'blocked',
          blockType: resolvedBlockType,
          blockedAt: new Date()
        },
        { new: true, upsert: true }
      );

      // If target had sent a pending request to current user, dismiss it
      await Contact.updateOne(
        { ownerId: targetUserId, contactUserId: currentUserId, status: 'pending' },
        { status: 'rejected' }
      );

      return sendSuccess(
        res,
        {
          status: 'blocked',
          blockType: resolvedBlockType,
          peerId: targetUserId,
          contact: blockedContact.toJSON()
        },
        'Đã chặn người dùng thành công',
        200
      );
    } catch (error) {
      next(error);
    }
  }

  /**
   * POST /api/v1/contacts/unblock
   * Unblock a previously blocked user.
   * Body: { peerId, contactUserId }
   */
  async unblockContact(req, res, next) {
    try {
      const currentUserId = req.user.userId;
      const { peerId, contactUserId } = req.body;
      const targetUserId = peerId || contactUserId;

      if (!targetUserId) {
        return sendError(res, 'Vui lòng cung cấp peerId hoặc contactUserId', 400);
      }

      const blockedContact = await Contact.findOne({
        ownerId: currentUserId,
        contactUserId: targetUserId,
        status: 'blocked'
      });

      if (!blockedContact) {
        return sendError(res, 'Người dùng này chưa bị chặn', 400, {
          code: 'USER_NOT_BLOCKED'
        });
      }

      blockedContact.status = 'rejected';
      blockedContact.blockType = null;
      blockedContact.blockedAt = null;
      await blockedContact.save();

      return sendSuccess(
        res,
        {
          status: 'unblocked',
          peerId: targetUserId
        },
        'Đã bỏ chặn người dùng thành công',
        200
      );
    } catch (error) {
      next(error);
    }
  }

  /**
   * GET /api/v1/contacts
   * Query params:
   * ?type=friends (default) | incoming | outgoing | blocked
   */
  async getContacts(req, res, next) {
    try {
      const currentUserId = req.user.userId;
      const { type = 'friends' } = req.query;

      let contacts = [];

      if (type === 'friends') {
        contacts = await Contact.find({
          ownerId: currentUserId,
          status: 'accepted'
        }).sort({ updatedAt: -1 });
      } else if (type === 'incoming') {
        contacts = await Contact.find({
          contactUserId: currentUserId,
          status: 'pending'
        }).sort({ createdAt: -1 });
      } else if (type === 'outgoing') {
        contacts = await Contact.find({
          ownerId: currentUserId,
          status: 'pending'
        }).sort({ createdAt: -1 });
      } else if (type === 'blocked') {
        contacts = await Contact.find({
          ownerId: currentUserId,
          status: 'blocked'
        }).sort({ blockedAt: -1 });
      } else {
        return sendError(res, "type phải là 'friends', 'incoming', 'outgoing', hoặc 'blocked'", 400);
      }

      return sendSuccess(
        res,
        {
          type,
          count: contacts.length,
          contacts: contacts.map((c) => c.toJSON())
        },
        'Lấy danh sách thành công',
        200
      );
    } catch (error) {
      next(error);
    }
  }
}

module.exports = new ContactController();
