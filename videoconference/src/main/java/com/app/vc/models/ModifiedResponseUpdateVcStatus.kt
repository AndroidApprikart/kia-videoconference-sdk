package com.app.vc.models

import com.app.vc.KeepModel

data class ModifiedResponseUpdateVcStatus(
    val responseData: ResponseModelUpdateVideoStatus?,
    var apiResponseStatus:Boolean,
):KeepModel
