package com.kia.vc.feedback

import com.app.vc.utils.KeepModel
import com.google.gson.annotations.SerializedName

data class RequestModelCreateSurvey(
    @SerializedName("cust_code")
    val customerCode: String,
    @SerializedName("dealer_no")
    val dealerNumber: String,
    @SerializedName("ro_code")
    val roNumber: String,
    @SerializedName("meeting_code")
    val meetingCode: String,
    @SerializedName("feedback")
    val feedback: ArrayList<FeedbackListModel>,
    @SerializedName("username")
    val userName:String


): KeepModel