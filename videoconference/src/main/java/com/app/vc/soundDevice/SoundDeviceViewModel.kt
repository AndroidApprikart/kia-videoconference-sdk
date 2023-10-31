package com.app.vc.soundDevice

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.antmedia.webrtcandroidframework.apprtc.AppRTCAudioManager
import javax.inject.Inject

/* created by Naghma 27/09/23*/

@HiltViewModel
class SoundDeviceViewModel @Inject constructor():ViewModel(){
    var selectedAudioDevice = MutableLiveData<AppRTCAudioManager.AudioDevice>()

    fun changeToSpeakerPhone() {
        selectedAudioDevice.value = AppRTCAudioManager.AudioDevice.SPEAKER_PHONE
    }

    fun changeToEarPiece() {
        selectedAudioDevice.value = AppRTCAudioManager.AudioDevice.EARPIECE
    }

    fun changeToWiredHeadset() {
        selectedAudioDevice.value = AppRTCAudioManager.AudioDevice.WIRED_HEADSET
    }

    fun changeToBluetooth() {
        selectedAudioDevice.value = AppRTCAudioManager.AudioDevice.BLUETOOTH
    }
}