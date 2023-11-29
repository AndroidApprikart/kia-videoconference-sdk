package com.app.vc.models

import com.google.gson.annotations.SerializedName

data class DisplayNameResponse (
    @SerializedName("stream_id")
    var streamID:String?,
    @SerializedName("display_name")
    var displayName:String?,
    @SerializedName("user_type")
var userType:String?
) {
    constructor() : this(null, null, null)
}