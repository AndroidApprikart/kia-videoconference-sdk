package com.app.vc.soundDevice

import android.app.Dialog
import android.content.Context
import android.os.Bundle
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
import com.app.vc.databinding.FragmentSoundDeviceBinding
import io.antmedia.webrtcandroidframework.apprtc.AppRTCAudioManager

/* created by Naghma 27/09/23*/

class SoundDeviceFragment : BottomSheetDialogFragment() {

    val TAG= "SoundDeviceFragment::"
    private val viewModel: SoundDeviceViewModel by viewModels()
    private val sharedViewModel: MainViewModel by activityViewModels()


    private lateinit var binding: FragmentSoundDeviceBinding


    private lateinit var mContext: Context

    private var mAudioDevices: Set<AppRTCAudioManager.AudioDevice>? = null

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
            DataBindingUtil.inflate(inflater, R.layout.fragment_sound_device, container, false)
        binding.soundDeviceVM = viewModel
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        setUpClickListeners()
        viewModelObservers()
        setAudioDevices()
    }

    private fun init() {


    }

    private fun setUpClickListeners(){
        binding.cancelSoundDeviceLayout.setOnClickListener {
            dismiss()
        }
        binding.speakerPhoneLayout.setOnClickListener {
            viewModel.changeToSpeakerPhone()
        }
        binding.earpieceLayout.setOnClickListener {
            viewModel.changeToEarPiece()
        }
        binding.wiredHeadsetLayout.setOnClickListener {
            viewModel.changeToWiredHeadset()
        }
        binding.bluetoothLayout.setOnClickListener {
            viewModel.changeToBluetooth()
        }
    }

    private fun viewModelObservers(){
        viewModel.selectedAudioDevice.observe(viewLifecycleOwner) {
            if (it!=null){
                sharedViewModel.newSelectedAudioDevice.value = it
                dismiss()
            }
        }
        sharedViewModel.isAudioDeviceUpdated.observe(viewLifecycleOwner){
            it?.let {
                if(it ==true)
                {
                    setAudioDevices()
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


    private fun setAudioDevices() {
        /**naghma update -> ignoring EARPIECE audio device*/
        binding.speakerPhoneSelectedOption.visibility = View.GONE
        binding.wiredHeadsetSelectedOption.visibility = View.GONE
//                    binding.earpieceSelectedOption.visibility = View.GONE
        binding.bluetoothSelectedOption.visibility = View.GONE

        val currentSelectAudioDevice = sharedViewModel.currentSelectedAudioDevice.value
        if (currentSelectAudioDevice!=null) {
            when (currentSelectAudioDevice) {
                AppRTCAudioManager.AudioDevice.SPEAKER_PHONE -> {
                    binding.speakerPhoneSelectedOption.visibility = View.VISIBLE
                    binding.wiredHeadsetSelectedOption.visibility = View.GONE
//                    binding.earpieceSelectedOption.visibility = View.GONE
                    binding.bluetoothSelectedOption.visibility = View.GONE
                }
                AppRTCAudioManager.AudioDevice.WIRED_HEADSET -> {
                    binding.speakerPhoneSelectedOption.visibility = View.GONE
                    binding.wiredHeadsetSelectedOption.visibility = View.VISIBLE
//                    binding.earpieceSelectedOption.visibility = View.GONE
                    binding.bluetoothSelectedOption.visibility = View.GONE
                }
//                AppRTCAudioManager.AudioDevice.EARPIECE -> {
//                    binding.speakerPhoneSelectedOption.visibility = View.GONE
//                    binding.wiredHeadsetSelectedOption.visibility = View.GONE
//                    binding.earpieceSelectedOption.visibility = View.VISIBLE
//                    binding.bluetoothSelectedOption.visibility = View.GONE
//                }
                AppRTCAudioManager.AudioDevice.BLUETOOTH -> {
                    binding.speakerPhoneSelectedOption.visibility = View.GONE
                    binding.wiredHeadsetSelectedOption.visibility = View.GONE
//                    binding.earpieceSelectedOption.visibility = View.GONE
                    binding.bluetoothSelectedOption.visibility = View.VISIBLE
                }
                else -> {
                    binding.speakerPhoneSelectedOption.visibility = View.GONE
                    binding.wiredHeadsetSelectedOption.visibility = View.GONE
//                    binding.earpieceSelectedOption.visibility = View.GONE
                    binding.bluetoothSelectedOption.visibility = View.GONE
                }
            }
        }
        mAudioDevices = sharedViewModel.audioDevices.value
        if (mAudioDevices==null || mAudioDevices!!.contains(AppRTCAudioManager.AudioDevice.NONE)){
            binding.speakerPhoneLayout.visibility = View.GONE
//            binding.earpieceLayout.visibility = View.GONE
            binding.wiredHeadsetLayout.visibility = View.GONE
            binding.bluetoothLayout.visibility = View.GONE
        } else {
            if (mAudioDevices!!.contains(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE)){
                binding.speakerPhoneLayout.visibility = View.VISIBLE
            }else{
                binding.speakerPhoneLayout.visibility = View.GONE
            }
            if (mAudioDevices!!.contains(AppRTCAudioManager.AudioDevice.WIRED_HEADSET)){
                binding.wiredHeadsetLayout.visibility = View.VISIBLE
                // modified_21Jun2023_nbg
                //modified in such a way that if the wired earphones are connected, listening to audio through earphones is the only option
                binding.speakerPhoneLayout.visibility = View.GONE
            }else{
                binding.wiredHeadsetLayout.visibility = View.GONE
            }
//            if (mAudioDevices!!.contains(AppRTCAudioManager.AudioDevice.EARPIECE)){
//                binding.earpieceLayout.visibility = View.VISIBLE
//            }else{
//                binding.earpieceLayout.visibility = View.GONE
//            }
            if (mAudioDevices!!.contains(AppRTCAudioManager.AudioDevice.BLUETOOTH)){
                binding.bluetoothLayout.visibility = View.VISIBLE
            }else{
                binding.bluetoothLayout.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sharedViewModel.soundDeviceFragVisible = false
    }

}