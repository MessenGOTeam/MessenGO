/**
 * MessenGO - Database Seeding Script (Sprint 2)
 *
 * Populates MongoDB with standard realistic accounts and relationships
 * for testing Search, Friendship, Outbox, and 1-1 Chat.
 *
 * Databases targeted:
 *  - messengo_identity: users, contacts
 *  - messengo_auth: role_assignments
 *  - messengo_message: conversations
 *
 * Usage:
 *  node backend/scripts/seed.js
 *  npm run seed (from backend/)
 */

const mongoose = require('mongoose');
const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, '../../.env') });

const config = require('../shared/src/config');

// Fixed deterministic IDs for consistent testing
const USER_IDS = {
  USER_1: new mongoose.Types.ObjectId('672cf0010000000000000001'), // Phú (Tài khoản chính)
  USER_2: new mongoose.Types.ObjectId('672cf0010000000000000002'), // Trần Thị Bích (Bạn bè - accepted)
  USER_3: new mongoose.Types.ObjectId('672cf0010000000000000003'), // Lê Hoàng Long (Đã gửi lời mời - pending_received)
  USER_4: new mongoose.Types.ObjectId('672cf0010000000000000004'), // Phạm Minh Tuấn (Người lạ - none)
  USER_5: new mongoose.Types.ObjectId('672cf0010000000000000005')  // Đặng Thu Hà (Bị chặn - blocked)
};

const SEED_USERS = [
  {
    _id: USER_IDS.USER_1,
    phone: '+84912345678',
    displayName: 'Phú (Tôi)',
    statusText: 'Đang phát triển MessenGO 🚀',
    locale: 'vi',
    accountStatus: 'active'
  },
  {
    _id: USER_IDS.USER_2,
    phone: '+84901111111',
    displayName: 'Trần Thị Bích',
    statusText: 'Hôm nay trời đẹp quá ☀️',
    locale: 'vi',
    accountStatus: 'active'
  },
  {
    _id: USER_IDS.USER_3,
    phone: '+84902222222',
    displayName: 'Lê Hoàng Long',
    statusText: 'Kết bạn trao đổi bài tập nhóm nhé!',
    locale: 'vi',
    accountStatus: 'active'
  },
  {
    _id: USER_IDS.USER_4,
    phone: '+84903333333',
    displayName: 'Phạm Minh Tuấn',
    statusText: 'Sinh viên CNTT MessenGO',
    locale: 'vi',
    accountStatus: 'active'
  },
  {
    _id: USER_IDS.USER_5,
    phone: '+84904444444',
    displayName: 'Đặng Thu Hà',
    statusText: 'Vui lòng không làm phiền 🔕',
    locale: 'vi',
    accountStatus: 'active'
  }
];

function getMongoUri(dbName) {
  let rawUri = config.mongo.uri;
  try {
    const parsed = new URL(rawUri);
    parsed.pathname = `/${dbName}`;
    parsed.searchParams.set('authSource', 'admin');
    return parsed.toString();
  } catch {
    const qIdx = rawUri.indexOf('?');
    const base = qIdx === -1 ? rawUri : rawUri.slice(0, qIdx);
    return `${base.replace(/\/+$/, '')}/${dbName}?authSource=admin`;
  }
}

