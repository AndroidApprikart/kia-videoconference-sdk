package com.app.vc

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.app.vc.models.MessageModel
import com.app.vc.models.ParticipantsModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.antmedia.webrtcandroidframework.apprtc.AppRTCAudioManager
import org.webrtc.VideoTrack
import javax.inject.Inject
@HiltViewModel
class MainViewModel @Inject constructor(): ViewModel() {

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
    fun trackFunction() {
        for( k  in tracks)
        {
            k
        }
    }

    var isInitialConferenceStarted = false
    var internetSpeed = MutableLiveData<String>()
    var reconnectAttemptCount = 0
    var rejoinInProgress = false

    var roomInfoStreamsList = ArrayList<String>()

    var addNewRemoteMessage = MutableLiveData<Boolean>()
    var addNewLocalMessage = MutableLiveData<Boolean>()
    var updateLocalMessage = MutableLiveData<Boolean>()

    var newLocalMessage = MessageModel()
    var newRemoteMessage = MessageModel()

    var sendLocalTextMessageToDataChannel = MutableLiveData<Boolean>()
    var sendLocalFileMessageToDataChannel = MutableLiveData<Boolean>()

    fun processNewLocalTextMessage(userInputText: String){
        var tempMessage = MessageModel(
            "Local",
            userInputText.trim().toString(),
            true,
            VCConstants.TEXT_MESSAGE,
            AndroidUtils.getCurrentTimeInMill(),
            "",
            ""
        )
        newLocalMessage = tempMessage
        sendLocalTextMessageToDataChannel.value = true
        messageListInMVM.add(tempMessage)
        addNewLocalMessage.value = true
        addNewLocalMessage.value = false /*to stop re observing when message fragment obserign it*/
    }

    fun processNewLocalFileMessage(fileName:String, serverFilePath:String){
        var tempMessage = MessageModel(
            "Local",
            "",
            true,
            VCConstants.FILE_MESSAGE,
            AndroidUtils.getCurrentTimeInMill(),
            fileName,
            serverFilePath
        )
        newLocalMessage = tempMessage
        sendLocalFileMessageToDataChannel.value = true
        messageListInMVM.add(tempMessage)
        addNewLocalMessage.value = true
        addNewLocalMessage.value = false /*to stop re observing when message fragment obserign it*/
    }


    var messageFragmentClose = MutableLiveData<Boolean>()
    var openFileManager = MutableLiveData<Boolean>()

    var messageListInMVM = ArrayList<MessageModel>()

    var startScreenShare = MutableLiveData<Boolean>()
    var stopScreenShare = MutableLiveData<Boolean>()

    var frontCamera = true

    /*keep track of fragment viewing*/
    var participantFragVisible = false
    var messageFragVisible= false
    var soundDeviceFragVisible = false
    var screenShareFragVisible = false

}