// MessenGO MongoDB Initialization Script (Database-per-Service)

// 1. Identity Database
db = db.getSiblingDB('messengo_identity');
db.createCollection('users');
db.users.createIndex({ "phone": 1 }, { unique: true });
db.createCollection('devices');
db.devices.createIndex({ "userId": 1 });
db.devices.createIndex({ "fcmToken": 1 });
db.createCollection('contacts');
db.contacts.createIndex({ "ownerId": 1, "contactUserId": 1 }, { unique: true });
db.contacts.createIndex({ "ownerId": 1, "status": 1 });

// 2. Auth Database
db = db.getSiblingDB('messengo_auth');
db.createCollection('refresh_tokens');
db.refresh_tokens.createIndex({ "tokenHash": 1 }, { unique: true });
db.refresh_tokens.createIndex({ "userId": 1 });
db.refresh_tokens.createIndex({ "expiresAt": 1 }, { expireAfterSeconds: 0 });
db.createCollection('role_assignments');
db.role_assignments.createIndex({ "userId": 1, "role": 1 }, { unique: true });

// 3. Message Database
db = db.getSiblingDB('messengo_message');
db.createCollection('conversations');
db.conversations.createIndex({ "pairKey": 1 }, { unique: true });
db.createCollection('conversation_members');
db.conversation_members.createIndex({ "conversationId": 1, "userId": 1 }, { unique: true });
db.conversation_members.createIndex({ "userId": 1 });
db.createCollection('messages');
db.messages.createIndex({ "clientMsgId": 1 }, { unique: true });
db.messages.createIndex({ "conversationId": 1, "seq": 1 }, { unique: true });
db.messages.createIndex({ "conversationId": 1, "createdAt": -1 });

// 4. Media Database
db = db.getSiblingDB('messengo_media');
db.createCollection('media');
db.media.createIndex({ "ownerId": 1 });
db.media.createIndex({ "status": 1 });

// 5. Call Database
db = db.getSiblingDB('messengo_call');
db.createCollection('call_logs');
db.call_logs.createIndex({ "conversationId": 1 });
db.call_logs.createIndex({ "callerId": 1 });
db.call_logs.createIndex({ "calleeId": 1 });
db.call_logs.createIndex({ "startedAt": -1 });

// 6. Utility Database (Backup jobs)
db = db.getSiblingDB('messengo_utility');
db.createCollection('backup_jobs');
db.backup_jobs.createIndex({ "userId": 1 });
db.backup_jobs.createIndex({ "status": 1 });

// 7. Search Database
db = db.getSiblingDB('messengo_search');
db.createCollection('message_index');
db.message_index.createIndex({ "userId": 1, "content": "text" });

print("MessenGO MongoDB initialization completed successfully.");
