package com.app.vc

import android.os.RemoteException
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.app.vc.models.MessageModel
import com.app.vc.models.ParticipantsModel
import com.app.vc.message.ChatModelItem
import com.app.vc.message.EstimateModel
import com.app.vc.message.RequestModelOpenEstimate
import com.app.vc.message.RequestModelUpdateEstimationStatus
import com.app.vc.message.ResponseModelEstimateData
import com.app.vc.message.ResponseModelUpdateEstimateStatus
import com.app.vc.message.ResponseModelUpdateEstimationStatus
import com.app.vc.models.DisplayNameResponse
import com.app.vc.models.MessageStatusEnum
import com.app.vc.models.ModifiedResponseUpdateVcStatus
import com.app.vc.models.RequestModelUpdateVcStatusCustomer
import com.app.vc.models.ResponseModelUpdateVideoStatus
import com.app.vc.models.UpdateStreamIdResponse
import com.app.vc.models.UploadVcFileResponse
import com.app.vc.models.ValidateVcResponse
import com.app.vc.models.VcConfigurationResponse
import com.app.vc.models.login.RequestModelLogin
import com.app.vc.models.login.ResponseModelLogin
import com.app.vc.utils.ApiDetails
import com.app.vc.utils.ApiInterface
import com.app.vc.network.ResponseModelDeleteBroadcast
import com.app.vc.utils.PreferenceManager
import com.app.vc.network.RetrofitClient
import com.app.vc.utils.VCConstants
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kia.vc.message.RequestModelSendUserManual
import com.kia.vc.message.ResponseModelSendUserManual
import io.antmedia.webrtcandroidframework.apprtc.AppRTCAudioManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.webrtc.VideoTrack
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.net.ConnectException
import java.net.MalformedURLException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class MainViewModel : ViewModel() {
    var repository = DataRepository()
    var TAG = "MAINVM::"
    var serverUrl: String = "ws://vc.apprikart.com:5080/WebRTCAppEE/websocket"
    var streamId: String? = null
    var userType = VCConstants.UserType.CUSTOMER.value
    var toastMessage = MutableLiveData<String>()
    var testUserType: String? = null
    var roomID: String? = null
    var serviceAdvisorID: String? = null
    var customerCode: String? = null
    var roNo: String? = null
    var dealerCode: String? = null
    var userName: String? = null
    var displayName: String? = null


    var userId: String? = null
    var password: String? = null
    var deviceToken: String? = null
    var vcEndTime: String? = null
    var kecName: String? = null

    var meetingPasscode: String? = null

    var callType:String? = null
    var customerName:String? = null
    var customerPhoneNumber:String? = null

    var baseURL = ""
    private fun getRetrofitServiceClient(baseURL: String) =
        RetrofitClient().getRetrofitClient(baseURL).create(
            ApiInterface::class.java
        )


    var kiaApprikartRetrofitClient = RetrofitClient().getRetrofitClient(ApiDetails.BASE_URL).create(
        ApiInterface::class.java
    )

    var initialConfigurationSucess = false

    //    var isProgressVisible = MutableLiveData<Boolean>() //removed and replaced with normal function call
    var isServiceStarted = false//used just for testing


    var endVCByUser = false

    var streams = ArrayList<String>()

    var tracks = ArrayList<VideoTrack>()

    var participants = ArrayList<ParticipantsModel>()
    var localAudio = true
    var localVideo = true
    var grid = true
    var bottomSheet = true


    var updateParticipants = MutableLiveData<Boolean>()
    var updateParticipantsNameUI = MutableLiveData<Boolean>() //to update the display name in UI
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
    var sendLocalEstimationMessageToDataChannel = MutableLiveData<Long>()

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

    var isProgressBarVisible = MutableLiveData<Boolean>()

    var getEstimationDetails = MutableLiveData<Boolean>()
    var tempEstimateModel:ResponseModelEstimateData? = null
    var estimateDetailsResponse = MutableLiveData<ResponseModelEstimateData>()

    var estimateDetailsAfterApproval: ResponseModelEstimateData? =null
    var tempMessageModelAfterApprovalOrReject: MessageModel? = null
    var tempParentPosition: Int? = null
    var updateEstimateStatus = MutableLiveData<Boolean>()
    var selectedPartList:String? = ""
    var selectedLabourList: String?  = ""
    var isSuccessEstimationResponse = MutableLiveData<Boolean>()


    var saveMessageList  =  MutableLiveData<kotlin.collections.ArrayList<ChatModelItem>>()
    var isInPhoneCall = MutableLiveData<Boolean>(false)
    var isPhoneCallEnded = MutableLiveData<Boolean>()
    var isPhoneCallStarted = MutableLiveData<Boolean>()

    //dev mode
    var isDevMode:Boolean = false

    var isRejoinClicked = false
    var tempRoomInfo = ArrayList<String>()
    var unwantedStreams = ArrayList<String>()

    var tempStreamIdToClearUI:String? = null

    //Added 09Jan2024
    var apiCallToSendWelcomeMessage = MutableLiveData<Boolean>(false)
    var isSendWelcomeMessageEnabled = MutableLiveData<Boolean>(true)
    var sendUserManualResponse = MutableLiveData<ResponseModelSendUserManual>()


    fun makeApiCallToSendWelcomeMessage(baseUrl: String) {
        var requestObject = RequestModelSendUserManual(
            customerName = customerName!!,
            dealer_no = dealerCode!!,
            mobileNo = customerPhoneNumber!!
        )
        val call = getServiceObject(baseUrl).sendUserManual(PreferenceManager.getEstimateToken()!!,requestObject)
        call.enqueue(object : Callback<ResponseModelSendUserManual?> {
            override fun onResponse(
                call: Call<ResponseModelSendUserManual?>,
                response: Response<ResponseModelSendUserManual?>
            ) {
                if(response.code() in 200..299) {
                    if(response.body()!=null) {
                        sendUserManualResponse.value = response.body()
                    }else {
                        isSendWelcomeMessageEnabled.value = true
                        toastMessage.value = "Something went wrong. null response. sendUserManual"
                    }
                }else {
                    isSendWelcomeMessageEnabled.value = true
                    toastMessage.value = "Something went wrong. response code. sendUserManual"
                }
            }

            override fun onFailure(call: Call<ResponseModelSendUserManual?>, t: Throwable) {
                isSendWelcomeMessageEnabled.value = true
                toastMessage.value = "Something went wrong. Failure. Send User Manual"
                toastMessage.value = "Failure: localizedMessage ${t.message}"
            }
        })
    }

    /*message handling functions*/
    fun processNewLocalTextMessage(userInputText: String, id: Long) {
        Log.d(TAG, "processNewLocalTextMessage: ")
        val tempMessage = MessageModel(
            userName = displayName!!, //nahusha help
            messageText = userInputText.trim().toString(),
            isLocalMessage = true,
            messageType = VCConstants.TEXT_MESSAGE,
            id = id,
            fileName = "",
            serverFilePath = "",
            status = MessageStatusEnum.MSG_SENDING_IN_PROGRESS.tag,
            estimationDetails = null
        )
        messageListInMVM.add(tempMessage)
        addNewLocalMessage.value = id
        sendLocalTextMessageToDataChannel.value = id
    }

    fun processLocalEstimationMessage(estimationDetails: ResponseModelEstimateData, id:Long) {
        val tempEstimation = MessageModel(
            userName = displayName!!, //nahusha help
            messageText = "",
            isLocalMessage = true,
            messageType = VCConstants.ESTIMATION_MESSAGE,
            id = id,
            fileName = "",
            serverFilePath = "",
            status = MessageStatusEnum.MSG_SENDING_IN_PROGRESS.tag,
            estimationDetails = estimationDetails
        )
        messageListInMVM.add(tempEstimation)
        addNewLocalMessage.value = id
        sendLocalEstimationMessageToDataChannel.value = id

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
                userName = displayName!!, //nahusha help,
                messageText = "",
                isLocalMessage = true,
                messageType = VCConstants.FILE_MESSAGE,
                id = id,
                fileName = fileName,
                serverFilePath = serverFilePath,
                status = MessageStatusEnum.MSG_SENDING_IN_PROGRESS.tag,
                estimationDetails = null
            )
            tempMessage.localFilePath = localFilePath
            messageListInMVM.add(tempMessage)
            addNewLocalMessage.value = id
            sendLocalFileMessageToDataChannel.value =
                -1//to be careful here -> just update it locally
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

    fun uploadVcFileAPICallNew(
        file: File,
        vcRoom: String,
        userType: String,
        who: String,
        messageId: Long,
        forRetryProcess: Boolean
    ) {
        Log.d(TAG, "uploadVcFileAPICallNew:  ")
        val requestFile = RequestBody.create("*/*".toMediaTypeOrNull(), file)
        val fileData = MultipartBody.Part.createFormData("file", file.name, requestFile)
        var retryCount = 0
        val call = getRetrofitServiceClient(ApiDetails.BASE_URL).uploadVcFile(
            file = fileData,
            vc_room = vcRoom,
            user_type = userType,
            who = who,
            VCConstants.version
        )
        /*set status as UPLOAD_PROGRESS */
        updateUploadStatusForFileMessage(
            serverFileURL = "",
            msgID = messageId,
            status = MessageStatusEnum.FILE_UPLOAD_PROGRESS.tag
        )
        call.enqueue(object : retrofit2.Callback<UploadVcFileResponse> {
            override fun onFailure(call: Call<UploadVcFileResponse>, t: Throwable) {
                Log.d(TAG, "${t.message}")
                Log.d(TAG, "onFailure: uploadFile: Failure ${t.cause}")
                Log.d(TAG, "onFailure: uploadFile: Failure ${t.localizedMessage}")
                if (retryCount++ < TOTAL_RETRIES) {
                    Log.d(TAG, "onFailure: Retrying... $retryCount out of $TOTAL_RETRIES")
                    retry()
                } else {
                    retryCount = 0
                    try {
                        /*update the message ID as "UPLOAD_FAILED*/
                        updateUploadStatusForFileMessage(
                            serverFileURL = "",
                            msgID = messageId,
                            status = MessageStatusEnum.FILE_UPLOAD_FAILURE.tag
                        )
                        toastMessage.value = t?.let { getErrorMessage(t) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        //testing purpose, to be commented after testing
//                        data.value = Resource.error("Server Busy: ${e.toString()}")
                        //testing purpose, to be uncommented after testing
                        toastMessage.value =
                            "Something went wrong while sending the file. Please try after some time"
                        /*update the message ID as "UPLOAD_FAILED*/
                        updateUploadStatusForFileMessage(
                            serverFileURL = "",
                            msgID = messageId,
                            status = MessageStatusEnum.FILE_UPLOAD_FAILURE.tag
                        )
                    }
                }
            }

            override fun onResponse(
                call: Call<UploadVcFileResponse>,
                response: Response<UploadVcFileResponse>
            ) {
                Log.d(TAG, "onResponse: uploadFile: Success: ")
                response?.body()?.let { it ->
                    if (response.code() in 200..299) {
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
                    } else {
                        /*update the message ID as "UPLOAD_FAILED*/
                        updateUploadStatusForFileMessage(
                            serverFileURL = "",
                            msgID = messageId,
                            status = MessageStatusEnum.FILE_UPLOAD_FAILURE.tag
                        )
                        toastMessage.value = response.errorBody().toString()
                    }
                }
            }

            fun retry() {
                call.clone().enqueue(this)
            }
        })


    }

    private fun getErrorMessage(t: Throwable): String {
        Log.d(TAG, "getErrorMessage: t-${Gson().toJson(t)}")
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
            is HttpException -> "exception"
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

    fun incrementMessageBadgeValue() {
        messageBadgeValue++
        messageUnreadCount.value = "${messageBadgeValue}"
        Log.d(TAG, "incrementMessageBadgeValue: " + messageBadgeValue)
    }

    fun clearMessageBadgeValue() {
        messageBadgeValue = 0
        messageUnreadCount.value = ""
        Log.d(TAG, "clearMessageBadgeValue: ")
    }

    /*validate vc API's*/
    var validateVCResponse = MutableLiveData<ValidateVcResponse>()

    fun validateVcForCustomer(roomId: String, pass: String, userType: String) {
        Log.d(TAG, "validateVcForCustomer:  ")
        val call = kiaApprikartRetrofitClient.validateVcForCustomer(
            room = roomId,
            authPasscode = pass,
            userType = userType,
            appVersion = VCConstants.version
        )
        call.enqueue(object : Callback<ValidateVcResponse> {
            override fun onFailure(call: Call<ValidateVcResponse>, t: Throwable) {
                toastMessage.value = getErrorMessage(t)
                validateVCResponse.value = ValidateVcResponse("failed", getErrorMessage(t))
            }

            override fun onResponse(
                call: Call<ValidateVcResponse>,
                response: Response<ValidateVcResponse>
            ) {
                Log.d(TAG, "onResponse: validateVcForCustomer: ${response.body()}")
                if (response.code() in 200..299) {
                    if (response.body() != null) {
                        validateVCResponse.value = response.body()
                    } else {
                        validateVCResponse.value = ValidateVcResponse(
                            "failed",
                            "Something went wrong."
                        )
                    }
                } else {
                    validateVCResponse.value = ValidateVcResponse("failed", "Something went wrong.")
                }
            }

        })

    }

    fun validateVcForServicePerson(
        roomId: String, pass: String, userType: String, service_id: String
    ) {
        Log.d(TAG, "validateVcForServicePerson:  ")
        val call = kiaApprikartRetrofitClient.validateVcForServicePerson(
            room = roomId, authPasscode = pass,
            userType = userType,
            servicePersonId = service_id,
            appVersion = VCConstants.version
        )
        call.enqueue(object : Callback<ValidateVcResponse> {
            override fun onFailure(call: Call<ValidateVcResponse>, t: Throwable) {
                toastMessage.value = "Something went wrong"
                validateVCResponse.value = ValidateVcResponse("failed", getErrorMessage(t))
            }

            override fun onResponse(
                call: Call<ValidateVcResponse>,
                response: Response<ValidateVcResponse>
            ) {
                Log.d(TAG, "onResponse: validateVcForServicePerson: ${response.body()}")
                if (response.code() in 200..299) {
                    if (response.body() != null) {
                        validateVCResponse.value = response.body()
                    } else {
                        validateVCResponse.value =
                            ValidateVcResponse("failed", "Something went wrong.")
                    }
                } else {
                    validateVCResponse.value = ValidateVcResponse("failed", "Something went wrong.")
                }
            }

        })
    }

    var vcConfigurationResponse = MutableLiveData<VcConfigurationResponse>()

    fun getVCConfiguration(roomId: String) {
        Log.d(TAG, "getVCConfiguration:  ")
        val call = kiaApprikartRetrofitClient.getVcConfiguration(
            room = roomId,
            who = "Android",
            userType = "kec",
            appVersion = VCConstants.version
        )
        call.enqueue(object : Callback<VcConfigurationResponse> {
            override fun onFailure(call: Call<VcConfigurationResponse>, t: Throwable) {
                toastMessage.value = getErrorMessage(t)
                vcConfigurationResponse.value =
                    VcConfigurationResponse(null, "failure", getErrorMessage(t))
            }

            override fun onResponse(
                call: Call<VcConfigurationResponse>,
                response: Response<VcConfigurationResponse>
            ) {
                Log.d(TAG, "onResponse: getVCConfiguration: ${response.body()}")
                if (response.code() in 200..299) {
                    if (response.body() != null) {
                        vcConfigurationResponse.value = response.body()
                    } else {
                        vcConfigurationResponse.value =
                            VcConfigurationResponse(null, "failure", "Something went wrong. ")
                    }
                } else {
                    vcConfigurationResponse.value =
                        VcConfigurationResponse(null, "failure", "Something went wrong.")
                }
            }

        })


    }

    var isLocalStreamIdUpdated = false
    private var updateStreamIdResponse = MutableLiveData<UpdateStreamIdResponse>()

    fun updateStreamIdInServerAPICall(
        displayName: String,
        roomId: String,
        userType: String,
        streamId: String,
        version: String
    ) {
        Log.d(TAG, "updateStreamIdInServerAPICall:  ")
        var retryCount = 0
        val call = kiaApprikartRetrofitClient.updateStreamIdInServer(
            displayName = displayName,
            streamId = streamId,
            roomId = roomId,
            userType = userType,
            appVersion = version
        )
        call.enqueue(object : Callback<UpdateStreamIdResponse> {
            override fun onFailure(call: Call<UpdateStreamIdResponse>, t: Throwable) {
                if (retryCount++ < TOTAL_RETRIES) {
                    Log.d(TAG, "onFailure: Retrying... $retryCount out of $TOTAL_RETRIES")
                    retry()
                } else {
                    retryCount = 0
                    toastMessage.value = "Something went wrong. Failure."
                }
            }

            override fun onResponse(
                call: Call<UpdateStreamIdResponse>,
                response: Response<UpdateStreamIdResponse>
            ) {
                Log.d(TAG, "onResponse: updateStreamIdInServerAPICall: ${response.body()}")
                if (response.code() in 200..299) {
                    if (response.body() != null) {
                        updateStreamIdResponse.value = response.body()

                        updateStreamIdResponse.value?.let {
                            if (it.status.isNullOrBlank()) {
                                if (it.apiErrorMessage.isNullOrBlank()) {
                                    toastMessage.value = "Server busy!"
                                } else {
                                    toastMessage.value = it.apiErrorMessage.toString()
                                }
                                if (!isLocalStreamIdUpdated) {
                                    //nahusha help :: be careful here..there will be retrying of the network call done here
//                                        retry()
                                }
                            } else {
                                if (it.status.equals("success", true)) {
                                    isLocalStreamIdUpdated = true
                                    getDisplayNameForStreamId(roomId, streamId, VCConstants.version)
                                } else if (!isLocalStreamIdUpdated) {
                                    //nahusha help :: be careful here..there will be retrying of the network call done here
//                                    retry()
                                }
                            }
                        }
                    } else {
                        toastMessage.value = "Something went wrong."
                    }
                } else {
                    toastMessage.value = "Something went wrong."
                }
            }

            fun retry() {
                call.clone().enqueue(this)
            }
        })

    }

    private var getDisplayNameResponse = MutableLiveData<DisplayNameResponse>()

    fun getDisplayNameForStreamId(roomId: String, streamId: String, version: String) {
        Log.d(TAG, "getDisplayNameForStreamId:  ")
        val call = kiaApprikartRetrofitClient.getDisplayName(
            roomId,
            streamId,
            version
        )
        call.enqueue(object : Callback<DisplayNameResponse> {
            override fun onFailure(call: Call<DisplayNameResponse>, t: Throwable) {
                toastMessage.value = "Something went wrong. Failure."
                getDisplayNameResponse.value = DisplayNameResponse(null, null, null)
            }

            override fun onResponse(
                call: Call<DisplayNameResponse>,
                response: Response<DisplayNameResponse>
            ) {
                Log.d(TAG, "onResponse: getDisplayNameForStreamId: ${response.body()}")
                if (response.code() in 200..299) {
                    if (response.body() != null) {
                        getDisplayNameResponse.value = response.body()
                        getDisplayNameResponse.value?.let {
                            processGetDisplayNameResponse(
                                roomId, streamId,
                                it
                            )
                        }
                    } else {
                        getDisplayNameResponse.value = DisplayNameResponse(null, null, null)
                    }
                } else {
                    getDisplayNameResponse.value = DisplayNameResponse(null, null, null)
                }
            }

        })
    }

    private fun processGetDisplayNameResponse(
        roomId: String,
        stream_Id: String,
        displayData: DisplayNameResponse
    ) {
        Log.d(
            TAG,
            "processGetDisplayNameResponse: stream_id ->${stream_Id} :: streamID -> ${streamId} ::displayName -> ${displayData.displayName}"
        )
        if (stream_Id.equals(streamId)) {
            /*this is local participant*/
            /*do not update..ignore*/
            var resultPair = getLocalParticipant()
            if (resultPair.second != -1) {
                /*update this participants display name message*/
                Log.d(TAG, "processGetDisplayNameResponse: localParticipant found found")
                var foundParticipant = participants[resultPair.second]
                foundParticipant.displayName = displayData.displayName ?: stream_Id
                participants[resultPair.second] = foundParticipant
                Log.d(
                    TAG,
                    "processGetDisplayNameResponse: particiapnts -> ${Gson().toJson(participants)}"
                )
                updateParticipants.value = true // updating display Name from api
                updateParticipantsNameUI.value = true
            }
            return
        }
        Log.d(TAG, "processGetDisplayNameResponse: -> ")
        /*just extra comparision ..can be commented*/
        var resultPair = getParticipantForStream(stream_Id)
        if (resultPair.second != -1) {
            /*update this participants display name message*/
            Log.d(TAG, "processGetDisplayNameResponse: participant found")
            var foundParticipant = participants[resultPair.second]
            foundParticipant.displayName = displayData.displayName ?: stream_Id
            participants[resultPair.second] = foundParticipant
            Log.d(
                TAG,
                "processGetDisplayNameResponse: particiapnts -> ${Gson().toJson(participants)}"
            )
            updateParticipants.value = true // updating display Name from api
            updateParticipantsNameUI.value = true
        }
    }

    private fun getParticipantForStream(stream_Id: String): Pair<ParticipantsModel?, Int> {
        Log.d(TAG, "getParticipantForStream:")
        var participantIndex = -1
        for (i in participants.indices) {
            Log.d(
                TAG,
                "getParticipantForStream: trackID -> ${participants[i].trackId} :;streamId -> ${stream_Id}"
            )
            if (participants[i].trackId.contains(stream_Id)) {
                participantIndex = i
                break;
            }
        }
        return if (participantIndex != -1) {
            Pair(participants[participantIndex], participantIndex)
        } else {
            Pair(null, -1)
        }
    }

    private fun getLocalParticipant(): Pair<ParticipantsModel?, Int> {
        Log.d(TAG, "getLocalParticipant:")
        var participantIndex = -1
        for (i in participants.indices) {
            if (participants[i].isLocal) {
                participantIndex = i
                break;
            }
        }
        return if (participantIndex != -1) {
            Pair(participants[participantIndex], participantIndex)
        } else {
            Pair(null, -1)
        }
    }

    var loginResponse = MutableLiveData<ResponseModelLogin>()

    fun doLogin(requestObject: RequestModelLogin) {
        val call = kiaApprikartRetrofitClient.login(requestObject)
        call.enqueue(object : Callback<ResponseModelLogin> {
            override fun onFailure(call: Call<ResponseModelLogin>, t: Throwable) {
                toastMessage.value = "Something went wrong. Failure"
                loginResponse.value = ResponseModelLogin(null, null, null, "failure", false)
            }

            override fun onResponse(
                call: Call<ResponseModelLogin>,
                response: Response<ResponseModelLogin>
            ) {
                Log.d(TAG, "onResponse: doLogin: ${response.body()}")
                if (response.code() in 200..299) {
                    if (response.body() != null) {
                        loginResponse.value = response.body()
                    } else {
                        toastMessage.value = "Something went wrong."
                        loginResponse.value = ResponseModelLogin(null, null, null, "failure", false)
                    }
                } else {
                    toastMessage.value = "Something went wrong"
                    loginResponse.value = ResponseModelLogin(null, null, null, "failure", false)
                }
            }

        })
    }

    var updateVcStatusResponse = MutableLiveData<ModifiedResponseUpdateVcStatus>()
    fun updateVCStatusForCustomerNew(
        baseUrl: String,
        requestObject: RequestModelUpdateVcStatusCustomer
    ) {
        Log.d(TAG, "updateVCStatusForCustomer: requestObject: ${requestObject}")
        val call = getServiceObject(baseUrl).updateVcStatusForCustomerNew(requestObject)
        call.enqueue(object : Callback<ResponseModelUpdateVideoStatus?> {
            override fun onResponse(
                call: Call<ResponseModelUpdateVideoStatus?>,
                response: Response<ResponseModelUpdateVideoStatus?>
            ) {
                if (response.body() != null) {
                    if (response.code() in 200..299) {
                        updateVcStatusResponse.value = ModifiedResponseUpdateVcStatus(
                            response.body()!!,
                            true
                        )
                        Log.d(TAG, "updateVCStatusForCustomer: onResponse: ${response.body()}")
                    } else {
                        Log.d(TAG, "onResponse: updateVCStatusForCustomer: else: !200.299")
                        isProgressBarVisible.value = false
                        updateVcStatusResponse.value = ModifiedResponseUpdateVcStatus(
                            response.body(),
                            false
                        )
                        toastMessage.value = "Something went wrong.responseCode.UpdateVcStatus"
                    }
                } else {
                    updateVcStatusResponse.value = ModifiedResponseUpdateVcStatus(
                        null,
                        false
                    )
                    isProgressBarVisible.value = false
                    toastMessage.value = "Null Response.UpdateVCStatus"
                }

            }

            override fun onFailure(call: Call<ResponseModelUpdateVideoStatus?>, t: Throwable) {
                Log.d(TAG, "onFailure: updateVCStatusForCustomer: ")
                toastMessage.value = "Something went wrong.Failure.UpdateVcStatus"
                isProgressBarVisible.value = false
                updateVcStatusResponse.value = ModifiedResponseUpdateVcStatus(
                    null,
                    false
                )
                Log.d(TAG, "onFailure: updateVCStatusForCustomer: message: ${t.message}")
                Log.d(TAG, "onFailure: updateVCStatusForCustomer: message: ${t.localizedMessage}")
                Log.d(TAG, "onFailure: updateVCStatusForCustomer: message: ${t.cause}")
                Log.d(TAG, "onFailure: updateVCStatusForCustomer: message: ${t.printStackTrace()}")
            }
        })
    }


    fun getEstimationDetailsNew(baseUrl:String,id:Long) {
        Log.d(TAG, "getEstimationDetails: ")
//        var requestObject = RequestModelOpenEstimate("R202300090")
        var requestObject = RequestModelOpenEstimate(roNo.toString())

        val call = getServiceObject(baseUrl).getEstimationListNew(PreferenceManager.getEstimateToken()!!,requestObject)
//        val call = service.sendEstimation(staticEstimationToken,requestObject)
        Log.d(TAG,"Estimation call"+call.request())

        call.enqueue(object : Callback<EstimateModel> {
            override fun onFailure(call: Call<EstimateModel>, t: Throwable) {
                Log.d(TAG, "testEstimation: onFailure: Estimation ${t.message.toString()}")
                toastMessage.value = "Something went wrong.Failure.EstimationDetails"
                toastMessage.value = t.message.toString()
                isProgressBarVisible.value= false

//                val validateVcResponse = ValidateVcResponse("error","Server error...")
//                mValidateVcResponse.value = validateVcResponse
            }

            override fun onResponse(
                call: Call<EstimateModel>,
                response: Response<EstimateModel>
            ) {
                Log.d(TAG, "testEstimation: onResponse: Estimation : ${response.body()}")
                Log.d(TAG, "testEstimation: onResponse: $response.code ")

                if(response.code() in 200..299) {
                    if(response.body()!=null) {
                        //EstimateResponse
//                        var estimateModel = MessageModel(
//                            messageText.value.toString(),
//                            kecName,
//                            true,
//                            isTextMessage = false,
//                            fileName = "",
//                            serverFilePath = null,
//                            fileLocal = null,
//                            downloadStatus = "",
//                            uploadStatus = "",
//                            downloadRefId = null,
//                            messageId = AndroidUtils.getCurrentTimeInMill(),
//                            estimateDetails = response.body()!!.data
//                        )

                        if((response.body()!!.data!=null)) {
//                            var estimateModel = MessageModel(
//                                messageText.value.toString(),
//                                displayName.toString(),
//                                true,
//                                isTextMessage = false,
//                                fileName = "",
//                                serverFilePath = null,
//                                fileLocal = null,
//                                downloadStatus = "",
//                                uploadStatus = "",
//                                downloadRefId = null,
//                                messageId = AndroidUtils.getCurrentTimeInMill(),
//                                estimateDetails = response.body()!!.data
//                            )


                            tempEstimateModel = response.body()!!.data
                            estimateDetailsResponse.value = response.body()!!.data
                        }else {
                            toastMessage.value = "Part list and labour list is empty."
                            isProgressBarVisible.value= false
                        }
                    }else {
                        isProgressBarVisible.value= false
                        toastMessage.value = "Null response from the api.EstimationDetails"
                    }
                }else {
                    isProgressBarVisible.value= false
                    toastMessage.value = "Something went wrong.responseCode.EstimationDetails"
                }
            }

        })
    }

    var updateEstimationStatusResponse = MutableLiveData<ResponseModelUpdateEstimateStatus>()
    fun updateEstimationStatusNew(
        customerCode: String,
        estimationStatus: String,
        employeeNumber: String,
        partListCodes: String,
        labourListCodes: String,
        roNumber:String,
        dealerCode:String,
        baseUrl: String
    ) {
        var requestObject = RequestModelUpdateEstimationStatus(
            customerCode =customerCode ,
            employeeNumber =employeeNumber ,
            estimationStatus = estimationStatus,
            labourListCodes = labourListCodes,
            partListCodes = partListCodes,
            roNo = roNumber,
            dealerNumber = dealerCode
        )

        Log.d(TAG, "updateEstimationStatus: requestObject: ${requestObject}")
//        val call = service.updateEstimationStatus(staticEstimationToken,requestObject)
        val call = getServiceObject(baseUrl).updateEstimationStatusNew(
            PreferenceManager.getEstimateToken()!!,
            requestObject
        )

        call.enqueue(object : Callback<ResponseModelUpdateEstimateStatus?> {
            override fun onResponse(
                call: Call<ResponseModelUpdateEstimateStatus?>,
                response: Response<ResponseModelUpdateEstimateStatus?>
            ) {
                if(response.code() in 200 .. 299) {
                    if(response.body()!=null) {
                        Log.d(TAG, "updateStatusResponse: onResponse: ")
                        updateEstimationStatusResponse.value = response.body()
                    }else {
                        isProgressBarVisible.value = false
                        toastMessage.value = "Null response. update Estimation Status."
                    }
                }else {
                    isProgressBarVisible.value = false
                    toastMessage.value = "Response Code. update Estimation Status."
                }
            }

            override fun onFailure(call: Call<ResponseModelUpdateEstimateStatus?>, t: Throwable) {
                toastMessage.value = "Failure. Update Estimation Status."
                isProgressBarVisible.value = false
                Log.d(TAG, "updateEstimationStatus: ")
                Log.d(TAG, "updateStatusResponse: onFailure: ")
            }
        })
    }


    var saveChatResponse = MutableLiveData<ResponseModelUpdateEstimationStatus>()
    fun saveChatListNew(baseUrl: String,chatList: kotlin.collections.ArrayList<ChatModelItem>) {
        Log.d(TAG, "saveChatList: ${chatList}")
        var call = getServiceObject(baseUrl).saveChatListNew(
            PreferenceManager.getEstimateToken()!!,
            chatList
        )
//        var call = service.saveChatList(
//            staticEstimationToken,
//            chatList
//        )


        call.enqueue(object : Callback<ResponseModelUpdateEstimationStatus?> {
            override fun onResponse(
                call: Call<ResponseModelUpdateEstimationStatus?>,
                response: Response<ResponseModelUpdateEstimationStatus?>
            ) {
                Log.d(TAG, "chatListResponse: success: SaveChatList: ${response.body()} ")
                if(response.code() in 200 .. 299) {
                    if(response.body()!=null) {
                        if(response.body()!!.success) {
                            saveChatResponse.value = response.body()!!
                        }
                    }
                }else {
                    isProgressBarVisible.value = false
                    Log.d(TAG, "chatListResponse: success: SaveChatList: response code ")
                }

            }

            override fun onFailure(call: Call<ResponseModelUpdateEstimationStatus?>, t: Throwable) {
                isProgressBarVisible.value = false
                Log.d(TAG, "chatListResponse: onFailure: SaveChatList ${t.message.toString()}")

            }
        })
    }

    var deleteBroadCastResponse = MutableLiveData<ResponseModelDeleteBroadcast>()


    fun deleteBroadCast(baseUrl: String,streamId:String) {
        var call = getServiceObject(baseUrl).deleteBroadcast(
            roomId = roomID!!,
            streamId = streamId
        )

        call.enqueue(object : Callback<ResponseModelDeleteBroadcast?> {
            override fun onResponse(
                call: Call<ResponseModelDeleteBroadcast?>,
                response: Response<ResponseModelDeleteBroadcast?>
            ) {
                if(response.code() in 200..299) {
                    if(response.body()!=null) {
                        unwantedStreams.remove(streamId)
                        if(response.body()!!.msg.success) {

                        }else {
                            Log.d(TAG, "onResponse: deleteBroadCast: StreamIdNotPresent in the room")
//                            toastMessage.value = "Stream Id not present in room"
                        }
                    }
                }
            }

            override fun onFailure(call: Call<ResponseModelDeleteBroadcast?>, t: Throwable) {
                toastMessage.value = "Failure.. deleteBroadcast.. ${t.message}"
                Log.d(TAG, "onFailure: deleteBroadCast:  ${t.message}")
            }
        })
    }

    var gson = GsonBuilder()
        .setLenient()
        .create()

    val okhttp = OkHttpClient().newBuilder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()
    fun getServiceObject(baseUrl: String): ApiInterface {
        var service: ApiInterface =
            Retrofit.Builder()
                .baseUrl(baseUrl)
//                .client(okhttp)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ApiInterface::class.java)

        return service
    }

}