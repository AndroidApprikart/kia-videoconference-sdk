package com.app.vc.virtualchatroom

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import androidx.exifinterface.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.util.Log.e
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.MainViewModel
import com.app.vc.MediaFragment
import com.app.vc.ParticipantsListFragment
import com.app.vc.R
import com.app.vc.RODetailsFragment
import com.app.vc.RepairOrderActivity
import com.app.vc.RequestVideoCallDialog
import com.app.vc.databinding.LayoutUniversalDialogBinding
import com.app.vc.databinding.VcActivityVirtualChatRoomBinding
import com.app.vc.message.ResponseModelEstimateData
import com.app.vc.models.GroupMemberResponse
import com.app.vc.models.MessageModel
import com.app.vc.models.MessageStatusEnum
import com.app.vc.network.LoginApiService
import com.app.vc.presence.PresenceStore
import com.app.vc.utils.ApiDetails
import com.app.vc.utils.ConnectivityBannerHandler
import com.app.vc.utils.ProgressRequestBody
import com.app.vc.utils.PreferenceManager
import com.app.vc.utils.VCConstants
import com.app.vc.virtualroomlist.GroupUnreadStore
import com.app.vc.virtualroomlist.UserRole
import com.app.vc.virtualroomlist.VirtualRoomUiModel
import com.app.vc.websocketconnection.NotificationWebSocketManager
import com.app.vc.websocketconnection.WebSocketManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.app.vc.views.WaveformView
import com.kia.vc.message.Labour
import com.kia.vc.message.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.webrtc.MediaStreamTrack
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class VirtualChatRoomActivity : AppCompatActivity(), WebSocketManager.WebSocketCallback, EstimationInteractionListener {

    val TAG="VirtualChatRoomActivity"
    private lateinit var binding: VcActivityVirtualChatRoomBinding
    private var audioRecord: AudioRecord? = null
    private var pcmFile: File? = null
    private val playbackHandler = Handler(Looper.getMainLooper())

    private var currentRole: UserRole = UserRole.CUSTOMER
    private var room: VirtualRoomUiModel? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private val messages: MutableList<ChatMessage> = mutableListOf()
    private var messageAdapter: VirtualChatMessageAdapter? = null
    private var lastClickTime = System.currentTimeMillis()
    private val clickTimeInterval = 2000
    private var audioTrack: AudioTrack? = null
    // Add this if you have a reference to your main ViewModel or use the Activity scope
    private val sharedViewModel: MainViewModel by viewModels()
    private var networkErrorVisible = false
    private var connectivityBannerHandler: ConnectivityBannerHandler? = null
    private var isNetworkAvailable = true
    private var pendingRecoveryBanner = false
    private var shouldShowRecoveryBannerOnReconnect = false
    private var isRecoveryBannerShowing = false
    private var socketRecoveryBannerPending = false
    private var shouldRefetchMessagesOnReconnect = false
    private var suppressSocketToastUntilMs = 0L
    private var isRetryingPendingMessages = false
    private var isLoadingOlderMessages = false
    private var hasMoreOlderMessages = true
    private var oldestLoadedMessageId: Int? = null
    private var suppressAutoScroll = false
    private val sentReadReceiptMessageIds = linkedSetOf<Int>()
    private val pendingReadReceiptMessageIds = linkedSetOf<Int>()
    private val networkBannerHandler = Handler(Looper.getMainLooper())
    private val gson = Gson()
    private val memberFirstNameByUsername = mutableMapOf<String, String>()
    private val memberUserIdToDisplayName = mutableMapOf<Int, String>()
    private val memberUserIdToRoleAbbrev = mutableMapOf<Int, String>()
    private val hideRecoveryBannerRunnable = Runnable {
        if (isNetworkAvailable) {
            hideNetworkErrorBanner(force = true)
        }
    }

    companion object {
        const val EXTRA_ROLE = "extra_role"
        const val STATUS = "room_status"

        const val EXTRA_ROOM_JSON = "extra_room_json"
    }
    private var dataList = ArrayList<MessageModel>()


    private var cameraPhotoPath: String? = null
    private var voiceNotePath: String? = null
    private var voiceNoteDurationSeconds: Int = 0
    private var mediaRecorder: MediaRecorder? = null
    private val voiceTimerHandler = Handler(Looper.getMainLooper())
    private var voiceNoteDialogTimerView: TextView? = null
    private val voiceTimerRunnable = object : Runnable {
        override fun run() {
            voiceNoteDurationSeconds++
            voiceNoteDialogTimerView?.text =
                "%02d:%02d".format(voiceNoteDurationSeconds / 60, voiceNoteDurationSeconds % 60)
            voiceTimerHandler.postDelayed(this, 1000)
        }
    }

    private val playbackTimerHandler = Handler(Looper.getMainLooper())

    private val playbackTimerRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let {

                val current = it.currentPosition / 1000

                voiceNoteDialogTimerView?.text =
                    "%02d:%02d".format(current / 60, current % 60)

                val progress =
                    (it.currentPosition.toFloat() / it.duration * 50).toInt()

                voiceNoteWaveformView?.updateProgress(progress)

                playbackTimerHandler.postDelayed(this, 200)
            }
        }
    }


    private var voiceNoteDialog: AlertDialog? = null
    private var voiceNoteWaveformView: WaveformView? = null
    private val recordedAmplitudes = mutableListOf<Int>()
    private val amplitudeHandler = Handler(Looper.getMainLooper())


    private val amplitudeRunnable = object : Runnable {
        override fun run() {
            val amp = try {
                mediaRecorder?.maxAmplitude ?: 0
            } catch (_: Exception) {
                0
            }

            val scaled = (amp / 400).coerceIn(3, 60)
            val smooth = if (recordedAmplitudes.isEmpty()) {
                scaled
            } else {
                (recordedAmplitudes.last() + scaled) / 2
            }

            recordedAmplitudes.add(smooth)
            voiceNoteWaveformView?.addAmplitude(smooth)

            if (isRecording) amplitudeHandler.postDelayed(this, 50)
        }
    }
    private var isRecording = false
    private var isTyping = false
    private val typingHandler = Handler(Looper.getMainLooper())
    private val stopTypingRunnable = Runnable { sendTypingStatus(false) }

    private val apiService: LoginApiService by lazy {
        val gson = GsonBuilder().setLenient().create()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                if (response.code == 401 && !response.request.url.encodedPath.contains("token/refresh")) {
                    response.close()
                    val newToken = refreshTokenBlocking()
                    if (!newToken.isNullOrEmpty()) {
                        val newRequest = chain.request().newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .build()
                        return@addInterceptor chain.proceed(newRequest)
                    }
                }
                response
            }
            .build()
        Retrofit.Builder()
            .baseUrl(ApiDetails.APRIK_Kia_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(LoginApiService::class.java)
    }

    private fun refreshTokenBlocking(): String? {
        val refresh = PreferenceManager.getRefreshToken() ?: return null
        if (refresh.isEmpty()) return null
        return try {
            val json = JsonObject().apply { addProperty("refresh", refresh) }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(ApiDetails.APRIK_Kia_BASE_URL + "api/token/refresh/")
                .post(body)
                .build()
            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
                val respJson = Gson().fromJson(response.body!!.string(), JsonObject::class.java)
                val access = respJson.get("access")?.asString
                if (!access.isNullOrBlank()) {
                    PreferenceManager.setAccessToken(access)
                    respJson.get("refresh")?.asString?.let { if (it.isNotBlank()) PreferenceManager.setRefreshToken(it) }
                    access
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private var quickReplies = mutableListOf<String>()
    private val previewSelectedFiles = mutableListOf<Pair<File, ChatMessageType>>()

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.any { !it }) Toast.makeText(
                this,
                "Permission needed for camera and files",
                Toast.LENGTH_SHORT
            ).show()
        }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera() else Toast.makeText(
                this,
                "Camera permission needed",
                Toast.LENGTH_SHORT
            ).show()
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraPhotoPath != null) {
                val file = File(cameraPhotoPath!!)
                appendFilesToPreview(listOf(file to ChatMessageType.IMAGE))
            } else cameraPhotoPath = null
        }

    private fun launchCamera() {
        val photoFile = File(cacheDir, "chat_photo_${System.currentTimeMillis()}.jpg")
        cameraPhotoPath = photoFile.absolutePath
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
        takePictureLauncher.launch(uri)
    }


    private val fileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri>? ->
            uris ?: return@registerForActivityResult
            val selectedFiles = mutableListOf<Pair<File, ChatMessageType>>()

            uris.forEach { uri ->
                val file = uriToFile(uri)
                if (file != null) {
                    val sizeInMb = file.length() / (1024 * 1024)
                    if (sizeInMb > 30) {
                        Toast.makeText(this, "File ${file.name} exceeds 30MB", Toast.LENGTH_LONG)
                            .show()
                    } else {
                        val mimeType = contentResolver.getType(uri) ?: ""
                        val type = when {
                            mimeType.startsWith("image") -> ChatMessageType.IMAGE
                            mimeType.startsWith("video") -> ChatMessageType.VIDEO
                            else -> ChatMessageType.FILE
                        }
                        selectedFiles.add(file to type)
                    }
                }
            }

            if (selectedFiles.isNotEmpty()) {
                appendFilesToPreview(selectedFiles)
            }
        }

