package com.mesengo.data.remote.websocket

import android.util.Log
import com.mesengo.BuildConfig
import com.mesengo.data.local.prefs.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager
) {
    private val tag = "MessageWebSocket"
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _events = MutableSharedFlow<WebSocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()

    fun connect() {
        if (isConnected || webSocket != null) return

        val token = tokenManager.getAccessToken() ?: return
        val wsUrl = getWebSocketUrl(token)

        Log.d(tag, "Connecting to WebSocket: $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(tag, "WebSocket connection opened successfully.")
                isConnected = true
                scope.launch {
                    _events.emit(WebSocketEvent.ConnectionState(true))
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(tag, "Received WebSocket frame: $text")
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket closing: $code / $reason")
                isConnected = false
                scope.launch {
                    _events.emit(WebSocketEvent.ConnectionState(false))
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(tag, "WebSocket failure: ${t.localizedMessage}")
                isConnected = false
                this@MessageWebSocketClient.webSocket = null
                scope.launch {
                    _events.emit(WebSocketEvent.ConnectionState(false))
                }
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        isConnected = false
    }

    fun sendMessage(
        clientMsgId: String,
        conversationId: String,
        senderId: String,
        content: String
    ): Boolean {
        if (!isConnected || webSocket == null) return false

        return try {
            val payload = JSONObject().apply {
                put("type", "send_message")
                put("clientMsgId", clientMsgId)
                put("conversationId", conversationId)
                put("senderId", senderId)
                put("content", content)
                put("messageType", "text")
                put("timestamp", System.currentTimeMillis())
            }
            webSocket?.send(payload.toString()) == true
        } catch (e: Exception) {
            Log.e(tag, "Failed to send WebSocket message frame", e)
            false
        }
    }

    fun sendReadReceipt(conversationId: String, seq: Long): Boolean {
        if (!isConnected || webSocket == null) return false

        return try {
            val payload = JSONObject().apply {
                put("type", "msg.read")
                put("conversationId", conversationId)
                put("seq", seq)
            }
            webSocket?.send(payload.toString()) == true
        } catch (e: Exception) {
            false
        }
    }

    private fun handleIncomingMessage(jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            val type = json.optString("type")

            val event: WebSocketEvent? = when (type) {
                "msg.ack", "message.ack" -> {
                    WebSocketEvent.MsgAck(
                        clientMsgId = json.getString("clientMsgId"),
                        seq = json.optLong("seq", 0L),
                        status = json.optString("status", "sent"),
                        timestamp = json.optLong("timestamp", System.currentTimeMillis())
                    )
                }
                "new_message", "message.created" -> {
                    WebSocketEvent.NewMessage(
                        clientMsgId = json.optString("clientMsgId", ""),
                        conversationId = json.getString("conversationId"),
                        senderId = json.getString("senderId"),
                        content = json.getString("content"),
                        type = json.optString("messageType", "text"),
                        seq = json.optLong("seq", 0L),
                        createdAt = json.optLong("createdAt", System.currentTimeMillis())
                    )
                }
                "msg.delivered" -> {
                    WebSocketEvent.MsgDelivered(
                        clientMsgId = json.getString("clientMsgId"),
                        conversationId = json.getString("conversationId"),
                        seq = json.optLong("seq", 0L)
                    )
                }
                "msg.read" -> {
                    WebSocketEvent.MsgRead(
                        conversationId = json.getString("conversationId"),
                        readerId = json.getString("readerId"),
                        lastReadSeq = json.optLong("lastReadSeq", 0L)
                    )
                }
                else -> null
            }

            if (event != null) {
                scope.launch { _events.emit(event) }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing incoming WebSocket frame", e)
        }
    }

    private fun getWebSocketUrl(token: String): String {
        val base = BuildConfig.BASE_URL.trimEnd('/')
        val wsBase = if (base.startsWith("https://")) {
            base.replace("https://", "wss://")
        } else {
            base.replace("http://", "ws://")
        }
        return "$wsBase/ws/messages?token=$token"
    }
}
