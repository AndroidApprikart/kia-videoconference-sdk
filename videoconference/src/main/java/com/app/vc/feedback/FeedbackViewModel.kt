package com.kia.vc.feedback

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.app.vc.PreferenceManager
import com.app.vc.network.ApiDetails
import com.app.vc.network.ApiInterface
import com.google.gson.GsonBuilder


import com.kia.vc.models.CreateSurvey

import com.kia.vc.models.ResponseModelGetSurveyQuestionList
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val TAG = "FeedbackViewModel: "
class FeedbackViewModel:ViewModel() {

    var toastString = MutableLiveData<String>()

    var customerCode: String? = null
    var dealerCode:String? = null
    var roNo:String? = null
    var meetingCode: String? = null
    var userName:String? = null

    var isProgressBarVisible = MutableLiveData<Boolean>()

    val mSurveyQuestionListResponse = MutableLiveData<ResponseModelGetSurveyQuestionList>()
    var postSurveyListResponse = MutableLiveData<CreateSurvey>()
    var isFailureMessageVisible = MutableLiveData<Boolean>()
    var isRatingListVisible = MutableLiveData<Boolean>(false)


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
//            .client(okhttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiInterface::class.java)


    fun getSurveyQuestionListNew(){
        Log.d(TAG, "getSurveyQuestionListNew: baseUrl: ${PreferenceManager.getBaseUrl()}")
        val call = getServiceObject(PreferenceManager.getBaseUrl()!!).getVcServeyQuestionsNew()
        call.enqueue(object : Callback<ResponseModelGetSurveyQuestionList> {
            override fun onFailure(call: Call<ResponseModelGetSurveyQuestionList>, t: Throwable) {
                Log.d(TAG, "getSurveyQuestionList: onFailure: ")
                isProgressBarVisible.value = false
                isFailureMessageVisible.value = true
                toastString.value = "Something went wrong.Failure.SurveyQuestions"
                Log.d(TAG, "onFailure: getSurveyQuestionList message ${t.message}")
                Log.d(TAG, "onFailure: getSurveyQuestionList cause ${t.cause}")
                Log.d(TAG, "onFailure: getSurveyQuestionList localizedMessage ${t.localizedMessage}")

            }

            override fun onResponse(
                call: Call<ResponseModelGetSurveyQuestionList>,
                response: Response<ResponseModelGetSurveyQuestionList>
            ) {
                Log.d(TAG, "getSurveyQuestionList: onResponse: ")

                if(response.code() in 200 .. 299) {
                    Log.d(TAG, "getSurveyQuestionList: onResponse:  ${response.body()}")
                    mSurveyQuestionListResponse.value=response.body()
                }else {
                    isFailureMessageVisible.value = true
                    isProgressBarVisible.value = false
                    toastString.value  = "Something went wrong.responseCode.SurveyQuestions"
                }


            }

        })
    }



    fun postSurveyDetailsNew(requestObject: com.kia.vc.feedback.RequestModelCreateSurvey){
        Log.d(TAG, "postSurveyDetailsNew: baseUrl : ${PreferenceManager.getBaseUrl()}")
        val call = getServiceObject(PreferenceManager.getBaseUrl()!!).postCreateSurveyNew(requestObject)
        call.enqueue(object : Callback<CreateSurvey>{
            override fun onFailure(call: Call<CreateSurvey>, t: Throwable) {
                toastString.value = "Something went wrong.Failure.PostSurvey."
                Log.d(TAG, "onFailure: postServeyList message ${t.message}")
                Log.d(TAG, "onFailure: postServeyList cause ${t.cause}")
                Log.d(TAG, "onFailure: postServeyList localizedMessage ${t.localizedMessage}")

            }

            override fun onResponse(
                call: Call<CreateSurvey>,
                response: Response<CreateSurvey>
            ) {
                if(response.code() in 200 .. 299) {
                    postSurveyListResponse.value = response.body()
                }else {
                    toastString.value = "Something went wrong.responseCode.PostSurvey"
                }
                Log.d(TAG, "onResponse: postServeyList: ${response.body()}")

            }

        })
    }



    fun getServiceObject(baseUrl: String):ApiInterface{
        var service: ApiInterface =
            Retrofit.Builder()
                .baseUrl(baseUrl)
//            .client(okhttp)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ApiInterface::class.java)

        return service
    }



}