//    private fun uriToFile(uri: Uri): File? {
//        return try {
//            contentResolver.openInputStream(uri)?.use { input ->
//                val file =
//                    File(cacheDir, "temp_file_${System.currentTimeMillis()}_${uri.lastPathSegment}")
//                file.outputStream().use { input.copyTo(it) }
//                file
//            }
//        } catch (_: Exception) {
//            null
//        }
//    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            val nameIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor?.moveToFirst()

            val fileName = if (nameIndex != null && nameIndex >= 0) {
                cursor.getString(nameIndex)
            } else {
                "file_${System.currentTimeMillis()}"
            }

            cursor?.close()

            val file = File(cacheDir, fileName)

            contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showUniversalConfirmationDialog(
        title: String,
        message: String,
        isCancelButtonVisible: Boolean,
        onPositiveClick: () -> Unit
    ) {
        val dialogBinding = LayoutUniversalDialogBinding.inflate(layoutInflater)
        dialogBinding.tvDialogTitle.text = title
        dialogBinding.tvDialogMessage.text = message
        dialogBinding.btnPositive.text = "OK"
        dialogBinding.btnNegative.visibility =
            if (isCancelButtonVisible) View.VISIBLE else View.GONE

        val dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)

        dialogBinding.btnPositive.setOnClickListener {
            onPositiveClick()
            dialog.dismiss()
        }

        dialogBinding.btnNegative.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private var progressDialog: Dialog? = null

    private fun showProgressDialogLocal() {
        if (progressDialog == null) {
            progressDialog = com.app.vc.utils.AndroidUtils.progressDialog(this)
        }
        if (progressDialog?.isShowing == false) {
            progressDialog?.show()
        }
    }

    private fun dismissProgressDialogLocal() {
        if (progressDialog?.isShowing == true) {
            progressDialog?.dismiss()
        }
    }

    private fun handleEstimationClick() {
        val now = System.currentTimeMillis()
        if (now - lastClickTime < clickTimeInterval) return
        lastClickTime = now
        Log.d("handleEstimationClick", "handleEstimationClick: ")

        sharedViewModel.isProgressBarVisible.value = true

        // Check if estimation is already shared/approved logic from MessageFragment
        if (isEstimationShared()) {
            if (!isAnyEstimationApproved()) {
                if (canEstimationBeSent()) {
                    sharedViewModel.getEstimationDetails.value = true
                } else {
                    sharedViewModel.isProgressBarVisible.value = false
                    showUniversalConfirmationDialog(
                        title = "Already Shared !!!",
                        message = "Estimation cannot be sent.",
                        isCancelButtonVisible = false
                    ) { /* Handle OK click */ }
                }
            } else {
                sharedViewModel.isProgressBarVisible.value = false
                // Estimation is shared and approved at least once
                showUniversalConfirmationDialog(
                    title = "Already Shared !!!",
                    message = "Estimation details has been shared and approved.",
                    isCancelButtonVisible = false
                ) { /* Handle OK click */ }
            }
        } else {
            // Estimation is not shared at all
            sharedViewModel.getEstimationDetails.value = true
        }
    }

    // Helper to check if estimation exists in current message list
    private fun isEstimationShared(): Boolean {
        for (message in dataList) {
            if (message.estimationDetails != null) {
                return true
            }
        }
        return false
    }

    private fun isAnyEstimationApproved(): Boolean {
        for (message in dataList) {
            if (message.estimationDetails?.estimationApprovalStatus == "Y") {
                return true
            }
        }
        return false
    }

    private fun canEstimationBeSent(): Boolean {
        //Cases estimation is sent and not approved even once.
        var canBeSent: Boolean? = null

        for (message in dataList) {
            if (message.estimationDetails?.estimationApprovalStatus == "N") {
                canBeSent = true
                continue
            }
            if (message.estimationDetails != null) {
                canBeSent = false
            }
        }

        return canBeSent ?: true
    }

    private fun showDialogToConfirmEstimation(estimationDetails: ResponseModelEstimateData) {
        val dialogBinding = LayoutUniversalDialogBinding.inflate(layoutInflater)
        dialogBinding.tvDialogTitle.visibility = View.GONE
        dialogBinding.tvDialogMessage.text = "Do you want to send the \n estimation details."
        dialogBinding.btnNegative.text = "No"
        dialogBinding.btnPositive.text = "Yes"

        val dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)

        dialogBinding.btnNegative.setOnClickListener {
            dialog.dismiss()
        }
        dialogBinding.btnPositive.setOnClickListener {
            dialog.dismiss()
            sendEstimationMessage(estimationDetails)
        }

        dialog.show()
    }

    private fun sendEstimationMessage(estimationDetails: ResponseModelEstimateData) {
        val timeLabel = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date()).lowercase()
        val localIdLong = System.currentTimeMillis()
        val localId = "local_est_$localIdLong"

        val chatMessage = ChatMessage(
            messageId = localId,
            text = "",
            isSender = true,
            timeLabel = timeLabel,
            type = ChatMessageType.ESTIMATION,
            estimationDetails = estimationDetails,
            status = MessageStatus.SENT
        )

        messageAdapter?.addMessage(chatMessage)
        scrollToLast()

        // Send via WebSocket
        val json = JsonObject()
        json.addProperty("type", "chat.message")
        json.addProperty("content", "Estimation Details Shared")
        json.add("estimation_details", Gson().toJsonTree(estimationDetails))
        WebSocketManager.getInstance().sendMessage(json.toString())

        // Also update dataList to keep it in sync for isEstimationShared() logic
        val messageModel = MessageModel(
            userName = "You",
            messageText = "",
            isLocalMessage = true,
            messageType = VCConstants.ESTIMATION_MESSAGE,
            id = localIdLong,
            fileName = "",
            serverFilePath = "",
            status = MessageStatusEnum.MSG_SENT_SUCCESS.tag,
            estimationDetails = estimationDetails
        )
        dataList.add(messageModel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = VcActivityVirtualChatRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        PreferenceManager.init(this)
        connectivityBannerHandler = ConnectivityBannerHandler(
            context = this,
            rootViewProvider = { findViewById(android.R.id.content) },
            onConnectionChanged = { connected ->
                val wasConnected = isNetworkAvailable
                isNetworkAvailable = connected
                if (connected) {
                    pendingRecoveryBanner = !wasConnected || shouldShowRecoveryBannerOnReconnect
                    suppressSocketToastUntilMs = SystemClock.elapsedRealtime() + 5000L
                    if (!pendingRecoveryBanner) {
                        hideNetworkErrorBanner()
                    }
                    WebSocketManager.getInstance().reconnectNow()
                } else {
                    pendingRecoveryBanner = false
                    shouldShowRecoveryBannerOnReconnect = true
                    suppressSocketToastUntilMs = SystemClock.elapsedRealtime() + 5000L
                    showNetworkErrorBanner()
                }
            }
        )

        val roleFromIntent = intent.getStringExtra(EXTRA_ROLE)

        val roomStatus=intent.getStringExtra(STATUS)
        Log.d(TAG, "onCreate: $roomStatus")

        val roomJson = intent.getStringExtra(EXTRA_ROOM_JSON)
        if (roomJson != null) {
            room = Gson().fromJson(roomJson, VirtualRoomUiModel::class.java)
            jobNotes = room?.serviceNotes
            statusLabel = room?.lifecycleStatusLabel
            room?.roNumber?.let { groupSlug ->
                GroupUnreadStore.markRead(groupSlug)
                NotificationWebSocketManager.getInstance().setActiveGroupSlug(groupSlug)
            }
        }

        // Status chip: initial value from room.status (service status only); updated by service.status WebSocket only (not lifecycle)
        binding.txtStatusChip?.let { tv ->
            Log.d(TAG, "onCreate:txtStatusChip:: $room")
            val initialStatus = room?.status ?: roomStatus ?: ""
            tv.text = initialStatus.replace('_', ' ')
            tv.visibility = if (initialStatus.isNotBlank()) View.VISIBLE else View.GONE
        }

        currentRole = when (roleFromIntent) {
            UserRole.SERVICE_ADVISOR.name -> UserRole.SERVICE_ADVISOR
            UserRole.MANAGER.name -> UserRole.MANAGER
            else -> UserRole.CUSTOMER
        }
        Log.d("init"
            , "init: sharedViewModel.messageListInMVM -> ${sharedViewModel.messageListInMVM}")
        dataList.clear()
        dataList.addAll(sharedViewModel.messageListInMVM)
        if (binding.tabParticipants != null) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            val tabRoDetails = binding.tabRoDetails ?: return
            loadFragment(RODetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(RODetailsFragment.KEY_JOB_NOTES, jobNotes ?: "")
                    putString(RODetailsFragment.KEY_STATUS_LABEL, statusLabel ?: "")
                    putString(RODetailsFragment.KEY_RO_NUMBER, room?.roNumberDisplay ?: "")
                }
            })
            setupTabs()
            selectRoDetailsTab()
            moveIndicator(tabRoDetails)

        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        binding.repairOrderLayout?.setOnClickListener {
            val intent = Intent(this, RepairOrderActivity::class.java)
            room?.let { r ->
                intent.putExtra(RepairOrderActivity.EXTRA_GROUP_SLUG, r.roNumber)
                intent.putExtra(RepairOrderActivity.EXTRA_RO_NUMBER, r.roNumberDisplay)
                intent.putExtra(RepairOrderActivity.EXTRA_STATUS_LABEL, r.lifecycleStatusLabel)
                intent.putExtra(RepairOrderActivity.EXTRA_DESCRIPTION, r.customerName)
                intent.putExtra(RepairOrderActivity.EXTRA_DAY_LABEL, r.dayLabel)
                intent.putExtra(RepairOrderActivity.EXTRA_TIME_LABEL, r.timeLabel)
            }
            startActivity(intent)
        }

        binding.btnVideoCall?.setOnClickListener {
            RequestVideoCallDialog(this).show()
        }

