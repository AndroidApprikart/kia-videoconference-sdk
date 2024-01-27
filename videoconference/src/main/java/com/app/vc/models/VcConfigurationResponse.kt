package com.app.vc.models

import com.app.vc.KeepModel
import com.google.gson.annotations.SerializedName

data class VcConfigurationResponse(
    @SerializedName("mcu_required")
    var mcu_required:Boolean?,
    @SerializedName("status")
    var status:String?,
    @SerializedName("error")
    var error:String?
): KeepModel