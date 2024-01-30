package com.app.vc.message

import com.app.vc.utils.KeepModel

data class ResponseModelUpdateEstimationStatus(
    val `data`: Data,
    val status: String,
    val success: Boolean
): KeepModel