package com.app.vc.message

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
//import javax.inject.Inject

/* created by Naghma 27/09/23*/

class MessageViewModel:ViewModel() {

    var userMessageInput = MutableLiveData<String>()
    var saveMessageList = MutableLiveData<Boolean>(false)
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

    fun saveMessageList() {
        saveMessageList.value = true
    }

}