package com.app.vc.virtualchatroom

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.R
import com.app.vc.message.ResponseModelEstimateData
import com.app.vc.utils.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.kia.vc.message.Labour
import com.kia.vc.message.LabourListAdapter
import com.kia.vc.message.Part
import com.kia.vc.message.PartListAdapter
import java.io.File
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

enum class ChatMessageType { TEXT, IMAGE, FILE, VIDEO, VOICE_NOTE, ESTIMATION, DATE_HEADER }
enum class MessageStatus { SENDING, SENT, READ, ERROR }

data class ChatMessage(
    var messageId: String? = null,
    val text: String,
    val isSender: Boolean,
    var senderName: String? = null,
    val senderUsername: String? = null,
    val senderId: String? = null,
    var senderRoleAbbrev: String? = null,
    val timeLabel: String,
    val createdAtMillis: Long? = null,
    var status: MessageStatus = MessageStatus.SENT,
    val type: ChatMessageType = ChatMessageType.TEXT,
    var attachmentUri: String? = null,
    val durationSeconds: Int? = null,
    val fileName: String? = null,
    val caption: String? = null,
    val groupId: String? = null,
    var uploadProgressPercent: Int? = null,
    val waveformData: String? = null,
    val mimeType: String? = null,
    var thumbnailUrl: String? = null,
    val estimationDetails: ResponseModelEstimateData? = null
)

interface EstimationInteractionListener : 
    PartListAdapter.OnPartCheckboxSelectedListener, 
    LabourListAdapter.OnLabourCheckboxSelectedListener {
    fun onAcceptClicked(parentPosition: Int, estimationDetails: ResponseModelEstimateData)
    fun onRejectClicked(parentPosition: Int, estimationDetails: ResponseModelEstimateData)
    fun onSelectAllClicked(parentPosition: Int, isSelected: Boolean, estimationDetails: ResponseModelEstimateData)
}

