package com.mesengo.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mesengo.data.local.db.dao.ContactDao
import com.mesengo.data.local.db.dao.ConversationDao
import com.mesengo.data.local.prefs.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MainTab {
    CHATS,
    CONTACTS,
    PROFILE
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    conversationDao: ConversationDao,
    contactDao: ContactDao
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(MainTab.CHATS)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    // Expose all conversations from Room DB
    val conversations: StateFlow<List<com.mesengo.data.local.db.entity.ConversationEntity>> = conversationDao.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expose all contacts from Room DB
    val contacts: StateFlow<List<com.mesengo.data.local.db.entity.ContactEntity>> = contactDao.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculate unread badge count from Room DB conversations
    val totalUnreadCount: StateFlow<Int> = conversationDao.getAllConversations()
        .map { conversations -> conversations.sumOf { it.unreadCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Calculate incoming friend requests badge count from Room DB contacts (only incoming)
    val pendingRequestsCount: StateFlow<Int> = contactDao.getAllContacts()
        .map { contacts -> contacts.count { it.status == "pending_received" || it.status == "pending" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            tokenManager.clearTokens()
            onLoggedOut()
        }
    }
}
