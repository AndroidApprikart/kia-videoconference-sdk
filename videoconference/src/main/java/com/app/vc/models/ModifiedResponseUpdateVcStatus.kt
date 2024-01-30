package com.app.vc.models

import com.app.vc.utils.KeepModel

data class ModifiedResponseUpdateVcStatus(
    val responseData: ResponseModelUpdateVideoStatus?,
    var apiResponseStatus:Boolean,
): KeepModel
