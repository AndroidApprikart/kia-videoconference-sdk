package com.app.vc.models

import com.app.vc.KeepModel
import com.google.gson.annotations.SerializedName


data class ValidateVcResponse(
    @SerializedName("status")
    var status:String?,
    @SerializedName("error")
    var error:String?

):KeepModel{
    constructor():this("","")
}