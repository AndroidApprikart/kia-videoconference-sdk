package com.app.vc.message

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.R
import com.app.vc.VCConstants
import com.app.vc.databinding.MessageItemLayoutBinding
import com.app.vc.models.MessageModel


class MessageAdapter(
    private val dataList: ArrayList<MessageModel>,
    private val listener: MessageClickListener
) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    inner class ViewHolder(
        private val binding: MessageItemLayoutBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: MessageModel) {
            when(data.messageType){
                VCConstants.TEXT_MESSAGE -> {
                    /*hide all other message type layout*/
                    if (data.isLocalMessage) {
                        binding.llLocalMsg.visibility = View.VISIBLE
                        binding.llRemoteMsg.visibility = View.GONE
//                binding.tvLocalUserName.text = dataList[position].userName
                        binding.tvLocalMsgText.text = data.messageText
                    } else {
                        binding.llLocalMsg.visibility = View.GONE
                        binding.llRemoteMsg.visibility = View.VISIBLE
                        binding.tvRemoteUserName.text = data.userName
                        binding.tvRemoteMsgText.text = data.messageText
                    }
                }
                VCConstants.FILE_MESSAGE -> {
                    /*hide all other message type layout*/
                    if (data.isLocalMessage) {
                        binding.llLocalMsg.visibility = View.VISIBLE
                        binding.llRemoteMsg.visibility = View.GONE
//                binding.tvLocalUserName.text = dataList[position].userName
                        binding.tvLocalMsgText.text = data.fileName
                    } else {
                        binding.llLocalMsg.visibility = View.GONE
                        binding.llRemoteMsg.visibility = View.VISIBLE
                        binding.tvRemoteUserName.text = data.userName
                        binding.tvRemoteMsgText.text = data.fileName
                    }
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: MessageItemLayoutBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.message_item_layout, parent, false
        )

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            when(dataList[position].messageType) {
                VCConstants.FILE_MESSAGE -> {
                    listener.openURLInWeb(dataList[position].serverFilePath)
                }
            }
        }
        holder.bind(dataList[position]);
    }
}

