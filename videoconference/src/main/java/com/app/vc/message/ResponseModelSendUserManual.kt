package com.kia.vc.message

data class ResponseModelSendUserManual(
    val message: String,
    val status: String,
    val success: Boolean
)