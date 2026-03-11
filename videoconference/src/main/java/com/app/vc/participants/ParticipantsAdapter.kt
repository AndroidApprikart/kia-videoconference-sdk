package com.app.vc.participants

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.R
import com.app.vc.models.GroupMemberResponse
import com.app.vc.utils.PreferenceManager


class ParticipantsAdapter(
    private var participantsList: List<GroupMemberResponse>,
) :
    RecyclerView.Adapter<ParticipantsAdapter.ViewHolder>() {
    var localParticipantSuffix = " (You)"
    private var onlineUserIds: Set<String> = emptySet()

    fun setOnlineUserIds(ids: Set<String>) {
        onlineUserIds = ids
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvParticipantName = itemView.findViewById(R.id.tctParticipantName) as TextView
        var imgParticipantMic = itemView.findViewById(R.id.txtInitial) as TextView
        var textRole = itemView.findViewById(R.id.txtLeftStatus) as TextView
        var statusDot = itemView.findViewById<View>(R.id.statusDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.participants_list_item, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return participantsList.size
    }

    fun updateList(newList: List<GroupMemberResponse>) {
        this.participantsList = newList
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val member = participantsList[position]
        val blackTint = ColorStateList.valueOf(Color.BLACK)
        ViewCompat.setBackgroundTintList(holder.imgParticipantMic, blackTint)

        val currentUserId = PreferenceManager.getUserId()
        val isLocal = member.userId.toString() == currentUserId

        // Display participant_role
        holder.textRole.text = member.participantRole ?: ""
        
        val displayName = member.displayName
        
        if (isLocal) {
            holder.tvParticipantName.text = displayName.plus(localParticipantSuffix)
        } else {
            holder.tvParticipantName.text = displayName
        }

        val initial = displayName
            .trim()
            .firstOrNull()
            ?.toString()
            ?.uppercase()

        holder.imgParticipantMic.text = initial ?: "?"
        val isOnline = member.userId.toString() in onlineUserIds
        ViewCompat.setBackgroundTintList(
            holder.statusDot,
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    holder.itemView.context,
                    if (isOnline) R.color.green else R.color.red
                )
            )
        )
    }
}
