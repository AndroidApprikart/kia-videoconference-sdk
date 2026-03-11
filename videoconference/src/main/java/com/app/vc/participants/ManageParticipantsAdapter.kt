package com.app.vc.participants

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.R
import com.app.vc.models.GroupMemberResponse
import com.app.vc.utils.PreferenceManager

class ManageParticipantsAdapter(
    private var participantsList: List<GroupMemberResponse>,
    private val onChangeClick: (GroupMemberResponse) -> Unit
) : RecyclerView.Adapter<ManageParticipantsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvParticipantName = itemView.findViewById(R.id.tctParticipantName) as TextView
        var imgParticipantMic = itemView.findViewById(R.id.txtInitial) as TextView
        var typeOfUser = itemView.findViewById(R.id.txtLeftStatus) as TextView
        var change = itemView.findViewById(R.id.change) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_participantslist, parent, false)
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
        
        val displayName = member.displayName

        if (isLocal) {
            holder.tvParticipantName.text = displayName.plus(" (You)")
        } else {
            holder.tvParticipantName.text = displayName
        }

        val initial = displayName
            .trim()
            .firstOrNull()
            ?.toString()
            ?.uppercase()

        holder.imgParticipantMic.text = initial ?: "?"
        
        // Display participant_role
        holder.typeOfUser.text = member.participantRole ?: ""

        // ✅ Visibility logic for "change" button: Show only if participant_role is service_advisor
        if (member.participantRole.equals("service_advisor", ignoreCase = true)) {
            holder.change.visibility = View.VISIBLE
        } else {
            holder.change.visibility = View.GONE
        }

        // ✅ Change Click
        holder.change.setOnClickListener {
            onChangeClick(member)
        }
    }
}
