package com.app.vc.participants

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.vc.models.GroupMemberResponse
import com.app.vc.utils.ApiDetails
import com.app.vc.utils.ApiInterface
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ParticipantsViewModel : ViewModel() {

    private val _members = MutableLiveData<List<GroupMemberResponse>>()
    val members: LiveData<List<GroupMemberResponse>> get() = _members

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private fun getApiInterface(): ApiInterface {
        val interceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        return Retrofit.Builder()
            .baseUrl(ApiDetails.APRIK_Kia_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            .build()
            .create(ApiInterface::class.java)
    }

    fun fetchParticipants(token: String, groupSlug: String?) {
        if (groupSlug.isNullOrBlank()) {
            _members.value = emptyList()
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = getApiInterface().getGroupMembers("Bearer $token", groupSlug)
                if (response.isSuccessful) {
                    val body = response.body()
                    _members.postValue(body ?: emptyList())
                } else {
                    _error.postValue("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Unknown Error")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
