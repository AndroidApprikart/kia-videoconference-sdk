package com.app.vc.models

import com.app.vc.utils.AndroidUtils
import com.app.vc.utils.KeepModel
import com.app.vc.utils.VCConstants
import com.app.vc.message.ResponseModelEstimateData
import java.text.SimpleDateFormat
import java.util.Date

/* created by Naghma 20/10/23*/


class MessageModel(
    var userName: String,
    var messageText: String,
    var isLocalMessage: Boolean,
    var messageType: String,
    var id: Long,
    var fileName:String,
    var serverFilePath:String,
    var status:String,
    var estimationDetails: ResponseModelEstimateData?
): KeepModel {


    constructor():this("","", true,
        VCConstants.TEXT_MESSAGE,
        AndroidUtils.getCurrentTimeInMill(),"","","",null)

    var downloadReferenceId = 0L
    var localFilePath = "" /*maintaining this to open file for local file type message..if not in requirement comment it*/
    var downloadableFileName = SimpleDateFormat("dMMyyyy_Hms_").format(Date(id)).toString()+fileName /*maintain this to make sure it is a new copy for every message- as a unique id*/
}

