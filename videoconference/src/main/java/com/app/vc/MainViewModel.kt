package com.app.vc

import android.os.RemoteException
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.app.vc.models.MessageModel
import com.app.vc.models.ParticipantsModel
import androidx.lifecycle.viewModelScope
import com.app.vc.models.MessageStatusEnum
import com.google.gson.Gson
import io.antmedia.webrtcandroidframework.apprtc.AppRTCAudioManager
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.webrtc.VideoTrack
import java.io.File
import java.net.ConnectException
import java.net.MalformedURLException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class MainViewModel : ViewModel() {
    var repository = DataRepository()
    var TAG = "MAINVM::"

    var toastMessage = MutableLiveData<String>()
//    var isProgressVisible = MutableLiveData<Boolean>() //removed and replaced with normal function call

    var isServiceStarted = false//used just for testing


    var endVCByUser = false

    var streams = ArrayList<String>()

    var tracks = ArrayList<VideoTrack>()

    var participants = ArrayList<ParticipantsModel>()
    var localAudio = true
    var localVideo = true
    var grid = true
    var bottomSheet = false


    var updateParticipants = MutableLiveData<Boolean>()
    var participantCount = MutableLiveData<String>()

    var isAudioDeviceUpdated = MutableLiveData<Boolean>()
    var audioDevices = MutableLiveData<Set<AppRTCAudioManager.AudioDevice>>()
    var newSelectedAudioDevice = MutableLiveData<AppRTCAudioManager.AudioDevice>()
    var currentSelectedAudioDevice = MutableLiveData<AppRTCAudioManager.AudioDevice>()

    var screenShareStatus = false
    var updateScreenShareForFragment = MutableLiveData<Boolean>()

    var isInitialConferenceStarted = false
    var internetSpeed = MutableLiveData<String>()
    var reconnectAttemptCount = 0
    var rejoinInProgress = false

    var roomInfoStreamsList = ArrayList<String>()

    var addNewRemoteMessage = MutableLiveData<Long>()
    var addNewLocalMessage = MutableLiveData<Long>()
    var updateLocalMessage = MutableLiveData<Long>()

    /*  var newLocalMessage = MessageModel() not required handling using the id's
      var oldLocalMessageToUpdate = MessageModel()
      var newRemoteMessage = MessageModel()*/

    var sendLocalTextMessageToDataChannel = MutableLiveData<Long>()
    var sendLocalFileMessageToDataChannel = MutableLiveData<Long>()

    var messageFragmentClose = MutableLiveData<Boolean>()
    var openFileManager = MutableLiveData<Boolean>()

    var messageListInMVM = ArrayList<MessageModel>()

    var startScreenShare = MutableLiveData<Boolean>()
    var stopScreenShare = MutableLiveData<Boolean>()

    var frontCamera = true

    /*keep track of fragment viewing*/
    var participantFragVisible = false
    var messageFragVisible = false
    var soundDeviceFragVisible = false
    var screenShareFragVisible = false

    /*message handling functions*/
    fun processNewLocalTextMessage(userInputText: String, id: Long) {
        val tempMessage = MessageModel(
            userName = "Local", //nahusha help
            messageText = userInputText.trim().toString(),
            isLocalMessage = true,
            messageType = VCConstants.TEXT_MESSAGE,
            id = id,
            fileName = "",
            serverFilePath = "",
            status = MessageStatusEnum.MSG_SENDING_IN_PROGRESS.tag
        )
        messageListInMVM.add(tempMessage)
        addNewLocalMessage.value = id
        sendLocalTextMessageToDataChannel.value = id
    }

    fun processNewLocalFileMessage(
        fileName: String,
        serverFilePath: String,
        id: Long,
        isBeforeFileUpload: Boolean,
        localFilePath: String
    ) {
        Log.d(TAG, "processNewLocalFileMessage: ")
        if (isBeforeFileUpload) {
            Log.d(TAG, "processNewLocalFileMessage: is before file upload")
            /*before file upload create and have it in the local..do not send to data channel */
            var tempMessage = MessageModel(
                userName = "Local",
                messageText = "",
                isLocalMessage = true,
                messageType = VCConstants.FILE_MESSAGE,
                id = id,
                fileName = fileName,
                serverFilePath = serverFilePath,
                status = MessageStatusEnum.MSG_SENDING_IN_PROGRESS.tag
            )
            tempMessage.localFilePath = localFilePath
            messageListInMVM.add(tempMessage)
            addNewLocalMessage.value = id
            sendLocalFileMessageToDataChannel.value = -1//to be careful here -> just update it locally
        } else {
            Log.d(TAG, "processNewLocalFileMessage: after file upload")
            /*find the file message using message ID and send it to data channel only*/
            val resultPair = getMessageFromId(id)
            if (resultPair.second != -1) {
                /*found*/
                Log.d(TAG, "processNewLocalFileMessage: message found")
                sendLocalFileMessageToDataChannel.value = id
            }
        }
    }

    /*06 Nov 2023 - Upload file API*/
    var TOTAL_RETRIES = 3

    /*07 Nov 2023:: IMP::nahusha help required here::to pass correct data for vc room, who and usertype*/
    fun uploadVcFileAPICall(
        file: File,
        vcRoom: String,
        userType: String,
        who: String,
        messageId: Long,
        forRetryProcess: Boolean
    ) {
        Log.d(TAG, "uploadVcFileAPICall:  ")
        val requestFile = RequestBody.create("*/*".toMediaTypeOrNull(), file)
        val fileData = MultipartBody.Part.createFormData("file", file.name, requestFile)
        var retryCount = 0

        viewModelScope.launch {
            /*set status as UPLOAD_PROGRESS */
            updateUploadStatusForFileMessage(
                serverFileURL = "",
                msgID = messageId,
                status = MessageStatusEnum.FILE_UPLOAD_PROGRESS.tag
            )


            repository.doUploadVCAPICall(
                file = fileData,
                vc_room = vcRoom,
                user_type = userType,
                who = who,
                appVersion = "1.0"
            ).retry(TOTAL_RETRIES.toLong())
                .catch { e ->
                    Log.d(TAG, "uploadVcFileAPICall: failure")

                    /*update the message ID as "UPLOAD_FAILED*/
                    updateUploadStatusForFileMessage(
                        serverFileURL = "",
                        msgID = messageId,
                        status = MessageStatusEnum.FILE_UPLOAD_FAILURE.tag
                    )
                    toastMessage.value = e.cause?.let { getErrorMessage(it) }

                }.collect {
                    Log.d(TAG, "uploadVcFileAPICall: success")
                    /*update the message ID as "UPLOAD_SUCCESS*/
                    updateUploadStatusForFileMessage(
                        serverFileURL = it.file.toString(),
                        msgID = messageId,
                        status = MessageStatusEnum.FILE_UPLOAD_SUCCESS.tag
                    )
                    if (forRetryProcess) {
                        processResendLocalFileMessage(
                            fileName = file.name,
                            serverFilePath = it.file.toString(),
                            msgID = messageId,
                            isBeforeFileUpload = false
                        )
                    } else {
                        processNewLocalFileMessage(
                            fileName = file.name,
                            serverFilePath = it.file.toString(),
                            id = messageId,
                            isBeforeFileUpload = false,
                            localFilePath = ""
                        )
                    }

                }

        }


    }

    private fun getErrorMessage(t: Throwable): String {
        var errorMessage = ""
        errorMessage = when (t) {
            is SocketTimeoutException -> "Timeout, Please try again with proper Internet Connection"
            is UnknownHostException -> "Unable to connect server, Please try again with proper Internet Connection"
            is MalformedURLException -> "Network error: Bad Url, Please try again"
            is SocketException -> "Network error: Socket, Please try again"
            is RemoteException -> "Network error, Please try again"
            is ConnectException -> "Network error: Connect, Please try again"
            //testing purpose, to be commented after testing
//            else -> errorMessage = "Network error: $t"
            //testing purpose, to be uncommented after testing
            else -> "Server Busyb"
        }
        if (errorMessage.isEmpty()) {
            errorMessage = "Server Busyc"
        }
        Log.d(TAG, "getErrorMessage: $errorMessage")
        return errorMessage
    }

    fun updateSendStatusForMessage(msgID: Long, successful: Boolean) {
        val resultPair = getMessageFromId(msgID)
        if (resultPair.second != -1) {
            /*replace this message with updated message*/
            Log.d(TAG, "updateSendStatusForMessage: message found")
            var foundMsg = messageListInMVM[resultPair.second]
            foundMsg.status =
                if (successful) MessageStatusEnum.MSG_SENT_SUCCESS.tag else MessageStatusEnum.MSG_SENT_FAILURE.tag
            messageListInMVM[resultPair.second] = foundMsg
            updateLocalMessage.value = msgID
        }
    }

    private fun updateUploadStatusForFileMessage(
        serverFileURL: String,
        msgID: Long,
        status: String
    ) {
        Log.d(TAG, "updateUploadStatusForFileMessage: status -> ${status}")
        val resultPair = getMessageFromId(msgID)
        if (resultPair.second != -1) {
            /*replace this message with updated message*/
            Log.d(TAG, "updateUploadStatusForFileMessage: message found")
            var foundMsg = messageListInMVM[resultPair.second]
            foundMsg.status = status
            foundMsg.serverFilePath = serverFileURL
            messageListInMVM[resultPair.second] = foundMsg
            updateLocalMessage.value = msgID
        }
    }

    fun updateDownloadStatusForFileMessage(
        downloadReferenceId: Long,
        serverFileURL: String,
        msgID: Long,
        status: String
    ) {
        Log.d(TAG, "updateUploadStatusForFileMessage: status -> ${status}")
        val resultPair = getMessageFromId(msgID)
        if (resultPair.second != -1) {
            /*repalce this messgae with updated message*/
            Log.d(TAG, "updateUploadStatusForFileMessage: message found")
            var foundMsg = messageListInMVM[resultPair.second]
            foundMsg.status = status
            foundMsg.serverFilePath = serverFileURL
            foundMsg.downloadReferenceId = downloadReferenceId
            messageListInMVM[resultPair.second] = foundMsg
            updateLocalMessage.value = msgID
        }
    }

    fun getMessageFromId(msgID: Long): Pair<MessageModel?, Int> {
        Log.d(TAG, "getMessageFromId:")
        var msgIndex = -1
        for (i in messageListInMVM.indices) {
            if (messageListInMVM[i].id == msgID) {
                msgIndex = i
                break;
            }
        }
        return if (msgIndex != -1) {
            Pair(messageListInMVM[msgIndex], msgIndex)
        } else {
            Pair(null, -1)
        }

    }

    fun processResendLocalTextMessage(msgID: Long, data: MessageModel) {
        val resultPair = getMessageFromId(msgID)
        if (resultPair.second != -1) {
            /*replace this message with updated message*/
            Log.d(TAG, "processResendLocalTextMessage: message found")
            var foundMsg = messageListInMVM[resultPair.second]
            foundMsg.status = MessageStatusEnum.MSG_SENDING_IN_PROGRESS.tag
            messageListInMVM[resultPair.second] = foundMsg
            updateLocalMessage.value = msgID
            sendLocalTextMessageToDataChannel.value = msgID
        }
    }

    fun processResendLocalFileMessage(
        fileName: String,
        serverFilePath: String,
        msgID: Long,
        isBeforeFileUpload: Boolean
    ) {

        if (isBeforeFileUpload) {
            Log.d(TAG, "processResendLocalFileMessage: is before file upload")
            /*before file upload update and have it in the local..do not send to data channel */
            val resultPair = getMessageFromId(msgID)
            if (resultPair.second != -1) {
                var foundMsg = messageListInMVM[resultPair.second]
                foundMsg.status = MessageStatusEnum.MSG_SENDING_IN_PROGRESS.tag
                messageListInMVM[resultPair.second] = foundMsg
                updateLocalMessage.value = msgID
                sendLocalFileMessageToDataChannel.value =
                    -1//to be careful here -> just update it locally
            }
        } else {
            Log.d(TAG, "processNewLocalFileMessage: after file upload")
            /*find the file message using message ID and send it to data channel only*/
            val resultPair = getMessageFromId(msgID)
            if (resultPair.second != -1) {
                updateLocalMessage.value = msgID
                sendLocalFileMessageToDataChannel.value =
                    msgID//to be careful here -> just update it locally
            }
        }
    }

    var messageBadgeValue = 0
    var messageUnreadCount = MutableLiveData<String>()

    fun incrementMessageBadgeValue(){
        messageBadgeValue++
        messageUnreadCount.value = "${messageBadgeValue}"
        Log.d(TAG, "incrementMessageBadgeValue: "+messageBadgeValue)
    }
    fun clearMessageBadgeValue(){
        messageBadgeValue = 0
        messageUnreadCount.value = ""
        Log.d(TAG, "clearMessageBadgeValue: ")
    }
}