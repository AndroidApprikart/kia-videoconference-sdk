package com.app.vc.virtualchatroom

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.app.vc.MediaFragment
import com.app.vc.ParticipantsListFragment
import com.app.vc.R
import com.app.vc.RepairOrderActivity
import com.app.vc.RequestVideoCallDialog
import com.app.vc.databinding.VcActivityVirtualChatRoomBinding
import com.app.vc.network.LoginApiService
import com.app.vc.utils.ApiDetails
import com.app.vc.utils.PreferenceManager
import com.app.vc.virtualroomlist.UserRole
import com.app.vc.virtualroomlist.VirtualRoomUiModel
import com.app.vc.websocketconnection.WebSocketManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.app.vc.views.WaveformView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class VirtualChatRoomActivity : AppCompatActivity(), WebSocketManager.WebSocketCallback {

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

    companion object {
        const val EXTRA_ROLE = "extra_role"
        const val EXTRA_ROOM_JSON = "extra_room_json"
    }


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
    private var voiceNoteDialog: AlertDialog? = null
    private var voiceNoteWaveformView: WaveformView? = null
    private val amplitudeHandler = Handler(Looper.getMainLooper())
    private val amplitudeRunnable = object : Runnable {
        override fun run() {
            val amp = try {
                mediaRecorder?.maxAmplitude ?: 0
            } catch (_: Exception) {
                0
            }
            voiceNoteWaveformView?.addAmplitude((amp / 200).coerceIn(5, 120))
            if (isRecording) amplitudeHandler.postDelayed(this, 50)
        }
    }
    private var isRecording = false

    private var isTyping = false
    private val typingHandler = Handler(Looper.getMainLooper())
    private val stopTypingRunnable = Runnable { sendTypingStatus(false) }

    private val apiService: LoginApiService by lazy {
        val gson = GsonBuilder().setLenient().create()
        Retrofit.Builder()
            .baseUrl(ApiDetails.APRIK_Kia_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(LoginApiService::class.java)
    }

    private val quickReplies = listOf(
        "When can we schedule for pickup?",
        "Can I share the payment link?",
        "Can we have a quick call?",
        "Will share the estimation in sometime"
    )

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

    private fun uriToFile(uri: Uri): File? {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                val file =
                    File(cacheDir, "temp_file_${System.currentTimeMillis()}_${uri.lastPathSegment}")
                file.outputStream().use { input.copyTo(it) }
                file
            }
        } catch (_: Exception) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = VcActivityVirtualChatRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val roleFromIntent = intent.getStringExtra(EXTRA_ROLE)
        currentRole = when (roleFromIntent) {
            UserRole.SERVICE_ADVISOR.name -> UserRole.SERVICE_ADVISOR
            UserRole.MANAGER.name -> UserRole.MANAGER
            else -> UserRole.CUSTOMER
        }

        if (binding.tabParticipants != null) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            loadFragment(ParticipantsListFragment())
            setupTabs()
            selectParticipantsTab()
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        binding.repairOrderLayout?.setOnClickListener {
            startActivity(Intent(this, RepairOrderActivity::class.java))
        }

        binding.btnVideoCall?.setOnClickListener {
            RequestVideoCallDialog(this).show()
        }

        val roomJson = intent.getStringExtra(EXTRA_ROOM_JSON)
        if (roomJson != null) {
            room = Gson().fromJson(roomJson, VirtualRoomUiModel::class.java)
        }

        setupToolbar()
        bindStaticPhoneHeader()
        bindStaticTabletPanels()
        setupMessageList()
        setupQuickReplies()
        setupSendActions()
        setupAttachmentAndMedia()
        setupVoiceNote()
        connectToWebSocket()
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
        voiceNoteDialogTimerView = view.findViewById(R.id.txtVoiceTimer)
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
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startRecordingFlow(btnRecord, btnDelete, btnPlay, pauseIcon)
            } else {
                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
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

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startVoiceRecording() {
        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        pcmFile = File(cacheDir, "voice_note.pcm")
        voiceNotePath = pcmFile?.absolutePath
        audioRecord?.startRecording()
        isRecording = true
        voiceTimerHandler.post(voiceTimerRunnable)
        Thread {
            val buffer = ShortArray(1024)
            val output = FileOutputStream(pcmFile!!)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val maxAmp = buffer.take(read).maxOf { abs(it.toInt()) }
                    runOnUiThread {
                        voiceNoteWaveformView?.addAmplitude(
                            (maxAmp / 300).coerceAtLeast(
                                1
                            )
                        )
                    }
                    output.write(
                        ByteBuffer.allocate(read * 2).order(ByteOrder.LITTLE_ENDIAN)
                            .apply { buffer.take(read).forEach { putShort(it) } }.array()
                    )
                }
            }
            output.close()
        }.start()
    }

    private fun stopVoiceRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        voiceTimerHandler.removeCallbacks(voiceTimerRunnable)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupTabs() {
        binding.tabParticipants?.setOnClickListener {
            loadFragment(ParticipantsListFragment())
            selectParticipantsTab()
            moveIndicator(it)
        }
        binding.tabMedia?.setOnClickListener {
            loadFragment(MediaFragment())
            selectMediaTab()
            moveIndicator(it)
        }
    }

    private fun moveIndicator(tab: View) {
        binding.tabIndicator?.post {
            binding.tabIndicator?.layoutParams?.width = tab.width
            binding.tabIndicator?.requestLayout()
            binding.tabIndicator?.x = tab.left.toFloat()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun selectParticipantsTab() {
        binding.tabParticipants?.setTextColor(getColor(R.color.colorPrimary_kia_kandid))
        binding.tabParticipants?.typeface = resources.getFont(R.font.kia_signature_fix_bold)
        binding.tabMedia?.setTextColor(getColor(R.color.gray_mic_background))
        binding.tabMedia?.typeface = resources.getFont(R.font.kia_signature_fix_regular)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun selectMediaTab() {
        binding.tabMedia?.setTextColor(getColor(R.color.colorPrimary_kia_kandid))
        binding.tabMedia?.typeface = resources.getFont(R.font.kia_signature_fix_bold)
        binding.tabParticipants?.setTextColor(getColor(R.color.gray_mic_background))
        binding.tabParticipants?.typeface = resources.getFont(R.font.kia_signature_fix_regular)
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
                        val senderId = jsonObject.get("sender_id")?.asString
                        val currentUserId = PreferenceManager.getUserId()

                        if (senderId != null && senderId == currentUserId) {
                            Log.d("VirtualChatRoom", "Ignoring echoed message from self")
                            // Update our local message to SENT status since server acknowledged it
                            messageAdapter?.updateMessageStatus("", MessageStatus.SENT)
                            return@runOnUiThread
                        }

                        val content = jsonObject.get("content")?.asString
                            ?: jsonObject.get("message")?.asString
                        val attachment = jsonObject.get("attachment")?.asJsonObject

                        if (attachment != null) {
                            val attachmentUrl = attachment.get("file_url")?.asString ?: ""
                            val fileName = attachment.get("file_name")?.asString ?: ""
                            val mimeType = attachment.get("mime_type")?.asString ?: ""
                            val fullUrl =
                                if (attachmentUrl.startsWith("http")) attachmentUrl else ApiDetails.APRIK_Kia_BASE_URL + attachmentUrl

                            val msgType = when {
                                mimeType.startsWith("image") -> ChatMessageType.IMAGE
                                mimeType.startsWith("video") -> ChatMessageType.VIDEO
                                else -> ChatMessageType.FILE
                            }

                            messageAdapter?.addMessage(
                                ChatMessage(
                                    text = "",
                                    isSender = false,
                                    timeLabel = SimpleDateFormat(
                                        "hh:mma",
                                        Locale.getDefault()
                                    ).format(Date()).lowercase(),
                                    type = msgType,
                                    attachmentUri = fullUrl,
                                    fileName = fileName,
                                    caption = content,
                                    mimeType = mimeType
                                )
                            )
                        } else if (content != null) {
                            addMessage(content, false)
                        }
                        scrollToLast()
                        sendReadReceipt(jsonObject.get("message_id")?.asString ?: "")
                    }

                    "chat.typing" -> {
                        val senderId = jsonObject.get("sender_id")?.asString
                        val currentUserId = PreferenceManager.getUserId()
                        if (senderId != currentUserId) {
                            val isTypingBroadcast = jsonObject.get("is_typing")?.asBoolean ?: false
                            binding.txtTypingIndicator?.visibility =
                                if (isTypingBroadcast) View.VISIBLE else View.GONE
                            binding.txtTypingIndicator?.text =
                                "${jsonObject.get("username")?.asString} is typing..."
                        }
                    }

                    "chat.read" -> {
                        messageAdapter?.updateMessageStatus("", MessageStatus.READ)
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
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.vc_bottom_sheet_attachment_preview, null)
        bottomSheetDialog.setContentView(view)

        val viewPager: ViewPager2 = view.findViewById(R.id.viewPagerPreview)
        val edtCaption: EditText = view.findViewById(R.id.edtCaptionPreview)
        val btnSend: View = view.findViewById(R.id.btnSendPreview)
        val btnCancel: View = view.findViewById(R.id.btnCancelPreview)

        viewPager.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                val img = ImageView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                }
                return object : RecyclerView.ViewHolder(img) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val (file, type) = selectedFiles[position]
                val img = holder.itemView as ImageView
                if (type == ChatMessageType.IMAGE) {
                    img.setImageURI(Uri.fromFile(file))
                } else if (type == ChatMessageType.VIDEO) {
                    img.setImageResource(android.R.drawable.ic_media_play)
                } else {
                    img.setImageResource(android.R.drawable.ic_menu_save)
                }
            }

            override fun getItemCount() = selectedFiles.size
        }

        btnCancel.setOnClickListener { bottomSheetDialog.dismiss() }

        btnSend.setOnClickListener {
            val caption = edtCaption.text.toString()
            bottomSheetDialog.dismiss()

            selectedFiles.forEach { (file, type) ->
                val timeLabel =
                    SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date()).lowercase()
                val localId = "local_${System.currentTimeMillis()}_${file.name}"
                val tempMessage = ChatMessage(
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
                messageAdapter?.addMessage(tempMessage)
                scrollToLast()

                lifecycleScope.launch {
                    val fileToUpload = if (type == ChatMessageType.IMAGE) {
                        try {
                            compressImage(file)
                        } catch (e: Exception) {
                            file
                        }
                    } else file
                    performUpload(
                        fileToUpload,
                        if (selectedFiles.size == 1) caption else "",
                        type,
                        localId
                    )
                }
            }
        }

        bottomSheetDialog.show()
    }

    private suspend fun compressImage(file: File): File = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
        val compressedFile = File(cacheDir, "compressed_${file.name}")
        FileOutputStream(compressedFile).use { it.write(out.toByteArray()) }
        compressedFile
    }

    private fun performUpload(file: File, caption: String, type: ChatMessageType, localId: String) {
        val slug = room?.roNumber ?: return
        val token = PreferenceManager.getAccessToken() ?: return

        val mimeType = when (type) {
            ChatMessageType.IMAGE -> "image/jpeg"
            ChatMessageType.VIDEO -> "video/mp4"
            ChatMessageType.VOICE_NOTE -> "audio/wav"
            else -> "application/octet-stream"
        }

        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val captionPart = caption.toRequestBody("text/plain".toMediaTypeOrNull())

        lifecycleScope.launch {
            try {
                val response = apiService.uploadFile("Bearer $token", slug, body, captionPart)
                if (response.isSuccessful && response.body() != null) {
                    runOnUiThread {
                        messageAdapter?.updateMessageStatus(
                            localId,
                            MessageStatus.SENT
                        )
                    }
                } else {
                    runOnUiThread {
                        messageAdapter?.updateMessageStatus(
                            localId,
                            MessageStatus.ERROR
                        )
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
        json.addProperty("type", "typing")
        json.addProperty("is_typing", typing)
        WebSocketManager.getInstance().sendMessage(json.toString())
    }

    private fun sendReadReceipt(messageId: String) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("type", "chat.read")
        jsonObject.addProperty("message_id", messageId)
        WebSocketManager.getInstance().sendMessage(jsonObject.toString())
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
        binding.txtLeftStatus?.text = room.status.name
    }

    private fun setupMessageList() {
        binding.recyclerMessages.layoutManager =
            LinearLayoutManager(this).apply { stackFromEnd = true }
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
            onItemClick = { message -> handleAttachmentClick(message) }
        )
        binding.recyclerMessages.adapter = messageAdapter
    }

    private fun handleAttachmentClick(message: ChatMessage) {
        val url = message.attachmentUri ?: return
        val fragment = MediaViewerFragment.newInstance(url, message.type.name)
        supportFragmentManager.beginTransaction()
            .replace(R.id.FragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    class MediaViewerFragment : Fragment() {
        companion object {
            fun newInstance(url: String, type: String): MediaViewerFragment {
                return MediaViewerFragment().apply {
                    arguments = Bundle().apply {
                        putString("url", url)
                        putString("type", type)
                    }
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
                sendWebSocketMessage(text)
                addMessage(text, true)
                binding.edtMessageTablet.setText("")
                scrollToLast()
                sendTypingStatus(false)
            }
        }
    }

    private fun scrollToLast() {
        binding.recyclerMessages.scrollToPosition((messages.size - 1).coerceAtLeast(0))
    }

    private fun addMessage(text: String, isSender: Boolean) {
        if (TextUtils.isEmpty(text)) return
        val timeLabel = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date()).lowercase()
        messageAdapter?.addMessage(
            ChatMessage(
                text = text,
                isSender = isSender,
                timeLabel = timeLabel,
                status = if (isSender) MessageStatus.SENDING else MessageStatus.SENT
            )
        )
    }

    private fun sendWebSocketMessage(text: String) {
        val json = JsonObject()
        json.addProperty("type", "chat.message")
        json.addProperty("content", text)
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
            fileLauncher.launch(arrayOf("image/*", "video/*"))
        }

        dialogView.findViewById<LinearLayout>(R.id.optionCamera)?.setOnClickListener {
            dialog.dismiss()
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        dialogView.findViewById<LinearLayout>(R.id.optionFile)?.setOnClickListener {
            dialog.dismiss()
            fileLauncher.launch(arrayOf("*/*"))
        }
    }
}