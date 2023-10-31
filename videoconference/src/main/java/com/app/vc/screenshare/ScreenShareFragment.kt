package com.app.vc.screenshare

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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.app.vc.MainViewModel
import com.app.vc.R
import com.app.vc.databinding.FragmentScreenShareBinding
import dagger.hilt.android.AndroidEntryPoint

/* created by Naghma 27/09/23*/


@AndroidEntryPoint
class ScreenShareFragment : BottomSheetDialogFragment() {

    val TAG = "ScrnShareFragment::"
    private val viewModel: ScreenShareViewModel by viewModels()
    private val sharedViewModel: MainViewModel by activityViewModels()


    private lateinit var binding: FragmentScreenShareBinding


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
            DataBindingUtil.inflate(inflater, R.layout.fragment_screen_share, container, false)
        binding.screenShareVM = viewModel
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        setUpClickListeners()

        viewModelObservers()
    }

    private fun init() {
        if (sharedViewModel.updateScreenShareForFragment.value == true) {
            binding.stopScreenShareMainLayout.visibility = View.VISIBLE
            binding.screenShareMainLayout.visibility = View.GONE
            binding.btnStartScreenShare.visibility = View.GONE
        } else {
            binding.stopScreenShareMainLayout.visibility = View.GONE
            binding.screenShareMainLayout.visibility = View.VISIBLE
            binding.btnStartScreenShare.visibility = View.VISIBLE
        }

    }

    private fun setUpClickListeners() {
        binding.btnCancel.setOnClickListener {
            viewModel.cancel()
        }
        binding.btnStartScreenShare.setOnClickListener {
            viewModel.dostartScreenShare()
        }
        binding.btnStopScreenShare.setOnClickListener {
            viewModel.doStopScreenShare()
        }
    }

    private fun viewModelObservers() {
        viewModel.startScreenShare.observe(viewLifecycleOwner) {
            it?.let {
                if (it) {
                    Log.d(TAG, "viewModelObservers: startScreen share")
                    sharedViewModel.startScreenShare.value = true
                    dismiss()
                    viewModel.startScreenShare.value = false
                }
            }
        }

        viewModel.cancel.observe(viewLifecycleOwner) {
            it?.let {
                Log.d(TAG, "viewModelObservers: cancel")
                if (it) dismiss()
            }
        }

        viewModel.stopScreenShare.observe(viewLifecycleOwner) {
            it?.let {
                if (it) {
                    Log.d(TAG, "viewModelObservers: stop screen share")
                    sharedViewModel.stopScreenShare.value = true
                    dismiss()
                    viewModel.stopScreenShare.value = false
                }
            }
        }
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        //        return super.onCreateDialog(savedInstanceState)
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sharedViewModel.screenShareFragVisible = false
    }
}