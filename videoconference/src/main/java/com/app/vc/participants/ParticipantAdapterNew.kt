package com.app.vc.participants

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.R
import com.app.vc.databinding.ParticipantsListItemBinding
import com.app.vc.models.ParticipantsModel

/* created by Naghma 20/08/23*/


class ParticipantAdapterNew(
    var dataList:List<ParticipantsModel>,
): RecyclerView.Adapter<ParticipantAdapterNew.ViewHolder>() {
    var localParticipant = " (You) "


   inner class ViewHolder(
        private val binding: ParticipantsListItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: ParticipantsModel) {
            val blackTint = ColorStateList.valueOf(Color.BLACK)
            ViewCompat.setBackgroundTintList(binding.imgParticipantMic, blackTint)
            if (data.isLocal)
            /*            holder.tvParticipantName.text =
                           participantsList[position].displayName.plus(localParticipant) /*display only You*/*/
                binding.tvParticipantName.text =  data.displayName +"\n"+localParticipant
            else
                binding .tvParticipantName.text = data.displayName



            if (data.isMicOn) {
//            holder.imgParticipant.setImageResource(R.drawable.icon_mic_enable)
                binding.imgParticipantMic.setBackgroundResource(R.drawable.ic_mic_enabled)

            } else {
//            holder.imgParticipant.setImageResource(R.drawable.icon_mic_disable)
                binding.imgParticipantMic.background = null
                binding.imgParticipantMic.setBackgroundResource(R.drawable.ic_mic_disabled_wighout_bg_tint)

            }
//        (holder.imgParticipantMic.background as AnimationDrawable).start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ParticipantsListItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.participants_list_item, parent, false
        )

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position]);
    }
}
