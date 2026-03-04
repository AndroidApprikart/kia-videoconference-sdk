package com.app.vc.websocketconnection

import android.util.Log
import okhttp3.*
import okio.ByteString

class WebSocketManager {

    private var client: OkHttpClient = OkHttpClient()
    private var webSocket: WebSocket? = null
    private var listener: WebSocketListener? = null

    companion object {
        private const val TAG = "WebSocketManager"
        @Volatile
        private var instance: WebSocketManager? = null

        fun getInstance(): WebSocketManager {
            return instance ?: synchronized(this) {
                instance ?: WebSocketManager().also { instance = it }
            }
        }
    }

    interface WebSocketCallback {
        fun onConnected()
        fun onMessageReceived(message: String)
        fun onDisconnected(reason: String)
        fun onError(error: String)
    }

    fun connect(url: String, callback: WebSocketCallback) {
        Log.d(TAG, "Connecting to: $url")
        val request = Request.Builder().url(url).build()
        
        listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                this@WebSocketManager.webSocket = webSocket
                Log.d(TAG, "Connected!")
                callback.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received message: $text")
                callback.onMessageReceived(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "Received bytes: ${bytes.hex()}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                Log.d(TAG, "Closing: $code / $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Closed: $code / $reason")
                callback.onDisconnected(reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Error: ${t.message}")
                callback.onError(t.message ?: "Unknown error")
            }
        }

        webSocket = client.newWebSocket(request, listener!!)
    }

    fun sendMessage(message: String) {
        Log.d(TAG, "Sending message: $message")
        webSocket?.send(message) ?: Log.e(TAG, "WebSocket not connected")
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }
}