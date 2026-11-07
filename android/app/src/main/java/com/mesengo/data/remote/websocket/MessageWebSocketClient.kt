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
            val dataObj = JSONObject().apply {
                put("clientMsgId", clientMsgId)
                put("convId", conversationId)
                put("conversationId", conversationId)
                put("senderId", senderId)
                put("content", content)
                put("type", "text")
            }
            val payload = JSONObject().apply {
                put("event", "msg.send")
                put("data", dataObj)
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
            val dataObj = JSONObject().apply {
                put("convId", conversationId)
                put("conversationId", conversationId)
                put("lastReadSeq", seq)
            }
            val payload = JSONObject().apply {
                put("event", "msg.read_receipt")
                put("data", dataObj)
            }
            webSocket?.send(payload.toString()) == true
        } catch (e: Exception) {
            false
        }
    }

    fun sendDeliveredAck(conversationId: String, seq: Long): Boolean {
        if (!isConnected || webSocket == null) return false

        return try {
            val dataObj = JSONObject().apply {
                put("convId", conversationId)
                put("conversationId", conversationId)
                put("seq", seq)
            }
            val payload = JSONObject().apply {
                put("event", "msg.delivered_ack")
                put("data", dataObj)
            }
            webSocket?.send(payload.toString()) == true
        } catch (e: Exception) {
            false
        }
    }

    private fun handleIncomingMessage(jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            val eventName = if (json.has("event")) json.getString("event") else json.optString("type")
            val data = json.optJSONObject("data") ?: json

            val event: WebSocketEvent? = when (eventName) {
                "msg.ack", "message.ack" -> {
                    WebSocketEvent.MsgAck(
                        clientMsgId = data.getString("clientMsgId"),
                        seq = data.optLong("seq", 0L),
                        status = data.optString("status", "sent"),
                        timestamp = data.optLong("timestamp", System.currentTimeMillis())
                    )
                }
                "msg.receive", "new_message", "message.created" -> {
                    val convId = if (data.has("convId")) data.getString("convId") else data.optString("conversationId", "")
                    val seq = data.optLong("seq", 0L)
                    if (seq > 0L && convId.isNotEmpty()) {
                        sendDeliveredAck(convId, seq)
                    }
                    WebSocketEvent.NewMessage(
                        clientMsgId = data.optString("clientMsgId", ""),
                        conversationId = convId,
                        senderId = data.optString("senderId", ""),
                        content = data.optString("content", ""),
                        type = data.optString("type", data.optString("messageType", "text")),
                        seq = seq,
                        createdAt = data.optLong("createdAt", System.currentTimeMillis())
                    )
                }
                "msg.status_update", "msg.delivered" -> {
                    val convId = if (data.has("convId")) data.getString("convId") else data.optString("conversationId", "")
                    WebSocketEvent.MsgDelivered(
                        clientMsgId = data.optString("clientMsgId", ""),
                        conversationId = convId,
                        seq = data.optLong("seq", 0L)
                    )
                }
                "msg.seen", "msg.read_receipt", "msg.read" -> {
                    val convId = if (data.has("convId")) data.getString("convId") else data.optString("conversationId", "")
                    WebSocketEvent.MsgRead(
                        conversationId = convId,
                        readerId = data.optString("readerId", ""),
                        lastReadSeq = data.optLong("lastReadSeq", 0L)
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
