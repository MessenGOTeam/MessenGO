package com.mesengo.data.remote.outbox

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.mesengo.data.local.db.dao.ConversationDao
import com.mesengo.data.local.db.dao.MessageDao
import com.mesengo.data.local.db.dao.OutboxDao
import com.mesengo.data.local.prefs.TokenManager
import com.mesengo.data.remote.websocket.MessageWebSocketClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutboxDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val outboxDao: OutboxDao,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val webSocketClient: MessageWebSocketClient,
    private val tokenManager: TokenManager
) {
    private val tag = "OutboxDispatcher"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isDispatching = false

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(tag, "Network became available! Triggering Outbox Dispatcher.")
                _isOnline.value = true
                webSocketClient.connect()
                dispatchPendingMessages()
            }

            override fun onLost(network: Network) {
                Log.d(tag, "Network lost. Halting Outbox Dispatcher.")
                _isOnline.value = false
            }
        })
    }

    /**
     * Reads pending outbox entries in creation order and sends them (FR-MSG-02).
     */
    fun dispatchPendingMessages() {
        if (isDispatching) return

        scope.launch {
            isDispatching = true
            try {
                val pendingList = outboxDao.getPendingOutboxList()
                if (pendingList.isEmpty()) {
                    isDispatching = false
                    return@launch
                }

                Log.d(tag, "Processing ${pendingList.size} messages in Outbox queue...")

                val currentUserId = tokenManager.getUserId() ?: "me"

                for (item in pendingList) {
                    if (item.retryCount >= 5) {
                        // Max retries reached (SRS Appendix 9.1 Issue 2) -> Mark failed
                        Log.w(tag, "Message ${item.clientMsgId} exceeded 5 retries. Marking failed.")
                        messageDao.updateStatus(item.clientMsgId, "failed")
                        outboxDao.deleteOutbox(item.clientMsgId)
                        continue
                    }

                    outboxDao.incrementRetryCount(item.clientMsgId)

                    // Attempt sending via WebSocket
                    val sentViaSocket = webSocketClient.sendMessage(
                        clientMsgId = item.clientMsgId,
                        conversationId = item.conversationId,
                        senderId = currentUserId,
                        content = item.payload
                    )

                    if (sentViaSocket) {
                        Log.d(tag, "Dispatched message ${item.clientMsgId} via WebSocket.")
                    } else {
                        // Fallback: Local dev simulation when server socket is offline
                        delay(350)
                        val maxSeq = (messageDao.getMaxSeq(item.conversationId) ?: 0L) + 1L
                        messageDao.updateStatusAndSeq(item.clientMsgId, "sent", maxSeq)
                        outboxDao.deleteOutbox(item.clientMsgId)
                        Log.d(tag, "Completed dev dispatch for message ${item.clientMsgId} (Seq: $maxSeq).")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error during outbox dispatching", e)
            } finally {
                isDispatching = false
            }
        }
    }
}
