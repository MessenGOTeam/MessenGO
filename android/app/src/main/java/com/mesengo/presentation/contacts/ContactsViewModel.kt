package com.mesengo.presentation.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mesengo.data.local.db.entity.ContactEntity
import com.mesengo.data.remote.api.SearchUserItem
import com.mesengo.domain.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository
) : ViewModel() {

    val allContacts: StateFlow<List<ContactEntity>> = contactRepository.getContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friends: StateFlow<List<ContactEntity>> = contactRepository.getFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Incoming requests (someone sent to current user)
    val pendingRequests: StateFlow<List<ContactEntity>> = contactRepository.getPendingRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Outgoing requests (current user sent to someone else)
    val outgoingRequests: StateFlow<List<ContactEntity>> = contactRepository.getOutgoingRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun searchUser(phone: String, onResult: (SearchUserItem?, String?) -> Unit) {
        viewModelScope.launch {
            val result = contactRepository.searchUserByPhone(phone)
            if (result.isSuccess) {
                onResult(result.getOrNull(), null)
            } else {
                onResult(null, result.exceptionOrNull()?.message ?: "Không tìm thấy người dùng.")
            }
        }
    }

    fun sendFriendRequest(targetUser: SearchUserItem, onComplete: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            val result = contactRepository.sendFriendRequest(targetUser)
            if (result.isSuccess) {
                onComplete?.invoke(true, result.getOrNull())
            } else {
                onComplete?.invoke(false, result.exceptionOrNull()?.message)
            }
        }
    }

    fun cancelFriendRequest(contactUserId: String) {
        viewModelScope.launch {
            contactRepository.cancelFriendRequest(contactUserId)
        }
    }

    fun acceptFriendRequest(contactUserId: String) {
        viewModelScope.launch {
            contactRepository.acceptFriendRequest(contactUserId)
        }
    }

    fun rejectFriendRequest(contactUserId: String) {
        viewModelScope.launch {
            contactRepository.rejectFriendRequest(contactUserId)
        }
    }

    fun blockUser(contactUserId: String) {
        viewModelScope.launch {
            contactRepository.blockUser(contactUserId)
        }
    }
}
