package com.app.vc.message

import com.app.vc.KeepModel

data class ResponseModelUpdateEstimationStatus(
    val `data`: Data,
    val status: String,
    val success: Boolean
): KeepModel