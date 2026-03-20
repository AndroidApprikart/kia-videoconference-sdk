package com.app.vc.virtualroomlist

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.R

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
        private val txtUnreadBadge: TextView? =
            itemView.findViewById(R.id.txtUnreadBadge) ?: itemView.findViewById(R.id.number)

        private val txtCustomerName: TextView? = itemView.findViewById(R.id.txtCustomerName)
        private val txtRoNumber: TextView? = itemView.findViewById(R.id.txtRoNumber)
        private val txtContactNumber: TextView? = itemView.findViewById(R.id.txtContactNumber)
        private val btnViewRoom: Button? = itemView.findViewById(R.id.btnViewRoom)

        fun bind(room: VirtualRoomUiModel, onRoomClick: (VirtualRoomUiModel) -> Unit) {
            val context = itemView.context

            // Phone view: group name only, no appointment/RO number
            txtTitle?.text = room.subject
            txtSubtitle?.text = "${room.dayLabel} \u2022 ${room.timeLabel}"

            txtStatus.text = room.lifecycleStatusLabel?.takeIf { it.isNotBlank() } ?: room.status.replace('_', ' ')
            
            txtUnreadBadge?.apply {
                visibility = if (room.unreadCount > 0) View.VISIBLE else View.GONE
                text = room.unreadCount.toString().padStart(2, '0')
            }

            // Tablet view: display RO Number and Customer Name; show "-" when RO number is absent
            txtCustomerName?.text = room.customerName
            txtRoNumber?.text = (room.roNumberDisplay ?: room.roNumber)?.takeIf { it.isNotBlank() } ?: "-"
            txtContactNumber?.text = room.contactNumber

            itemView.setOnClickListener { onRoomClick(room) }
            btnViewRoom?.setOnClickListener { onRoomClick(room) }
        }
    }
}
