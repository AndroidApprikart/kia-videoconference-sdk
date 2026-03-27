package com.app.vc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.virtualchatroom.ChatMessage
import com.app.vc.virtualchatroom.ChatMessageType
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

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
        holder.image.visibility = View.INVISIBLE
        holder.loading.visibility = View.VISIBLE
        val url = item.thumbnailUrl?.takeIf { it.isNotBlank() } ?: item.attachmentUri
        Glide.with(holder.image)
            .load(url)
            .centerCrop()
            .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.image.visibility = View.VISIBLE
                    holder.loading.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: Target<android.graphics.drawable.Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.image.visibility = View.VISIBLE
                    holder.loading.visibility = View.GONE
                    return false
                }
            })
            .into(holder.image)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.imgMediaThumb)
        val loading: ProgressBar = itemView.findViewById(R.id.progressThumbLoading)
        val videoIndicatorContainer: FrameLayout = itemView.findViewById(R.id.layoutVideoIndicator)
        val videoIndicator: ImageView = itemView.findViewById(R.id.imgVideoIndicator)
    }
}
