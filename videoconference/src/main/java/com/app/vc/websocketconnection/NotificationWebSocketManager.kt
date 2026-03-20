package com.app.vc.websocketconnection

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.app.vc.utils.PreferenceManager
import com.app.vc.virtualroomlist.GroupUnreadStore
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class NotificationWebSocketManager private constructor() {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val handler = Handler(Looper.getMainLooper())

    private var webSocket: WebSocket? = null
    private var currentUrl: String? = null
    private var isConnected = false
    private var isConnecting = false
    private var shouldReconnect = true
    private var reconnectScheduled = false
    private var activeGroupSlug: String? = null

    private val reconnectRunnable = Runnable {
        reconnectScheduled = false
        val url = currentUrl
        if (!shouldReconnect || isConnected || isConnecting || url.isNullOrBlank()) return@Runnable
        connect(url)
    }

    companion object {
        private const val TAG = "NotificationSocket"

        @Volatile
        private var instance: NotificationWebSocketManager? = null

        fun getInstance(): NotificationWebSocketManager {
            return instance ?: synchronized(this) {
                instance ?: NotificationWebSocketManager().also { instance = it }
            }
        }
    }

    fun connectWithToken(accessToken: String?) {
        if (accessToken.isNullOrBlank()) return
        val url = "wss://testingchat.apprikart.com/ws/chat/notifications/?token=$accessToken"
        connect(url)
    }

    fun connect(url: String) {
        val sameActiveConnection = currentUrl == url && webSocket != null && (isConnected || isConnecting)
        shouldReconnect = true
        cancelReconnect()
        if (sameActiveConnection) return

        currentUrl = url
        closeCurrentSocket("Reconnecting notifications")
        isConnecting = true
        Log.d(TAG, "Connecting to: $url")

        webSocket = client.newWebSocket(
            Request.Builder().url(url).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    if (this@NotificationWebSocketManager.webSocket !== webSocket) return
                    isConnecting = false
                    isConnected = true
                    cancelReconnect()
                    Log.d(TAG, "Notification WebSocket connected")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (this@NotificationWebSocketManager.webSocket !== webSocket) return
                    handleIncomingMessage(text)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (this@NotificationWebSocketManager.webSocket !== webSocket) return
                    Log.d(TAG, "Notification socket closed: $code / $reason")
                    this@NotificationWebSocketManager.webSocket = null
                    isConnected = false
                    isConnecting = false
                    if (isAuthFailure(code = code, reason = reason)) {
                        SocketSessionCoordinator.getInstance().handleSocketAuthFailure("notification_socket")
                    } else {
                        scheduleReconnect()
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (this@NotificationWebSocketManager.webSocket !== webSocket) return
                    Log.e(TAG, "Notification socket failure: ${t.message}")
                    this@NotificationWebSocketManager.webSocket = null
                    isConnected = false
                    isConnecting = false
                    if (isAuthFailure(error = t.message, response = response)) {
                        SocketSessionCoordinator.getInstance().handleSocketAuthFailure("notification_socket")
                    } else {
                        scheduleReconnect()
                    }
                }
            }
        )
    }

    fun reconnectWithLatestToken() {
        connectWithToken(PreferenceManager.getAccessToken())
    }

    fun setActiveGroupSlug(groupSlug: String?) {
        activeGroupSlug = groupSlug
    }

    fun disconnect() {
        shouldReconnect = false
        cancelReconnect()
        closeCurrentSocket("Notifications disconnected")
        currentUrl = null
        activeGroupSlug = null
    }

    private fun handleIncomingMessage(rawMessage: String) {
        try {
            val json = Gson().fromJson(rawMessage, JsonObject::class.java)
            val type = json.get("type")?.asString ?: return
            if (type != "group.new_message") return

            val groupSlug = json.get("group_slug")?.asString ?: return
            if (groupSlug == activeGroupSlug) return

            val unreadCount = json.get("unread_count")?.let { element ->
                if (element.isJsonPrimitive && element.asJsonPrimitive.isNumber) element.asInt else 0
            } ?: 0
            GroupUnreadStore.updateUnreadCount(groupSlug, unreadCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse notification message: ${e.message}")
        }
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect || isConnected || isConnecting || reconnectScheduled) return
        reconnectScheduled = true
        handler.postDelayed(reconnectRunnable, 3000L)
    }

    private fun cancelReconnect() {
        reconnectScheduled = false
        handler.removeCallbacks(reconnectRunnable)
    }

    private fun closeCurrentSocket(reason: String) {
        webSocket?.close(1000, reason)
        webSocket = null
        isConnected = false
        isConnecting = false
    }

    private fun isAuthFailure(
        code: Int? = null,
        reason: String? = null,
        error: String? = null,
        response: Response? = null
    ): Boolean {
        if (code == 4001 || response?.code == 401 || response?.code == 403) return true
        val combined = listOfNotNull(reason, error).joinToString(" ").lowercase()
        return combined.contains("4001") ||
            combined.contains("401") ||
            combined.contains("unauthorized") ||
            combined.contains("forbidden") ||
            combined.contains("token") ||
            combined.contains("auth")
    }
}
