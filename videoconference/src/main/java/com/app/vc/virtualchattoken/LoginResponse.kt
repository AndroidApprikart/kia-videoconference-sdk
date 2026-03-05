package com.app.vc.virtualchattoken

data class LoginResponse(
    val access: String,
    val dealer_code: String,
    val display_name: String,
    val refresh: String,
    val role: String,
    val username: String
)