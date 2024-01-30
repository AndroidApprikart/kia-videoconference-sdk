package com.app.vc.message

import com.app.vc.utils.KeepModel

data class ResponseModelUpdateEstimateStatus(
    val `data`: DataXX,
    val message: String,
    val messageList: Any,
    val status: String
): KeepModel