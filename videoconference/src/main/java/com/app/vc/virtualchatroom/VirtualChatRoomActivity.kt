package com.app.vc.virtualchatroom

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
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
import android.view.LayoutInflater
import android.view.View
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.MediaFragment
import com.app.vc.ParticipantsListFragment
import com.app.vc.R
import com.app.vc.RODetailsFragment
import com.app.vc.RepairOrderActivity
import com.app.vc.RequestVideoCallDialog
import com.app.vc.databinding.VcActivityVirtualChatRoomBinding
import com.app.vc.utils.PreferenceManager
import com.app.vc.virtualroomlist.UserRole
import com.app.vc.virtualroomlist.VirtualRoomUiModel
import com.app.vc.websocketconnection.WebSocketManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.app.vc.views.WaveformView
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class VirtualChatRoomActivity : AppCompatActivity() {

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

    private var cameraPhotoPath: String? = null
    private var voiceNotePath: String? = null
    private var voiceNoteDurationSeconds: Int = 0
    private var mediaRecorder: MediaRecorder? = null
    private val voiceTimerHandler = Handler(Looper.getMainLooper())
    private val voiceTimerRunnable = object : Runnable {
        override fun run() {
            voiceNoteDurationSeconds++
            voiceNoteDialogTimerView?.text = "%02d:%02d".format(voiceNoteDurationSeconds / 60, voiceNoteDurationSeconds % 60)
            voiceTimerHandler.postDelayed(this, 1000)
        }
    }
    private var voiceNoteDialog: AlertDialog? = null
    private var voiceNoteDialogTimerView: TextView? = null
    private var voiceNoteWaveformView: WaveformView? = null
    private val voiceAmplitudes = mutableListOf<Float>()
    private var dialogBtnRecord: ImageView? = null
    private var dialogBtnDelete: ImageView? = null
    private var dialogBtnPlay: ImageView? = null
    private var dialogPauseIcon: ImageView? = null
    private val amplitudeHandler = Handler(Looper.getMainLooper())
    private val amplitudeRunnable = object : Runnable {

        override fun run() {

            val amp = mediaRecorder?.maxAmplitude ?: 0

            voiceNoteWaveformView?.addAmplitude(
                (amp / 200).coerceIn(5,120)
            )

            if (isRecording)
                amplitudeHandler.postDelayed(this,50)
        }
    }
    private var isRecording = false

    private var isTyping = false
    private val typingHandler = Handler(Looper.getMainLooper())
    private val stopTypingRunnable = Runnable {
        sendTypingStatus(false)
    }

    private val quickReplies = listOf(
        "When can we schedule for pickup?",
        "Can I share the payment link?",
        "Can we have a quick call?",
        "Will share the estimation in sometime"
    )

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { !it }) Toast.makeText(this, "Permission needed for camera and files", Toast.LENGTH_SHORT).show()
    }

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else Toast.makeText(this, "Camera permission needed", Toast.LENGTH_SHORT).show()
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraPhotoPath != null) showImagePreviewDialog(cameraPhotoPath!!)
        else cameraPhotoPath = null
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        val path = uriToPath(uri) ?: uri.toString()
        addImageMessage(path, true)
        scrollToLast()
    }

    private val fileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        val name = resolveFileName(uri) ?: "File"
        val path = uriToPath(uri) ?: uri.toString()
        messageAdapter?.addMessage(ChatMessage(
            text = "",
            isSender = true,
            timeLabel = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date()).lowercase(),
            type = ChatMessageType.FILE,
            attachmentUri = path,
            fileName = name
        ))
        scrollToLast()
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

        // Tablet-only UI (left panel with tabs) – present only on sw600dp layout
        val hasTabletPanels = binding.tabParticipants != null

        if (hasTabletPanels) {

            // ✅ Lock landscape for tablet
            requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            val tabRoDetails = binding.tabRoDetails ?: return

            loadFragment(RODetailsFragment())
            setupTabs()
            selectRoDetailsTab()
            moveIndicator(tabRoDetails)

        } else {

            // ✅ Phone behaves normally
            requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        binding.repairOrderLayout?.setOnClickListener {
            startActivity(
                Intent(this, RepairOrderActivity::class.java)
            )
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

    private fun connectToWebSocket() {
        val rawRoNumber = room?.roNumber ?: "default-room"
        val roomSlug = "test-08d50b33"
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

                        val content = jsonObject.get("content")?.asString ?: jsonObject.get("message")?.asString
                        if (content != null) {
                            addMessage(content, false)
                            scrollToLast()
                            // Send read receipt
                            sendReadReceipt(jsonObject.get("message_id")?.asString ?: "")
                        }
                    }
                    "chat.typing" -> {
                        val senderId = jsonObject.get("sender_id")?.asString
                        val currentUserId = PreferenceManager.getUserId()
                        if (senderId != currentUserId) {
                            val isTyping = jsonObject.get("is_typing")?.asBoolean ?: false
                            binding.txtTypingIndicator?.visibility = if (isTyping) View.VISIBLE else View.GONE
                            binding.txtTypingIndicator?.text = "${jsonObject.get("username")?.asString} is typing..."
                        }
                    }
                    "chat.read" -> {
                        // Someone read our message
                        messageAdapter?.updateMessageStatus("", MessageStatus.READ)
                    }
                }
            } catch (e: Exception) {
                Log.e("VirtualChatRoom", "Error parsing message: ${e.message}")
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
        val json = JsonObject()
        json.addProperty("type", "chat.read")
        json.addProperty("message_id", messageId)
        WebSocketManager.getInstance().sendMessage(json.toString())
    }

    override fun onDisconnected(reason: String) {
        runOnUiThread {
            Log.d("VirtualChatRoom", "WebSocket Disconnected: $reason")
        }
    }

    override fun onError(error: String) {
        runOnUiThread {
            Log.e("VirtualChatRoom", "WebSocket Error: $error")
            if (error.contains("403")) {
                Toast.makeText(this, "Access Denied. Please check token or room slug.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
        val titleView = binding.txtRoomTitle ?: return
        titleView.text = when (currentRole) {
            UserRole.CUSTOMER -> getString(R.string.vc_title_virtual_chat_room_customer)
            UserRole.SERVICE_ADVISOR -> getString(R.string.vc_title_virtual_chat_room_service_advisor)
            UserRole.MANAGER -> getString(R.string.vc_title_virtual_chat_room_manager)
        }
    }

    private fun bindStaticPhoneHeader() {
        val room = room ?: return
        val txtRoomTitle = binding.txtRoomTitle

        val subject = room.subject ?: ""

        val trimmedSubject =
            if (subject.length > 20)
                subject.substring(0, 20) + "..."
            else
                subject

    private fun bindStaticTabletPanels() {
        val room = room ?: return
        val txtLeftCustomerName = binding.txtLeftCustomerName
        val txtLeftRoNumber = binding.txtLeftRoNumber
        val txtLeftStatus = binding.txtLeftStatus
        txtLeftCustomerName?.text = room.customerName
        txtLeftRoNumber?.text = room.roNumber
        txtLeftStatus?.text = room.status.name
//        val txtWelcomeHeader = binding.txtWelcomeHeaderTablet
//        val txtWelcomeBody = binding.txtWelcomeBodyTablet
//        txtWelcomeHeader?.text = getString(R.string.vc_chat_welcome_title, room.customerName)
//        txtWelcomeBody?.text = getString(R.string.vc_chat_welcome_body, room.roNumber)
//
    }

    private fun setupMessageList() {
        val recycler: RecyclerView = binding.recyclerMessages
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recycler.layoutManager = layoutManager
        if (messages.isEmpty()) {
            room?.let { r ->
                messages.add(ChatMessage(text = getString(R.string.vc_chat_welcome_body, r.roNumber), isSender = false, timeLabel = r.timeLabel))
                messages.add(ChatMessage(text = getString(R.string.vc_chat_sample_message), isSender = true, timeLabel = r.timeLabel))
            }
        }
        messageAdapter = VirtualChatMessageAdapter(messages)
        recycler.adapter = messageAdapter
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

            val chip = inflater.inflate(
                R.layout.vc_quick_reply_chip,
                container,
                false
            ) as TextView

            chip.text = text

            chip.setOnClickListener {
                messageField.setText(text)
                messageField.setSelection(text.length)
            }

            container.addView(chip)
        }
    }

    private fun setupSendActions() {
        val recycler: RecyclerView = binding.recyclerMessages
        val edtMessage: EditText? = binding.edtMessageTablet
        val recordLayout: LinearLayout? = binding.recordLayout
        val sendLayout: LinearLayout? = binding.sendLayout
        val sendBtn: ImageView? = binding.imgSendTablet

        edtMessage?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrBlank()) {
                    sendTypingStatus(false)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                recordLayout?.visibility = if (hasText) View.GONE else View.VISIBLE
                sendLayout?.visibility = if (hasText) View.VISIBLE else View.GONE

                if (hasText) {
                    sendTypingStatus(true)

                    typingHandler.removeCallbacks(stopTypingRunnable)
                    typingHandler.postDelayed(stopTypingRunnable, 2000)
                }
            }
        })

        sendBtn?.setOnClickListener {
            val text = edtMessage?.text?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) {
                sendWebSocketMessage(text)
                addMessage(text, true)
                edtMessage?.setText("")
                scrollToLast()
                sendTypingStatus(false)
            }
        }
    }

    private fun sendWebSocketMessage(text: String) {
        val json = JsonObject()
        json.addProperty("type", "chat.message")
        json.addProperty("content", text)
        WebSocketManager.getInstance().sendMessage(json.toString())
    }

    private fun setupAttachmentAndMedia() {
        binding.imgAttachmentTablet.visibility=View.VISIBLE
        val attachmentBtn: ImageView? = binding.imgAttachmentTablet
        attachmentBtn?.setOnClickListener { showAttachmentOptionsDialog() }
    }
