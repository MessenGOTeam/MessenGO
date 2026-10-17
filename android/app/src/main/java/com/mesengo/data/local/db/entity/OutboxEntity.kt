package com.mesengo.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// data/local/db/entity/OutboxEntity.kt
@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey val clientMsgId: String,
    val conversationId: String,
    val payload: String,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)