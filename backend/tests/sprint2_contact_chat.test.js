const assert = require('assert');
const http = require('http');
const app = require('../services/chat-service/src/app');
const userApp = require('../services/user-service/src/app');
const Contact = require('../services/chat-service/src/models/Contact');
const User = require('../services/chat-service/src/models/User');
const { signAccessToken, connectMongo, mongoose } = require('../shared');

function makeRequest(server, options, body = null) {
  return new Promise((resolve, reject) => {
    const port = server.address().port;
    const req = http.request(
      {
        hostname: '127.0.0.1',
        port,
        path: options.path,
        method: options.method || 'GET',
        headers: {
          'Content-Type': 'application/json',
          ...(options.headers || {})
        }
      },
      (res) => {
        let rawData = '';
        res.on('data', (chunk) => {
          rawData += chunk;
        });
        res.on('end', () => {
          try {
            const parsed = rawData ? JSON.parse(rawData) : {};
            resolve({ statusCode: res.statusCode, data: parsed });
          } catch (e) {
            resolve({ statusCode: res.statusCode, raw: rawData });
          }
        });
      }
    );

    req.on('error', reject);

    if (body) {
      req.write(typeof body === 'string' ? body : JSON.stringify(body));
    }
    req.end();
  });
}

async function runSprint2ContactTests() {
  console.log('--- Starting Sprint 2 Contact & Block Flow Automated Tests ---\n');

  // Connect to test database
  await connectMongo('messengo_identity_test');

  // Start chat app on ephemeral port
  const server = http.createServer(app);
  await new Promise((resolve) => server.listen(0, resolve));
  const serverPort = server.address().port;
  console.log(`Chat test server running on port ${serverPort}`);

  // Start user app on ephemeral port for search tests
  const userServer = http.createServer(userApp);
  await new Promise((resolve) => userServer.listen(0, resolve));
  const userServerPort = userServer.address().port;
  console.log(`User test server running on port ${userServerPort}\n`);

  // Seed test users
  await User.deleteMany({
    phone: { $in: ['+84911111111', '+84922222222', '+84933333333', '+84944444444'] }
  });

  const userA = await User.create({
    phone: '+84911111111',
    displayName: 'Nguyễn Văn A',
    accountStatus: 'active'
  });
  const userB = await User.create({
    phone: '+84922222222',
    displayName: 'Trần Thị B',
    accountStatus: 'active'
  });
  const userC = await User.create({
    phone: '+84933333333',
    displayName: 'Lê Văn C',
    accountStatus: 'active'
  });
  const userD = await User.create({
    phone: '+84944444444',
    displayName: 'Phạm Thị D',
    accountStatus: 'active'
  });

  const idA = userA.id;
  const idB = userB.id;
  const idC = userC.id;
  const idD = userD.id;

  // Clear previous test contacts
  await Contact.deleteMany({
    $or: [
      { ownerId: { $in: [idA, idB, idC, idD] } },
      { contactUserId: { $in: [idA, idB, idC, idD] } }
    ]
  });

  const tokenA = signAccessToken({ userId: idA, phone: userA.phone, roles: ['user'] });
  const tokenB = signAccessToken({ userId: idB, phone: userB.phone, roles: ['user'] });
  const tokenC = signAccessToken({ userId: idC, phone: userC.phone, roles: ['user'] });
  const tokenD = signAccessToken({ userId: idD, phone: userD.phone, roles: ['user'] });

  // Test 1: Health check
  console.log('1. Testing Contact Service Health Check:');
  const healthRes = await makeRequest(server, { path: '/api/v1/contacts/health' });
  assert.strictEqual(healthRes.statusCode, 200);
  assert.strictEqual(healthRes.data.status, 'ok');
  console.log('   ✓ Health check returned 200 OK.');

  // Test 2: Missing target identifier
  console.log('2. Testing Validation: Missing target identifier:');
  const invalidRes1 = await makeRequest(
    server,
    {
      path: '/api/v1/contacts/request',
      method: 'POST',
      headers: { Authorization: `Bearer ${tokenA}` }
    },
    {}
  );
  assert.strictEqual(invalidRes1.statusCode, 400);
  console.log('   ✓ Rejected request with missing target (400).');

  // Test 3: Self-friending prevention
  console.log('3. Testing Validation: Self-friending rejection:');
  const selfRes = await makeRequest(
    server,
    {
      path: '/api/v1/contacts/request',
      method: 'POST',
      headers: { Authorization: `Bearer ${tokenA}` }
    },
    { peerId: idA }
  );
  assert.strictEqual(selfRes.statusCode, 400);
  assert.strictEqual(selfRes.data.errors?.code, 'CANNOT_FRIEND_SELF');
  console.log('   ✓ Rejected self-friending (400 CANNOT_FRIEND_SELF).');

  // Test 4: Valid friend request (User A -> User B)
  console.log('4. Testing Successful Friend Request (User A -> User B):');
  const reqRes = await makeRequest(
    server,
    {
      path: '/api/v1/contacts/request',
      method: 'POST',
      headers: { Authorization: `Bearer ${tokenA}` }
    },
    {
      peerId: idB,
      message: 'Chào bạn B, mình kết bạn nhé!'
    }
  );
  assert.strictEqual(reqRes.statusCode, 201);
  assert.strictEqual(reqRes.data.success, true);
  assert.strictEqual(reqRes.data.data.status, 'pending');
  assert.strictEqual(reqRes.data.data.contact.ownerId, idA);
  assert.strictEqual(reqRes.data.data.contact.contactUserId, idB);
  console.log('   ✓ Friend request created with status "pending" (201 Created).');

  // Test 5: Duplicate pending request
  console.log('5. Testing Duplicate Request Rejection:');
  const dupRes = await makeRequest(
    server,
    {
      path: '/api/v1/contacts/request',
      method: 'POST',
      headers: { Authorization: `Bearer ${tokenA}` }
    },
    { peerId: idB }
  );
  assert.strictEqual(dupRes.statusCode, 400);
  assert.strictEqual(dupRes.data.errors?.code, 'REQUEST_ALREADY_PENDING');
  console.log('   ✓ Duplicate request rejected (400 REQUEST_ALREADY_PENDING).');

  // Test 6: Mutual Friend Request popup flow (User B -> User A without acceptExisting)
  console.log('6. Testing Mutual Friend Request (User B -> User A): NO auto-accept, returns 409 for popup:');
  const mutualRes = await makeRequest(
    server,
    {
      path: '/api/v1/contacts/request',
      method: 'POST',
      headers: { Authorization: `Bearer ${tokenB}` }
    },
    {
      peerId: idA,
      message: 'Chào A'
    }
  );
  assert.strictEqual(mutualRes.statusCode, 409);
  assert.strictEqual(mutualRes.data.errors?.code, 'INCOMING_REQUEST_PENDING');
  assert.ok(mutualRes.data.errors?.incomingRequest, 'Should contain incomingRequest details for popup');
  assert.strictEqual(mutualRes.data.errors.incomingRequest.senderId, idA);
  assert.strictEqual(mutualRes.data.errors.incomingRequest.message, 'Chào bạn B, mình kết bạn nhé!');
  console.log('   ✓ Correctly returned 409 INCOMING_REQUEST_PENDING with sender info for popup dialog.');

  // Test 7: User B accepts friend request right from popup (acceptExisting = true)
  console.log('7. Testing User B accepts friend request on popup (acceptExisting: true):');
  const acceptOnPopupRes = await makeRequest(
    server,
    {
      path: '/api/v1/contacts/request',
      method: 'POST',
      headers: { Authorization: `Bearer ${tokenB}` }
    },
    {
      peerId: idA,
      acceptExisting: true
    }
  );
  assert.strictEqual(acceptOnPopupRes.statusCode, 200);
  assert.strictEqual(acceptOnPopupRes.data.data.status, 'accepted');
  assert.strictEqual(acceptOnPopupRes.data.data.isMutualAccept, true);
  console.log('   ✓ Friend request accepted on popup, bidirectional relationship established (200 OK).');

  // Test 8: Trying to add when already friends
  console.log('8. Testing Request when Already Friends:');
  const alreadyFriendsRes = await makeRequest(
    server,
    {
      path: '/api/v1/contacts/request',
      method: 'POST',
      headers: { Authorization: `Bearer ${tokenA}` }
    },
    { peerId: idB }
  );
  assert.strictEqual(alreadyFriendsRes.statusCode, 400);
  assert.strictEqual(alreadyFriendsRes.data.errors?.code, 'ALREADY_FRIENDS');
  console.log('   ✓ Request blocked because both are already friends (400 ALREADY_FRIENDS).');

  // Test 9: Respond to friend request (POST /api/v1/contacts/respond)
  console.log('9. Testing Respond Endpoint (User C requests User D -> User D accepts):');
  // C sends to D
  await makeRequest(
    server,
    {
      path: '/api/v1/contacts/request',
      method: 'POST',
      headers: { Authorization: `Bearer ${tokenC}` }
    },
    { peerId: idD, message: 'Kết bạn nha D' }
  );
  // D accepts via /respond
  const respondRes = await makeRequest(
    server,
    {
      path: '/api/v1/contacts/respond',
      method: 'POST',
      headers: { Authorization: `Bearer ${tokenD}` }
    },
    { peerId: idC, action: 'accept' }
  );
  assert.strictEqual(respondRes.statusCode, 200);
  assert.strictEqual(respondRes.data.data.status, 'accepted');
  console.log('   ✓ /respond endpoint accepted request successfully (200 OK).');

  // Test 10: Block Modal with various blockTypes (FR-CON-04)
  console.log('10. Testing Block Contact with Modal Types (messages_only, messages_and_calls, all):');
  // Invalid block type
  const invalidBlock = await makeRequest(
    server,
    {
      path: '/api/v1/contacts/block',
      method: 'POST',
      headers: { Authorization: `Bearer ${tokenA}` }
    },
    { peerId: idB, blockType: 'invalid_type' }
  );
  assert.strictEqual(invalidBlock.statusCode, 400);
  assert.strictEqual(invalidBlock.data.errors?.code, 'INVALID_BLOCK_TYPE');
  console.log('   ✓ Invalid blockType rejected (400 INVALID_BLOCK_TYPE).');

  // Cannot block self
  const blockSelf = await makeRequest(
    server,
    {
      path: '/api/v1/contacts/block',
      method: 'POST',
      headers: { Authorization: `Bearer ${tokenA}` }
    },
    { peerId: idA, blockType: 'all' }
  );
  assert.strictEqual(blockSelf.statusCode, 400);
  assert.strictEqual(blockSelf.data.errors?.code, 'CANNOT_BLOCK_SELF');
  console.log('   ✓ Cannot block self rejected (400 CANNOT_BLOCK_SELF).');

  // User A blocks User B with blockType: 'messages_only'
  const blockRes = await makeRequest(
    server,
    {
      path: '/api/v1/contacts/block',
      method: 'POST',
      headers: { Authorization: `Bearer ${tokenA}` }
    },
    { peerId: idB, blockType: 'messages_only' }
  );
  assert.strictEqual(blockRes.statusCode, 200);
  assert.strictEqual(blockRes.data.data.status, 'blocked');
  assert.strictEqual(blockRes.data.data.blockType, 'messages_only');
  console.log('   ✓ User A blocked User B with blockType: "messages_only" (200 OK).');

  // Test 11: Invisible to blocked user in Search & Add Friend
  console.log('11. Testing Invisible User Behavior for Blocked Requester:');
  // B searches for A's phone -> MUST BE INVISIBLE (404 USER_NOT_FOUND)
  const searchBlockedRes = await makeRequest(
    userServer,
    {
      path: `/api/v1/users/search?phone=${encodeURIComponent(userA.phone)}`,
      method: 'GET',
      headers: { Authorization: `Bearer ${tokenB}` }
    }
  );
  assert.strictEqual(searchBlockedRes.statusCode, 404);
  assert.strictEqual(searchBlockedRes.data.errors?.code, 'USER_NOT_FOUND');
  console.log('   ✓ B searches A by phone -> Returns 404 USER_NOT_FOUND (completely invisible!).');

  // B tries to send friend request to A -> MUST RETURN 404 USER_NOT_FOUND
  const reqBlockedRes = await makeRequest(
    server,
    {
      path: '/api/v1/contacts/request',
      method: 'POST',
      headers: { Authorization: `Bearer ${tokenB}` }
    },
    { peerId: idA }
  );
  assert.strictEqual(reqBlockedRes.statusCode, 404);
  assert.strictEqual(reqBlockedRes.data.errors?.code, 'USER_NOT_FOUND');
  console.log('   ✓ B tries to add A as friend -> Returns 404 USER_NOT_FOUND (invisible!).');

  // A tries to send friend request to B (whom A blocked) -> 400 YOU_HAVE_BLOCKED_USER
  const aTriesAddB = await makeRequest(
    server,
    {
      path: '/api/v1/contacts/request',
      method: 'POST',
      headers: { Authorization: `Bearer ${tokenA}` }
    },
    { peerId: idB }
  );
  assert.strictEqual(aTriesAddB.statusCode, 400);
  assert.strictEqual(aTriesAddB.data.errors?.code, 'YOU_HAVE_BLOCKED_USER');
  console.log('   ✓ A tries to add B -> Returns 400 YOU_HAVE_BLOCKED_USER (must unblock first).');

  // Test 12: Unblock Contact
  console.log('12. Testing Unblock Contact:');
  const unblockRes = await makeRequest(
    server,
    {
      path: '/api/v1/contacts/unblock',
      method: 'POST',
      headers: { Authorization: `Bearer ${tokenA}` }
    },
    { peerId: idB }
  );
  assert.strictEqual(unblockRes.statusCode, 200);
  assert.strictEqual(unblockRes.data.data.status, 'unblocked');
  console.log('   ✓ Unblocked successfully (200 OK).');

  // B can now search A again!
  const searchAfterUnblock = await makeRequest(
    userServer,
    {
      path: `/api/v1/users/search?phone=${encodeURIComponent(userA.phone)}`,
      method: 'GET',
      headers: { Authorization: `Bearer ${tokenB}` }
    }
  );
  assert.strictEqual(searchAfterUnblock.statusCode, 200);
  assert.strictEqual(searchAfterUnblock.data.data.user.phone, userA.phone);
  console.log('   ✓ B can now search A again after being unblocked.');

  // Test 13: List Contacts
  console.log('13. Testing Get Contacts List:');
  const listFriends = await makeRequest(
    server,
    {
      path: '/api/v1/contacts?type=friends',
      method: 'GET',
      headers: { Authorization: `Bearer ${tokenC}` }
    }
  );
  assert.strictEqual(listFriends.statusCode, 200);
  assert.strictEqual(listFriends.data.data.type, 'friends');
  assert.strictEqual(listFriends.data.data.count, 1);
  console.log('   ✓ /contacts?type=friends returned correct count (200 OK).');

  console.log('\n======================================================');
  console.log('🎉 ALL SPRINT 2 CONTACT & BLOCK TESTS PASSED (13/13)!');
  console.log('======================================================\n');

  server.close();
  userServer.close();
  await mongoose.disconnect();
  process.exit(0);
}

runSprint2ContactTests().catch((err) => {
  console.error('Test failed with error:', err);
  process.exit(1);
});
