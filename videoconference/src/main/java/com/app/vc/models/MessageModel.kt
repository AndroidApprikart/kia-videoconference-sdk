package com.app.vc.models

import com.app.vc.AndroidUtils
import com.app.vc.VCConstants

/* created by Naghma 20/10/23*/


class MessageModel(
    var userName: String,
    var messageText: String,
    var isLocalMessage: Boolean,
    var messageType: String,
    var id: Long,
    var fileName:String,
    var serverFilePath:String
){

    constructor():this("","", true,
        VCConstants.TEXT_MESSAGE,
        AndroidUtils.getCurrentTimeInMill(),"","")
}

