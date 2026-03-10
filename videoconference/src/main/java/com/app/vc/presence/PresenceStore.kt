package com.app.vc.presence

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Tracks which user IDs are currently "online" based on WebSocket activity
 * (e.g. typing, sending messages). Used to show green/red status in the participants list.
 */
object PresenceStore {

    private const val PRESENCE_TIMEOUT_MS = 90_000L // 90 seconds without activity -> offline

    private val handler = Handler(Looper.getMainLooper())
    private val onlineUserIds = mutableSetOf<String>()
    private val listeners = CopyOnWriteArraySet<(Set<String>) -> Unit>()
    private val pendingOffline = mutableMapOf<String, Runnable>()

    /** Mark a user as online; they will be considered offline after [PRESENCE_TIMEOUT_MS] with no further updates. */
    @Synchronized
    fun setUserOnline(userId: String?) {
        if (userId.isNullOrBlank()) return
        pendingOffline[userId]?.let { handler.removeCallbacks(it) }
        pendingOffline.remove(userId)
        val added = onlineUserIds.add(userId)
        pendingOffline[userId] = Runnable {
            synchronized(this@PresenceStore) {
                onlineUserIds.remove(userId)
                pendingOffline.remove(userId)
            }
            notifyListeners()
        }.also { handler.postDelayed(it, PRESENCE_TIMEOUT_MS) }
        if (added) notifyListeners()
    }

    /** Mark a user as offline (e.g. from user.presence WebSocket event). */
    @Synchronized
    fun setUserOffline(userId: String?) {
        if (userId.isNullOrBlank()) return
        pendingOffline[userId]?.let { handler.removeCallbacks(it) }
        pendingOffline.remove(userId)
        val removed = onlineUserIds.remove(userId)
        if (removed) notifyListeners()
    }

    /** Current set of user IDs considered online. */
    fun getOnlineUserIds(): Set<String> = synchronized(this) { onlineUserIds.toSet() }

    /** Register to be notified when the online set changes. Callbacks are invoked on the main thread. */
    fun addListener(listener: (Set<String>) -> Unit) {
        listeners.add(listener)
        listener(getOnlineUserIds())
    }

    fun removeListener(listener: (Set<String>) -> Unit) {
        listeners.remove(listener)
    }

    /** Clear all presence (e.g. when leaving the room or WebSocket disconnects). */
    @Synchronized
    fun clear() {
        pendingOffline.values.forEach { handler.removeCallbacks(it) }
        pendingOffline.clear()
        if (onlineUserIds.isNotEmpty()) {
            onlineUserIds.clear()
            handler.post { notifyListeners() }
        }
    }

    private fun notifyListeners() {
        val copy = getOnlineUserIds()
        handler.post {
            listeners.forEach { it(copy) }
        }
    }
}

