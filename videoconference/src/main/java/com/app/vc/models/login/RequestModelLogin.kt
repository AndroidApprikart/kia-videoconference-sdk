package com.app.vc.models.login

import com.app.vc.KeepModel
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class RequestModelLogin(
    @SerializedName("device_token")
    val deviceToken: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("user_name")
    val userName: String
): KeepModel