package com.app.vc.websocketconnection

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.app.vc.utils.ApiDetails
import com.app.vc.utils.PreferenceManager
import com.app.vc.virtualroomlist.GroupUnreadStore
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class SocketSessionCoordinator private constructor() {

    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastSuccessfulRefreshAtMs = 0L

    companion object {
        private const val TAG = "SocketSessionCoordinator"

        @Volatile
        private var instance: SocketSessionCoordinator? = null

        fun getInstance(): SocketSessionCoordinator {
            return instance ?: synchronized(this) {
                instance ?: SocketSessionCoordinator().also { instance = it }
            }
        }
    }

    @Synchronized
    fun handleSocketAuthFailure(source: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSuccessfulRefreshAtMs < 2000L && !PreferenceManager.getAccessToken().isNullOrBlank()) {
            Log.d(TAG, "Recent token refresh already completed. Reconnecting sockets for $source")
            reconnectSockets()
            return
        }

        val newAccessToken = refreshAccessTokenBlocking()
        if (!newAccessToken.isNullOrBlank()) {
            lastSuccessfulRefreshAtMs = SystemClock.elapsedRealtime()
            Log.d(TAG, "Token refreshed after $source auth failure. Reconnecting sockets.")
            reconnectSockets()
        } else {
            Log.e(TAG, "Token refresh failed after $source auth failure. Clearing session.")
            clearSession()
        }
    }

    fun clearSession() {
        mainHandler.post {
            NotificationWebSocketManager.getInstance().disconnect()
            WebSocketManager.getInstance().disconnect()
            GroupUnreadStore.clear()
            PreferenceManager.clearAuthSession()
        }
    }

    private fun reconnectSockets() {
        mainHandler.post {
            NotificationWebSocketManager.getInstance().reconnectWithLatestToken()
            WebSocketManager.getInstance().reconnectWithLatestToken()
        }
    }

    private fun refreshAccessTokenBlocking(): String? {
        val refresh = PreferenceManager.getRefreshToken() ?: return null
        if (refresh.isBlank()) return null

        return try {
            val json = JsonObject().apply { addProperty("refresh", refresh) }
            val body = json.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(ApiDetails.APRIK_Kia_BASE_URL + "api/token/refresh/")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful || response.body == null) return null
                val responseJson = Gson().fromJson(response.body!!.string(), JsonObject::class.java)
                val access = responseJson.get("access")?.asString
                if (access.isNullOrBlank()) return null
                PreferenceManager.setAccessToken(access)
                responseJson.get("refresh")?.asString?.takeIf { it.isNotBlank() }
                    ?.let { PreferenceManager.setRefreshToken(it) }
                access
            }
        } catch (e: Exception) {
            Log.e(TAG, "refreshAccessTokenBlocking failed: ${e.message}")
            null
        }
    }
}
