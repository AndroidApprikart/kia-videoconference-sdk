package com.app.vc

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class VirtualRoomListAdapter(
    private var rooms: List<VirtualRoomUiModel>,
    private val onRoomClick: (VirtualRoomUiModel) -> Unit
) : RecyclerView.Adapter<VirtualRoomListAdapter.VirtualRoomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VirtualRoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.vc_item_virtual_room, parent, false)
        return VirtualRoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: VirtualRoomViewHolder, position: Int) {
        holder.bind(rooms[position], onRoomClick)
    }

    override fun getItemCount(): Int = rooms.size

    fun updateRooms(newRooms: List<VirtualRoomUiModel>) {
        rooms = newRooms
        notifyDataSetChanged()
    }

    class VirtualRoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val iconChat: ImageView? = itemView.findViewById(R.id.iconChat)
        private val txtTitle: TextView? = itemView.findViewById(R.id.txtTitle)
        private val txtSubtitle: TextView? = itemView.findViewById(R.id.txtSubtitle)
        private val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)
        private val txtUnreadBadge: TextView? = itemView.findViewById(R.id.txtUnreadBadge)

        // Tablet specific views (will be null on phone layout)
        private val txtCustomerName: TextView? = itemView.findViewById(R.id.txtCustomerName)
        private val txtRoNumber: TextView? = itemView.findViewById(R.id.txtRoNumber)
        private val txtContactNumber: TextView? = itemView.findViewById(R.id.txtContactNumber)
        private val btnViewRoom: Button? = itemView.findViewById(R.id.btnViewRoom)

        fun bind(room: VirtualRoomUiModel, onRoomClick: (VirtualRoomUiModel) -> Unit) {
            val context = itemView.context

            // Mobile & tablet common title/subtitle (only present in phone layout)
            txtTitle?.text = "${room.roNumber} | ${room.subject}"
            txtSubtitle?.text = "${room.dayLabel} \u2022 ${room.timeLabel}"

            // Status chip
            txtStatus.text = room.status.name.replace('_', ' ')
            val bg = txtStatus.background as? GradientDrawable
            bg?.setColor(ContextCompat.getColor(context, statusColor(room.status)))

            // Unread badge (only visible when > 0)
            txtUnreadBadge?.apply {
                visibility = if (room.unreadCount > 0) View.VISIBLE else View.GONE
                text = room.unreadCount.toString()
            }

            // Tablet-specific columns
            txtCustomerName?.text = room.customerName
            txtRoNumber?.text = room.roNumber
            txtContactNumber?.text = room.contactNumber

            // Clicks open chat room (both phone item and tablet 'View Room' button)
            itemView.setOnClickListener { onRoomClick(room) }
            btnViewRoom?.setOnClickListener { onRoomClick(room) }
        }

        private fun statusColor(status: RoomStatus): Int {
            return when (status) {
                RoomStatus.OPEN -> R.color.vc_status_open
                RoomStatus.IN_PROGRESS -> R.color.vc_status_in_progress
                RoomStatus.CLOSED -> R.color.vc_status_closed
                RoomStatus.REOPENED -> R.color.vc_status_reopened
                RoomStatus.CANCELLED -> R.color.vc_status_cancelled
            }
        }
    }
}

