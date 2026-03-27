package com.app.vc.virtualroomlist

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object VirtualRoomStorage {
    private const val PREFS_NAME = "virtual_room_cache"
    private const val KEY_ROOMS = "rooms"
    private val gson = Gson()
    private val listType = object : TypeToken<List<VirtualRoomUiModel>>() {}.type

    fun saveRooms(context: Context, rooms: List<VirtualRoomUiModel>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ROOMS, gson.toJson(rooms))
            .apply()
    }

    fun loadRooms(context: Context): List<VirtualRoomUiModel> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ROOMS, null)
            ?: return emptyList()
        return runCatching {
            gson.fromJson<List<VirtualRoomUiModel>>(raw, listType).orEmpty()
        }.getOrDefault(emptyList())
    }
}
