package com.app.vc.message

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/* created by Naghma 27/09/23*/

@HiltViewModel
class MessageViewModel @Inject constructor():ViewModel() {

    var userMessageInput = MutableLiveData<String>()

    fun validateUserInputMessage():Boolean{
        if(userMessageInput.value!=null)
        {
            if(userMessageInput.value!!.isNotBlank())
            {
                return true
            }
        }
        return false
    }
}