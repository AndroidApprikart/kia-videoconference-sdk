package com.app.vc.virtualroomlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.R
import java.text.SimpleDateFormat
import java.util.Locale

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
        private val dealerShipName: TextView? = itemView.findViewById(R.id.dealershipName)
        private val appointmentdate: TextView? = itemView.findViewById(R.id.appointmentconfirmed)
        private val roNumOrDate: TextView? = itemView.findViewById(R.id.roDate)
//        private val txtSubtitle: TextView? = itemView.findViewById(R.id.txtSubtitle)
        private val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)
        private val txtUnreadBadge: TextView? =
            itemView.findViewById(R.id.txtUnreadBadge) ?: itemView.findViewById(R.id.number)

        private val txtCustomerName: TextView? = itemView.findViewById(R.id.txtCustomerName)
        private val txtReferenceLabel: TextView? = itemView.findViewById(R.id.txtReferenceLabel)
        private val txtRoNumber: TextView? = itemView.findViewById(R.id.txtRoNumber)
//        private val txtContactNumber: TextView? = itemView.findViewById(R.id.txtContactNumber)
        private val btnViewRoom: Button? = itemView.findViewById(R.id.btnViewRoom)

        fun formatAppointmentDate(dateStr: String?): String {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
            return try {
                val date = inputFormat.parse(dateStr)
                outputFormat.format(date!!)
            } catch (e: Exception) {
                ""
            }
        }
        fun formatWorkType(workType: String?): String {
            return workType
                ?.lowercase()
                ?.split("_")
                ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                ?: ""
        }

        fun bind(room: VirtualRoomUiModel, onRoomClick: (VirtualRoomUiModel) -> Unit) {
            val referenceValue = (room.roNumberDisplay ?: room.appointmentIdDisplay).orEmpty()
            val trailingTitle = if (!room.roNumberDisplay.isNullOrBlank()) {
                "Repair Order"
            } else {
                "Service Appointment"
            }

            // Phone view: show RO number first, otherwise appointment id.

            val formatted = formatWorkType(room.work_type)
            txtTitle?.text = formatted

            if (room.roNumberDisplay.isNullOrEmpty()){
               val formatted = formatAppointmentDate(room.appointment_date)
                roNumOrDate?.text =formatted
            }else{
                roNumOrDate?.text=room.roNumberDisplay
            }

//            txtSubtitle?.text = "${room.dayLabel} ${if (room.dayLabel.isNotBlank() && room.timeLabel.isNotBlank()) "\u2022" else ""} ${room.timeLabel}".trim()

            txtStatus.text = room.lifecycleStatusLabel?.takeIf { it.isNotBlank() } ?: room.status.replace('_', ' ')

             val appointmentidlayout="Appointment confirmed for ${formatAppointmentDate(room.appointment_date)}"
            appointmentdate?.text =appointmentidlayout

            txtUnreadBadge?.apply {
                visibility = if (room.unreadCount > 0) View.VISIBLE else View.GONE
                text = room.unreadCount.toString().padStart(2, '0')
            }

            // Tablet view: show RO when present, otherwise appointment id
            txtCustomerName?.text = room.customerName
            dealerShipName?.text = room.dealer_name
            val hasRoNumber = !room.roNumberDisplay.isNullOrBlank()
            txtReferenceLabel?.text = if (hasRoNumber) "RO No" else "Appointment No"
            txtRoNumber?.text = (room.roNumberDisplay ?: room.appointmentIdDisplay)?.takeIf { it.isNotBlank() } ?: "-"

            itemView.setOnClickListener { onRoomClick(room) }
            btnViewRoom?.setOnClickListener { onRoomClick(room) }
        }
    }
}
