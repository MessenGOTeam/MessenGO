package com.mesengo.data.repository

import com.mesengo.data.local.db.dao.ContactDao
import com.mesengo.data.local.db.dao.ConversationDao
import com.mesengo.data.local.db.entity.ContactEntity
import com.mesengo.data.local.db.entity.ConversationEntity
import com.mesengo.data.local.prefs.TokenManager
import com.mesengo.data.remote.api.ContactApi
import com.mesengo.data.remote.api.ContactRequestDto
import com.mesengo.data.remote.api.ContactRespondDto
import com.mesengo.data.remote.api.SearchUserItem
import com.mesengo.domain.repository.ContactRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val contactApi: ContactApi,
    private val contactDao: ContactDao,
    private val conversationDao: ConversationDao,
    private val tokenManager: TokenManager
) : ContactRepository {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                contactDao.migrateLegacyPendingStatus()
                syncContactsFromServer()
            } catch (_: Exception) {}
        }
    }

    override fun getContacts(): Flow<List<ContactEntity>> {
        return contactDao.getAllContacts()
    }

    override fun getFriends(): Flow<List<ContactEntity>> {
        return contactDao.getContactsByStatus("accepted")
    }

    override fun getPendingRequests(): Flow<List<ContactEntity>> {
        return contactDao.getIncomingRequests()
    }

    override fun getOutgoingRequests(): Flow<List<ContactEntity>> {
        return contactDao.getOutgoingRequests()
    }

    override suspend fun searchUserByPhone(phone: String): Result<SearchUserItem> {
        val myPhone = tokenManager.getPhone()
        val myUserId = tokenManager.getUserId()

        return try {
            val response = contactApi.searchByPhone(phone)
            if (response.isSuccessful && response.body()?.data?.user != null) {
                val user = response.body()!!.data!!.user
                val relationship = response.body()!!.data?.relationship

                val isSelf = relationship?.isSelf == true ||
                    (myUserId != null && myUserId == user._id) ||
                    (myPhone != null && isSamePhone(myPhone, user.phone)) ||
                    (myPhone != null && isSamePhone(myPhone, phone))

                Result.success(user.copy(isSelf = isSelf))
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Không tìm thấy người dùng với số điện thoại này."
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            // Real check enforcement: no fake mock users for arbitrary phone numbers
            val errorMsg = if (e is ConnectException || e is SocketTimeoutException || e is UnknownHostException) {
                "Không thể kết nối máy chủ MessenGO (Backend chưa bật). Vui lòng thử lại sau."
            } else {
                e.localizedMessage ?: "Lỗi kết nối khi tìm kiếm bạn bè."
            }
            Result.failure(Exception(errorMsg))
        }
    }

    override suspend fun sendFriendRequest(targetUser: SearchUserItem): Result<String> {
        return try {
            val myPhone = tokenManager.getPhone()
            val myUserId = tokenManager.getUserId()

            if (targetUser.isSelf ||
                (myUserId != null && myUserId == targetUser._id) ||
                (myPhone != null && isSamePhone(myPhone, targetUser.phone))
            ) {
                return Result.failure(Exception("Bạn không thể gửi lời mời kết bạn cho chính mình!"))
            }

            // Sync with backend API (non-blocking for offline-first Room DB)
            try {
                contactApi.requestContact(
                    ContactRequestDto(
                        peerId = targetUser._id,
                        contactPhone = targetUser.phone,
                        message = "Xin chào, mình kết bạn nhé!"
                    )
                )
            } catch (_: Exception) {}

            // Save locally to Room DB with status 'pending_sent' (OUTGOING request)
            val contact = ContactEntity(
                contactUserId = targetUser._id,
                displayName = targetUser.displayName,
                phone = targetUser.phone,
                avatarUrl = null,
                status = "pending_sent"
            )
            contactDao.insertContact(contact)
            Result.success("Đã gửi lời mời kết bạn tới ${targetUser.displayName}!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelFriendRequest(contactUserId: String): Result<Unit> {
        return try {
            contactDao.deleteContact(contactUserId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acceptFriendRequest(contactUserId: String): Result<Unit> {
        return try {
            val contact = contactDao.getContactById(contactUserId)
            if (contact != null) {
                contactDao.updateContactStatus(contactUserId, "accepted")

                // Auto-create initial conversation in Room DB so user can chat right away (Seq 2)
                val convId = "conv_${contactUserId}"
                val conversation = ConversationEntity(
                    conversationId = convId,
                    peerUserId = contact.contactUserId,
                    peerDisplayName = contact.displayName,
                    peerAvatarUrl = contact.avatarUrl,
                    lastMessage = "Hai bạn đã trở thành bạn bè. Hãy bắt đầu trò chuyện!",
                    lastMessageAt = System.currentTimeMillis(),
                    lastSeq = 0,
                    lastReadSeq = 0,
                    unreadCount = 0
                )
                conversationDao.insertConversation(conversation)
            }

            // Sync with backend API
            try {
                contactApi.respondContact(ContactRespondDto(peerId = contactUserId, action = "accept"))
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectFriendRequest(contactUserId: String): Result<Unit> {
        return try {
            contactDao.deleteContact(contactUserId)
            try {
                contactApi.respondContact(ContactRespondDto(peerId = contactUserId, action = "reject"))
            } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun blockUser(contactUserId: String): Result<Unit> {
        return try {
            contactDao.updateContactStatus(contactUserId, "blocked")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncContactsFromServer() {
        try {
            val res = contactApi.getContacts()
            if (res.isSuccessful && res.body()?.data?.contacts != null) {
                val serverContacts = res.body()!!.data!!.contacts!!
                val currentUserId = tokenManager.getUserId()
                val entities = serverContacts.map { sc ->
                    val statusStr = when (sc.status) {
                        "accepted" -> "accepted"
                        "pending" -> if (sc.ownerId == currentUserId) "pending_sent" else "pending_received"
                        "blocked" -> "blocked"
                        else -> sc.status
                    }
                    ContactEntity(
                        contactUserId = if (sc.ownerId == currentUserId) sc.contactUserId else sc.ownerId,
                        displayName = sc.profile?.displayName ?: "Người dùng ${sc.contactUserId.takeLast(4)}",
                        phone = sc.profile?.phone ?: "",
                        avatarUrl = null,
                        status = statusStr
                    )
                }
                if (entities.isNotEmpty()) {
                    contactDao.insertContacts(entities)
                }
            }
        } catch (_: Exception) {}
    }

    private fun isSamePhone(phoneA: String, phoneB: String): Boolean {
        val digitsA = phoneA.filter { it.isDigit() }
        val digitsB = phoneB.filter { it.isDigit() }
        if (digitsA.isEmpty() || digitsB.isEmpty()) return false
        val minLen = minOf(9, digitsA.length, digitsB.length)
        return digitsA.takeLast(minLen) == digitsB.takeLast(minLen)
    }

    private fun parseErrorMessage(rawJson: String?): String? {
        if (rawJson.isNullOrBlank()) return null
        return try {
            val json = JSONObject(rawJson)
            json.optString("message").ifEmpty {
                json.optString("error").ifEmpty { null }
            }
        } catch (e: Exception) {
            null
        }
    }
}
