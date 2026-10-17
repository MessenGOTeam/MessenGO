package com.mesengo.di

import android.content.Context
import androidx.room.Room
import com.mesengo.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "messengo.db"
        ).build()
    }

    @Provides fun provideMessageDao(db: AppDatabase) = db.messageDao()
    @Provides fun provideOutboxDao(db: AppDatabase) = db.outboxDao()
    @Provides fun provideConversationDao(db: AppDatabase) = db.conversationDao()
    @Provides fun provideContactDao(db: AppDatabase) = db.contactDao()
    @Provides fun provideCallDao(db: AppDatabase) = db.callDao()
}
