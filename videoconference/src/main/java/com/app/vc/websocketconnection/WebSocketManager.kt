package com.app.vc.websocketconnection

import android.util.Log
import okhttp3.*
import okio.ByteString

class WebSocketManager {

    private var client: OkHttpClient = OkHttpClient()
    private var webSocket: WebSocket? = null
    private var listener: WebSocketListener? = null
    private var callback: WebSocketCallback? = null
    private var currentUrl: String? = null

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
        if (webSocket != null && currentUrl == url) {
            Log.d(TAG, "Already connected to $url, updating callback")
            this.callback = callback
            callback.onConnected()
            return
        }
        disconnect()
        Log.d(TAG, "Connecting to: $url")
        currentUrl = url
        this.callback = callback
        val request = Request.Builder().url(url).build()
        
        listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                this@WebSocketManager.webSocket = webSocket
                Log.d(TAG, "Connected!")
                this@WebSocketManager.callback?.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received message: $text")
                this@WebSocketManager.callback?.onMessageReceived(text)
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
                currentUrl = null
                this@WebSocketManager.webSocket = null
                this@WebSocketManager.callback?.onDisconnected(reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Error: ${t.message}")
                currentUrl = null
                this@WebSocketManager.webSocket = null
                this@WebSocketManager.callback?.onError(t.message ?: "Unknown error")
            }
        }

        webSocket = client.newWebSocket(request, listener!!)
    }

    fun sendMessage(message: String) {
        Log.d(TAG, "Sending message: $message")
        webSocket?.send(message) ?: Log.e(TAG, "WebSocket not connected")
    }

    fun disconnect() {
        callback = null
        currentUrl = null
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        listener = null
    }

    /**
     * Clears the activity callback without closing the WebSocket.
     * Use when leaving the chat screen so the connection stays alive in the background
     * until the app is killed or the user opens a different room.
     */
    fun clearCallback() {
        callback = null
    }
}