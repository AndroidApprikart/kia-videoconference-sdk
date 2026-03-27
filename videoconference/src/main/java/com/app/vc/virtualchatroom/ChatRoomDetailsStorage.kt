package com.app.vc.virtualchatroom

import android.content.Context
import com.google.gson.Gson

data class CachedRoomDetails(
    val jobNotes: String? = null,
    val statusLabel: String? = null,
    val roNumberDisplay: String? = null
)

object ChatRoomDetailsStorage {
    private const val PREFS_NAME = "chat_room_details_cache"
    private val gson = Gson()

    fun save(context: Context, roomSlug: String, details: CachedRoomDetails) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(roomSlug, gson.toJson(details))
            .apply()
    }

    fun load(context: Context, roomSlug: String): CachedRoomDetails? {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(roomSlug, null)
            ?: return null
        return runCatching { gson.fromJson(raw, CachedRoomDetails::class.java) }.getOrNull()
    }
}
