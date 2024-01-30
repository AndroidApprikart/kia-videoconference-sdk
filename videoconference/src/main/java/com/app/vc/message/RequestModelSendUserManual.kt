package com.kia.vc.message

data class RequestModelSendUserManual(
    val customerName: String,
    val dealer_no: String,
    val mobileNo: String
)