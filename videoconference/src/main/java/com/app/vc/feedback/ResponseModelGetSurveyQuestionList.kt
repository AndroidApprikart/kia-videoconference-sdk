package com.kia.vc.models

import com.app.vc.utils.KeepModel
import com.app.vc.feedback.SurveyListData
import com.google.gson.annotations.SerializedName

data class ResponseModelGetSurveyQuestionList(
    @SerializedName("data")
    val surveyQuestionListData: SurveyListData,
    @SerializedName("message")
    val message: String,
    @SerializedName("messageList")
    val messageList: Any,
    @SerializedName("status")
    val status: String
): KeepModel