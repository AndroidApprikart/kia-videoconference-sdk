package com.app.vc.utils

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject

object JwtUtils {

    fun getUserIdFromToken(token: String): String? {
        try {
            val parts = token.split(".")
            if (parts.size != 3) {
                Log.e("JwtUtils", "Invalid JWT format")
                return null
            }
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE), Charsets.UTF_8)
            val jsonObject = Gson().fromJson(payload, JsonObject::class.java)
            return jsonObject.get("user_id")?.asString
        } catch (e: Exception) {
            Log.e("JwtUtils", "Error decoding JWT: ${e.message}")
            return null
        }
    }
}