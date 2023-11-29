package com.app.vc.message

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.vc.AndroidUtils
import com.app.vc.MainViewModel
import com.app.vc.R
import com.app.vc.baseui.BaseFragment
import com.app.vc.databinding.FragmentMessageBinding
import com.app.vc.models.MessageModel
import com.app.vc.network.ApiDetails


/* created by Naghma 27/09/23*/

class MessageFragment : BaseFragment(), MessageClickListener {

    override val TAG= "MessageFragment::"
    private val viewModel: MessageViewModel by viewModels()
    private val sharedViewModel: MainViewModel by activityViewModels()


    private lateinit var binding: FragmentMessageBinding


    private var dataList = ArrayList<MessageModel>()
//    private lateinit var adapter: MessageAdapterNew
    private lateinit var adapter: MessageAdapterMutliple
    private lateinit var mContext: Context


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
            sharedViewModel.openFileManager.value = true
        }

        binding.messageToolBar.setNavigationOnClickListener {
            /*close the messages screen*/
            sharedViewModel.messageFragmentClose.value = true
        }
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
    }


    private fun setUpRecyclerView() {
        Log.d(TAG, "setUpRecyclerView: new DataList ->${dataList.size}")
        /*new setup recycler view for the same */
        binding.rvData.layoutManager = LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false)
//        adapter = MessageAdapterNew(dataList,this)
        adapter = MessageAdapterMutliple(dataList,this)
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