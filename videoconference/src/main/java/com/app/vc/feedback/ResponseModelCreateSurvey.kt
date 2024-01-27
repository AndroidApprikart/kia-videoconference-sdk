package com.kia.vc.feedback

import com.app.vc.KeepModel

data class ResponseModelCreateSurvey(
    val `data`: Any,
    val message: String,
    val messageList: Any,
    val status: String
):KeepModel