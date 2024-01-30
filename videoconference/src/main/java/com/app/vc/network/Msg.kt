package com.app.vc.network

import com.app.vc.utils.KeepModel

data class Msg(
    val dataId: Any,
    val errorId: Int,
    val message: Any,
    val success: Boolean
): KeepModel