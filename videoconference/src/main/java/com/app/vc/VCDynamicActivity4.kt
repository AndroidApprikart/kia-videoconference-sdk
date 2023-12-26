package com.app.vc


import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.view.children
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.app.vc.VCConstants.CAMERA_STATUS
import com.app.vc.VCConstants.CAM_TURNED_OFF
import com.app.vc.VCConstants.CAM_TURNED_ON
import com.app.vc.VCConstants.FILE_MESSAGE
import com.app.vc.VCConstants.MIC_MUTED
import com.app.vc.VCConstants.MIC_STATUS
import com.app.vc.VCConstants.MIC_UNMUTED
import com.app.vc.VCConstants.PARTICIPANT_FRAG
import com.app.vc.VCConstants.PERMISSIONS
import com.app.vc.VCConstants.PERMISSION_CODE
import com.app.vc.VCConstants.SCREEN_SHARE_FRAG
import com.app.vc.VCConstants.SDK_BROADCAST_AUDIO_DEVICE_UPDATE
import com.app.vc.VCConstants.SDK_BROADCAST_CAMERA_DEVICE_UPDATE
import com.app.vc.VCConstants.SDK_CUSTOM_BROADCAST_ACTION
import com.app.vc.VCConstants.SOUND_DEVICE_FRAG
import com.app.vc.VCConstants.TEXT_MESSAGE
import com.app.vc.VCConstants.UPDATE_STATUS
import com.app.vc.VCConstants.ESTIMATION_MESSAGE
import com.app.vc.VCConstants.SCREEN_SHARE_ENABLED
import com.app.vc.VCConstants.SCREEN_SHARE_DISABLED
import com.app.vc.baseui.BaseActivity
import com.app.vc.customui.RemotePeerView
import com.app.vc.databinding.ActivityVcDynamic4Binding
import com.app.vc.databinding.AlertDialogLayoutBinding
import com.app.vc.databinding.DialogCameraEnableBinding
import com.app.vc.databinding.DialogEndTrheCallBinding
import com.app.vc.databinding.LayoutDialogConfirmationBinding
import com.app.vc.databinding.PermissionsDialogLayoutBinding
import com.app.vc.databinding.ReadyToJoinDialogLayoutBinding
import com.app.vc.databinding.RejoinDialogLayoutBinding
import com.app.vc.message.MessageFragment
import com.app.vc.message.ResponseModelEstimateData
import com.app.vc.models.MessageModel
import com.app.vc.models.ParticipantsModel
import com.app.vc.models.RequestModelUpdateVcStatusCustomer
import com.app.vc.models.login.RequestModelLogin
import com.app.vc.participants.ParticipantFragment
import com.app.vc.screenshare.MediaProjectionService
import com.app.vc.screenshare.ScreenShareFragment
import com.app.vc.soundDevice.SoundDeviceFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.kia.vc.feedback.FeedbackActivity
import de.tavendo.autobahn.WebSocket
import io.antmedia.webrtcandroidframework.IDataChannelObserver
import io.antmedia.webrtcandroidframework.IWebRTCListener
import io.antmedia.webrtcandroidframework.MultitrackConferenceManager
import io.antmedia.webrtcandroidframework.PermissionCallback
import io.antmedia.webrtcandroidframework.StreamInfo
import io.antmedia.webrtcandroidframework.WebRTCClient
import io.antmedia.webrtcandroidframework.apprtc.AppRTCAudioManager.AudioDevice
import io.antmedia.webrtcandroidframework.apprtc.CallActivity
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.DataChannel
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets


class VCDynamicActivity4 : BaseActivity() {
    private lateinit var binding: ActivityVcDynamic4Binding

    private lateinit var viewModel: MainViewModel

    override val TAG = "VCActivity::"
    val TAG_PERMISSION = "VCPermission::"




    private var conferenceManager: MultitrackConferenceManager? = null
    var telephonyManager: TelephonyManager? = null

    private lateinit var settingsDialog: Dialog
    private lateinit var dialog: Dialog


    //    var trackRendererMap = HashMap<SurfaceViewRenderer,String?>()
    var trackRelMap = HashMap<RemotePeerView, String?>()
    var trackObjectRelMap = HashMap<RemotePeerView, VideoTrack?>()


    private lateinit var joinVCDialog: Dialog
    private lateinit var endVCDialog: Dialog

    private var stoppedStream = false

    private var count = 0

    private lateinit var reconnectionVCDialog: Dialog

    /*for bottom more options bar*/
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var participantFragment: ParticipantFragment
    private lateinit var soundDeviceFragment: SoundDeviceFragment
    private lateinit var screenShareFragment: ScreenShareFragment
    private lateinit var messageFragment: MessageFragment

    private var isScreenLargeOrXlarge: Boolean = false
    private var isScreenSmallOrNormal: Boolean = false
    private lateinit var rateUSDialog: Dialog
    private lateinit var estimationConfirmationDialog: Dialog

    var sContainerSizeLandscape:Int = 0
    var sContainerSizePortrait:Int = 0


    /*any customized local broadcasts made exclusively in SDK will be reveived here*/
    private val customSDKBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action.equals(SDK_CUSTOM_BROADCAST_ACTION)) {
                if (intent.hasExtra("task")) {
                    when (intent.getStringExtra("task")) {
                        SDK_BROADCAST_AUDIO_DEVICE_UPDATE -> {
                            Log.d(TAG, "onReceive: audio device udpate ")
                            processAudioDeviceUpdateFromSDK()
                        }

                        SDK_BROADCAST_CAMERA_DEVICE_UPDATE -> {
                            Log.d(TAG, "onReceive: camera device update")
                            processCameraErrorUpdateFromSDK()
                        }

//                        SDK_BROADCAST_STREAM_LIST_UPDATE -> {
//                            Log.d(TAG, "onReceive: stream list update")
//                            processStreamListUpdateFromSDK()
//                        }
                    }
                }
            }
        }
    }

    private lateinit var cameraDialog: Dialog

    private var videoWidth = 0
    private var videoHeight = 0

    private var isIntentForReconnect = false

    /*send status of mic and video*/
    private val STATUS_SEND_PERIOD_MILLIS = 5000

    private val handler = Handler()

    private val sendStatusRunnable: Runnable = object : Runnable {
        override fun run() {
            sendStatusMessage()
            handler.postDelayed(this, STATUS_SEND_PERIOD_MILLIS.toLong())
        }
    }

    /*log internet speed*/
    private val internetLogHandler = Handler(Looper.getMainLooper())
    private val logInternetSpeedRunnable = Runnable { logInternetSpeed() }
    private val internetLogInterval: Long = 5000 // 1 second

    /*to listen to network changes*/
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        // network is available for use
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d(TAG, "onAvailable: NETWORK_TEST::")
            // Network is available. You can start your video conference here.

            // Start measuring data usage when network becomes available
//            startTime = System.currentTimeMillis()
//            startTxBytes = TrafficStats.getUidTxBytes(android.os.Process.myUid())
//            startRxBytes = TrafficStats.getUidRxBytes(android.os.Process.myUid())
//            printSpeedsHandler.post(printSpeedsRunnable)
            removeInternetLostDialog()
        }

        // lost network connection
        override fun onLost(network: Network) {
            super.onLost(network)

            // Network connection is lost. You might want to handle this event and stop your video conference.
            Log.d(TAG, "onLost: NETWORK_TEST")
            showInternetLostDialog()

        }

        // Network is no longer valid, such as the user turning off cellular data or the network disappearing from under the device.
        override fun onUnavailable() {
            super.onUnavailable()
            // Network is no longer available. You might want to handle this event and stop your video conference.
            Log.d(TAG, "onUnavailable: NETWORK_TEST")
        }
    }

    private lateinit var connectivityManager: ConnectivityManager

    private var publisherContainer: RemotePeerView? = null

    private lateinit var noInternetDialog: Dialog

    private  var iWebRTCListener: IWebRTCListener? =null
    private  var dataChannelObserver: IDataChannelObserver? = null

    private lateinit var fragmentTransaction: FragmentTransaction
    private lateinit var fragmentManager: FragmentManager
    val PICKFILE_RESULT_CODE = 700
    val EXTRA_MEDIA_PROJECTION_DATA = "mediaProjectionData"

    private var webSocketNotConnectedThread: Thread? = null
    private var isAudioDisableDueToPhoneCall = false

    /*callbakc handling*/
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            processVCActivityBackPress()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ")
        if (intent.hasExtra("intent_for_reconnect")) {
            isIntentForReconnect = intent.getBooleanExtra("intent_for_reconnect", false)
//            streamId = intent.getStringExtra("stream_id_in_use")
        }
        Log.d(TAG, "onCreate: intentForReconnect -> $isIntentForReconnect")
        Log.d(TAG, "onCreate: screenSize: isSmall: ${resources.getBoolean(R.bool.is_device_small)}")
        Log.d(TAG, "onCreate: screenSize: isNormal: ${resources.getBoolean(R.bool.is_device_normal)}")
        Log.d(TAG, "onCreate: screenSize: isLarge: ${resources.getBoolean(R.bool.is_device_large)}")
        Log.d(TAG, "onCreate: screenSize: isXlarge: ${resources.getBoolean(R.bool.is_device_xlarge)}")
        isScreenLargeOrXlarge = resources.getBoolean(R.bool.is_device_xlarge) or resources.getBoolean(R.bool.is_device_large)
        isScreenSmallOrNormal = resources.getBoolean(R.bool.is_device_normal) or resources.getBoolean(R.bool.is_device_small)
        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        //getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());
        binding = ActivityVcDynamic4Binding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        setContentView(binding.root)
        binding.vm = viewModel
        binding.lifecycleOwner = this
        // adding onbackpressed callback listener.
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        this.intent.putExtra(CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED, true)
        PreferenceManager.init(this)

        sContainerSizePortrait= resources.getDimension(com.intuit.sdp.R.dimen._100sdp).toInt()
        sContainerSizeLandscape= resources.getDimension(com.intuit.sdp.R.dimen._70sdp).toInt()
        init()
        showProgressDialog()
        viewModelObservers()
        initDataChannelListener()
        setUpVcDetails()

    }

    private fun init() {
        progressDialog = AndroidUtils.progressDialog(this)

        bottomSheetBehavior =
            BottomSheetBehavior.from(binding.moreOptionsLayoutSheet)
        setUpBottomSheetBehaviour() /*this is to get callbacks from the more options bottom sheet*/
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        participantFragment = ParticipantFragment()
        soundDeviceFragment = SoundDeviceFragment()
        screenShareFragment = ScreenShareFragment()
        messageFragment = MessageFragment()
        fragmentManager = this.supportFragmentManager
        fragmentTransaction = fragmentManager.beginTransaction()
        fragmentManager.beginTransaction()
            .add(R.id.fragment_holder_for_message, messageFragment, "MESSAGE_FRAG")
            .commit()
//        LocalBroadcastManager.getInstance(this).registerReceiver(customSDKBroadcastReceiver, IntentFilter(
//            SDK_CUSTOM_BROADCAST_ACTION))
        val intentFilter = IntentFilter()
        intentFilter.addAction(VCConstants.SDK_CUSTOM_BROADCAST_ACTION)
        registerReceiver(customSDKBroadcastReceiver, intentFilter)
        logInternetSpeedRunnable.run()
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        // Register the network callback to start monitoring network changes
        val networkRequest = android.net.NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        publisherContainer = RemotePeerView(this)
        val layoutParamsContainer = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )

        publisherContainer!!.layoutParams = layoutParamsContainer
