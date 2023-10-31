package com.app.vc.participants

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.R
import com.app.vc.models.ParticipantsModel


class ParticipantsAdapter(
    private val participantsList: ArrayList<ParticipantsModel>,
) :
    RecyclerView.Adapter<ParticipantsAdapter.ViewHolder>() {
        var localParticipant = " (You) "

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var tvParticipantName = itemView.findViewById(R.id.tv_participant_name) as TextView
        var imgParticipantMic = itemView.findViewById(R.id.img_participant_mic) as ImageView


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.participants_list_item, parent, false)
        return ViewHolder(
            v
        )
    }

    override fun getItemCount(): Int {
        return participantsList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val blackTint = ColorStateList.valueOf(Color.BLACK)
        ViewCompat.setBackgroundTintList(holder.imgParticipantMic, blackTint)
        if (participantsList[position].isLocal)
/*            holder.tvParticipantName.text =
               participantsList[position].displayName.plus(localParticipant) /*display only You*/*/
            holder.tvParticipantName.text = localParticipant
        else
            holder.tvParticipantName.text = participantsList[position].trackId


        Log.d(
            "PARTICIPANT_SCREEN",
            "in adapter :: is Audio on ${participantsList[position].isMicOn}"
        )
        Log.d(
            "PARTICIPANT_SCREEN",
            "in adapter :: displayName on ${participantsList[position].streamId}"
        )



        if (participantsList[position].isMicOn) {
//            holder.imgParticipant.setImageResource(R.drawable.icon_mic_enable)
            holder.imgParticipantMic.setBackgroundResource(R.drawable.ic_mic_on)

        } else {
//            holder.imgParticipant.setImageResource(R.drawable.icon_mic_disable)
            holder.imgParticipantMic.setBackgroundResource(R.drawable.ic_mic_off)
        }
//        (holder.imgParticipantMic.background as AnimationDrawable).start()
    }


}