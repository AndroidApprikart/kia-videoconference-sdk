package com.app.vc.models

import com.app.vc.utils.KeepModel
import com.google.gson.annotations.SerializedName

data class UploadVcFileResponse(
    @SerializedName("id")
    var id:Long?,
    @SerializedName("vc_room")
    var vcRoom:String?,
    @SerializedName("who")
    var who:String?,
    @SerializedName("user_type")
    var userType:String?,
    @SerializedName("file")
    var file:String?,
    @SerializedName("created_date_time")
    var createdDateTime:String?,
    @SerializedName("apiErrorMessage", alternate = arrayOf("error"))
    var apiErrorMessage: String?
): KeepModel