package com.app.vc.network

import com.app.vc.utils.KeepModel

data class ResponseModelDeleteBroadcast(
    val msg: Msg,
    val status: String
): KeepModel