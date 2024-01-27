package com.app.vc.models

import com.app.vc.KeepModel

data class Data(
    val `data`: Int,
    val message: String,
    val status: String,
    val success: Boolean
): KeepModel