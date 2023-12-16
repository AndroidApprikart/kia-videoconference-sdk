package com.app.vc.message

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.vc.AndroidUtils
import com.app.vc.MainViewModel
import com.app.vc.PreferenceManager
import com.app.vc.R
import com.app.vc.VCConstants
import com.app.vc.baseui.BaseFragment
import com.app.vc.databinding.FragmentMessageBinding
import com.app.vc.databinding.LayoutDialogConfirmationBinding
import com.app.vc.models.MessageModel
import com.app.vc.network.ApiDetails
import com.google.gson.Gson
import com.kia.vc.message.Labour
import com.kia.vc.message.LabourListAdapter
import com.kia.vc.message.Part
import com.kia.vc.message.PartListAdapter


/* created by Naghma 27/09/23*/

class MessageFragment : BaseFragment(), MessageClickListener, LabourListAdapter.OnLabourCheckboxSelectedListener,
    PartListAdapter.OnPartCheckboxSelectedListener {

    override val TAG= "MessageFragment::"
    private val viewModel: MessageViewModel by viewModels()
    private val sharedViewModel: MainViewModel by activityViewModels()


    private lateinit var binding: FragmentMessageBinding


    private var dataList = ArrayList<MessageModel>()
//    private lateinit var adapter: MessageAdapterNew
    private lateinit var adapter: MessageAdapterMutliple
    private lateinit var mContext: Context

    var selectedPartList = kotlin.collections.ArrayList<String>()
    var selectedLabourList = kotlin.collections.ArrayList<String>()
    private lateinit var estimationConfirmationDialog: Dialog

    var isClickable = true
    var lastClickTime = System.currentTimeMillis()
    var clickTimeInterval = 2000


    /*uncomment for download and open dunitnality*/
    /*private var downloadManager: DownloadManager? = null
    private lateinit var fileDownloadBroadCastReceiver: BroadcastReceiver*/
    override fun onAttach(c: Context) {
        context?.let {
            super.onAttach(it)
            mContext = c
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
//        binding = FragmentChatBinding.inflate(layoutInflater)
//
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_message, container, false)
        binding.messageVM = viewModel
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        setUpClickListeners()



//        processNewRemoteMessage(sharedViewModel.messageList)
        /*downloadManager = mContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager*/
        /*registerDownloadReceiver()*/
    }

    private fun init() {
        Log.d(TAG, "init: sharedViewModel.messageListInMVM -> ${sharedViewModel.messageListInMVM}")
        dataList.clear()
        dataList.addAll(sharedViewModel.messageListInMVM)


        if(PreferenceManager.getuserType() == VCConstants.UserType.CUSTOMER.value) {
            binding.btnSaveChat?.visibility = View.GONE
            binding.es.visibility = View.GONE
        }else if(PreferenceManager.getuserType() == "SERVICE_PERSON") {
            if(sharedViewModel.roNo.isNullOrEmpty()) {
                binding.es.visibility = View.GONE
            }
        }

    }

    override fun onStart() {
        super.onStart()
    }


    private fun setUpClickListeners(){
        binding.btnSendMessage.setOnClickListener{
            binding.btnSendMessage.isEnabled = false
            if(viewModel.validateUserInputMessage()){
                /*new message creation*/
                viewModel.userMessageInput.value?.let { input ->
                    sharedViewModel.processNewLocalTextMessage(
                        userInputText = input,
                        id = AndroidUtils.getCurrentTimeInMill(),
                    )
                    binding.edUseRInput.text?.clear()
                }
//                viewModel.userMessageInput.value = null
//                hideSoftKeyboard(binding.edUseRInput)
                binding.btnSendMessage.isEnabled = true
            }else{
                binding.btnSendMessage.isEnabled = true
                Toast.makeText(mContext, "Enter some text to send!", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnAttachFile.setOnClickListener{
            /*open file explorer*/

            var now = System.currentTimeMillis()
            if (now - lastClickTime < clickTimeInterval) {
                return@setOnClickListener
            }
            lastClickTime = now
            if (!isClickable) {
                return@setOnClickListener
            } else {
                sharedViewModel.openFileManager.value = true
            }

        }

        binding.messageToolBar.setNavigationOnClickListener {
            /*close the messages screen*/
            sharedViewModel.messageFragmentClose.value = true
        }

        binding.es.setOnClickListener {
            sharedViewModel.isProgressBarVisible.value = true
//            sharedViewModel.getEstimationDetails.value = true


            if(isEstimationShared()) {
                if(!isAnyEstimationApproved()) {
                    if(canEstimationBeSent()) {
                        sharedViewModel.getEstimationDetails.value = true
                    }else {
                        sharedViewModel.isProgressBarVisible.value = false
                        showConfirmationDialog(
                            context = requireActivity(),
                            isCancelable = true,
                            title = "Already Shared !!!",
                            message = "Estimation cannot be sent.",
                            isCancelButtonVisible = false
                        ) {
                            Log.d(TAG, "setUpObserver: Estimation details has been shared and approved.")
                        }
                    }
                }else {
                    sharedViewModel.isProgressBarVisible.value = false
                    // Estimation is shared and approved at least once
                    showConfirmationDialog(
                        context = requireActivity(),
                        isCancelable = true,
                        title = "Already Shared !!!",
                        message = "Estimation details has been shared and approved.",
                        isCancelButtonVisible = false
                    ) {
                        Log.d(TAG, "setUpObserver: Estimation details has been shared and approved.")
                    }
                }
            }else {
                //Estimation is not shared at all
                sharedViewModel.getEstimationDetails.value = true
            }
        }

        binding.btnSaveChat?.setOnClickListener {
            sharedViewModel.isProgressBarVisible.value = true
            if(AndroidUtils.isNetworkAvailable(mContext)) {
                viewModel.saveMessageList()
            }else {
                sharedViewModel.toastMessage.value  = "No Internet connection."
                sharedViewModel.isProgressBarVisible.value = false
            }
        }
    }


    fun showConfirmationDialog(
        context: Context,
        isCancelable: Boolean,
        title: String,
        message: String,
        isCancelButtonVisible: Boolean,
        onPositiveButtonClickListener: View.OnClickListener
    ) {
        val binding = LayoutDialogConfirmationBinding.inflate(LayoutInflater.from(context))
        binding.tvDialogTitle.text = title
        binding.tvDialogMessage.text = message

        binding.tvCancelButton.isVisible = isCancelButtonVisible

        val confirmationDialog = Dialog(context)
        confirmationDialog.setContentView(binding.root)
        confirmationDialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        confirmationDialog.setCancelable(isCancelable)

        binding.btnUpdate.setOnClickListener {
            onPositiveButtonClickListener.onClick(it)
            confirmationDialog.dismiss()
        }

        binding.tvCancelButton.setOnClickListener {
            confirmationDialog.dismiss()
        }

        confirmationDialog.show()
    }
    private fun isEstimationShared():Boolean {
        for (message in dataList) {
            if(message.estimationDetails!=null) {
                return true
            }
        }
        return false
    }
    private fun isAnyEstimationApproved(): Boolean{
        for(message in dataList) {
            if(message.estimationDetails?.estimationApprovalStatus =="Y") {
                return true
            }
        }
        return false
    }

    private fun canEstimationBeSent():Boolean {
        //Cases estimation is sent and not approved even once.
        var canBeSent:Boolean? = null

        for(message in dataList) {
            if(message.estimationDetails?.estimationApprovalStatus =="N") {
                canBeSent = true
                continue
            }
            if(message.estimationDetails!=null) {
                canBeSent = false
            }
        }

        return canBeSent!!
    }

    private fun viewModelObservers(){
        sharedViewModel.addNewRemoteMessage.observe(viewLifecycleOwner){
            Log.d(TAG, "viewModelObservers: addNewRemoteMessage")
            it?.let {
                if(it!=-1L)
                {
                    sharedViewModel.getMessageFromId(it)?.let{messageAndIndexPair ->
                        if(messageAndIndexPair.first!=null) {
                            processNewRemoteMessage(messageAndIndexPair.first!!)
                        }
                    }
                    sharedViewModel.addNewRemoteMessage.value = -1L /*to stop re observing when message fragment obserign it*/
                }
            }
        }
        sharedViewModel.addNewLocalMessage.observe(viewLifecycleOwner){
            Log.d(TAG, "viewModelObservers: addNewLocalMessage  ")
            it?.let {
                if(it!=-1L)
                {
                    sharedViewModel.getMessageFromId(it)?.let{messageAndIndexPair->
                        if(messageAndIndexPair.first!=null) {
                            processNewLocalMessage(messageAndIndexPair.first!!)
                        }
                    }
                    sharedViewModel.addNewLocalMessage.value = -1L /*to stop re observing when message fragment obserign it*/
                }
            }
        }
        sharedViewModel.updateLocalMessage.observe(viewLifecycleOwner){
            Log.d(TAG, "viewModelObservers: updateLocalMessage")
            it?.let {
                if(it!=-1L)
                {
                    sharedViewModel.getMessageFromId(it).let{messageIndexPair ->
                        if (messageIndexPair.first != null) {
                            processUpdateLocalMessage(messageIndexPair.first!!)
                        }
                    }
                    sharedViewModel.updateLocalMessage.value = -1L /*to stop re observing when message fragment obserign it*/
                }
            }
        }

        sharedViewModel.isSuccessEstimationResponse.observe(viewLifecycleOwner) {
            if(it) {
                Log.d(TAG, "viewModelObservers: updateAdapterPosition: successResponse: true: ")
                if(sharedViewModel.tempParentPosition!=null) {
                    if(sharedViewModel.estimateDetailsAfterApproval?.estimationApprovalStatus!=null) {
                        dataList[sharedViewModel.tempParentPosition!!].estimationDetails?.estimationApprovalStatus = sharedViewModel.estimateDetailsAfterApproval?.estimationApprovalStatus
                        adapter.notifyItemChanged(sharedViewModel.tempParentPosition!!)
                        sharedViewModel.isProgressBarVisible.value = false
                    }else {
                        Log.d(TAG, "viewModelObservers: updateAdapterPosition status: approvalStatus: Null ::  ${sharedViewModel.tempMessageModelAfterApprovalOrReject?.estimationDetails} ")
                    }
                }else {
                    Log.d(TAG, "viewModelObservers: updateAdapterPosition: successResponse: false: ")
                    sharedViewModel.toastMessage.value = "tempPosition Null"
                }

            }
        }

        viewModel.saveMessageList.observe(viewLifecycleOwner) {
            if(it!=null) {
                if(it) {
                    //get message list and update thhe message list and save it.
                    sharedViewModel.saveMessageList.value = getChatList()
                }
            }
        }
    }

    private fun getChatList(): kotlin.collections.ArrayList<ChatModelItem>{
        var chatList = kotlin.collections.ArrayList<ChatModelItem>()

        if(dataList.isNotEmpty()) {
            for(message in dataList) {
                if(message.messageType == VCConstants.TEXT_MESSAGE) {
                    chatList.add(
                        when(PreferenceManager.getuserType()) {
                            "SERVICE_PERSON" -> {
                                ChatModelItem(
                                    chat_box =message.messageText ,
                                    mes_from_id = sharedViewModel.serviceAdvisorID.toString(),
                                    mes_to_id = sharedViewModel.customerCode.toString(),
                                    message_type = VCConstants.SaveChatMessageType.TEXT.value,
                                    vc_id =sharedViewModel.roomID!!
                                )
                            }
                            else -> {
                                ChatModelItem(
                                    chat_box =message.messageText ,
                                    mes_from_id = sharedViewModel.customerCode.toString(),
                                    mes_to_id =sharedViewModel.serviceAdvisorID.toString() ,
                                    message_type = VCConstants.SaveChatMessageType.TEXT.value,
                                    vc_id = sharedViewModel.roomID!!
                                )
                            }
                        }

                    )
                }else if(message.estimationDetails!=null) {
                    var estimateJsonString:String  = Gson().toJson(message.estimationDetails)
                    // Commented as estimation details will not be saved.
//                    chatList.add(
//                        when(PreferenceManager.getuserType()) {
//                            "SERVICE_PERSON" -> {
//                                ChatModelItem(
//                                    chat_box = estimateJsonString,
//                                    mes_from_id = vcScreenViewModel.serviceAdvisorID.toString(),
//                                    mes_to_id = vcScreenViewModel.customerCode.toString(),
//                                    message_type = Constants.Companion.MessageType.ESTIMATION.value,
//                                    vc_id =vcScreenViewModel.roomID!!
//                                )
//                            }
//                            else -> {
//                                ChatModelItem(
//                                    chat_box =estimateJsonString!! ,
//                                    mes_from_id = vcScreenViewModel.customerCode.toString(),
//                                    mes_to_id =vcScreenViewModel.serviceAdvisorID.toString() ,
//                                    message_type = Constants.Companion.MessageType.ESTIMATION.value,
//                                    vc_id = vcScreenViewModel.roomID!!
//                                )
//                            }
//                        }
//                    )
                }else {
                    chatList.add(
                        when(PreferenceManager.getuserType()) {
                            "SERVICE_PERSON" -> {
                                ChatModelItem(
                                    chat_box =message.fileName ,
                                    mes_from_id = sharedViewModel.serviceAdvisorID.toString(),
                                    mes_to_id = sharedViewModel.customerCode.toString(),
                                    message_type = VCConstants.SaveChatMessageType.FILE.value,
                                    vc_id =sharedViewModel.roomID!!
                                )
                            }
                            else -> {
                                ChatModelItem(
                                    chat_box =message.fileName ,
                                    mes_from_id = sharedViewModel.customerCode.toString(),
                                    mes_to_id =sharedViewModel.serviceAdvisorID.toString() ,
                                    message_type = VCConstants.SaveChatMessageType.FILE.value,
                                    vc_id = sharedViewModel.roomID!!
                                )
                            }
                        }
                    )
                }
            }
        }


        return chatList
    }


    private fun setUpRecyclerView() {
        Log.d(TAG, "setUpRecyclerView: new DataList ->${dataList.size}")
        /*new setup recycler view for the same */
        binding.rvData.layoutManager = LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false)
//        adapter = MessageAdapterNew(dataList,this)
        adapter = MessageAdapterMutliple(dataList,this,mContext,this)
        binding.rvData.adapter = adapter
    }

    private fun processNewRemoteMessage(messages: MessageModel) {
           Log.d(TAG, "processNewRemoteMessage: ")
           dataList.add(messages)
           adapter.notifyItemInserted(dataList.lastIndex)
           binding.rvData.smoothScrollToPosition(dataList.lastIndex)


    }

    private fun processNewLocalMessage(messages: MessageModel) {
          Log.d(TAG, "processNewLocalMessage: ")
          dataList.add(messages)
          adapter.notifyItemInserted(dataList.lastIndex)
          binding.rvData.smoothScrollToPosition(dataList.lastIndex)

    }

    private fun processUpdateLocalMessage(oldMessageToUpdate: MessageModel) {
            Log.d(TAG, "processUpdateLocalMessage: ")
            /*to update the download and upload status-es and others and update the local list as well*/
            var foundIndex = -1
            for(i in dataList.indices)
            {
                if(dataList[i].id == oldMessageToUpdate.id)
                {
                    foundIndex = i
                    break;
                }
            }
            if(foundIndex!=-1)
            {
                /*replace this message with updated message and update adapter at the position*/
                Log.d(TAG, "processUpdateLocalMessage: message found foundIndex")
                dataList[foundIndex] = oldMessageToUpdate
                adapter.notifyItemChanged(foundIndex)
            }
    }

    override fun openURLInWeb(url: String) {
        Log.d(TAG, "openURLInWeb: url -> ${url}")
        val sendIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ApiDetails.MEDIA_BASE_URL+url))
        val chooser = Intent.createChooser(sendIntent, "Choose Your Browser")
        if (sendIntent.resolveActivity(mContext!!.packageManager) != null) {
            startActivity(chooser)
        }
//        val uri = Uri.parse(url)
//        val intent = Intent(Intent.ACTION_VIEW, uri)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//        if (mContext?.packageManager?.let { intent.resolveActivity(it) } != null) {
//            mContext.startActivity(intent)
//        }
    }

    override fun downloadFileAndOpen(data: MessageModel) { /*if requirement arises -> fetch code form extra code section in the file*/ }

    override fun openFile(data: MessageModel) {  /*if requirement arises -> fetch code form extra code section in the file*/ }

    override fun resendMessage(data: MessageModel) {  /*if requirement arises -> fetch code form extra code section in the file*/ }

    override fun onDestroy() {
        super.onDestroy()
//        mContext.unregisterReceiver(fileDownloadBroadCastReceiver)
    }
    fun hideSoftKeyboard(input:EditText ) {
            var imm = mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
    }

    override fun onResume() {
        super.onResume()
        hideSoftKeyboard(binding.edUseRInput)
        setUpRecyclerView()
        viewModelObservers()
        initializeAdapterListeners()
        if(dataList.isNotEmpty()){
            binding.rvData.smoothScrollToPosition(dataList.lastIndex)
        }
//        moveToLastPosition()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideSoftKeyboard(
            binding
                .edUseRInput
        )
    }

    override fun onLabourCheckboxClick(
        parentPosition: Int,
        childPosition: Int,
        isSelected: Boolean,
        arrayList: ArrayList<Labour>
    ) {
        updateLabourListData(parentPosition,childPosition,isSelected,arrayList)
        updateGrandTotalConsideringLabourList(
            parentPosition = parentPosition,
            childPosition = childPosition,
            isSelected = isSelected,
            labourList =  arrayList
        )


        if(dataList[parentPosition].estimationDetails?.labour_list!!.all{it.isSelected == "Y"} && dataList[parentPosition].estimationDetails?.part_list!!.all { it.isSelected =="N" }) {
            dataList[parentPosition].estimationDetails?.areAllItemsSelected = true
        }else {
            dataList[parentPosition].estimationDetails?.areAllItemsSelected = false
        }
        adapter.notifyItemChanged(parentPosition)
    }

    override fun onPartCheckBoxClicked(
        parentPosition: Int,
        childPosition: Int,
        isSelected: Boolean,
        arrayList: ArrayList<Part>
    ) {
        updatePartListData(parentPosition,childPosition,isSelected,arrayList)
        updateGrandTotalConsideringPartList(
            parentPosition = parentPosition,
            childPosition = childPosition,
            isSelected = isSelected,
            partList =  arrayList
        )
        if(dataList[parentPosition].estimationDetails?.labour_list!!.all{it.isSelected == "Y"} && dataList[parentPosition].estimationDetails?.part_list!!.all { it.isSelected =="N" }) {
            dataList[parentPosition].estimationDetails?.areAllItemsSelected = true
        }else {
            dataList[parentPosition].estimationDetails?.areAllItemsSelected = false
        }

        adapter.notifyItemChanged(parentPosition)
    }


    private fun updateLabourListData(
        parentPosition: Int,
        childPosition: Int,
        isSelected: Boolean,
        arrayList: ArrayList<Labour>
    ) {
        dataList[parentPosition].estimationDetails!!.labour_list[childPosition].isSelected =
            if (isSelected) "Y" else "N"

    }


    private fun updateGrandTotalConsideringPartList(
        parentPosition: Int,
        childPosition: Int,
        isSelected: Boolean,
        partList: ArrayList<Part>
    ):Double? {
        if(isSelected) {
            var totalValue = dataList[parentPosition].estimationDetails?.selectedItemsTotal!!.plus(dataList[parentPosition].estimationDetails!!.part_list[childPosition].totalPrice.toDouble())
            var roundedValue = "%.2f".format(totalValue).toDouble()
            dataList[parentPosition].estimationDetails?.selectedItemsTotal = roundedValue
        }else {
            var totalValue = dataList[parentPosition].estimationDetails?.selectedItemsTotal!!.minus(dataList[parentPosition].estimationDetails!!.part_list[childPosition].totalPrice.toDouble())
            var roundedValue = "%.2f".format(totalValue).toDouble()
            dataList[parentPosition].estimationDetails?.selectedItemsTotal = roundedValue
        }
        return null
    }

    private fun updateGrandTotalConsideringLabourList(
        parentPosition: Int,
        childPosition: Int,
        isSelected: Boolean,
        labourList: ArrayList<Labour>
    ):Double? {
        if(isSelected) {
            var totalValue = dataList[parentPosition].estimationDetails?.selectedItemsTotal!!.plus(dataList[parentPosition].estimationDetails!!.labour_list[childPosition].totalLabourCost.toDouble())
            var roundedValue = "%.2f".format(totalValue).toDouble()
            dataList[parentPosition].estimationDetails?.selectedItemsTotal = roundedValue
        }else {
            var totalValue =  dataList[parentPosition].estimationDetails?.selectedItemsTotal!!.minus(dataList[parentPosition].estimationDetails!!.labour_list[childPosition].totalLabourCost.toDouble())
            var roundedValue = "%.2f".format(totalValue).toDouble()
            dataList[parentPosition].estimationDetails?.selectedItemsTotal = roundedValue
        }
        return null
    }

    private fun updatePartListData(
        parentPosition: Int,
        childPosition: Int,
        isSelected: Boolean,
        arrayList: ArrayList<Part>
    ) {
        dataList[parentPosition].estimationDetails!!.part_list[childPosition].isSelected =
            if (isSelected) "Y" else "N"

    }

    private fun initializeAdapterListeners() {
        adapter.onAcceptClickListener = object : MessageAdapterMutliple.OnAcceptClickListener {
            override fun onAcceptClicked(
                parentPosition: Int,
                estimationDetails: ResponseModelEstimateData
            ) {
                sharedViewModel.isProgressBarVisible.value = true

                updateSelectedPartList(parentPosition,dataList[parentPosition].estimationDetails!!.part_list as ArrayList<Part>)
                updateSelectedLabourList(parentPosition,dataList[parentPosition].estimationDetails!!.labour_list as ArrayList<Labour>)

//                var approvedEstimateData = estimationDetails
                var approvedEstimateData = ResponseModelEstimateData(
                    deferred_job_list = estimationDetails.deferred_job_list,
                    estimationApprovalStatus = estimationDetails.estimationApprovalStatus,
                    labour_list = estimationDetails.labour_list,
                    part_list = estimationDetails.part_list,
                    totalEstimate = estimationDetails.totalEstimate,
                    totalLabourEstimate = estimationDetails.totalLabourEstimate,
                    totalPartsEstimate = estimationDetails.totalPartsEstimate,
                    selectedItemsTotal = estimationDetails.selectedItemsTotal
                )

                approvedEstimateData.estimationApprovalStatus = "Y"

                val partIterator = approvedEstimateData.part_list.iterator()
                while (partIterator.hasNext()) {
                    val part = partIterator.next()
                    if (part.isSelected != "Y") {
                        partIterator.remove()
                    }
                }

                val labourIterator = approvedEstimateData.labour_list.iterator()
                while (labourIterator.hasNext()) {
                    val labour = labourIterator.next()
                    if (labour.isSelected != "Y") {
                        labourIterator.remove()
                    }
                }
                sharedViewModel.estimateDetailsAfterApproval = approvedEstimateData

//                var estimateModel = MessageModel(
//                    "",
//                    sharedViewModel.displayName.toString(),
//                    true,
//                    isTextMessage = false,
//                    fileName = "",
//                    serverFilePath = null,
//                    fileLocal = null,
//                    downloadStatus = "",
//                    uploadStatus = "",
//                    downloadRefId = null,
//                    messageId = AndroidUtils.getCurrentTimeInMill(),
//                    estimationDetails = approvedEstimateData
//                )
//                sharedViewModel.tempMessageModelAfterApprovalOrReject = estimateModel


                sharedViewModel.tempParentPosition = parentPosition
                sharedViewModel.updateEstimateStatus.value = true
            }
        }

        adapter.onRejectClickListener = object : MessageAdapterMutliple.OnRejectClickListener {
            override fun onRejectClicked(parentPosition: Int,estimationDetails: ResponseModelEstimateData) {
                Log.d(TAG, "serviceAdvisorFeedback: onRejectClicked: ")
                sharedViewModel.isProgressBarVisible.value = true
                showDialogToConfirmEstimateRejection(parentPosition,estimationDetails)
            }

        }

        adapter.selectAllListener = object : MessageAdapterMutliple.SelectAllItemsClickListener {
            override fun onSelectAllClicked(
                parentPosition: Int,
                isSelected: Boolean,
                estimationDetails: ResponseModelEstimateData
            ) {
                Log.d(TAG, "onSelectAllClicked: ")
                dataList[parentPosition].estimationDetails = updateEstimationListToSelectAllItems(estimationDetails,isSelected)
                adapter.notifyDataSetChanged()
            }

        }
    }


    private fun updateSelectedPartList(parentPosition:Int, partList : kotlin.collections.ArrayList<Part>) {
        for(part in partList) {
            if(part.isSelected =="Y") {
                selectedLabourList.add(part.partNumber)
            }
        }
        if(selectedPartList.isNotEmpty()) {
            sharedViewModel.selectedPartList =selectedPartList.joinToString(",")
        }

    }

    private  fun updateEstimationListToSelectAllItems(estimateData: ResponseModelEstimateData,isSelected: Boolean): ResponseModelEstimateData {
        Log.d(TAG, "updateEstimationListToSelectAllItems: estimateData: ${estimateData}")
        val modifiedEstimateData = estimateData

        for(part in estimateData.part_list) {
            Log.d(TAG, "updateEstimationListToSelectAllItems: part: ${part}")
            if(isSelected) {
                part.isSelected = "Y"
            }else {
                part.isSelected = "N"
            }

        }

        for (labour in estimateData.labour_list) {
            Log.d(TAG, "updateEstimationListToSelectAllItems: labour: ${labour}")
            if(isSelected) {
                labour.isSelected = "Y"
            }else {
                labour.isSelected = "N"
            }
        }

        if(isSelected) {
            modifiedEstimateData.selectedItemsTotal = modifiedEstimateData.totalEstimate
        }else {
            modifiedEstimateData.selectedItemsTotal  = 0.0
        }

        if(estimateData.labour_list.all{it.isSelected == "Y"} && estimateData.part_list.all { it.isSelected =="N" }) {
            modifiedEstimateData.areAllItemsSelected = true
        }else {
            modifiedEstimateData.areAllItemsSelected = false
        }


        return modifiedEstimateData


    }
    private fun updateSelectedLabourList(parentPosition:Int, labourList : kotlin.collections.ArrayList<Labour>) {
        for(labour in labourList) {
            if(labour.isSelected =="Y") {
                selectedLabourList.add(labour.labourCode)
            }
        }
        if(selectedLabourList.isNotEmpty()) {
            sharedViewModel.selectedLabourList =selectedLabourList.joinToString(",")
        }
    }

    private fun showDialogToConfirmEstimateRejection(parentPosition: Int, estimationDetails: ResponseModelEstimateData) {
        estimationConfirmationDialog = Dialog(requireContext())
        estimationConfirmationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        var dialogBinding = LayoutDialogConfirmationBinding.inflate(LayoutInflater.from(context))
        estimationConfirmationDialog.setContentView(dialogBinding.root)
        dialogBinding.tvDialogMessage.text = "Are you sure you want to reject the estimation."
        estimationConfirmationDialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        estimationConfirmationDialog.setCancelable(false)
//        val cancel = estimationConfirmationDialog.findViewById(R.id.neg_btn) as TextView
//        val ok = estimationConfirmationDialog.findViewById(R.id.pos_btn) as TextView


        dialogBinding.tvCancelButton.setOnClickListener {
            estimationConfirmationDialog.dismiss()
            sharedViewModel.isProgressBarVisible.value = false
        }
        dialogBinding.btnUpdate.setOnClickListener {
            estimationConfirmationDialog.dismiss()
//            goBack()
//                finish()

//            var approvedEstimateData = estimationDetails
            var approvedEstimateData = ResponseModelEstimateData(
                deferred_job_list = estimationDetails.deferred_job_list,
                estimationApprovalStatus = estimationDetails.estimationApprovalStatus,
                labour_list = estimationDetails.labour_list,
                part_list = estimationDetails.part_list,
                totalEstimate = estimationDetails.totalEstimate,
                totalLabourEstimate = estimationDetails.totalLabourEstimate,
                totalPartsEstimate = estimationDetails.totalPartsEstimate,
                selectedItemsTotal = estimationDetails.selectedItemsTotal
            )
            approvedEstimateData.estimationApprovalStatus = "N"
            sharedViewModel.estimateDetailsAfterApproval = approvedEstimateData

//            var estimateModel = MessageModel(
//                "",
//                "",
//                true,
//                isTextMessage = false,
//                fileName = "",
//                serverFilePath = null,
//                fileLocal = null,
//                downloadStatus = "",
//                uploadStatus = "",
//                downloadRefId = null,
//                messageId = AndroidUtils.getCurrentTimeInMill(),
//                estimateDetails = approvedEstimateData
//            )
//
////            vcScreenViewModel.messageModelFromVC.value = estimateModel
//            sharedViewModel.tempMessageModelAfterApprovalOrReject = estimateModel
            sharedViewModel.tempParentPosition = parentPosition

            sharedViewModel.updateEstimateStatus.value = true
        }

        estimationConfirmationDialog.show()
    }

}
/****
 *Extra code(doenlaod and open, resend) to be uncommented when required

override fun downloadFileAndOpen(data: MessageModel) {
/*if required check for read write permissions*/
/*check if file exists.
 * if yes .. just open with an intent action to view
 * else ..download in downloads directory*/
