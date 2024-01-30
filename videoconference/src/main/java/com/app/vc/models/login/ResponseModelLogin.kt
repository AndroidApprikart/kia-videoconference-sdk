package com.app.vc.models.login

import com.app.vc.utils.KeepModel
import com.google.gson.annotations.SerializedName

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
): KeepModel
{
    constructor():this(null,null,null,"failed",false)
}