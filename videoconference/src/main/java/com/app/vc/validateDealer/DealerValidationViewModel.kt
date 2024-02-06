package com.kia.vc.validateDealer

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.app.vc.utils.VCConstants
import com.app.vc.utils.ApiDetails
import com.app.vc.utils.ApiInterface
import com.google.gson.GsonBuilder

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
private const val TAG = "DealerValidationViewModel:"
class DealerValidationViewModel:ViewModel() {
    var toastString = MutableLiveData<String>()
    var isProgressBarVisible = MutableLiveData<Boolean>(false)
    var isContinueButtonClickable = MutableLiveData<Boolean>(true)

    var roomId:String? = null
    var serviceAdvisorID:String? = null
    var userType:String? = null
    var passcode:String? = null
    var customerCode:String? = null
    var dealerCode:String? = null
    var roNo:String? = null
    var displayName:String? = null
    var userName: String? = null
    var vcEndTime:String? = null
    var dealerName:String? = null

    var callType:String? = null
    var customerName:String? = null
    var customerPhoneNumber:String? = null


    var validateDealerCodeResponse = MutableLiveData<ResponseModelValidateDealerCode>()

    var gson = GsonBuilder()
        .setLenient()
        .create()

    val okhttp = OkHttpClient().newBuilder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()

    private var service: ApiInterface =
        Retrofit.Builder()
            .baseUrl(ApiDetails.BASE_URL)
            .client(okhttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiInterface::class.java)


    fun validateDealerCode() {
        val call = service.validateDealerCode(dealerCode!!, VCConstants.version)
        call.enqueue(object : Callback<ResponseModelValidateDealerCode?> {
            override fun onResponse(
                call: Call<ResponseModelValidateDealerCode?>,
                response: Response<ResponseModelValidateDealerCode?>
            ) {
                if(response.code() in 200 .. 299) {
                    validateDealerCodeResponse.value = response.body()
                }else {
                    isProgressBarVisible.value = false
                    isContinueButtonClickable.value = true
                    toastString.value = "Something went wrong.responseCode.DealerCodeValidation"
                }
            }

            override fun onFailure(call: Call<ResponseModelValidateDealerCode?>, t: Throwable) {
                Log.d(TAG, "onFailure: validateDealerCode: ")
                toastString.value = "Something went wrong.Failure.DealerCodeValidation."
                isProgressBarVisible.value = false
                isContinueButtonClickable.value = true
            }
        })
    }
}