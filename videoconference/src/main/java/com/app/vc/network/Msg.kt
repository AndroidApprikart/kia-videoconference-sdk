package com.app.vc.network

data class Msg(
    val dataId: Any,
    val errorId: Int,
    val message: Any,
    val success: Boolean
)