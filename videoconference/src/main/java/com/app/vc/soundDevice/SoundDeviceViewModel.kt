package com.app.vc.soundDevice

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.antmedia.webrtcandroidframework.apprtc.AppRTCAudioManager

/* created by Naghma 27/09/23*/

class SoundDeviceViewModel :ViewModel(){
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