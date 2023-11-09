package com.app.vc.screenshare

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/* created by Naghma 27/09/23*/

class ScreenShareViewModel :ViewModel(){

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