package com.mesengo.domain.repository

import com.mesengo.data.local.db.entity.ContactEntity
import com.mesengo.data.remote.api.SearchUserItem
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getContacts(): Flow<List<ContactEntity>>
    fun getFriends(): Flow<List<ContactEntity>>
    fun getPendingRequests(): Flow<List<ContactEntity>>
    fun getOutgoingRequests(): Flow<List<ContactEntity>>
    suspend fun searchUserByPhone(phone: String): Result<SearchUserItem>
    suspend fun sendFriendRequest(targetUser: SearchUserItem): Result<String>
    suspend fun cancelFriendRequest(contactUserId: String): Result<Unit>
    suspend fun acceptFriendRequest(contactUserId: String): Result<Unit>
    suspend fun rejectFriendRequest(contactUserId: String): Result<Unit>
    suspend fun blockUser(contactUserId: String): Result<Unit>
}
