package com.app.vc

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.app.vc.models.UploadVcFileResponse
import com.app.vc.network.ApiDetails
import com.app.vc.network.ApiInterface
import com.app.vc.network.RetrofitClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Part
import retrofit2.http.Query

class DataRepository (
){

    private val retrofit =
        RetrofitClient().getRetrofitClient()
    private val api: ApiInterface = retrofit.create(ApiInterface::class.java)

    val TAG = "DataRepository"

    suspend fun doUploadVCAPICall(
        file: MultipartBody.Part,
        vc_room: String,
        user_type: String,
         who: String,
      appVersion: String
    ): kotlinx.coroutines.flow.Flow<UploadVcFileResponse> {
        Log.d(TAG, "doUploadVCAPICall: ")
        return flow {
            emit(api.uploadVcFile(file, vc_room,user_type,who,appVersion))
        }
    }
}
