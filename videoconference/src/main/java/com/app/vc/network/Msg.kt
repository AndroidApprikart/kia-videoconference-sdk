package com.app.vc.network

import com.app.vc.KeepModel

data class Msg(
    val dataId: Any,
    val errorId: Int,
    val message: Any,
    val success: Boolean
): KeepModel