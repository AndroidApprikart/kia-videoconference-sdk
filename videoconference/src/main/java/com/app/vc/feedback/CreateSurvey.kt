package com.kia.vc.models

import com.app.vc.feedback.DataX

data class CreateSurvey(
    val `data`: DataX,
    val message: String,
    val messageList: Any,
    val status: String
)