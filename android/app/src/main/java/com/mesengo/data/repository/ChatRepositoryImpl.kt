package com.mesengo.data.repository

import com.mesengo.data.local.db.dao.ConversationDao
import com.mesengo.data.local.db.dao.MessageDao
import com.mesengo.data.local.db.dao.OutboxDao
import com.mesengo.data.local.db.entity.ConversationEntity
import com.mesengo.data.local.db.entity.MessageEntity
import com.mesengo.data.local.db.entity.OutboxEntity
import com.mesengo.data.local.prefs.TokenManager
import com.mesengo.data.remote.outbox.OutboxDispatcher
import com.mesengo.data.remote.websocket.MessageWebSocketClient
import com.mesengo.data.remote.websocket.WebSocketEvent
import com.mesengo.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val outboxDao: OutboxDao,
    private val outboxDispatcher: OutboxDispatcher,
    private val webSocketClient: MessageWebSocketClient,
    private val tokenManager: TokenManager
) : ChatRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        observeWebSocketEvents()
        webSocketClient.connect()
    }

    private fun observeWebSocketEvents() {
        scope.launch {
            webSocketClient.events.collect { event ->
                when (event) {
                    is WebSocketEvent.MsgAck -> {
                        // Server confirmed message received (Seq 3 Phase 2)
                        messageDao.updateStatusAndSeq(event.clientMsgId, "sent", event.seq)
                        outboxDao.deleteOutbox(event.clientMsgId)
                    }
                    is WebSocketEvent.NewMessage -> {
                        // Incoming message from friend (Seq 3 Phase 3)
                        val incoming = MessageEntity(
                            clientMsgId = event.clientMsgId.ifEmpty { UUID.randomUUID().toString() },
                            conversationId = event.conversationId,
                            senderId = event.senderId,
                            content = event.content,
                            type = event.type,
                            seq = event.seq,
                            status = "delivered",
                            createdAt = event.createdAt
                        )
                        messageDao.insertMessage(incoming)
                        conversationDao.updateLastMessage(
                            convId = event.conversationId,
                            lastMessage = event.content,
                            lastMessageAt = event.createdAt,
                            lastSeq = event.seq
                        )
                    }
                    is WebSocketEvent.MsgRead -> {
                        // Peer viewed up to lastReadSeq (Seq 3 Phase 4 / US-03)
                        conversationDao.updateLastReadSeq(event.conversationId, event.lastReadSeq)
                    }
                    is WebSocketEvent.MsgDelivered -> {
                        messageDao.updateStatus(event.clientMsgId, "delivered")
                    }
                    else -> Unit
                }
            }
        }
    }

    override fun getConversations(): Flow<List<ConversationEntity>> {
        return conversationDao.getAllConversations()
    }

    override fun getConversation(conversationId: String): Flow<ConversationEntity?> {
        return conversationDao.getConversationById(conversationId)
    }

    override fun getMessages(conversationId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesByConversation(conversationId)
    }

    override suspend fun sendMessage(
        conversationId: String,
        peerUserId: String,
        content: String
    ): Result<MessageEntity> {
        return try {
            val currentUserId = tokenManager.getUserId() ?: "me"
            val clientMsgId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            // 1. Optimistic Local Room Insertion with 'pending' status (FR-MSG-02)
            val pendingMessage = MessageEntity(
                clientMsgId = clientMsgId,
                conversationId = conversationId,
                senderId = currentUserId,
                content = content.trim(),
                type = "text",
                status = "pending",
                createdAt = now
            )
            messageDao.insertMessage(pendingMessage)

            // 2. Enqueue into Outbox Queue (FR-MSG-02)
            val outboxItem = OutboxEntity(
                clientMsgId = clientMsgId,
                conversationId = conversationId,
                payload = content.trim(),
                createdAt = now
            )
            outboxDao.insertOutbox(outboxItem)

            // 3. Update Conversation snippet immediately
            val currentSeq = (messageDao.getMaxSeq(conversationId) ?: 0L) + 1L
            conversationDao.updateLastMessage(
                convId = conversationId,
                lastMessage = content.trim(),
                lastMessageAt = now,
                lastSeq = currentSeq
            )

            // 4. Trigger Outbox Dispatcher
            outboxDispatcher.dispatchPendingMessages()

            Result.success(pendingMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(conversationId: String, seq: Long): Result<Unit> {
        return try {
            conversationDao.clearUnreadCount(conversationId)
            webSocketClient.sendReadReceipt(conversationId, seq)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
