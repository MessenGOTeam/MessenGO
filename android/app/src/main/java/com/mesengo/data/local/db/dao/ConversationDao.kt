package com.mesengo.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mesengo.data.local.db.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastMessageAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE conversationId = :convId LIMIT 1")
    fun getConversationById(convId: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE conversationId = :convId LIMIT 1")
    suspend fun getConversationSync(convId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET lastMessage = :lastMessage, lastMessageAt = :lastMessageAt, lastSeq = :lastSeq WHERE conversationId = :convId")
    suspend fun updateLastMessage(convId: String, lastMessage: String, lastMessageAt: Long, lastSeq: Long)

    @Query("UPDATE conversations SET lastReadSeq = :lastReadSeq WHERE conversationId = :convId")
    suspend fun updateLastReadSeq(convId: String, lastReadSeq: Long)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE conversationId = :convId")
    suspend fun clearUnreadCount(convId: String)
}