class VirtualChatMessageAdapter(
    private val messages: MutableList<ChatMessage>,
    private val onRetryClick: (ChatMessage) -> Unit,
    private val onItemClick: (ChatMessage) -> Unit,
    private val onSaveMedia: (ChatMessage) -> Unit = {},
    private val estimationListener: EstimationInteractionListener? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


//    init {
//        setHasStableIds(true)
//    }
    private var recyclerView: RecyclerView? = null

    private var currentRecyclerView: RecyclerView? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playingPosition: Int = -1
    private var handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    companion object {
        private const val VIEW_TYPE_OUTGOING = 1
        private const val VIEW_TYPE_INCOMING = 2
        private const val VIEW_TYPE_ESTIMATION_OUTGOING = 3
        private const val VIEW_TYPE_ESTIMATION_INCOMING = 4
        private const val VIEW_TYPE_DATE_HEADER = 5

        fun parseWaveformData(waveformData: String?): FloatArray {
            if (waveformData.isNullOrBlank()) return floatArrayOf()
            return waveformData.split(",").mapNotNull { it.trim().toFloatOrNull() }.toFloatArray()
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        currentRecyclerView = recyclerView
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        if (message.type == ChatMessageType.DATE_HEADER) return VIEW_TYPE_DATE_HEADER
        return if (message.type == ChatMessageType.ESTIMATION) {
            if (message.isSender) VIEW_TYPE_ESTIMATION_OUTGOING else VIEW_TYPE_ESTIMATION_INCOMING
        } else {
            if (message.isSender) VIEW_TYPE_OUTGOING else VIEW_TYPE_INCOMING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_DATE_HEADER -> {
                val view = inflater.inflate(R.layout.vc_item_chat_date_header, parent, false)
                DateHeaderViewHolder(view)
            }
            VIEW_TYPE_OUTGOING -> {
                val view = inflater.inflate(R.layout.vc_item_chat_message_outgoing, parent, false)
                OutgoingViewHolder(view, this, onRetryClick, onItemClick, onSaveMedia)
            }
            VIEW_TYPE_INCOMING -> {
                val view = inflater.inflate(R.layout.vc_item_chat_message_incoming, parent, false)
                IncomingViewHolder(view, this, onItemClick, onSaveMedia)
            }
            VIEW_TYPE_ESTIMATION_OUTGOING -> {
                val view = inflater.inflate(R.layout.layout_estimation_message_self, parent, false)
                EstimationOutgoingViewHolder(view, estimationListener)
            }
            VIEW_TYPE_ESTIMATION_INCOMING -> {
                val view = inflater.inflate(R.layout.layout_estimation_message_remote, parent, false)
                EstimationIncomingViewHolder(view, estimationListener)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is DateHeaderViewHolder -> holder.bind(message)
            is OutgoingViewHolder -> holder.bind(message, position)
            is IncomingViewHolder -> holder.bind(message, position)
            is EstimationOutgoingViewHolder -> holder.bind(message, position)
            is EstimationIncomingViewHolder -> holder.bind(message, position)
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemId(position: Int): Long {
        val id = messages.getOrNull(position)?.messageId ?: position.toString()
        return id.hashCode().toLong()
    }



    fun playAudio(message: ChatMessage, position: Int) {

        if (playingPosition == position) {
            stopAudio()
            return
        }

        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(message.attachmentUri)
            prepare()
            start()
        }

        val oldPosition = playingPosition
        playingPosition = position

        if (oldPosition != -1) notifyItemChanged(oldPosition)
        notifyItemChanged(position)

        startTimer()

        mediaPlayer?.setOnCompletionListener {
            stopAudio()
        }
    }
    fun stopAudio() {

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        handler.removeCallbacksAndMessages(null)

        val oldPosition = playingPosition
        playingPosition = -1

        if (oldPosition != -1) notifyItemChanged(oldPosition)
    }

    private fun startTimer() {

        updateRunnable = object : Runnable {

            override fun run() {

                val pos = playingPosition
                if (pos == -1) return

                val holder = currentRecyclerView?.findViewHolderForAdapterPosition(pos)

                if (holder != null && mediaPlayer != null) {

                    val seconds = mediaPlayer!!.currentPosition / 1000

                    when (holder) {
                        is OutgoingViewHolder -> holder.updateVoiceTimer(seconds)
                        is IncomingViewHolder -> holder.updateVoiceTimer(seconds)
                    }
                }

                handler.postDelayed(this, 500)
            }
        }

        handler.post(updateRunnable!!)
    }




    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun replaceAll(newMessages: List<ChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtDate: TextView = itemView.findViewById(R.id.txtDateHeader)
        fun bind(message: ChatMessage) {
            txtDate.text = message.text
        }
    }
    
    fun updateMessageStatus(messageId: String, newStatus: MessageStatus) {
        if (messageId.isEmpty() && newStatus == MessageStatus.READ) {
            messages.forEachIndexed { index, msg ->
                if (msg.isSender && msg.status == MessageStatus.SENT) {
                    msg.status = MessageStatus.READ
                    notifyItemChanged(index)
                }
            }
            return
        }

        var index = if (messageId.isNotEmpty()) {
            messages.indexOfFirst { it.messageId == messageId }
        } else {
            messages.indexOfLast { it.isSender }
        }

        if (index == -1 && newStatus == MessageStatus.SENT) {
            index = messages.indexOfLast { it.isSender && it.status == MessageStatus.SENDING }
            if (index != -1) {
                messages[index].messageId = messageId
            }
        }

        if (index == -1) return
        if (newStatus == MessageStatus.READ) {
            // Mark this message and all previous outgoing (SENT) as READ so read ticks update in tab UI
            for (i in 0..index) {
                if (messages[i].isSender && messages[i].status == MessageStatus.SENT) {
                    messages[i].status = MessageStatus.READ
                    notifyItemChanged(i)
                }
            }
        } else {
            messages[index].status = newStatus
            notifyItemChanged(index)
        }
    }

    fun updateUploadProgress(messageId: String, percent: Int) {
        val idx = messages.indexOfFirst { it.messageId == messageId }
        if (idx == -1) return
        messages[idx].uploadProgressPercent = percent.coerceIn(0, 100)
        notifyItemChanged(idx)
    }

    fun updateMessageId(localId: String, serverId: String) {
        val index = messages.indexOfLast { it.messageId == localId }
        if (index != -1) {
            messages[index].messageId = serverId
            notifyItemChanged(index)
        }
    }

    fun updateMessageIdAndStatus(localId: String, serverId: String, newStatus: MessageStatus) {
        val index = messages.indexOfLast { it.messageId == localId }
        if (index != -1) {
            messages[index].messageId = serverId
            messages[index].status = newStatus
            notifyItemChanged(index)
        }
    }

    fun updateMessageAttachmentUrl(localId: String, serverUrl: String, thumbnailUrl: String? = null) {
        val index = messages.indexOfLast { it.messageId == localId }
        if (index != -1) {
            messages[index].attachmentUri = serverUrl
            if (thumbnailUrl != null) messages[index].thumbnailUrl = thumbnailUrl
            notifyItemChanged(index)
        }
    }

    class OutgoingViewHolder(
        itemView: View,
        private val adapter: VirtualChatMessageAdapter,
        private val onRetry: (ChatMessage) -> Unit,
        private val onClick: (ChatMessage) -> Unit,
        private val onSaveMedia: (ChatMessage) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val btnPlayVoice: ImageView? = itemView.findViewById(R.id.btnPlayVoice)

        private val txtSenderName: TextView? = itemView.findViewById(R.id.txtSenderName)
        private val txtSenderInitial: TextView? = itemView.findViewById(R.id.txtSenderInitial)
        private val txtMessage: TextView? = itemView.findViewById(R.id.txtMessage)
        private val txtTime: TextView = itemView.findViewById(R.id.txtTime)
        private val imgStatus: ImageView? = itemView.findViewById(R.id.imgStatus)
        private val imgAttachment: ImageView? = itemView.findViewById(R.id.imgAttachment)
        private val layoutText: View? = itemView.findViewById(R.id.layoutText)
        private val layoutImageContainer: View? = itemView.findViewById(R.id.layoutImageContainer)
        private val txtImageCaption: TextView? = itemView.findViewById(R.id.txtImageCaption)
        private val layoutFileContainer: View? = itemView.findViewById(R.id.layoutFileContainer)
        private val txtFileTitle: TextView? = itemView.findViewById(R.id.txtFileTitle)
        private val txtFileSubtitle: TextView? = itemView.findViewById(R.id.txtFileSubtitle)
        private val layoutVoice: View? = itemView.findViewById(R.id.layoutVoice)
        private val txtVoiceDuration: TextView? = itemView.findViewById(R.id.txtVoiceDuration)
        private val txtFileName: TextView? = itemView.findViewById(R.id.txtFileName)
        private val imgPlayVideo: ImageView? = itemView.findViewById(R.id.imgPlayVideo)
        private val layoutUploadProgress: View? = itemView.findViewById(R.id.layoutUploadProgress)
        private val progressUpload: android.widget.ProgressBar? = itemView.findViewById(R.id.progressUpload)
        private val txtUploadPercent: TextView? = itemView.findViewById(R.id.txtUploadPercent)
        private val layoutError: View? = itemView.findViewById(R.id.layoutError)
        private val btnRetry: View? = itemView.findViewById(R.id.btnRetry)
        private val layoutFileError: View? = itemView.findViewById(R.id.layoutFileError)
        private val btnFileRetry: View? = itemView.findViewById(R.id.btnFileRetry)
        private val imgFileThumbnail: ImageView? = itemView.findViewById(R.id.imgFileThumbnail)
        private val txtFileCaption: TextView? = itemView.findViewById(R.id.txtFileCaption)
        private val btnFileOverflow: ImageView? = itemView.findViewById(R.id.btnFileOverflow)
        private val btnImageOverflow: ImageView? = itemView.findViewById(R.id.btnImageOverflow)
        private val imgFileIcon: ImageView? = itemView.findViewById(R.id.imgFileIcon)
        private val mediaLoader: ProgressBar? =
            itemView.findViewById(R.id.mediaLoader)


        fun updateVoiceTimer(seconds: Int) {
            txtVoiceDuration?.text = formatDuration(seconds)
        }

        fun bind(message: ChatMessage, position: Int) {
            bindSenderInfo(message)


            itemView.setOnClickListener {
                if (message.type == ChatMessageType.VOICE_NOTE) return@setOnClickListener

                if (message.status == MessageStatus.ERROR) onRetry(message)
                else onClick(message)
            }

            btnPlayVoice?.isFocusable = false
            btnPlayVoice?.isFocusableInTouchMode = false

            btnRetry?.setOnClickListener { onRetry(message) }
            btnFileRetry?.setOnClickListener { onRetry(message) }

            btnFileOverflow?.visibility = View.GONE
            btnImageOverflow?.visibility = View.GONE

            txtTime.text = message.timeLabel
            layoutText?.visibility = View.GONE

            layoutImageContainer?.visibility = View.GONE
            layoutFileContainer?.visibility = View.GONE
            layoutVoice?.visibility = View.GONE
            txtFileName?.visibility = View.GONE
            imgPlayVideo?.visibility = View.GONE
            layoutError?.visibility = View.GONE
            layoutFileError?.visibility = View.GONE

            imgStatus?.visibility = View.VISIBLE
            when (message.status) {
                MessageStatus.SENDING -> imgStatus?.setImageResource(R.drawable.tick_svgrepo_com) // Placeholder
                MessageStatus.SENT -> imgStatus?.setImageResource(R.drawable.tick_mark_delivered)
                MessageStatus.READ -> imgStatus?.setImageResource(R.drawable.read_status)
                MessageStatus.ERROR -> imgStatus?.visibility = View.GONE
            }

            when (message.type) {
                ChatMessageType.TEXT -> {
                    layoutText?.visibility = View.VISIBLE
                    txtMessage?.visibility = View.VISIBLE
                    txtMessage?.text = message.text
                }
                ChatMessageType.IMAGE, ChatMessageType.VIDEO -> {
                    layoutImageContainer?.visibility = View.VISIBLE

                    layoutError?.visibility = if (message.status == MessageStatus.ERROR) View.VISIBLE else View.GONE
                    btnImageOverflow?.visibility = View.VISIBLE
                    btnImageOverflow?.setOnClickListener { v -> showSavePopup(v, message) }
                    imgPlayVideo?.visibility = if (message.type == ChatMessageType.VIDEO) View.VISIBLE else View.GONE
                    val uri = message.attachmentUri
                    if (!uri.isNullOrEmpty()) {
                        if (message.type == ChatMessageType.VIDEO) {
                            loadVideoThumbnail(itemView.context, imgAttachment, uri)
                        } else {
                            loadImage(itemView.context, imgAttachment, uri)
                        }
                    }
                    val showCaption = !message.caption.isNullOrBlank()
                    txtImageCaption?.visibility = if (showCaption) View.VISIBLE else View.GONE
                    txtImageCaption?.text = message.caption

                    val showProgress = message.status == MessageStatus.SENDING && (message.uploadProgressPercent ?: 0) in 0..99
                    layoutUploadProgress?.visibility = if (showProgress) View.VISIBLE else View.GONE
                    val p = (message.uploadProgressPercent ?: 0).coerceIn(0, 100)
                    progressUpload?.progress = p
                    txtUploadPercent?.text = "$p%"
                }
                ChatMessageType.FILE -> {
                    layoutImageContainer?.visibility = View.GONE
                    layoutFileContainer?.visibility = View.VISIBLE
                    layoutFileError?.visibility = if (message.status == MessageStatus.ERROR) View.VISIBLE else View.GONE
                    btnFileOverflow?.visibility = View.VISIBLE
                    btnFileOverflow?.setOnClickListener { v -> showSavePopup(v, message) }
                    txtFileTitle?.text = message.fileName ?: "Document"
                    val isPdf = message.fileName?.endsWith(".pdf", ignoreCase = true) == true || message.mimeType?.contains("pdf") == true
                    txtFileSubtitle?.text = if (isPdf) "PDF" else "Document"
                    imgFileThumbnail?.visibility = View.GONE
                    imgFileThumbnail?.setImageDrawable(null)
                    val showFileCaption = !message.caption.isNullOrBlank()
                    txtFileCaption?.visibility = if (showFileCaption) View.VISIBLE else View.GONE
                    txtFileCaption?.text = message.caption

                    val showProgress = message.status == MessageStatus.SENDING && (message.uploadProgressPercent ?: 0) in 0..99
                    layoutFileError?.visibility = if (message.status == MessageStatus.ERROR) View.VISIBLE else View.GONE
                    // (File upload progress UI can be added later if needed)
                    val fileName = message.fileName ?: ""
                    val extension = fileName.substringAfterLast('.', "").lowercase()

                    val iconRes = when (extension) {
                        "pdf" -> R.drawable.file_pdf_icon
                        "doc", "docx" -> R.drawable.doc_icon
                        "xls", "xlsx" -> R.drawable.file_xls_color_red_icon_1__1_

                        else -> R.drawable.doc_icon
                    }

                    imgFileIcon?.setImageResource(iconRes)
                }
                ChatMessageType.VOICE_NOTE -> {

                    layoutVoice?.visibility = View.VISIBLE
                    layoutVoice?.isClickable = false
                    layoutVoice?.isFocusable = false

                    val isPlaying = adapter.playingPosition == position

                    if (isPlaying) {
                        btnPlayVoice?.setImageResource(R.drawable.pause)

                        val current = (adapter.mediaPlayer?.currentPosition ?: 0) / 1000
                        txtVoiceDuration?.text = formatDuration(current)

                    } else {
                        btnPlayVoice?.setImageResource(R.drawable.play_circle)
                        txtVoiceDuration?.text = formatDuration(message.durationSeconds ?: 0)
                    }

                    btnPlayVoice?.setOnClickListener {

                        if (adapter.playingPosition == position) {
                            adapter.stopAudio()
                        } else {
                            adapter.playAudio(message, position)
                        }
                    }
                }
                else -> {}
            }
        }

        private fun showSavePopup(anchor: View, message: ChatMessage) {
            val popup = PopupMenu(anchor.context, anchor)
            popup.menu.add(0, 1, 0, "Save")
            popup.setOnMenuItemClickListener {
                if (it.itemId == 1) {
                    onSaveMedia(message)
                    true
                } else false
            }
            popup.show()
        }

//        private fun loadImage(context: Context, imageView: ImageView?, uri: String) {
//            if (imageView == null) return
//            val loadUri = if (uri.startsWith("http")) Uri.parse(uri) else Uri.fromFile(File(uri))
//            Glide.with(context)
//                .load(loadUri)
//                .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(24)))
////                .placeholder(android.R.drawable.ic_menu_gallery)
//                .error(android.R.drawable.ic_dialog_alert)
//                .into(imageView)
//        }

        private fun loadImage(context: Context, imageView: ImageView?, uri: String) {

            mediaLoader?.visibility = View.VISIBLE

            val loadUri = if (uri.startsWith("http"))
                Uri.parse(uri)
            else
                Uri.fromFile(File(uri))

            Glide.with(context)
                .load(loadUri)
                .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(24)))
                .listener(object : RequestListener<Drawable> {

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        mediaLoader?.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        mediaLoader?.visibility = View.GONE
                        return false
                    }
                })
                .into(imageView!!)
        }

//        private fun loadVideoThumbnail(context: Context, imageView: ImageView?, uri: String) {
//            try {
//                val retriever = MediaMetadataRetriever()
//                if (uri.startsWith("http")) {
//                    retriever.setDataSource(uri, HashMap<String, String>())
//                } else {
//                    retriever.setDataSource(uri)
//                }
//                val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
//                imageView?.setImageBitmap(bitmap)
//                retriever.release()
//            } catch (_: Exception) {
//                imageView?.let { Glide.with(context).load(android.R.drawable.ic_media_play).into(it) }
//            }
//        }

        private fun loadVideoThumbnail(context: Context, imageView: ImageView?, uri: String) {

            mediaLoader?.visibility = View.VISIBLE

            val loadUri = if (uri.startsWith("http"))
                Uri.parse(uri)
            else
                Uri.fromFile(File(uri))

            Glide.with(context)
                .asBitmap()
                .load(loadUri)
                .frame(1000000)
                .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(24)))
                .listener(object : RequestListener<Bitmap> {

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>,
                        isFirstResource: Boolean
                    ): Boolean {
                        mediaLoader?.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        model: Any,
                        target: Target<Bitmap>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        mediaLoader?.visibility = View.GONE
                        return false
                    }
                })
                .into(imageView!!)
        }

        private fun formatDuration(seconds: Int): String {
            val m = seconds / 60
            val s = seconds % 60
            return "%02d:%02d".format(m, s)
        }

        private fun bindSenderInfo(message: ChatMessage) {
            val displayName = message.senderName?.takeIf { it.isNotBlank() }
                ?: if (message.isSender) "You" else "User"
            txtSenderName?.text = displayName
            txtSenderInitial?.text = displayName.firstOrNull()?.uppercase() ?: "?"
        }
    }

    class IncomingViewHolder(
        itemView: View,
        private val adapter: VirtualChatMessageAdapter,
        private val onClick: (ChatMessage) -> Unit,
        private val onSaveMedia: (ChatMessage) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val txtSenderName: TextView? = itemView.findViewById(R.id.txtSenderName)
        private val txtSenderInitial: TextView? = itemView.findViewById(R.id.txtSenderInitial)
        private val txtSenderRole: TextView? = itemView.findViewById(R.id.txtSenderRole)
        private val txtMessage: TextView? = itemView.findViewById(R.id.txtMessage)
        private val txtTime: TextView = itemView.findViewById(R.id.txtTime)
        private val imgAttachment: ImageView? = itemView.findViewById(R.id.imgAttachment)
        private val layoutText: View? = itemView.findViewById(R.id.layoutText)
        private val layoutImageContainer: View? = itemView.findViewById(R.id.layoutImageContainer)
        private val txtImageCaption: TextView? = itemView.findViewById(R.id.txtImageCaption)
        private val txtMediaSenderName: TextView? = itemView.findViewById(R.id.txtMediaSenderName)
        private val txtMediaSenderInitial: TextView? = itemView.findViewById(R.id.txtMediaSenderInitial)
        private val txtMediaSenderRole: TextView? = itemView.findViewById(R.id.txtMediaSenderRole)
        private val layoutFileContainer: View? = itemView.findViewById(R.id.layoutFileContainer)
        private val txtFileTitle: TextView? = itemView.findViewById(R.id.txtFileTitle)
        private val txtFileSubtitle: TextView? = itemView.findViewById(R.id.txtFileSubtitle)
        private val layoutVoice: View? = itemView.findViewById(R.id.layoutVoice)
        private val txtVoiceDuration: TextView? = itemView.findViewById(R.id.txtVoiceDuration)
        private val txtFileName: TextView? = itemView.findViewById(R.id.txtFileName)
        private val imgPlayVideo: ImageView? = itemView.findViewById(R.id.imgPlayVideo)
        private val imgFileThumbnail: ImageView? = itemView.findViewById(R.id.imgFileThumbnail)
        private val txtFileCaption: TextView? = itemView.findViewById(R.id.txtFileCaption)
        private val btnFileOverflow: ImageView? = itemView.findViewById(R.id.btnFileOverflow)
        private val btnImageOverflow: ImageView? = itemView.findViewById(R.id.btnImageOverflow)
        private val txtFileSenderName: TextView? = itemView.findViewById(R.id.txtFileSenderName)
        private val txtFileSenderInitial: TextView? = itemView.findViewById(R.id.txtFileSenderInitial)
        private val txtFileSenderRole: TextView? = itemView.findViewById(R.id.txtFileSenderRole)
        private val layoutMediaSenderRow: View? = itemView.findViewById(R.id.layoutMediaSenderRow)
        private val layoutFileSenderRow: View? = itemView.findViewById(R.id.layoutFileSenderRow)
        private val btnPlayVoice: ImageView? = itemView.findViewById(R.id.btnPlayVoice)
        private val imgFileIcon: ImageView? = itemView.findViewById(R.id.imgFileIcon)
        private val mediaLoader: ProgressBar? =
            itemView.findViewById(R.id.mediaLoader)


        fun updateVoiceTimer(seconds: Int) {
            txtVoiceDuration?.text = formatDuration(seconds)
        }


        fun bind(message: ChatMessage, position: Int) {
            itemView.setOnClickListener {
                if (message.type == ChatMessageType.VOICE_NOTE) return@setOnClickListener
                else onClick(message)
            }

            btnPlayVoice?.isFocusable = false
            btnPlayVoice?.isFocusableInTouchMode = false

            txtTime.text = message.timeLabel
            layoutText?.visibility = View.GONE
            layoutImageContainer?.visibility = View.GONE
            layoutFileContainer?.visibility = View.GONE
            layoutVoice?.visibility = View.GONE
            txtFileName?.visibility = View.GONE
            imgPlayVideo?.visibility = View.GONE
            btnFileOverflow?.visibility = View.GONE
            btnImageOverflow?.visibility = View.GONE

            when (message.type) {
                ChatMessageType.TEXT -> {
                    layoutText?.visibility = View.VISIBLE
                    bindSenderInfo(message)
                    txtMessage?.visibility = View.VISIBLE
                    txtMessage?.text = message.text
                }

                ChatMessageType.IMAGE, ChatMessageType.VIDEO -> {
                    layoutImageContainer?.visibility = View.VISIBLE
                    layoutMediaSenderRow?.visibility = View.VISIBLE
                    bindMediaSenderInfo(message)
                    btnImageOverflow?.visibility = View.VISIBLE
                    btnImageOverflow?.setOnClickListener { v -> showSavePopup(v, message) }
                    imgPlayVideo?.visibility =
                        if (message.type == ChatMessageType.VIDEO) View.VISIBLE else View.GONE
                    val uri = message.attachmentUri
                    if (!uri.isNullOrEmpty()) {
                        if (message.type == ChatMessageType.VIDEO) {
                            loadVideoThumbnail(itemView.context, imgAttachment, uri)
                        } else {
                            loadImage(itemView.context, imgAttachment, uri)
                        }
                    }
                    val showCaption = !message.caption.isNullOrBlank()
                    txtImageCaption?.visibility = if (showCaption) View.VISIBLE else View.GONE
                    txtImageCaption?.text = message.caption
                }

                ChatMessageType.FILE -> {
                    layoutImageContainer?.visibility = View.GONE
                    layoutText?.visibility = View.GONE
                    layoutFileContainer?.visibility = View.VISIBLE
                    layoutFileSenderRow?.visibility = View.VISIBLE
                    bindFileSenderInfo(message)
                    btnFileOverflow?.visibility = View.VISIBLE
                    btnFileOverflow?.setOnClickListener { v -> showSavePopup(v, message) }
                    txtFileTitle?.text = message.fileName ?: "Document"
                    val isPdf = message.fileName?.endsWith(
                        ".pdf",
                        ignoreCase = true
                    ) == true || message.mimeType?.contains("pdf") == true
                    txtFileSubtitle?.text = if (isPdf) "PDF" else "Document"
                    imgFileThumbnail?.visibility = View.GONE
                    imgFileThumbnail?.setImageDrawable(null)
                    val showFileCaption = !message.caption.isNullOrBlank()
                    txtFileCaption?.visibility = if (showFileCaption) View.VISIBLE else View.GONE
                    txtFileCaption?.text = message.caption

                    val fileName = message.fileName ?: ""
                    val extension = fileName.substringAfterLast('.', "").lowercase()

                    val iconRes = when (extension) {
                        "pdf" -> R.drawable.file_pdf_icon
                        "doc", "docx" -> R.drawable.doc_icon
                        "xls", "xlsx" -> R.drawable.file_xls_color_red_icon_1__1_

                        else -> R.drawable.doc_icon
                    }
                    imgFileIcon?.setImageResource(iconRes)

                }
                ChatMessageType.VOICE_NOTE -> {

                    layoutVoice?.visibility = View.VISIBLE
                    layoutVoice?.isClickable = false
                    layoutVoice?.isFocusable = false

                    val isPlaying = adapter.playingPosition == position

                    if (isPlaying) {
                        btnPlayVoice?.setImageResource(R.drawable.pause)

                        val current = (adapter.mediaPlayer?.currentPosition ?: 0) / 1000
                        txtVoiceDuration?.text = formatDuration(current)

                    } else {
                        btnPlayVoice?.setImageResource(R.drawable.play_circle)
                        txtVoiceDuration?.text = formatDuration(message.durationSeconds ?: 0)
                    }

                    btnPlayVoice?.setOnClickListener {

                        if (adapter.playingPosition == position) {
                            adapter.stopAudio()
                        } else {
                            adapter.playAudio(message, position)
                        }
                    }
                }

                else -> {}
            }
        }

        private fun showSavePopup(anchor: View, message: ChatMessage) {
            val popup = PopupMenu(anchor.context, anchor)
            popup.menu.add(0, 1, 0, "Save")
            popup.setOnMenuItemClickListener {
                if (it.itemId == 1) {
                    onSaveMedia(message)
                    true
                } else false
            }
            popup.show()
        }

//        private fun loadImage(context: Context, imageView: ImageView?, uri: String) {
//            if (imageView == null) return
//            val loadUri = if (uri.startsWith("http")) Uri.parse(uri) else Uri.fromFile(File(uri))
//            Glide.with(context)
//                .load(loadUri)
//                .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(24)))
////                .placeholder(android.R.drawable.ic_menu_gallery)
//                .error(android.R.drawable.ic_dialog_alert)
//                .into(imageView)
//        }


        private fun loadImage(context: Context, imageView: ImageView?, uri: String) {

            mediaLoader?.visibility = View.VISIBLE

            val loadUri = if (uri.startsWith("http"))
                Uri.parse(uri)
            else
                Uri.fromFile(File(uri))

            Glide.with(context)
                .load(loadUri)
                .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(24)))
                .listener(object : RequestListener<Drawable> {

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        mediaLoader?.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        mediaLoader?.visibility = View.GONE
                        return false
                    }
                })
                .into(imageView!!)
        }