//            remotePeerView.setBackgroundResource(R.drawable.bg_peer_container)
        publisherContainer!!.changeVideoActiveStatus(true)
        publisherContainer!!.changeAudioActiveStatus(true)
        publisherContainer!!.streamName.text = getString(R.string.you)
        binding.fContainer.addView(publisherContainer)

        binding.fContainer.setBackgroundColor(resources.getColor(R.color.colorPrimary))

        if (isScreenLargeOrXlarge) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else if(isScreenSmallOrNormal) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }


    }

    private fun viewModelObservers() {
        viewModel.toastMessage.observe(this) {
            it?.let {
                Toast.makeText(this@VCDynamicActivity4, it, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.newSelectedAudioDevice.observe(this) {
            if (it != null) {
                if (it != viewModel.currentSelectedAudioDevice.value) {
                    changeAudioDevice(it)
                }
            }
        }

        viewModel.startScreenShare.observe(this) {
            it?.let {
                if (it) {
                    startScreenShare()
                    viewModel.startScreenShare.value = false
                }
            }
        }
        viewModel.stopScreenShare.observe(this) {
            it?.let {
                if (it) {
                    stopScreenShare()

                    viewModel.stopScreenShare.value = false
                }
            }
        }
        viewModel.messageFragmentClose.observe(this) {
            it?.let {
                if (it) {
                    closeMessageFragment()
                    viewModel.messageFragmentClose.value = false /*to handle the reobserving of it*/
                }
            }
        }

        viewModel.openFileManager.observe(this) {
            it?.let {
                if (it) {
                    openFileExplorer()
                    viewModel.openFileManager.value = false /*to handle the reobserving of it*/
                }
            }
        }
        viewModel.sendLocalTextMessageToDataChannel.observe(this) {
            it?.let {
                if (it!=-1L) {
                    viewModel.getMessageFromId(it)?.let{ messageAndIndexPair ->

                        if(messageAndIndexPair.first!=null){
                            sendTextMessage(messageAndIndexPair.first!!)
                        }
//                        sendTextMessage(viewModel.newLocalMessage)
                    }
//                    viewModel.sendLocalTextMessageToDataChannel.value = false
                    viewModel.sendLocalTextMessageToDataChannel.value = -1L
                }
            }

        }
        viewModel.sendLocalFileMessageToDataChannel.observe(this) {
            it?.let {
                if (it!=-1L) {
                    viewModel.getMessageFromId(it)?.let{ messageAndIndexPair ->
//                        sendFileMessage(viewModel.newLocalMessage)
                        if(messageAndIndexPair.first!=null) {
                            sendFileMessage(messageAndIndexPair.first!!)
                        }
                    }
//                    viewModel.sendLocalFileMessageToDataChannel.value = false
                    viewModel.sendLocalFileMessageToDataChannel.value = -1L
                }
            }

        }

        viewModel.sendLocalEstimationMessageToDataChannel.observe(this) {
            it?.let {
                if(it!=-1L) {
                    viewModel.getMessageFromId(it)?.let {messageAndIndexPair->
                        if(messageAndIndexPair.first!=null) {
                            //send EstimationMessage
                            sendEstimationMessage(messageAndIndexPair.first!!)

                        }
                    }
                    viewModel.sendLocalFileMessageToDataChannel.value = -1L
                }
            }
        }
        viewModel.messageUnreadCount.observe(this){
            it?.let{
                if(it.isBlank()){
                    binding.tvMsgCount.visibility = View.GONE
                }else{
                    binding.tvMsgCount.visibility = View.VISIBLE
                }
            }
        }
        /*api observers*/
        viewModel.loginResponse.observe(this) {
            viewModel.toastMessage.value = "Login Success"
//             viewModel.toastString.value = it.loginData.token
            Log.d(TAG, "viewmodelObservers login:  ${it}")
            if(it!=null) {
                if(it.loginData?.token!=null) {
                    saveEstimateToken(
                        context = this,
                        token = it.loginData?.token
                    )
                }
            }
        }
        viewModel.validateVCResponse.observe(this) {it ->
            if (it != null) {
                Log.d(TAG, "viewModelObservers: validate vc response -> ${Gson().toJson(it)}")
                if (it.status.equals("success",true)) {
                    if(AndroidUtils.isNetworkOnLine(this)) {
                        viewModel.getVCConfiguration(viewModel.roomID!!)
                    }else{
                        showValidateVcAlert("No internet connection. Please try after some time","configure")

                    }
                } else {
                    showValidateVcAlert(error = it.error!!,"validate")
                }
            }
        }

        viewModel.vcConfigurationResponse.observe(this) {it ->
            if (it != null) {
                if(it.mcu_required!=null|| it.status.equals("success",true)){
                    Log.d(TAG, "viewModelObservers: initialize all the data")
                    processVCConfigurationSuccessResult()
                }
                else {
                    showValidateVcAlert(error = it.error!!,"configure")
                }
            }

        }

        viewModel.updateParticipantsNameUI.observe(this){
            if(it!=null)
            {
                if(it)
                {
                    updateParticipantsDisplayNames()
                    viewModel.updateParticipantsNameUI.value = false
                }
            }
        }

        viewModel.isProgressBarVisible.observe(this) {
            if(it!=null) {
                if(it) {
                    showProgressDialog()
                }else {
                    dismissProgressDialog()
                }
            }
        }

        viewModel.updateVcStatusResponse.observe(this) {

            Log.d(TAG, "initializeObservers: updateVCStatusResponse:  ${it}")
            if(it.apiResponseStatus) {
                when(it.responseData?.status) {
                    "I"-> {
                        if(it.responseData!!.data.success) {
                            viewModel.isProgressBarVisible.value = false
                            Log.d(TAG, "initializeObservers: updateVC Success: ")
                        }else {
                            viewModel.isProgressBarVisible.value = false
                            viewModel.toastMessage.value = it.responseData.message.toString()
                        }
                    }
                    "E"-> {

                        viewModel.isProgressBarVisible.value = false
                        viewModel.toastMessage.value = it.responseData.message.toString()
                    }
                    else -> {

                        viewModel.isProgressBarVisible.value = false
                        viewModel.toastMessage.value = it.responseData?.message.toString()
                    }
                }
            }else {
                viewModel.isProgressBarVisible.value = false
                Log.d(TAG, "initializeObservers: updateVC Failure: ")

            }

            openRateUs()

        }

        viewModel.getEstimationDetails.observe(this) {
            viewModel.getEstimationDetailsNew(
                baseUrl = PreferenceManager.getBaseUrl()!!,
                id = AndroidUtils.getCurrentTimeInMill()
            )
        }

        viewModel.estimateDetailsResponse.observe(this) {
            if (it != null) {
                dismissProgressDialog()
                showDialogToConfirmEstimation(it)
            }
        }

        viewModel.updateEstimateStatus.observe(this) {
            if (it != null) {
                updateEstimateStatus(it)
                viewModel.updateEstimateStatus.value = null
            }
        }

        viewModel.updateEstimationStatusResponse.observe(this) {
            if (it != null) {
                if (it.status == "I") {
//                    viewModel.messageModelFromVC.value = viewModel.tempMessageModelAfterApprovalOrReject
//                    sendEstimateMessage(viewModel.estimateDetailsAfterApproval!!)

                    //process local message and send vid datachannel
                    viewModel.processLocalEstimationMessage(
                        estimationDetails = viewModel.estimateDetailsAfterApproval!!,
                        id = AndroidUtils.getCurrentTimeInMill()
                    )

                    updateAdapterPosition()
                    viewModel.tempMessageModelAfterApprovalOrReject = null
                } else if (it.status == "N") {
                    Log.d(TAG, "initializeObservers: estimateRejectionStatus: ")
                    viewModel.toastMessage.value = it.data.message.toString()
                    viewModel.isProgressBarVisible.value = false
//                    showDialogToConfirmEstimationRejection(sharedViewModel.estimateDetailsAfterApproval!!)
                } else if (it.status == "E") {
                    viewModel.toastMessage.value = it.data.message.toString()
                    viewModel.isProgressBarVisible.value = false
                } else {
                    viewModel.toastMessage.value = it.message.toString()
                    viewModel.isProgressBarVisible.value = false
                }
            }
        }

        viewModel.saveMessageList.observe(this) {
            if (it != null) {
//                newImplementation_20Sep2023
//                vCScreenViewModel.saveChatList(it)
                viewModel.saveChatListNew(PreferenceManager.getBaseUrl()!!,it)
                viewModel.saveMessageList.value = null
            }
        }

        viewModel.saveChatResponse.observe(this) {
            if (it != null) {
                if (it.success) {
                    viewModel.isProgressBarVisible.value = false
                    Toast.makeText(this, "Chat Saved", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.isProgressBarVisible.value = false
                    Toast.makeText(this, "Something went wrong: Save Chat", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        viewModel.isInPhoneCall.observe(this) {
            if (it) {
//                disconnectCurrentVC()//commented on 24th Apr 21
                //added on 24th Apr 21
                if (conferenceManager!!.isPublisherAudioOn && viewModel.isPhoneCallStarted.value != true) {
                    viewModel.isPhoneCallStarted.value = true
                    isAudioDisableDueToPhoneCall = true

//                    isAudioEnable = false
//                    conferenceManager.disableAudio()
//                    vcScreenBinding.imgAudioOption.setImageResource(
//                        com.kia.R.drawable.audio_disable
//                    )

                    if (conferenceManager!!.isPublisherAudioOn) {
                        conferenceManager!!.disableAudio()
                        viewModel.localAudio = false
                        processMicUIForPublishContainer(MIC_MUTED)
                    }
                }
                webSocketNotConnectedThread()
                //
            }
        }

        viewModel.isPhoneCallEnded.observe(this) {
            Log.d(TAG, "observer: isPhoneCallEnded: $it")
            if (it) {
//                reconnectVc()//commented on 24th Apr 21
                //added on 24th Apr 21
                if (webSocketNotConnectedThread != null && webSocketNotConnectedThread!!.isAlive) {
                    webSocketNotConnectedThread!!.interrupt()
                    webSocketNotConnectedThread = null
                }

                if (conferenceManager!!.isWebSocketNotConnected) {
//                    reconnectVc()
                    showReconnectionVCDialog()
                }
                if (!conferenceManager!!.isPublisherAudioOn && isAudioDisableDueToPhoneCall) {
                    viewModel.isPhoneCallStarted.value = false
                    isAudioDisableDueToPhoneCall = false

//                    isAudioEnable = true
//                    conferenceManager.enableAudio()
//                    vcScreenBinding.imgAudioOption.setImageResource(com.kia.R.drawable.mic_icon_enable)


                    if (conferenceManager!!.isPublisherAudioOn) {
                        conferenceManager!!.enableAudio()
                        viewModel.localAudio = true
                        processMicUIForPublishContainer(MIC_UNMUTED)
                    }
                }
                //
            }
        }

    }

    private fun updateAdapterPosition() {
        Log.d(TAG, "updateAdapterPosition: updateAdapterPosistion: ")
        viewModel.isSuccessEstimationResponse.value = true
    }

    private fun showDialogToConfirmEstimation(estimationDetails: ResponseModelEstimateData) {
        estimationConfirmationDialog = Dialog(this)
        estimationConfirmationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        var dialogBinding = LayoutDialogConfirmationBinding.inflate(LayoutInflater.from(this))
        estimationConfirmationDialog.setContentView(dialogBinding.root)
        dialogBinding.tvDialogMessage.text =  "Do you want to send the \n estimation details."
        estimationConfirmationDialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        estimationConfirmationDialog.setCancelable(false)
//        val cancel = estimationConfirmationDialog.findViewById(R.id.neg_btn) as TextView
//        val ok = estimationConfirmationDialog.findViewById(R.id.pos_btn) as TextView


        dialogBinding.tvCancelButton.setOnClickListener {
            estimationConfirmationDialog.dismiss()
        }
        dialogBinding.btnUpdate.setOnClickListener {
            estimationConfirmationDialog.dismiss()
//            vCScreenViewModel.messageModelFromVC.value = viewModel.tempEstimateModel
//            sendEstimateMessage(estimationDetails)
            viewModel.processLocalEstimationMessage(
                estimationDetails = estimationDetails,
                id = AndroidUtils.getCurrentTimeInMill())
        }

        estimationConfirmationDialog.show()
    }

    private fun openRateUs() {
        Log.d(TAG, "openRateUsDialog")
        rateUSDialog = Dialog(this)
        rateUSDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        rateUSDialog.setContentView(R.layout.rateus_dialog_layout)
        rateUSDialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        rateUSDialog.setCancelable(false)
        val rateus = rateUSDialog.findViewById(R.id.rateus) as TextView
        rateus.setOnClickListener {
            rateUSDialog.dismiss()
            Log.d(TAG, "openRateUs:  rateUsClicked")
            var intent = Intent(this, FeedbackActivity::class.java)
            intent.putExtra("customerCode", viewModel.customerCode)
            intent.putExtra("dealerCode", viewModel.dealerCode)
            intent.putExtra("roNumber", viewModel.roNo)
            intent.putExtra("meetingCode", viewModel.roomID)
            intent.putExtra("userName",viewModel.userName)
            startActivity(intent)
            finish()
        }

        rateUSDialog.show()
    }

    private fun setUpOnClickListeners() {
        binding.btnAudioSwitch.setOnClickListener {
            if (conferenceManager != null) {
                if (conferenceManager!!.isJoined) {
                    controlAudio()
                }
            }
        }

        binding.btnVideo.setOnClickListener {
            if (conferenceManager != null) {
                if (conferenceManager!!.isJoined) {
                    controlVideo()
                }
            }
        }
        binding.btnEndCall.setOnClickListener {
            if (conferenceManager != null) {
                if (conferenceManager!!.isJoined) {
                    leaveConference()
                }
            }
        }

        binding.btnGrid.setOnClickListener {
            Log.d(
                TAG,
                "setUpOnClickListeners: binding.sContainer count -> " + binding.sContainer.childCount
            )
            if (conferenceManager != null) {
                if (conferenceManager!!.isJoined) {
                    if (viewModel.grid) {
                        /*grid is visible..mkae it gone*/
                        binding.scrollView.visibility = View.GONE
                        binding.sContainer.visibility = View.GONE
                        /*Android 9 is giving torture for this visbility issuee..so just in case if any renderer is missed out.*/
                        for (e in binding.sContainer.children) {
                            e.visibility = View.GONE
                        }
                        binding.btnHidPeersSlash.visibility = View.VISIBLE
//                        binding.btnGrid.setImageResource(R.drawable.ic_grid_off)

                    } else {
                        /*grid is hidden.,..make it visible*/
                        binding.scrollView.visibility = View.VISIBLE
                        binding.sContainer.visibility = View.VISIBLE

                        /*Android 9 is giving torture for this visbility issuee..so just in case if any renderer is missed out.*/
                        for (e in binding.sContainer.children) {
                            e.visibility = View.VISIBLE
                        }
                        binding.btnHidPeersSlash.visibility = View.GONE
//                        binding.btnGrid.setImageResource(R.drawable.ic_show_peers)

                    }
                    viewModel.grid = !viewModel.grid
                }
            }
        }

        binding.btnCameraToggle.setOnClickListener {
            if (conferenceManager != null) {
                if (conferenceManager!!.isJoined) {
                    if (viewModel.screenShareStatus == false) {
                        viewModel.frontCamera = !viewModel.frontCamera
                        conferenceManager!!.switchCamera()
                    }
                }
            }
        }


        binding.btnMoreMenu?.setOnClickListener {
            Log.d(TAG, "setUpOnClickListeners: moreOptionsClicked: ")
            if (conferenceManager != null) {
                if (conferenceManager!!.isJoined) {
                    showMoreOptions(viewModel.bottomSheet)
//                    viewModel.bottomSheet = !viewModel.bottomSheet
                }
            }
        }

        /**bottom sheet options handling*/
        binding.screenShareOptionLayout.setOnClickListener {
            /*check fro screen share and do action*/
            Log.d(TAG, "setUpOnClickListeners: ")
            if (conferenceManager != null) {
                if (conferenceManager!!.isJoined) {
                    openScreenShareOptions()
                }
            }
        }
        binding.btnScreenShare?.setOnClickListener {
            /*check fro screen share and do action*/
            Log.d(TAG, "setUpOnClickListeners: ")
            if (conferenceManager != null) {
                if (conferenceManager!!.isJoined) {
                    if(viewModel.localVideo) {
                        openScreenShareOptions()
                    }else {
                        viewModel.toastMessage.value = "Please enabled the video."
                    }
                }
            }
        }

        binding.participantsOptionsLayout.setOnClickListener {
            /*check for participants and do action*/
            if (conferenceManager != null) {
                if (conferenceManager!!.isJoined) {
                    openParticipantsListInRoom()
                }
            }
        }
        binding.btnParticipants?.setOnClickListener {
            /*check for participants and do action*/
            if (conferenceManager != null) {
                if (conferenceManager!!.isJoined) {
                    openParticipantsListInRoom()
                }
            }
        }

        binding.soundDeviceOptionLayout.setOnClickListener {
            /*check for sound device options and do action*/
            if (conferenceManager != null) {
                if (conferenceManager!!.isJoined) {
                    viewModel.audioDevices.value = getAudioDevices()
                    viewModel.currentSelectedAudioDevice.value = getCurrentSelectedAudioDevice()
                    Log.d(
                        TAG,
                        "setUpOnClickListeners: btnSoundDevice getAudioDevices -> " + Gson().toJson(
                            getAudioDevices().toString()
                        )
                    )
                    Log.d(
                        TAG,
                        "setUpOnClickListeners: btnSoundDevice getCurrentSelectedAudioDevice-> " + Gson().toJson(
                            getCurrentSelectedAudioDevice().toString()
                        )
                    )
                    openSoundDeviceListInRoom()
                }
            }
        }
        binding.btnAudioOutputDevices?.setOnClickListener {
            /*check for sound device options and do action*/
            if (conferenceManager != null) {
                if (conferenceManager!!.isJoined) {
                    viewModel.audioDevices.value = getAudioDevices()
                    viewModel.currentSelectedAudioDevice.value = getCurrentSelectedAudioDevice()
                    Log.d(
                        TAG,
                        "setUpOnClickListeners: btnSoundDevice getAudioDevices -> " + Gson().toJson(
                            getAudioDevices().toString()
                        )
                    )
                    Log.d(
                        TAG,
                        "setUpOnClickListeners: btnSoundDevice getCurrentSelectedAudioDevice-> " + Gson().toJson(
                            getCurrentSelectedAudioDevice().toString()
                        )
                    )
                    openSoundDeviceListInRoom()
                }
            }
        }

        binding.btnChat.setOnClickListener {
//            openMessageFragment()
            fragmentManager.beginTransaction()
                .show(messageFragment)
                .commit()
           binding.fragmentHolderForMessage.visibility = View.VISIBLE
            viewModel.messageFragVisible = true
            viewModel.clearMessageBadgeValue()
        }
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart: ")
        checkForMandatoryPermissions()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: ")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
        conferenceManager?.let {
            Log.d(TAG, "onDestroy: is joined")
            it.leaveFromConference()
        }
        conferenceManager =null
        if (customSDKBroadcastReceiver != null) {
            unregisterReceiver(customSDKBroadcastReceiver)
        }
        viewModel.isInitialConferenceStarted = false
        clearSendStatusSchedule()
        removeLogInternetSpeedCallbacks()
        iWebRTCListener = null
        dataChannelObserver = null
    }

    //permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.isNotEmpty()) {
            for (i in permissions.indices) {
                val permission = permissions[i]
                if (PERMISSIONS.contains(permission)) {

                    if (Manifest.permission.RECORD_AUDIO == permission) {
                        if (grantResults[i] == (PackageManager.PERMISSION_GRANTED)) {
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult:  RECORD_AUDIO: Granted"
                            )
                            // User Granted Permission

                        } else if (Build.VERSION.SDK_INT >= 23 && !shouldShowRequestPermissionRationale(
                                permissions[i]
                            )
                        ) {
                            // if user denied permission and selected Don't Ask Again
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult: RECORD_AUDIO: NotGranted: OpenSettings:  "
                            )
                            showDialogForAppSettings()
                            return
                        } else {
                            // User Denied Permission
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult: RECORD_AUDIO: NotGranted: ShowDialogAgain:   "
                            )
                            showDialogToReqPermissionAgain()
                            return
                        }
                    }
                    if (Manifest.permission.CAMERA == permission) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            // you now have permission
                            Log.d(TAG_PERMISSION, "onRequestPermissionsResult: CAMERA: Granted ")

                        } else if (Build.VERSION.SDK_INT >= 23 && !shouldShowRequestPermissionRationale(
                                permissions[i]
                            )
                        ) {
                            // if user denied permission and selected Don't Ask Again
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult: CAMERA: NotGranted: OpenSettings:  "
                            )
                            showDialogForAppSettings()
                            return
                        } else {
                            // User Denied Permission
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult: CAMERA: NotGranted: ShowDialogAgain:  "
                            )
                            showDialogToReqPermissionAgain()
                            return
                        }
                    }
                    if (Manifest.permission.MODIFY_AUDIO_SETTINGS == permission) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            // you now have permission
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult: MODIFY_AUDIO_SETTINGS: Granted:  "
                            )
                        } else if (Build.VERSION.SDK_INT >= 23 && !shouldShowRequestPermissionRationale(
                                permissions[i]
                            )
                        ) {
                            // if user denied permission and selected Don't Ask Again
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult: MODIFY_AUDIO_SETTINGS: NotGranted: OpenSettings:  "
                            )
                            showDialogForAppSettings()
                            return
                        } else {
                            // User Denied Permission
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult: MODIFY_AUDIO_SETTINGS: NotGranted: ShowDialogAgain:  "
                            )
                            showDialogToReqPermissionAgain()
                            return
                        }
                    }
                    if (Manifest.permission.READ_EXTERNAL_STORAGE == permission) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            // you now have permission
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult: READ_EXTERNAL_STORAGE: Granted:  "
                            )
                        } else if (Build.VERSION.SDK_INT >= 23 && !shouldShowRequestPermissionRationale(
                                permissions[i]
                            )
                        ) {
                            // if user denied permission and selected Don't Ask Again
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult: READ_EXTERNAL_STORAGE: NotGranted: OpenSettings:   "
                            )
                            showDialogForAppSettings()
                            return
                        } else {
                            // User Denied Permission
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult: READ_EXTERNAL_STORAGE: NotGranted: ShowDialogAgain:   "
                            )
                            showDialogToReqPermissionAgain()
                            return
                        }
                    }
                    if (Manifest.permission.WRITE_EXTERNAL_STORAGE == permission) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            // you now have permission
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult: WRITE_EXTERNAL_STORAGE: Granted:  "
                            )
                        } else if (Build.VERSION.SDK_INT >= 23 && !shouldShowRequestPermissionRationale(
                                permissions[i]
                            )
                        ) {
                            // if user denied permission and selected Don't Ask Again
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult: WRITE_EXTERNAL_STORAGE: NotGranted: OpenSettings:  "
                            )
                            showDialogForAppSettings()
                            return
                        } else {
                            // User Denied Permission
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult: WRITE_EXTERNAL_STORAGE: NotGranted: ShowDialogAgain:  "
                            )
                            showDialogToReqPermissionAgain()
                            return
                        }
                    }
                    if (Manifest.permission.READ_PHONE_STATE == permission) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            // you now have permission
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult: READ_PHONE_STATE: Granted:  "
                            )

                        } else if (Build.VERSION.SDK_INT >= 23 && !shouldShowRequestPermissionRationale(
                                permissions[i]
                            )
                        ) {
                            // if user denied permission and selected Don't Ask Again
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult: READ_PHONE_STATE: NotGranted: OpenSettings:  "
                            )
                            showDialogForAppSettings()
                            return
                        } else {
                            // User Denied Permission
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult: READ_PHONE_STATE: NotGranted: ShowDialogAgain:  "
                            )
                            showDialogToReqPermissionAgain()
                            return
                        }
                    }
                    if (Manifest.permission.BLUETOOTH_CONNECT == permission) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            // you now have permission
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult: BLUETOOTH_CONNECT: Granted:  "
                            )

                        } else if (Build.VERSION.SDK_INT >= 23 && !shouldShowRequestPermissionRationale(
                                permissions[i]
                            )
                        ) {
                            // if user denied permission and selected Don't Ask Again
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult: BLUETOOTH_CONNECT: NotGranted: OpenSettings:  "
                            )
                            showDialogForAppSettings()
                            return
                        } else {
                            // User Denied Permission
                            Log.d(
                                TAG_PERMISSION,
                                "onRequestPermissionsResult: BLUETOOTH_CONNECT: NotGranted: ShowDialogAgain:  "
                            )
                            showDialogToReqPermissionAgain()
                            return
                        }
                    }

                }
                if (i == permissions.size - 1) {
                    Log.d(TAG, "onRequestPermissionsResult: size-1 AndroidUtils")
                    if (AndroidUtils.hasPermissions(this, PERMISSIONS)) {
                        Log.d(TAG, "onRequestPermissionsResult: size -1 ..true")
                        initTelephonyManagerListener()
                        initConferenceManager(false)
                    } else {
                        showDialogForAppSettings()
                    }
                }
            }
        }
    }

    private fun showDialogForAppSettings() {
        if (this@VCDynamicActivity4::settingsDialog.isInitialized) {
            if (settingsDialog.isShowing) {
                settingsDialog.dismiss()
//                return
            }
        }
        settingsDialog = Dialog(this)
        val dialogBinding = PermissionsDialogLayoutBinding.inflate(LayoutInflater.from(this))
        settingsDialog.setContentView(dialogBinding.root)
        settingsDialog.setCancelable(false)
        settingsDialog.setCanceledOnTouchOutside(false)
//        settingsDialog.window?.setLayout(
//            ViewGroup.LayoutParams.MATCH_PARENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT
//        )

        if(isScreenLargeOrXlarge) {
            settingsDialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }else {
            joinVCDialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        dialogBinding.messageTv.text =
            resources.getString(R.string.permissions_msg_to_open_settings)
        dialogBinding.posBtn.text = resources.getString(R.string.open_settings)
        dialogBinding.negBtn.text = resources.getString(R.string.per_cancel)
        dialogBinding.negBtn.visibility = View.GONE

        dialogBinding.posBtn.setOnClickListener {
            Log.d(
                TAG, "" +
                        "() Checking for  onPositive Btn"
            )
            settingsDialog.dismiss()
            openSettings()
        }

        dialogBinding.negBtn.setOnClickListener {
            Log.d(TAG_PERMISSION, "showDialogForAppSettings() Checking for onNegative Btn")
            settingsDialog.dismiss()
//            Toast.makeText(this, "Not enough permissions to join", Toast.LENGTH_SHORT).show()
//            goBack()
            finishActivity(false)
        }

        //dialog alignment and size code.
//        val lp = WindowManager.LayoutParams()
//        lp.copyFrom(settingsDialog.window?.attributes)
//        lp.width = WindowManager.LayoutParams.MATCH_PARENT
//        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
//        lp.gravity = Gravity.CENTER
//        settingsDialog.window?.attributes = lp
//        settingsDialog.getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
        settingsDialog.show()
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }


    private fun checkForMandatoryPermissions() {
        if(viewModel.initialConfigurationSucess==false)
        {
            return
        }
        if (conferenceManager != null) {
            if (conferenceManager!!.isJoined) {
                return
            }
        }
        Log.d(TAG, "checkForMandatoryPermissions: AndroidUtils")
        if (AndroidUtils.hasPermissions(this, PERMISSIONS)) {
            Log.d(TAG, "checkForMandatoryPermissions: true")
            initTelephonyManagerListener()
            initConferenceManager(false)
        } else {
            ActivityCompat.requestPermissions(
                this,
                PERMISSIONS,
                PERMISSION_CODE
            )
        }
    }

    private fun showDialogToReqPermissionAgain() {
        if (!this::dialog.isInitialized) {
            dialog = Dialog(this)
            val dialogBinding = PermissionsDialogLayoutBinding.inflate(LayoutInflater.from(this))
            dialog.setContentView(dialogBinding.root)
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
//            dialog.window?.setLayout(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT
//            )


            if(isScreenLargeOrXlarge) {
                dialog.window?.setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }else {
                dialog.window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            dialogBinding.messageTv.text = resources.getString(R.string.per_allow_msg)
            dialogBinding.posBtn.text = resources.getString(R.string.allow)
            dialogBinding.negBtn.text = resources.getString(R.string.per_cancel)

            dialogBinding.posBtn.setOnClickListener {
                Log.d(
                    TAG,
                    "showDialogToReqPermissionAgain() Checking for  onPositive Btn"
                )

                dialog.dismiss()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    checkForMandatoryPermissions()
                }
            }

            dialogBinding.negBtn.setOnClickListener {
                dialog.dismiss()
//                Toast.makeText(this, "Not enough permissions to join", Toast.LENGTH_SHORT).show()
//                goBack()
//                finish()
            }
            //dialog alignment and size code.
//            val lp = WindowManager.LayoutParams()
//            lp.copyFrom(dialog.window?.attributes)
//            lp.width = WindowManager.LayoutParams.MATCH_PARENT
//            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
//            lp.gravity = Gravity.CENTER
//            dialog.window?.attributes = lp
//            dialog.getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
            dialog.show()
        } else {
            dialog.show()
        }

    }

    private fun initConferenceManager(isForRejoin: Boolean) {
        if (AndroidUtils.isNetworkOnLine(this)) {
            this.intent.putExtra(
                CallActivity.EXTRA_SCREENCAPTURE,
                false
            ) //initially for the screen capture to be false
            viewModel.clearMessageBadgeValue()
            Log.d(TAG, "initConferenceManager: ")
            binding.fContainer.removeAllViews()
            binding.sContainer.removeAllViews()
            initPublishContainerClickListener()
//            initDataChannelListener()
            initWebRTCListener()
            viewModel.streamId = null
            conferenceManager = MultitrackConferenceManager(
                this,
                iWebRTCListener,
                intent,
                viewModel.serverUrl,
                viewModel.roomID,
                publisherContainer!!.surfaceViewRenderer,
                ArrayList<SurfaceViewRenderer>(),  //new ArrayList<>(),//
                viewModel.streamId,
                dataChannelObserver
            )
            Log.d(TAG, "initConferenceManager: ")

            conferenceManager!!.init()
            conferenceManager!!.isPlayOnlyMode = false
            conferenceManager!!.setOpenFrontCamera(false)
            conferenceManager!!.isReconnectionEnabled = false
            dismissProgressDialog()
            Log.d(TAG, "initConferenceManager: isIntentForReconnect -> $isIntentForReconnect")
//            if (isIntentForReconnect) { //if restarting activity
//                /*handle the UI state here*/
//                joinConference()
//            } else {
//                /*this will be fresh UI state*/
//                showJoinVCRoomDialog()
//            }
            if (isForRejoin) { //if rejoining conference
                viewModel.rejoinInProgress = false
                joinConference()
                Log.d(TAG, "initConferenceManager: afterrejoin: conferenceManagerVideoOn : ${conferenceManager!!.isPublisherVideoOn}")
                Log.d(TAG, "initConferenceManager: afterrejoin: localVideoON : ${viewModel.localVideo}")
                Log.d(TAG, "initConferenceManager: afterrejoin: conferenceManagerAudioOn : ${conferenceManager!!.isPublisherAudioOn}")
                Log.d(TAG, "initConferenceManager: afterrejoin: localVideoON : ${viewModel.localAudio}")

                // added_nbg_19Dec2023 bcoz after rejoining the video and audio is in the enabled state
                processMicUIForPublishContainer(MIC_UNMUTED)
                processCameraUIForPublishContainer(VCConstants.CAM_TURNED_ON)

                // commented_nbg_19Dec2023 out the logic as it was causing issues when rejoining.. trying to send data Channel when the data channel is  not active yet
//                if(conferenceManager!!.isPublisherVideoOn && viewModel.localVideo)
//                {
//                    /**all good*/
//                }else {
//                    if(viewModel.localVideo)
//                    {
//                        /*true value...so cam must be on*/
//                        conferenceManager!!.enableVideo()
//                        viewModel.localVideo = true
//                        processCameraUIForPublishContainer(VCConstants.CAM_TURNED_ON)
//                    }else{
//                        /*false calue...so cam must be off*/
//                        conferenceManager!!.disableVideo()
//                        viewModel.localVideo = false
//                        processCameraUIForPublishContainer(VCConstants.CAM_TURNED_OFF)
//                    }
//                }
//                if(conferenceManager!!.isPublisherAudioOn && viewModel.localAudio)
//                {
//                    /**all good*/
//                }else {
//                    if(viewModel.localAudio)
//                    {
//                        /*true value...so audio must be on*/
//                        conferenceManager!!.enableAudio()
//                        viewModel.localAudio = true
//                        processMicUIForPublishContainer(VCConstants.MIC_UNMUTED)
//                    }else{
//                        /*false calue...so cam must be off*/
//                        conferenceManager!!.disableAudio()
//                        viewModel.localAudio = false
//                        processMicUIForPublishContainer(VCConstants.MIC_MUTED)
//                    }
//                }
                var source = if(viewModel.frontCamera){
                    WebRTCClient.SOURCE_FRONT
                }else  WebRTCClient.SOURCE_REAR
                try {
                conferenceManager!!.publishWebRTCClient.changeVideoSource(source)
                }catch(e:Exception){
                    Log.d(TAG, "initConferenceManager: exception caught!!")
                }
            } else {
                /*this will be fresh UI state*/
                showJoinVCRoomDialog()
            }
            Log.d(TAG, "initConferenceManager: end")
        } else {
            viewModel.toastMessage.value = " No internet connection.. try again"
        }
    }

//    private fun initConferenceManager(isForRejoin: Boolean) {
//        if (AndroidUtils.isNetworkOnLine(this)) {
//            intent.putExtra(CallActivity.EXTRA_SCREENCAPTURE, false) // initially for the screen capture to be false
//            viewModel.clearMessageBadgeValue()
//            Log.d(TAG, "initConferenceManager: ")
//            binding.fContainer.removeAllViews()
//            binding.sContainer.removeAllViews()
//            initPublishContainerClickListener()
//            // initDataChannelListener() // Removed (uncomment if needed)
//            initWebRTCListener()
//            viewModel.streamId = null // Set stream ID to null initially
//
//            conferenceManager = MultitrackConferenceManager(
//                this,
//                iWebRTCListener,
//                intent,
//                viewModel.serverUrl,
//                viewModel.roomID,
//                publisherContainer!!.surfaceViewRenderer,
//                ArrayList<SurfaceViewRenderer>(), // Empty list for remote streams
//                viewModel.streamId,
//                dataChannelObserver
//            )
//            Log.d(TAG, "initConferenceManager: ")
//
//            conferenceManager!!.init()
//            conferenceManager!!.isPlayOnlyMode = false // Default to publish-subscribe mode
//            conferenceManager!!.setOpenFrontCamera(false) // Default camera not front-facing
//
//            // Handle UI state based on rejoin flag
//            if (isForRejoin) {
//                viewModel.rejoinInProgress = false
//                joinConference()
//            } else {
//                showJoinVCRoomDialog() // Show UI for fresh joins
//            }
//
//            // Handle camera and microphone state based on flags and update UI
//            if (conferenceManager!!.isPublisherVideoOn && viewModel.localVideo) {
//                // Video already in state desired by flags, do nothing
//            } else {
//                if (viewModel.localVideo) {
//                    conferenceManager!!.enableVideo()
//                    viewModel.localVideo = true
//                    processCameraUIForPublishContainer(VCConstants.CAM_TURNED_ON)
//                } else {
//                    conferenceManager!!.disableVideo()
//                    viewModel.localVideo = false
//                    processCameraUIForPublishContainer(VCConstants.CAM_TURNED_OFF)
//                }
//            }
//
//            if (conferenceManager!!.isPublisherAudioOn && viewModel.localAudio) {
//                // Audio already in state desired by flags, do nothing
//            } else {
//                if (viewModel.localAudio) {
//                    conferenceManager!!.enableAudio()
//                    viewModel.localAudio = true
//                    processMicUIForPublishContainer(VCConstants.MIC_UNMUTED)
//                } else {
//                    conferenceManager!!.disableAudio()
//                    viewModel.localAudio = false
//                    processMicUIForPublishContainer(VCConstants.MIC_MUTED)
//                }
//            }
//
//            // Set camera source based on front camera flag
//            val source = if (viewModel.frontCamera) {
//                WebRTCClient.SOURCE_FRONT
//            } else {
//                WebRTCClient.SOURCE_REAR
//            }
//            try {
//                conferenceManager!!.publishWebRTCClient.changeVideoSource(source)
//            } catch (e: Exception) {
//                Log.e(TAG, "Error changing video source", e)
//            }
//
//            Log.d(TAG, "initConferenceManager: end")
//        } else {
//            viewModel.toastMessage.value = " No internet connection.. try again"
//        }
//    }

    fun joinConference() {
        Log.d(TAG, "joinConference: ")
        if (conferenceManager != null) {
            if (!conferenceManager!!.isJoined) {
                Log.w(TAG, "Joining Conference")
                conferenceManager!!.joinTheConference()
            }
        }
    }

    private fun showJoinVCRoomDialog() {
        Log.d(TAG, "showJoinVCRoomDialog: ")
        if (this::joinVCDialog.isInitialized) {
            if (joinVCDialog.isShowing) {
                joinVCDialog.dismiss()
            }
        }
        joinVCDialog = Dialog(this)
        joinVCDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

//        joinVCDialog.context.setTheme(R.style.MyAlertDialogTheme)
        val dialogBinding = ReadyToJoinDialogLayoutBinding.inflate(LayoutInflater.from(this))
        joinVCDialog.setContentView(dialogBinding.root)
        joinVCDialog.setCancelable(false)
        joinVCDialog.setCanceledOnTouchOutside(false)
        Log.d(TAG, "showJoinVCRoomDialog: ${isScreenLargeOrXlarge}")
        if(isScreenLargeOrXlarge) {
            joinVCDialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }else {
            joinVCDialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }


        dialogBinding.negBtn.setOnClickListener {
            joinVCDialog.dismiss()
            dismissProgressDialog()
//            goBack()
            finishActivity(false)
        }
        dialogBinding.posBtn.setOnClickListener {
            if (AndroidUtils.isNetworkOnLine(this)) {
                Log.d(TAG, "openJoinVCRoomDialog: internetPresent: ")
                joinVCDialog.dismiss()
                joinConference()
                showProgressDialog()
            } else {
                Log.d(TAG, "openJoinVCRoomDialog: internetNotPresent: ")
//                viewModel.noInternetScenario = Constants.NO_INTERNET_JOIN_CONFERENCE
//                joinVCDialog.dismiss()
//                showNoInternetDialog()
                viewModel.toastMessage.value = "No Internet Connection"
            }
        }
        //dialog alignment and size code.
//        val lp = WindowManager.LayoutParams()
////        lp.copyFrom(joinVCDialog.window?.attributes)
//        lp.width = WindowManager.LayoutParams.MATCH_PARENT
//        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
////        lp.gravity = Gravity.CENTER
//        joinVCDialog.window?.attributes = lp
//        joinVCDialog.getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
        joinVCDialog.show()
    }


    private fun controlAudio() {

        if (conferenceManager!!.isPublisherAudioOn) {
            conferenceManager!!.disableAudio()
            viewModel.localAudio = false
            processMicUIForPublishContainer(MIC_MUTED)
        } else {
            conferenceManager!!.enableAudio()
            viewModel.localAudio = true
            processMicUIForPublishContainer(MIC_UNMUTED)
        }
    }

    private fun controlVideo() {
        if(!viewModel.screenShareStatus) {
            if (conferenceManager!!.isPublisherVideoOn) {
                conferenceManager!!.disableVideo()
                viewModel.localVideo = false
                processCameraUIForPublishContainer(CAM_TURNED_OFF)
            } else {
                conferenceManager!!.enableVideo()
                viewModel.localVideo = true
                processCameraUIForPublishContainer(CAM_TURNED_ON)
            }
        }else {
            viewModel.toastMessage.value = "Please disable screen share."
        }

    }

    private fun leaveConference() {
        if(::endVCDialog.isInitialized) {
            if(endVCDialog.isShowing) {

            }else {
                showEndVCDialog()
            }
        }else {
            showEndVCDialog()
        }
    }

    private fun showEndVCDialog() {
        Log.d(TAG, "showAlertToEndtheUserCall: ")
        endVCDialog = Dialog(this)
        endVCDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogEndTrheCallBinding.inflate(LayoutInflater.from(this))
        endVCDialog.setContentView(dialogBinding.root)
        endVCDialog.setCancelable(false)
        endVCDialog.setCanceledOnTouchOutside(false)

        if(isScreenLargeOrXlarge) {
            endVCDialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }else {
            endVCDialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        dialogBinding.negBtn.setOnClickListener {
            endVCDialog.dismiss()
//                viewModel.isEndVcEnabled.value = true
            Log.d(TAG, "showAlertToEndtheUserCall: stopVc: true: can")

        }
        dialogBinding.posBtn.setOnClickListener {
            endVCDialog.dismiss()
            //show progress dialog
            // check if the userType is customer if yes , then make api call to update vc status else endVC
            // based on the response if success show rate us dialog if failed then, show show errorr message with okay button
            // on click of okay button, open feedback screen

            showProgressDialog()

            if(viewModel.userType == VCConstants.UserType.CUSTOMER.value) {
                //updateVcStatusForCustomer
                makeApiCallToUpdateVcStatus()
            }else {
                executeEndVc()
            }


        }
        //dialog alignment and size code.
//        val lp = WindowManager.LayoutParams()
//        lp.copyFrom(endVCDialog.window?.attributes)
//        lp.width = WindowManager.LayoutParams.MATCH_PARENT
//        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
//        lp.gravity = Gravity.CENTER
//        endVCDialog.window?.attributes = lp
//        endVCDialog.getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));

        endVCDialog.show()
    }

    private fun makeApiCallToUpdateVcStatus() {
        if (AndroidUtils.isNetworkAvailable(this)) {
            viewModel.updateVCStatusForCustomerNew(PreferenceManager.getBaseUrl()!!,getVideoStatusObjectCustomer())
        } else {
            dismissProgressDialog()
            viewModel.toastMessage.value = "No Internet Connection"
        }
    }
    private fun getVideoStatusObjectCustomer(): RequestModelUpdateVcStatusCustomer {
        return RequestModelUpdateVcStatusCustomer(
            userName = viewModel.userName.toString(),
            meetingCode = viewModel.roomID.toString(),
            vcStatus = "D",
            dealerNumber = viewModel.dealerCode.toString(),
        )
    }

    private fun executeEndVc() {
        if (conferenceManager != null) {
            if (conferenceManager!!.isJoined) {
                viewModel.endVCByUser = true
                conferenceManager!!.leaveFromConference()
                stoppedStream = true
            }
        }
        dismissProgressDialog()
        finishActivity(true)

    }

    private fun displayRendererMap() {
        Log.d(TAG, "displayRendererMap: ---- Renderer Map Start----")
//        for(entry in trackRendererMap){
//            Log.d(TAG, "displayRendererMap: ${entry.key.id} ->  ${entry.value.toString()}")
//        }
        for (entry in trackRelMap) {
            Log.d(TAG, "displayRendererMap: ${entry.key.id} ->  ${entry.value.toString()}")
        }
        Log.d(TAG, "displayRendererMap: ---- Renderer Map End----")
    }

    private fun initTelephonyManagerListener() {
        Log.d(TAG, "initTelephonyManagerListener: start")
        if (telephonyManager == null) {
            telephonyManager = this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyManager!!.registerTelephonyCallback(
                    this.mainExecutor,
                    object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                        override fun onCallStateChanged(state: Int) {
                            when (state) {
                                TelephonyManager.CALL_STATE_IDLE -> {
                                    Log.d(TAG, "onCallStateChanged: StateTest: State:Idle")
                                    //phone is neither ringing nor in a call
                                    if(viewModel.isInPhoneCall.value!=null) {
                                        if (viewModel.isInPhoneCall.value!!) {
                                            viewModel.isInPhoneCall.value = false
                                            viewModel.isPhoneCallEnded.value = true
                                        }
                                    }else {

                                    }

                                }
                                TelephonyManager.CALL_STATE_RINGING -> {
                                    Log.d(TAG, "onCallStateChanged: StateTest: Ringing: ")
                                    viewModel.isInPhoneCall.value = true
                                    viewModel.isPhoneCallEnded.value = false
                                }
                                TelephonyManager.CALL_STATE_OFFHOOK -> {
                                    Log.d(TAG, "onCallStateChanged: StateTest: OffHook: ")
                                    viewModel.isInPhoneCall.value = true
                                }
                            }
                        }
                    })
            } else {
                telephonyManager!!.listen(object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        when (state) {
                            TelephonyManager.CALL_STATE_IDLE -> {
                                //phone is neither ringing nor in a call
                                Log.d(TAG, "onCallStateChanged: StateTest: State:Idle")
                                if (viewModel.isInPhoneCall.value!!) {
                                    viewModel.isInPhoneCall.value = false
                                    viewModel.isPhoneCallEnded.value = true
                                }
                            }
                            TelephonyManager.CALL_STATE_RINGING -> {
                                Log.d(TAG, "onCallStateChanged: StateTest: Ringing: ")
                                viewModel.isInPhoneCall.value = true
                                viewModel.isPhoneCallEnded.value = false
                            }
                            TelephonyManager.CALL_STATE_OFFHOOK -> {
                                Log.d(TAG, "onCallStateChanged: StateTest: OffHook: ")
                                viewModel.isInPhoneCall.value = true
                            }

                        }
                    }
                }, PhoneStateListener.LISTEN_CALL_STATE)
            }
        }
        Log.d(TAG, "initTelephonyManagerListener: end")

    }


    private fun showReconnectionVCDialog() {
        Log.d(TAG, "showReconnectionVCDialog: ")
        if (this::reconnectionVCDialog.isInitialized) {
            if (reconnectionVCDialog.isShowing) {
                reconnectionVCDialog.dismiss()
            }
        }
        reconnectionVCDialog = Dialog(this)
        reconnectionVCDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = RejoinDialogLayoutBinding.inflate(LayoutInflater.from(this))
        reconnectionVCDialog.setContentView(dialogBinding.root)
        reconnectionVCDialog.setCancelable(false)
        reconnectionVCDialog.setCanceledOnTouchOutside(false)
//        reconnectionVCDialog.window?.setLayout(
//            ViewGroup.LayoutParams.MATCH_PARENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT
//        )

        if(isScreenLargeOrXlarge) {
            reconnectionVCDialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }else {
            reconnectionVCDialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        dialogBinding.negBtn.setOnClickListener {
            reconnectionVCDialog.dismiss()
//            goBack()
//            if (conferenceManager != null) {
//                if (!conferenceManager!!.isJoined) {
//                    viewModel.endVCByUser = true
//                    conferenceManager!!.leaveFromConference()
//                    stoppedStream = true
//                }
//            }
//            finishActivity(true)
            showEndVCDialog()
        }
        dialogBinding.posBtn.setOnClickListener {
            if (AndroidUtils.isNetworkOnLine(this)) {
                Log.d(TAG, "openJoinVCRoomDialog: internetPresent: ")
                rejoinConferenceRestartConference()
//                rejoinConferenceRestartActivity()
                reconnectionVCDialog.dismiss()
            } else {
                Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show()
            }
        }
//        if (!conferenceManager!!.isJoined) {
//            Log.d(TAG, "rejoinConference: before leave from conference")
//            conferenceManager!!.leaveFromConference()
//            stoppedStream = true
//
//        }


//        val lp = WindowManager.LayoutParams()
//        lp.copyFrom(reconnectionVCDialog.window?.attributes)
//        lp.width = WindowManager.LayoutParams.MATCH_PARENT
//        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
//        lp.gravity = Gravity.CENTER
//        reconnectionVCDialog.window?.attributes = lp
//        reconnectionVCDialog.getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
        reconnectionVCDialog.show()
    }


    private fun rejoinConference() {
        Log.d(TAG, "rejoinConference: ")
        if (conferenceManager != null) {
            Log.d(TAG, "rejoinConference: ")
            if (!conferenceManager!!.isJoined) {
                Log.d(TAG, "rejoinConference: before leave from conference")
                conferenceManager!!.leaveFromConference()
                stoppedStream = true
            }
            Log.d(TAG, "rejoinConference:end ")
//            reInitializeConference()
            publisherContainer = null
            conferenceManager = null
//            initConferenceManager(true)
        }

    }


    private fun addNewContainer(newContainer: RemotePeerView) {
        val containerMargin = resources.getDimension(com.intuit.sdp.R.dimen._2sdp).toInt()
        /*add to F::remove first from F and add to S*/
        val fContainerLayoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
//        newContainer.setPadding(containerMargin,containerMargin,containerMargin,containerMargin)
//        newContainer.setBackgroundColor(getColor(android.R.color.holo_blue_dark))
        newContainer.layoutParams = fContainerLayoutParams
//        binding.sContainer.removeView(newRenderer
        binding.fContainer.addView(newContainer, binding.fContainer.childCount)

        val k = binding.fContainer.getChildAt(0)
        binding.fContainer.removeViewAt(0)

        var sContainerLayoutParams:LinearLayout.LayoutParams? = null
        if(isScreenLargeOrXlarge) {
            sContainerLayoutParams= LinearLayout.LayoutParams(sContainerSizeLandscape, sContainerSizeLandscape)
        }else {
            sContainerLayoutParams= LinearLayout.LayoutParams(sContainerSizePortrait, sContainerSizePortrait)
        }


        sContainerLayoutParams.setMargins(
            containerMargin,
            0,
            containerMargin,
            0
        )
//        k.setPadding(containerMargin,containerMargin,containerMargin,containerMargin)
//        k.setBackgroundColor(getColor(android.R.color.holo_blue_dark))
        k.layoutParams = sContainerLayoutParams

//        k.translationZ = 3F
        binding.sContainer.addView(k, binding.sContainer.childCount)
        binding.sContainer.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        for (i in binding.sContainer.children) {
            i.visibility = View.VISIBLE
            val k = i
            binding.sContainer.removeView(i)
            binding.sContainer.addView(k)
            k.layoutParams = sContainerLayoutParams
        }


//        for(i in 0..binding.sContainer.childCount)
//        {
//            var temp = binding.sContainer.getChildAt(i)
//            binding.sContainer.removeView(temp)
//            binding.sContainer.addView(temp)
//            binding.sContainer.getChildAt(i).visibility = View.VISIBLE
//        }
        Log.d(TAG, "addNewContainer: binding.sContainer count " + binding.sContainer.childCount)

    }

    private fun removeEndedContainer(leftContainer: RemotePeerView) {
        var containerMargin = resources.getDimension(com.intuit.sdp.R.dimen._2sdp).toInt()
        if (leftContainer.parent == binding.fContainer) {
            Log.d(TAG, "removeEndedContainer: in the fContainer")
            /*if the renderer is in F...then remove from F and add last added renderer of S to F*/
            binding.fContainer.removeView(leftContainer)
            if (binding.sContainer.childCount >= 1) {
                val k = binding.sContainer.getChildAt(binding.sContainer.childCount - 1)
                binding.sContainer.removeViewAt(binding.sContainer.childCount - 1)
                val fContainerLayoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
//                k.setPadding(containerMargin,containerMargin,containerMargin,containerMargin)
//                k.setBackgroundColor(getColor(android.R.color.holo_blue_dark))
                k.layoutParams = fContainerLayoutParams
                binding.fContainer.addView(k)
            }
        } else {
            /*if the rendere is in S..simply just remove the view from S*/
            binding.sContainer.removeView(leftContainer)
        }
//        val sContainerLayoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(500, 500)
        var sContainerLayoutParams:LinearLayout.LayoutParams? = null
        if(isScreenLargeOrXlarge) {
            sContainerLayoutParams= LinearLayout.LayoutParams(sContainerSizeLandscape, sContainerSizeLandscape)
        }else {
            sContainerLayoutParams= LinearLayout.LayoutParams(sContainerSizePortrait, sContainerSizePortrait)
        }
        sContainerLayoutParams.setMargins(
            containerMargin,
            0,
            containerMargin,
            0
        )
        for (i in binding.sContainer.children) {
            i.visibility = View.VISIBLE
            var k = i;
            binding.sContainer.removeView(i)
            binding.sContainer.addView(k)
            k.layoutParams = sContainerLayoutParams
        }
        Log.d(
            TAG,
            "removeEndedContainer: binding.sContainer count " + binding.sContainer.childCount
        )

    }


    private fun swapContainer(clickedContainer: RemotePeerView) {
        Log.d(TAG, "swapContainer: SWAP ")
        val containerMargin = resources.getDimension(com.intuit.sdp.R.dimen._2sdp).toInt()
        /*add to F::remove first from F and add to S*/
        if (clickedContainer.parent == binding.sContainer) {

            Log.d(TAG, "swapContainer: position : clickedPosition:  ${binding.sContainer.indexOfChild(clickedContainer)}")
            Log.d(TAG, "swapContainer: position: childCount : ${binding.sContainer.childCount}")
            Log.d(TAG, "swapContainer: position: childCount+1 : ${binding.sContainer.indexOfChild(clickedContainer)+1}")

            var clickedIndex = binding.sContainer.indexOfChild(clickedContainer)


            binding.sContainer.removeView(clickedContainer)
            val fContainerLayoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
//            clickedContainer.setPadding(containerMargin,containerMargin,containerMargin,containerMargin)
//            clickedContainer.setBackgroundColor(getColor(android.R.color.holo_blue_dark))
            clickedContainer.layoutParams = fContainerLayoutParams
//        binding.sContainer.removeView(newRenderer
            binding.fContainer.addView(clickedContainer, binding.fContainer.childCount)

            val k = binding.fContainer.getChildAt(0)
            binding.fContainer.removeViewAt(0)
//            val sContainerLayoutParams: LinearLayout.LayoutParams =
//                LinearLayout.LayoutParams(500, 500)
            var sContainerLayoutParams:LinearLayout.LayoutParams? = null
            if(isScreenLargeOrXlarge) {
                sContainerLayoutParams= LinearLayout.LayoutParams(sContainerSizeLandscape, sContainerSizeLandscape)
            }else {
                sContainerLayoutParams= LinearLayout.LayoutParams(sContainerSizePortrait, sContainerSizePortrait)
            }
            sContainerLayoutParams.setMargins(
                containerMargin,
                0,
                containerMargin,
                0
            )
//            k.setPadding(containerMargin,containerMargin,containerMargin,containerMargin)
//            k.setBackgroundColor(getColor(android.R.color.holo_blue_dark))
            k.layoutParams = sContainerLayoutParams



            for (i in binding.sContainer.children) {
                i.visibility = View.VISIBLE
                val k = i
                binding.sContainer.removeView(i)
                binding.sContainer.addView(k)
                k.layoutParams = sContainerLayoutParams
            }

            binding.sContainer.addView(k, clickedIndex)
        } else {
            Log.d(TAG, "swapContainer: SWAP already in F")
        }

//        val sContainerLayoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(500, 500)
        var sContainerLayoutParams:LinearLayout.LayoutParams? = null
        if(isScreenLargeOrXlarge) {
            sContainerLayoutParams= LinearLayout.LayoutParams(sContainerSizeLandscape, sContainerSizeLandscape)
        }else {
            sContainerLayoutParams= LinearLayout.LayoutParams(sContainerSizePortrait, sContainerSizePortrait)
        }
        sContainerLayoutParams.setMargins(
            containerMargin,
            0,
            containerMargin,
            0
        )

        Log.d(TAG, "swapContainer: binding.sContainer count " + binding.sContainer.childCount)


    }

    private fun updateStreamNameTextView(container: RemotePeerView, name: String) {
        Log.d(TAG, "updateStreamNameTextView: name -> ${name}")
        container.streamName.text = name
    }

    private fun processIncomingDataChannelMessage(jsonObject: JSONObject) {
        Log.d(TAG, "processIncomingDataChannelMessage: ")
        try {
            val eventType =
                jsonObject.getString(VCConstants.EVENT_TYPE)
            val sId =
                jsonObject.getString(VCConstants.STREAM_ID)
            Log.d(TAG, "processIncomingDataChannelMessage: eventType: $eventType")
            when (eventType) {
                MIC_MUTED -> {
                    processMicrophoneDataChannelMessage(sId, eventType)
                }

                MIC_UNMUTED -> {
                    processMicrophoneDataChannelMessage(sId, eventType)
                }

                CAM_TURNED_ON -> {
                    processCameraDataChannelMessage(sId, eventType)

                }

                CAM_TURNED_OFF -> {
                    processCameraDataChannelMessage(sId, eventType)

                }

                UPDATE_STATUS -> {
                    val info = jsonObject.getJSONObject("info")
                    val micStatus =
                        info.getBoolean(MIC_STATUS)
                    val cameraStatus =
                        info.getBoolean(CAMERA_STATUS)
                    cameraStatus?.let {
                        if (it) {
                            processCameraDataChannelMessage(sId, CAM_TURNED_ON)
                        } else {
                            processCameraDataChannelMessage(sId, CAM_TURNED_OFF)
                        }
                    }
                    micStatus?.let {
                        if (it) {
                            processMicrophoneDataChannelMessage(sId, MIC_UNMUTED)
                        } else {
                            processMicrophoneDataChannelMessage(sId, MIC_MUTED)
                        }
                    }


                }

                TEXT_MESSAGE -> {
                    processTextMessageFromDataChannel(jsonObject, sId, eventType)
                }

                FILE_MESSAGE -> {
                    processFileMessageFromDataChannel(jsonObject, sId, eventType)

                }
                ESTIMATION_MESSAGE-> {
                    Log.d(TAG, "processIncomingDataChannelMessage: testRemoteEstimate:  estimationMessageRecieved: True")

                    playNotificationSound(this)
                    val rootView = findViewById<View>(android.R.id.content)
                    val snackbar = Snackbar.make(rootView, "Estimation Received", Snackbar.LENGTH_SHORT)
                    snackbar.show()

                    processEstimationMessageFromDataChannel(jsonObject,sId,eventType)
                }

            }

        } catch (e: JSONException) {
            Log.d(
                TAG,
                "processIncomingDataChannelMessage: exception during processing of the message"
            )
//            throw java.lang.RuntimeException(e) //just comment out for if no exception to be thrown
        }

    }


    private fun processMicrophoneDataChannelMessage(streamId: String?, eventType: String) {
        streamId?.let {
            if (conferenceManager?.streamId.equals(it)) {
                /*this should never happen -> just for safety purposes */
                Log.d(TAG, "processMicrophoneDataChannelMessage: ")
            } else {
                for (e in trackRelMap) {
                    if (e.value?.contains(streamId) == true) {
                        Log.d(TAG, "processMicrophoneDataChannelMessage: found the track")
                        updateMicrophoneUIForContainer(e.key, eventType)
                        updateMicrophoneStatusForParticipant(streamId, eventType, false)
                    }
                }
            }

        }
    }

    private fun processCameraDataChannelMessage(streamId: String?, eventType: String) {
        streamId?.let {
            if (conferenceManager?.streamId.equals(it)) {
                /*this should never happen -> just for safety purposes */
                Log.d(TAG, "processMicrophoneDataChannelMessage: ")
            } else {
                for (e in trackRelMap) {
                    if (e.value?.contains(streamId) == true) {
                        Log.d(TAG, "processMicrophoneDataChannelMessage: found the track")
                        updateCameraUIForContainer(e.key, eventType)
                        updateCameraStatusForParticipant(streamId, eventType, false)
                    }
                }
            }

        }
    }


    private fun updateMicrophoneUIForContainer(remotePeerView: RemotePeerView, eventType: String) {
        Log.d(TAG, "updateMicrophoneUIForContainer: ")
        if (trackRelMap.containsKey(remotePeerView)) {
            processMicImageUI(remotePeerView, eventType)
        } else {
            /*do nothing*/
        }
    }

    private fun updateCameraUIForContainer(remotePeerView: RemotePeerView, eventType: String) {
        Log.d(TAG, "updateCameraUIForContainer: ")
        if (trackRelMap.containsKey(remotePeerView)) {
            processCameraImageUI(remotePeerView, eventType)
        } else {
            /*do nothing*/
            Log.d(TAG, "updateCameraUIForContainer: video image not found")
        }
        if (trackRelMap.containsKey(remotePeerView)) {
            processSurfaceRendererBgUI(remotePeerView, eventType)
        } else {
            Log.d(TAG, "updateCameraUIForContainer: surface renderer not found")
        }
    }

    private fun processCameraUIForPublishContainer(eventType: String) {
        Log.d(TAG, "processCameraUIForPublishContainer: ")
        if (eventType.equals(CAM_TURNED_OFF)) {
            /*change Video UI to video muted UI*/
//            binding.btnVideo.setBackgroundResource(R.drawable.bg_rounded_new)
            binding.btnVideo.setImageResource(R.drawable.ic_video_disabled)
//            binding.btnVideo.setColorFilter(ContextCompat.getColor(this, R.color.black))
        } else if (eventType.equals(CAM_TURNED_ON)) {
            /*change Video UI to video active UI*/
            binding.btnVideo.setBackgroundResource(0)
            binding.btnVideo.setImageResource(R.drawable.ic_video_enabled)
//            binding.btnVideo.setColorFilter(ContextCompat.getColor(this, R.color.white))
        } else {
            /*do nothing*/
        }
        processCameraImageUI(publisherContainer!!, eventType)
        processSurfaceRendererBgUI(publisherContainer!!, eventType)
        updateCameraStatusForParticipant(conferenceManager!!.streamId, eventType, true)

    }


    private fun processMicUIForPublishContainer(eventType: String) {
        Log.d(TAG, "processMicUIForPublishContainer: ")
        if (eventType.equals(MIC_MUTED)) {
            /*change MIC UI to mic muted UI*/
//            binding.btnAudio.setBackgroundResource(R.drawable.bg_rounded_new)
            binding.btnAudioSwitch.setImageResource(R.drawable.ic_mic_disabled)
//            binding.btnAudio.setColorFilter(ContextCompat.getColor(this, R.color.black))
        } else if (eventType.equals(MIC_UNMUTED)) {
            /*change MIC UI to mic active UI*/
//            binding.btnAudio.setBackgroundResource(0)
            binding.btnAudioSwitch.setImageResource(R.drawable.ic_mic_enabled)

//            binding.btnAudio.setColorFilter(ContextCompat.getColor(this, R.color.white))
        } else {
            /*do nothing*/
        }
        processMicImageUI(publisherContainer!!, eventType)
        updateMicrophoneStatusForParticipant(conferenceManager!!.streamId, eventType, true)

    }

    private fun processMicImageUI(remotePeerView: RemotePeerView, eventType: String) {
        if (eventType.equals(MIC_MUTED)) {
            /*change MIC UI to mic muted UI*/
            remotePeerView.changeAudioActiveStatus(false)
        } else if (eventType.equals(MIC_UNMUTED)) {
            /*change MIC UI to mic active UI*/
            remotePeerView.changeAudioActiveStatus(true)
        } else {
            /*do nothing*/
        }
    }

    private fun processCameraImageUI(remotePeerView: RemotePeerView, eventType: String) {
        if (eventType.equals(CAM_TURNED_OFF)) {
            /*change Video UI to video muted UI*/
            remotePeerView.changeVideoActiveStatus(false)
        } else if (eventType.equals(CAM_TURNED_ON)) {
            /*change Video UI to video active UI*/
            remotePeerView.changeVideoActiveStatus(true)
        } else {
            /*do nothing*/
        }
    }

    private fun processSurfaceRendererBgUI(remotePeerView: RemotePeerView, eventType: String) {
        Handler().postDelayed({
            if (eventType.equals(CAM_TURNED_OFF)) {
                /*change surface bg UI to single character UI*/
                applyDefaultBgToRenderer(remotePeerView)
            } else if (eventType.equals(CAM_TURNED_ON)) {
                /*change surface  UI to null*/
                removeDefaultBgFromRenderer(remotePeerView)
            } else {
                /*do nothing*/
            }
        }, 500)
    }

    private fun processSurfaceRendererBgUiForScreenShare(remotePeerView: RemotePeerView, eventType: String) {
        Handler().postDelayed({
            if (eventType.equals(SCREEN_SHARE_DISABLED)) {
                /*change surface bg UI to single character UI*/
                removeDefaultBgFromRenderer(remotePeerView)

            } else if (eventType.equals(SCREEN_SHARE_ENABLED)) {
                /*change surface  UI to null*/
                applyDefaultBgToRenderer(remotePeerView)
            } else {
                /*do nothing*/
            }
        }, 500)
    }


    private fun applyDefaultBgToRenderer(remotePeerView: RemotePeerView) {
        remotePeerView.addCharacterBg()
    }


    private fun removeDefaultBgFromRenderer(remotePeerView: RemotePeerView) {
        remotePeerView.removeCharacterBg()
    }

    private fun openParticipantsListInRoom() {
        var participantFragment = supportFragmentManager.findFragmentByTag(PARTICIPANT_FRAG)
        if (participantFragment == null) {
            showMoreOptions(makeMoreOptionsVisible = false)
//            val participantsBundle = Bundle()
            // Add data to participantsBundle if needed

            val participants = ParticipantFragment()
//            participants.arguments = participantsBundle

            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(participants, PARTICIPANT_FRAG)
            transaction.commit()

//            viewModel.showParticipants.value = false
//            viewModel.isParticipantsClickable.value = true
        } else {
//            viewModel.isParticipantsClickable.value = true
        }
        viewModel.participantFragVisible = true
    }

    /*audio 22 sep 2023 */
    private fun getAudioDevices(): Set<AudioDevice>? {
        Log.d(TAG, "getAudioDevices: ")
        if (conferenceManager != null) {
            if (conferenceManager!!.isJoined) {

                if (conferenceManager!!.publishWebRTCClient != null && conferenceManager!!.publishWebRTCClient.isStreaming()) {
                    return conferenceManager!!.publishWebRTCClient.audioManager!!.audioDevices
                } else {
                    return null
                }
            }
            return null
        }
        return null
    }

    fun getCurrentSelectedAudioDevice(): AudioDevice? {
        Log.d(TAG, "getCurrentSelectedAudioDevice: ")
        if (conferenceManager != null) {
            if (conferenceManager!!.isJoined) {

                return if (conferenceManager!!.publishWebRTCClient != null && conferenceManager!!.publishWebRTCClient.isStreaming) {
                    conferenceManager!!.publishWebRTCClient.audioManager!!.selectedAudioDevice
                } else {
                    null
                }
            }
            return null
        }
        return null
    }

    private fun changeAudioDevice(device: AudioDevice) {
        Log.d(TAG, "changeAudioDevice:  ${device.name}")
        if (conferenceManager != null) {
            if (conferenceManager!!.isJoined) {
                Log.d(TAG, "changeAudioDevice: is joined")
                if (conferenceManager!!.publishWebRTCClient != null && conferenceManager!!.publishWebRTCClient.isStreaming) {
                    Log.d(TAG, "changeAudioDevice: before change")
                    conferenceManager!!.publishWebRTCClient.audioManager!!.selectAudioDevice(device)
                }

            }
        }

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "onConfigurationChanged: new Configuration ->${newConfig} ")
//        if (this::conferenceManager != null && conferenceManager!!.isJoined) {
//            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                Log.d(TAG, "onConfigurationChanged: ORIENTATION_LANDSCAPE")
////                conferenceManager.sendOrientationNotification(false)
//            } else {
//                Log.d(TAG, "onConfigurationChanged: ORIENTATION_PORTRAIT")
////                conferenceManager.sendOrientationNotification(true)
//            }
//        }
    }

    private fun setUpBottomSheetBehaviour() {
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // handle onSlide
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        /*to be implemented*/
//                        vcScreenBinding.playViewRenderer1.setZOrderMediaOverlay(true)
//                        vcScreenBinding.playViewRenderer2.setZOrderMediaOverlay(true)
//                        vcScreenBinding.playViewRenderer3.setZOrderMediaOverlay(true)
//                        vcScreenBinding.playViewRenderer4.setZOrderMediaOverlay(true)
//                        vcScreenBinding.publishViewRenderer.setZOrderMediaOverlay(true)
                    }

                    BottomSheetBehavior.STATE_EXPANDED -> {
//                        vcScreenBinding.playViewRenderer1.setZOrderMediaOverlay(false)
//                        vcScreenBinding.playViewRenderer2.setZOrderMediaOverlay(false)
//                        vcScreenBinding.playViewRenderer3.setZOrderMediaOverlay(false)
//                        vcScreenBinding.playViewRenderer4.setZOrderMediaOverlay(false)
//                        vcScreenBinding.publishViewRenderer.setZOrderMediaOverlay(false)
                        /*to be implemented*/

                    }

                    BottomSheetBehavior.STATE_DRAGGING -> {
                        /*to be implemented*/

                    }

                    BottomSheetBehavior.STATE_SETTLING -> {
                        /*to be implemented*/

                    }

                    BottomSheetBehavior.STATE_HIDDEN -> {
                        /*to be implemented*/

                    }

                }
            }
        })
    }

    private fun showMoreOptions(makeMoreOptionsVisible: Boolean) {
        /*implement the show more options logic*/
        if (makeMoreOptionsVisible) {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            else
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun addInitialLocalParticipant(streamId: String) {
        var tempParticipant = conferenceManager?.let {
            ParticipantsModel(
                "", it.streamId ?: "publish", true,
                it.isPublisherAudioOn, conferenceManager!!.isPublisherVideoOn, null
            )
        }
        if (tempParticipant != null) {
            tempParticipant.displayName = viewModel.displayName?:"Local"
            viewModel.participants.add(tempParticipant)
        }
        Log.d(
            TAG,
            "addInitialLocalParticipant: participant -> ${Gson().toJson(viewModel.participants)}"
        )
        viewModel.participantCount.value = " ${viewModel.participants.size} "
        viewModel.updateParticipants.value = true // adding local participants when publish started
    }

    private fun addNewParticipant(track: VideoTrack, isLocal: Boolean) {
        if (isLocal) {
            /*already added initally-> udpate the track id*/
            var isFound = false /*not required ..but just to be sure*/
            for (participant in viewModel.participants) {
                if (participant.streamId == conferenceManager!!.streamId) {
                    isFound = true
                    participant.trackId = track.id()
                    break
                }
            }
            if (!isFound) {
                var tempParticipant = conferenceManager?.let {
                    ParticipantsModel(
                        track.id(), it.streamId ?: "publish", true,
                        it.isPublisherAudioOn, conferenceManager!!.isPublisherVideoOn, track
                    )
                }
                if (tempParticipant != null) {
                    viewModel.participants.add(tempParticipant)
                }
            } else {
                /*do nothing*/
            }
        } else {
            Log.d(TAG, "addNewParticipant: trackId:  ${track.id()}")


            val pattern = Regex("""v(stream\d+)""")
            val matchResult = pattern.find(track.id())

            val streamId = matchResult?.groupValues?.get(1)
            Log.d(TAG, "addNewParticipant: test22: StreamId: ${streamId}")
            var tempParticipant = conferenceManager?.let {
                ParticipantsModel(
                    track.id(), streamId ?: "peer", false,
                    isMicOn = true, isCamOn = true, track = track
                )
            }
            if (tempParticipant != null) {
                viewModel.participants.add(tempParticipant)
            }

        }
        /*notify adapter/ other places*/
        Log.d(TAG, "addNewParticipant: participants -> ${Gson().toJson(viewModel.participants)}")
        viewModel.participantCount.value = " ${viewModel.participants.size} "
        viewModel.updateParticipants.value = true // add new particiant on NewVideo Track
    }

    private fun removeParticipant(track: VideoTrack) {
        var participantPosition = -1
        for (partiipant in viewModel.participants) {
            if (track.id().equals(partiipant.trackId)) {
                participantPosition = viewModel.participants.indexOf(partiipant)
                break
            }
        }
        if (participantPosition != -1) {
            viewModel.participants.removeAt(participantPosition)
        }
        /*notify adapter/ other places*/
        Log.d(TAG, "removeParticipant: participants -> ${Gson().toJson(viewModel.participants)}")
        viewModel.participantCount.value = " ${viewModel.participants.size} "
        viewModel.updateParticipants.value = true // remove participants
    }

    private fun updateMicrophoneStatusForParticipant(
        streamID: String,
        eventType: String,
        isForLocal: Boolean
    ) {
        Log.d(TAG, "updateMicrophoneStatusForParticipant: test2244:1 streamId: ${streamID} eventType : ${eventType} isLocal : ${isForLocal}")
        var isAudioOn = if (eventType.equals(VCConstants.MIC_MUTED)) {
            false
        } else eventType.equals(VCConstants.MIC_UNMUTED)

        Log.d(TAG, "updateMicrophoneStatusForParticipant: test2244:2 ${Gson().toJson(viewModel.participants)}")

        for (participant in viewModel.participants) {
            Log.d(TAG, "updateMicrophoneStatusForParticipant: test2244:3 pStreamId: ${participant.streamId} uStreamId : ${streamID}")
            if (isForLocal) {
                Log.d(TAG, "updateMicrophoneStatusForParticipant: test2244:4: localParticipant: true: break")
                if(participant.isLocal) {
                    participant.isMicOn = isAudioOn
                }
//                break
            } else {
                Log.d(TAG, "updateMicrophoneStatusForParticipant: test2244:5: localParticipant: false: ")
                if (participant.trackId.contains(streamID)) {
                    Log.d(TAG, "updateMicrophoneStatusForParticipant: test2244:6  trackId: containsStreamId: true break   ")
                    participant.isMicOn = isAudioOn
                    break
                }else {
                    Log.d(TAG, "updateMicrophoneStatusForParticipant: test2244:7  trackId: containsStreamId: false")
                }
            }
        }
        Log.d(
            TAG,
            "updateMicrophoneStatusForParticipant: participants -> ${Gson().toJson(viewModel.participants)}"
        )
        viewModel.participantCount.value = " ${viewModel.participants.size} "
        viewModel.updateParticipants.value = true // update mic status for a participant
    }

    private fun updateCameraStatusForParticipant(
        streamID: String,
        eventType: String,
        isForLocal: Boolean
    ) {
        var isCamOn = if (eventType.equals(VCConstants.CAM_TURNED_OFF)) {
            false
        } else eventType.equals(VCConstants.CAM_TURNED_ON)
        for (participant in viewModel.participants) {
            if (isForLocal) {
//                participant.isCamOn = isCamOn
//                break
                if(participant.isLocal) {
                    participant.isCamOn = isCamOn
                }
            } else {
                if (participant.trackId.contains(streamID)) {
                    participant.isCamOn = isCamOn
                    break
                }
            }
        }
        Log.d(
            TAG,
            "updateCameraStatusForParticipant: participants -> ${Gson().toJson(viewModel.participants)}"
        )
        viewModel.participantCount.value = " ${viewModel.participants.size} "
        viewModel.updateParticipants.value = true // update camera status for participants

    }

    private fun removeLocalParticipant(streamId: String) {
        var participantPosition = -1
        for (partiipant in viewModel.participants) {
            if (partiipant.isLocal) {
                participantPosition = viewModel.participants.indexOf(partiipant)
                break
            }
        }
        if (participantPosition != -1) {
            viewModel.participants.removeAt(participantPosition)
        }
        /*notify adapter/ other places*/
        Log.d(
            TAG,
            "removeInitialLocalParticipant: participants -> ${Gson().toJson(viewModel.participants)}"
        )
    }


    private fun openSoundDeviceListInRoom() {
        var soundDeviceFrag = supportFragmentManager.findFragmentByTag(SOUND_DEVICE_FRAG)
        if (soundDeviceFrag == null) {
            showMoreOptions(makeMoreOptionsVisible = false)
//            val participantsBundle = Bundle()
            // Add data to participantsBundle if needed

            val soundDeviceFragment = SoundDeviceFragment()
//            participants.arguments = participantsBundle

            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(soundDeviceFragment, SOUND_DEVICE_FRAG)
            transaction.commit()

//            vCScreenViewModel.showParticipants.value = false
//            vCScreenViewModel.isParticipantsClickable.value = true
        } else {
//            vCScreenViewModel.isParticipantsClickable.value = true
        }
        viewModel.soundDeviceFragVisible = true

    }

    private fun processAudioDeviceUpdateFromSDK() {
        Log.d(TAG, "processAudioDeviceUpdateFromSDK: ")
        viewModel.audioDevices.value = getAudioDevices()
        viewModel.currentSelectedAudioDevice.value = getCurrentSelectedAudioDevice()
        Log.d(
            TAG,
            "processAudioDeviceUpdateFromSDK: audio devices -> ${Gson().toJson(viewModel.audioDevices.value)}"
        )
        Log.d(
            TAG,
            "processAudioDeviceUpdateFromSDK: currentSelectedAudioDevice -> ${
                Gson().toJson(viewModel.currentSelectedAudioDevice.value)
            }"
        )
        viewModel.isAudioDeviceUpdated.value = true


    }

    private fun openScreenShareOptions() {
        Log.d(TAG, "openScreenShareOptions: ")
        var screenShareFragment = supportFragmentManager.findFragmentByTag(SCREEN_SHARE_FRAG)
        if (screenShareFragment == null) {
            var screenShare = ScreenShareFragment()
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(screenShare, SCREEN_SHARE_FRAG)
            transaction.commit()
        }
        viewModel.screenShareFragVisible = true
    }

    private fun startScreenShare() {
        Log.d(TAG, "startScreenShare: ")

        startScreenCaptureWithIntent()


    }

    private fun startScreenCaptureWithIntent() {
        Log.d(TAG, "startScreenCaptureWithIntent: ")
        val displayMetrics = getDisplayMetrics()
        displayMetrics?.let {
            videoWidth = it.widthPixels
            videoHeight = it.heightPixels
        }

        Log.d(
            TAG,
            "startScreenCaptureWithIntent: videoWidth -> ${videoWidth}::videoHeight -> ${videoHeight}"
        )
        // fix:- to remove the black boarder issue
//        val displayMetrics: DisplayMetrics = webRTCClient.getDisplayMetrics()
//        videoWidth = displayMetrics.widthPixels
//        videoHeight = displayMetrics.heightPixels

        this.intent.putExtra(CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED, true)
        this.intent.putExtra(CallActivity.EXTRA_VIDEO_BITRATE, 25)
        this.intent.putExtra(CallActivity.EXTRA_VIDEO_WIDTH, videoWidth)
        this.intent.putExtra(CallActivity.EXTRA_VIDEO_HEIGHT, videoHeight)
        //this.getIntent().putExtra(CallActivity.EXTRA_SCREENCAPTURE, true);
        this.intent.putExtra(CallActivity.EXTRA_SCREENCAPTURE, true);
        this.intent.putExtra(CallActivity.EXTRA_VIDEO_FPS, 30)


//        WebRTCClient.SOURCE_SCREEN - screen
//        WebRTCClient.SOURCE_FRONT - front screen
//        WebRTCClient.SOURCE_REAR - rear screen
        conferenceManager!!.publishWebRTCClient.changeVideoSource(WebRTCClient.SOURCE_SCREEN)
        if (conferenceManager!!.publishWebRTCClient.isScreenSharePermissionRequired) {
            /*permission not given...onACtivityREsult will handle the status change*/
        } else {
            /*permission already given change te status*/
            viewModel.toastMessage.value = "Screen share started"
            viewModel.updateScreenShareForFragment.value = true
            viewModel.screenShareStatus = true
            //Added to hide the insception during screen share
            processSurfaceRendererBgUiForScreenShare(publisherContainer!!, SCREEN_SHARE_ENABLED)
        }
//        val mediaProjectionManager = application.getSystemService(
//            Context.MEDIA_PROJECTION_SERVICE
//        ) as MediaProjectionManager
//        startActivityForResult(
//            mediaProjectionManager.createScreenCaptureIntent(),
//            200
//        )
    }


    private fun stopScreenShare() {
        Log.d(TAG, "stopScreenShare: ")
        viewModel.toastMessage.value = "Screen share stopped"
        viewModel.updateScreenShareForFragment.value = false
        viewModel.screenShareStatus = false
        //Added to handle screen flickered during screen-share
        processSurfaceRendererBgUiForScreenShare(publisherContainer!!, SCREEN_SHARE_DISABLED)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
////            stopService(
////                Intent(
////                    applicationContext,
////                    MediaProjectionService::class.java
////                )
////            )
//            val serviceIntent = Intent(this, MediaProjectionService::class.java)
//            serviceIntent.action = "STOP"
//            startForegroundService(serviceIntent)
//        }
        var source = if (viewModel.frontCamera) {
            WebRTCClient.SOURCE_FRONT
        } else WebRTCClient.SOURCE_REAR
        conferenceManager!!.publishWebRTCClient.changeVideoSource(source)
        //        publishStream?.setMediaProjectionParams(0, null)
//        publishStream?.stopScreenCapturePublish()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestcode->${requestCode}")
        // If the device version is v29 or higher, screen sharing will work service due to media projection policy.
        // Otherwise media projection will work without service
        /*commented for now ... need to uncomment for the screen share funcitonality*/
        when (requestCode) {
            1234 -> {
                if (resultCode == Activity.RESULT_OK) {
                    viewModel.toastMessage.value = "Screen share started"
                    viewModel.updateScreenShareForFragment.value = true
                    viewModel.screenShareStatus = true
                    // Added to handle insception of video frame in the publisher container
                    processSurfaceRendererBgUiForScreenShare(publisherContainer!!, SCREEN_SHARE_ENABLED)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaProjectionService.setListener { mediaProjection ->
                            conferenceManager!!.publishWebRTCClient.setMediaProjection(
                                mediaProjection
                            )
                            conferenceManager!!.publishWebRTCClient.onActivityResult(
                                requestCode,
                                resultCode,
                                data
                            )
                        }
                        val serviceIntent = Intent(this, MediaProjectionService::class.java)
                        serviceIntent.putExtra(EXTRA_MEDIA_PROJECTION_DATA, data)
                        startForegroundService(serviceIntent)
                    } else {
                        conferenceManager!!.publishWebRTCClient.onActivityResult(
                            requestCode,
                            resultCode,
                            data
                        )
                    }
                }

            }

            PICKFILE_RESULT_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "onActivityResult: intent from file explorer -> result ok")
                    processPickFileData(data)
                } else {
                    Log.d(TAG, "onActivityResult: intent from file explorer -> result not ok ")
                }
            }

        }
    }

    private fun getDisplayMetrics(): DisplayMetrics? {
        val displayMetrics = DisplayMetrics()
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        return displayMetrics
    }

    private fun processCameraErrorUpdateFromSDK() {
        Log.d(TAG, "processCameraErrorUpdateFromSDK: ")
        if (conferenceManager != null) {
            if (conferenceManager!!.isPublisherVideoOn) {
                Log.d(TAG, "processCameraErrorUpdateFromSDK: isPublisherVideoOn")
                conferenceManager!!.disableVideo()
                viewModel.localVideo = false
                processCameraUIForPublishContainer(CAM_TURNED_OFF)
                showDialogToEnableCamera()
            }
        }
        /*show dialog/notification to inform user on the event*/
    }

    private fun showDialogToEnableCamera() {
        if (!this::cameraDialog.isInitialized) {
            cameraDialog = Dialog(this)
            val dialogBinding = DialogCameraEnableBinding.inflate(LayoutInflater.from(this))
            cameraDialog.setContentView(dialogBinding.root)
            cameraDialog.setCancelable(false)
            cameraDialog.setCanceledOnTouchOutside(false)
            cameraDialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            /*msgTv.text = resources.getString(R.string.per_allow_msg)
            posBtn.text = resources.getString(R.string.allow)
            negBtn.text = resources.getString(R.string.per_cancel)*/

            dialogBinding.posBtn.setOnClickListener {
                Log.d(TAG, "showDialogToEnableCamera():posBtn")
                controlVideo()
                cameraDialog.dismiss()
            }
            dialogBinding.negBtn.setOnClickListener {
                Log.d(TAG, "showDialogToEnableCamera(): negBtn")
                cameraDialog.dismiss()
            }
            //dialog alignment and size code.
//            val lp = WindowManager.LayoutParams()
//            lp.copyFrom(cameraDialog.window?.attributes)
//            lp.width = WindowManager.LayoutParams.MATCH_PARENT
//            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
//            lp.gravity = Gravity.CENTER
//            cameraDialog.window?.attributes = lp
//            cameraDialog.getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
            cameraDialog.show()
        } else {
            cameraDialog.show()
        }

    }

    private fun reInitializeConference() {
        /*clear data from participant list*/
        /*clear track list
        * remove other track views*/
        clearRemoteUIs()
        //ignore
        intent.putExtra("intent_for_reconnect", true)
        intent.putExtra("stream_id_in_use", conferenceManager!!.publishWebRTCClient.streamId)
        intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        if (Build.VERSION.SDK_INT >= 11) {

            recreate()
            overridePendingTransition(0, 0)
        } else {
            finishActivity(true)

            overridePendingTransition(0, 0)
            startActivity(intent)
            overridePendingTransition(0, 0)

        }

    }

    private fun clearTrackList() {
        for (track in viewModel.tracks) {
            if (!track.id().contains(conferenceManager!!.streamId)) {
                Log.d(TAG, "clearTrackList: track ID -> ${track.id()}")
            }
        }
    }

    private fun processStreamListUpdateFromSDK() {
        Log.d(TAG, "processStreamListUpdateFromSDK: ")
//        if(conferenceManager!=null) {
//            if(conferenceManager!!.isJoined) {
//                conferenceManager!!.playWebRTCClient.leaveFromConference(roomId)
//            }
//        }
    }

    private fun clearRemoteUIs() {
        runOnUiThread {
            for (entry in trackRelMap) {

                removeEndedContainer(entry.key)

            }
            trackObjectRelMap.clear()
            trackRelMap.clear()
            viewModel.tracks.clear()
            viewModel.participants.clear()
            viewModel.roomInfoStreamsList.clear()
        }
    }

    private fun scheduleSendStatusTimer() {
        handler.postDelayed(sendStatusRunnable, STATUS_SEND_PERIOD_MILLIS.toLong())
    }

    fun sendStatusMessage() {
        if (conferenceManager != null) {
            try {
                val jsonObject = JSONObject()
                jsonObject.put(
                    MIC_STATUS,
                    conferenceManager!!.isPublisherAudioOn
                )
                jsonObject.put(
                    CAMERA_STATUS,
                    conferenceManager!!.isPublisherVideoOn
                )
                jsonObject.put(
                    VCConstants.SCREEN_SHARE_STATUS,
                    viewModel.screenShareStatus
                )
                conferenceManager!!.sendNotificationEventForStatus(
                    UPDATE_STATUS,
                    jsonObject
                )
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }
        }
    }


    private fun clearSendStatusSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (handler.hasCallbacks(sendStatusRunnable)) {
                handler.removeCallbacks(sendStatusRunnable)
            }
        } else {
            handler.removeCallbacks(sendStatusRunnable)
        }
    }

    private fun logInternetSpeed() {
        val context = applicationContext
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        val nc = cm.getNetworkCapabilities(cm.activeNetwork)

        if (netInfo != null && nc != null) {
//            val downSpeed = nc.linkDownstreamBandwidthKbps
//            val upSpeed = nc.linkUpstreamBandwidthKbps
//
//            // Log the internet speed
//            val speedLog = "Downstream Speed: $downSpeed Kbps, Upstream Speed: $upSpeed Kbps"
//
//            Log.d(TAG, "logInternetSpeed: ${speedLog}")
//            println(speedLog) // You can use Log.d() for Android logging


            val downSpeedKbps = nc.linkDownstreamBandwidthKbps
            val upSpeedKbps = nc.linkUpstreamBandwidthKbps

            val speedLogKbps = "D: $downSpeedKbps Kbps, U: $upSpeedKbps Kbps"

            val downSpeedMbps = downSpeedKbps / 1000.0
            val upSpeedMbps = upSpeedKbps / 1000.0

            // Log the internet speed in Mbps
            val speedLogInMbps = "D: $downSpeedMbps Mbps, U: $upSpeedMbps Mbps"
            println(speedLogInMbps) // You can use Log.d() for Android logging
//            Log.d(TAG, "logInternetSpeed: $speedLogInMbps")

            if (downSpeedMbps < 4.0) {
                Toast.makeText(
                    context,
                    "Low Internet speed. Functionalities of the VC may be impacted.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            viewModel.internetSpeed.value = speedLogInMbps


//            if(this::conferenceManager.isInitialized ) {
//                var webRTCClient: WebRTCClient? = conferenceManager.peers.get(conferenceManager.streamId)
//                Log.d(TAG, "logInternetSpeed: streamId : ${conferenceManager.streamId}")
//                Log.d(TAG, "logInternetSpeed: conferenceManager : IsInitailized")
//                Log.d(TAG, "logInternetSpeed: webrtcClient:  ${webRTCClient}")
//                Log.d(TAG, "logInternetSpeed: isStreaming:  ${webRTCClient?.isStreaming}")
//                if(webRTCClient!=null && webRTCClient?.isStreaming == false) {
//                    try {
//                        Log.d(TAG, "logInternetSpeed: publishStream : ")
//                        conferenceManager.publishStream(conferenceManager.streamId)
//                    }catch (e:Exception) {
//                        Log.d(TAG, "logInternetSpeed: exception : Found: ")
////                        showRejoinPopup()
//                    }
//                }else {
//                    Log.d(TAG, "logInternetSpeed: streaming:Success")
//                }
//            }else {
//                Log.d(TAG, "logInternetSpeed:  notInitialized: ")
//            }


        } else {
            viewModel.internetSpeed.value = "-"
//            Log.d(TAG, "logInternetSpeed:-")
        }

        // Schedule the next logging after 1 second
        handler.postDelayed(::logInternetSpeed, internetLogInterval)
    }

    override fun onStop() {
        super.onStop()
        removeLogInternetSpeedCallbacks()

    }

    private fun rejoinConferenceRestartActivity() {
        if (!conferenceManager!!.isJoined) {
            viewModel.endVCByUser = true
            conferenceManager!!.leaveFromConference()
            stoppedStream = true
        }
        intent.putExtra("intent_for_reconnect", true)
        intent.putExtra("stream_id_in_use", conferenceManager!!.publishWebRTCClient.streamId)
        intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        if (Build.VERSION.SDK_INT >= 11) {

            recreate();
            overridePendingTransition(0, 0);
        } else {
            finishActivity(true);

            overridePendingTransition(0, 0);
            startActivity(intent);
            overridePendingTransition(0, 0);

        }
    }

    private fun rejoinConferenceRestartConference() {
        Log.d(TAG, "rejoinConferenceRestartConference: ")
        viewModel.rejoinInProgress = true

        if (conferenceManager != null) {
            Log.d(TAG, "rejoinConference: ")
            if (!conferenceManager!!.isJoined) {

                Log.d(TAG, "rejoinConference: before leave from conference")
                conferenceManager!!.leaveFromConference()
                stoppedStream = true

            }
            Log.d(TAG, "rejoinConferenceRestartConference: ")
            clearSendStatusSchedule()
            removeLogInternetSpeedCallbacks()
            checkForScreenShareProcess()
            Log.d(TAG, "rejoinConference:end ")
//            reInitializeConference()
            conferenceManager = null
            Log.d(TAG, "rejoinConferenceRestartConference: ")
            clearRemoteUIs()
            showProgressDialog()
            Handler().postDelayed({
                initConferenceManager(true)
            }, 10000)

        }

    }

    private fun showInternetLostDialog() {
        Log.d(TAG, "showInternetLostDialog: ")
        if (this::noInternetDialog.isInitialized) {
            if (noInternetDialog.isShowing) {
                noInternetDialog.dismiss()
            }
        }
        noInternetDialog = Dialog(this)
        noInternetDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

//        joinVCDialog.context.setTheme(R.style.MyAlertDialogTheme)
        noInternetDialog.setContentView(R.layout.layout_no_internet_connection)
        if(isScreenLargeOrXlarge) {
            noInternetDialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }else {
            noInternetDialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        noInternetDialog.setCancelable(false)
//        val tryAgain = noInternetDialog.findViewById(R.id.btn_try_again) as Button
//        tryAgain.setOnClickListener {
//                if (AndroidUtils.isNetworkOnLine(this)) {
//                    noInternetDialog.dismiss()
//                }
//        }
        try {
            noInternetDialog.show()
        } catch (e: Exception) {
            Log.d(TAG, "showInternetLostDialog: exception caught!!-")
        }
    }


    private fun removeInternetLostDialog() {
        Log.d(TAG, "removeInternetLostDialog: ")
        if (this::noInternetDialog.isInitialized) {
            if (noInternetDialog.isShowing) {
                noInternetDialog.dismiss()
            }
        }
    }


    private fun removeLogInternetSpeedCallbacks() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (internetLogHandler.hasCallbacks(logInternetSpeedRunnable)) {
                internetLogHandler.removeCallbacks(logInternetSpeedRunnable)
            }
        } else {
            internetLogHandler.removeCallbacks(logInternetSpeedRunnable)
        }
    }


    private fun initWebRTCListener() {
        iWebRTCListener = null
        iWebRTCListener = object : IWebRTCListener {
            override fun onDisconnected(streamId: String?) {
                viewModel.toastMessage.value = "Disconnected for $streamId"
                Log.w(TAG, "onDisconnected - $streamId")
                if(!viewModel.rejoinInProgress) {
                    if (conferenceManager != null) {
                        if (conferenceManager!!.isJoined) {
                            if (conferenceManager!!.streamId.equals(streamId)) {
                                showProgressDialog()
                                binding.broadcastingTextView.text = "Disconnected"
                                if (!viewModel.endVCByUser) {
                                    showReconnectionVCDialog()
                                } else {
                                    finishActivity(true)
                                }
                            }
                        } else {
                            //to check
                            Log.d(
                                TAG,
                                "onDisconnected: getting callback before joined -> restart conference"
                            )
                            viewModel.toastMessage.value = "Not able to connect to the web socket"
                            try {
                                if (joinVCDialog != null) {
                                    if (joinVCDialog.isShowing) {
                                        joinVCDialog.dismiss()
                                    }
                                }
                            } catch (e: Exception) {

                            }
                            showReconnectionVCDialog()
//                        rejoinConferenceRestartConference()
                        }
                    }
                }
//        audioButton!!.text = "Disable Audio"
//        videoButton!!.text = "Disable Video"
            }


            /**
             * listeners for the vc from webRTC*/

            override fun onPublishFinished(streamId: String?) {
                Log.w(TAG, "onPublishFinished - $streamId")
                viewModel.toastMessage.value = "Publish finished for $streamId"
                binding.broadcastingTextView.visibility = View.GONE
                if(streamId!=null) {
                    Log.d(TAG, "onPublishFinished: ")
                    if (conferenceManager != null) {
                        if (viewModel.endVCByUser) {
                            Log.d(TAG, "onPublishFinished: end by user")
                            /*ok..user ended the vc and then it was called*/
                        } else {
                            /*user did nit end vc...abruptly or due to some case this is called*/
                            if (viewModel.rejoinInProgress) {
                                Log.d(TAG, "onPublishFinished: rejoin in progress")
                                /*called when rejoinin in progress....do not do anything*/
                            } else {
                                /*take action here*/
                                if(conferenceManager!!.streamId == streamId){
                                    //show rejoin
                                    Log.d(TAG, "onPublishFinished: ")
                                    showReconnectionVCDialog()
                                }
                            }
                        }
                    }
                }
            }

            override fun onPlayFinished(streamId: String?) {
                Log.w(TAG, "onPlayFinished - $streamId")
                viewModel.toastMessage.value = "Play finished for $streamId"
                if (streamId != null) {
                    viewModel.streams.remove(streamId)
                }
                clearSendStatusSchedule()
            }

            override fun onPublishStarted(streamId: String?) {
                viewModel.toastMessage.value = "Publish started for $streamId"
                Log.w(TAG, "onPublishStarted - $streamId")
                binding.broadcastingTextView.visibility = View.VISIBLE
                binding.broadcastingTextView.text = "Publishing"
                conferenceManager?.publishWebRTCClient?.switchVideoScaling(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                if (streamId != null) {
                    viewModel.streamId = streamId
                    viewModel.streams.add(streamId)
                    addInitialLocalParticipant(streamId)
                    /*initiallly update the maps*/
                    trackRelMap[publisherContainer!!] = streamId
                    trackObjectRelMap[publisherContainer!!] = null
                    viewModel.isInitialConferenceStarted = true
                    scheduleSendStatusTimer()
                    dismissProgressDialog()
                    viewModel.reconnectAttemptCount = 0
                    if (streamId.equals(conferenceManager!!.streamId, ignoreCase = true)) {
                    }
                    viewModel.updateStreamIdInServerAPICall(
                        displayName = viewModel.displayName?:viewModel.userType,
                        roomId = viewModel.roomID?:"",
                        userType = viewModel.userType,
                        streamId = viewModel.streamId?:"",
                        version = VCConstants.version
                    )
                    updateStreamNameTextView(publisherContainer!!,viewModel.displayName!!) //until the api call
                }
            }

            override fun onPlayStarted(streamId: String?) {
                viewModel.toastMessage.value = "Play started for $streamId"
                Log.w(TAG, "onPlayStarted - $streamId")
                if (streamId != null) {
                    viewModel.streams.add(streamId)
                }
            }

            override fun noStreamExistsToPlay(streamId: String?) {
//                viewModel.toastMessage.value = "No stream exists to play for $streamId"
                Log.w(TAG, "noStreamExistsToPlay - $streamId")
                removeOnlyRemoteViews()
            }

            override fun onError(description: String?, streamId: String?) {
                viewModel.toastMessage.value = "Error for $streamId : $description"
                Log.w(TAG, "onError - $streamId : $description")

            }

            override fun onSignalChannelClosed(
                code: WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification?,
                streamId: String?
            ) {
                viewModel.toastMessage.value = "Signal channel closed for $streamId : $code"
                Log.w(TAG, "onSignalChannelClosed - $streamId : $code")

            }

            override fun streamIdInUse(streamId: String?) {
                viewModel.toastMessage.value = "Stream id is already in use $streamId"
                viewModel.streamId = streamId
                Log.w(TAG, "streamIdInUse - $streamId")
            }

            override fun onIceConnected(streamId: String?) {
                viewModel.toastMessage.value = "Ice connected for $streamId"
                if (conferenceManager != null) {
                    if(!viewModel.rejoinInProgress){
                        if(conferenceManager!!.isJoined) {
                            if (streamId.equals(conferenceManager!!.streamId)) {
                                try {
                                    if (reconnectionVCDialog != null) {
                                        if (reconnectionVCDialog.isShowing) {
                                            reconnectionVCDialog.dismiss()
                                            dismissProgressDialog()

                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.d(TAG, "onIceConnected: exception caught!")
                                }
                            }
                            Log.w(TAG, "onIceConnected - $streamId")
                        }else{
                            showReconnectionVCDialog()
                        }
                    }else{
                        if(reconnectionVCDialog!=null) {
                            if (reconnectionVCDialog.isShowing) {
                                reconnectionVCDialog.dismiss()
                                dismissProgressDialog()
                            }
                        }
                    }
                }

            }

            override fun onIceDisconnected(streamId: String?) {
                viewModel.toastMessage.value = "Ice disconnected for $streamId"
                Log.w(TAG, "onIceDisconnected - $streamId")
            }

            override fun onTrackList(tracks: Array<out String>?) {
                viewModel.toastMessage.value = "Track list received - ${tracks?.size}"
                Log.w(TAG, "onTrackList - ${Gson().toJson(tracks)}")
                tracks?.let {
                    viewModel.roomInfoStreamsList.addAll(it)
                }
            }

            override fun onBitrateMeasurement(
                streamId: String?,
                targetBitrate: Int,
                videoBitrate: Int,
                audioBitrate: Int
            ) {
                viewModel.toastMessage.value = "Bitrate measurement received"
                Log.w(TAG, "onBitrateMeasurement - $streamId")
            }

            override fun onStreamInfoList(
                streamId: String?,
                streamInfoList: java.util.ArrayList<StreamInfo>?
            ) {
                viewModel.toastMessage.value = "Stream info list received ${streamInfoList?.size}"
                Log.w(TAG, "onStreamInfoList - $streamId : ${Gson().toJson(streamInfoList)}")
            }

            override fun onNewVideoTrack(track: VideoTrack?) {
                Log.d(TAG, "onNewVideoTrack: already in vm ${Gson().toJson(viewModel.tracks)}")
                runOnUiThread { viewModel.toastMessage.value = "New video track received" }
                Log.w(TAG, "onNewVideoTrack id-Object -${track!!.id()} -${Gson().toJson(track)}")

                //(working logic) commented out for testing as the (logic for syncing streams) uncomment if required
//                if (viewModel.roomInfoStreamsList.isNotEmpty()) {
//                    if (!track!!.id().contains(conferenceManager!!.streamId)) {
//                        if (initTRackListHasStreamForTrack(track.id())) {
//                            Log.d(
//                                TAG,
//                                "onNewVideoTrack: initTrack list contains track -> ${track.id()}"
//                            )
//                        } else {
//                            Log.d(
//                                TAG,
//                                "onNewVideoTrack: initTrack list does not contain track -> ${track.id()}"
//                            )
//                            return
//                        }
//                    }
//                }
//                if (viewModel.tracks.contains(track))
//                    Log.d(TAG, "onNewVideoTrack: viewModel.Track.containsTrack: True: and return")
//                    return
//                if (track != null) {
//                    viewModel.tracks.add(track)
//                }


                //cc
                Log.d(TAG, "onNewVideoTrack: my stream ->${conferenceManager!!.streamId}")
                Log.d(TAG, "onNewVideoTrack: incoming track id ->${track!!.id()}")
                runOnUiThread {
                    if (!track!!.id().contains(conferenceManager!!.streamId)) {
                        Log.d(TAG, "onNewVideoTrack: if")
                        var remotePeerView = RemotePeerView(applicationContext)

                        val layoutParamsContainer = RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.MATCH_PARENT
                        )
                        val containerMargin =
                            resources.getDimension(com.intuit.sdp.R.dimen._2sdp).toInt()

                        layoutParamsContainer.setMargins(
                            containerMargin,
                            0,
                            containerMargin,
                            0
                        )

                        remotePeerView.layoutParams = layoutParamsContainer
//            remotePeerView.setBackgroundResource(R.drawable.bg_peer_container)
                        remotePeerView.changeVideoActiveStatus(true)
                        remotePeerView.changeAudioActiveStatus(true)

                        count++
                        remotePeerView.id = count
                        remotePeerView.streamName.append(count.toString())
                        remotePeerView.surfaceViewRenderer.setOnClickListener {
                            runOnUiThread {
//                    addIncomingRendererToScreen(renderer)
//                    swapTheViewInFullScreen(renderer)
                                Log.d(TAG, "onNewVideoTrack: new renderer click")
//                    swapRenderer(renderer) //cc
                                swapContainer(remotePeerView)
                            }
                        }
//            binding.sContainer.addView(renderer)
//                    swapTheViewInFullScreen(renderer)
                        conferenceManager!!.addTrackToRenderer(
                            track,
                            remotePeerView.surfaceViewRenderer
                        )
//            trackRendererMap.put(renderer,track.id())
                        trackRelMap[remotePeerView] = track.id()
                        trackObjectRelMap[remotePeerView] = track
//            addNewRenderer(renderer) //cc
                        addNewContainer(remotePeerView)
                        updateStreamNameTextView(remotePeerView, track.id()!!)
                        addNewParticipant(track, false)
                        viewModel.getDisplayNameForStreamId(viewModel.roomID!!,track.id().replace("ARDAMSv",""),VCConstants.version)
                    } else {
                        Log.d(TAG, "onNewVideoTrack: else ")
//            trackRendererMap.put(binding.publishViewRenderer,track.id())
                        trackRelMap[publisherContainer!!] = track.id()
                        trackObjectRelMap[publisherContainer!!] = track
                        addNewParticipant(track, true)
                        viewModel.getDisplayNameForStreamId(viewModel.roomID!!,viewModel.streamId!!,VCConstants.version) /*this will call again for updated name--> else can be commented for the local participant*/
                    }
                    displayRendererMap()
                }
            }

            override fun onVideoTrackEnded(track: VideoTrack?) {
                viewModel.tracks.remove(track)
                viewModel.toastMessage.value = "Video track ended - ${track!!.id()}"
                Log.w(TAG, "onVideoTrackEnded - ${Gson().toJson(track)}")
//        runOnUiThread {
//            var surfaceViewFound : SurfaceViewRenderer? = null
//            for (entrou in trackRendererMap) {
//                if (track.id().equals(entry.value)) {
//                    //remove this renderer
//                    surfaceViewFound = entry.key
//                    binding.llPlayViewsContainer.removeView(entry.key)
//                    removeEndedRenderer(entry.key)
//                }
//            }
//            if(surfaceViewFound!=null) {
//                trackRendererMap.remove(surfaceViewFound)
//            }
//            displayRendererMap()
//        }
                runOnUiThread {
                    var remotePeerViewFound: RelativeLayout? = null
                    for (entry in trackRelMap) {
                        if (track.id().equals(entry.value)) {
                            //remove this renderer
                            remotePeerViewFound = entry.key
                            removeEndedContainer(entry.key)
                            removeParticipant(track)
                            break
                        }
                    }
                    if (remotePeerViewFound != null) {
                        trackRelMap.remove(remotePeerViewFound)
                        trackObjectRelMap.remove(remotePeerViewFound)
                    }
                    displayRendererMap()
                }
            }

            override fun onReconnectionAttempt(streamId: String?) {
                viewModel.toastMessage.value = "Reconnection attempt for $streamId"
                Log.w(TAG, "onReconnectionAttempt - $streamId")
//                viewModel.reconnectAttemptCount++
//                if (viewModel.reconnectAttemptCount > 5) {
//                    Log.d(
//                        TAG,
//                        "onReconnectionAttempt: login attempts  ${viewModel.reconnectAttemptCount} > 5..now stop and show the rejoin dialog"
//                    )
//                    showReconnectionVCDialog()
////                    conferenceManager!!.publishWebRTCClient.isReconnectionEnabled = false
////                    conferenceManager!!.playWebRTCClient.isReconnectionEnabled = false
//                } else {
//                    Log.d(
//                        TAG,
//                        "onReconnectionAttempt: login attemts are ${viewModel.reconnectAttemptCount}"
//                    )
//                }
            }

            override fun onJoinedTheRoom(streamId: String?, streams: Array<out String>?) {
                Log.d(TAG, "onJoinedTheRoom: ")
                viewModel.isInitialConferenceStarted = true
            }

            override fun onRoomInformation(streams: Array<out String>?) {
                Log.w(TAG, "onRoomInformation:  streams -> ${Gson().toJson(streams!!)}")
                //Commented synncing logic for removing unwanted (streams with black screen)
//                Thread(Runnable {
//                    if (viewModel.roomInfoStreamsList.isEmpty()) {
//                        viewModel.roomInfoStreamsList.addAll(streams)
//                        udpateStreamsFromRoomInformation(streams, true)
//                    } else {
//                        udpateStreamsFromRoomInformation(streams, false)
//                    }
//                }).start()

            }

            override fun onLeftTheRoom(roomId: String?) {
                Log.d(TAG, "onLeftTheRoom: ")
            }

            override fun onMutedFor(streamId: String?) {
            }

            override fun onUnmutedFor(streamId: String?) {
            }

            override fun onCameraTurnOnFor(streamId: String?) {
            }

            override fun onCameraTurnOffFor(streamId: String?) {
            }

            override fun onSatatusUpdateFor(
                streamId: String?,
                micStatus: Boolean,
                cameraStatus: Boolean
            ) {
            }

            override fun checkAndRequestPermisssions(
                isForPublish: Boolean,
                permissionCallback: PermissionCallback?
            ): Boolean {
                Log.d(
                    TAG,
                    "checkAndRequestPermisssions: AndroidUtils -> ${
                        AndroidUtils.hasPermissions(
                            applicationContext,
                            PERMISSIONS
                        )
                    }"
                )
                return true
            }

//    override fun onCameraError(errorDescription: String?) {
//        Log.d(TAG, "onCameraError: on camera error received -> CAM_TEST")
//    }
        }
    }

    private fun initDataChannelListener() {
        dataChannelObserver = null
        dataChannelObserver = object : IDataChannelObserver {
            /**data channel callbacks**/
            override fun onBufferedAmountChange(previousAmount: Long, dataChannelLabel: String?) {
//       viewModel.toastMessage.value = "Data channel buffered amount changed: $dataChannelLabel: $previousAmount"
                Log.w(TAG, "onBufferedAmountChange - $dataChannelLabel")

            }

            override fun onStateChange(state: DataChannel.State?, dataChannelLabel: String?) {
                viewModel.toastMessage.value =
                    "Data channel state changed: $dataChannelLabel: $state"
                Log.w(TAG, "onStateChange - $dataChannelLabel")

            }

            override fun onMessage(buffer: DataChannel.Buffer?, dataChannelLabel: String?) {
                val data = buffer?.data
                val messageText = data?.array()?.let { String(it, StandardCharsets.UTF_8) }
//        viewModel.toastMessage.value = messageText
                Log.w(TAG, "onMessage - $messageText")
                processIncomingDataChannelMessage(JSONObject(messageText))
            }

            override fun onMessageSent(buffer: DataChannel.Buffer?, successful: Boolean) {
                if (successful) {
                    val data = buffer?.data
                    val bytes = ByteArray(data!!.capacity())
                    data[bytes]
                    val messageText = String(bytes, StandardCharsets.UTF_8)
//            viewModel.toastMessage.value = "Message is sent ${messageText}"
                    Log.w(TAG, "onMessageSent - success $messageText")

                } else {
//            viewModel.toastMessage.value = "Could not send the text message"
                    Log.w(TAG, "onMessageSent - failure ")

                }
                val data = buffer?.data
                val messageText = data?.array()?.let { String(it, StandardCharsets.UTF_8) }
                processMessageSentFromChat(JSONObject(messageText),successful)
            }

        }
    }

    private fun initPublishContainerClickListener() {
        publisherContainer = null
        publisherContainer = RemotePeerView(this)

        val layoutParamsContainer = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )

        publisherContainer!!.layoutParams = layoutParamsContainer
//            remotePeerView.setBackgroundResource(R.drawable.bg_peer_container)
        publisherContainer!!.changeVideoActiveStatus(true)
        publisherContainer!!.changeAudioActiveStatus(true)
        binding.fContainer.addView(publisherContainer)
        publisherContainer!!.streamName.text = viewModel.displayName?:"You"
        publisherContainer!!.surfaceViewRenderer.setOnClickListener {
            Log.d(TAG, "publisherContainer setUpOnClickListeners: ")
            if (conferenceManager != null) {
                if (conferenceManager!!.isJoined) {
                    if (publisherContainer!!.parent == binding.sContainer) {
                        Log.d(
                            TAG,
                            "publisherContainer setUpOnClickListenerGONEs: SWAP publishViewContainer in S"
                        )
                        swapContainer(publisherContainer!!)
                    } else {
                        Log.d(
                            TAG,
                            "publisherContainer setUpOnClickListeners: SWAP publishViewContainer in F"
                        )
                    }
                }
            }
        }

    }

    private fun removeOnlyRemoteViews() {
        Log.d(TAG, "removeOnlyRemoteViews: ")
        runOnUiThread {
            var tempRemoteViews = ArrayList<RemotePeerView>()
            for (entry in trackRelMap) {
                if (entry.key != publisherContainer) {
                    tempRemoteViews.add(entry.key)
                }
            }
            for (i in tempRemoteViews) {
                viewModel.tracks.remove(trackObjectRelMap[i])
                trackObjectRelMap[i]?.let { removeParticipant(it) }
                removeEndedContainer(i)
                trackObjectRelMap.remove(i)

            }
        }
    }

    private fun udpateStreamsFromRoomInformation(
        newStreams: Array<out String>?,
        isFirstTime: Boolean
    ) {
        Log.d(TAG, "udpateStreamsFromRoomInformation: newStreams size " + newStreams!!.size)
        runOnUiThread {
            if (newStreams != null) {
                if (isFirstTime) {
                    /*first time ...process it*/
                    processStreamListFromRoomInfo(newStreams)
                } else {
                    /*process*/
//                        Log.d(TAG, "udpateStreamsFromRoomInformation: isNewStreamsSameAsVMStreamList -> ${isNewStreamsSameAsVMStreamList(newStreams)}")
//                        if(isNewStreamsSameAsVMStreamList(newStreams)){
//                            /*stream list is the same ..do not process this*/
//                        }else{
//                            /*stream list is not the same...process this*/
//                        }
                    processStreamListFromRoomInfo(newStreams)
                }
            }


        }

    }

    private fun processStreamListFromRoomInfo(newStreams: Array<out String>) {
        val extraStreams = ArrayList<RemotePeerView>()
        for (entry in trackRelMap) {
            entry.value?.let {
                Log.d(TAG, "processStreamListFromRoomInfo: it  $it")

                if (it.contains(conferenceManager!!.streamId, true)) {
                    /*it is the publish stream...do not remove this*/
                    Log.d(
                        TAG,
                        "processStreamListFromRoomInfo: it is publish stream"
                    )

                } else {
                    if (!isTrackStringPresentInNewStreams(it, newStreams)) {
                        Log.d(TAG, "processStreamListFromRoomInfo: it false")
                        extraStreams.add(entry.key)
                    } else {
                        Log.d(TAG, "processStreamListFromRoomInfo: it true")

                    }
                }
            }
        }
        for (i in extraStreams) {
            viewModel.tracks.remove(trackObjectRelMap[i])
            trackObjectRelMap[i]?.let { removeParticipant(it) }
            removeEndedContainer(i)
            trackObjectRelMap.remove(i)
            trackRelMap.remove(i)

        }
        viewModel.roomInfoStreamsList.clear()
        viewModel.roomInfoStreamsList.addAll(newStreams)
    }

    infix fun <T> List<T>.equalsIgnoreOrder(other: List<T>) =
        this.size == other.size && this.toSet() == other.toSet()


    private fun isNewStreamsSameAsVMStreamList(newStreams: Array<out String>): Boolean {
        val newStreamList = ArrayList<String>()
        for (i in newStreams.indices) {
            newStreamList.add(newStreams[i])
        }
        return (newStreamList.toList()).equalsIgnoreOrder(viewModel.roomInfoStreamsList.toList())
    }

    private fun isTrackStringPresentInNewStreams(
        s: String,
        newStreams: Array<out String>?
    ): Boolean {
        if (newStreams != null) {
            for (i in newStreams.indices) {
                Log.d(
                    TAG,
                    "isTrackStringPresentInNewStreams: newStreams[i]:${newStreams[i]} ---- s:$s"
                )
                if (s.contains(newStreams[i], true)) {
                    return true
                }
            }
        }
        return false
    }

    private fun initTRackListHasStreamForTrack(newTrackId: String): Boolean {
        for (i in viewModel.roomInfoStreamsList) {
            if (newTrackId.contains(i, true)) {
                return true
            }
        }
        return false
    }

    private fun openMessageFragment() {
        Log.d(TAG, "openMessageFragment: ")



    }

    private fun closeMessageFragment() {
        fragmentManager.beginTransaction()
            .hide(messageFragment)
            .commit()
        binding.fragmentHolderForMessage.visibility = View.GONE
        viewModel.messageFragVisible = false
        viewModel.clearMessageBadgeValue()
        Log.d(TAG, "closeMessageFragmet: ")
//        val f = fragmentManager.findFragmentByTag("MESSAGE_FRAG")
//        if (f != null) {
//            Log.d(TAG, "closeMessageFragmet: remove_message_screen")
//            fragmentTransaction.remove(f)
//        }
//        fragmentTransaction.commit()


    }

    private fun openFileExplorer() {
        var chooseFileIntent = Intent(Intent.ACTION_GET_CONTENT)
        chooseFileIntent.type = "*/*"
        chooseFileIntent = Intent.createChooser(chooseFileIntent, "Choose a file")
        startActivityForResult(chooseFileIntent, PICKFILE_RESULT_CODE)
    }

    private fun sendTextMessage(messageModel: MessageModel) {
        /*sending json object as a string
        * Event name is TEXT_MESSAGE
        * TEXT_MESSAGE_VALUE is the text message value*/
        val jsonObject = JSONObject()
        try {
            jsonObject.put(VCConstants.STREAM_ID, conferenceManager!!.streamId)
            jsonObject.put(VCConstants.EVENT_TYPE, TEXT_MESSAGE)
            jsonObject.put(VCConstants.TEXT_MESSAGE_VALUE, messageModel.messageText)
            jsonObject.put(VCConstants.DISPLAY_NAME, viewModel.displayName)
            Log.d(TAG, "sendTextMessage: timeTest: ${AndroidUtils.getCurrentTimeInMill()}")
            jsonObject.put(VCConstants.currentTime, AndroidUtils.getCurrentTimeInMill())
            jsonObject.put(VCConstants.MESSAGEID, messageModel.id) /*extra added to have a process status update locally in OnMessageSent()*/

            val messageEvent = jsonObject.toString()
            val buffer = ByteBuffer.wrap(messageEvent.toByteArray(StandardCharsets.UTF_8))
            val buf = DataChannel.Buffer(buffer, false)
            conferenceManager!!.sendMessageViaDataChannel(buf)
        } catch (e: JSONException) {
            Log.d(TAG, "sendTextMessage: exception caught!!")
        }
    }


    private fun sendFileMessage(messageModel: MessageModel) {
        /*sending json object as a string
        * Event name is TEXT_MESSAGE
        * TEXT_MESSAGE_VALUE is the text message value*/
        val jsonObject = JSONObject()
        try {
            jsonObject.put(VCConstants.STREAM_ID, conferenceManager!!.streamId)
            jsonObject.put(VCConstants.EVENT_TYPE, FILE_MESSAGE)
            jsonObject.put(VCConstants.FILE_NAME, messageModel.fileName)
            jsonObject.put(VCConstants.SERVER_FILE_PATH, messageModel.serverFilePath)
            jsonObject.put(VCConstants.TEXT_MESSAGE_VALUE, messageModel.messageText)
//            jsonObject.put(Constants.DISPLAY_NAME, viewModel.localDisplayName)
            jsonObject.put(VCConstants.DISPLAY_NAME, viewModel.displayName)
            Log.d(TAG, "sendTextMessage: timeTest: ${AndroidUtils.getCurrentTimeInMill()}")
            jsonObject.put(VCConstants.currentTime, AndroidUtils.getCurrentTimeInMill())
            jsonObject.put(VCConstants.MESSAGEID, messageModel.id) /*extra added to have a process status update locally in OnMessageSent()*/

            val messageEvent = jsonObject.toString()
            val buffer = ByteBuffer.wrap(messageEvent.toByteArray(StandardCharsets.UTF_8))
            val buf = DataChannel.Buffer(buffer, false)
            conferenceManager!!.sendMessageViaDataChannel(buf)
        } catch (e: JSONException) {
            Log.d(TAG, "sendFileMessage: exception caught!! -> while sending data")
        }
    }

    private fun sendEstimationMessage(messageModel: MessageModel) {
        /*sending json object as a string
        * Event name is TEXT_MESSAGE
        * TEXT_MESSAGE_VALUE is the text message value*/
        val jsonObject = JSONObject()
        try {
            jsonObject.put(VCConstants.STREAM_ID, conferenceManager!!.streamId)
            jsonObject.put(VCConstants.EVENT_TYPE, VCConstants.ESTIMATION_MESSAGE)
            jsonObject.put(VCConstants.ESTIMATION_MESSAGE_VALUE,Gson().toJson(messageModel.estimationDetails))
            jsonObject.put(VCConstants.DISPLAY_NAME, viewModel.displayName)
            Log.d(TAG, "sendTextMessage: timeTest: ${AndroidUtils.getCurrentTimeInMill()}")
            jsonObject.put(VCConstants.currentTime, AndroidUtils.getCurrentTimeInMill())
            jsonObject.put(VCConstants.MESSAGEID, messageModel.id) /*extra added to have a process status update locally in OnMessageSent()*/

            val messageEvent = jsonObject.toString()
            val buffer = ByteBuffer.wrap(messageEvent.toByteArray(StandardCharsets.UTF_8))
            val buf = DataChannel.Buffer(buffer, false)
            conferenceManager!!.sendMessageViaDataChannel(buf)
        } catch (e: JSONException) {
            Log.d(TAG, "sendFileMessage: exception caught!! -> while sending data")
        }
    }

    private fun processTextMessageFromDataChannel(
        json: JSONObject,
        streamId: String?,
        eventType: String
    ) {
        if(!viewModel.messageFragVisible) {
            viewModel.incrementMessageBadgeValue()
        }
        val textMessage = json.getString(VCConstants.TEXT_MESSAGE_VALUE)?:""
//                var displayName = ""
        val displayName = json.getString(VCConstants.DISPLAY_NAME)?:""
        viewModel.toastMessage.value = "${displayName} messaged You"
        val remoteMessageId = AndroidUtils.getCurrentTimeInMill()
        Log.d(TAG, "onMessage: displayName: $displayName")
        val tempRemoteMessage = MessageModel(
            displayName,
            textMessage,
            false,
            TEXT_MESSAGE,
            remoteMessageId,
            "",
            "",
            "",
            estimationDetails = null
        )
        viewModel.messageListInMVM.add(tempRemoteMessage)
        viewModel.addNewRemoteMessage.value = remoteMessageId

    }

    private fun processFileMessageFromDataChannel(
        json: JSONObject,
        streamId: String?,
        eventType: String
    ) {

        val textMessage = json.getString(VCConstants.TEXT_MESSAGE_VALUE)
//                var displayName = ""
        val displayName = json.getString(VCConstants.DISPLAY_NAME)?:""
        viewModel.toastMessage.value = "${displayName} messaged You"
        val fileName = json.getString(VCConstants.FILE_NAME)?:""
        val serverFilePath = json.getString(VCConstants.SERVER_FILE_PATH)?:""
        Log.d(TAG, "onMessage: displayName: $displayName")
        val remoteMessageId = AndroidUtils.getCurrentTimeInMill()
        val tempRemoteMessage = MessageModel(
            displayName,
            textMessage,
            false,
            FILE_MESSAGE,
            remoteMessageId,
            fileName,
            serverFilePath,
            "",
            estimationDetails = null
        )
        viewModel.messageListInMVM.add(tempRemoteMessage)
        viewModel.addNewRemoteMessage.value = remoteMessageId
        if(!viewModel.messageFragVisible) {
            viewModel.incrementMessageBadgeValue()
        }
    }

    private fun processEstimationMessageFromDataChannel(
        json: JSONObject,
        streamId: String?,
        eventType: String
    ) {
        Log.d(TAG, "processEstimationMessageFromDataChannel: testRemoteEstimate: ")
        try {
            Log.d(TAG, "processEstimationMessageFromDataChannel:testRemoteEstimate: json:  ${Gson().toJson(json)}")
            val estimateDetailsString = json.getString(VCConstants.ESTIMATION_MESSAGE_VALUE)
            val estimateDetails =
                Gson().fromJson(estimateDetailsString, ResponseModelEstimateData::class.java)

// Use Gson to parse the JSON string into ResponseModelEstimateData


            val displayName = json.getString(VCConstants.DISPLAY_NAME)?:""
            viewModel.toastMessage.value = "${displayName} messaged You"
            val remoteMessageId = AndroidUtils.getCurrentTimeInMill()
            val tempRemoteMessage = MessageModel(
                displayName,
                "",
                false,
                ESTIMATION_MESSAGE,
                remoteMessageId,
                "",
                "",
                "",
                estimationDetails = estimateDetails
            )
            viewModel.messageListInMVM.add(tempRemoteMessage)
            viewModel.addNewRemoteMessage.value = remoteMessageId
            if(!viewModel.messageFragVisible) {
                viewModel.incrementMessageBadgeValue()
            }
        }catch (e:Exception) {
            Log.d(TAG, "processEstimationMessageFromDataChannel: testRemoteEstimate: ${e.message}")
        }
        
    }


    private fun checkForScreenShareProcess() {
        if (viewModel.updateScreenShareForFragment.value == true) {
            /*screen share is enabled
            * stop screen share*/
            stopScreenShare()
        } else {
            /*screen share is not in progress
            * do nothing*/
        }
    }

    /*file attah message*/
    private fun processPickFileData(intent: Intent?) {
        if(intent ==null)
            return
        if(intent.data==null)
            return
        intent.let { i ->
            val fileUri = i.data
            var filePath: String? = null
            try {
                filePath = fileUri?.let { FileOperations.getPath(this, it) }
                Log.d(TAG, "processPickFileData: filePath : ${filePath}")
            } catch (e: Exception) {
                Log.e(TAG, "processPickFileData: exception caught while parsing file path: ${e.message}")
//                Toast.makeText(this,"Oops, error while parsing file. Please check the file and try again", Toast.LENGTH_SHORT).show()
//                return@let
            }
            if (filePath.isNullOrBlank()) {
                try {
                    filePath = fileUri?.let {
                        FileOperations.getImagePathFromInputStreamUri(this,
                            it
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "processPickFileData:  ${e.message}")
                    Toast.makeText(this,"Oops, error while parsing file. Please check the file and try again", Toast.LENGTH_SHORT).show()
                }
            }
            if (filePath != null) {

                val file = File(filePath)
    //                        val specialCharactersRegex = Regex("[^a-zA-Z0-9._ ]")
    //                        if (specialCharactersRegex.containsMatchIn(file.name)) {
    //                            viewModel.toastString.value = "Invalid File name. File name cannot contain special characters. Please rename."
    //                            return
    //                        }


                var fileSizeInBytes = file.length()
                var fileSizeInMB = fileSizeInBytes / (1024 * 1024)
                if (fileSizeInMB < 25) {
                    var msgID = AndroidUtils.getCurrentTimeInMill()
                    /*call API*/
                    viewModel.processNewLocalFileMessage(file.name,"",msgID,true,filePath)
                    viewModel.uploadVcFileAPICallNew(file,viewModel.roomID?:"","CUSTOMER","CUSTOMER",msgID,false)     /*07 Nov 2023:: IMP::nahusha help required here::to pass correct data for vc room, who and usertype*/
                }else {
                    viewModel.toastMessage.value = "File cannot be more than 25MB"
                }
            }else {
                Toast.makeText(this,"Oops, error while parsing file. Please check the file and try again", Toast.LENGTH_SHORT).show()
                return@let
            }
        }

    }

    private fun processVCActivityBackPress() {
        /*1.bottom menu dialog
        * 2.screen share frag
        * 3.sound device frag
        * 4.participants frag
        * 5.message frag
        * 6.end vc call*/
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            showMoreOptions(makeMoreOptionsVisible = false)
            return
        }
        Log.d(TAG, "processVCActivityBackPress: bottom sheet -> " + bottomSheetBehavior.state)
        Log.d(
            TAG,
            "processVCActivityBackPress: screen share -> " + viewModel.screenShareFragVisible
        )
        Log.d(
            TAG,
            "processVCActivityBackPress: sound device -> " + viewModel.soundDeviceFragVisible
        )
        Log.d(
            TAG,
            "processVCActivityBackPress: participants -> " + viewModel.participantFragVisible
        )
        Log.d(TAG, "processVCActivityBackPress: message  -> " + viewModel.messageFragVisible)
        if (viewModel.screenShareFragVisible) {
            dismissScreenShareFragment()
            return
        }
        if (viewModel.soundDeviceFragVisible) {
            dismissSoundDeviceFragment()
            return
        }
        if (viewModel.participantFragVisible) {
            dismissParticipantsFragment()
            return
        }
        if (viewModel.messageFragVisible) {
            dismissMessageFragment()
            return
        }
        /*if all clear--no showing fragments--leave conference*/
        if (conferenceManager != null) {
            if (conferenceManager!!.isJoined) {
                leaveConference()
            }
        }
    }

    private fun dismissScreenShareFragment() {
        removeFragment(SCREEN_SHARE_FRAG)
    }

    private fun dismissMessageFragment() {
        closeMessageFragment()
    }

    private fun dismissParticipantsFragment() {
        removeFragment(PARTICIPANT_FRAG)
    }

    private fun dismissSoundDeviceFragment() {
        removeFragment(SOUND_DEVICE_FRAG)
    }

    private fun removeFragment(tag: String) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val list = supportFragmentManager.fragments
        list.forEach {
            if (it.tag.equals(tag)) {
                fragmentTransaction.remove(it)
                fragmentTransaction.commit()
            }
        }
    }
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val isPipSupported = this.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        if (isPipSupported ) {
            Log.d(TAG, "onUserLeaveHint: pip : isSupported: true")
            this.enterPictureInPictureMode()
        }else {
            Log.d(TAG, "onUserLeaveHint: pip: isSupported: false")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        Log.d(TAG, "onPictureInPictureModeChanged: ${lifecycle.currentState}")

        if (isInPictureInPictureMode) {
            // Inflate the PiP layout
            Log.d(TAG, "onPictureInPictureModeChanged: ")
            binding.groupPipHide?.visibility = View.GONE

            if(viewModel.messageFragVisible) {
                binding.fragmentHolderForMessage.visibility = View.GONE
            }

            // TODO: Handle PiP mode layout and functionality
        }else if(lifecycle.currentState == Lifecycle.State.STARTED) {
            if (isInPictureInPictureMode) {

            } else {
//                if(vCScreenViewModel.isScreenShareEnabled.value!=null) {
//                    if(vCScreenViewModel.isScreenShareEnabled.value == false) {
//                        vcScreenBinding.svRenderLayout.visibility = View.VISIBLE
//                    }
//                }else {
//                    vcScreenBinding.svRenderLayout.visibility = View.VISIBLE
//                }
                binding.groupPipHide?.visibility = View.VISIBLE
                if(viewModel.messageFragVisible) {
                    binding.fragmentHolderForMessage.visibility = View.VISIBLE
                }
                Log.d(TAG, "onPictureInPictureModeChanged:  maxClicked: ")
            }
        } else if(lifecycle.currentState == Lifecycle.State.CREATED) {
            Log.d(TAG, "onPictureInPictureModeChanged: close: Clicked: ")
//            stopCurrentVC()

//            vcScreenBinding.topLayoutVcScreen.visibility = View.VISIBLE
//            vcScreenBinding.mainOptionsLayout.visibility = View.VISIBLE
//            vcScreenBinding.svRenderLayout.visibility = View.VISIBLE

            viewModel.endVCByUser = true
            conferenceManager!!.leaveFromConference()
            stoppedStream = true
            // TODO: Handle exiting PiP mode and restore the main activity layout
        }



        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }
    private fun processMessageSentFromChat(jsonObject: JSONObject,successful:Boolean) {
        try {
            val eventType =
                jsonObject.getString(VCConstants.EVENT_TYPE)
           if(eventType.equals(TEXT_MESSAGE,false)||eventType.equals(FILE_MESSAGE,false)||eventType.equals(VCConstants.ESTIMATION_MESSAGE,false) ){
                val messageID = jsonObject.getLong(VCConstants.MESSAGEID)
                viewModel.updateSendStatusForMessage(messageID,successful)
            }
        }catch(e:Exception)
        {
            Log.d(TAG, "processMessageSentFromChat: exeception caught!!")
        }
    }

    private fun setUpVcDetails() {
        // customer or service advisor
        if (intent != null) {
//            serverURL = intent.getStringExtra(VcConnector.SERVER_URL)
//            serverURL = "ws://onvideo.apprikart.com:5080/WebRTCAppEE/websocket"
//             viewModel.roomID = intent.getStringExtra("room")
//             viewModel.serviceAdvisorID = intent.getStringExtra("service_person_id")
//            userType = intent.getStringExtra("user_type")
//            passcode = intent.getStringExtra("auth_passcode")
//             viewModel.customerCode = intent.getStringExtra("customerCode")
//             viewModel.dealerCode = intent.getStringExtra("dealerCode")
//             viewModel.roNo = intent.getStringExtra("roNo")
//             viewModel.displayName = intent.getStringExtra("displayName")
//             viewModel.userName = intent.getStringExtra("userName")




             viewModel.testUserType = intent.getStringExtra("testUserType")

            if ( viewModel.testUserType != null) {

                 viewModel.userId = intent.getStringExtra("userId")
                 viewModel.password = intent.getStringExtra("password")
                 viewModel.deviceToken = intent.getStringExtra("deviceToken")
                Log.d(TAG, "setUpVcDetails: ${ viewModel.userId}")
                Log.d(TAG, "setUpVcDetails: ${ viewModel.password}")
                Log.d(TAG, "setUpVcDetails: ${ viewModel.deviceToken}")
                if((! viewModel.userId.isNullOrEmpty()) &&
                    (! viewModel.password.isNullOrEmpty()) &&
                    (! viewModel.deviceToken.isNullOrEmpty())
                ) {
                     viewModel.doLogin(
                        RequestModelLogin(
                            userName =  viewModel.userId!!,
                            password =  viewModel.password!!,
                            deviceToken =  viewModel.deviceToken!!,
                        )
                    )
                }else {
                     viewModel.toastMessage.value = "Login Credentials. Null or Empty."
                }
                //Updating VC End-Time
                 viewModel.vcEndTime = intent.getStringExtra("vcEndTime")

                when ( viewModel.testUserType) {
//                    Constants.Companion.UserType.SERVICE_PERSON.value -> {
//                         viewModel.roomID = "CDF1RC0EEQ"
//                         viewModel.serviceAdvisorID = "EUP3070025"
//                        userType = Constants.Companion.UserType.SERVICE_PERSON.value
//                        passcode = "24992"
//                         viewModel.customerCode = "2"
//                         viewModel.dealerCode = "UP307"
//                         viewModel.roNo = ""
//                         viewModel.displayName = "Android Service Advisor 2"
//                    }
//
//                    else -> {
//                         viewModel.roomID = "CDF1RC0EEQ"
//                         viewModel.serviceAdvisorID = "EUP3070025"
//                        userType = Constants.Companion.UserType.CUSTOMER.value
//                        passcode = "24992"
//                         viewModel.customerCode = "2"
//                         viewModel.dealerCode = "UP307"
//                         viewModel.roNo = ""
//                         viewModel.displayName = "Android Customer"
//                         viewModel.userName = "9136388890"
//                    }


//                    Constants.Companion.UserType.SERVICE_PERSON.value -> {
//                         viewModel.roomID = "DWYCQUNC0T"
//                         viewModel.serviceAdvisorID = "EUP3070025"
//                        userType = Constants.Companion.UserType.SERVICE_PERSON.value
//                        passcode = "34232"
//                         viewModel.customerCode = "C2019070005"
//                         viewModel.dealerCode = "UP307"
//                         viewModel.roNo = "R202300212"
//                         viewModel.displayName = "Android Service Advisor 2"
//                    }
//
//                    else -> {
//                         viewModel.roomID = "DWYCQUNC0T"
//                         viewModel.serviceAdvisorID = "EUP3070025"
//                        userType = Constants.Companion.UserType.CUSTOMER.value
//                        passcode = "34232"
//                         viewModel.customerCode = "C2019070005"
//                         viewModel.dealerCode = "UP307"
//                         viewModel.roNo = "R202300212"
//                         viewModel.displayName = "Android Customer"
//                         viewModel.userName = "9136388890"
//                    }


//                    VCConstants.UserType.SERVICE_PERSON.value-> {
//                        PreferenceManager.setBaseUrl("https://kialinkd-qa.kiaindia.net/dev/")
//                         viewModel.roomID = "QF2O5ZIVYN"
//                         viewModel.serviceAdvisorID = "EUP3070025"
//                        viewModel.userType = VCConstants.UserType.SERVICE_PERSON.value
//                        viewModel.meetingPasscode = "17498"
//                         viewModel.customerCode = "C2019070005"
//                         viewModel.dealerCode = "UP307"
//                         viewModel.roNo = "R202300212"
//                         viewModel.displayName = "Android Service Advisor 2"
//                    }
//
//                    else -> {
//                        PreferenceManager.setBaseUrl("http://10.107.11.242:7001/kiakandit/")
//                         viewModel.roomID = "QF2O5ZIVYN"
//                         viewModel.serviceAdvisorID = "EUP3070025"
//                        viewModel.userType  = VCConstants.UserType.CUSTOMER.value
//                        viewModel.meetingPasscode= "17498"
//                         viewModel.customerCode = "C2019070005"
//                         viewModel.dealerCode = "UP307"
//                         viewModel.roNo = "R202300212"
//                         viewModel.displayName = "Android Customer"
//                         viewModel.userName = "9136388890"
//                    }


//                    VCConstants.UserType.SERVICE_PERSON.value -> {
//                        saveEstimateToken(this,"SA-3dyLZHCR8hJzqGi1oWm3npiIBU1WI4JBRnOuQPZC_EUP3070302")
//                        PreferenceManager.setBaseUrl("https://kialinkd.kiaindia.net/api/")
//                         viewModel.roomID = "0LIRTVNQNR"
//                         viewModel.serviceAdvisorID = "EUP3070302"
//                         viewModel.userType = VCConstants.UserType.SERVICE_PERSON.value
//                         viewModel.meetingPasscode= "67578"
//                         viewModel.customerCode = "C2023100556"
//                         viewModel.dealerCode = "UP307"
//                         viewModel.roNo = "R202314775"
//                         viewModel.displayName = "Android Service Advisor 2"
//                    }
//
//                    else -> {
//                        saveEstimateToken(this,"SA-3dyLZHCR8hJzqGi1oWm3npiIBU1WI4JBRnOuQPZC_EUP3070302")
//                        PreferenceManager.setBaseUrl("https://mykia.kiaindia.net/apimykia/")
//                         viewModel.roomID = "0LIRTVNQNR"
//                         viewModel.serviceAdvisorID = "EUP3070302"
//                        viewModel.userType = VCConstants.UserType.CUSTOMER.value
//                        viewModel.meetingPasscode = "67578"
//                         viewModel.customerCode = "C2023100556"
//                         viewModel.dealerCode = "UP307"
//                         viewModel.roNo = "R202314775"
//                         viewModel.displayName = "Android Customer"
//                         viewModel.userName = "9136388890"
//                    }

//                    VCConstants.UserType.SERVICE_PERSON.value -> {
//                        PreferenceManager.setBaseUrl("https://kialinkd-qa.kiaindia.net/dev/")
//                         viewModel.roomID = "QF2O5ZIVYN"
//                         viewModel.serviceAdvisorID = "EUP3070025"
//                        viewModel.userType = VCConstants.UserType.SERVICE_PERSON.value
//                        viewModel.meetingPasscode = "17498"
//                         viewModel.customerCode = "C2019070005"
//                         viewModel.dealerCode = "UP307"
//                         viewModel.roNo = "R202300212"
//                         viewModel.displayName = "Android Service Advisor 2"
//                    }
//
//                    else -> {
//                        PreferenceManager.setBaseUrl("http://10.107.11.242:7001/kiakandit/")
//                         viewModel.roomID = "QF2O5ZIVYN"
//                         viewModel.serviceAdvisorID = "EUP3070025"
//                        viewModel.userType = VCConstants.UserType.CUSTOMER.value
//                        viewModel.meetingPasscode = "17498"
//                         viewModel.customerCode = "C2019070005"
//                         viewModel.dealerCode = "UP307"
//                         viewModel.roNo = "R202300212"
//                         viewModel.displayName = "Android Customer"
//                         viewModel.userName = "9136388890"
//                    }


//                    VCConstants.UserType.SERVICE_PERSON.value -> {
//                        PreferenceManager.setBaseUrl("https://kialinkd-qa.kiaindia.net/dev/")
//                        viewModel.roomID = "D8WRJKU1VT"
//                        viewModel.serviceAdvisorID = "EUP3070025"
//                        viewModel.userType = VCConstants.UserType.SERVICE_PERSON.value
//                        viewModel.meetingPasscode = "44147"
//                        viewModel.customerCode = "C2019070005"
//                        viewModel.dealerCode = "UP307"
//                        viewModel.roNo = "R202300212"
//                        viewModel.displayName = "Android Service Advisor 2"
//                    }
////
//                    else -> {
//                        PreferenceManager.setBaseUrl("http://10.107.11.242:7001/kiakandit/")
//                        viewModel.roomID = "D8WRJKU1VT"
//                        viewModel.serviceAdvisorID = "EUP3070025"
//                        viewModel.userType = VCConstants.UserType.CUSTOMER.value
//                        viewModel.meetingPasscode = "44147"
//                        viewModel.customerCode = "C2019070005"
//                        viewModel.dealerCode = "UP307"
//                        viewModel.roNo = "R202300212"
//                        viewModel.displayName = "Android Customer"
//                        viewModel.userName = "9136388890"
//                    }
                    VCConstants.UserType.SERVICE_PERSON.value -> {
                        PreferenceManager.setBaseUrl("https://kialinkd-qa.kiaindia.net/dev/")
                        viewModel.roomID = "DWYCQUNC0T"
                        viewModel.serviceAdvisorID = "EUP3070025"
                        viewModel.userType = VCConstants.UserType.SERVICE_PERSON.value
                        viewModel.meetingPasscode = "34232"
                        viewModel.customerCode = "C2019070005"
                        viewModel.dealerCode = "UP307"
                        viewModel.roNo = "R202300212"
                        viewModel.displayName = "Android Service Advisor 2"
                    }

                    else -> {
                        PreferenceManager.setBaseUrl("http://10.107.11.242:7001/kiakandit/")
                        viewModel.roomID = "DWYCQUNC0T"
                        viewModel.serviceAdvisorID = "EUP3070025"
                        viewModel.userType = VCConstants.UserType.CUSTOMER.value
                        viewModel.meetingPasscode = "34232"
                        viewModel.customerCode = "C2019070005"
                        viewModel.dealerCode = "UP307"
                        viewModel.roNo = "R202300212"
                        viewModel.displayName = "Android Customer 2"
                        viewModel.userName = "9136388890"
                    }
                }
            } else {
                 viewModel.roomID = intent.getStringExtra("room")
                 viewModel.serviceAdvisorID = intent.getStringExtra("service_person_id")
                 viewModel.userType = intent.getStringExtra("user_type").toString()
                 viewModel.meetingPasscode = intent.getStringExtra("auth_passcode")
                 viewModel.customerCode = intent.getStringExtra("customerCode")
                 viewModel.dealerCode = intent.getStringExtra("dealerCode")
                 viewModel.roNo = intent.getStringExtra("roNo")
                 viewModel.displayName = intent.getStringExtra("displayName")
                 viewModel.userName = intent.getStringExtra("userName")
                 viewModel.vcEndTime = intent.getStringExtra("vcEndTime")

//                 viewModel.userId = intent.getStringExtra("userId")
//                 viewModel.password = intent.getStringExtra("password")
//                 viewModel.deviceToken = intent.getStringExtra("deviceToken")
//
//                if( viewModel.userId!=null &&  viewModel.password!=null&&  viewModel.deviceToken!=null) {
//                     viewModel.login(
//                        RequestModelLogin(
//                            userName =  viewModel.userId!!,
//                            password =  viewModel.password!!,
//                            deviceToken =  viewModel.deviceToken!!,
//                        )
//                    )
//                }
            }


//            :::: Set up VC
//             viewModel.roomID = "83VPFAV9GF"
//             viewModel.serviceAdvisorID = "EUP3070025"
//            userType = "SERVICE_PERSON"
//            passcode = "26433"
//             viewModel.customerCode="C2023060010"
//             viewModel.dealerCode="UP307"
//             viewModel.roNo="R202300113"
//             viewModel.displayName="ASB Automobiles Private Limited"
//
//             viewModel.roomID = "83VPFAV9GF"
//             viewModel.serviceAdvisorID = "EUP3070025"
//            userType = "customer"
//            passcode = "26433"
//             viewModel.customerCode="C2023060010"
//             viewModel.dealerCode="UP307"
//             viewModel.roNo="R202300113"
//             viewModel.displayName="Suneel Kumar"
//             viewModel.userName = "9136388890"

             viewModel.kecName = viewModel.userType!!
        }
        PreferenceManager.setUserType(viewModel.userType!!)

        Log.d(TAG, "room ID :: ${ viewModel.roomID}")
        Log.d(TAG, "server URL :: ${viewModel.serverUrl}")
        Log.d(TAG, "serviceAdvisorID :: ${ viewModel.serviceAdvisorID}")
        Log.d(TAG, "userType :: ${viewModel.userType}")
        Log.d(TAG, "passcode :: ${viewModel.meetingPasscode}")
        Log.d(TAG, "customerCode :: ${ viewModel.customerCode}")
        Log.d(TAG, "dealerCode :: ${ viewModel.dealerCode}")
        Log.d(TAG, "roNo :: ${ viewModel.roNo.toString()}")

        /*saving roomID in viewModel*/
//         viewModel.roomId =  viewModel.roomID!!

        validateVcBasedOnVcDetails()

    }

    private fun saveEstimateToken(context: Activity, token:String) {
        PreferenceManager.setEstimateToken(token)
    }

    private fun validateVcBasedOnVcDetails() {
        if (AndroidUtils.isNetworkOnLine(this)) {
            if (viewModel.userType.equals(VCConstants.UserType.SERVICE_PERSON.value)) {
                viewModel.validateVcForServicePerson(
                    viewModel.roomID!!,
                    viewModel.meetingPasscode!!,
                    viewModel.userType!!,
                    viewModel.serviceAdvisorID!!
                )
            } else if (viewModel.userType.equals(VCConstants.UserType.CUSTOMER.value)) {
                viewModel.validateVcForCustomer(
                    viewModel.roomID!!,
                    viewModel.meetingPasscode!!,
                    viewModel.userType!!
                )
            }
        } else {

            viewModel.toastMessage.value = "No internet connection try after some time"
            showValidateVcAlert("No internet connection. Please try after some time","validate")
        }
    }

    private fun showValidateVcAlert(error: String,isforAPI:String) {

          var alertDialog = Dialog(this)
            val dialogBinding = AlertDialogLayoutBinding.inflate(LayoutInflater.from(this))
        alertDialog.setContentView(dialogBinding.root)
        alertDialog.setCancelable(false)
        alertDialog.setCanceledOnTouchOutside(false)
//        alertDialog.window?.setLayout(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT
//            )

        if(isScreenLargeOrXlarge) {
            alertDialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }else {
            alertDialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        dialogBinding.messageTv.text = error
        dialogBinding.posBtn.text = "Retry"
        dialogBinding.negBtn.text = "Go back"
        dialogBinding.headerTv.text = when(isforAPI){
            "validate"->{"VC Validation Error!"}
            "configure" ->{"VC Configuration Error!"}
            else -> {""}
        }
        dialogBinding.posBtn.setOnClickListener {
            Log.d(TAG, "showDialogToEnableCamera():posBtn")
            alertDialog.dismiss()
            when(isforAPI)
            {
                "validate"->{validateVcBasedOnVcDetails()}
                "configure" ->{viewModel.getVCConfiguration(viewModel.roomID?:"")}
            }

            }
        dialogBinding.negBtn.setOnClickListener {
            Log.d(TAG, "showDialogToEnableCamera(): negBtn")
            alertDialog.dismiss()
            finishActivity(false)
            }
            //dialog alignment and size code.
//            val lp = WindowManager.LayoutParams()
//            lp.copyFrom(cameraDialog.window?.attributes)
//            lp.width = WindowManager.LayoutParams.MATCH_PARENT
//            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
//            lp.gravity = Gravity.CENTER
//            cameraDialog.window?.attributes = lp
//            cameraDialog.getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
        alertDialog.show()
    }

    private fun finishActivity(isResultOK :Boolean){
        val resultIntent = Intent()
        resultIntent.putExtra("meeting_id", viewModel.roomID)
        if(isResultOK) {
            setResult(Activity.RESULT_OK, resultIntent)
        }else{
            setResult(Activity.RESULT_CANCELED, resultIntent)
        }
        finish()
    }

    private fun processVCConfigurationSuccessResult(){
       viewModel.initialConfigurationSucess= true
        setUpOnClickListeners()
        checkForMandatoryPermissions()
    }

    private fun updateParticipantsDisplayNames(){
        Log.d(TAG, "updateParticipantsDisplayNames: ")
        for( participant in viewModel.participants){
            if(participant.isLocal){
                /*track ID might not have been updated */
                Log.d(TAG, "updateParticipantsDisplayNames: for local")
                updateStreamNameTextView(publisherContainer!!,participant.displayName)
            }else {
                var resultRemoteView = getRemoteViewForTrackId(participant.trackId)
                if (resultRemoteView != null) {
                    updateStreamNameTextView(resultRemoteView, participant.displayName)
                }
            }
        }
    }


    private fun getRemoteViewForTrackId(trackId:String):RemotePeerView?{
        Log.d(TAG, "getRemoteViewForTrackId: trackId ->$trackId")
        for( entry in trackRelMap){
            if(entry.value.equals(trackId))
            {
                return entry.key
            }
        }
        return null
    }

    fun playNotificationSound(context: Context) {
        val mediaPlayer = MediaPlayer.create(context, R.raw.audio_nokia_beep_once)

        // Set audio attributes for Android 8.0 (Oreo) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            mediaPlayer.setAudioAttributes(audioAttributes)
        } else {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION)
        }

        mediaPlayer.setOnCompletionListener { mp: MediaPlayer ->
            // Release the media player resources after playback completes
            mp.release()
        }

        mediaPlayer.start()
    }

    private fun updateEstimateStatus(estimationStatus: Boolean) {
        if(estimationStatus) {
            when(viewModel.estimateDetailsAfterApproval?.estimationApprovalStatus) {
                "Y" -> {
//                    newImplementation_20Sep2023
//                    vCScreenViewModel.updateEstimationStatus(
//                        customerCode = vCScreenViewModel.customerCode!!,
//                        estimationStatus = "Y",
//                        employeeNumber = vCScreenViewModel.serviceAdvisorID.toString(),
//                        labourListCodes = vCScreenViewModel.selectedLabourList!!,
//                        partListCodes = vCScreenViewModel.selectedPartList!!,
//                        roNumber = vCScreenViewModel.roNo!!,
//                        dealerCode = vCScreenViewModel.dealerCode!!
//                    )

                    viewModel.updateEstimationStatusNew(
                        customerCode = viewModel.customerCode!!,
                        estimationStatus = "Y",
                        employeeNumber = viewModel.serviceAdvisorID.toString(),
                        labourListCodes = viewModel.selectedLabourList!!,
                        partListCodes = viewModel.selectedPartList!!,
                        roNumber = viewModel.roNo!!,
                        dealerCode = viewModel.dealerCode!!,
                        baseUrl = PreferenceManager.getBaseUrl()!!
                    )
                }
                "N"-> {

//                    vCScreenViewModel.updateEstimationStatus(
//                        customerCode = vCScreenViewModel.customerCode!!,
//                        estimationStatus = "N",
//                        employeeNumber = vCScreenViewModel.serviceAdvisorID.toString(),
//                        labourListCodes = vCScreenViewModel.selectedLabourList!!,
//                        partListCodes = vCScreenViewModel.selectedPartList!!,
//                        roNumber = vCScreenViewModel.roNo!!,
//                        dealerCode = vCScreenViewModel.dealerCode!!
//                    )
                    viewModel.updateEstimationStatusNew(
                        customerCode = viewModel.customerCode!!,
                        estimationStatus = "N",
                        employeeNumber = viewModel.serviceAdvisorID.toString(),
                        labourListCodes = viewModel.selectedLabourList!!,
                        partListCodes = viewModel.selectedPartList!!,
                        roNumber = viewModel.roNo!!,
                        dealerCode = viewModel.dealerCode!!,
                        baseUrl = PreferenceManager.getBaseUrl()!!
                    )

                }
            }
        }
//        if (estimationStatus) {
//            vCScreenViewModel.updateEstimationStatus(
//                customerCode = vCScreenViewModel.customerCode!!,
//                estimationStatus = "Y",
//                employeeNumber = vCScreenViewModel.serviceAdvisorID.toString(),
//                labourListCodes = vCScreenViewModel.selectedLabourList!!,
//                partListCodes = vCScreenViewModel.selectedPartList!!,
//                roNumber = vCScreenViewModel.roNo!!,
//                dealerCode = vCScreenViewModel.dealerCode!!
//            )
//        } else {
//            vCScreenViewModel.updateEstimationStatus(
//                customerCode = vCScreenViewModel.customerCode!!,
//                estimationStatus = "N",
//                employeeNumber = vCScreenViewModel.serviceAdvisorID.toString(),
//                labourListCodes = vCScreenViewModel.selectedLabourList!!,
//                partListCodes = vCScreenViewModel.selectedPartList!!,
//                roNumber = vCScreenViewModel.roNo!!,
//                dealerCode = vCScreenViewModel.dealerCode!!
//            )
//        }
    }

    private fun webSocketNotConnectedThread() {
        try {
            if (webSocketNotConnectedThread == null) {
                webSocketNotConnectedThread = Thread(Runnable {
                    Log.d(TAG, "observer: isInPhoneCall")
                    try {
                        Thread.sleep(1500)
                        Log.d(TAG, "observer: isInPhoneCall")
                        if (conferenceManager!!.isWebSocketNotConnected) {
                            Log.d(TAG, "observer: isInPhoneCall: isWebSocketNotConnected")
                            runOnUiThread {
                                if (this::reconnectionVCDialog.isInitialized) {
                                    reconnectionVCDialog.show()
                                } else {
                                    showReconnectionVCDialog()
                                }
                            }
                        } else {
                            if (this::noInternetDialog.isInitialized) {
                                noInternetDialog.hide()
                            }
                            webSocketNotConnectedThread()
                        }
                    } catch (e: InterruptedException) {

                    } catch (e: Exception) {

                    }
                })
            }
        } catch (e: IllegalThreadStateException) {

        } catch (e: Exception) {

        }

        try {
            if (webSocketNotConnectedThread != null && !webSocketNotConnectedThread!!.isAlive)
                webSocketNotConnectedThread!!.start()
        } catch (e: IllegalThreadStateException) {

        } catch (e: Exception) {

        }

    }


}
