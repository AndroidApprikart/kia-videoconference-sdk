package com.app.vc.message

import com.app.vc.KeepModel

data class EstimateModel(
    val `data`: ResponseModelEstimateData,
    val status: String,
    val success: Boolean
): KeepModel