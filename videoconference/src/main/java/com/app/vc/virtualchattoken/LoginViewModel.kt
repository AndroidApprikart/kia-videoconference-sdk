package com.app.vc.virtualchattoken

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.vc.network.LoginApiService
import com.app.vc.network.TokenRefreshRequest
import com.app.vc.network.TokenVerifyRequest
import com.app.vc.utils.ApiDetails
import com.app.vc.utils.ApiInterface
import com.app.vc.utils.JwtUtils
import com.app.vc.utils.PreferenceManager
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginViewModel : ViewModel() {

    private val _loginResponse = MutableLiveData<LoginResponse?>()
    val loginResponse: LiveData<LoginResponse?> = _loginResponse



    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isVerified = MutableLiveData<Boolean>()
    val isVerified: LiveData<Boolean> = _isVerified

    private val _sessionExpired = MutableLiveData<Boolean>()
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    private val loginApiService: LoginApiService by lazy {
        val gson = GsonBuilder().setLenient().create()
        Retrofit.Builder()
            .baseUrl(ApiDetails.APRIK_Kia_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(LoginApiService::class.java)
    }



    fun login(username: String, unique_id: String, role: String,dealer_code: String) {
        if (username.isEmpty() || unique_id.isEmpty() || role.isEmpty() || dealer_code.isEmpty()) {
            _errorMessage.value = "Please enter username . role dealer code and uniqueId "
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = loginApiService.login(username, unique_id, role = role, dealer_code = dealer_code)
                if (response.isSuccessful && response.body() != null) {
                    val loginData = response.body()!!

                    Log.d("LoginViewModel", "Access Token Received: ${loginData.access}")
                    Log.d("LoginViewModel", "Refresh Token Received: ${loginData.refresh}")

                    // Decode JWT and save user ID
                    if (loginData.access != null) {
                        val userId = JwtUtils.getUserIdFromToken(loginData.access)
                        Log.d("LoginViewModel", "User ID from token: $userId")
                        PreferenceManager.setUserId(userId)
                    }

                    PreferenceManager.setAccessToken(loginData.access)
                    PreferenceManager.setRefreshToken(loginData.refresh)

                    verifyToken(loginData.access!!)
                } else {
                    _errorMessage.value = "Login failed: ${response.message()}"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.localizedMessage}"
                _isLoading.value = false
            }
        }
    }



    private suspend fun verifyToken(token: String) {
        try {
            val response = loginApiService.verifyToken(TokenVerifyRequest(token))
            if (response.isSuccessful) {
                _isVerified.value = true
                _isLoading.value = false
            } else {
                refreshAuthToken()
            }
        } catch (e: Exception) {
            refreshAuthToken()
        }
    }

    private suspend fun refreshAuthToken() {
        val refreshToken = PreferenceManager.getRefreshToken()
        if (refreshToken.isNullOrEmpty()) {
            _sessionExpired.postValue(true)
            _isLoading.value = false
            return
        }

        try {
            val response = loginApiService.refreshToken(TokenRefreshRequest(refreshToken))
            if (response.isSuccessful && response.body() != null) {
                val newData = response.body()!!
                Log.d("LoginViewModel", "New Access Token from Refresh: ${newData.access}")
                PreferenceManager.setAccessToken(newData.access)
                if (!newData.refresh.isNullOrEmpty()) {
                    PreferenceManager.setRefreshToken(newData.refresh)
                }
                _isVerified.value = true
            } else {
                _sessionExpired.postValue(true)
            }
        } catch (e: Exception) {
            _sessionExpired.postValue(true)
        } finally {
            _isLoading.value = false
        }
    }
}