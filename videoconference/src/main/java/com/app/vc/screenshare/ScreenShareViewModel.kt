package com.app.vc.screenshare

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/* created by Naghma 27/09/23*/

@HiltViewModel
class ScreenShareViewModel @Inject constructor():ViewModel(){

    var startScreenShare = MutableLiveData<Boolean>()
    var stopScreenShare = MutableLiveData<Boolean>()
    var cancel = MutableLiveData<Boolean>()
    fun dostartScreenShare() {
        startScreenShare.value = true
    }

    fun doStopScreenShare(){
        stopScreenShare.value = true
    }

    fun cancel(){
        cancel.value = true
    }

}