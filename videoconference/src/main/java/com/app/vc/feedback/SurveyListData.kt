package com.app.vc.feedback

import com.app.vc.utils.KeepModel
import com.google.gson.annotations.SerializedName

data class SurveyListData(
    @SerializedName("survey_list")
    val surveyList: List<SurveyData>
): KeepModel