package com.app.vc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.virtualchatroom.ChatMessage
import com.app.vc.virtualchatroom.ChatMessageType
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class PhotoVideoMediaAdapter(
    private var rows: List<RowItem>,
    private val onClick: (ChatMessage) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed class RowItem {
        data class Header(val label: String) : RowItem()
        data class Media(val message: ChatMessage) : RowItem()
    }

    fun updateItems(newRows: List<RowItem>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (rows[position]) {
            is RowItem.Header -> 0
            is RowItem.Media -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.vc_item_media_section_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_photo_video_media, parent, false)
            MediaViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is RowItem.Header -> (holder as HeaderViewHolder).bind(row.label)
            is RowItem.Media -> (holder as MediaViewHolder).bind(row.message)
        }
    }

    override fun getItemCount(): Int = rows.size

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtHeader: TextView = itemView.findViewById(R.id.txtMediaSectionHeader)
        fun bind(label: String) {
            txtHeader.text = label
        }
    }

    inner class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.imgMediaThumb)
        val loading: ProgressBar = itemView.findViewById(R.id.progressThumbLoading)
        val videoIndicatorContainer: FrameLayout = itemView.findViewById(R.id.layoutVideoIndicator)
        val videoIndicator: ImageView = itemView.findViewById(R.id.imgVideoIndicator)

        fun bind(item: ChatMessage) {
            itemView.setOnClickListener { onClick(item) }
            videoIndicatorContainer.visibility =
                if (item.type == ChatMessageType.VIDEO) View.VISIBLE else View.GONE
            image.visibility = View.INVISIBLE
            loading.visibility = View.VISIBLE
            val url = item.thumbnailUrl?.takeIf { it.isNotBlank() } ?: item.attachmentUri
            Glide.with(image)
                .load(url)
                .centerCrop()
                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<android.graphics.drawable.Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        image.visibility = View.VISIBLE
                        loading.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        model: Any,
                        target: Target<android.graphics.drawable.Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        image.visibility = View.VISIBLE
                        loading.visibility = View.GONE
                        return false
                    }
                })
                .into(image)
        }
    }
}