//        val roomJson = intent.getStringExtra(EXTRA_ROOM_JSON)
//        if (roomJson != null) {
//            room = Gson().fromJson(roomJson, VirtualRoomUiModel::class.java)
//        }
// Inside onCreate

        setupToolbar()
        bindStaticPhoneHeader()
        bindStaticTabletPanels()
        setupMessageList()
        setupNetworkErrorBanner()
        setupQuickReplies()
        setupSendActions()
        setupAttachmentAndMedia()
        setupVoiceNote()
        setMessagesLoading(true)
        connectToWebSocket()
        fetchGroupMembers()
        fetchQuickReplies()
        fetchMessages()
        fetchServiceLifecycle()
        fetchTemplates()

        sharedViewModel.estimateDetailsResponse.observe(this) {
            if (it != null) {
                dismissProgressDialogLocal()
                showDialogToConfirmEstimation(it)
            }
        }

        sharedViewModel.isProgressBarVisible.observe(this) {
            if (it != null) {
                if (it) {
                    showProgressDialogLocal()
                } else {
                    dismissProgressDialogLocal()
                }
            }
        }

        sharedViewModel.updateEstimateStatus.observe(this) {
            if (it != null) {
                updateEstimateStatusApi(it)
                sharedViewModel.updateEstimateStatus.value = null
            }
        }

        sharedViewModel.updateEstimationStatusResponse.observe(this) {
            if (it != null) {
                if (it.status == "I") {
                    // Update local message list and notify adapter
                    if (sharedViewModel.tempParentPosition != null && sharedViewModel.estimateDetailsAfterApproval != null) {
                        val pos = sharedViewModel.tempParentPosition!!
                        messages[pos] = messages[pos].copy(estimationDetails = sharedViewModel.estimateDetailsAfterApproval)
                        messageAdapter?.notifyItemChanged(pos)
                        
                        // Also update dataList for shared logic
                        dataList[pos].estimationDetails = sharedViewModel.estimateDetailsAfterApproval
                    }
                    sharedViewModel.isProgressBarVisible.value = false
                } else {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                    sharedViewModel.isProgressBarVisible.value = false
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()

        if (!WebSocketManager.getInstance().isConnected()) {
            WebSocketManager.getInstance().reconnectNow()
        }
    }


    override fun onDestroy() {
        WebSocketManager.getInstance().disconnect()
        NotificationWebSocketManager.getInstance().setActiveGroupSlug(null)
        typingHandler.removeCallbacks(stopTypingRunnable)
        voiceTimerHandler.removeCallbacks(voiceTimerRunnable)
        amplitudeHandler.removeCallbacks(amplitudeRunnable)

        mediaRecorder?.release()
        mediaRecorder = null
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        connectivityBannerHandler?.register()
    }

    override fun onStop() {
        connectivityBannerHandler?.unregister()
        super.onStop()
    }

    private fun fetchQuickReplies() {
        val role = if (binding.tabParticipants != null) "service_person" else "customer"
        lifecycleScope.launch {
            try {
                val response = apiService.getQuickReplies(role)
                if (response.isSuccessful && response.body() != null) {
                    val quickReplyResponse = response.body()!!
                    Log.d(TAG, "Quick replies API response: ${Gson().toJson(quickReplyResponse)}")
                    val apiReplies = quickReplyResponse.sortedBy { it.displayOrder }.map { it.text }
                    if (apiReplies.isNotEmpty()) {
                        quickReplies.clear()
                        quickReplies.addAll(apiReplies)
                        runOnUiThread {
                            setupQuickReplies()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("VirtualChatRoom", "Error fetching quick replies: ${e.message}")
            }
        }
    }

    private fun fetchGroupMembers() {
        val slug = room?.roNumber ?: return
        val token = PreferenceManager.getAccessToken() ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getGroupMembers("Bearer $token", slug)
                if (response.isSuccessful && response.body() != null) {
                    val members = response.body()!!
                    Log.d(TAG, "Group members API response for $slug: ${Gson().toJson(members)}")
                    val userIdToDisplay: Map<Int, String> = members.associate { member: GroupMemberResponse ->
                        member.userId to (member.displayName.takeIf { it.isNotBlank() }.orEmpty())
                    }
                    val userIdToRole: Map<Int, String> = members.associate { member: GroupMemberResponse ->
                        member.userId to roleAbbrev(member.participantRole)
                    }
                    withContext(Dispatchers.Main) {
                        memberUserIdToDisplayName.clear()
                        memberUserIdToDisplayName.putAll(userIdToDisplay.filterValues { it.isNotBlank() })
                        memberUserIdToRoleAbbrev.clear()
                        memberUserIdToRoleAbbrev.putAll(userIdToRole.filterValues { it.isNotBlank() })
                        refreshResolvedSenderNames()
                    }
                }
            } catch (e: Exception) {
                Log.e("VirtualChatRoom", "Error fetching group members: ${e.message}")
            }
        }
    }

    private fun roleAbbrev(participantRole: String?): String {
        return when (participantRole?.lowercase(Locale.getDefault())) {
            "service_advisor" -> "SA"
            "service_manager" -> "SM"
            "manager" -> "M"
            "customer" -> "C"
            else -> ""
        }
    }

    private fun resolveRoleAbbrevByUserId(userId: Int?): String? {
        if (userId == null) return null
        return memberUserIdToRoleAbbrev[userId]?.takeIf { it.isNotBlank() }
    }


    private fun fetchMessageCounts() {
        val slug = room?.slug ?: return
        val token = PreferenceManager.getAccessToken() ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getStatusOfMessagesRead("Bearer $token", slug)
                if (response.isSuccessful && response.body() != null) {
                    val members = response.body()!!
                    Log.d(TAG, "fetchMessageCounts for $slug: successfull")
                }
            } catch (e: Exception) {
                Log.e("VirtualChatRoom", "Error fetching fetchMessageCounts: ${e.message}")
            }
        }
    }


    private fun fetchMessages(beforeMessageId: Int? = null, isPagination: Boolean = false) {
        if (!isPagination) {
            hasMoreOlderMessages = true
            oldestLoadedMessageId = null
            showOlderMessagesLoading(false)
        }
        val slug = room?.roNumber ?: run {
            if (!isPagination) setMessagesLoading(false) else showOlderMessagesLoading(false)
            return
        }
        val token = PreferenceManager.getAccessToken() ?: run {
            if (!isPagination) setMessagesLoading(false) else showOlderMessagesLoading(false)
            return
        }
        if (isPagination) {
            if (isLoadingOlderMessages || !hasMoreOlderMessages) return
            isLoadingOlderMessages = true
            showOlderMessagesLoading(true)
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getMessages("Bearer $token", slug, beforeMessageId)
                if (response.isSuccessful && response.body() != null) {
                    val currentUserId = PreferenceManager.getUserId()
                    val apiMessages = parseApiMessagesResponse(response.body())
                    Log.d(TAG, "Messages API response for $slug before=$beforeMessageId: ${gson.toJson(apiMessages)}")
                    val chatMessages = apiMessages.map { apiMsg -> apiMessageToChatMessage(apiMsg, currentUserId) }
                    fetchMessageCounts()
                    withContext(Dispatchers.Main) {
                        if (!isPagination) {
                            sentReadReceiptMessageIds.clear()
                            pendingReadReceiptMessageIds.clear()
                            apiMessages.filter { apiMsg ->
                                val isOwnMessage = apiMsg.sender?.id?.toString() == currentUserId
                                !isOwnMessage && apiMsg.isRead
                            }.forEach { sentReadReceiptMessageIds.add(it.id) }
                        }
                        oldestLoadedMessageId = (currentRawMessages() + chatMessages)
                            .mapNotNull { it.messageId?.toIntOrNull() }
                            .minOrNull()
                        hasMoreOlderMessages = chatMessages.isNotEmpty()
                        if (isPagination) {
                            prependOlderMessages(chatMessages)
                        } else {
                            val withHeaders = buildMessagesWithDateHeaders(chatMessages)
                            messages.clear()
                            messages.addAll(withHeaders)
                            ChatMediaStore.replaceMessages(slug, chatMessages)
                            messageAdapter?.notifyDataSetChanged()
                            scrollToLast()
                            scheduleVisibleReadReceipt(120L)
                        }
                        setMessagesLoading(false)
                        showOlderMessagesLoading(false)
                        isLoadingOlderMessages = false
                        hideNetworkErrorBanner()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        setMessagesLoading(false)
                        showOlderMessagesLoading(false)
                        isLoadingOlderMessages = false
                        showNetworkErrorBanner()
                    }
                }
            } catch (e: Exception) {
                Log.e("VirtualChatRoom", "Error fetching messages: ${e.message}")
                withContext(Dispatchers.Main) {
                    setMessagesLoading(false)
                    showOlderMessagesLoading(false)
                    isLoadingOlderMessages = false
                    showNetworkErrorBanner()
                }
            }
        }
    }

    private fun apiMessageToChatMessage(apiMsg: com.app.vc.network.ApiMessageResponse, currentUserId: String?): ChatMessage {
        val isSender = apiMsg.sender?.id?.toString() == currentUserId
        val attachment = apiMsg.attachments?.firstOrNull()
        val type = when {
            attachment != null -> when {
                attachment.mimeType.startsWith("image") -> ChatMessageType.IMAGE
                attachment.mimeType.startsWith("video") -> ChatMessageType.VIDEO
                attachment.mimeType.startsWith("audio") -> ChatMessageType.VOICE_NOTE
                else -> ChatMessageType.FILE
            }
            else -> when (apiMsg.messageType) {
                "image" -> ChatMessageType.IMAGE
                "video" -> ChatMessageType.VIDEO
                "document" -> ChatMessageType.FILE
                else -> ChatMessageType.TEXT
            }
        }

        val attachmentUri = attachment?.fileUrl?.let { url ->
            if (url.startsWith("http")) url else ApiDetails.APRIK_Kia_BASE_URL + url
        }
        val thumbUrl = attachment?.thumbnailUrl?.let { url ->
            if (url.startsWith("http")) url else ApiDetails.APRIK_Kia_BASE_URL + url
        }
        val isRead = apiMsg.isRead || (apiMsg.receipts?.isNotEmpty() == true)

        return ChatMessage(
            messageId = apiMsg.id.toString(),
            text = if (type == ChatMessageType.TEXT) apiMsg.content else "",
            isSender = isSender,
            senderName = if (isSender) null else resolveDisplayName(apiMsg.sender?.id, apiMsg.sender?.username),
            senderUsername = apiMsg.sender?.username,
            senderId = apiMsg.sender?.id?.toString(),
            senderRoleAbbrev = if (isSender) roleAbbrev(PreferenceManager.getuserType()) else resolveRoleAbbrevByUserId(apiMsg.sender?.id),
            timeLabel = formatApiDate(apiMsg.createdAt),
            createdAtMillis = parseApiCreatedAtMillis(apiMsg.createdAt),
            status = if (isRead) MessageStatus.READ else MessageStatus.SENT,
            type = type,
            attachmentUri = attachmentUri,
            fileName = attachment?.fileName,
            caption = if (type != ChatMessageType.TEXT) apiMsg.content else null,
            thumbnailUrl = thumbUrl
        )
    }

    private fun parseApiMessagesResponse(body: JsonElement?): List<com.app.vc.network.ApiMessageResponse> {
        if (body == null || body.isJsonNull) return emptyList()
        return try {
            when {
                body.isJsonArray -> body.asJsonArray.mapNotNull {
                    gson.fromJson(it, com.app.vc.network.ApiMessageResponse::class.java)
                }
                body.isJsonObject -> {
                    val obj = body.asJsonObject
                    val array = when {
                        obj.get("results")?.isJsonArray == true -> obj.getAsJsonArray("results")
                        obj.get("data")?.isJsonArray == true -> obj.getAsJsonArray("data")
                        obj.get("messages")?.isJsonArray == true -> obj.getAsJsonArray("messages")
                        else -> null
                    }
                    array?.mapNotNull {
                        gson.fromJson(it, com.app.vc.network.ApiMessageResponse::class.java)
                    } ?: emptyList()
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse messages response: ${e.message}")
            emptyList()
        }
    }

    private fun currentRawMessages(): List<ChatMessage> =
        messages.filter { it.type != ChatMessageType.DATE_HEADER }

    private fun prependOlderMessages(olderMessages: List<ChatMessage>) {
        if (olderMessages.isEmpty()) return
        val recycler = binding.recyclerMessages
        val layoutManager = recycler.layoutManager as? LinearLayoutManager ?: return
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val anchorId = messages.getOrNull(firstVisiblePosition)?.messageId
        val anchorTop = recycler.getChildAt(0)?.top ?: 0
        val existingIds = currentRawMessages().mapNotNull { it.messageId }.toHashSet()
        val uniqueOlderMessages = olderMessages.filter { messageId ->
            messageId.messageId.isNullOrBlank() || !existingIds.contains(messageId.messageId)
        }
        if (uniqueOlderMessages.isEmpty()) {
            hasMoreOlderMessages = false
            return
        }
        val combinedMessages = uniqueOlderMessages + currentRawMessages()
        val withHeaders = buildMessagesWithDateHeaders(combinedMessages)
        suppressAutoScroll = true
        messageAdapter?.replaceAll(withHeaders)
        ChatMediaStore.replaceMessages(room?.roNumber ?: return, combinedMessages)
        recycler.post {
            val anchorIndex = if (anchorId.isNullOrBlank()) -1 else messages.indexOfFirst { it.messageId == anchorId }
            if (anchorIndex >= 0) {
                layoutManager.scrollToPositionWithOffset(anchorIndex, anchorTop)
            }
            suppressAutoScroll = false
        }
    }

    private fun setupNetworkErrorBanner() {
        binding.root.findViewById<View>(R.id.includeNetworkErrorBanner)
            ?.findViewById<View>(R.id.btnDismissNetworkError)
            ?.setOnClickListener { hideNetworkErrorBanner(force = true) }
    }

    private fun showNetworkErrorBanner() {
        networkBannerHandler.removeCallbacks(hideRecoveryBannerRunnable)
        val banner = binding.root.findViewById<View>(R.id.includeNetworkErrorBanner) ?: return
        banner.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
        banner.findViewById<TextView>(R.id.txtNetworkError)?.text =
            "Network error Please check your Internet connection."
        banner.visibility = View.VISIBLE
        networkErrorVisible = true
        isRecoveryBannerShowing = false
        shouldShowRecoveryBannerOnReconnect = true
    }

    private fun hideNetworkErrorBanner(force: Boolean = false) {
        if (!force && isRecoveryBannerShowing && isNetworkAvailable) return
        networkBannerHandler.removeCallbacks(hideRecoveryBannerRunnable)
        binding.root.findViewById<View>(R.id.includeNetworkErrorBanner)?.visibility = View.GONE
        networkErrorVisible = false
        isRecoveryBannerShowing = false
    }

    private fun showNetworkRecoveryBanner() {
        networkBannerHandler.removeCallbacks(hideRecoveryBannerRunnable)
        val banner = binding.root.findViewById<View>(R.id.includeNetworkErrorBanner) ?: return
        banner.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
        banner.findViewById<TextView>(R.id.txtNetworkError)?.text =
            "Network connection is available."
        banner.visibility = View.VISIBLE
        networkErrorVisible = true
        isRecoveryBannerShowing = true
        shouldShowRecoveryBannerOnReconnect = false
        networkBannerHandler.postDelayed(hideRecoveryBannerRunnable, 2500L)
    }

    private fun setMessagesLoading(isLoading: Boolean) {
        binding.root.findViewById<ProgressBar>(R.id.progressLoadingMessages)?.visibility =
            if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerMessages.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
    }

    private fun showOlderMessagesLoading(isLoading: Boolean) {
        binding.root.findViewById<ProgressBar>(R.id.progressLoadingOlderMessages)?.visibility =
            if (isLoading) View.VISIBLE else View.GONE
    }

    private var jobNotes: String? = null
    private var statusLabel: String? = null

    /** Builds the text shown in the pinned lifecycle banner (API and WebSocket). */
    private fun buildPinnedLifecycleText(
        statusLabel: String?
    ): String {
        val status = statusLabel?.takeIf { it.isNotBlank() }
        return  "$status"
    }

    private fun fetchServiceLifecycle() {
        val slug = room?.roNumber ?: return
        val token = PreferenceManager.getAccessToken() ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getServiceLifecycleCurrent("Bearer $token", slug)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d(TAG, "Service lifecycle API response for $slug: ${Gson().toJson(body)}")
                    jobNotes = body.notes?.takeIf { it.isNotBlank() }
                    statusLabel = body.statusLabel?.takeIf { it.isNotBlank() }
                    Log.d("VirtualChatRoom", "Service lifecycle response: $body")
                    withContext(Dispatchers.Main) {
                        // Pinned: lifecycle only. Show on both phone and tablet when we have lifecycle data.
                        val pinnedText = buildPinnedLifecycleText(
                            statusLabel = statusLabel
                        )
                        binding.layoutPinnedStatus?.visibility =
                            if (pinnedText.isNotBlank()) View.VISIBLE else View.GONE
                        binding.txtPinnedLifecycle?.text = pinnedText

                        // Do NOT update txtStatusChip here — chip shows only service status (room.status initially, then service.status WebSocket)
                        (supportFragmentManager.findFragmentById(R.id.FragmentContainer) as? RODetailsFragment)?.let { frag ->
                            frag.setJobNotes(jobNotes)
                            frag.setStatusLabel(statusLabel)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("VirtualChatRoom", "Error fetching service lifecycle: ${e.message}")
            }
        }
    }

    private fun fetchTemplates() {
        val token = PreferenceManager.getAccessToken() ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getTemplates("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val templates = response.body()!!
                    Log.d(TAG, "Templates API response: ${Gson().toJson(templates)}")
                    withContext(Dispatchers.Main) {
                        Log.d("VirtualChatRoom", "Templates API response: count=${templates.size}")
                        templates.forEachIndexed { index, t ->
                            Log.d("VirtualChatRoom", "Template[$index]: id=${t.id}, key=${t.key}, title=${t.title}, body=${t.body}, is_active=${t.isActive}")
                        }
                    }
                } else {
                    Log.e("VirtualChatRoom", "Templates API failed: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("VirtualChatRoom", "Error fetching templates: ${e.message}")
            }
        }
    }

    private fun formatApiDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            if (date != null) {
                SimpleDateFormat("hh:mma", Locale.getDefault()).format(date).lowercase()
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseApiCreatedAtMillis(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            // API uses microseconds; we parse first 19 chars (seconds precision)
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            inputFormat.parse(dateStr.take(19))?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun buildMessagesWithDateHeaders(source: List<ChatMessage>): List<ChatMessage> {
        if (source.isEmpty()) return emptyList()
        val out = ArrayList<ChatMessage>(source.size + 8)

        fun dayKey(ms: Long): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(ms))
        }

        fun headerLabelFor(ms: Long): String {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = ms
            val today = java.util.Calendar.getInstance()
            val yday = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
            val key = dayKey(ms)
            return when (key) {
                dayKey(today.timeInMillis) -> "Today"
                dayKey(yday.timeInMillis) -> "Yesterday"
                else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(ms))
            }
        }

        var lastDay: String? = null
        source.forEach { msg ->
            if (msg.type == ChatMessageType.DATE_HEADER) return@forEach
            val ms = msg.createdAtMillis ?: System.currentTimeMillis()
            val d = dayKey(ms)
            if (d != lastDay) {
                lastDay = d
                out.add(
                    ChatMessage(
                        messageId = "date_$d",
                        text = headerLabelFor(ms),
                        isSender = false,
                        timeLabel = "",
                        type = ChatMessageType.DATE_HEADER,
                        createdAtMillis = ms
                    )
                )
            }
            out.add(msg)
        }
        return out
    }

    private fun updateEstimateStatusApi(estimationStatus: Boolean) {
        if (estimationStatus) {
            sharedViewModel.updateEstimationStatusNew(
                customerCode = sharedViewModel.customerCode!!,
                estimationStatus = sharedViewModel.estimateDetailsAfterApproval?.estimationApprovalStatus ?: "N",
                employeeNumber = sharedViewModel.serviceAdvisorID.toString(),
                labourListCodes = sharedViewModel.selectedLabourList!!,
                partListCodes = sharedViewModel.selectedPartList!!,
                roNumber = sharedViewModel.roNo!!,
                dealerCode = sharedViewModel.dealerCode!!,
                baseUrl = PreferenceManager.getBaseUrl()!!
            )
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.FragmentContainer, fragment).commit()
    }

    private fun setupVoiceNote() {
        binding.recordLayout?.setOnClickListener { showVoiceNoteDialog() }
    }

    private fun showVoiceNoteDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.vc_dialog_voice_note, null)
        voiceNoteWaveformView = view.findViewById(R.id.waveformView)
        voiceNoteDialogTimerView = view.findViewById<TextView>(R.id.txtVoiceTimer)
        val btnClose = view.findViewById<ImageView>(R.id.btnCloseVoice)
        val btnRecord = view.findViewById<ImageView>(R.id.btnRecordVoice)
        val btnDelete = view.findViewById<ImageView>(R.id.btnDeleteVoice)
        val btnPlay = view.findViewById<ImageView>(R.id.btnPlayPauseVoice)
        val pauseIcon = view.findViewById<ImageView>(R.id.pauseIcon)
        val btnSave = view.findViewById<TextView>(R.id.btnSaveVoice)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancelVoice)

        voiceNoteDurationSeconds = 0
        voiceNotePath = null
        voiceNoteDialogTimerView?.text = "00:00"

        val dialog = AlertDialog.Builder(this).setView(view).create()

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnRecord.setOnClickListener {

            if (isPlaying) {
                Toast.makeText(this, "Stop playback before recording", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            recordedAmplitudes.clear()
            voiceNoteWaveformView?.clear()
            voiceNoteWaveformView?.visibility = View.VISIBLE

            // ⭐ If old recording exists, delete it and start new recording
            voiceNotePath?.let {
                val file = File(it)
                if (file.exists()) file.delete()
            }

            voiceNotePath = null
            voiceNoteDurationSeconds = 0
            voiceNoteDialogTimerView?.text = "00:00"



            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {

                recordedAmplitudes.clear()
                voiceNoteWaveformView?.clear()
                startRecordingFlow(btnRecord, btnDelete, btnPlay, pauseIcon)
            } else {
                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        pauseIcon.setOnClickListener {
            if (isRecording) {
                stopVoiceRecording()
                isRecording = false

                pauseIcon.visibility = View.GONE
                btnRecord.visibility = View.VISIBLE
                btnDelete.visibility = View.VISIBLE
                btnPlay.visibility = View.VISIBLE
            }
        }

        btnPlay.setOnClickListener {

            voiceNotePath?.let { path ->

                if (isPlaying) {
                    stopPlayback()
                    btnPlay.setImageResource(R.drawable.play_circle)

                    btnDelete.isEnabled = true
                    btnRecord.isEnabled = true

                } else {

                    voiceNoteWaveformView?.setAmplitudes(
                        recordedAmplitudes.takeLast(50).toIntArray()
                    )

                    voiceNoteWaveformView?.resetProgress()

                    playVoiceNote(path, btnPlay, btnDelete, btnRecord)

                    btnPlay.setImageResource(R.drawable.pause)

                    btnDelete.isEnabled = false
                    btnRecord.isEnabled = false
                }
            }
        }

        btnDelete.setOnClickListener {

            if (isPlaying) {
                Toast.makeText(this, "Stop playback before deleting", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            voiceNotePath?.let {
                val file = File(it)
                if (file.exists()) file.delete()
            }

            voiceNotePath = null
            voiceNoteDurationSeconds = 0
            voiceNoteDialogTimerView?.text = "00:00"

            // ⭐ Clear waveform data
            recordedAmplitudes.clear()

            // ⭐ Clear waveform view
            voiceNoteWaveformView?.clear()

            // ⭐ Hide waveform
            voiceNoteWaveformView?.visibility = View.GONE
            btnDelete.visibility = View.GONE
            btnPlay.visibility = View.GONE
            btnRecord.visibility = View.VISIBLE

        }

        btnSave.setOnClickListener {
            if (isRecording) {
                stopVoiceRecording()
                isRecording = false
            }
            dialog.dismiss()
            voiceNotePath?.let { path ->
                val localId = "local_voice_${System.currentTimeMillis()}"
                val tempMessage = ChatMessage(
                    messageId = localId,
                    text = "",
                    isSender = true,
                    timeLabel = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date())
                        .lowercase(),
                    type = ChatMessageType.VOICE_NOTE,
                    attachmentUri = path,
                    durationSeconds = voiceNoteDurationSeconds,
                    status = MessageStatus.SENDING
                )
                messageAdapter?.addMessage(tempMessage)
                scrollToLast()
                performUpload(File(path), "Voice Note", ChatMessageType.VOICE_NOTE, localId)
            }
        }

        dialog.show()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecordingFlow(r: ImageView, d: ImageView, p: ImageView, pause: ImageView) {
        startVoiceRecording()
        isRecording = true
        r.visibility = View.GONE
        d.visibility = View.GONE
        p.visibility = View.GONE
        pause.visibility = View.VISIBLE
    }

    private val recordAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show()
        }
//
//    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
//    private fun startVoiceRecording() {
//        val sampleRate = 44100
//        val bufferSize = AudioRecord.getMinBufferSize(
//            sampleRate,
//            AudioFormat.CHANNEL_IN_MONO,
//            AudioFormat.ENCODING_PCM_16BIT
//        )
//        audioRecord = AudioRecord(
//            MediaRecorder.AudioSource.MIC,
//            sampleRate,
//            AudioFormat.CHANNEL_IN_MONO,
//            AudioFormat.ENCODING_PCM_16BIT,
//            bufferSize
//        )
//        pcmFile = File(cacheDir, "voice_note.pcm")
//        voiceNotePath = pcmFile?.absolutePath
//        audioRecord?.startRecording()
//        isRecording = true
//        voiceTimerHandler.post(voiceTimerRunnable)
//        Thread {
//            val buffer = ShortArray(1024)
//            val output = FileOutputStream(pcmFile!!)
//            while (isRecording) {
//                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
//                if (read > 0) {
//                    val maxAmp = buffer.take(read).maxOf { abs(it.toInt()) }
//                    runOnUiThread {
//                        voiceNoteWaveformView?.addAmplitude(
//                            (maxAmp / 300).coerceAtLeast(
//                                1
//                            )
//                        )
//                    }
//                    output.write(
//                        ByteBuffer.allocate(read * 2).order(ByteOrder.LITTLE_ENDIAN)
//                            .apply { buffer.take(read).forEach { putShort(it) } }.array()
//                    )
//                }
//            }
//            output.close()
//        }.start()
//    }

//    private fun stopVoiceRecording() {
//        isRecording = false
//        audioRecord?.stop()
//        audioRecord?.release()
//        audioRecord = null
//        voiceTimerHandler.removeCallbacks(voiceTimerRunnable)
//    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startVoiceRecording() {

        val file = File(cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
        voiceNotePath = file.absolutePath

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(voiceNotePath)
            prepare()
            start()
        }

        isRecording = true
        voiceTimerHandler.post(voiceTimerRunnable)

        // ⭐ START WAVEFORM ANIMATION
        amplitudeHandler.post(amplitudeRunnable)
    }
    private fun stopVoiceRecording() {

        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mediaRecorder = null
        isRecording = false

        voiceTimerHandler.removeCallbacks(voiceTimerRunnable)
    }
    private fun playVoiceNote(path: String) {

        // ⭐ STOP waveform animation
        amplitudeHandler.removeCallbacks(amplitudeRunnable)
    }
    private fun playVoiceNote(
        path: String,
        btnPlay: ImageView,
        btnDelete: ImageView,
        btnRecord: ImageView
    ) {

        mediaPlayer = MediaPlayer()
        mediaPlayer?.setDataSource(path)
        mediaPlayer?.prepare()
        mediaPlayer?.start()

        isPlaying = true

        playbackTimerHandler.post(playbackTimerRunnable)

        mediaPlayer?.setOnCompletionListener {

            stopPlayback()

            runOnUiThread {

                // Reset play icon
                btnPlay.setImageResource(R.drawable.play_circle)

                // Enable delete + record
                btnDelete.isEnabled = true
                btnRecord.isEnabled = true

                // Reset timer to recorded duration
                voiceNoteDialogTimerView?.text =
                    "%02d:%02d".format(
                        voiceNoteDurationSeconds / 60,
                        voiceNoteDurationSeconds % 60
                    )
            }
        }
    }
    private fun stopPlayback() {

        isPlaying = false

        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {}

        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}

        mediaPlayer = null

        playbackTimerHandler.removeCallbacks(playbackTimerRunnable)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupTabs() {

        val tabParticipants = binding.tabParticipants ?: return
        val tabMedia = binding.tabMedia ?: return
        val tabRoDetails = binding.tabRoDetails ?: return

        tabParticipants.setOnClickListener {

            loadFragment(ParticipantsListFragment().apply {
                arguments = Bundle().apply {
                    putString(ParticipantsListFragment.KEY_GROUP_SLUG, room?.roNumber)
                }
            })

            selectParticipantsTab()

            moveIndicator(tabParticipants)
        }

        tabMedia.setOnClickListener {

            loadFragment(MediaFragment().apply {
                arguments = Bundle().apply {
                    putString(MediaFragment.KEY_GROUP_SLUG, room?.roNumber)
                }
            })

            selectMediaTab()
            moveIndicator(tabMedia)
        }
        tabRoDetails.setOnClickListener {

            loadFragment(RODetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(RODetailsFragment.KEY_JOB_NOTES, jobNotes ?: "")
                    putString(RODetailsFragment.KEY_STATUS_LABEL, statusLabel ?: "")
                    putString(RODetailsFragment.KEY_RO_NUMBER, room?.roNumberDisplay ?: "")
                }
            })

            selectRoDetailsTab()
            moveIndicator(tabRoDetails)
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun selectParticipantsTab() {

        val tabParticipants = binding.tabParticipants ?: return
        val tabMedia = binding.tabMedia ?: return
        val tabRodetails=binding.tabRoDetails?: return

        tabParticipants.setTextColor(
            getColor(R.color.colorPrimary_kia_kandid)
        )

        tabParticipants.typeface =
            resources.getFont(R.font.kia_signature_fix_bold)

        tabMedia.setTextColor(
            getColor(R.color.gray_mic_background)
        )

        tabMedia.typeface =
            resources.getFont(R.font.kia_signature_fix_regular)

        tabRodetails.setTextColor(
            getColor(R.color.gray_mic_background)
        )

        tabRodetails.typeface =
            resources.getFont(R.font.kia_signature_fix_regular)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun selectRoDetailsTab() {

        val tabParticipants = binding.tabParticipants ?: return
        val tabMedia = binding.tabMedia ?: return
        val tabRodetails=binding.tabRoDetails?: return

        tabRodetails.setTextColor(
            getColor(R.color.colorPrimary_kia_kandid)
        )

        tabRodetails.typeface =
            resources.getFont(R.font.kia_signature_fix_bold)


        tabParticipants.setTextColor(
            getColor(R.color.gray_mic_background)
        )

        tabParticipants.typeface =
            resources.getFont(R.font.kia_signature_fix_regular)

        tabMedia.setTextColor(
            getColor(R.color.gray_mic_background)
        )

        tabMedia.typeface =
            resources.getFont(R.font.kia_signature_fix_regular)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun selectMediaTab() {
        val tabRodetails=binding.tabRoDetails?: return

        val tabParticipants = binding.tabParticipants ?: return
        val tabMedia = binding.tabMedia ?: return

        tabMedia.setTextColor(
            getColor(R.color.colorPrimary_kia_kandid)
        )

        tabMedia.typeface =
            resources.getFont(R.font.kia_signature_fix_bold)

        tabParticipants.setTextColor(
            getColor(R.color.gray_mic_background)
        )

        tabParticipants.typeface =
            resources.getFont(R.font.kia_signature_fix_regular)

        tabRodetails.setTextColor(
            getColor(R.color.gray_mic_background)
        )

        tabRodetails.typeface =
            resources.getFont(R.font.kia_signature_fix_regular)
    }

    private fun moveIndicator(tab: View) {

        val indicator = binding.tabIndicator ?: return

        indicator.post {

            val width = tab.width
            val start = tab.left

            indicator.layoutParams.width = width
            indicator.requestLayout()

            indicator.x = start.toFloat()
        }
    }


    private fun connectToWebSocket() {
        val rawRoNumber = room?.roNumber ?: "default-room"
        val roomSlug = rawRoNumber.replace(" ", "_").lowercase()
        val token = PreferenceManager.getAccessToken()

        val url = "wss://testingchat.apprikart.com/ws/chat/$roomSlug/?token=$token"
        Log.d("VirtualChatRoom", "Connecting to WebSocket URL: $url")
        WebSocketManager.getInstance().connect(url, this)
    }

    private fun hasMessage(messageId: String?): Boolean {
        if (messageId.isNullOrBlank()) return false
        return messages.any { it.messageId == messageId }
    }

    private fun appendIncomingMessage(message: ChatMessage) {
        if (hasMessage(message.messageId)) return
        val ms = message.createdAtMillis ?: System.currentTimeMillis()
        val key = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(ms))
        val hasHeaderForDay = messages.any { it.type == ChatMessageType.DATE_HEADER && it.messageId == "date_$key" }
        if (!hasHeaderForDay) {
            val label = buildMessagesWithDateHeaders(listOf(message))
                .firstOrNull { it.type == ChatMessageType.DATE_HEADER }
                ?.text
                ?.takeIf { it.isNotBlank() }
            if (label != null) {
                messageAdapter?.addMessage(
                    ChatMessage(
                        messageId = "date_$key",
                        text = label,
                        isSender = false,
                        timeLabel = "",
                        type = ChatMessageType.DATE_HEADER,
                        createdAtMillis = ms
                    )
                )
            }
        }
        messageAdapter?.addMessage(message)
    }

    private fun resolveSenderDisplayName(username: String?): String? {
        if (username.isNullOrBlank()) return null
        return memberFirstNameByUsername[username]?.takeIf { it.isNotBlank() } ?: username
    }

    private fun resolveSenderDisplayNameByUserId(userId: Int?): String? {
        if (userId == null) return null
        return memberUserIdToDisplayName[userId]?.takeIf { it.isNotBlank() }
    }

    /** Resolves display name from WebSocket/API payload: prefer userId lookup, then username. */
    private fun resolveDisplayName(userId: Int?, username: String?): String? =
        resolveSenderDisplayNameByUserId(userId) ?: resolveSenderDisplayName(username)

    private fun refreshResolvedSenderNames() {
        var changed = false
        messages.forEach { message ->
            if (!message.isSender && message.type != ChatMessageType.DATE_HEADER) {
                val userId = message.senderId?.toIntOrNull()
                val resolvedName = if (userId != null) {
                    memberUserIdToDisplayName[userId]?.takeIf { it.isNotBlank() }
                } else {
                    resolveSenderDisplayName(message.senderUsername)
                }
                if (!resolvedName.isNullOrBlank() && message.senderName != resolvedName) {
                    message.senderName = resolvedName
                    changed = true
                }
                val resolvedRole = if (userId != null) {
                    memberUserIdToRoleAbbrev[userId]?.takeIf { it.isNotBlank() }
                } else null
                if (resolvedRole != null && message.senderRoleAbbrev != resolvedRole) {
                    message.senderRoleAbbrev = resolvedRole
                    changed = true
                }
            }
        }
        if (changed) {
            messageAdapter?.notifyDataSetChanged()
        }
    }

    override fun onConnected() {
        runOnUiThread {
            Log.d("VirtualChatRoom", "WebSocket Connected Successfully")
            PresenceStore.setUserOnline(PreferenceManager.getUserId())
            if (socketRecoveryBannerPending || pendingRecoveryBanner || shouldShowRecoveryBannerOnReconnect) {
                showNetworkRecoveryBanner()
                socketRecoveryBannerPending = false
                pendingRecoveryBanner = false
            } else if (networkErrorVisible && isNetworkAvailable) {
                hideNetworkErrorBanner()
            }
            if (shouldRefetchMessagesOnReconnect) {
                shouldRefetchMessagesOnReconnect = false
                fetchMessages()
            }
            pendingReadReceiptMessageIds.toList().sorted().forEach { sendReadReceipt(it.toString()) }
            scheduleVisibleReadReceipt(150L)
            retryPendingOutgoingMessages()
        }
    }

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            Log.d("VirtualChatRoom", "Message Received: $message")
            try {
                val jsonObject = Gson().fromJson(message, JsonObject::class.java)
                val type = jsonObject.get("type")?.asString

                when (type) {
                    "chat.message" -> {
                        val senderId = when (val el = jsonObject.get("sender_id")) {
                            null -> null
                            else -> if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) el.asJsonPrimitive.asInt.toString() else el.asString
                        }
                        val currentUserId = PreferenceManager.getUserId()
                        PresenceStore.setUserOnline(senderId)

                        if (senderId != null && senderId == currentUserId) {
                            // Echoed message from self: update local message status and attachment URL so bubble shows server file
                            val msgId = messageIdFromJson(jsonObject) ?: return@runOnUiThread
                            messageAdapter?.updateMessageStatus(msgId, MessageStatus.SENT)
                            val fileUrl = jsonObject.get("file_url")?.asString
                                ?: jsonObject.get("attachment")?.asJsonObject?.get("file_url")?.asString
                            if (!fileUrl.isNullOrBlank()) {
                                val fullUrl = if (fileUrl.startsWith("http")) fileUrl else ApiDetails.APRIK_Kia_BASE_URL + fileUrl
                                messageAdapter?.updateMessageAttachmentUrl(msgId, fullUrl)
                            }
                            scrollToLast()
                            return@runOnUiThread
                        }

                        val content = jsonObject.get("content")?.asString
                            ?: jsonObject.get("message")?.asString
                        val attachment = jsonObject.get("attachment")?.asJsonObject


                        if (attachment != null) {

                            val attachmentUrl = attachment.get("file_url")?.asString ?: ""
                            val fullAttachmentUrl =
                                if (attachmentUrl.startsWith("http")) attachmentUrl
                                else ApiDetails.APRIK_Kia_BASE_URL + attachmentUrl
                            val fileName = attachment.get("file_name")?.asString ?: ""
                            val mimeType = attachment.get("mime_type")?.asString ?: ""

                            val msgType = when {
                                mimeType.startsWith("image") -> ChatMessageType.IMAGE
                                mimeType.startsWith("video") -> ChatMessageType.VIDEO
                                mimeType.startsWith("audio") -> ChatMessageType.VOICE_NOTE
                                else -> ChatMessageType.FILE
                            }

                            val mediaMessage = ChatMessage(
                                    messageId = messageIdFromJson(jsonObject),
                                    text = "",
                                    isSender = false,
                                    senderName = resolveDisplayName(jsonObject.get("sender_id")?.let { el -> if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) el.asInt else null }, jsonObject.get("username")?.asString),
                                    senderUsername = jsonObject.get("username")?.asString,
                                    senderId = senderId,
                                    senderRoleAbbrev = resolveRoleAbbrevByUserId(senderId?.toIntOrNull()),
                                    timeLabel = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date()).lowercase(),
                                    type = msgType,
                                    attachmentUri = fullAttachmentUrl,
                                    fileName = fileName,
                                    caption = content,
                                    mimeType = mimeType
                                )
                            appendIncomingMessage(mediaMessage)
                            room?.roNumber?.let { ChatMediaStore.addOrUpdateMessage(it, mediaMessage) }
                        } else if (jsonObject.has("file_url")) {
                            // Server sent chat.message with top-level file_url (e.g. after chat.media)
                            val fileUrl = jsonObject.get("file_url")?.asString ?: ""
                            val fullUrl = if (fileUrl.startsWith("http")) fileUrl else ApiDetails.APRIK_Kia_BASE_URL + fileUrl
                            val messageTypeStr = jsonObject.get("message_type")?.asString ?: "document"
                            val msgType = when (messageTypeStr.lowercase(Locale.getDefault())) {
                                "image" -> ChatMessageType.IMAGE
                                "video" -> ChatMessageType.VIDEO
                                "document" -> ChatMessageType.FILE
                                "audio", "voice" -> ChatMessageType.VOICE_NOTE
                                else -> ChatMessageType.FILE
                            }
                            val fileName = fileUrl.substringAfterLast('/', missingDelimiterValue = "")
                            val caption = jsonObject.get("content")?.asString
                            val thumbUrl = jsonObject.get("thumbnail_url")?.asString?.let { t ->
                                if (t.startsWith("http")) t else ApiDetails.APRIK_Kia_BASE_URL + t
                            }
                            val mediaMessage = ChatMessage(
                                    messageId = messageIdFromJson(jsonObject),
                                    text = "",
                                    isSender = false,
                                    senderName = resolveDisplayName(senderId?.toIntOrNull(), jsonObject.get("username")?.asString),
                                    senderUsername = jsonObject.get("username")?.asString,
                                    senderId = senderId,
                                    senderRoleAbbrev = resolveRoleAbbrevByUserId(senderId?.toIntOrNull()),
                                    timeLabel = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date()).lowercase(),
                                    type = msgType,
                                    attachmentUri = fullUrl,
                                    fileName = if (fileName.isNotBlank()) fileName else null,
                                    caption = caption,
                                    thumbnailUrl = thumbUrl
                                )
                            appendIncomingMessage(mediaMessage)
                            room?.roNumber?.let { ChatMediaStore.addOrUpdateMessage(it, mediaMessage) }
                        }
//                        if (attachment != null) {
//                            val attachmentUrl = attachment.get("file_url")?.asString ?: ""
//                            val fileName = attachment.get("file_name")?.asString ?: ""
//                            val mimeType = attachment.get("mime_type")?.asString ?: ""
//                            val fullUrl =
//                                if (attachmentUrl.startsWith("http")) attachmentUrl else ApiDetails.APRIK_Kia_BASE_URL + attachmentUrl
//
//                            val msgType = when {
//                                mimeType.startsWith("image") -> ChatMessageType.IMAGE
//                                mimeType.startsWith("video") -> ChatMessageType.VIDEO
//                                else -> ChatMessageType.FILE
//                            }
//
//                            messageAdapter?.addMessage(
//                                ChatMessage(
//                                    messageId = jsonObject.get("message_id")?.asString,
//                                    text = "",
//                                    isSender = false,
//                                    timeLabel = SimpleDateFormat(
//                                        "hh:mma",
//                                        Locale.getDefault()
//                                    ).format(Date()).lowercase(),
//                                    type = msgType,
//                                    attachmentUri = fullUrl,
//                                    fileName = fileName,
//                                    caption = content,
//                                    mimeType = mimeType
//                                )
//                            )
//                        }
//
                        else if (content != null) {
                            if (content.contains("_image") || content.contains(".jpg", ignoreCase = true) ||
                                content.contains(".jpeg", ignoreCase = true) || content.contains(".png", ignoreCase = true) ||
                                content.contains(".mp4", ignoreCase = true) || content.contains(".pdf", ignoreCase = true)) {
                                fetchMessages()
                            } else {
                                addMessage(
                                    text = content,
                                    isSender = false,
                                    messageId = messageIdFromJson(jsonObject),
                                    senderName = resolveDisplayName(jsonObject.get("sender_id")?.let { el -> if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) el.asInt else null }, jsonObject.get("username")?.asString),
                                    senderUsername = jsonObject.get("username")?.asString
                                )
                            }
                        }
                        scrollToLast()
                        scheduleVisibleReadReceipt(120L)
                    }

                    "chat.media" -> {
                        val senderId = when (val el = jsonObject.get("sender_id")) {
                            null -> null
                            else -> if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) el.asJsonPrimitive.asInt.toString() else el.asString
                        }
                        val currentUserId = PreferenceManager.getUserId()
                        PresenceStore.setUserOnline(senderId)

                        if (senderId != null && senderId == currentUserId) {
                            Log.d("VirtualChatRoom", "Ignoring echoed media message from self")
                            val msgId = messageIdFromJson(jsonObject)
                            if (msgId != null) {
                                messageAdapter?.updateMessageStatus(msgId, MessageStatus.SENT)
                            }
                            return@runOnUiThread
                        }

                        val messageTypeStr = jsonObject.get("message_type")?.asString ?: ""
                        val msgType = when (messageTypeStr.lowercase(Locale.getDefault())) {
                            "image" -> ChatMessageType.IMAGE
                            "video" -> ChatMessageType.VIDEO
                            "document" -> ChatMessageType.FILE
                            "audio", "voice" -> ChatMessageType.VOICE_NOTE
                            else -> ChatMessageType.FILE
                        }

                        val rawUrl = jsonObject.get("file_url")?.asString ?: ""
                        val fullUrl =
                            if (rawUrl.startsWith("http")) rawUrl else ApiDetails.APRIK_Kia_BASE_URL + rawUrl
                        val caption = jsonObject.get("caption")?.asString
                        val thumbUrl = jsonObject.get("thumbnail_url")?.asString?.let { t ->
                            if (t.startsWith("http")) t else ApiDetails.APRIK_Kia_BASE_URL + t
                        }

                        // Derive a filename from URL if server doesn't send it separately
                        val fileName = rawUrl.substringAfterLast('/', missingDelimiterValue = "")

                        val mediaMessage = ChatMessage(
                                messageId = messageIdFromJson(jsonObject),
                                text = "",
                                isSender = false,
                                senderName = resolveDisplayName(jsonObject.get("sender_id")?.let { el -> if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) el.asInt else null }, jsonObject.get("username")?.asString),
                                senderUsername = jsonObject.get("username")?.asString,
                                senderId = senderId,
                                senderRoleAbbrev = resolveRoleAbbrevByUserId(senderId?.toIntOrNull()),
                                timeLabel = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date()).lowercase(),
                                type = msgType,
                                attachmentUri = fullUrl,
                                fileName = if (fileName.isNotBlank()) fileName else null,
                                caption = caption,
                                thumbnailUrl = thumbUrl
                            )
                        appendIncomingMessage(mediaMessage)
                        room?.roNumber?.let { ChatMediaStore.addOrUpdateMessage(it, mediaMessage) }

                        scrollToLast()
                        scheduleVisibleReadReceipt(120L)
                    }


                    "chat.typing" -> {
                        val userId = when (val el = jsonObject.get("user_id") ?: jsonObject.get("sender_id")) {
                            null -> null
                            else -> if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) el.asJsonPrimitive.asInt.toString() else el.asString
                        }
                        PresenceStore.setUserOnline(userId)
                        val currentUserId = PreferenceManager.getUserId()
                        if (userId != null && userId != currentUserId) {
                            val isTypingBroadcast = jsonObject.get("is_typing")?.asBoolean ?: false
                            if (isTypingBroadcast) {
                                val username = jsonObject.get("username")?.asString
                                val displayName = resolveDisplayName(userId?.toIntOrNull(), username) ?: "Someone"
                                binding.layoutTypingIndicator?.visibility = View.VISIBLE
                                binding.txtTypingName?.text = displayName
                                binding.txtTypingInitial?.text = displayName.firstOrNull()?.uppercase() ?: "?"
                                binding.txtTypingIndicator?.text = "is typing..."
                            } else {
                                binding.layoutTypingIndicator?.visibility = View.GONE
                            }
                        } else {
                            binding.layoutTypingIndicator?.visibility = View.GONE
                        }
                    }

                    "chat.read" -> {
                        val msgId = messageIdFromJson(jsonObject) ?: ""
                        messageAdapter?.updateMessageStatus(msgId, MessageStatus.READ)
                    }

                    "user.presence" -> {
                        val userId = when (val el = jsonObject.get("user_id")) {
                            null -> null
                            else -> if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) el.asJsonPrimitive.asInt.toString() else el.asString
                        }
                        when (jsonObject.get("status")?.asString?.lowercase()) {
                            "online" -> PresenceStore.setUserOnline(userId)
                            "offline" -> PresenceStore.setUserOffline(userId)
                        }
                    }

                    // Real-time service lifecycle — update only pinned (lifecycle status); do NOT update txtStatusChip (chip = service status only)
                    "service.lifecycle" -> {
                        val newStatusLabel = jsonObject.get("status_label")?.asString?.takeIf { it.isNotBlank() }
                        val newNotes = jsonObject.get("notes")?.asString?.takeIf { it.isNotBlank() }
                        val previousStatusLabel = jsonObject.get("previous_status_label")?.asString?.takeIf { it.isNotBlank() }
                        val content = jsonObject.get("content")?.asString?.takeIf { it.isNotBlank() }

                        jobNotes = newNotes ?: jobNotes
                        statusLabel = newStatusLabel ?: statusLabel

                        val pinnedText =  buildPinnedLifecycleText(statusLabel)
                        Log.d(TAG,"service.lifecycle pinned text: $pinnedText")
                        // Pinned: lifecycle only; show on both phone and tablet when we have lifecycle data
                        binding.layoutPinnedStatus?.visibility =
                            if (pinnedText.isNotBlank()) View.VISIBLE else View.GONE
                        binding.txtPinnedLifecycle?.text = pinnedText


                        (supportFragmentManager.findFragmentById(R.id.FragmentContainer) as? RODetailsFragment)?.let { frag ->
                            frag.setJobNotes(jobNotes)
                            frag.setStatusLabel(statusLabel)
                        }
                    }

                    // Service status — update only txtStatusChip (service status) and RO details; do NOT update pinned (pinned = lifecycle only)
                    "service.status" -> {
                        val newStatusLabel = jsonObject.get("status_label")?.asString?.takeIf { it.isNotBlank() }
                        val newStatus = jsonObject.get("status")?.asString?.takeIf { it.isNotBlank() }
                        val newNotes = jsonObject.get("notes")?.asString?.takeIf { it.isNotBlank() }

                        jobNotes = newNotes ?: jobNotes
                        statusLabel = newStatusLabel ?: statusLabel

                        // Chip: service status only — prefer status_label, else raw status (e.g. CLOSED), formatted
                        binding.txtStatusChip?.let { tv ->
                            Log.d(TAG,"service.status::statusLabel: $statusLabel")

                            Log.d(TAG,"service.status::status: $jobNotes")
                            val chipText = newStatusLabel ?: newStatus?.replace('_', ' ') ?: statusLabel ?: tv.text
                            tv.text = chipText
                            tv.visibility = if (chipText.isNotBlank()) View.VISIBLE else View.GONE
                        }
                        binding.txtLeftStatus?.let { tv ->
                            Log.d(TAG,"txtLeftStatus service.status::statusLabel: $statusLabel")

                            Log.d(TAG,"txtLeftStatus service.status::status: $jobNotes")
                            val chipText = newStatusLabel ?: newStatus?.replace('_', ' ') ?: statusLabel ?: tv.text
                            tv.text = chipText
                            tv.visibility = if (chipText.isNotBlank()) View.VISIBLE else View.GONE
                        }

                        (supportFragmentManager.findFragmentById(R.id.FragmentContainer) as? RODetailsFragment)?.let { frag ->
                            frag.setJobNotes(jobNotes)
                            frag.setStatusLabel(statusLabel)
                        }
                    }

                    // RO number updated for this appointment
                    "ro.number.updated" -> {
                        // Expected payload (example):
                        // { "type": "ro.number.updated", "ro_number": "RO-2026-12345" }
                        val newRoNumber = jsonObject.get("ro_number")?.asString
                            ?.takeIf { it.isNotBlank() }

                        if (!newRoNumber.isNullOrBlank()) {
                            // Update in-memory room model so future intents use latest RO
                            room = room?.copy(roNumberDisplay = newRoNumber)

                            // Update header title: "RO-XXXX | <subject>"
                            val titlePrefix = "$newRoNumber | "
                            binding.txtRoomTitle?.text = titlePrefix + (room?.subject ?: "")

                            // Update RO details fragment on tablet
                            (supportFragmentManager.findFragmentById(R.id.FragmentContainer) as? RODetailsFragment)
                                ?.setRoNumber(newRoNumber)
                        }
                    }

                    // Generic error event from backend
                    "error" -> {
                        val errorMessage = jsonObject.get("message")?.asString
                            ?: "Unexpected error from server"
                        Log.e(TAG, "Server error event: $errorMessage")
                        Toast.makeText(this@VirtualChatRoomActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("VirtualChatRoom", "Error parsing message: ${e.message}")
            }
        }
    }

    override fun onDisconnected(reason: String) {
        runOnUiThread {
            Log.d(TAG, "WebSocket Disconnected: $reason")
            shouldRefetchMessagesOnReconnect = true
            if (isNetworkAvailable) {
                socketRecoveryBannerPending = true
                showNetworkErrorBanner()
                WebSocketManager.getInstance().reconnectNow()
                if (SystemClock.elapsedRealtime() >= suppressSocketToastUntilMs) {
                    showWebSocketDisconnectedToast("Chat connection lost")
                }
            }
        }
    }

    override fun onError(error: String) {
        runOnUiThread {
            Log.e("VirtualChatRoom", "WebSocket Error: $error")
            shouldRefetchMessagesOnReconnect = true
            socketRecoveryBannerPending = true
            showNetworkErrorBanner()
            if (isNetworkAvailable && SystemClock.elapsedRealtime() >= suppressSocketToastUntilMs) {
                showWebSocketDisconnectedToast(
                    if (error.contains("not connected", ignoreCase = true)) {
                        "WebSocket disconnected. Reconnecting..."
                    } else {
                        "Chat connection lost"
                    }
                )
            }
        }
    }

    private fun showPreviewBottomSheet(selectedFiles: List<Pair<File, ChatMessageType>>) {
        previewSelectedFiles.clear()
        previewSelectedFiles.addAll(selectedFiles)
        showPreviewInPlace()
    }

    private fun showPreviewInPlace() {
        binding.layoutPreviewContainer.visibility = View.VISIBLE
        binding.cardViewBottomChat?.visibility = View.GONE
        binding.layoutTypingIndicator?.visibility = View.GONE

        val previewRoot = binding.layoutPreviewContainer.getChildAt(0)
        val edtCaption: EditText = previewRoot.findViewById(R.id.edtPreviewCaption)
        val btnSend: View = previewRoot.findViewById(R.id.btnPreviewSend)
        val btnCancel: View = previewRoot.findViewById(R.id.btnPreviewCancel)
        val txtTitle: TextView = previewRoot.findViewById(R.id.txtPreviewTitle)
        txtTitle.text = if (previewSelectedFiles.size <= 1) "Attach" else "Attach (${previewSelectedFiles.size})"
        if (edtCaption.text.isNullOrEmpty()) {
            edtCaption.setText("")
        }
        renderPreviewSelection(previewRoot)

        btnCancel.setOnClickListener {
            closePreviewPanel()
        }
        btnSend.setOnClickListener {
            val caption = edtCaption.text.toString()
            val selectedFiles = previewSelectedFiles.toList()
            closePreviewPanel()
            val groupId = "grp_${System.currentTimeMillis()}"
            selectedFiles.forEachIndexed { index, (file, type) ->
                val timeLabel = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date()).lowercase()
                val localId = "local_${System.currentTimeMillis()}_${file.name}"
                messageAdapter?.addMessage(
                    ChatMessage(
                        messageId = localId,
                        text = "",
                        isSender = true,
                        timeLabel = timeLabel,
                        type = type,
                        attachmentUri = file.absolutePath,
                        // Multi-select: show caption on first item only (like WhatsApp group). Single-select: show caption on the item.
                        caption = if (selectedFiles.size == 1) caption else if (index == 0) caption else null,
                        status = MessageStatus.SENDING,
                        fileName = file.name,
                        groupId = groupId,
                        uploadProgressPercent = 0
                    )
                )
                scrollToLast()
                lifecycleScope.launch {
                    val fileToUpload = if (type == ChatMessageType.IMAGE) {
                        try { compressImage(file) } catch (e: Exception) { file }
                    } else file
                    // Multi-select: send caption with first upload only (server still receives one caption).
                    val captionToSend = if (selectedFiles.size == 1) caption else if (index == 0) caption else ""
                    performUpload(fileToUpload, captionToSend, type, localId)
                }
            }
        }
    }

    private fun renderPreviewSelection(previewRoot: View) {
        val itemsContainer = previewRoot.findViewById<LinearLayout>(R.id.layoutPreviewItems)
        val txtTitle = previewRoot.findViewById<TextView>(R.id.txtPreviewTitle)
        itemsContainer.removeAllViews()
        txtTitle.text = if (previewSelectedFiles.size <= 1) "Attach" else "Attach (${previewSelectedFiles.size})"

        previewSelectedFiles.forEachIndexed { index, (file, type) ->
            val itemView = layoutInflater.inflate(R.layout.vc_item_preview_attachment, itemsContainer, false)
            val imgThumb = itemView.findViewById<ImageView>(R.id.imgPreviewThumb)
            val imgVideo = itemView.findViewById<ImageView>(R.id.imgPreviewVideo)
            val btnRemove = itemView.findViewById<ImageView>(R.id.btnRemovePreview)
            bindPreviewThumbnail(file, type, imgThumb, imgVideo)
            btnRemove.setOnClickListener {
                if (index in previewSelectedFiles.indices) {
                    previewSelectedFiles.removeAt(index)
                    if (previewSelectedFiles.isEmpty()) {
                        closePreviewPanel()
                    } else {
                        renderPreviewSelection(previewRoot)
                    }
                }
            }
            itemsContainer.addView(itemView)
        }

        itemsContainer.addView(createAddMorePreviewView())
    }

    private fun bindPreviewThumbnail(
        file: File,
        type: ChatMessageType,
        imgThumb: ImageView,
        imgVideo: ImageView
    ) {
        imgVideo.visibility = View.GONE
        when (type) {
            ChatMessageType.IMAGE -> imgThumb.setImageURI(Uri.fromFile(file))
            ChatMessageType.VIDEO -> {
                imgVideo.visibility = View.VISIBLE
                lifecycleScope.launch {
                    val bitmap = withContext(Dispatchers.IO) {
                        try {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(file.absolutePath)
                            val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            retriever.release()
                            frame
                        } catch (_: Exception) {
                            null
                        }
                    }
                    runOnUiThread {
                        if (bitmap != null) {
                            imgThumb.setImageBitmap(bitmap)
                        } else {
                            imgThumb.setImageResource(android.R.drawable.ic_media_play)
                        }
                    }
                }
            }
            ChatMessageType.FILE -> {
                imgThumb.setImageResource(R.drawable.file_pdf_icon)
            }
            else -> imgThumb.setImageResource(R.drawable.file_pdf_icon)
        }
    }

    private fun createAddMorePreviewView(): View {
        val container = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(72), dp(72)).apply {
                marginEnd = dp(8)
            }
            background = ContextCompat.getDrawable(this@VirtualChatRoomActivity, R.drawable.bg_comment_box)
            foreground = ContextCompat.getDrawable(this@VirtualChatRoomActivity, android.R.drawable.list_selector_background)
            setOnClickListener { launchAttachmentPicker() }
        }
        val plus = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER)
            setImageResource(android.R.drawable.ic_input_add)
            contentDescription = "Add more files"
        }
        container.addView(plus)
        return container
    }

    private fun closePreviewPanel() {
        val previewRoot = binding.layoutPreviewContainer.getChildAt(0)
        previewRoot?.findViewById<EditText>(R.id.edtPreviewCaption)?.setText("")
        previewSelectedFiles.clear()
        binding.layoutPreviewContainer.visibility = View.GONE
        binding.cardViewBottomChat?.visibility = View.VISIBLE
    }

    private fun appendFilesToPreview(newFiles: List<Pair<File, ChatMessageType>>) {
        if (newFiles.isEmpty()) return
        previewSelectedFiles.addAll(newFiles)
        if (binding.layoutPreviewContainer.visibility == View.VISIBLE) {
            renderPreviewSelection(binding.layoutPreviewContainer.getChildAt(0))
        } else {
            showPreviewBottomSheet(previewSelectedFiles.toList())
        }
    }

    private fun launchAttachmentPicker() {
        fileLauncher.launch(arrayOf("image/*", "video/*", "application/pdf", "*/*"))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun renderPdfFirstPage(file: File): Bitmap? {
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount == 0) null else {
                        renderer.openPage(0).use { page ->
                            val bitmap = Bitmap.createBitmap(
                                page.width * 2,
                                page.height * 2,
                                Bitmap.Config.ARGB_8888
                            )
                            page.render(
                                bitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )
                            bitmap
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VirtualChatRoom", "PDF first page: ${e.message}")
            null
        }
    }


    private suspend fun compressImage(file: File): File = withContext(Dispatchers.IO) {

        var bitmap = BitmapFactory.decodeFile(file.absolutePath)

        val exif = ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val matrix = android.graphics.Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        bitmap = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)

        val compressedFile = File(cacheDir, file.name)
        FileOutputStream(compressedFile).use {
            it.write(out.toByteArray())
        }

        compressedFile
    }

    private fun performUpload(file: File, caption: String, type: ChatMessageType, localId: String) {
        val slug = room?.roNumber ?: return
        val token = PreferenceManager.getAccessToken() ?: return

        val mimeType = when (type) {
            ChatMessageType.IMAGE -> "image/jpeg"
            ChatMessageType.VIDEO -> "video/mp4"
            ChatMessageType.VOICE_NOTE -> when (file.extension.lowercase()) {
                "m4a" -> "audio/mp4"
                "3gp" -> "audio/3gpp"
                "aac" -> "audio/aac"
                "wav" -> "audio/wav"
                else -> "audio/*"
            }
            ChatMessageType.FILE -> when (file.extension.lowercase()) {
                "pdf" -> "application/pdf"
                "doc", "docx" -> "application/msword"
                "xls", "xlsx" -> "application/vnd.ms-excel"
                else -> "application/octet-stream"
            }
            else -> "application/octet-stream"
        }

        val requestFile = ProgressRequestBody(
            file = file,
            contentType = mimeType.toMediaTypeOrNull()
        ) { written, total ->
            val percent = ((written * 100L) / total.coerceAtLeast(1L)).toInt()
            runOnUiThread {
                messageAdapter?.updateUploadProgress(localId, percent)
            }
        }
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val captionPart = caption.toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull())

        lifecycleScope.launch {
            try {
                val response = apiService.uploadFile("Bearer $token", slug, body, captionPart)
                if (response.isSuccessful && response.body() != null) {
                    val fileResponse = response.body()!!
                    Log.d(TAG, "Upload API response for $slug: ${Gson().toJson(fileResponse)}")
                    
                    val fullFileUrl = if (fileResponse.attachment.fileUrl.startsWith("http")) {
                        fileResponse.attachment.fileUrl
                    } else {
                        ApiDetails.APRIK_Kia_BASE_URL + fileResponse.attachment.fileUrl
                    }

                    // Caption should only contain user-entered text; leave blank if none
                    val captionText = if (caption.isNotBlank()) caption else ""

                    runOnUiThread {
                        val serverId = fileResponse.messageId.toString()
                        val thumbUrl = fileResponse.attachment.thumbnailUrl?.let { t ->
                            if (t.startsWith("http")) t else ApiDetails.APRIK_Kia_BASE_URL + t
                        }
                        val sent = sendMediaMessageOverWebSocket(
                            messageId = serverId,
                            type = type,
                            fileUrl = fullFileUrl,
                            caption = captionText,
                            thumbnailUrl = thumbUrl
                        )
                        messageAdapter?.updateMessageAttachmentUrl(localId, fullFileUrl, thumbUrl)
                        messageAdapter?.updateMessageId(localId, serverId)
                        messageAdapter?.updateMessageStatus(
                            serverId,
                            if (sent) MessageStatus.SENT else MessageStatus.ERROR
                        )
                        if (sent) {
                            room?.roNumber?.let {
                                ChatMediaStore.addOrUpdateMessage(
                                    it,
                                    ChatMessage(
                                        messageId = serverId,
                                        text = "",
                                        isSender = true,
                                        timeLabel = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date()).lowercase(),
                                        status = MessageStatus.SENT,
                                        type = type,
                                        attachmentUri = fullFileUrl,
                                        fileName = file.name,
                                        caption = caption,
                                        thumbnailUrl = thumbUrl,
                                        mimeType = mimeType
                                    )
                                )
                            }
                            hideNetworkErrorBanner()
                        } else {
                            showNetworkErrorBanner()
                        }
                    }
                } else {

                    val errorBody = response.errorBody()?.string()

                    Log.e("UploadDebug", "Code: ${response.code()}")
                    Log.e("UploadDebug", "ErrorBody: $errorBody")

                    runOnUiThread {
                        messageAdapter?.updateMessageStatus(localId, MessageStatus.ERROR)
                        showNetworkErrorBanner()
                    }

                    Toast.makeText(
                        this@VirtualChatRoomActivity,
                        "Upload failed: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    messageAdapter?.updateMessageStatus(localId, MessageStatus.ERROR)
                    showNetworkErrorBanner()
                }
                Log.e("VirtualChatRoom", "Upload Error: ${e.message}")
            }
        }
    }

    private fun sendTypingStatus(typing: Boolean) {
        if (isTyping == typing) return
        isTyping = typing
        val json = JsonObject()
        json.addProperty("type", "chat.typing")
        json.addProperty("is_typing", typing)
        WebSocketManager.getInstance().sendMessage(json.toString())
    }

    private fun sendReadReceipt(messageId: String) {
        if (messageId.isEmpty()) return
        val idInt = messageId.toIntOrNull() ?: return
        if (sentReadReceiptMessageIds.contains(idInt)) return
        val jsonObject = JsonObject()
        jsonObject.addProperty("type", "chat.read")
        jsonObject.addProperty("message_id", idInt)
        if (WebSocketManager.getInstance().sendMessage(jsonObject.toString())) {
            sentReadReceiptMessageIds.add(idInt)
            pendingReadReceiptMessageIds.remove(idInt)
            markMessageAsReadLocally(idInt)
        } else {
            pendingReadReceiptMessageIds.add(idInt)
        }
    }

    private fun scheduleVisibleReadReceipt(delayMs: Long = 0L) {
        binding.recyclerMessages.removeCallbacks(visibleReadReceiptRunnable)
        binding.recyclerMessages.postDelayed(visibleReadReceiptRunnable, delayMs)
    }

    private val visibleReadReceiptRunnable = Runnable {
        sendReadReceiptsForVisibleIncomingMessages()
    }

    private fun sendReadReceiptsForVisibleIncomingMessages() {
        val layoutManager = binding.recyclerMessages.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) return

        val visibleUnreadMessageIds = linkedSetOf<Int>()
        for (index in firstVisible..lastVisible) {
            val message = messages.getOrNull(index) ?: continue
            if (message.type == ChatMessageType.DATE_HEADER || message.isSender) continue
            val idInt = message.messageId?.toIntOrNull() ?: continue
            if (message.status == MessageStatus.READ) continue
            if (sentReadReceiptMessageIds.contains(idInt)) continue
            visibleUnreadMessageIds.add(idInt)
        }
        visibleUnreadMessageIds.forEach { visibleId ->
            sendReadReceipt(visibleId.toString())
        }
    }

    private fun markMessageAsReadLocally(messageId: Int) {
        val index = messages.indexOfFirst { it.messageId == messageId.toString() }
        if (index == -1) return
        val message = messages[index]
        if (message.isSender || message.type == ChatMessageType.DATE_HEADER || message.status == MessageStatus.READ) return
        message.status = MessageStatus.READ
    }

    private fun sendMediaMessageOverWebSocket(
        messageId: String,
        type: ChatMessageType,
        fileUrl: String,
        caption: String,
        thumbnailUrl: String? = null
    ): Boolean {
        val idInt = messageId.toIntOrNull() ?: return false
        val json = JsonObject().apply {
            addProperty("type", "chat.media")
            addProperty("message_id", idInt)
            addProperty(
                "message_type",
                when (type) {
                    ChatMessageType.IMAGE -> "image"
                    ChatMessageType.VIDEO -> "video"
                    ChatMessageType.VOICE_NOTE -> "audio"
                    ChatMessageType.FILE -> "document"
                    else -> "document"
                }
            )
            addProperty("file_url", fileUrl)
            addProperty("caption", caption)
            thumbnailUrl?.takeIf { it.isNotBlank() }?.let { addProperty("thumbnail_url", it) }
        }
        return WebSocketManager.getInstance().sendMessage(json.toString())
    }

    private fun retryOutgoingMessage(message: ChatMessage) {
        val type = message.type
        val caption = message.caption ?: ""
        val messageId = message.messageId ?: return
        val attachmentUri = message.attachmentUri

        messageAdapter?.updateMessageStatus(messageId, MessageStatus.SENDING)

        if (type == ChatMessageType.TEXT) {
            val sent = sendWebSocketMessage(message.text)
            messageAdapter?.updateMessageStatus(
                messageId,
                if (sent) MessageStatus.SENDING else MessageStatus.ERROR
            )
            if (sent) hideNetworkErrorBanner() else showNetworkErrorBanner()
            return
        }

        if (attachmentUri.isNullOrBlank()) return
        if (attachmentUri.startsWith("http") && messageId.toIntOrNull() != null) {
            val sent = sendMediaMessageOverWebSocket(
                messageId = messageId,
                type = type,
                fileUrl = attachmentUri,
                caption = caption,
                thumbnailUrl = message.thumbnailUrl
            )
            messageAdapter?.updateMessageStatus(
                messageId,
                if (sent) MessageStatus.SENT else MessageStatus.ERROR
            )
            if (sent) hideNetworkErrorBanner() else showNetworkErrorBanner()
        } else {
            val file = File(attachmentUri)
            lifecycleScope.launch {
                val fileToUpload = if (type == ChatMessageType.IMAGE) {
                    try {
                        compressImage(file)
                    } catch (e: Exception) {
                        file
                    }
                } else file
                performUpload(fileToUpload, caption, type, messageId)
            }
        }
    }

    private fun messageIdFromJson(json: JsonObject): String? {
        val el = json.get("message_id") ?: return null
        if (!el.isJsonPrimitive) return null
        val p = el.asJsonPrimitive
        return if (p.isNumber) p.asInt.toString() else p.asString
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
        binding.repairordertv?.setOnClickListener { finish() }
        val titleView = binding.txtRoomTitle
        titleView?.text = when (currentRole) {
            UserRole.CUSTOMER -> getString(R.string.vc_title_virtual_chat_room_customer)
            UserRole.SERVICE_ADVISOR -> getString(R.string.vc_title_virtual_chat_room_service_advisor)
            UserRole.MANAGER -> getString(R.string.vc_title_virtual_chat_room_manager)
        }
    }

    private fun bindStaticPhoneHeader() {
        val room = room ?: return
//        binding.txtRoomTitle?.text = "${room.roNumber} | ${room.subject}"
        binding.txtRoomTitle?.text = " ${room.subject}"

    }

    private fun bindStaticTabletPanels() {
        val room = room ?: return
        binding.txtLeftCustomerName?.text = room.customerName
        binding.txtLeftRoNumber?.text = room.roNumber
        binding.txtLeftStatus?.text = room.status
//        binding.txtLeftCustomerName?.text= PreferenceManager.getName()

        Log.d(TAG, "bindStaticTabletPanels:  ${PreferenceManager.getName()}")

        val initial = room.customerName
            ?.trim()
            ?.firstOrNull()
            ?.toString()
            ?.uppercase()
         binding.txtInitial?.text=initial
    }

    private fun setupMessageList() {
        binding.recyclerMessages.layoutManager =
            LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.recyclerMessages.setHasFixedSize(false)
        binding.recyclerMessages.setItemViewCacheSize(20)
//        binding.recyclerMessages.setHasStableIds(true)
        messageAdapter = VirtualChatMessageAdapter(
            messages,
            onRetryClick = { message -> retryOutgoingMessage(message) },
            onItemClick = { message -> handleAttachmentClick(message) },
            onSaveMedia = { message -> downloadAndSaveMedia(message) },
            estimationListener = this
        )
        binding.recyclerMessages.adapter = messageAdapter
        messageAdapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (!suppressAutoScroll) {
                    scrollToLast()
                    scheduleVisibleReadReceipt(120L)
                }
            }

            override fun onChanged() {
                if (!suppressAutoScroll) {
                    scrollToLast()
                    scheduleVisibleReadReceipt(120L)
                }
            }
        })
        binding.recyclerMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy >= 0) return
                if (isLoadingOlderMessages || !hasMoreOlderMessages) return
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (layoutManager.findFirstVisibleItemPosition() <= 2) {
                    fetchOlderMessages()
                }
                if (dy > 0) {
                    scheduleVisibleReadReceipt(80L)
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    scheduleVisibleReadReceipt(50L)
                }
            }
        })
    }

    private fun fetchOlderMessages() {
        val beforeId = oldestLoadedMessageId ?: return
        if (beforeId <= 0) {
            hasMoreOlderMessages = false
            return
        }
        fetchMessages(beforeMessageId = beforeId, isPagination = true)
    }

    private fun handleAttachmentClick(message: ChatMessage) {
        val rawUrl = message.attachmentUri ?: return
        val fullUrl = if (rawUrl.startsWith("http")) rawUrl else ApiDetails.APRIK_Kia_BASE_URL + rawUrl
        val intent = Intent(this, MediaViewerActivity::class.java).apply {
            putExtra(MediaViewerActivity.EXTRA_URL, fullUrl)
            putExtra(MediaViewerActivity.EXTRA_TYPE, message.type.name)
            putExtra(MediaViewerActivity.EXTRA_FILE_NAME, message.fileName)
        }
        startActivity(intent)
    }

    private val requestWriteStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pendingSaveMessage?.let { doSaveMedia(it) }
            } else {
                Toast.makeText(this, "Storage permission required to save files", Toast.LENGTH_LONG).show()
            }
            pendingSaveMessage = null
        }

    private var pendingSaveMessage: ChatMessage? = null

    private fun downloadAndSaveMedia(message: ChatMessage) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val perm = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                pendingSaveMessage = message
                requestWriteStoragePermission.launch(perm)
                return
            }
        }
        doSaveMedia(message)
    }

    private fun doSaveMedia(message: ChatMessage) {
        val rawUrl = message.attachmentUri ?: run {
            Toast.makeText(this, "No file to save", Toast.LENGTH_SHORT).show()
            return
        }
        val rawName = message.fileName?.takeIf { it.isNotBlank() }
            ?: rawUrl.substringAfterLast('/').substringBefore('?').takeIf { it.isNotBlank() }
            ?: "download_${System.currentTimeMillis()}"
        val fileName = rawName.substringBefore('?').replace(Regex("[\\\\/:*?\"<>|]"), "_")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (!rawUrl.startsWith("http")) {
                    val localFile = File(rawUrl)
                    if (localFile.exists()) {
                        saveLocalFileToPublicStorage(localFile, fileName, message)
                    } else {
                        throw IOException("Local file not found: $rawUrl")
                    }
                } else {
                    downloadRemoteFileToPublicStorage(rawUrl, fileName, message)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VirtualChatRoomActivity, "Saved to gallery: $fileName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("VirtualChatRoom", "Save failed for $fileName: ${e.javaClass.simpleName} - ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VirtualChatRoomActivity, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun downloadRemoteFileToPublicStorage(
        fileUrl: String,
        fileName: String,
        message: ChatMessage
    ) {
        val token = PreferenceManager.getAccessToken()
        val request = Request.Builder()
            .url(fileUrl)
            .apply { if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token") }
            .build()
        val client = OkHttpClient()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Download failed: ${response.code}")
            }
            val body = response.body ?: throw IOException("Empty response body")
            body.byteStream().use { input ->
                saveInputStreamToPublicStorage(input, fileName, message)
            }
        }
    }

    private fun saveLocalFileToPublicStorage(
        sourceFile: File,
        fileName: String,
        message: ChatMessage
    ) {
        sourceFile.inputStream().use { input ->
            saveInputStreamToPublicStorage(input, fileName, message)
        }
    }

    private fun saveInputStreamToPublicStorage(
        inputStream: InputStream,
        fileName: String,
        message: ChatMessage
    ) {
        val mimeType = resolveMimeType(fileName, message)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = when (message.type) {
                ChatMessageType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                ChatMessageType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
            }
            val relativePath = when (message.type) {
                ChatMessageType.IMAGE -> "${Environment.DIRECTORY_PICTURES}/KiaKandid"
                ChatMessageType.VIDEO -> "${Environment.DIRECTORY_MOVIES}/KiaKandid"
                else -> "${Environment.DIRECTORY_DOWNLOADS}/KiaKandid"
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(collection, values)
                ?: throw IOException("Unable to create destination")
            contentResolver.openOutputStream(uri)?.use { output ->
                inputStream.copyTo(output)
            } ?: throw IOException("Unable to open destination")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
        } else {
            val directory = when (message.type) {
                ChatMessageType.IMAGE -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                ChatMessageType.VIDEO -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                else -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            }
            val targetDir = File(directory, "KiaKandid")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val targetFile = File(targetDir, fileName)
            FileOutputStream(targetFile).use { output ->
                inputStream.copyTo(output)
            }
            MediaScannerConnection.scanFile(
                this,
                arrayOf(targetFile.absolutePath),
                arrayOf(mimeType),
                null
            )
        }
    }

    private fun resolveMimeType(fileName: String, message: ChatMessage): String {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.getDefault())
        return when (message.type) {
            ChatMessageType.IMAGE -> when (extension) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }
            ChatMessageType.VIDEO -> when (extension) {
                "3gp" -> "video/3gpp"
                "mkv" -> "video/x-matroska"
                else -> "video/mp4"
            }
            ChatMessageType.FILE -> when (extension) {
                "pdf" -> "application/pdf"
                "doc" -> "application/msword"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "xls" -> "application/vnd.ms-excel"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                else -> "application/octet-stream"
            }
            ChatMessageType.VOICE_NOTE -> "audio/*"
            else -> "application/octet-stream"
        }
    }

    private fun setupQuickReplies() {
        val containerPhone: LinearLayout? = binding.quickReplyContainer
        val messageFieldTablet: EditText? = binding.edtMessageTablet
        val containerTablet: LinearLayout? = binding.quickReplyContainerTablet
        initQuickReplyContainer(containerPhone, messageFieldTablet)
        initQuickReplyContainer(containerTablet, messageFieldTablet)
    }

    private fun initQuickReplyContainer(container: LinearLayout?, messageField: EditText?) {
        if (container == null || messageField == null) return

        container.removeAllViews()
        val inflater = LayoutInflater.from(this)
        quickReplies.forEach { text ->
            val chip = inflater.inflate(R.layout.vc_quick_reply_chip, container, false) as TextView
            chip.text = text
            chip.setOnClickListener {
                messageField.setText(text)
                messageField.setSelection(text.length)
            }

            container.addView(chip)
        }
    }

    private fun setupSendActions() {
        binding.edtMessageTablet.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrBlank()) sendTypingStatus(false)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                binding.recordLayout?.visibility = if (hasText) View.GONE else View.VISIBLE
                binding.sendLayout?.visibility = if (hasText) View.VISIBLE else View.GONE
                if (hasText) {
                    sendTypingStatus(true)
                    typingHandler.removeCallbacks(stopTypingRunnable)
                    typingHandler.postDelayed(stopTypingRunnable, 2000)
                }
            }
        })

        binding.imgSendTablet.setOnClickListener {
            val text = binding.edtMessageTablet.text.toString().trim()
            if (text.isNotEmpty()) {
                val localId = "local_txt_${System.currentTimeMillis()}"
                addMessage(text, true, localId)
                val sent = sendWebSocketMessage(text, localId)
                if (!sent) {
                    messageAdapter?.updateMessageStatus(localId, MessageStatus.ERROR)
                    showNetworkErrorBanner()
                }
                binding.edtMessageTablet.setText("")
                scrollToLast()
                sendTypingStatus(false)
            }
        }
    }

    private fun scrollToLast() {
        val recycler = binding.recyclerMessages
        val layoutManager = recycler.layoutManager as? LinearLayoutManager ?: return

        recycler.post {
            recycler.postDelayed({
                val position = messageAdapter?.itemCount?.minus(1) ?: 0
                if (position >= 0) {
                    layoutManager.scrollToPositionWithOffset(position, 0)
                }
            }, 50)
        }
    }

    private fun addMessage(
        text: String,
        isSender: Boolean,
        messageId: String? = null,
        senderName: String? = null,
        senderUsername: String? = null
    ) {
        if (TextUtils.isEmpty(text)) return
        if (hasMessage(messageId)) return
        val timeLabel = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date()).lowercase()
        messageAdapter?.addMessage(
            ChatMessage(
                messageId = messageId,
                text = text,
                isSender = isSender,
                senderName = senderName,
                senderUsername = senderUsername,
                timeLabel = timeLabel,
                status = if (isSender) MessageStatus.SENDING else MessageStatus.SENT
            )
        )
    }

    private fun sendWebSocketMessage(text: String, messageId: String? = null): Boolean {
        val json = JsonObject()
        json.addProperty("type", "chat.message")
        json.addProperty("content", text)
        // Only add message_id if it's a valid integer. 
        // If it starts with 'local_', it's a tracking ID, we might need to send it differently or not at all.
        // Assuming the server only wants integer for 'message_id' field.
        return WebSocketManager.getInstance().sendMessage(json.toString())
    }

    private fun retryPendingOutgoingMessages() {
        if (isRetryingPendingMessages) return
        val pendingMessages = messages.filter { message ->
            message.isSender &&
                message.type != ChatMessageType.DATE_HEADER &&
                (
                    message.status == MessageStatus.ERROR ||
                        (message.type == ChatMessageType.TEXT && message.status == MessageStatus.SENDING)
                    )
        }
        if (pendingMessages.isEmpty()) return

        isRetryingPendingMessages = true
        lifecycleScope.launch {
            try {
                pendingMessages.forEach { message ->
                    retryOutgoingMessage(message)
                    delay(200)
                }
            } finally {
                isRetryingPendingMessages = false
            }
        }
    }

    private fun showWebSocketDisconnectedToast(message: String) {
        Toast.makeText(this@VirtualChatRoomActivity, message, Toast.LENGTH_LONG).show()
    }

    private fun setupAttachmentAndMedia() {
        binding.imgAttachmentTablet.setOnClickListener { showAttachmentOptionsDialog() }
    }

    private fun showAttachmentOptionsDialog() {

        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.vc_dialog_attachment_options, null)

        val dialog = AlertDialog.Builder(this, R.style.FloatingDialogStyle)
            .setView(dialogView)
            .create()

        dialog.show()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)

            val params = attributes
            params.gravity = Gravity.BOTTOM or Gravity.END
            params.y = 140
            params.x = 20

            attributes = params

            setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        dialogView.findViewById<LinearLayout>(R.id.optionGallery)?.setOnClickListener {
            dialog.dismiss()
            launchAttachmentPicker()
        }

        dialogView.findViewById<LinearLayout>(R.id.optionCamera)?.setOnClickListener {
            dialog.dismiss()
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        dialogView.findViewById<LinearLayout>(R.id.estimation)?.setOnClickListener {
            dialog.dismiss()
            handleEstimationClick()
        }
    }

    override fun onPartCheckBoxClicked(parentPosition: Int, childPosition: Int, isSelected: Boolean, arrayList: ArrayList<Part>) {
        updatePartListData(parentPosition, childPosition, isSelected, arrayList)
        updateGrandTotalConsideringPartList(parentPosition, childPosition, isSelected, arrayList)
        
        val details = messages[parentPosition].estimationDetails ?: return
        details.areAllItemsSelected = details.labour_list.all { it.isSelected == "Y" } && details.part_list.all { it.isSelected == "Y" }
        
        messageAdapter?.notifyItemChanged(parentPosition)
    }

    override fun onLabourCheckboxClick(parentPosition: Int, childPosition: Int, isSelected: Boolean, arrayList: ArrayList<Labour>) {
        updateLabourListData(parentPosition, childPosition, isSelected, arrayList)
        updateGrandTotalConsideringLabourList(parentPosition, childPosition, isSelected, arrayList)
        
        val details = messages[parentPosition].estimationDetails ?: return
        details.areAllItemsSelected = details.labour_list.all { it.isSelected == "Y" } && details.part_list.all { it.isSelected == "Y" }
        
        messageAdapter?.notifyItemChanged(parentPosition)
    }

    private fun updatePartListData(parentPosition: Int, childPosition: Int, isSelected: Boolean, arrayList: ArrayList<Part>) {
        messages[parentPosition].estimationDetails!!.part_list[childPosition].isSelected = if (isSelected) "Y" else "N"
    }

    private fun updateLabourListData(parentPosition: Int, childPosition: Int, isSelected: Boolean, arrayList: ArrayList<Labour>) {
        messages[parentPosition].estimationDetails!!.labour_list[childPosition].isSelected = if (isSelected) "Y" else "N"
    }

    private fun updateGrandTotalConsideringPartList(parentPosition: Int, childPosition: Int, isSelected: Boolean, partList: ArrayList<Part>) {
        val details = messages[parentPosition].estimationDetails ?: return
        val itemPrice = details.part_list[childPosition].totalPrice.toDouble()
        if (isSelected) {
            details.selectedItemsTotal += itemPrice
        } else {
            details.selectedItemsTotal -= itemPrice
        }
        details.selectedItemsTotal = "%.2f".format(details.selectedItemsTotal).toDouble()
    }

    private fun updateGrandTotalConsideringLabourList(parentPosition: Int, childPosition: Int, isSelected: Boolean, labourList: ArrayList<Labour>) {
        val details = messages[parentPosition].estimationDetails ?: return
        val itemPrice = details.labour_list[childPosition].totalLabourCost.toDouble()
        if (isSelected) {
            details.selectedItemsTotal += itemPrice
        } else {
            details.selectedItemsTotal -= itemPrice
        }
        details.selectedItemsTotal = "%.2f".format(details.selectedItemsTotal).toDouble()
    }

    override fun onAcceptClicked(parentPosition: Int, estimationDetails: ResponseModelEstimateData) {
        sharedViewModel.isProgressBarVisible.value = true
        
        updateSelectedPartList(parentPosition, messages[parentPosition].estimationDetails!!.part_list)
        updateSelectedLabourList(parentPosition, messages[parentPosition].estimationDetails!!.labour_list)

        val approvedEstimateData = ResponseModelEstimateData(
            deferred_job_list = estimationDetails.deferred_job_list,
            estimationApprovalStatus = "Y",
            labour_list = ArrayList(estimationDetails.labour_list.filter { it.isSelected == "Y" }),
            part_list = ArrayList(estimationDetails.part_list.filter { it.isSelected == "Y" }),
            totalEstimate = estimationDetails.totalEstimate,
            totalLabourEstimate = estimationDetails.totalLabourEstimate,
            totalPartsEstimate = estimationDetails.totalPartsEstimate,
            selectedItemsTotal = estimationDetails.selectedItemsTotal
        )

        sharedViewModel.estimateDetailsAfterApproval = approvedEstimateData
        sharedViewModel.tempParentPosition = parentPosition
        sharedViewModel.updateEstimateStatus.value = true
    }

    override fun onRejectClicked(parentPosition: Int, estimationDetails: ResponseModelEstimateData) {
        sharedViewModel.isProgressBarVisible.value = true
        showDialogToConfirmEstimateRejection(parentPosition, estimationDetails)
    }

    private fun showDialogToConfirmEstimateRejection(parentPosition: Int, estimationDetails: ResponseModelEstimateData) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = LayoutUniversalDialogBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogBinding.root)
        dialogBinding.tvDialogMessage.text = "Are you sure you want to reject the estimation."
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(false)

        dialogBinding.tvDialogTitle.visibility = View.GONE
        dialogBinding.btnPositive.text = "Yes"
        dialogBinding.btnNegative.text = "No"

        dialogBinding.btnNegative.setOnClickListener {
            dialog.dismiss()
            sharedViewModel.isProgressBarVisible.value = false
        }
        dialogBinding.btnPositive.setOnClickListener {
            dialog.dismiss()
            val rejectedEstimateData = ResponseModelEstimateData(
                deferred_job_list = estimationDetails.deferred_job_list,
                estimationApprovalStatus = "N",
                labour_list = estimationDetails.labour_list,
                part_list = estimationDetails.part_list,
                totalEstimate = estimationDetails.totalEstimate,
                totalLabourEstimate = estimationDetails.totalLabourEstimate,
                totalPartsEstimate = estimationDetails.totalPartsEstimate,
                selectedItemsTotal = estimationDetails.selectedItemsTotal
            )
            sharedViewModel.estimateDetailsAfterApproval = rejectedEstimateData
            sharedViewModel.tempParentPosition = parentPosition
            sharedViewModel.updateEstimateStatus.value = true
        }
        dialog.show()
    }

    override fun onSelectAllClicked(parentPosition: Int, isSelected: Boolean, estimationDetails: ResponseModelEstimateData) {
        messages[parentPosition] = messages[parentPosition].copy(estimationDetails = updateEstimationListToSelectAllItems(estimationDetails, isSelected))
        messageAdapter?.notifyItemChanged(parentPosition)
    }

    private fun updateEstimationListToSelectAllItems(estimateData: ResponseModelEstimateData, isSelected: Boolean): ResponseModelEstimateData {
        val modifiedEstimateData = estimateData
        val status = if (isSelected) "Y" else "N"
        
        for (part in modifiedEstimateData.part_list) {
            part.isSelected = status
        }
        for (labour in modifiedEstimateData.labour_list) {
            labour.isSelected = status
        }

        if (isSelected) {
            modifiedEstimateData.selectedItemsTotal = modifiedEstimateData.totalEstimate
        } else {
            modifiedEstimateData.selectedItemsTotal = 0.0
        }
        modifiedEstimateData.areAllItemsSelected = isSelected
        return modifiedEstimateData
    }

    private fun updateSelectedPartList(parentPosition: Int, partList: ArrayList<Part>) {
        val selected = partList.filter { it.isSelected == "Y" }.map { it.partNumber }
        if (selected.isNotEmpty()) {
            sharedViewModel.selectedPartList = selected.joinToString(",")
        }
    }

    private fun updateSelectedLabourList(parentPosition: Int, labourList: ArrayList<Labour>) {
        val selected = labourList.filter { it.isSelected == "Y" }.map { it.labourCode }
        if (selected.isNotEmpty()) {
            sharedViewModel.selectedLabourList = selected.joinToString(",")
        }
    }

    private fun startPlayback(path: String) {

        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
            AudioTrack.MODE_STREAM
        )

        audioTrack?.play()
        isPlaying = true

        Thread {
            val file = File(path)
            val input = FileInputStream(file)

            val buffer = ByteArray(bufferSize)

            while (isPlaying) {
                val read = input.read(buffer)
                if (read <= 0) break
                audioTrack?.write(buffer, 0, read)
            }

            input.close()
            stopPlayback()

        }.start()
    }


}
