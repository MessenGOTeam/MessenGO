package com.mesengo.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
// data/local/db/entity/CallEntity.kt
@Entity(tableName = "calls")
data class CallEntity(
    @PrimaryKey val callId: String,
    val peerId: String,
    val peerName: String,
    val callType: String,              // audio, video
    val direction: String,             // incoming, outgoing
    val status: String,                // completed, missed, rejected
    val duration: Int = 0,             // seconds
    val startedAt: Long = System.currentTimeMillis()
)