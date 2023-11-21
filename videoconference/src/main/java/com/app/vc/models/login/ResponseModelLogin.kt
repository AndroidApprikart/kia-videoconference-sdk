package com.app.vc.models.login

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ResponseModelLogin(
    @SerializedName("data")
    val loginData: Data?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("pwd_change_date")
    val pwd_change_date: String?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("success")
    val success: Boolean?
):Serializable
{
    constructor():this(null,null,null,"failed",false)
}