/**/
Log.d(TAG, "downloadFileAndOpen: data.downloadableFileName"+data.downloadableFileName)
val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), data.downloadableFileName)
if (file.exists()) {
/*to make sure it is a new copy...may be this same file would have been downlaoded in the past...
yet still for this session we will consider this as a new file*/
if(data.status.equals(MessageStatusEnum.FILE_DOWNLOAD_SUCCESS.tag, false)){
/*fiel successfully downloaded -> open it
 * else -> download it and then open*/
/*open this with intent action view*/
val uri = FileProvider.getUriForFile(
mContext,
"com.app.vc" + ".provider",
file,
file.name
)
val sendIntent = Intent(Intent.ACTION_VIEW, uri)

sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
val chooser = Intent.createChooser(sendIntent, "Choose application to view")
if (sendIntent.resolveActivity(mContext!!.packageManager) != null) {
startActivity(chooser)
}
}else {
/*download a new copy*/
data.downloadReferenceId = downloadFile(
data.serverFilePath,
data.downloadableFileName,
Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
)
sharedViewModel.updateDownloadStatusForFileMessage(data.downloadReferenceId,data.serverFilePath,data.id,MessageStatusEnum.FILE_DOWNLOAD_PROGRESS.tag)

}

}else
{
/*download this*/
data.downloadReferenceId = downloadFile(
data.serverFilePath,
data.downloadableFileName,
Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
)
sharedViewModel.updateDownloadStatusForFileMessage(data.downloadReferenceId,data.serverFilePath,data.id,MessageStatusEnum.FILE_DOWNLOAD_PROGRESS.tag)
}
}

