package com.app.vc.models.login

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class RequestModelLogin(
    @SerializedName("device_token")
    val deviceToken: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("user_name")
    val userName: String
):Serializable