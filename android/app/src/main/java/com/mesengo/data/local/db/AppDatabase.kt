package com.mesengo.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mesengo.data.local.db.dao.CallDao
import com.mesengo.data.local.db.dao.ContactDao
import com.mesengo.data.local.db.dao.ConversationDao
import com.mesengo.data.local.db.dao.MessageDao
import com.mesengo.data.local.db.dao.OutboxDao
import com.mesengo.data.local.db.entity.CallEntity
import com.mesengo.data.local.db.entity.ContactEntity
import com.mesengo.data.local.db.entity.ConversationEntity
import com.mesengo.data.local.db.entity.MessageEntity
import com.mesengo.data.local.db.entity.OutboxEntity

@Database(
    entities = [
        MessageEntity::class,
        OutboxEntity::class,
        ConversationEntity::class,
        ContactEntity::class,
        CallEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun outboxDao(): OutboxDao
    abstract fun conversationDao(): ConversationDao
    abstract fun contactDao(): ContactDao
    abstract fun callDao(): CallDao
}
