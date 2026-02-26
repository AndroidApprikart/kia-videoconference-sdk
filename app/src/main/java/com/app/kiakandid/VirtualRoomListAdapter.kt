package com.app.kiakandid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VirtualRoomListAdapter(
    private var rooms: List<VirtualRoom>
) : RecyclerView.Adapter<VirtualRoomListAdapter.VirtualRoomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VirtualRoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_virtual_room, parent, false)
        return VirtualRoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: VirtualRoomViewHolder, position: Int) {
        holder.bind(rooms[position])
    }

    override fun getItemCount(): Int = rooms.size

    fun updateRooms(newRooms: List<VirtualRoom>) {
        rooms = newRooms
        notifyDataSetChanged()
    }

    class VirtualRoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtCustomerName: TextView = itemView.findViewById(R.id.txtCustomerName)
        private val txtRoNumber: TextView = itemView.findViewById(R.id.txtRoNumber)
        private val txtComplaint: TextView = itemView.findViewById(R.id.txtComplaint)
        private val txtContactNumber: TextView = itemView.findViewById(R.id.txtContactNumber)
        private val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)

        fun bind(room: VirtualRoom) {
            txtCustomerName.text = room.customerName
            txtRoNumber.text = room.roNumber
            txtComplaint.text = room.complaint
            txtContactNumber.text = room.contactNumber
            txtStatus.text = room.status
        }
    }
}

