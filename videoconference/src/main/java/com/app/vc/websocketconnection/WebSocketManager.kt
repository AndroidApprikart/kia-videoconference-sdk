package com.app.vc.websocketconnection

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

class WebSocketManager {

    private var client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var listener: WebSocketListener? = null
    private var callback: WebSocketCallback? = null
    private var currentUrl: String? = null

    private var isConnected = false
    private var isConnecting = false
    private var shouldReconnect = true
    private var reconnectScheduled = false

    private val handler = Handler(Looper.getMainLooper())
    private val reconnectRunnable = Runnable {
        reconnectScheduled = false
        val url = currentUrl
        val currentCallback = callback
        if (!shouldReconnect || isConnected || isConnecting || url.isNullOrBlank() || currentCallback == null) {
            return@Runnable
        }
        Log.d(TAG, "Reconnecting to $url")
        connect(url, currentCallback)
    }

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
        val sameActiveConnection = webSocket != null && currentUrl == url && (isConnected || isConnecting)
        this.callback = callback
        shouldReconnect = true
        cancelReconnect()

        if (sameActiveConnection) {
            Log.d(TAG, "Socket active. Updating callback only.")
            if (isConnected) {
                callback.onConnected()
            }
            return
        }

        if (isConnecting && currentUrl == url) {
            Log.d(TAG, "Connection already in progress for $url")
            return
        }

        currentUrl = url
        closeCurrentSocket("Reconnecting")
        isConnecting = true

        Log.d(TAG, "Connecting to: $url")

        val request = Request.Builder()
            .url(url)
            .build()

        listener = object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (this@WebSocketManager.webSocket !== webSocket) return

                isConnecting = false
                isConnected = true
                cancelReconnect()

                Log.d(TAG, "WebSocket Connected")

                this@WebSocketManager.callback?.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (this@WebSocketManager.webSocket !== webSocket) return

                Log.d(TAG, "Message Received: $text")

                this@WebSocketManager.callback?.onMessageReceived(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (this@WebSocketManager.webSocket !== webSocket) return

                Log.d(TAG, "Bytes Received: ${bytes.hex()}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (this@WebSocketManager.webSocket !== webSocket) return

                Log.d(TAG, "Closing: $code / $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (this@WebSocketManager.webSocket !== webSocket) return

                Log.d(TAG, "Closed: $code / $reason")

                this@WebSocketManager.webSocket = null
                listener = null
                isConnected = false
                isConnecting = false

                this@WebSocketManager.callback?.onDisconnected(reason)
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (this@WebSocketManager.webSocket !== webSocket) return

                Log.e(TAG, "WebSocket Failure: ${t.message}")

                this@WebSocketManager.webSocket = null
                listener = null
                isConnected = false
                isConnecting = false

                this@WebSocketManager.callback?.onError(t.message ?: "Unknown error")
                scheduleReconnect()
            }
        }

        val newSocket = client.newWebSocket(request, listener!!)
        webSocket = newSocket
    }

    fun sendMessage(message: String): Boolean {
        if (!isConnected || webSocket == null) {
            Log.e(TAG, "WebSocket not connected")
            scheduleReconnect()
            callback?.onError("WebSocket not connected")
            return false
        }

        Log.d(TAG, "Sending message: $message")
        return webSocket!!.send(message)
    }

    fun reconnectNow() {
        if (!shouldReconnect) return
        cancelReconnect()
        if (!isConnected && !isConnecting) {
            reconnectRunnable.run()
        }
    }

    fun disconnect() {
        shouldReconnect = false
        cancelReconnect()
        callback = null
        currentUrl = null
        closeCurrentSocket("User disconnected")
    }

    fun clearCallback() {
        callback = null
    }

    fun isConnected(): Boolean {
        return isConnected
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect || isConnected || isConnecting || reconnectScheduled) return
        reconnectScheduled = true
        Log.d(TAG, "Reconnecting in 3 seconds...")
        handler.postDelayed(reconnectRunnable, 3000)
    }

    private fun cancelReconnect() {
        reconnectScheduled = false
        handler.removeCallbacks(reconnectRunnable)
    }

    private fun closeCurrentSocket(reason: String) {
        webSocket?.close(1000, reason)
        webSocket = null
        listener = null
        isConnected = false
        isConnecting = false
    }
}