override fun openFile(data: MessageModel) {
/*if required check for read write permissions*/
/*check if file exists.
 * if yes .. just open with an intent action to view
 * else ..show does nto exist*/
val file = File(data.localFilePath)
if (file.exists()) {
//            /*open this with intent action view*/
val uri = FileProvider.getUriForFile(
mContext,
"com.app.vc" + ".provider",
file,
file.name
)
val sendIntent = Intent(Intent.ACTION_VIEW, uri)

sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
val chooser = Intent.createChooser(sendIntent, "Choose application to view")

if (sendIntent.resolveActivity(mContext!!.packageManager) != null) {
startActivity(chooser)
}
}else{
Toast.makeText(mContext, "File not present", Toast.LENGTH_SHORT).show()
}
}


fun downloadFile(
sourceFileUrlToDownload: String?,
fileName: String?,
destinationDirectory: File?,
): Long {
var file: File = File(destinationDirectory,fileName)
val uri = Uri.parse(ApiDetails.MEDIA_BASE_URL + sourceFileUrlToDownload)
val request = DownloadManager.Request(uri)
request.setTitle("Downloading $fileName")
request.setDescription("Downloading $fileName")
request.setVisibleInDownloadsUi(true)
request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
request.setDestinationUri(Uri.fromFile(file))
return downloadManager!!.enqueue(request)
}

