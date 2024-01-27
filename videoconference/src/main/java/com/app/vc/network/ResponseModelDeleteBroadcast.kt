package com.app.vc.network

import com.app.vc.KeepModel

data class ResponseModelDeleteBroadcast(
    val msg: Msg,
    val status: String
): KeepModel