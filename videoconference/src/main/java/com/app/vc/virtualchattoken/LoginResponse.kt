package com.app.vc.virtualchattoken

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("refresh")
    val refresh: String?,
    @SerializedName("access")
    val access: String?
)