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
        private val txtReferenceLabel: TextView? = itemView.findViewById(R.id.txtReferenceLabel)
        private val txtRoNumber: TextView? = itemView.findViewById(R.id.txtRoNumber)
//        private val txtContactNumber: TextView? = itemView.findViewById(R.id.txtContactNumber)
        private val btnViewRoom: Button? = itemView.findViewById(R.id.btnViewRoom)

        fun bind(room: VirtualRoomUiModel, onRoomClick: (VirtualRoomUiModel) -> Unit) {
            val referenceValue = (room.roNumberDisplay ?: room.appointmentIdDisplay).orEmpty()
            val trailingTitle = if (!room.roNumberDisplay.isNullOrBlank()) {
                "Repair Order"
            } else {
                "Service Appointment"
            }

            // Phone view: show RO number first, otherwise appointment id.
            txtTitle?.text = when {
                referenceValue.isNotBlank() && trailingTitle.isNotBlank() -> "$referenceValue | $trailingTitle"
                referenceValue.isNotBlank() -> referenceValue
                else -> trailingTitle
            }
            txtSubtitle?.text = "${room.dayLabel} ${if (room.dayLabel.isNotBlank() && room.timeLabel.isNotBlank()) "\u2022" else ""} ${room.timeLabel}".trim()

            txtStatus.text = room.lifecycleStatusLabel?.takeIf { it.isNotBlank() } ?: room.status.replace('_', ' ')
            
            txtUnreadBadge?.apply {
                visibility = if (room.unreadCount > 0) View.VISIBLE else View.GONE
                text = room.unreadCount.toString().padStart(2, '0')
            }

            // Tablet view: show RO when present, otherwise appointment id
            txtCustomerName?.text = room.customerName
            val hasRoNumber = !room.roNumberDisplay.isNullOrBlank()
            txtReferenceLabel?.text = if (hasRoNumber) "RO No" else "Appointment No"
            txtRoNumber?.text = (room.roNumberDisplay ?: room.appointmentIdDisplay)?.takeIf { it.isNotBlank() } ?: "-"

            itemView.setOnClickListener { onRoomClick(room) }
            btnViewRoom?.setOnClickListener { onRoomClick(room) }
        }
    }
}
