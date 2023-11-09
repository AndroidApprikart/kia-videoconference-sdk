package com.app.vc.models

enum class MessageStatusEnum (var tag:String, var displayText:String){
    /*message status*/
    MSG_SENDING_IN_PROGRESS("MSG_SEND_IN_PROGRESS","sending"),
    MSG_SENT_SUCCESS("MSG_SENT_SUCCESS","sent"),
    MSG_SENT_FAILURE("MSG_SENT_FAILURE","failed"),
    FILE_UPLOAD_PROGRESS("FILE_UPLOAD_PROGRESS","uploading"),
    FILE_UPLOAD_SUCCESS("FILE_UPLOAD_SUCCESS","upload success"),
    FILE_UPLOAD_FAILURE("FILE_UPLOAD_FAILURE","upload failed"),

    FILE_DOWNLOAD_PROGRESS("FILE_DOWNLOAD_PROGRESS","downloading"),
    FILE_DOWNLOAD_SUCCESS("FILE_DOWNLOAD_SUCCESS","download success"),
    FILE_DOWNLOAD_FAILURE("FILE_DOWNLOAD_FAILURE","download failed")


}