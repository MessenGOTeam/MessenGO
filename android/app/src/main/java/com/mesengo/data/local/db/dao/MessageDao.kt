package com.mesengo.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mesengo.data.local.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY createdAt DESC")
    fun getMessagesByConversation(convId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE clientMsgId = :clientMsgId")
    suspend fun updateStatus(clientMsgId: String, status: String)

    @Query("UPDATE messages SET status = :status, seq = :seq WHERE clientMsgId = :clientMsgId")
    suspend fun updateStatusAndSeq(clientMsgId: String, status: String, seq: Long)

    @Query("SELECT MAX(seq) FROM messages WHERE conversationId = :convId")
    suspend fun getMaxSeq(convId: String): Long?
}
