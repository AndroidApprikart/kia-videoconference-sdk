package com.app.vc.feedback

import com.app.vc.KeepModel
import com.google.gson.annotations.SerializedName

data class SurveyData(
    @SerializedName("cmm_code")
    val cmmCode: String,
    @SerializedName("cmm_code_name")
    val surveyQuestion: String,
    @SerializedName("cmm_grp_code_name")
    val cmmGroupName: String
): KeepModel