//
//    private fun showAttachmentOptionsDialog() {
//        val view = LayoutInflater.from(this).inflate(R.layout.vc_dialog_attachment_options, null)
//        val dialog = AlertDialog.Builder(this).setView(view).create()
//        view.findViewById<LinearLayout>(R.id.optionGallery).setOnClickListener {
//            dialog.dismiss()
//            requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_MEDIA_IMAGES))
//            galleryLauncher.launch("image/*")
//        }
//        view.findViewById<LinearLayout>(R.id.optionCamera).setOnClickListener {
//            dialog.dismiss()
//            requestCameraPermission.launch(Manifest.permission.CAMERA)
//        }
//        view.findViewById<LinearLayout>(R.id.optionFile).setOnClickListener {
//            dialog.dismiss()
//            fileLauncher.launch(arrayOf("*/*"))
//        }
//        dialog.show()
//    }

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
            params.gravity = Gravity.BOTTOM or Gravity.END   // Bottom Right

            params.y = 140   // height above message box
            params.x = 20    // small right margin

            attributes = params

            setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }


        dialogView.findViewById<LinearLayout>(R.id.optionGallery).setOnClickListener {
            dialog.dismiss()
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            )
            galleryLauncher.launch("image/*")
        }

        dialogView.findViewById<LinearLayout>(R.id.optionCamera).setOnClickListener {
            dialog.dismiss()
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        dialogView.findViewById<LinearLayout>(R.id.optionFile).setOnClickListener {
            dialog.dismiss()
            fileLauncher.launch(arrayOf("*/*"))
        }
        dialog.show()
    }

    private fun launchCamera() {
        val photoFile = File(cacheDir, "chat_photo_${System.currentTimeMillis()}.jpg")
        cameraPhotoPath = photoFile.absolutePath
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
        takePictureLauncher.launch(uri)
    }

    private fun showImagePreviewDialog(imagePath: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.vc_dialog_image_preview, null)
        val img = view.findViewById<ImageView>(R.id.imgPreview)
        img.setImageURI(Uri.fromFile(File(imagePath)))
        val dialog = AlertDialog.Builder(this).setView(view).create()
        view.findViewById<TextView>(R.id.btnCancelPreview).setOnClickListener { dialog.dismiss() }
        view.findViewById<TextView>(R.id.btnSendPreview).setOnClickListener {
            dialog.dismiss()
            addImageMessage(imagePath, true)
            scrollToLast()
        }
        dialog.show()
    }

    private fun addImageMessage(path: String, isSender: Boolean) {
        val timeLabel = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date()).lowercase()
        messageAdapter?.addMessage(ChatMessage(
            text = "",
            isSender = isSender,
            timeLabel = timeLabel,
            type = ChatMessageType.IMAGE,
            attachmentUri = path
        ))
    }

    private fun setupVoiceNote() {
        val recordLayout: LinearLayout? = binding.recordLayout
        recordLayout?.setOnClickListener { showVoiceNoteDialog() }
    }

    private fun showVoiceNoteDialog() {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.vc_dialog_voice_note, null)

        voiceNoteWaveformView = view.findViewById(R.id.waveformView)

        voiceNoteWaveformView?.visibility = View.GONE
        voiceNoteDialogTimerView = view.findViewById(R.id.txtVoiceTimer)

        val btnClose = view.findViewById<ImageView>(R.id.btnCloseVoice)
        val btnRecord = view.findViewById<ImageView>(R.id.btnRecordVoice)
        val btnDelete = view.findViewById<ImageView>(R.id.btnDeleteVoice)
        val btnPlay = view.findViewById<ImageView>(R.id.btnPlayPauseVoice)
        val pauseIcon = view.findViewById<ImageView>(R.id.pauseIcon)

        // cache references for permission callback
        dialogBtnRecord = btnRecord
        dialogBtnDelete = btnDelete
        dialogBtnPlay = btnPlay
        dialogPauseIcon = pauseIcon

        val btnCancel = view.findViewById<TextView>(R.id.btnCancelVoice)
        val btnSave = view.findViewById<TextView>(R.id.btnSaveVoice)

        voiceNoteDurationSeconds = 0
        voiceNotePath = null
        voiceNoteDialogTimerView?.text = "00:00"

        voiceNoteDialog = AlertDialog.Builder(this).setView(view).create()

        btnClose.setOnClickListener { voiceNoteDialog?.dismiss() }
        btnCancel.setOnClickListener { voiceNoteDialog?.dismiss() }

        btnRecord.setOnClickListener {

            if (isPlaying) {
                Toast.makeText(this, "Stop playback first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
        pauseIcon.setOnClickListener {

            stopVoiceRecording()
            isRecording = false

            btnRecord.visibility = View.VISIBLE
            btnDelete.visibility = View.VISIBLE
            btnPlay.visibility = View.VISIBLE

            pauseIcon.visibility = View.GONE
        }

        btnDelete.setOnClickListener {

            if (isPlaying) {
                Toast.makeText(this,"Stop playback first",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            stopVoiceRecording()

            voiceNotePath?.let {
                File(it).delete()
            }

            voiceNotePath = null
            voiceNoteDurationSeconds = 0

            voiceNoteDialogTimerView?.text = "00:00"
            voiceNoteWaveformView?.visibility = View.VISIBLE
            voiceNoteWaveformView?.clear()

        }

        btnPlay.setOnClickListener {

            voiceNotePath ?: return@setOnClickListener

            if (!isPlaying) {

                mediaPlayer = MediaPlayer().apply {
                    setDataSource(voiceNotePath)
                    prepare()
                }

                mediaPlayer?.start()

                isPlaying = true

                btnPlay.setImageResource(R.drawable.pause)

                btnDelete.isEnabled = false
                btnRecord.isEnabled = false

                voiceNoteWaveformView?.visibility = View.VISIBLE
                voiceNoteWaveformView?.resetProgress()

                playbackHandler.removeCallbacks(playbackRunnable)
                playbackHandler.post(playbackRunnable)

                mediaPlayer?.setOnCompletionListener {

                    stopPlaybackTimer()

                    isPlaying = false

                    btnPlay.setImageResource(R.drawable.play_circle)

                    btnDelete.isEnabled = true
                    btnRecord.isEnabled = true

                    voiceNoteWaveformView?.resetProgress()
                }

            } else {

                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null

                stopPlaybackTimer()

                isPlaying = false

                btnPlay.setImageResource(R.drawable.play_circle)

                btnDelete.isEnabled = true
                btnRecord.isEnabled = true
            }
        }

        btnSave.setOnClickListener {

            if (isRecording) {
                stopVoiceRecording()
                isRecording = false
            }

            voiceNoteDialog?.dismiss()

            voiceNotePath?.let { path ->

                val timeLabel = SimpleDateFormat(
                    "hh:mma",
                    Locale.getDefault()
                ).format(Date()).lowercase()

                messageAdapter?.addMessage(
                    ChatMessage(
                        text = "",
                        isSender = true,
                        timeLabel = timeLabel,
                        type = ChatMessageType.VOICE_NOTE,
                        attachmentUri = path,
                        durationSeconds = voiceNoteDurationSeconds
                    )
                )

                scrollToLast()
            }
        }

        voiceNoteDialog?.show()
    }
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecordingFlow(
        btnRecord: ImageView,
        btnDelete: ImageView,
        btnPlay: ImageView,
        pauseIcon: ImageView
    ) {
        startVoiceRecording()
        isRecording = true

        btnRecord.visibility = View.GONE
        btnDelete.visibility = View.GONE
        btnPlay.visibility = View.GONE

        pauseIcon.visibility = View.VISIBLE
        voiceNoteWaveformView?.visibility = View.VISIBLE
        voiceNoteWaveformView?.clear()
    }
    private val recordAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                dialogBtnRecord?.let { r ->
                    dialogBtnDelete?.let { d ->
                        dialogBtnPlay?.let { p ->
                            dialogPauseIcon?.let { pause ->
                                startRecordingFlow(r, d, p, pause)
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
            }
        }
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startVoiceRecording() {

        if (isRecording) return

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

        pcmFile = File(cacheDir,"voice_note.pcm")
        if (pcmFile!!.exists()) pcmFile!!.delete()

        audioRecord?.startRecording()

        isRecording = true

        voiceTimerHandler.post(voiceTimerRunnable)

        Thread {

            val buffer = ShortArray(1024)
            val output = FileOutputStream(pcmFile!!)

            while (isRecording) {

                val read = audioRecord?.read(buffer,0,buffer.size) ?: 0

                if (read > 0) {

                    val maxAmp =
                        buffer.take(read).maxOf {
                            abs(it.toInt())
                        }

                    val normalized =
                        (maxAmp / 300).coerceAtLeast(1)

                    runOnUiThread {

                        voiceNoteWaveformView?.addAmplitude(
                            normalized
                        )
                    }

                    val byteBuffer =
                        ByteBuffer.allocate(read*2)

                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

                    buffer.take(read).forEach {
                        byteBuffer.putShort(it)
                    }

                    output.write(byteBuffer.array())
                }
            }

            output.close()

        }.start()
    }

    private fun stopVoiceRecording() {

        if (!isRecording) return

        isRecording = false

        audioRecord?.stop()
        audioRecord?.release()

        audioRecord = null

        voiceTimerHandler.removeCallbacks(
            voiceTimerRunnable
        )
    }

    private fun scrollToLast() {
        binding.recyclerMessages.scrollToPosition((messages.size - 1).coerceAtLeast(0))
    }

    private fun addMessage(text: String, isSender: Boolean) {
        if (TextUtils.isEmpty(text)) return
        val timeLabel = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date()).lowercase()
        messageAdapter?.addMessage(ChatMessage(text = text, isSender = isSender, timeLabel = timeLabel, status = if(isSender) MessageStatus.SENDING else MessageStatus.SENT))
    }

    private fun uriToPath(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                val file = File(cacheDir, "file_${System.currentTimeMillis()}")
                file.outputStream().use { input.copyTo(it) }
                file.absolutePath
            }
        } catch (_: Exception) { null }
    }

    private fun resolveFileName(uri: Uri): String? {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment
    }

    override fun onDestroy() {
        voiceTimerHandler.removeCallbacks(voiceTimerRunnable)
        try { mediaRecorder?.release() } catch (_: Exception) {}
        WebSocketManager.getInstance().disconnect()
        super.onDestroy()
    }
    private fun startPlaybackTimer() {
        playbackHandler.post(playbackRunnable)
    }
    private fun stopPlaybackTimer() {
        playbackHandler.removeCallbacks(playbackRunnable)
    }

    private val playbackRunnable = object : Runnable {

        override fun run() {

            val player = mediaPlayer ?: return

            if (!player.isPlaying) return

            val position = player.currentPosition
            val duration = player.duration.takeIf { it > 0 } ?: return

            val sec = position / 1000

            voiceNoteDialogTimerView?.text =
                "%02d:%02d".format(sec / 60, sec % 60)

            val progress =
                (position.toFloat() / duration.toFloat() * 50).toInt()

            voiceNoteWaveformView?.updateProgress(progress)

            playbackHandler.postDelayed(this,40)
        }
    }

    companion object {
        const val EXTRA_ROLE = "extra_role"
        const val EXTRA_ROOM_JSON = "extra_room_json"
    }

    private fun loadFragment(fragment: Fragment) {

        supportFragmentManager.beginTransaction()
            .replace(R.id.FragmentContainer, fragment)
            .commit()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupTabs() {

        val tabParticipants = binding.tabParticipants ?: return
        val tabMedia = binding.tabMedia ?: return
        val tabRoDetails = binding.tabRoDetails ?: return

        tabParticipants.setOnClickListener {

            loadFragment(ParticipantsListFragment())

            selectParticipantsTab()

            moveIndicator(tabParticipants)
        }

        tabMedia.setOnClickListener {

            loadFragment(MediaFragment())

            selectMediaTab()
            moveIndicator(tabMedia)
        }
        tabRoDetails.setOnClickListener {

            loadFragment(RODetailsFragment())

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
}

