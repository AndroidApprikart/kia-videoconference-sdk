package com.kia.vc.feedback

import com.app.vc.utils.KeepModel

data class FeedbackListModel(
    val comments: String,
    val rating: String,
    val cmm_code:String
): KeepModel