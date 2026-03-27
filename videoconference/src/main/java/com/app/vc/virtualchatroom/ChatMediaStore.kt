package com.app.vc.virtualchatroom

import java.util.concurrent.CopyOnWriteArraySet

data class RoomMediaSnapshot(
    val photosVideos: List<ChatMessage>,
    val documents: List<ChatMessage>,
    val isLoading: Boolean = false
)

object ChatMediaStore {
    private val roomMessages = mutableMapOf<String, MutableList<ChatMessage>>()
    private val listeners = mutableMapOf<String, CopyOnWriteArraySet<(RoomMediaSnapshot) -> Unit>>()
    private val roomLoading = mutableMapOf<String, Boolean>()

    @Synchronized
    fun replaceMessages(roomSlug: String, messages: List<ChatMessage>) {
        roomMessages[roomSlug] = messages.map { it.copy() }.toMutableList()
        notifyListeners(roomSlug)
    }

    @Synchronized
    fun addOrUpdateMessage(roomSlug: String, message: ChatMessage) {
        val list = roomMessages.getOrPut(roomSlug) { mutableListOf() }
        val index = list.indexOfFirst { it.messageId == message.messageId && !message.messageId.isNullOrBlank() }
        if (index >= 0) {
            list[index] = message.copy()
        } else {
            list.add(message.copy())
        }
        notifyListeners(roomSlug)
    }

    @Synchronized
    fun setLoading(roomSlug: String, isLoading: Boolean) {
        roomLoading[roomSlug] = isLoading
        notifyListeners(roomSlug)
    }

    @Synchronized
    fun addListener(roomSlug: String, listener: (RoomMediaSnapshot) -> Unit) {
        listeners.getOrPut(roomSlug) { CopyOnWriteArraySet() }.add(listener)
        listener(snapshotFor(roomSlug))
    }

    @Synchronized
    fun removeListener(roomSlug: String, listener: (RoomMediaSnapshot) -> Unit) {
        listeners[roomSlug]?.remove(listener)
    }

    @Synchronized
    private fun notifyListeners(roomSlug: String) {
        val snapshot = snapshotFor(roomSlug)
        listeners[roomSlug]?.forEach { it(snapshot) }
    }

    @Synchronized
    private fun snapshotFor(roomSlug: String): RoomMediaSnapshot {
        val messages = roomMessages[roomSlug]
            .orEmpty()
            .sortedByDescending { it.createdAtMillis ?: 0L }
        return RoomMediaSnapshot(
            photosVideos = messages.filter { it.type == ChatMessageType.IMAGE || it.type == ChatMessageType.VIDEO },
            documents = messages.filter { it.type == ChatMessageType.FILE },
            isLoading = roomLoading[roomSlug] == true
        )
    }
}
