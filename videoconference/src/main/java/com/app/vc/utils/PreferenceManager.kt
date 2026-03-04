package com.app.vc.utils

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
        private const val ACCESS_TOKEN = "ACCESS_TOKEN"
        private const val REFRESH_TOKEN = "REFRESH_TOKEN"
        private const val USER_ID = "USER_ID"


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
            return mSharedPrefs!!.getBoolean(IS_FRONT_CAMERA, true)
        }

        fun setIsFrontCamera(user: Boolean) {
            mSharedPrefs!!.edit().putBoolean(IS_FRONT_CAMERA, user).apply()
        }
        fun getuserType(): String? {
            return mSharedPrefs!!.getString(USERTYPE, "null")
        }

        fun setUserType(user: String?) {
            mSharedPrefs!!.edit().putString(USERTYPE, user).apply()
        }

        fun setEstimateToken(token:String) {
            return mSharedPrefs!!.edit().putString(ESTIMATE_TOKEN,token).apply()
        }

        fun getEstimateToken():String? {
            return mSharedPrefs!!.getString(ESTIMATE_TOKEN,"")
        }

        fun setBaseUrl(baseUrl:String) {
            return mSharedPrefs!!.edit().putString(BASE_URL,baseUrl).apply()
        }
        fun getBaseUrl():String? {
            return mSharedPrefs!!.getString(BASE_URL,"")
        }

        fun setAccessToken(token: String?) {
            mSharedPrefs?.edit()?.putString(ACCESS_TOKEN, token)?.apply()
        }

        fun getAccessToken(): String? {
            return mSharedPrefs?.getString(ACCESS_TOKEN, "")
        }

        fun setRefreshToken(token: String?) {
            mSharedPrefs?.edit()?.putString(REFRESH_TOKEN, token)?.apply()
        }

        fun getRefreshToken(): String? {
            return mSharedPrefs?.getString(REFRESH_TOKEN, "")
        }

        fun setUserId(userId: String?) {
            mSharedPrefs?.edit()?.putString(USER_ID, userId)?.apply()
        }

        fun getUserId(): String? {
            return mSharedPrefs?.getString(USER_ID, "")
        }
    }
}