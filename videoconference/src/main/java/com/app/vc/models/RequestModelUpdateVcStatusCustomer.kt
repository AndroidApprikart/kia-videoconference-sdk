package com.app.vc.models

import com.google.gson.annotations.SerializedName

data class RequestModelUpdateVcStatusCustomer(
    @SerializedName("username")
    val userName: String,
    @SerializedName("meeting_code")
    val meetingCode: String,
    @SerializedName("vc_status")
    val vcStatus: String,
    @SerializedName("dealer_no")
    val dealerNumber: String,
)