package com.app.vc.participants

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.app.vc.MainViewModel
import com.app.vc.R
import com.app.vc.databinding.FragmentParticipantsBinding
import com.app.vc.models.ParticipantsModel
import dagger.hilt.android.AndroidEntryPoint

/* created by Naghma 27/09/23*/

@AndroidEntryPoint
class ParticipantFragment : BottomSheetDialogFragment() {

    val TAG= "ParticipantFragment::"
    private val viewModel: ParticipantsViewModel by viewModels()
    private val sharedViewModel: MainViewModel by activityViewModels()


    private lateinit var binding: FragmentParticipantsBinding


    private var dataList = ArrayList<ParticipantsModel>()
    private lateinit var adapter: ParticipantAdapterNew
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
            DataBindingUtil.inflate(inflater, R.layout.fragment_participants, container, false)
//        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        setUpClickListeners()

        viewModelObservers()
        setUpRecyclerView()
        processParticipantData(sharedViewModel.participants)
    }

    private fun init() {


    }

    private fun setUpClickListeners(){

    }

    private fun viewModelObservers(){
        sharedViewModel.updateParticipants.observe(viewLifecycleOwner){
            it?.let {
                if(true)
                {
                    processParticipantData(sharedViewModel.participants)
                }else
                {
                    /*do nothing*/
                }
            }
        }
    }


    private fun setUpRecyclerView() {
        Log.d(TAG, "setUpRecyclerView: new DataList")
        /*new setup recycler view for the same */
        binding.rvData.layoutManager = LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false)
        adapter = ParticipantAdapterNew(dataList)
        binding.rvData.adapter = adapter
    }

    private fun processParticipantData(participants: ArrayList<ParticipantsModel>) {
        Log.d(TAG, "processParticipantData: ")
        dataList.clear()
        dataList.addAll(participants)
        adapter.notifyDataSetChanged()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        return super.onCreateDialog(savedInstanceState)
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sharedViewModel.participantFragVisible=false
    }

}