package com.app.vc.message

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.utils.FileOperations
import com.app.vc.R
import com.app.vc.utils.VCConstants
import com.app.vc.databinding.MessageItemLayoutNewBinding
import com.app.vc.models.MessageModel
import com.app.vc.models.MessageStatusEnum
import java.text.SimpleDateFormat
import java.util.Date

class MessageAdapterNew (
    private val dataList: ArrayList<MessageModel>,
    private val listener: MessageClickListener
) :
    RecyclerView.Adapter<MessageAdapterNew.ViewHolder>() {
    val TAG = "MSG_ADAPTER"
    inner class ViewHolder(
        private val binding: MessageItemLayoutNewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: MessageModel) {
//            if(data.isLocalMessage) {
//                binding.selfLayout.visibility = View.VISIBLE
//                binding.receiveLayout.visibility = View.GONE
//                /*hiding  time stamps and status -> out of the layout*/
//                binding.tvReceiverTimeStamp.visibility = View.GONE
//                binding.tvReceiverMsgStatus.visibility = View.GONE
//                binding.tvTimeStampSelf.visibility = View.VISIBLE
//                binding.tvSelfMsgStatus.visibility = View.VISIBLE
//                binding.tvTimeStampSelf.text = getLocalHoursAndMinutesFromMilliseconds(data.id!!).first.toString() + ":"+
//                        getLocalHoursAndMinutesFromMilliseconds(data.id!!).second.toString()
//                binding.selfUserNameTv.text = data.userName
//                updateLocalFileStatusUI(binding.tvSelfMsgStatus,data.status,data.messageType)
//
//            }else{
//                binding.selfLayout.visibility = View.GONE
//                binding.receiveLayout.visibility = View.VISIBLE
//                /*hiding  time stamps and status -> out of the layout*/
//                binding.tvReceiverTimeStamp.visibility = View.VISIBLE
//                binding.tvReceiverMsgStatus.visibility = View.GONE//not showing status for incoming remote messages for all the statuses..only minimal ones
//                binding.tvTimeStampSelf.visibility = View.GONE
//                binding.tvSelfMsgStatus.visibility = View.GONE
//                binding.tvReceiverTimeStamp.text = getLocalHoursAndMinutesFromMilliseconds(data.id!!).first.toString() + ":"+
//                        getLocalHoursAndMinutesFromMilliseconds(data.id!!).second.toString()
//                binding.userNameTv.text = data.userName
//                updateRemoteFileStatusUI(binding.tvReceiverMsgStatus,data.status,data.messageType) /*if required to show the status of remote message*/
//            }
            when(data.messageType){
                VCConstants.TEXT_MESSAGE -> {
                    if (data.isLocalMessage) {
                        binding.selfLayout.visibility = View.VISIBLE
                        binding.receiveLayout.visibility = View.GONE
                        binding.selfUserNameTv.text = data.userName
                        /*hide message and estimation layout*/
                        binding.selfFileLayout.visibility = View.GONE
                        binding.selfLayoutEstimation.visibility = View.GONE
                        binding.receiveFileLayout.visibility = View.GONE
                        binding.rvReceiverEstimationLayout.visibility = View.GONE
                        binding.selfMessageTv.visibility = View.VISIBLE
//                binding.tvLocalUserName.text = dataList[position].userName
                        binding.selfMessageTv.text = data.messageText
                binding.tvTimeStampSelf.text = getLocalHoursAndMinutesFromMilliseconds(data.id!!).first.toString() + ":"+
                        getLocalHoursAndMinutesFromMilliseconds(data.id!!).second.toString()
                    } else {
                        binding.selfLayout.visibility = View.GONE
                        binding.receiveLayout.visibility = View.VISIBLE
                        /*hide message and estimation layout*/
                        binding.receiveFileLayout.visibility = View.GONE
                        binding.rvReceiverEstimationLayout.visibility = View.GONE
                        binding.selfFileLayout.visibility = View.GONE
                        binding.selfLayoutEstimation.visibility = View.GONE
                        binding.tvReceiverTimeStamp.visibility = View.VISIBLE
                        binding.receiveMessageTv.visibility = View.VISIBLE
                        binding.receiveMessageTv.text = data.messageText
                        binding.userNameTv.text = data.userName
//                        binding.tvReceiverMsgStatus .text = data.status   //not showing status for incoming remote messages
                        binding.tvReceiverTimeStamp.text = getLocalHoursAndMinutesFromMilliseconds(data.id!!).first.toString() + ":"+
                        getLocalHoursAndMinutesFromMilliseconds(data.id!!).second.toString()
                    }
                }
                VCConstants.FILE_MESSAGE -> {
                    if (data.isLocalMessage) {
                        binding.selfLayout.visibility = View.VISIBLE
                        binding.receiveLayout.visibility = View.GONE
                        /*hide estimation layout*/
                        binding.selfFileLayout.visibility = View.VISIBLE
                        binding.selfFileDocLayout.visibility = View.VISIBLE
                        binding.selfLayoutEstimation.visibility = View.GONE
                        binding.selfMessageTv.visibility = View.GONE
                        binding.selfUserNameTv.text = data.userName
//                binding.tvLocalUserName.text = dataList[position].userName
                        binding.selfFileDocTv.text = data.fileName
                        setFileTypeUi(binding.selfFileTypeTv, FileOperations.getFileExtension(data.fileName))
                        binding.tvReceiverTimeStamp.visibility = View.VISIBLE
                binding.tvTimeStampSelf.text = getLocalHoursAndMinutesFromMilliseconds(data.id!!).first.toString() + ":"+
                        getLocalHoursAndMinutesFromMilliseconds(data.id!!).second.toString()

                    } else {
                        binding.selfLayout.visibility = View.GONE
                        binding.receiveLayout.visibility = View.VISIBLE
                        /*hide estimation layout*/
                        binding.receiveFileLayout.visibility = View.VISIBLE
                        binding.receiveFileDocLayout.visibility = View.VISIBLE
                        binding.rvReceiverEstimationLayout.visibility = View.GONE
                        binding.receiveMessageTv.visibility = View.GONE
                        binding.receiveFileDocTv.text = data.fileName
                        binding.userNameTv.text = data.userName
//                        binding.tvReceiverMsgStatus .text = data.status  //not showing status for incoming remote messages
                        setFileTypeUi(binding.receiveFileTypeTv, FileOperations.getFileExtension(data.fileName))
                        binding.tvReceiverTimeStamp.visibility = View.VISIBLE
                        binding.tvReceiverTimeStamp.text = getLocalHoursAndMinutesFromMilliseconds(data.id!!).first.toString() + ":"+
                        getLocalHoursAndMinutesFromMilliseconds(data.id!!).second.toString()
                    }
                }
            }

        }
    }

    private fun updateLocalFileStatusUI(
        tvSelfMsgStatus: AppCompatTextView,
        status: String,
        messageType: String
    ) {
        when(status)
        {
            MessageStatusEnum.MSG_SENDING_IN_PROGRESS.tag -> {
                tvSelfMsgStatus.text = MessageStatusEnum.MSG_SENDING_IN_PROGRESS.displayText
            }
            MessageStatusEnum.MSG_SENT_SUCCESS.tag -> {
                tvSelfMsgStatus.text = MessageStatusEnum.MSG_SENT_SUCCESS.displayText

            }
            MessageStatusEnum.MSG_SENT_FAILURE.tag -> {
                tvSelfMsgStatus.text = MessageStatusEnum.MSG_SENT_FAILURE.displayText

            }

            MessageStatusEnum.FILE_UPLOAD_PROGRESS.tag -> {
                tvSelfMsgStatus.text = MessageStatusEnum.FILE_UPLOAD_PROGRESS.displayText

            }
            MessageStatusEnum.FILE_UPLOAD_SUCCESS.tag -> {
                tvSelfMsgStatus.text = MessageStatusEnum.FILE_UPLOAD_SUCCESS.displayText

            }
            MessageStatusEnum.FILE_UPLOAD_FAILURE.tag -> {
                tvSelfMsgStatus.text = MessageStatusEnum.FILE_UPLOAD_FAILURE.displayText

            }

        }
    }

    private fun updateRemoteFileStatusUI(
        tvReceiverMsgStatus: AppCompatTextView,
        status: String,
        messageType: String
    ) {
        Log.d(TAG, "updateRemoteFileStatusUI: ")
        when(status)
        {
            /*showing only the download status*/
            /* MessageStatusEnum.MSG_SENDING_IN_PROGRESS.tag -> {
                 tvReceiverMsgStatus.text = MessageStatusEnum.MSG_SENDING_IN_PROGRESS.displayText
             }
             MessageStatusEnum.MSG_SENT_SUCCESS.tag -> {
                 tvReceiverMsgStatus.text = MessageStatusEnum.MSG_SENT_SUCCESS.displayText

             }
             MessageStatusEnum.MSG_SENT_FAILURE.tag -> {
                 tvReceiverMsgStatus.text = MessageStatusEnum.MSG_SENT_FAILURE.displayText

             }*/

            MessageStatusEnum.FILE_DOWNLOAD_PROGRESS.tag -> {
                tvReceiverMsgStatus.visibility = View.VISIBLE
                tvReceiverMsgStatus.text = MessageStatusEnum.FILE_DOWNLOAD_PROGRESS.displayText


            }
            MessageStatusEnum.FILE_DOWNLOAD_SUCCESS.tag -> {
                tvReceiverMsgStatus.visibility = View.VISIBLE
                tvReceiverMsgStatus.text = MessageStatusEnum.FILE_DOWNLOAD_SUCCESS.displayText

            }
            MessageStatusEnum.FILE_DOWNLOAD_FAILURE.tag -> {
                tvReceiverMsgStatus.visibility = View.VISIBLE
                tvReceiverMsgStatus.text = MessageStatusEnum.FILE_DOWNLOAD_FAILURE.displayText
            }
            else ->{
                tvReceiverMsgStatus.visibility = View.GONE
            }

        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: MessageItemLayoutNewBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.message_item_layout_new, parent, false
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
                    Log.d("TAG", "onBindViewHolder: ")
                    listener.openURLInWeb(dataList[position].serverFilePath) /*to open in web browser*/

                    /*to download and view...comment this if not in requirement*/
                    /*download and view logic */
                    /*file download or open logic*/
                    /* if(dataList[position].isLocalMessage){
                         listener.openFile(dataList[position])
                     }else{
                         if(!dataList[position].status.equals(MessageStatusEnum.FILE_DOWNLOAD_PROGRESS.tag,false)){
                             listener.downloadFileAndOpen(dataList[position])
                         }else {
                         Log.d(TAG, "onBindViewHolder: already downloading!")}
                     }*/
                    /*download and view logic */
                    /*retry logic -> to be added with extra status checking conditions - please check before using this*/
                    /*if(dataList[position].isLocalMessage)
                    {
                        listener.resendMessage(dataList[position])
                    }else
                    {
                        Log.d(TAG, "" +
                                ": fileName::"+dataList[position].downloadableFileName+"id:"+dataList[position].id)
                    }*/
                    /*retry logic*/
                }
                VCConstants.TEXT_MESSAGE -> {
                    /*retry logic -> to be added with extra status checking conditions - please check before using this*/
                   /* if(dataList[position].isLocalMessage)
                    {
                        listener.resendMessage(dataList[position])
                    }
                    */
                }
            }
        }
        holder.bind(dataList[position]);
    }

    fun getLocalHoursAndMinutesFromMilliseconds(milliseconds: Long): Pair<Int, String> {
        val sdf = SimpleDateFormat("HH:mm")
        val localTime = sdf.format(Date(milliseconds))
//        Log.d(TAG, "getLocalHoursAndMinutesFromMilliseconds: localTime : ${localTime}")
        val hours = localTime.substringBefore(":").toInt()
        val minutes = localTime.substringAfter(":").padStart(2, '0')
        return Pair(hours, minutes)
    }
    private fun setFileTypeUi(fileTypeTv: AppCompatTextView, fileExtension: String) {
        if (fileExtension.contains("pdf", true)) {
            fileTypeTv.setBackgroundResource(R.drawable.file_type_pdf)
            fileTypeTv.text = fileExtension
        }else if (fileExtension.contains("doc", true)
            || fileExtension.contains("docx", true)
            || fileExtension.contains("odt", true)
            || fileExtension.contains("ott", true)
            || fileExtension.contains("fodt", true)
            || fileExtension.contains("dot", true)
        ) {
            fileTypeTv.setBackgroundResource(R.drawable.file_type_doc)
            fileTypeTv.text = ""
        } else if (fileExtension.contains("xls", true)
            || fileExtension.contains("xlsx", true)
            || fileExtension.contains("xlt", true)
            || fileExtension.contains("xml", true)
            || fileExtension.contains("csv", true)
            || fileExtension.contains("xlsm", true)
            || fileExtension.contains("ods", true)
            || fileExtension.contains("ots", true)
            || fileExtension.contains("uos", true)
            || fileExtension.contains("fods", true)
        ) {
            fileTypeTv.setBackgroundResource(R.drawable.file_type_excel)
            fileTypeTv.text = ""
        } else if (fileExtension.contains("mp4", true)
            || fileExtension.contains("avi", true)
            || fileExtension.contains("mkv", true)
            || fileExtension.contains("MOV", true)
            || fileExtension.contains("WEBM", true)
            || fileExtension.contains("WMV", true)
            || fileExtension.contains("FLV", true)
            || fileExtension.contains("SWF", true)
        ) {
            fileTypeTv.setBackgroundResource(R.drawable.file_type_video)
            fileTypeTv.text = ""
        } else {
            fileTypeTv.setBackgroundResource(R.drawable.file_type_generic)
            fileTypeTv.text = fileExtension        }
        /*else if (fileExtension.contains("png", true)
            || fileExtension.contains("jpg", true)
            || fileExtension.contains("jpeg", true)
        ) {
            fileTypeTv.setBackgroundResource(R.drawable.file_type_generic)
            fileTypeTv.text = fileExtension
        }*/
    }

}

