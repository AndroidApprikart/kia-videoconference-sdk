package com.app.vc.message

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.vc.MainViewModel
import com.app.vc.R
import com.app.vc.baseui.BaseFragment
import com.app.vc.databinding.FragmentMessageBinding
import com.app.vc.models.MessageModel
import dagger.hilt.android.AndroidEntryPoint

/* created by Naghma 27/09/23*/

@AndroidEntryPoint
class MessageFragment : BaseFragment(), MessageClickListener {

    override val TAG= "MessageFragment::"
    private val viewModel: MessageViewModel by viewModels()
    private val sharedViewModel: MainViewModel by activityViewModels()


    private lateinit var binding: FragmentMessageBinding


    private var dataList = ArrayList<MessageModel>()
    private lateinit var adapter: MessageAdapter
    private lateinit var mContext: Context


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

        viewModelObservers()
        setUpRecyclerView()
//        processNewRemoteMessage(sharedViewModel.messageList)
    }

    private fun init() {
        Log.d(TAG, "init: sharedViewModel.messageListInMVM -> ${sharedViewModel.messageListInMVM}")
        dataList.clear()
        dataList.addAll(sharedViewModel.messageListInMVM)

    }

    private fun setUpClickListeners(){
        binding.btnSendMessage.setOnClickListener{
            if(viewModel.validateUserInputMessage()){
                /*new message creation*/
                viewModel.userMessageInput.value?.let { it1 ->
                    sharedViewModel.processNewLocalTextMessage(
                        it1
                    )
                }
            }else{
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
                if(it)
                {
                    processNewRemoteMessage(sharedViewModel.newRemoteMessage)
                }else
                {
                    /*do nothing*/
                }
            }
        }
        sharedViewModel.addNewLocalMessage.observe(viewLifecycleOwner){
            Log.d(TAG, "viewModelObservers: addNewLocalMessage ")
            it?.let {
                if(it)
                {
                    processNewLocalMessage(sharedViewModel.newLocalMessage)

                }else
                {
                    /*do nothing*/
                }
            }
        }
        sharedViewModel.updateLocalMessage.observe(viewLifecycleOwner){
            it?.let {
                if(true)
                {
//                    processUpdateLocalMessage(sharedViewModel.oldLocalMessage)
                }else
                {
                    /*do nothing*/
                }
            }
        }
    }


    private fun setUpRecyclerView() {
        Log.d(TAG, "setUpRecyclerView: new DataList ->${dataList.size}")
        /*new setup recycler view for the same */
        binding.rvData.layoutManager = LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false)
        adapter = MessageAdapter(dataList,this)
        binding.rvData.adapter = adapter
    }

    private fun processNewRemoteMessage(messages: MessageModel) {
        Log.d(TAG, "processNewRemoteMessage: ")
        dataList.add(messages)
        adapter.notifyDataSetChanged()
        binding.rvData.smoothScrollToPosition(dataList.size - 1 )
    }

    private fun processNewLocalMessage(messages: MessageModel) {
        Log.d(TAG, "processNewLocalMessage: ")
        dataList.add(messages)
        adapter.notifyDataSetChanged()
        binding.rvData.smoothScrollToPosition(dataList.size - 1)
    }

    private fun processUpdateLocalMessage(messages: MessageModel) {
        Log.d(TAG, "processUpdateLocalMessage: ")
        /*to update the download and upload status-es*/
//        dataList.add(messages)
//        adapter.notifyDataSetChanged()
    }

    override fun openURLInWeb(url: String) {
        val sendIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
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


}