async function seedDatabase() {
  console.log('====================================================');
  console.log('🌱 MessenGO - Seeding Databases with Mock Matrix');
  console.log('====================================================\n');

  // 1. Connect & Seed 'messengo_identity' (users, contacts)
  const identityUri = getMongoUri('messengo_identity');
  console.log(`[1/3] Connecting to messengo_identity: ${identityUri.replace(/:[^:]*@/, ':***@')}`);
  const identityConn = await mongoose.createConnection(identityUri, { serverSelectionTimeoutMS: 5000 }).asPromise();

  const UserCol = identityConn.collection('users');
  const ContactCol = identityConn.collection('contacts');

  // Upsert users
  console.log('  -> Upserting 5 mock users into messengo_identity.users...');
  for (const user of SEED_USERS) {
    await UserCol.updateOne(
      { phone: user.phone },
      { $set: user },
      { upsert: true }
    );
    console.log(`     ✓ User: ${user.displayName} (${user.phone})`);
  }

  // Clear & Re-seed relationships for clean test state
  const testIds = Object.values(USER_IDS);
  await ContactCol.deleteMany({
    $or: [
      { ownerId: { $in: testIds } },
      { contactUserId: { $in: testIds } }
    ]
  });

  console.log('  -> Seeding friend relationships into messengo_identity.contacts...');

  // Relationship 1: User 1 <-> User 2 (BẠN BÈ / ACCEPTED)
  await ContactCol.insertOne({
    ownerId: USER_IDS.USER_1,
    contactUserId: USER_IDS.USER_2,
    alias: 'Bích Nhóm Trưởng',
    status: 'accepted',
    createdAt: new Date(),
    updatedAt: new Date()
  });
  await ContactCol.insertOne({
    ownerId: USER_IDS.USER_2,
    contactUserId: USER_IDS.USER_1,
    alias: 'Phú Dev',
    status: 'accepted',
    createdAt: new Date(),
    updatedAt: new Date()
  });
  console.log('     ✓ User 1 <-> User 2: Bạn bè (accepted)');

  // Relationship 2: User 3 -> User 1 (LỜI MỜI ĐẾN USER 1 / PENDING_RECEIVED)
  await ContactCol.insertOne({
    ownerId: USER_IDS.USER_3,
    contactUserId: USER_IDS.USER_1,
    alias: null,
    status: 'pending',
    message: 'Chào Phú, mình kết bạn trao đổi bài tập nhóm nhé!',
    createdAt: new Date(),
    updatedAt: new Date()
  });
  console.log('     ✓ User 3 -> User 1: Gửi lời mời kết bạn (pending_received for User 1)');

  // Relationship 3: User 1 -> User 5 (BỊ CHẶN / BLOCKED)
  await ContactCol.insertOne({
    ownerId: USER_IDS.USER_1,
    contactUserId: USER_IDS.USER_5,
    alias: null,
    status: 'blocked',
    blockType: 'all',
    blockedAt: new Date(),
    createdAt: new Date(),
    updatedAt: new Date()
  });
  console.log('     ✓ User 1 -> User 5: Đã chặn (blocked)');

  // User 4: Chưa có quan hệ (status: none) -> Thích hợp để test tìm kiếm và bấm "Kết bạn ngay"
  console.log('     ✓ User 4: Người lạ (none) -> Sẵn sàng cho test tìm kiếm');

  // 2. Connect & Seed 'messengo_auth' (role_assignments)
  const authUri = getMongoUri('messengo_auth');
  console.log(`\n[2/3] Connecting to messengo_auth...`);
  const authConn = await mongoose.createConnection(authUri, { serverSelectionTimeoutMS: 5000 }).asPromise();
  const RoleCol = authConn.collection('role_assignments');

  console.log('  -> Upserting roles into messengo_auth.role_assignments...');
  for (const user of SEED_USERS) {
    await RoleCol.updateOne(
      { userId: user._id.toString(), role: 'user' },
      { $set: { userId: user._id.toString(), role: 'user', createdAt: new Date() } },
      { upsert: true }
    );
  }
  // Admin role for User 1
  await RoleCol.updateOne(
    { userId: USER_IDS.USER_1.toString(), role: 'admin' },
    { $set: { userId: USER_IDS.USER_1.toString(), role: 'admin', createdAt: new Date() } },
    { upsert: true }
  );
  console.log('     ✓ Roles assigned (admin for User 1, user for all)');

  // 3. Connect & Seed 'messengo_message' (conversations)
  const msgUri = getMongoUri('messengo_message');
  console.log(`\n[3/3] Connecting to messengo_message...`);
  const msgConn = await mongoose.createConnection(msgUri, { serverSelectionTimeoutMS: 5000 }).asPromise();
  const ConvCol = msgConn.collection('conversations');

  const pairKey = [USER_IDS.USER_1.toString(), USER_IDS.USER_2.toString()].sort().join(':');
  await ConvCol.updateOne(
    { pairKey },
    {
      $set: {
        pairKey,
        type: 'direct',
        title: null,
        avatarMediaId: null,
        members: [USER_IDS.USER_1.toString(), USER_IDS.USER_2.toString()],
        memberSettings: [
          { userId: USER_IDS.USER_1.toString(), lastReadSeq: 0, unreadCount: 0, isPinned: false, isMuted: false },
          { userId: USER_IDS.USER_2.toString(), lastReadSeq: 0, unreadCount: 0, isPinned: false, isMuted: false }
        ],
        lastMessage: {
          id: 'msg_seed_001',
          clientMsgId: 'client_msg_seed_001',
          senderId: USER_IDS.USER_2.toString(),
          text: 'Chào Phú! Tụi mình đã là bạn bè trên MessenGO 🚀',
          type: 'text',
          seq: 1,
          createdAt: new Date()
        },
        lastSeq: 1,
        createdAt: new Date(),
        updatedAt: new Date()
      }
    },
    { upsert: true }
  );
  console.log('     ✓ Conversation seeded between User 1 and User 2');

  // Close all connections
  await identityConn.close();
  await authConn.close();
  await msgConn.close();

  console.log('\n====================================================');
  console.log('✅ SEED COMPLETED SUCCESSFULLY!');
  console.log('====================================================');
  console.log('\nDanh sách tài khoản kiểm thử đã sẵn sàng:');
  console.log('  1. Phú (Tôi)         : +84912345678  [Tài khoản test đăng nhập chính]');
  console.log('  2. Trần Thị Bích     : +84901111111  [Đã là bạn bè - có sẵn cuộc trò chuyện]');
  console.log('  3. Lê Hoàng Long     : +84902222222  [Đã gửi lời mời kết bạn tới bạn - test Đồng ý]');
  console.log('  4. Phạm Minh Tuấn    : +84903333333  [Người lạ - test Tìm kiếm & Bấm kết bạn]');
  console.log('  5. Đặng Thu Hà       : +84904444444  [Đã chặn - test tính năng ẩn]');
  console.log('  6. Số không tồn tại  : +84999999999  [Test lỗi 404 thật - Real Check]');
  console.log('----------------------------------------------------\n');
}

seedDatabase().catch((err) => {
  console.error('❌ Lỗi khi seed database:', err);
  process.exit(1);
});
