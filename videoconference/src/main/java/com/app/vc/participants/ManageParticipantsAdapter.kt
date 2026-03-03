package com.app.vc.participants

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.R
import com.app.vc.models.ParticipantsModel

class ManageParticipantsAdapter(
    private val participantsList: ArrayList<ParticipantsModel>,
    private val onChangeClick: (ParticipantsModel) -> Unit
) : RecyclerView.Adapter<ManageParticipantsAdapter.ViewHolder>() {
    var localParticipant = " (You) "

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var tvParticipantName = itemView.findViewById(R.id.tctParticipantName) as TextView
        var imgParticipantMic = itemView.findViewById(R.id.txtInitial) as TextView
        var typeOfUser = itemView.findViewById(R.id.txtLeftStatus) as TextView
        var change = itemView.findViewById(R.id.change) as TextView


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_participantslist, parent, false)
        return ViewHolder(
            v
        )
    }

    override fun getItemCount(): Int {
        return participantsList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val participant = participantsList[position]

        val blackTint = ColorStateList.valueOf(Color.BLACK)
        ViewCompat.setBackgroundTintList(holder.imgParticipantMic, blackTint)

        if (participant.isLocal)
            holder.tvParticipantName.text = participant.displayName
        else
            holder.tvParticipantName.text = participant.displayName

        val initial = participant.displayName
            .trim()
            .firstOrNull()
            ?.toString()
            ?.uppercase()

        holder.imgParticipantMic.text = initial ?: "?"

        // ✅ Change Click
        holder.change.setOnClickListener {
            onChangeClick(participant)
        }
    }

}