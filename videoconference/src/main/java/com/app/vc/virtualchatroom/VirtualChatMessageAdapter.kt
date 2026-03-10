package com.app.vc.virtualchatroom

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
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

enum class ChatMessageType { TEXT, IMAGE, FILE, VIDEO, VOICE_NOTE, ESTIMATION }
enum class MessageStatus { SENDING, SENT, READ, ERROR }

data class ChatMessage(
    var messageId: String? = null,
    val text: String,
    val isSender: Boolean,
    var senderName: String? = null,
    val senderUsername: String? = null,
    val timeLabel: String,
    var status: MessageStatus = MessageStatus.SENT,
    val type: ChatMessageType = ChatMessageType.TEXT,
    var attachmentUri: String? = null,
    val durationSeconds: Int? = null,
    val fileName: String? = null,
    val caption: String? = null,
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

    companion object {
        private const val VIEW_TYPE_OUTGOING = 1
        private const val VIEW_TYPE_INCOMING = 2
        private const val VIEW_TYPE_ESTIMATION_OUTGOING = 3
        private const val VIEW_TYPE_ESTIMATION_INCOMING = 4

        fun parseWaveformData(waveformData: String?): FloatArray {
            if (waveformData.isNullOrBlank()) return floatArrayOf()
            return waveformData.split(",").mapNotNull { it.trim().toFloatOrNull() }.toFloatArray()
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.type == ChatMessageType.ESTIMATION) {
            if (message.isSender) VIEW_TYPE_ESTIMATION_OUTGOING else VIEW_TYPE_ESTIMATION_INCOMING
        } else {
            if (message.isSender) VIEW_TYPE_OUTGOING else VIEW_TYPE_INCOMING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_OUTGOING -> {
                val view = inflater.inflate(R.layout.vc_item_chat_message_outgoing, parent, false)
                OutgoingViewHolder(view, onRetryClick, onItemClick, onSaveMedia)
            }
            VIEW_TYPE_INCOMING -> {
                val view = inflater.inflate(R.layout.vc_item_chat_message_incoming, parent, false)
                IncomingViewHolder(view, onItemClick, onSaveMedia)
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
            is OutgoingViewHolder -> holder.bind(message)
            is IncomingViewHolder -> holder.bind(message)
            is EstimationOutgoingViewHolder -> holder.bind(message, position)
            is EstimationIncomingViewHolder -> holder.bind(message, position)
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemId(position: Int): Long {
        val id = messages.getOrNull(position)?.messageId ?: position.toString()
        return id.hashCode().toLong()
    }

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
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
        private val onRetry: (ChatMessage) -> Unit,
        private val onClick: (ChatMessage) -> Unit,
        private val onSaveMedia: (ChatMessage) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
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
        private val layoutError: View? = itemView.findViewById(R.id.layoutError)
        private val btnRetry: View? = itemView.findViewById(R.id.btnRetry)
        private val layoutFileError: View? = itemView.findViewById(R.id.layoutFileError)
        private val btnFileRetry: View? = itemView.findViewById(R.id.btnFileRetry)
        private val imgFileThumbnail: ImageView? = itemView.findViewById(R.id.imgFileThumbnail)
        private val txtFileCaption: TextView? = itemView.findViewById(R.id.txtFileCaption)
        private val btnFileOverflow: ImageView? = itemView.findViewById(R.id.btnFileOverflow)
        private val btnImageOverflow: ImageView? = itemView.findViewById(R.id.btnImageOverflow)

        fun bind(message: ChatMessage) {
            bindSenderInfo(message)
            itemView.setOnClickListener {
                if (message.status == MessageStatus.ERROR) onRetry(message) else onClick(message)
            }
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
                MessageStatus.SENDING -> imgStatus?.setImageResource(R.drawable.ic_back) // Placeholder
                MessageStatus.SENT -> imgStatus?.setImageResource(R.drawable.ic_status_sent)
                MessageStatus.READ -> imgStatus?.setImageResource(R.drawable.ic_status_read)
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
                }
                ChatMessageType.FILE -> {
                    layoutImageContainer?.visibility = View.GONE
                    layoutText?.visibility = View.GONE
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
                }
                ChatMessageType.VOICE_NOTE -> {
                    layoutVoice?.visibility = View.VISIBLE
                    txtVoiceDuration?.text = formatDuration(message.durationSeconds ?: 0)
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

        private fun loadImage(context: Context, imageView: ImageView?, uri: String) {
            if (imageView == null) return
            val loadUri = if (uri.startsWith("http")) Uri.parse(uri) else Uri.fromFile(File(uri))
            Glide.with(context)
                .load(loadUri)
                .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(24)))
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_dialog_alert)
                .into(imageView)
        }

        private fun loadVideoThumbnail(context: Context, imageView: ImageView?, uri: String) {
            try {
                val retriever = MediaMetadataRetriever()
                if (uri.startsWith("http")) {
                    retriever.setDataSource(uri, HashMap<String, String>())
                } else {
                    retriever.setDataSource(uri)
                }
                val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                imageView?.setImageBitmap(bitmap)
                retriever.release()
            } catch (_: Exception) {
                imageView?.let { Glide.with(context).load(android.R.drawable.ic_media_play).into(it) }
            }
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
        private val onClick: (ChatMessage) -> Unit,
        private val onSaveMedia: (ChatMessage) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val txtSenderName: TextView? = itemView.findViewById(R.id.txtSenderName)
        private val txtSenderInitial: TextView? = itemView.findViewById(R.id.txtSenderInitial)
        private val txtMessage: TextView? = itemView.findViewById(R.id.txtMessage)
        private val txtTime: TextView = itemView.findViewById(R.id.txtTime)
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
        private val imgFileThumbnail: ImageView? = itemView.findViewById(R.id.imgFileThumbnail)
        private val txtFileCaption: TextView? = itemView.findViewById(R.id.txtFileCaption)
        private val btnFileOverflow: ImageView? = itemView.findViewById(R.id.btnFileOverflow)
        private val btnImageOverflow: ImageView? = itemView.findViewById(R.id.btnImageOverflow)

        fun bind(message: ChatMessage) {
            bindSenderInfo(message)
            itemView.setOnClickListener { onClick(message) }
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
                    txtMessage?.visibility = View.VISIBLE
                    txtMessage?.text = message.text
                }
                ChatMessageType.IMAGE, ChatMessageType.VIDEO -> {
                    layoutImageContainer?.visibility = View.VISIBLE
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
                }
                ChatMessageType.FILE -> {
                    layoutImageContainer?.visibility = View.GONE
                    layoutText?.visibility = View.GONE
                    layoutFileContainer?.visibility = View.VISIBLE
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
                }
                ChatMessageType.VOICE_NOTE -> {
                    layoutVoice?.visibility = View.VISIBLE
                    txtVoiceDuration?.text = formatDuration(message.durationSeconds ?: 0)
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

        private fun loadImage(context: Context, imageView: ImageView?, uri: String) {
            if (imageView == null) return
            val loadUri = if (uri.startsWith("http")) Uri.parse(uri) else Uri.fromFile(File(uri))
            Glide.with(context)
                .load(loadUri)
                .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(24)))
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_dialog_alert)
                .into(imageView)
        }

        private fun loadVideoThumbnail(context: Context, imageView: ImageView?, uri: String) {
            try {
                val retriever = MediaMetadataRetriever()
                if (uri.startsWith("http")) {
                    retriever.setDataSource(uri, HashMap<String, String>())
                } else {
                    retriever.setDataSource(uri)
                }
                val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                imageView?.setImageBitmap(bitmap)
                retriever.release()
            } catch (_: Exception) {
                imageView?.let { Glide.with(context).load(android.R.drawable.ic_media_play).into(it) }
            }
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
