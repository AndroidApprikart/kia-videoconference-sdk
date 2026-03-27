package com.app.vc.virtualchatroom

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ChatMessageStorage {
    private const val PREFS_NAME = "chat_message_cache"
    private const val KEY_LAST_ID_PREFIX = "last_displayed_id_"
    private const val KEY_DRAFT_PREFIX = "draft_"
    private val gson = Gson()
    private val listType = object : TypeToken<List<ChatMessage>>() {}.type

    fun saveMessages(context: Context, roomSlug: String, messages: List<ChatMessage>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawMessages = messages.filter { it.type != ChatMessageType.DATE_HEADER }
        val lastDisplayedId = rawMessages.mapNotNull { it.messageId?.toIntOrNull() }.maxOrNull() ?: 0
        prefs.edit()
            .putString(roomSlug, gson.toJson(rawMessages))
            .putInt(KEY_LAST_ID_PREFIX + roomSlug, lastDisplayedId)
            .apply()
    }

    fun loadMessages(context: Context, roomSlug: String): List<ChatMessage> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(roomSlug, null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<ChatMessage>>(json, listType).orEmpty()
        }.getOrDefault(emptyList())
    }

    fun getLastDisplayedMessageId(context: Context, roomSlug: String): Int? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_LAST_ID_PREFIX + roomSlug, 0).takeIf { it > 0 }
    }

    fun saveDraft(context: Context, roomSlug: String, draft: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DRAFT_PREFIX + roomSlug, draft).apply()
    }

    fun loadDraft(context: Context, roomSlug: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DRAFT_PREFIX + roomSlug, "").orEmpty()
    }

    fun clearDraft(context: Context, roomSlug: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_DRAFT_PREFIX + roomSlug).apply()
    }
}
