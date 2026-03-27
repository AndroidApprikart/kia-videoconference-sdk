package com.app.vc.participants

import android.content.Context
import com.app.vc.models.GroupMemberResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ParticipantsStorage {
    private const val PREFS_NAME = "participants_cache"
    private val gson = Gson()
    private val listType = object : TypeToken<List<GroupMemberResponse>>() {}.type

    fun save(context: Context, groupSlug: String, members: List<GroupMemberResponse>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(groupSlug, gson.toJson(members))
            .apply()
    }

    fun load(context: Context, groupSlug: String): List<GroupMemberResponse> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(groupSlug, null)
            ?: return emptyList()
        return runCatching { gson.fromJson<List<GroupMemberResponse>>(raw, listType).orEmpty() }
            .getOrDefault(emptyList())
    }
}
