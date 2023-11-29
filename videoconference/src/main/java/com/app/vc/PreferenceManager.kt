package com.app.vc

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class PreferenceManager {
    companion object {

        fun init(context: Context) {
            Log.d("PreferenceManager:::", "KiaUntactApp: init: ")
            mSharedPrefs = context.getSharedPreferences(
                SHARED_PREF,
                Context.MODE_PRIVATE
            )
        }

        val SHARED_PREF = "shared_pref"
        var mSharedPrefs: SharedPreferences? = null
        private const val IS_CHAT_SCREEN_OPEN = "IS_CHAT_SCREEN_OPEN"
        private const val IS_FRONT_CAMERA = "is_Front_Camera"
        private const val USERTYPE = "USERTYPE"
        private const val ESTIMATE_TOKEN = "ESTIMATE_TOKEN"
        private const val BASE_URL = "BASE_URL"

        fun isChatScreenOpen(): Boolean? {

            return mSharedPrefs?.getBoolean(
                IS_CHAT_SCREEN_OPEN,
                false
            )
        }

        fun setChatScreenIsOpen(bgmRunningFlag: Boolean) {
            mSharedPrefs?.edit()?.putBoolean(
                IS_CHAT_SCREEN_OPEN,
                bgmRunningFlag
            )?.apply()
        }

        fun isFrontCamera(): Boolean {
            return mSharedPrefs!!.getBoolean(PreferenceManager.IS_FRONT_CAMERA, true)
        }

        fun setIsFrontCamera(user: Boolean) {
            mSharedPrefs!!.edit().putBoolean(PreferenceManager.IS_FRONT_CAMERA, user).apply()
        }
        fun getuserType(): String? {
            return mSharedPrefs!!.getString(PreferenceManager.USERTYPE, "null")
        }

        fun setUserType(user: String?) {
            mSharedPrefs!!.edit().putString(PreferenceManager.USERTYPE, user).apply()
        }

        fun setEstimateToken(token:String) {
            return mSharedPrefs!!.edit().putString(PreferenceManager.ESTIMATE_TOKEN,token).apply()
        }

        fun getEstimateToken():String? {
            return mSharedPrefs!!.getString(PreferenceManager.ESTIMATE_TOKEN,"")
        }

        fun setBaseUrl(baseUrl:String) {
            return mSharedPrefs!!.edit().putString(PreferenceManager.BASE_URL,baseUrl).apply()
        }
        fun getBaseUrl():String? {
            return mSharedPrefs!!.getString(PreferenceManager.BASE_URL,"")
        }
    }
}