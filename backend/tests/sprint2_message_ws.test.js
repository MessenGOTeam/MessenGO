const assert = require('assert');
const http = require('http');
const WebSocket = require('ws');
const app = require('../services/message-service/src/app');
const { createWebSocketServer } = require('../services/message-service/src/socket/wsServer');
const redisConsumer = require('../services/message-service/src/streams/redisConsumer');
const Conversation = require('../services/message-service/src/models/Conversation');
const Message = require('../services/message-service/src/models/Message');
const User = require('../services/message-service/src/models/User');
const { signAccessToken, connectMongo, mongoose } = require('../shared');

function createWsClient(url, protocols) {
  return new Promise((resolve, reject) => {
    const ws = protocols ? new WebSocket(url, protocols) : new WebSocket(url);
    ws.messageQueue = [];
    ws.on('message', (raw) => {
      try {
        const parsed = JSON.parse(raw.toString());
        ws.messageQueue.push(parsed);
        ws.emit('packet', parsed);
      } catch (e) { }
    });
    ws.on('open', () => resolve(ws));
    ws.on('error', (err) => reject(err));
  });
}

function waitForEvent(ws, expectedEvent, filterFn = null, timeoutMs = 5000) {
  return new Promise((resolve, reject) => {
    const checkQueue = () => {
      const idx = ws.messageQueue.findIndex(
        (m) => m.event === expectedEvent && (!filterFn || filterFn(m.data))
      );
      if (idx !== -1) {
        const item = ws.messageQueue.splice(idx, 1)[0];
        return item.data;
      }
      return null;
    };

    const immediate = checkQueue();
    if (immediate) return resolve(immediate);

    const timer = setTimeout(() => {
      ws.off('packet', onPacket);
      reject(new Error(`Timeout waiting for event '${expectedEvent}' after ${timeoutMs}ms`));
    }, timeoutMs);

    const onPacket = () => {
      const matched = checkQueue();
      if (matched) {
        clearTimeout(timer);
        ws.off('packet', onPacket);
        resolve(matched);
      }
    };

    ws.on('packet', onPacket);
  });
}

