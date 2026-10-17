package com.mesengo.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

//data/local/db/entity/MessageEntity.kt
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val clientMsgId: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val type: String = "text",
    val mediaId: String? = null,
    val remoteUrl: String? = null,
    val localPath: String? = null,
    val seq: Long? = null,
    val status: String = "pending",
    val createdAt: Long = System.currentTimeMillis()
)

