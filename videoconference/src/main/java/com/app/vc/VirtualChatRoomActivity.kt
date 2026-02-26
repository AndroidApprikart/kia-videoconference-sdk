package com.app.vc

import android.Manifest
import android.content.Intent
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
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.app.vc.views.WaveformView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VirtualChatRoomActivity : AppCompatActivity() {

    private var currentRole: UserRole = UserRole.CUSTOMER
    private var room: VirtualRoomUiModel? = null

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
    private val amplitudeHandler = Handler(Looper.getMainLooper())
    private val amplitudeRunnable = object : Runnable {
        override fun run() {
            try {
                val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                val normalized = (maxAmp / 32768f).coerceIn(0.05f, 1f)
                voiceAmplitudes.add(normalized)
                voiceNoteWaveformView?.addAmplitude(normalized)
            } catch (_: Exception) {}
            if (isRecording) amplitudeHandler.postDelayed(this, 80)
        }
    }
    private var isRecording = false

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vc_activity_virtual_chat_room)

        val roleFromIntent = intent.getStringExtra(EXTRA_ROLE)
        currentRole = when (roleFromIntent) {
            UserRole.SERVICE_ADVISOR.name -> UserRole.SERVICE_ADVISOR
            UserRole.MANAGER.name -> UserRole.MANAGER
            else -> UserRole.CUSTOMER
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
    }

    private fun setupToolbar() {
        findViewById<ImageView?>(R.id.btnBack)?.setOnClickListener { finish() }
        findViewById<TextView?>(R.id.txtTitle)?.text = when (currentRole) {
            UserRole.CUSTOMER -> getString(R.string.vc_title_virtual_chat_room_customer)
            UserRole.SERVICE_ADVISOR -> getString(R.string.vc_title_virtual_chat_room_service_advisor)
            UserRole.MANAGER -> getString(R.string.vc_title_virtual_chat_room_manager)
        }
    }

    private fun bindStaticPhoneHeader() {
        val room = room ?: return
        val txtRoomTitle = findViewById<TextView?>(R.id.txtRoomTitle) ?: return
        txtRoomTitle.text = "${room.roNumber} | ${room.subject}"
    }

    private fun bindStaticTabletPanels() {
        val room = room ?: return
        val txtBreadcrumb = findViewById<TextView?>(R.id.txtBreadcrumb) ?: return
        val txtLeftCustomerName = findViewById<TextView?>(R.id.txtLeftCustomerName)
        val txtLeftRoNumber = findViewById<TextView?>(R.id.txtLeftRoNumber)
        val txtLeftStatus = findViewById<TextView?>(R.id.txtLeftStatus)
        val txtLeftRoStatusText = findViewById<TextView?>(R.id.txtLeftRoStatusText)
        txtBreadcrumb.text = getString(R.string.vc_chat_breadcrumb)
        txtLeftCustomerName?.text = room.customerName
        txtLeftRoNumber?.text = room.roNumber
        txtLeftStatus?.text = room.status.name
        txtLeftRoStatusText?.text = getString(R.string.vc_chat_ro_status_value)
        val txtWelcomeHeader = findViewById<TextView?>(R.id.txtWelcomeHeaderTablet)
        val txtWelcomeBody = findViewById<TextView?>(R.id.txtWelcomeBodyTablet)
        txtWelcomeHeader?.text = getString(R.string.vc_chat_welcome_title, room.customerName)
        txtWelcomeBody?.text = getString(R.string.vc_chat_welcome_body, room.roNumber)
    }

    private fun setupMessageList() {
        val recycler = findViewById<RecyclerView?>(R.id.recyclerMessages) ?: return
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
        val containerPhone = findViewById<LinearLayout?>(R.id.quickReplyContainer)
        val messageFieldTablet = findViewById<EditText?>(R.id.edtMessageTablet)
        val containerTablet = findViewById<LinearLayout?>(R.id.quickReplyContainerTablet)
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
        val recycler = findViewById<RecyclerView?>(R.id.recyclerMessages)
        val edtMessage = findViewById<EditText?>(R.id.edtMessageTablet)
        val recordLayout = findViewById<LinearLayout?>(R.id.recordLayout)
        val sendLayout = findViewById<LinearLayout?>(R.id.sendLayout)
        val sendBtn = findViewById<ImageView?>(R.id.imgSendTablet)

        edtMessage?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                recordLayout?.visibility = if (hasText) View.GONE else View.VISIBLE
                sendLayout?.visibility = if (hasText) View.VISIBLE else View.GONE
            }
        })

        sendBtn?.setOnClickListener {
            val text = edtMessage?.text?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) {
                addMessage(text, true)
                edtMessage?.setText("")
                scrollToLast()
            }
        }
    }

    private fun setupAttachmentAndMedia() {
        val attachmentBtn = findViewById<ImageView?>(R.id.imgAttachmentTablet)
        attachmentBtn?.setOnClickListener { showAttachmentOptionsDialog() }
    }

    private fun showAttachmentOptionsDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.vc_dialog_attachment_options, null)
        val dialog = AlertDialog.Builder(this).setView(view).create()
        view.findViewById<LinearLayout>(R.id.optionGallery).setOnClickListener {
            dialog.dismiss()
            requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_MEDIA_IMAGES))
            galleryLauncher.launch("image/*")
        }
        view.findViewById<LinearLayout>(R.id.optionCamera).setOnClickListener {
            dialog.dismiss()
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
        view.findViewById<LinearLayout>(R.id.optionFile).setOnClickListener {
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
        val recordLayout = findViewById<LinearLayout?>(R.id.recordLayout)
        recordLayout?.setOnClickListener { showVoiceNoteDialog() }
    }

    private fun showVoiceNoteDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.vc_dialog_voice_note, null)
        voiceNoteDialogTimerView = view.findViewById(R.id.txtVoiceTimer)
        val btnClose = view.findViewById<ImageView>(R.id.btnCloseVoice)
        val btnRecord = view.findViewById<ImageView>(R.id.btnRecordVoice)
        val btnDelete = view.findViewById<ImageView>(R.id.btnDeleteVoice)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancelVoice)
        val btnSave = view.findViewById<TextView>(R.id.btnSaveVoice)

        voiceNoteDurationSeconds = 0
        voiceNotePath = null
        voiceNoteDialogTimerView?.text = "00:00"

        voiceNoteDialog = AlertDialog.Builder(this).setView(view).create()

        btnClose.setOnClickListener { voiceNoteDialog?.dismiss() }
        btnCancel.setOnClickListener { voiceNoteDialog?.dismiss() }

        btnRecord.setOnClickListener {
            if (!isRecording) startVoiceRecording() else stopVoiceRecording()
            isRecording = !isRecording
        }

        btnDelete.setOnClickListener {
            stopVoiceRecording()
            voiceNotePath = null
            voiceNoteDurationSeconds = 0
            voiceNoteDialogTimerView?.text = "00:00"
        }

        btnSave.setOnClickListener {
            if (isRecording) {
                stopVoiceRecording()
                isRecording = false
            }
            voiceNoteDialog?.dismiss()
            voiceNotePath?.let { path ->
                val timeLabel = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date()).lowercase()
                messageAdapter?.addMessage(ChatMessage(
                    text = "",
                    isSender = true,
                    timeLabel = timeLabel,
                    type = ChatMessageType.VOICE_NOTE,
                    attachmentUri = path,
                    durationSeconds = voiceNoteDurationSeconds
                ))
                scrollToLast()
            }
        }
        voiceNoteDialog?.show()
    }

    private fun startVoiceRecording() {
        requestPermission.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
        try {
            val file = File(cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            voiceNotePath = file.absolutePath
            voiceNoteDurationSeconds = 0
            voiceTimerHandler.post(voiceTimerRunnable)
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION") MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(voiceNotePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopVoiceRecording() {
        voiceTimerHandler.removeCallbacks(voiceTimerRunnable)
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        voiceNotePath?.let { path ->
            val mp = MediaPlayer().apply {
                setDataSource(path)
                prepare()
            }
            voiceNoteDurationSeconds = (mp.duration / 1000).coerceAtLeast(0)
            mp.release()
        }
    }

    private fun scrollToLast() {
        findViewById<RecyclerView?>(R.id.recyclerMessages)?.scrollToPosition((messages.size - 1).coerceAtLeast(0))
    }

    private fun addMessage(text: String, isSender: Boolean) {
        if (TextUtils.isEmpty(text)) return
        val timeLabel = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date()).lowercase()
        messageAdapter?.addMessage(ChatMessage(text = text, isSender = isSender, timeLabel = timeLabel))
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
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ROLE = "extra_role"
        const val EXTRA_ROOM_JSON = "extra_room_json"
    }
}