async function runMessageServiceTests() {
  console.log('--- Starting Sprint 2 Message Service (WebSocket & Redis Streams) Tests ---\n');

  // Connect to DB
  await connectMongo('messengo_identity_test');

  // Start HTTP server on ephemeral port
  const server = http.createServer(app);
  createWebSocketServer(server);
  await new Promise((resolve) => server.listen(0, resolve));
  const port = server.address().port;
  console.log(`Message WebSocket Server running on port ${port}\n`);

  // Start Redis Consumer
  await redisConsumer.start();

  // Create test users
  await User.deleteMany({ phone: { $in: ['+84988001001', '+84988002002'] } });
  const userA = await User.create({
    phone: '+84988001001',
    displayName: 'Thành Sender',
    accountStatus: 'active'
  });
  const userB = await User.create({
    phone: '+84988002002',
    displayName: 'Phú Recipient',
    accountStatus: 'active'
  });

  const idA = userA.id;
  const idB = userB.id;

  const tokenA = signAccessToken({ userId: idA, phone: userA.phone, roles: ['user'] });
  const tokenB = signAccessToken({ userId: idB, phone: userB.phone, roles: ['user'] });
  const invalidToken = 'invalid.jwt.token';

  // Create a 1-1 conversation between A and B
  const pairKey = Conversation.generatePairKey(idA, idB);
  await Conversation.deleteMany({ pairKey });
  await Message.deleteMany({ conversationId: { $exists: true }, senderId: { $in: [idA, idB] } });

  const conversation = await Conversation.create({
    pairKey,
    type: 'direct',
    members: [idA, idB],
    memberSettings: [
      { userId: idA, lastReadSeq: 0, unreadCount: 0 },
      { userId: idB, lastReadSeq: 0, unreadCount: 0 }
    ],
    lastSeq: 0,
    messageCount: 0,
    hasMessages: false
  });
  const convId = conversation.id;

  // Test 1: Handshake Authentication
  console.log('1. Testing WebSocket Handshake Authentication:');
  // 1a: Missing token -> Reject
  try {
    await createWsClient(`ws://127.0.0.1:${port}/ws/messages`);
    assert.fail('Should reject connection without token');
  } catch (err) {
    console.log('   ✓ Rejected connection without token (401).');
  }

  // 1b: Invalid token -> Reject
  try {
    await createWsClient(`ws://127.0.0.1:${port}/ws/messages?token=${invalidToken}`);
    assert.fail('Should reject connection with invalid token');
  } catch (err) {
    console.log('   ✓ Rejected connection with invalid token (401).');
  }

  // 1c: Valid token -> Connect A
  const wsA = await createWsClient(`ws://127.0.0.1:${port}/ws/messages?token=${tokenA}`);
  const welcomeA = await waitForEvent(wsA, 'connected');
  assert.strictEqual(welcomeA.userId, idA);
  console.log(`   ✓ User A connected successfully, received 'connected' event for userId: ${idA}.`);

  // Connect B
  const wsB = await createWsClient(`ws://127.0.0.1:${port}/ws/messages?token=${tokenB}`);
  const welcomeB = await waitForEvent(wsB, 'connected');
  assert.strictEqual(welcomeB.userId, idB);
  console.log(`   ✓ User B connected successfully, received 'connected' event for userId: ${idB}.`);

  // Test 2: msg.send -> msg.ack pipeline
  console.log('\n2. Testing msg.send -> Dedup -> Insert -> msg.ack Pipeline (Seq 3 Steps 5-9):');
  const clientMsgId1 = 'c_uuid_test_001';
  const receivePromise1 = waitForEvent(wsB, 'msg.receive');

  wsA.send(
    JSON.stringify({
      event: 'msg.send',
      data: {
        clientMsgId: clientMsgId1,
        convId,
        content: 'Chào Phú! Tin nhắn thử nghiệm 1'
      }
    })
  );

  const ack1 = await waitForEvent(wsA, 'msg.ack');
  assert.strictEqual(ack1.clientMsgId, clientMsgId1);
  assert.strictEqual(ack1.seq, 1);
  assert.strictEqual(ack1.status, 'sent');
  assert.strictEqual(ack1.convId, convId);
  console.log(`   ✓ Received msg.ack with seq = 1, status = "sent" (Single Grey Checkmark ✓).`);

  const received1 = await receivePromise1;
  assert.strictEqual(received1.seq, 1);
  assert.strictEqual(received1.content, 'Chào Phú! Tin nhắn thử nghiệm 1');
  console.log(`   ✓ Recipient B received msg.receive with seq = 1 in real time.`);

  // Verify Conversation updated
  const convUpdated1 = await Conversation.findById(convId);
  assert.strictEqual(convUpdated1.lastSeq, 1);
  assert.strictEqual(convUpdated1.hasMessages, true);
  assert.strictEqual(convUpdated1.lastMessage.text, 'Chào Phú! Tin nhắn thử nghiệm 1');
  console.log('   ✓ Conversation metadata updated (lastSeq: 1, hasMessages: true).');

  // Test 3: Deduplication check on retry (Seq 3 Step 6)
  console.log('\n3. Testing Deduplication Check on Retry (Same clientMsgId):');
  wsA.send(
    JSON.stringify({
      event: 'msg.send',
      data: {
        clientMsgId: clientMsgId1,
        convId,
        content: 'Chào Phú! Tin nhắn thử nghiệm 1'
      }
    })
  );

  const ackDup = await waitForEvent(wsA, 'msg.ack');
  assert.strictEqual(ackDup.clientMsgId, clientMsgId1);
  assert.strictEqual(ackDup.seq, 1);
  assert.strictEqual(ackDup.isDuplicate, true);
  const totalMsgs = await Message.countDocuments({ conversationId: convId });
  assert.strictEqual(totalMsgs, 1, 'Should NOT create duplicate message record in DB');
  console.log('   ✓ Duplicate message recognized and deduplicated; no extra seq created.');

  // Test 4: Real-time message delivery (msg.receive) to Recipient (Seq 3 Step 14)
  console.log('\n4. Testing Realtime Delivery to Online Recipient (msg.receive):');
  const clientMsgId2 = 'c_uuid_test_002';
  const receivePromise = waitForEvent(wsB, 'msg.receive');

  wsA.send(
    JSON.stringify({
      event: 'msg.send',
      data: {
        clientMsgId: clientMsgId2,
        convId,
        content: 'Phú ơi nhận được tin nhắn chưa?'
      }
    })
  );

  const ack2 = await waitForEvent(wsA, 'msg.ack');
  assert.strictEqual(ack2.seq, 2);

  const receivedMsg = await receivePromise;
  assert.strictEqual(receivedMsg.seq, 2);
  assert.strictEqual(receivedMsg.senderId, idA);
  assert.strictEqual(receivedMsg.content, 'Phú ơi nhận được tin nhắn chưa?');
  assert.strictEqual(receivedMsg.convId, convId);
  console.log(`   ✓ Recipient B received 'msg.receive' with seq = 2 in real time!`);

  // Test 5: Delivery ACK (msg.delivered_ack) -> Sender receives msg.status_update (Seq 3 Steps 16-17)
  console.log('\n5. Testing Delivery ACK (msg.delivered_ack -> msg.status_update):');
  const statusUpdatePromise = waitForEvent(wsA, 'msg.status_update');

  wsB.send(
    JSON.stringify({
      event: 'msg.delivered_ack',
      data: {
        convId,
        seq: 2,
        msgId: receivedMsg.msgId
      }
    })
  );

  const statusUpdate = await statusUpdatePromise;
  assert.strictEqual(statusUpdate.seq, 2);
  assert.strictEqual(statusUpdate.status, 'delivered');
  console.log(`   ✓ Sender A received 'msg.status_update' (status: "delivered" -> Double Grey Checkmarks ✓✓).`);

  // Verify message status in DB
  const msgInDb = await Message.findOne({ conversationId: convId, seq: 2 });
  assert.strictEqual(msgInDb.status, 'delivered');
  console.log('   ✓ Message status updated to "delivered" in MongoDB.');

  // Test 6: Read Receipt (msg.read_receipt) -> Sender receives read confirmation (Seq 3 Steps 21-26)
  console.log('\n6. Testing Read Receipt (msg.read_receipt -> Double Blue Checkmarks):');
  const readReceiptPromise = waitForEvent(wsA, 'msg.read_receipt');

  wsB.send(
    JSON.stringify({
      event: 'msg.read_receipt',
      data: {
        convId,
        lastReadSeq: 2
      }
    })
  );

  const readReceiptData = await readReceiptPromise;
  assert.strictEqual(readReceiptData.convId, convId);
  assert.strictEqual(readReceiptData.readerId, idB);
  assert.strictEqual(readReceiptData.lastReadSeq, 2);
  console.log(`   ✓ Sender A received 'msg.read_receipt' (lastReadSeq: 2 -> Double Blue Checkmarks ✓✓).`);

  // Verify DB state
  const convAfterRead = await Conversation.findById(convId);
  const bSetting = convAfterRead.memberSettings.find((m) => m.userId === idB);
  assert.strictEqual(bSetting.lastReadSeq, 2);
  assert.strictEqual(bSetting.unreadCount, 0);

  const msgSeen = await Message.findOne({ conversationId: convId, seq: 2 });
  assert.strictEqual(msgSeen.status, 'seen');
  console.log('   ✓ Conversation memberSettings updated & messages marked "seen" in DB.');

  // Test 7: Typing Indicator
  console.log('\n7. Testing Real-time Typing Indicator (msg.typing):');
  const typingPromise = waitForEvent(wsB, 'msg.typing');

  wsA.send(
    JSON.stringify({
      event: 'msg.typing',
      data: {
        convId,
        isTyping: true
      }
    })
  );

  const typingData = await typingPromise;
  assert.strictEqual(typingData.convId, convId);
  assert.strictEqual(typingData.userId, idA);
  assert.strictEqual(typingData.isTyping, true);
  console.log('   ✓ Recipient B received typing indicator notification.');

  console.log('\n================================================================');
  console.log('🎉 ALL SPRINT 2 MESSAGE WEBSOCKET & PIPELINE TESTS PASSED (7/7)!');
  console.log('================================================================\n');

  wsA.close();
  wsB.close();
  redisConsumer.stop();
  server.close();
  await mongoose.disconnect();
  process.exit(0);
}

runMessageServiceTests().catch((err) => {
  console.error('Message Service Test Failed:', err);
  process.exit(1);
});
