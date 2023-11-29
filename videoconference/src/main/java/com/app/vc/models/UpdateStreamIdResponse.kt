package com.app.vc.models

import com.google.gson.annotations.SerializedName

data class UpdateStreamIdResponse(
    @SerializedName("status")
    var status:String?,
    @SerializedName("apiErrorMessage", alternate = arrayOf("error"))
    var apiErrorMessage: String?
)