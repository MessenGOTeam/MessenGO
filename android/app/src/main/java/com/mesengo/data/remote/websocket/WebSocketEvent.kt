package com.mesengo.data.remote.websocket

sealed class WebSocketEvent {
    data class ConnectionState(val isConnected: Boolean) : WebSocketEvent()

    data class MsgAck(
        val clientMsgId: String,
        val seq: Long,
        val status: String = "sent",
        val timestamp: Long = System.currentTimeMillis()
    ) : WebSocketEvent()

    data class NewMessage(
        val clientMsgId: String,
        val conversationId: String,
        val senderId: String,
        val content: String,
        val type: String = "text",
        val seq: Long,
        val createdAt: Long = System.currentTimeMillis()
    ) : WebSocketEvent()

    data class MsgDelivered(
        val clientMsgId: String,
        val conversationId: String,
        val seq: Long
    ) : WebSocketEvent()

    data class MsgRead(
        val conversationId: String,
        val readerId: String,
        val lastReadSeq: Long
    ) : WebSocketEvent()

    data class Error(val message: String) : WebSocketEvent()
}
