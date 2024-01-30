package com.app.vc.models

import com.app.vc.utils.KeepModel

data class ResponseModelUpdateVideoStatus(
    val `data`: Data,
    val message: String,
    val messageList: Any,
    val status: String
): KeepModel