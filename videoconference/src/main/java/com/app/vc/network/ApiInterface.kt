package com.app.vc.network

import com.app.vc.models.DisplayNameResponse
import com.app.vc.models.RequestModelUpdateVcStatusCustomer
import com.app.vc.models.ResponseModelUpdateVideoStatus
import com.app.vc.models.UpdateStreamIdResponse
import com.app.vc.models.UploadVcFileResponse
import com.app.vc.models.ValidateVcResponse
import com.app.vc.models.VcConfigurationResponse
import com.app.vc.models.login.RequestModelLogin
import com.app.vc.models.login.ResponseModelLogin
import com.kia.vc.models.CreateSurvey
import com.kia.vc.models.ResponseModelGetSurveyQuestionList
import com.kia.vc.validateDealer.ResponseModelValidateDealerCode
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

/* created by Naghma 06/10/23*/


interface ApiInterface {

    @Multipart
    @POST(ApiDetails.UPLOAD_VC_FILE)
    fun uploadVcFile(
        @Part file: MultipartBody.Part,
        @Part("vc_room") vc_room: String,
        @Part("user_type") user_type: String,
        @Part("who") who: String,
        @Query("app_version") appVersion: String
    ): Call<UploadVcFileResponse>

    @GET(ApiDetails.VALIDATE_VC)
     fun validateVcForServicePerson(
        @Query("room") room: String,
        @Query("auth_passcode") authPasscode: String,
        @Query("user_type") userType: String,
        @Query("service_person_id") servicePersonId: String,
        @Query("app_version") appVersion: String
    ): Call<ValidateVcResponse>

    @GET(ApiDetails.VALIDATE_VC)
    fun validateVcForCustomer(
        @Query("room") room: String,
        @Query("auth_passcode") authPasscode: String,
        @Query("user_type") userType: String,
        @Query("app_version") appVersion: String
    ): Call<ValidateVcResponse>

    @GET(ApiDetails.GET_VC_CONFIGURATION)
    fun getVcConfiguration(
        @Query("room") room: String,
        @Query("user_type") userType: String,
        @Query("device_type") who: String,
        @Query("app_version") appVersion: String
    ): Call<VcConfigurationResponse>

    @GET(ApiDetails.UPDATE_STREAM_ID)
    fun updateStreamIdInServer(
        @Query("display_name") displayName: String,
        @Query("stream_id") streamId: String,
        @Query("room") roomId: String,
        @Query("user_type") userType: String,
        @Query("app_version") appVersion: String
    ): Call<UpdateStreamIdResponse>


    @GET(ApiDetails.GET_DISPLAY_NAME)
    fun getDisplayName(
//            @Query("user_type") userType: String,
        @Query("room") room: String,
        @Query("stream_id") streamID: String,
        @Query("app_version") appVersion: String
    ): Call<DisplayNameResponse>

    @POST(ApiDetails.LOGIN)
    fun login(
        @Body body: RequestModelLogin
    ):Call<ResponseModelLogin>

    @POST(ApiDetails.UPDATE_VC_STATUS_CUSTOMER_NEW)
    fun updateVcStatusForCustomerNew(
        @Body body: RequestModelUpdateVcStatusCustomer
    ):Call<ResponseModelUpdateVideoStatus>
    @GET(ApiDetails.GET_SURVEY_QUESTIONS_NEW)
    fun getVcServeyQuestionsNew(

    ): Call<ResponseModelGetSurveyQuestionList>

    @POST(ApiDetails.POST_SURVEY_QUESTIONS_NEW)
    fun postCreateSurveyNew(@Body body:com.kia.vc.feedback.RequestModelCreateSurvey): Call<CreateSurvey>
    @GET(ApiDetails.VALIDATE_DEALER_CODE)
    fun validateDealerCode(
        @Query("dealer_code") dealerCode: String,
        @Query("app_version") appVersion: String
    ):Call<ResponseModelValidateDealerCode>

}