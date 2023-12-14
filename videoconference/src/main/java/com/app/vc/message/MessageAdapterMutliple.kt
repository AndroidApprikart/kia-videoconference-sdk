package com.app.vc.message

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.FileOperations
import com.app.vc.PreferenceManager
import com.app.vc.R
import com.app.vc.VCConstants
import com.app.vc.databinding.LayoutEstimationMessageSelfBinding
import com.app.vc.databinding.LayoutLocalFileMessageBinding
import com.app.vc.databinding.LayoutLocalTextMessageBinding
import com.app.vc.databinding.LayoutRemoteFileMessageBinding
import com.app.vc.databinding.LayoutRemoteTextMessageBinding
import com.app.vc.databinding.MessageItemLayoutNewBinding
import com.app.vc.models.MessageModel
import com.app.vc.models.MessageStatusEnum
import com.kia.vc.message.Labour
import com.kia.vc.message.LabourListAdapter
import com.kia.vc.message.Part
import com.kia.vc.message.PartListAdapter
import java.text.SimpleDateFormat
import java.util.Date
import android.content.Context
import android.widget.Toast

import com.app.vc.databinding.LayoutEstimationMessageRemoteBinding

class MessageAdapterMutliple (
    private val dataList: ArrayList<MessageModel>,
    private val listener: MessageClickListener,
    private var mContext: Context,
    private var messageScreenContext:MessageFragment
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val TAG = "MSG_ADAPTER"

    var onAcceptClickListener: MessageAdapterMutliple.OnAcceptClickListener ?=null
    var onRejectClickListener: MessageAdapterMutliple.OnRejectClickListener? = null
    var selectAllListener: MessageAdapterMutliple.SelectAllItemsClickListener? = null

    var isClickable = true
    var lastClickTime = System.currentTimeMillis()
    var clickTimeInterval = 2000

    private fun updateLocalMessageStatusUI(
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
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        Log.d(TAG, "onCreateViewHolder: ${viewType}")
        when (viewType) {
            1 -> { /*local text*/
                val binding = LayoutLocalTextMessageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return  LocalTextHolder(binding)
            }

            2 -> { /*local file*/
                val binding = LayoutLocalFileMessageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return   LocalFileHolder(binding)
            }

            3 -> { /*local estimate*/
                val binding=  LayoutEstimationMessageSelfBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return LocalEstimateHolder(binding)
            }

            4 -> { /*remote text*/
                val binding = LayoutRemoteTextMessageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return  RemoteTextHolder(binding)
            }

            5 -> { /*remote file*/
                val binding = LayoutRemoteFileMessageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return  RemoteFileHolder(binding)
            }

            6 -> { /*remote estimate*/
//                (holder as RemoteEstimateHolder).bind(dataList[position]).also{ }
                val binding=  LayoutEstimationMessageRemoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return RemoteEstimateHolder(binding)
            }
        }
        return RemoteFileHolder( LayoutRemoteFileMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ))
    }


    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun getItemViewType(position: Int): Int {
        Log.d(TAG, "getItemViewType: ")
        /*refere to MessageTypeEnum class _>*/
        if(dataList[position].isLocalMessage)
        {
            when(dataList[position].messageType)
            {
                VCConstants.TEXT_MESSAGE->{ return 1}
                VCConstants.FILE_MESSAGE->{ return 2}
                VCConstants.ESTIMATION_MESSAGE->{ return 3}
            }
        }else{
            when(dataList[position].messageType)
            {
                VCConstants.TEXT_MESSAGE->{return 4}
                VCConstants.FILE_MESSAGE->{return 5}
                VCConstants.ESTIMATION_MESSAGE->{return 6}
            }
        }
        return if (dataList[position].isLocalMessage) 1 else 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(getItemViewType(position))
        {
            1->{ /*local text*/
                (holder as LocalTextHolder).bind(dataList[position]).also{}
            }
            2->{ /*local file*/
                (holder as LocalFileHolder).bind(dataList[position]).also{ holder.itemView.setOnClickListener{listener.openURLInWeb(dataList[position].serverFilePath)}}
            }
            3->{ /*local estimate*/
                (holder as LocalEstimateHolder).bind(dataList[position]).also{ }
            }

            4->{ /*remote text*/
                (holder as RemoteTextHolder).bind(dataList[position]).also{ }
            }
            5->{ /*remote file*/
                (holder as RemoteFileHolder).bind(dataList[position]).also{ holder.itemView.setOnClickListener{listener.openURLInWeb(dataList[position].serverFilePath)}}
            }
            6->{ /*remote estimate*/
                (holder as RemoteEstimateHolder).bind(dataList[position]).also{ }
            }
        }
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


    /*view holders*/
    /*local text*/
    inner class LocalTextHolder(
        private val binding: LayoutLocalTextMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: MessageModel) {
            binding.localUserNameTv.text = data.userName
            binding.localMessageTv.text = data.messageText
            binding.tvLocalTimeStamp.text = getLocalHoursAndMinutesFromMilliseconds(data.id!!).first.toString() + ":"+
                    getLocalHoursAndMinutesFromMilliseconds(data.id!!).second.toString()
            updateLocalMessageStatusUI(binding.tvLocalMsgStatus,data.status,data.messageType)
        }
    }

    /*local file*/
    inner class LocalFileHolder(
        private val binding: LayoutLocalFileMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: MessageModel) {
            binding.localUserNameTv.text = data.userName
            binding.localFileDocTv.text = data.fileName
            binding.tvLocalTimeStamp.text = getLocalHoursAndMinutesFromMilliseconds(data.id!!).first.toString() + ":"+
                    getLocalHoursAndMinutesFromMilliseconds(data.id!!).second.toString()
            setFileTypeUi(binding.localFileTypeTv, FileOperations.getFileExtension(data.fileName))
            updateLocalMessageStatusUI(binding.tvLocalMsgStatus,data.status,data.messageType)
        }
    }

    /*local estimate*/
    inner class LocalEstimateHolder(
        private val binding: LayoutEstimationMessageSelfBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: MessageModel) {
            binding.userNameTv.text = data.userName
            binding.tvTimeStampSelf.visibility = View.VISIBLE
            binding.tvTimeStampSelf.text = getLocalHoursAndMinutesFromMilliseconds(data.id!!).first.toString() + ":"+
                    getLocalHoursAndMinutesFromMilliseconds(data.id!!).second.toString()
            updateLocalMessageStatusUI(binding.tvLocalMsgStatus,data.status,data.messageType)



            if(PreferenceManager.getuserType() == VCConstants.UserType.SERVICE_PERSON.value) {
                binding.tvCheckboxTitle.visibility = View.GONE
            }

            var partListAdapter = PartListAdapter(
                mContext  = mContext ,
                dataList = dataList[position].estimationDetails!!.part_list as ArrayList<Part>,
                parentPosition = position,
                messageScreenContext as MessageFragment,
                isCheckboxVisible = PreferenceManager.getuserType() != VCConstants.UserType.SERVICE_PERSON.value,
                isCheckboxSelectable = dataList[position].estimationDetails?.estimationApprovalStatus.isNullOrEmpty()
            )
            binding.rvEstimationPartList.adapter = partListAdapter
            partListAdapter.notifyDataSetChanged()

            var labourListAdapter  = LabourListAdapter(
                mContext = mContext,
                dataList = dataList[position].estimationDetails!!.labour_list as ArrayList<Labour>,
                parentPosition = position,
                messageScreenContext as MessageFragment,
                isCheckBoxVisible = PreferenceManager.getuserType() != VCConstants.UserType.SERVICE_PERSON.value,
                isCheckboxSelectable = dataList[position].estimationDetails?.estimationApprovalStatus.isNullOrEmpty()
            )
            binding.rvEstimationLabourList.adapter = labourListAdapter
            labourListAdapter.notifyDataSetChanged()

            Log.d(TAG, "estimateStatus: local: bind:  ${dataList[position].estimationDetails?.estimationApprovalStatus}")

            if(dataList[position].estimationDetails?.estimationApprovalStatus!=null) {
                binding.tvEstimateStatus.visibility = View.VISIBLE
                if(dataList[position].estimationDetails?.estimationApprovalStatus == "Y") {
                    binding.tvEstimateStatus.text = "Approved"
                    binding.tvEstimateStatus.setTextColor(mContext.resources.getColor(R.color.green))
                }else {
                    binding.tvEstimateStatus.text = "Rejected"
                    binding.tvEstimateStatus.setTextColor(mContext.resources.getColor(R.color.text_colour1))
                }
            }else {
                binding.tvEstimateStatus.visibility = View.GONE
            }

        }
    }

    /*local text*/
    inner class RemoteTextHolder(
        private val binding: LayoutRemoteTextMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: MessageModel) {
            binding.remoteUserNameTv.text = data.userName
            binding.remoteMessageTv.text = data.messageText
            binding.tvRemoteTimeStamp.text = getLocalHoursAndMinutesFromMilliseconds(data.id!!).first.toString() + ":"+
                    getLocalHoursAndMinutesFromMilliseconds(data.id!!).second.toString()
        }
    }

    /*local file*/
    inner class RemoteFileHolder(
        private val binding: LayoutRemoteFileMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: MessageModel) {
            binding.remoteUserNameTv.text = data.userName
            binding.remoteFileDocTv.text = data.fileName
            binding.tvRemoteTimeStamp.text = getLocalHoursAndMinutesFromMilliseconds(data.id!!).first.toString() + ":"+
                    getLocalHoursAndMinutesFromMilliseconds(data.id!!).second.toString()
            setFileTypeUi(binding.remoteFileTypeTv, FileOperations.getFileExtension(data.fileName))
        }
    }

    /*local estimate*/
    inner class RemoteEstimateHolder(
        private val binding: LayoutEstimationMessageRemoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: MessageModel) {
            binding.remoteUserNameTv.text = data.userName
            binding.tvRemoteTimeStamp.text = getLocalHoursAndMinutesFromMilliseconds(data.id!!).first.toString() + ":"+
                    getLocalHoursAndMinutesFromMilliseconds(data.id!!).second.toString()

            Log.d(TAG, "bind: test partList ${dataList[position].estimationDetails!!}")
//            if(dataList[position].estimationDetails!=null) {
//                binding.checkboxSelectAll.isChecked = dataList[position].estimationDetails!!.areAllItemsSelected
//            }

            binding.checkboxSelectAll.post {
                if (dataList[position].estimationDetails != null) {
                    binding.checkboxSelectAll.isChecked = dataList[position].estimationDetails!!.areAllItemsSelected
                }
            }


            if(dataList[position].estimationDetails?.estimationApprovalStatus!=null) {
                binding.btnRejectEstimation.visibility = View.GONE
                binding.btnApproveEstimation.visibility = View.GONE
            }else {
                binding.btnRejectEstimation.visibility = View.VISIBLE
                binding.btnApproveEstimation.visibility = View.VISIBLE
            }

            if(PreferenceManager.getuserType() == VCConstants.UserType.CUSTOMER.value && dataList[position].estimationDetails?.estimationApprovalStatus!="Y") {

                // Check for selected parts
                val selectedParts = dataList[position].estimationDetails?.part_list?.filter { part -> part.isSelected == "Y" }

                // Check for selected labor items
                val selectedLabors = dataList[position].estimationDetails?.labour_list?.filter { labor -> labor.isSelected == "Y" }

                // Update the UI based on the selected parts and labor items
                if (selectedParts!!.isNotEmpty() || selectedLabors!!.isNotEmpty()) {
                    // At least one part or labor item is selected
                    // Update the UI accordingly
                    binding.btnRejectEstimation.visibility = View.GONE
                } else {
                    if(dataList[position].estimationDetails!!.estimationApprovalStatus == null) {
                        binding.btnRejectEstimation.visibility = View.VISIBLE
                    }

                    // No part or labor item is selected
                    // Update the UI accordingly
                }
            }



            var partListAdapter = PartListAdapter(
                mContext  = mContext ,
                dataList = dataList[position].estimationDetails!!.part_list as ArrayList<Part>,
                position,
                messageScreenContext as MessageFragment,
                isCheckboxVisible = null,
                isCheckboxSelectable = dataList[position].estimationDetails?.estimationApprovalStatus.isNullOrEmpty()
            )
            binding.rvReceiverEstimationPartList.adapter = partListAdapter
            partListAdapter.notifyDataSetChanged()


            var labourListAdapter  = LabourListAdapter(
                mContext = mContext,
                dataList = dataList[position].estimationDetails!!.labour_list as ArrayList<Labour>,
                parentPosition = position,
                messageScreenContext as MessageFragment,
                isCheckBoxVisible = null,
                isCheckboxSelectable = dataList[position].estimationDetails?.estimationApprovalStatus.isNullOrEmpty()
            )
            binding.rvReceiverEstimationLabourList.adapter = labourListAdapter
            labourListAdapter.notifyDataSetChanged()


            binding.tvGrandTotalValue.text = dataList[position].estimationDetails!!.selectedItemsTotal.toString()

            if(PreferenceManager.getuserType().equals(VCConstants.UserType.CUSTOMER.value)){
                binding.rvEstimationApprovalLayout.visibility= View.VISIBLE
                binding.checkboxSelectAll.isClickable =
                    dataList[position].estimationDetails?.estimationApprovalStatus == null
            }else {
                binding.checkboxSelectAll.visibility = View.GONE
                binding.rvEstimationApprovalLayout.visibility= View.GONE
                if(dataList[position].estimationDetails?.estimationApprovalStatus!=null) {
                    Log.d(TAG, "onBindViewHolder: estimationStatus: ${dataList[position].estimationDetails?.estimationApprovalStatus}")
                    if(dataList[position].estimationDetails?.estimationApprovalStatus == "Y") {
                        binding.tvEstimateStatusReceiver.visibility = View.VISIBLE
                        binding.tvEstimateStatusReceiver.text = "Approved"
                        binding.tvEstimateStatusReceiver.setTextColor(mContext.resources.getColor(R.color.green))
                    }else {
                        binding.tvEstimateStatusReceiver.visibility = View.VISIBLE
                        binding.tvEstimateStatusReceiver.text = "Rejected"
                        binding.tvEstimateStatusReceiver.setTextColor(mContext.resources.getColor(R.color.text_colour1))
                    }
                }else {
                    binding.tvEstimateStatusReceiver.visibility = View.GONE
                }
            }

            binding.checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
                selectAllListener?.onSelectAllClicked(
                    parentPosition = position,
                    isSelected = isChecked,
                    estimateDetails =dataList[position].estimationDetails!!
                )
            }

            binding.btnApproveEstimation.setOnClickListener {
                var now = System.currentTimeMillis()
                if (now - lastClickTime < clickTimeInterval) {
                    return@setOnClickListener
                }
                lastClickTime = now
                if (!isClickable) {
                    return@setOnClickListener
                } else {
                    val selectedParts = dataList[position].estimationDetails?.part_list?.filter { part -> part.isSelected == "Y" }

                    // Check for selected labor items
                    val selectedLabors = dataList[position].estimationDetails?.labour_list?.filter { labor -> labor.isSelected == "Y" }

                    // Update the UI based on the selected parts and labor items
                    if (selectedParts!!.isNotEmpty() || selectedLabors!!.isNotEmpty()) {
                        onAcceptClickListener?.onAcceptClicked(position,dataList[position].estimationDetails!!)
                    } else {
                        if(dataList[position].estimationDetails!!.estimationApprovalStatus == null) {
                            Toast.makeText(mContext,"Please select labours and parts", Toast.LENGTH_SHORT).show()
                        }
                        // No part or labor item is selected
                        // Update the UI accordingly
                    }
                }
            }

            binding.btnRejectEstimation.setOnClickListener {
                var now = System.currentTimeMillis()
                if (now - lastClickTime < clickTimeInterval) {
                    return@setOnClickListener
                }
                lastClickTime = now
                if (!isClickable) {
                    return@setOnClickListener
                } else {
                    onRejectClickListener?.onRejectClicked(position,dataList[position].estimationDetails!!)
                }
            }

        }
    }

    interface OnAcceptClickListener {
        fun onAcceptClicked(parentPosition: Int, estimateDetails: ResponseModelEstimateData)
    }

    interface OnRejectClickListener {
        fun onRejectClicked(parentPosition: Int, estimateDetails: ResponseModelEstimateData)
    }

    interface MessagesAdapterListener {
        fun onMessageSelectionChanged(message: MessageModel, isSelected: Boolean)
    }

    interface SelectAllItemsClickListener {
        fun onSelectAllClicked(parentPosition: Int, isSelected: Boolean, estimateDetails: ResponseModelEstimateData)
    }
}


/**
 * extra listener code -> to download and view, retry and only view
 *  /*to download and view...comment this if not in requirement*/
 *                             /*download and view logic */
 *                             /*file download or open logic*/
 *                             /* if(dataList[position].isLocalMessage){
 *                                  listener.openFile(dataList[position])
 *                              }else{
 *                                  if(!dataList[position].status.equals(MessageStatusEnum.FILE_DOWNLOAD_PROGRESS.tag,false)){
 *                                      listener.downloadFileAndOpen(dataList[position])
 *                                  }else {
 *                                  Log.d(TAG, "onBindViewHolder: already downloading!")}
 *                              }*/
 *                             /*download and view logic */
 *                             /*retry logic -> to be added with extra status checking conditions - please check before using this*/
 *                             /*if(dataList[position].isLocalMessage)
 *                             {
 *                                 listener.resendMessage(dataList[position])
 *                             }else
 *                             {
 *                                 Log.d(TAG, "" +
 *                                         ": fileName::"+dataList[position].downloadableFileName+"id:"+dataList[position].id)
 *                             }*/
 *                             /*retry logic*/*/