package com.app.vc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.virtualchatroom.ChatMessage
import com.app.vc.virtualchatroom.ChatMessageType
import com.bumptech.glide.Glide

class PhotoVideoMediaAdapter(
    private var items: List<ChatMessage>,
    private val onClick: (ChatMessage) -> Unit
) : RecyclerView.Adapter<PhotoVideoMediaAdapter.ViewHolder>() {

    fun updateItems(newItems: List<ChatMessage>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_video_media, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.itemView.setOnClickListener { onClick(item) }
        holder.videoIndicatorContainer.visibility =
            if (item.type == ChatMessageType.VIDEO) View.VISIBLE else View.GONE
        val url = item.thumbnailUrl?.takeIf { it.isNotBlank() } ?: item.attachmentUri
        Glide.with(holder.image)
            .load(url)
            .centerCrop()
            .into(holder.image)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.imgMediaThumb)
        val videoIndicatorContainer: FrameLayout = itemView.findViewById(R.id.layoutVideoIndicator)
        val videoIndicator: ImageView = itemView.findViewById(R.id.imgVideoIndicator)
    }
}
