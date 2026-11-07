package com.mesengo.di

import com.mesengo.data.repository.AuthRepositoryImpl
import com.mesengo.data.repository.ChatRepositoryImpl
import com.mesengo.data.repository.ContactRepositoryImpl
import com.mesengo.domain.repository.AuthRepository
import com.mesengo.domain.repository.ChatRepository
import com.mesengo.domain.repository.ContactRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindContactRepository(
        contactRepositoryImpl: ContactRepositoryImpl
    ): ContactRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository
}