private fun registerDownloadReceiver() {

val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
Log.d("MessageScreen::", "registerDownloadReceiver: ")

fileDownloadBroadCastReceiver = object : BroadcastReceiver() {

override fun onReceive(context: Context?, intent: Intent?) {
Log.d("MessageScreen::", "onReceive: ")
val downloadReference = intent!!.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

val query = DownloadManager.Query()
query.setFilterById(downloadReference)

val cur = downloadManager!!.query(query)

if (cur.moveToFirst()) {
val columnIndex = cur.getColumnIndex(DownloadManager.COLUMN_STATUS)
when {
DownloadManager.STATUS_SUCCESSFUL == cur.getInt(columnIndex) -> {
val uriString =
cur.getString(cur.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
val fileName = File(uriString).name
Log.d(
"MessageScreen::",
"onReceive: STATUS_SUCCESSFUL: fileName: $fileName"
)
Log.d(
"MessageScreen::",
"onReceive: STATUS_SUCCESSFUL: uriString: $uriString"
)

var msgIndex = -1
for(i in sharedViewModel.messageListInMVM.indices)
{
if(sharedViewModel.messageListInMVM[i].downloadReferenceId == downloadReference)
{
msgIndex = i
break;
}
}
if(msgIndex!=-1)
{
var data = sharedViewModel.messageListInMVM[msgIndex]
sharedViewModel.updateDownloadStatusForFileMessage(data.downloadReferenceId,data.serverFilePath,data.id,MessageStatusEnum.FILE_DOWNLOAD_SUCCESS.tag)
//                                downloadFileAndOpen(data) //call the same overriding function to open the downloaded remote file

}

}
DownloadManager.STATUS_FAILED == cur.getInt(columnIndex) -> {
val columnReason = cur.getColumnIndex(DownloadManager.COLUMN_REASON);
val reason = cur.getInt(columnReason);
Log.d("MessageScreen::", "onReceive: STATUS_FAILED: $reason ")
var msgIndex = -1
for(i in sharedViewModel.messageListInMVM.indices)
{
if(sharedViewModel.messageListInMVM[i].downloadReferenceId == downloadReference)
{
msgIndex = i
break;
}
}
if(msgIndex!=-1)
{
var data = sharedViewModel.messageListInMVM[msgIndex]
sharedViewModel.updateDownloadStatusForFileMessage(data.downloadReferenceId,data.serverFilePath,data.id,MessageStatusEnum.FILE_DOWNLOAD_FAILURE.tag)

}

when (reason) {
DownloadManager.ERROR_FILE_ERROR -> {
//                                Toast.makeText(context, "Download Failed.File is corrupt.", Toast.LENGTH_LONG).show();
}
DownloadManager.ERROR_HTTP_DATA_ERROR -> {
}
DownloadManager.ERROR_INSUFFICIENT_SPACE -> {
Toast.makeText(
context,
"Download Failed due to insufficient space in internal storage",
Toast.LENGTH_LONG
).show();
}
DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> {
Toast.makeText(
context,
"Download Failed. Http Code Error Found.",
Toast.LENGTH_LONG
).show();
}
DownloadManager.ERROR_UNKNOWN -> {
}
DownloadManager.ERROR_CANNOT_RESUME -> {
}
DownloadManager.ERROR_TOO_MANY_REDIRECTS -> {
}
DownloadManager.ERROR_DEVICE_NOT_FOUND -> {
Toast.makeText(
context,
"ERROR_DEVICE_NOT_FOUND",
Toast.LENGTH_LONG
).show();
}
}
}
DownloadManager.STATUS_RUNNING == cur.getInt(columnIndex) -> {}
}
}
cur.close()
}
}
mContext.registerReceiver(fileDownloadBroadCastReceiver, filter)
}

override fun resendMessage(data: MessageModel) {
Log.d(TAG, "resendMessage: ")
/*if text message -> check if status is sent_failed
 * if file message -> check if status is upload_failure*/
when(data.messageType)
{
VCConstants.TEXT_MESSAGE-> {
sharedViewModel.processResendLocalTextMessage(data.id,data)
}
VCConstants.FILE_MESSAGE-> {
sharedViewModel.processResendLocalFileMessage(data.fileName,data.serverFilePath,data.id,true)
sharedViewModel.uploadVcFileAPICall(File(data.localFilePath),"vcroom","userType","who",data.id,true)     /*07 Nov 2023:: IMP::nahusha help required here::to pass correct data for vc room, who and usertype*/
}
}
}
 **/