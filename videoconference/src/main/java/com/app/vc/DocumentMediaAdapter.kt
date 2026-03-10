package com.app.vc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.virtualchatroom.ChatMessage
import java.util.Locale

class DocumentMediaAdapter(
    private var items: List<ChatMessage>,
    private val onClick: (ChatMessage) -> Unit
) : RecyclerView.Adapter<DocumentMediaAdapter.ViewHolder>() {

    fun updateItems(newItems: List<ChatMessage>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doc_media, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.itemView.setOnClickListener { onClick(item) }
        holder.docImage.setImageResource(R.drawable.file_pdf_icon)
        holder.fileName.text = item.fileName ?: "Document"
        holder.fileType.text = item.fileName?.substringAfterLast('.', "pdf")?.uppercase(Locale.getDefault()) ?: "PDF"
        holder.date.text = item.timeLabel
        holder.pages.visibility = View.GONE
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val docImage: ImageView = itemView.findViewById(R.id.docImg)
        val fileName: TextView = itemView.findViewById(R.id.fileName)
        val pages: TextView = itemView.findViewById(R.id.pages)
        val fileType: TextView = itemView.findViewById(R.id.fileType)
        val date: TextView = itemView.findViewById(R.id.date)
    }
}
