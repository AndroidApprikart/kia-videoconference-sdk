package com.app.vc.models

import com.app.vc.KeepModel
import com.google.gson.annotations.SerializedName

data class UpdateStreamIdResponse(
    @SerializedName("status")
    var status:String?,
    @SerializedName("apiErrorMessage", alternate = arrayOf("error"))
    var apiErrorMessage: String?
): KeepModel