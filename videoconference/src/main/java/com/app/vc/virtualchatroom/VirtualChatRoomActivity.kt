package com.app.vc.virtualchatroom

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
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

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
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
import android.widget.ImageView
import android.widget.LinearLayout
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
import androidx.viewpager2.widget.ViewPager2
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
import com.app.vc.models.MessageModel
import com.app.vc.models.MessageStatusEnum
import com.app.vc.network.LoginApiService
import com.app.vc.utils.ApiDetails
import com.app.vc.utils.PreferenceManager
import com.app.vc.utils.VCConstants
import com.app.vc.virtualroomlist.UserRole
import com.app.vc.virtualroomlist.VirtualRoomUiModel
import com.app.vc.websocketconnection.WebSocketManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.app.vc.views.WaveformView
import com.kia.vc.message.Labour
import com.kia.vc.message.Part
import kotlinx.coroutines.Dispatchers
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
                showPreviewBottomSheet(listOf(file to ChatMessageType.IMAGE))
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
                showPreviewBottomSheet(selectedFiles)
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

        val roleFromIntent = intent.getStringExtra(EXTRA_ROLE)

        val roomStatus=intent.getStringExtra(STATUS)
        Log.d(TAG, "onCreate: $roomStatus")

        val roomJson = intent.getStringExtra(EXTRA_ROOM_JSON)
        if (roomJson != null) {
            room = Gson().fromJson(roomJson, VirtualRoomUiModel::class.java)
            jobNotes = room?.serviceNotes
            statusLabel = room?.lifecycleStatusLabel
        }

        binding.txtStatusChip?.text = room?.lifecycleStatusLabel?.takeIf { it.isNotBlank() }
            ?: roomStatus ?: room?.status ?: ""

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
        setupQuickReplies()
        setupSendActions()
        setupAttachmentAndMedia()
        setupVoiceNote()
        connectToWebSocket()
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

    override fun onDestroy() {
        WebSocketManager.getInstance().clearCallback()
        typingHandler.removeCallbacks(stopTypingRunnable)
        voiceTimerHandler.removeCallbacks(voiceTimerRunnable)
        amplitudeHandler.removeCallbacks(amplitudeRunnable)
        mediaRecorder?.release()
        mediaRecorder = null
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    private fun fetchQuickReplies() {
        val role = if (binding.tabParticipants != null) "service_person" else "customer"
        lifecycleScope.launch {
            try {
                val response = apiService.getQuickReplies(role)
                if (response.isSuccessful && response.body() != null) {
                    val apiReplies = response.body()!!.sortedBy { it.displayOrder }.map { it.text }
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

    private fun fetchMessages() {
        val slug = room?.roNumber ?: return
        val token = PreferenceManager.getAccessToken() ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getMessages("Bearer $token", slug)
                if (response.isSuccessful && response.body() != null) {
                    val currentUserId = PreferenceManager.getUserId()
                    val apiMessages = response.body()!!
                    
                    val chatMessages = apiMessages.map { apiMsg ->
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
                        val isRead = (apiMsg.receipts?.isNotEmpty() == true)
                        
                        ChatMessage(
                            messageId = apiMsg.id.toString(),
                            text = if (type == ChatMessageType.TEXT) apiMsg.content else "",
                            isSender = isSender,
                            timeLabel = formatApiDate(apiMsg.createdAt),
                            status = if (isRead) MessageStatus.READ else MessageStatus.SENT,
                            type = type,
                            attachmentUri = attachmentUri,
                            fileName = attachment?.fileName,
                            caption = if (type != ChatMessageType.TEXT) apiMsg.content else null,
                            thumbnailUrl = thumbUrl
                        )
                    }
                    
                    withContext(Dispatchers.Main) {
                        messages.clear()
                        messages.addAll(chatMessages)
                        messageAdapter?.notifyDataSetChanged()
                        scrollToLast()
                    }
                }
            } catch (e: Exception) {
                Log.e("VirtualChatRoom", "Error fetching messages: ${e.message}")
            }
        }
    }

    private var jobNotes: String? = null
    private var statusLabel: String? = null

    private fun fetchServiceLifecycle() {
        val slug = room?.roNumber ?: return
        val token = PreferenceManager.getAccessToken() ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getServiceLifecycleCurrent("Bearer $token", slug)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    jobNotes = body.notes?.takeIf { it.isNotBlank() }
                    statusLabel = body.statusLabel?.takeIf { it.isNotBlank() }
                    Log.d("VirtualChatRoom", "Service lifecycle response: $body")
                    withContext(Dispatchers.Main) {
                        binding.txtStatusChip?.let { tv ->
                            tv.visibility = View.VISIBLE
                            tv.text = statusLabel ?: jobNotes ?: ""
                        }
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

            loadFragment(MediaFragment())

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

    override fun onConnected() {
        runOnUiThread {
            Log.d("VirtualChatRoom", "WebSocket Connected Successfully")
            Toast.makeText(this, "Chat connected", Toast.LENGTH_SHORT).show()
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

                            messageAdapter?.addMessage(
                                ChatMessage(
                                    messageId = messageIdFromJson(jsonObject),
                                    text = "",
                                    isSender = false,
                                    timeLabel = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date()).lowercase(),
                                    type = msgType,
                                    attachmentUri = fullAttachmentUrl,
                                    fileName = fileName,
                                    caption = content,
                                    mimeType = mimeType
                                )
                            )
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
                            messageAdapter?.addMessage(
                                ChatMessage(
                                    messageId = messageIdFromJson(jsonObject),
                                    text = "",
                                    isSender = false,
                                    timeLabel = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date()).lowercase(),
                                    type = msgType,
                                    attachmentUri = fullUrl,
                                    fileName = if (fileName.isNotBlank()) fileName else null,
                                    caption = caption,
                                    thumbnailUrl = thumbUrl
                                )
                            )
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
                                addMessage(content, false, messageIdFromJson(jsonObject))
                            }
                        }
                        scrollToLast()
                        sendReadReceipt(messageIdFromJson(jsonObject) ?: "")
                    }

                    "chat.media" -> {
                        val senderId = jsonObject.get("sender_id")?.asString
                        val currentUserId = PreferenceManager.getUserId()

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

                        messageAdapter?.addMessage(
                            ChatMessage(
                                messageId = messageIdFromJson(jsonObject),
                                text = "",
                                isSender = false,
                                timeLabel = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date()).lowercase(),
                                type = msgType,
                                attachmentUri = fullUrl,
                                fileName = if (fileName.isNotBlank()) fileName else null,
                                caption = caption,
                                thumbnailUrl = thumbUrl
                            )
                        )

                        scrollToLast()
                        sendReadReceipt(messageIdFromJson(jsonObject) ?: "")
                    }


                    "chat.typing" -> {
                        val userId = when (val el = jsonObject.get("user_id") ?: jsonObject.get("sender_id")) {
                            null -> null
                            else -> if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) el.asJsonPrimitive.asInt.toString() else el.asString
                        }
                        val currentUserId = PreferenceManager.getUserId()
                        if (userId != null && userId != currentUserId) {
                            val isTypingBroadcast = jsonObject.get("is_typing")?.asBoolean ?: false
                            binding.txtTypingIndicator?.visibility =
                                if (isTypingBroadcast) View.VISIBLE else View.GONE
                            binding.txtTypingIndicator?.text =
                                "${jsonObject.get("username")?.asString ?: "Someone"} is typing..."
                        } else {
                            binding.txtTypingIndicator?.visibility = View.GONE
                        }
                    }

                    "chat.read" -> {
                        val msgId = messageIdFromJson(jsonObject) ?: ""
                        messageAdapter?.updateMessageStatus(msgId, MessageStatus.READ)
                    }
                }
            } catch (e: Exception) {
                Log.e("VirtualChatRoom", "Error parsing message: ${e.message}")
            }
        }
    }

    override fun onDisconnected(reason: String) {
        runOnUiThread { Log.d("VirtualChatRoom", "WebSocket Disconnected: $reason") }
    }

    override fun onError(error: String) {
        runOnUiThread { Log.e("VirtualChatRoom", "WebSocket Error: $error") }
    }

    private fun showPreviewBottomSheet(selectedFiles: List<Pair<File, ChatMessageType>>) {
        showPreviewInPlace(selectedFiles)
    }

    private fun showPreviewInPlace(selectedFiles: List<Pair<File, ChatMessageType>>) {
        binding.recyclerMessages.visibility = View.GONE
        binding.layoutPreviewContainer.visibility = View.VISIBLE
        binding.cardViewBottomChat?.visibility = View.GONE
        binding.txtTypingIndicator?.visibility = View.GONE

        val previewRoot = binding.layoutPreviewContainer.getChildAt(0)
        val viewPager: ViewPager2 = previewRoot.findViewById(R.id.viewPagerPreview)
        val edtCaption: EditText = previewRoot.findViewById(R.id.edtPreviewCaption)
        val btnSend: View = previewRoot.findViewById(R.id.btnPreviewSend)
        val btnCancel: View = previewRoot.findViewById(R.id.btnPreviewCancel)
        val txtTitle: TextView = previewRoot.findViewById(R.id.txtPreviewTitle)

        txtTitle.text = if (selectedFiles.size == 1) "Preview" else "Preview (${selectedFiles.size} files)"
        edtCaption.setText("")

        viewPager.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val itemView = layoutInflater.inflate(R.layout.vc_preview_page_media, parent, false)
                return object : RecyclerView.ViewHolder(itemView) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val (file, type) = selectedFiles[position]
                val imgMedia = holder.itemView.findViewById<ImageView>(R.id.imgPreviewMedia)
                val imgPlay = holder.itemView.findViewById<ImageView>(R.id.imgPreviewPlay)
                val pdfRow = holder.itemView.findViewById<View>(R.id.layoutPreviewPdfRow)
                val txtPdfName = holder.itemView.findViewById<TextView>(R.id.txtPreviewPdfName)
                val videoView = holder.itemView.findViewById<VideoView>(R.id.videoPreview)

                imgPlay.visibility = View.GONE
                pdfRow.visibility = View.GONE
                videoView.visibility = View.GONE
                imgMedia.visibility = View.VISIBLE

                when (type) {
                    ChatMessageType.IMAGE -> imgMedia.setImageURI(Uri.fromFile(file))
                    ChatMessageType.VIDEO -> {
                        imgPlay.visibility = View.VISIBLE
                        lifecycleScope.launch {
                            val bitmap = withContext(Dispatchers.IO) {
                                try {
                                    val r = MediaMetadataRetriever()
                                    r.setDataSource(file.absolutePath)
                                    val frame = r.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                    r.release()
                                    frame
                                } catch (e: Exception) { null }
                            }
                            bitmap?.let { runOnUiThread { imgMedia.setImageBitmap(it) } }
                                ?: runOnUiThread { imgMedia.setImageResource(android.R.drawable.ic_media_play) }
                        }
                        imgPlay.setOnClickListener {
                            imgMedia.visibility = View.GONE
                            imgPlay.visibility = View.GONE
                            videoView.visibility = View.VISIBLE
                            videoView.setVideoPath(file.absolutePath)
                            videoView.setOnCompletionListener {
                                videoView.visibility = View.GONE
                                imgMedia.visibility = View.VISIBLE
                                imgPlay.visibility = View.VISIBLE
                            }
                            videoView.start()
                        }
                    }
                    ChatMessageType.FILE -> {
                        pdfRow.visibility = View.VISIBLE
                        txtPdfName.text = file.name
                        if (file.extension.equals("pdf", ignoreCase = true)) {
                            lifecycleScope.launch {
                                val bitmap = withContext(Dispatchers.IO) { renderPdfFirstPage(file) }
                                bitmap?.let { runOnUiThread { imgMedia.setImageBitmap(it) } }
                                    ?: runOnUiThread { imgMedia.setImageResource(android.R.drawable.ic_menu_save) }
                            }
                        } else {
                            imgMedia.setImageResource(android.R.drawable.ic_menu_save)
                        }
                    }
                    else -> imgMedia.setImageResource(android.R.drawable.ic_menu_save)
                }
            }

            override fun getItemCount() = selectedFiles.size
        }

        btnCancel.setOnClickListener {
            binding.layoutPreviewContainer.visibility = View.GONE
            binding.recyclerMessages.visibility = View.VISIBLE
            binding.cardViewBottomChat?.visibility = View.VISIBLE
        }
        btnSend.setOnClickListener {
            val caption = edtCaption.text.toString()
            binding.layoutPreviewContainer.visibility = View.GONE
            binding.recyclerMessages.visibility = View.VISIBLE
            binding.cardViewBottomChat?.visibility = View.VISIBLE
            selectedFiles.forEach { (file, type) ->
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
                        caption = if (selectedFiles.size == 1) caption else null,
                        status = MessageStatus.SENDING,
                        fileName = file.name
                    )
                )
                scrollToLast()
                lifecycleScope.launch {
                    val fileToUpload = if (type == ChatMessageType.IMAGE) {
                        try { compressImage(file) } catch (e: Exception) { file }
                    } else file
                    performUpload(fileToUpload, if (selectedFiles.size == 1) caption else "", type, localId)
                }
            }
        }
    }

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
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
//        val compressedFile = File(cacheDir, "compressed_${file.name}")
        val compressedFile = File(cacheDir, file.name)
        FileOutputStream(compressedFile).use { it.write(out.toByteArray()) }
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

        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val captionPart = caption.toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull())

        lifecycleScope.launch {
            try {
                val response = apiService.uploadFile("Bearer $token", slug, body, captionPart)
                if (response.isSuccessful && response.body() != null) {
                    val fileResponse = response.body()!!
                    
                    val fullFileUrl = if (fileResponse.attachment.fileUrl.startsWith("http")) {
                        fileResponse.attachment.fileUrl
                    } else {
                        ApiDetails.APRIK_Kia_BASE_URL + fileResponse.attachment.fileUrl
                    }

                    // Build chat.media payload as per server spec
                    val json = JsonObject()
                    json.addProperty("type", "chat.media")
                    json.addProperty("message_id", fileResponse.messageId)
                    // Prefer server-provided messageType, otherwise derive from our ChatMessageType
                    val messageTypeStr = when {
                        !fileResponse.messageType.isNullOrBlank() -> fileResponse.messageType
                        else -> when (type) {
                            ChatMessageType.IMAGE -> "image"
                            ChatMessageType.VIDEO -> "video"
                            ChatMessageType.VOICE_NOTE -> "audio"
                            ChatMessageType.FILE -> "document"
                            else -> "document"
                        }
                    }
                    json.addProperty("message_type", messageTypeStr)

                    json.addProperty("file_url", fullFileUrl)
                    fileResponse.attachment.thumbnailUrl?.let { thumb ->
                        if (thumb.isNotBlank()) {
                            val fullThumbUrl =
                                if (thumb.startsWith("http")) thumb else ApiDetails.APRIK_Kia_BASE_URL + thumb
                            json.addProperty("thumbnail_url", fullThumbUrl)
                        }
                    }

                    // Caption should only contain user-entered text; leave blank if none
                    val captionText = if (caption.isNotBlank()) caption else ""
                    json.addProperty("caption", captionText)

                    runOnUiThread {
                        WebSocketManager.getInstance().sendMessage(json.toString())
                    }

                    runOnUiThread {
                        messageAdapter?.updateMessageId(localId, fileResponse.messageId.toString())
                        val thumbUrl = fileResponse.attachment.thumbnailUrl?.let { t ->
                            if (t.startsWith("http")) t else ApiDetails.APRIK_Kia_BASE_URL + t
                        }
                        messageAdapter?.updateMessageAttachmentUrl(localId, fullFileUrl, thumbUrl)
                        messageAdapter?.updateMessageStatus(
                            localId,
                            MessageStatus.SENT
                        )
                    }
                } else {

                    val errorBody = response.errorBody()?.string()

                    Log.e("UploadDebug", "Code: ${response.code()}")
                    Log.e("UploadDebug", "ErrorBody: $errorBody")

                    runOnUiThread {
                        messageAdapter?.updateMessageStatus(localId, MessageStatus.ERROR)
                    }

                    Toast.makeText(
                        this@VirtualChatRoomActivity,
                        "Upload failed: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread { messageAdapter?.updateMessageStatus(localId, MessageStatus.ERROR) }
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
        val jsonObject = JsonObject()
        jsonObject.addProperty("type", "chat.read")
        jsonObject.addProperty("message_id", idInt)
        WebSocketManager.getInstance().sendMessage(jsonObject.toString())
    }

    private fun messageIdFromJson(json: JsonObject): String? {
        val el = json.get("message_id") ?: return null
        if (!el.isJsonPrimitive) return null
        val p = el.asJsonPrimitive
        return if (p.isNumber) p.asInt.toString() else p.asString
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
        val titleView = binding.txtRoomTitle
        titleView?.text = when (currentRole) {
            UserRole.CUSTOMER -> getString(R.string.vc_title_virtual_chat_room_customer)
            UserRole.SERVICE_ADVISOR -> getString(R.string.vc_title_virtual_chat_room_service_advisor)
            UserRole.MANAGER -> getString(R.string.vc_title_virtual_chat_room_manager)
        }
    }

    private fun bindStaticPhoneHeader() {
        val room = room ?: return
        binding.txtRoomTitle?.text = "${room.roNumber} | ${room.subject}"
    }

    private fun bindStaticTabletPanels() {
        val room = room ?: return
        binding.txtLeftCustomerName?.text = room.customerName
        binding.txtLeftRoNumber?.text = room.roNumber
        binding.txtLeftStatus?.text = room.status
    }

    private fun setupMessageList() {
        binding.recyclerMessages.layoutManager =
            LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.recyclerMessages.setHasFixedSize(false)
        binding.recyclerMessages.setItemViewCacheSize(20)
//        binding.recyclerMessages.setHasStableIds(true)
        messageAdapter = VirtualChatMessageAdapter(
            messages,
            onRetryClick = { message ->
                // Retry upload logic
                val type = message.type
                val file = File(message.attachmentUri!!)
                val caption = message.caption ?: ""
                val localId = message.messageId!!
                messageAdapter?.updateMessageStatus(localId, MessageStatus.SENDING)
                lifecycleScope.launch {
                    val fileToUpload = if (type == ChatMessageType.IMAGE) {
                        try {
                            compressImage(file)
                        } catch (e: Exception) {
                            file
                        }
                    } else file
                    performUpload(fileToUpload, caption, type, localId)
                }
            },
            onItemClick = { message -> handleAttachmentClick(message) },
            onSaveMedia = { message -> downloadAndSaveMedia(message) },
            estimationListener = this
        )
        binding.recyclerMessages.adapter = messageAdapter
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

    private fun downloadAndSaveMedia(message: ChatMessage) {
        val rawUrl = message.attachmentUri ?: run {
            Toast.makeText(this, "No file to save", Toast.LENGTH_SHORT).show()
            return
        }
        val fullUrl = if (rawUrl.startsWith("http")) rawUrl else ApiDetails.APRIK_Kia_BASE_URL + rawUrl
        val fileName = message.fileName?.takeIf { it.isNotBlank() }
            ?: fullUrl.substringAfterLast('/').takeIf { it.isNotBlank() }
            ?: "download_${System.currentTimeMillis()}"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = PreferenceManager.getAccessToken()
                val request = Request.Builder()
                    .url(fullUrl)
                    .apply { if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token") }
                    .build()
                val client = OkHttpClient()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@VirtualChatRoomActivity, "Download failed", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val body = response.body ?: return@launch
                val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir
                } else {
                    @Suppress("DEPRECATION")
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                }
                val file = File(dir, fileName)
                file.outputStream().use { body.byteStream().copyTo(it) }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VirtualChatRoomActivity, "Saved: ${file.name}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("VirtualChatRoom", "Save failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VirtualChatRoomActivity, "Save failed", Toast.LENGTH_SHORT).show()
                }
            }
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
                sendWebSocketMessage(text, localId)
                addMessage(text, true, localId)
                binding.edtMessageTablet.setText("")
                scrollToLast()
                sendTypingStatus(false)
            }
        }
    }

    private fun scrollToLast() {
        binding.recyclerMessages.scrollToPosition((messages.size - 1).coerceAtLeast(0))
    }

    private fun addMessage(text: String, isSender: Boolean, messageId: String? = null) {
        if (TextUtils.isEmpty(text)) return
        val timeLabel = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date()).lowercase()
        messageAdapter?.addMessage(
            ChatMessage(
                messageId = messageId,
                text = text,
                isSender = isSender,
                timeLabel = timeLabel,
                status = if (isSender) MessageStatus.SENDING else MessageStatus.SENT
            )
        )
    }

    private fun sendWebSocketMessage(text: String, messageId: String? = null) {
        val json = JsonObject()
        json.addProperty("type", "chat.message")
        json.addProperty("content", text)
        // Only add message_id if it's a valid integer. 
        // If it starts with 'local_', it's a tracking ID, we might need to send it differently or not at all.
        // Assuming the server only wants integer for 'message_id' field.
        WebSocketManager.getInstance().sendMessage(json.toString())
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
            fileLauncher.launch(arrayOf("image/*", "video/*", "application/pdf", "*/*"))
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