//        private fun loadVideoThumbnail(context: Context, imageView: ImageView?, uri: String) {
//            try {
//                val retriever = MediaMetadataRetriever()
//                if (uri.startsWith("http")) {
//                    retriever.setDataSource(uri, HashMap<String, String>())
//                } else {
//                    retriever.setDataSource(uri)
//                }
//                val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
//                imageView?.setImageBitmap(bitmap)
//                retriever.release()
//            } catch (_: Exception) {
//                imageView?.let { Glide.with(context).load(android.R.drawable.ic_media_play).into(it) }
//            }
//        }

        private fun loadVideoThumbnail(context: Context, imageView: ImageView?, uri: String) {

            mediaLoader?.visibility = View.VISIBLE

            val loadUri = if (uri.startsWith("http"))
                Uri.parse(uri)
            else
                Uri.fromFile(File(uri))

            Glide.with(context)
                .asBitmap()
                .load(loadUri)
                .frame(1000000)
                .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(24)))
                .listener(object : RequestListener<Bitmap> {

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>,
                        isFirstResource: Boolean
                    ): Boolean {
                        mediaLoader?.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        model: Any,
                        target: Target<Bitmap>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        mediaLoader?.visibility = View.GONE
                        return false
                    }
                })
                .into(imageView!!)
        }
        private fun formatDuration(seconds: Int): String {
            val m = seconds / 60
            val s = seconds % 60
            return "%02d:%02d".format(m, s)
        }

        private fun bindSenderInfo(message: ChatMessage) {
            val displayName = message.senderName?.takeIf { it.isNotBlank() }
                ?: if (message.isSender) "You" else "User"
            txtSenderName?.text = displayName
            txtSenderInitial?.text = displayName.firstOrNull()?.uppercase() ?: "?"
            val role = message.senderRoleAbbrev?.takeIf { it.isNotBlank() }
            txtSenderRole?.visibility = if (role != null) View.VISIBLE else View.GONE
            txtSenderRole?.text = role
        }

        private fun bindMediaSenderInfo(message: ChatMessage) {
            val displayName = message.senderName?.takeIf { it.isNotBlank() } ?: "User"
            txtMediaSenderName?.text = displayName
            txtMediaSenderInitial?.text = displayName.firstOrNull()?.uppercase() ?: "?"
            val role = message.senderRoleAbbrev?.takeIf { it.isNotBlank() }
            txtMediaSenderRole?.visibility = if (role != null) View.VISIBLE else View.GONE
            txtMediaSenderRole?.text = role
        }

        private fun bindFileSenderInfo(message: ChatMessage) {
            val displayName = message.senderName?.takeIf { it.isNotBlank() } ?: "User"
            txtFileSenderName?.text = displayName
            txtFileSenderInitial?.text = displayName.firstOrNull()?.uppercase() ?: "?"
            val role = message.senderRoleAbbrev?.takeIf { it.isNotBlank() }
            txtFileSenderRole?.visibility = if (role != null) View.VISIBLE else View.GONE
            txtFileSenderRole?.text = role
        }
    }

    class EstimationOutgoingViewHolder(itemView: View, private val listener: EstimationInteractionListener?) : RecyclerView.ViewHolder(itemView) {
        private val userNameTv: TextView = itemView.findViewById(R.id.user_name_tv)
        private val rvPartList: RecyclerView = itemView.findViewById(R.id.rv_estimation_part_list)
        private val rvLabourList: RecyclerView = itemView.findViewById(R.id.rv_estimation_labour_list)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_estimate_status)
        private val tvTimeStamp: TextView = itemView.findViewById(R.id.tv_time_stamp_self)




        fun bind(message: ChatMessage, position: Int) {
            val context = itemView.context
            userNameTv.text = if (message.isSender) "You" else "Service Advisor"
            tvTimeStamp.text = message.timeLabel

            val details = message.estimationDetails ?: return




            if (listener != null) {
                val partAdapter = PartListAdapter(context, details.part_list, position, listener, false, false)
                rvPartList.adapter = partAdapter
                rvPartList.layoutManager = LinearLayoutManager(context)

                val labourAdapter = LabourListAdapter(context, details.labour_list, position, listener, false, false)
                rvLabourList.adapter = labourAdapter
                rvLabourList.layoutManager = LinearLayoutManager(context)
            }

            if (!details.estimationApprovalStatus.isNullOrEmpty()) {
                tvStatus.visibility = View.VISIBLE
                if (details.estimationApprovalStatus == "Y") {
                    tvStatus.text = "Approved"
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green))
                } else {
                    tvStatus.text = "Rejected"
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.text_colour1))
                }
            } else {
                tvStatus.visibility = View.GONE
            }
        }
    }

    class EstimationIncomingViewHolder(itemView: View, private val listener: EstimationInteractionListener?) : RecyclerView.ViewHolder(itemView) {
        private val userNameTv: TextView = itemView.findViewById(R.id.remote_user_name_tv)
        private val rvPartList: RecyclerView = itemView.findViewById(R.id.rv_receiver_estimation_part_list)
        private val rvLabourList: RecyclerView = itemView.findViewById(R.id.rv_receiver_estimation_labour_list)
        private val tvTimeStamp: TextView = itemView.findViewById(R.id.tv_remote_time_stamp)
        private val tvGrandTotal: TextView = itemView.findViewById(R.id.tv_grand_total_value)
        private val btnApprove: TextView = itemView.findViewById(R.id.btn_approve_estimation)
        private val btnReject: TextView = itemView.findViewById(R.id.btn_reject_estimation)
        private val layoutApproval: View = itemView.findViewById(R.id.rv_estimation_approval_layout)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_estimate_status_receiver)
        private val checkboxSelectAll: android.widget.CheckBox = itemView.findViewById(R.id.checkbox_select_all)

        fun bind(message: ChatMessage, position: Int) {
            val context = itemView.context
            userNameTv.text = "Service Advisor"
            tvTimeStamp.text = message.timeLabel

            val details = message.estimationDetails ?: return
            tvGrandTotal.text = details.selectedItemsTotal.toString()

            if (listener != null) {
                val partAdapter = PartListAdapter(context, details.part_list, position, listener, null, details.estimationApprovalStatus == null)
                rvPartList.adapter = partAdapter
                rvPartList.layoutManager = LinearLayoutManager(context)

                val labourAdapter = LabourListAdapter(context, details.labour_list, position, listener, null, details.estimationApprovalStatus == null)
                rvLabourList.adapter = labourAdapter
                rvLabourList.layoutManager = LinearLayoutManager(context)

                checkboxSelectAll.isChecked = details.areAllItemsSelected
                checkboxSelectAll.isEnabled = details.estimationApprovalStatus == null
                checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
                    listener.onSelectAllClicked(position, isChecked, details)
                }

                btnApprove.setOnClickListener {
                    listener.onAcceptClicked(position, details)
                }
                btnReject.setOnClickListener {
                    listener.onRejectClicked(position, details)
                }
            }

            if (!details.estimationApprovalStatus.isNullOrEmpty()) {
                layoutApproval.visibility = View.GONE
                tvStatus.visibility = View.VISIBLE
                if (details.estimationApprovalStatus == "Y") {
                    tvStatus.text = "Approved"
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green))
                } else {
                    tvStatus.text = "Rejected"
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.text_colour1))
                }
            } else {
                layoutApproval.visibility = if (PreferenceManager.getuserType() == "customer") View.VISIBLE else View.GONE
                tvStatus.visibility = View.GONE
            }
        }
    }
}
