package com.app.vc.virtualchatroom

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.R
import com.app.vc.views.WaveformView
import java.io.File

enum class ChatMessageType { TEXT, IMAGE, FILE, VOICE_NOTE }
enum class MessageStatus { SENDING, SENT, READ }

data class ChatMessage(
    val text: String,
    val isSender: Boolean,
    val timeLabel: String,
    var status: MessageStatus = MessageStatus.SENT,
    val type: ChatMessageType = ChatMessageType.TEXT,
    val attachmentUri: String? = null,
    val durationSeconds: Int? = null,
    val fileName: String? = null,
    val caption: String? = null,
    val waveformData: String? = null
)

class VirtualChatMessageAdapter(
    private val messages: MutableList<ChatMessage>
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
            OutgoingViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.vc_item_chat_message_incoming, parent, false)
            IncomingViewHolder(view)
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
        // In a real app, you'd find the message by a unique ID
        // For this demo, we'll just update the last sent message
        val lastSentMessage = messages.lastOrNull { it.isSender }
        if (lastSentMessage != null) {
            lastSentMessage.status = newStatus
            notifyItemChanged(messages.lastIndexOf(lastSentMessage))
        }
    }


    class OutgoingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtMessage: TextView? = itemView.findViewById(R.id.txtMessage)
        private val txtTime: TextView = itemView.findViewById(R.id.txtTime)
        private val imgStatus: ImageView? = itemView.findViewById(R.id.imgStatus)
        private val imgAttachment: ImageView? = itemView.findViewById(R.id.imgAttachment)
        private val layoutText: View? = itemView.findViewById(R.id.layoutText)
        private val layoutImageContainer: View? = itemView.findViewById(R.id.layoutImageContainer)
        private val txtImageCaption: TextView? = itemView.findViewById(R.id.txtImageCaption)
        private val layoutVoice: View? = itemView.findViewById(R.id.layoutVoice)
        private val txtVoiceDuration: TextView? = itemView.findViewById(R.id.txtVoiceDuration)
        private val voiceWaveformView: WaveformView? = itemView.findViewById(R.id.voiceWaveformView)
        private val txtFileName: TextView? = itemView.findViewById(R.id.txtFileName)

        fun bind(message: ChatMessage) {
            txtTime.text = message.timeLabel
            layoutText?.visibility = View.GONE
            layoutImageContainer?.visibility = View.GONE
            layoutVoice?.visibility = View.GONE
            txtFileName?.visibility = View.GONE
            
            // Update status indicator
            imgStatus?.visibility = View.VISIBLE
            when (message.status) {
                MessageStatus.SENDING -> {
//                    imgStatus?.setImageResource(R.drawableble.ic_status_sending) // Replace with your sending icon
                }
                MessageStatus.SENT -> {
                    imgStatus?.setImageResource(R.drawable.ic_status_sent) // Replace with your sent icon
                }
                MessageStatus.READ -> {
                    imgStatus?.setImageResource(R.drawable.ic_status_read) // Replace with your read icon
                }
            }

            when (message.type) {
                ChatMessageType.TEXT -> {
                    layoutText?.visibility = View.VISIBLE
                    txtMessage?.visibility = View.VISIBLE
                    txtMessage?.text = message.text
                }
                ChatMessageType.IMAGE -> {
                    layoutImageContainer?.visibility = View.VISIBLE
                    val uri = message.attachmentUri
                    if (!uri.isNullOrEmpty()) {
                        try {
                            val u = if (File(uri).exists()) Uri.fromFile(File(uri)) else Uri.parse(uri)
                            imgAttachment?.setImageURI(u)
                        } catch (_: Exception) {}
                    }
                    val cap = message.caption
                    if (!cap.isNullOrBlank()) {
                        txtImageCaption?.visibility = View.VISIBLE
                        txtImageCaption?.text = cap
                    } else {
                        txtImageCaption?.visibility = View.GONE
                    }
                }
                ChatMessageType.FILE -> {
                    layoutText?.visibility = View.VISIBLE
                    txtFileName?.visibility = View.VISIBLE
                    txtFileName?.text = message.fileName ?: "File"
                    if (!message.caption.isNullOrBlank()) {
                        txtMessage?.visibility = View.VISIBLE
                        txtMessage?.text = message.caption
                    } else {
                        txtMessage?.visibility = View.GONE
                    }
                }
                ChatMessageType.VOICE_NOTE -> {
                    layoutVoice?.visibility = View.VISIBLE
                    txtVoiceDuration?.text = formatDuration(message.durationSeconds ?: 0)
                    val amps = parseWaveformData(message.waveformData)
                    if (amps.isNotEmpty()) {
                        voiceWaveformView?.visibility = View.VISIBLE
                    } else {
                        voiceWaveformView?.visibility = View.GONE
                    }
                }
            }
        }

        private fun formatDuration(seconds: Int): String {
            val m = seconds / 60
            val s = seconds % 60
            return "%02d:%02d".format(m, s)
        }
    }

    class IncomingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtMessage: TextView? = itemView.findViewById(R.id.txtMessage)
        private val txtTime: TextView = itemView.findViewById(R.id.txtTime)
        private val imgAttachment: ImageView? = itemView.findViewById(R.id.imgAttachment)
        private val layoutText: View? = itemView.findViewById(R.id.layoutText)
        private val layoutImageContainer: View? = itemView.findViewById(R.id.layoutImageContainer)
        private val txtImageCaption: TextView? = itemView.findViewById(R.id.txtImageCaption)
        private val layoutVoice: View? = itemView.findViewById(R.id.layoutVoice)
        private val txtVoiceDuration: TextView? = itemView.findViewById(R.id.txtVoiceDuration)
        private val voiceWaveformView: WaveformView? = itemView.findViewById(R.id.voiceWaveformView)
        private val txtFileName: TextView? = itemView.findViewById(R.id.txtFileName)

        fun bind(message: ChatMessage) {
            txtTime.text = message.timeLabel
            layoutText?.visibility = View.GONE
            layoutImageContainer?.visibility = View.GONE
            layoutVoice?.visibility = View.GONE
            txtFileName?.visibility = View.GONE
            when (message.type) {
                ChatMessageType.TEXT -> {
                    layoutText?.visibility = View.VISIBLE
                    txtMessage?.visibility = View.VISIBLE
                    txtMessage?.text = message.text
                }
                ChatMessageType.IMAGE -> {
                    layoutImageContainer?.visibility = View.VISIBLE
                    val uri = message.attachmentUri
                    if (!uri.isNullOrEmpty()) {
                        try {
                            val u = if (File(uri).exists()) Uri.fromFile(File(uri)) else Uri.parse(uri)
                            imgAttachment?.setImageURI(u)
                        } catch (_: Exception) {}
                    }
                    val cap = message.caption
                    if (!cap.isNullOrBlank()) {
                        txtImageCaption?.visibility = View.VISIBLE
                        txtImageCaption?.text = cap
                    } else {
                        txtImageCaption?.visibility = View.GONE
                    }
                }
                ChatMessageType.FILE -> {
                    layoutText?.visibility = View.VISIBLE
                    txtFileName?.visibility = View.VISIBLE
                    txtFileName?.text = message.fileName ?: "File"
                    if (!message.caption.isNullOrBlank()) {
                        txtMessage?.visibility = View.VISIBLE
                        txtMessage?.text = message.caption
                    } else {
                        txtMessage?.visibility = View.GONE
                    }
                }
                ChatMessageType.VOICE_NOTE -> {
                    layoutVoice?.visibility = View.VISIBLE
                    txtVoiceDuration?.text = formatDuration(message.durationSeconds ?: 0)
                    val amps = parseWaveformData(message.waveformData)
                    if (amps.isNotEmpty()) {
                        voiceWaveformView?.visibility = View.VISIBLE
                    } else {
                        voiceWaveformView?.visibility = View.GONE
                    }
                }
            }
        }

        private fun formatDuration(seconds: Int): String {
            val m = seconds / 60
            val s = seconds % 60
            return "%02d:%02d".format(m, s)
        }
    }
}
