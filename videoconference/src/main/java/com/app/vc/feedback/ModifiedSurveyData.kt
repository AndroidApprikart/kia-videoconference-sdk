package com.kia.vc.feedback

import com.app.vc.feedback.SurveyData


data class ModifiedSurveyData(
    val surveyData: SurveyData,
    var rating: Int = 0,
    var comment:String = "",
)
