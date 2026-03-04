package com.app.vc.virtualchatroom

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.R
import com.app.vc.views.WaveformView
import java.io.File

enum class ChatMessageType { TEXT, IMAGE, FILE, VIDEO, VOICE_NOTE }
enum class MessageStatus { SENDING, SENT, READ, ERROR }

data class ChatMessage(
    val messageId: String? = null,
    val text: String,
    val isSender: Boolean,
    val timeLabel: String,
    var status: MessageStatus = MessageStatus.SENT,
    val type: ChatMessageType = ChatMessageType.TEXT,
    val attachmentUri: String? = null,
    val durationSeconds: Int? = null,
    val fileName: String? = null,
    val caption: String? = null,
    val waveformData: String? = null,
    val mimeType: String? = null
)

class VirtualChatMessageAdapter(
    private val messages: MutableList<ChatMessage>,
    private val onRetryClick: (ChatMessage) -> Unit,
    private val onItemClick: (ChatMessage) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_OUTGOING = 1
        private const val VIEW_TYPE_INCOMING = 2

        fun parseWaveformData(waveformData: String?): FloatArray {
            if (waveformData.isNullOrBlank()) return floatArrayOf()
            return waveformData.split(",").mapNotNull { it.trim().toFloatOrNull() }.toFloatArray()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isSender) VIEW_TYPE_OUTGOING else VIEW_TYPE_INCOMING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_OUTGOING) {
            val view = inflater.inflate(R.layout.vc_item_chat_message_outgoing, parent, false)
            OutgoingViewHolder(view, onRetryClick, onItemClick)
        } else {
            val view = inflater.inflate(R.layout.vc_item_chat_message_incoming, parent, false)
            IncomingViewHolder(view, onItemClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is OutgoingViewHolder) {
            holder.bind(message)
        } else if (holder is IncomingViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
    
    fun updateMessageStatus(messageId: String, newStatus: MessageStatus) {
        val index = if (messageId.isNotEmpty()) {
            messages.indexOfLast { it.messageId == messageId }
        } else {
            messages.indexOfLast { it.isSender }
        }
        
        if (index != -1) {
            messages[index].status = newStatus
            notifyItemChanged(index)
        }
    }

    class OutgoingViewHolder(
        itemView: View, 
        private val onRetry: (ChatMessage) -> Unit,
        private val onClick: (ChatMessage) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val txtMessage: TextView? = itemView.findViewById(R.id.txtMessage)
        private val txtTime: TextView = itemView.findViewById(R.id.txtTime)
        private val imgStatus: ImageView? = itemView.findViewById(R.id.imgStatus)
        private val imgAttachment: ImageView? = itemView.findViewById(R.id.imgAttachment)
        private val layoutText: View? = itemView.findViewById(R.id.layoutText)
        private val layoutImageContainer: View? = itemView.findViewById(R.id.layoutImageContainer)
        private val txtImageCaption: TextView? = itemView.findViewById(R.id.txtImageCaption)
        private val layoutVoice: View? = itemView.findViewById(R.id.layoutVoice)
        private val txtVoiceDuration: TextView? = itemView.findViewById(R.id.txtVoiceDuration)
        private val txtFileName: TextView? = itemView.findViewById(R.id.txtFileName)
        private val imgPlayVideo: ImageView? = itemView.findViewById(R.id.imgPlayVideo)
        private val layoutError: View? = itemView.findViewById(R.id.layoutError)
        private val btnRetry: View? = itemView.findViewById(R.id.btnRetry)

        fun bind(message: ChatMessage) {
            itemView.setOnClickListener { onClick(message) }
            btnRetry?.setOnClickListener { onRetry(message) }
            
            txtTime.text = message.timeLabel
            layoutText?.visibility = View.GONE
            layoutImageContainer?.visibility = View.GONE
            layoutVoice?.visibility = View.GONE
            txtFileName?.visibility = View.GONE
            imgPlayVideo?.visibility = View.GONE
            layoutError?.visibility = if (message.status == MessageStatus.ERROR) View.VISIBLE else View.GONE
            
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
                    imgPlayVideo?.visibility = if (message.type == ChatMessageType.VIDEO) View.VISIBLE else View.GONE
                    val uri = message.attachmentUri
                    if (!uri.isNullOrEmpty()) {
                        if (message.type == ChatMessageType.VIDEO) {
                            loadVideoThumbnail(imgAttachment, uri)
                        } else {
                            try {
                                val u = if (uri.startsWith("http")) Uri.parse(uri) else Uri.fromFile(File(uri))
                                imgAttachment?.setImageURI(u)
                            } catch (_: Exception) {}
                        }
                    }
                    txtImageCaption?.visibility = if (message.caption.isNullOrBlank()) View.GONE else View.VISIBLE
                    txtImageCaption?.text = message.caption
                }
                ChatMessageType.FILE -> {
                    layoutText?.visibility = View.VISIBLE
                    txtFileName?.visibility = View.VISIBLE
                    txtFileName?.text = message.fileName ?: "Document.pdf"
                    txtMessage?.visibility = if (message.caption.isNullOrBlank()) View.GONE else View.VISIBLE
                    txtMessage?.text = message.caption
                }
                ChatMessageType.VOICE_NOTE -> {
                    layoutVoice?.visibility = View.VISIBLE
                    txtVoiceDuration?.text = formatDuration(message.durationSeconds ?: 0)
                }
            }
        }

        private fun loadVideoThumbnail(imageView: ImageView?, uri: String) {
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
            } catch (_: Exception) {}
        }

        private fun formatDuration(seconds: Int): String {
            val m = seconds / 60
            val s = seconds % 60
            return "%02d:%02d".format(m, s)
        }
    }

    class IncomingViewHolder(itemView: View, private val onClick: (ChatMessage) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val txtMessage: TextView? = itemView.findViewById(R.id.txtMessage)
        private val txtTime: TextView = itemView.findViewById(R.id.txtTime)
        private val imgAttachment: ImageView? = itemView.findViewById(R.id.imgAttachment)
        private val layoutText: View? = itemView.findViewById(R.id.layoutText)
        private val layoutImageContainer: View? = itemView.findViewById(R.id.layoutImageContainer)
        private val txtImageCaption: TextView? = itemView.findViewById(R.id.txtImageCaption)
        private val layoutVoice: View? = itemView.findViewById(R.id.layoutVoice)
        private val txtVoiceDuration: TextView? = itemView.findViewById(R.id.txtVoiceDuration)
        private val txtFileName: TextView? = itemView.findViewById(R.id.txtFileName)
        private val imgPlayVideo: ImageView? = itemView.findViewById(R.id.imgPlayVideo)

        fun bind(message: ChatMessage) {
            itemView.setOnClickListener { onClick(message) }
            txtTime.text = message.timeLabel
            layoutText?.visibility = View.GONE
            layoutImageContainer?.visibility = View.GONE
            layoutVoice?.visibility = View.GONE
            txtFileName?.visibility = View.GONE
            imgPlayVideo?.visibility = View.GONE

            when (message.type) {
                ChatMessageType.TEXT -> {
                    layoutText?.visibility = View.VISIBLE
                    txtMessage?.visibility = View.VISIBLE
                    txtMessage?.text = message.text
                }
                ChatMessageType.IMAGE, ChatMessageType.VIDEO -> {
                    layoutImageContainer?.visibility = View.VISIBLE
                    imgPlayVideo?.visibility = if (message.type == ChatMessageType.VIDEO) View.VISIBLE else View.GONE
                    val uri = message.attachmentUri
                    if (!uri.isNullOrEmpty()) {
                        if (message.type == ChatMessageType.VIDEO) {
                            loadVideoThumbnail(imgAttachment, uri)
                        } else {
                            try {
                                val u = if (uri.startsWith("http")) Uri.parse(uri) else Uri.fromFile(File(uri))
                                imgAttachment?.setImageURI(u)
                            } catch (_: Exception) {}
                        }
                    }
                    txtImageCaption?.visibility = if (message.caption.isNullOrBlank()) View.GONE else View.VISIBLE
                    txtImageCaption?.text = message.caption
                }
                ChatMessageType.FILE -> {
                    layoutText?.visibility = View.VISIBLE
                    txtFileName?.visibility = View.VISIBLE
                    txtFileName?.text = message.fileName ?: "Document.pdf"
                    txtMessage?.visibility = if (message.caption.isNullOrBlank()) View.GONE else View.VISIBLE
                    txtMessage?.text = message.caption
                }
                ChatMessageType.VOICE_NOTE -> {
                    layoutVoice?.visibility = View.VISIBLE
                    txtVoiceDuration?.text = formatDuration(message.durationSeconds ?: 0)
                }
            }
        }

        private fun loadVideoThumbnail(imageView: ImageView?, uri: String) {
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
            } catch (_: Exception) {}
        }

        private fun formatDuration(seconds: Int): String {
            val m = seconds / 60
            val s = seconds % 60
            return "%02d:%02d".format(m, s)
        }
    }
}
