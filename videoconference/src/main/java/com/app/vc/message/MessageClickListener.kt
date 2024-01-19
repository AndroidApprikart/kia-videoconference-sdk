package com.app.vc.message

import com.app.vc.models.MessageModel

/* created by Naghma 20/10/23*/


interface MessageClickListener {
    /**to open file_message type server file url*/
    fun openURLInWeb(url:String)

    /**use for file_message type remote message -> to download and then open the message locally*/
    fun downloadFileAndOpen(data: MessageModel)

    /**use for file_message type local message -> to open the local file path in device*/
    fun openFile(data: MessageModel)

    /**use for local message of both type(file, text) for resending, based on the message status*/
    fun resendMessage(data:MessageModel)

    fun downloadFileUsingServerFilePath(serverFilePath:String,fileName:String)
}