package com.mesengo.presentation.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mesengo.data.local.db.entity.ConversationEntity
import com.mesengo.data.local.db.entity.MessageEntity
import com.mesengo.data.local.prefs.TokenManager
import com.mesengo.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    var conversationId: String = ""
        private set

    var peerUserId: String = ""
        private set

    var peerDisplayName: String = ""
        private set

    var inputText by mutableStateOf("")
        private set

    val currentUserId: String
        get() = tokenManager.getUserId() ?: "me"

    private val _conversation = MutableStateFlow<ConversationEntity?>(null)
    val conversation: StateFlow<ConversationEntity?> = _conversation.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    fun initChat(convId: String, peerId: String, peerName: String) {
        conversationId = convId
        peerUserId = peerId
        peerDisplayName = peerName

        viewModelScope.launch {
            chatRepository.getConversation(convId).collect { conv ->
                _conversation.value = conv
            }
        }

        viewModelScope.launch {
            chatRepository.getMessages(convId).collect { msgList ->
                _messages.value = msgList
            }
        }

        // Mark conversation as read on screen entry
        viewModelScope.launch {
            chatRepository.markAsRead(convId, 0L)
        }
    }

    fun onInputChanged(text: String) {
        inputText = text
    }

    fun sendMessage() {
        val textToSend = inputText.trim()
        if (textToSend.isEmpty()) return

        inputText = ""
        viewModelScope.launch {
            chatRepository.sendMessage(
                conversationId = conversationId,
                peerUserId = peerUserId,
                content = textToSend
            )
        }
    }

    /**
     * Deduces whether an outgoing message is Seen:
     * seq <= peerLastReadSeq (Formula from SRS US-03 / Proposal note 2)
     */
    fun isMessageSeen(message: MessageEntity): Boolean {
        val peerLastReadSeq = _conversation.value?.lastReadSeq ?: 0L
        return message.seq != null && message.seq <= peerLastReadSeq && peerLastReadSeq > 0
    }
}
