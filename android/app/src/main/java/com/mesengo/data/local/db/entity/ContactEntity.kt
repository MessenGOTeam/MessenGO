package com.mesengo.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val contactUserId: String,
    val displayName: String,
    val phone: String,
    val avatarUrl: String? = null,
    val status: String = "accepted" // pending_sent, pending_received, accepted, blocked
)
