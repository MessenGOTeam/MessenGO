package com.mesengo.domain.repository

import com.mesengo.data.local.db.entity.ConversationEntity
import com.mesengo.data.local.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getConversations(): Flow<List<ConversationEntity>>
    fun getConversation(conversationId: String): Flow<ConversationEntity?>
    fun getMessages(conversationId: String): Flow<List<MessageEntity>>
    suspend fun sendMessage(conversationId: String, peerUserId: String, content: String): Result<MessageEntity>
    suspend fun markAsRead(conversationId: String, seq: Long): Result<Unit>
}
