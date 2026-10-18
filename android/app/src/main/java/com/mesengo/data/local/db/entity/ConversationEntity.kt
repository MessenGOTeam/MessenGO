package com.mesengo.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val conversationId: String,
    val peerUserId: String,
    val peerDisplayName: String,
    val peerAvatarUrl: String? = null,
    val lastMessage: String? = null,
    val lastMessageAt: Long? = null,
    val lastSeq: Long = 0,
    val lastReadSeq: Long = 0,
    val unreadCount: Int = 0
)