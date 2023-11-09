package com.app.vc.network

import com.app.vc.models.UploadVcFileResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

/* created by Naghma 06/10/23*/


interface ApiInterface {

    @Multipart
    @POST(ApiDetails.UPLOAD_VC_FILE)
    suspend fun uploadVcFile(
        @Part file: MultipartBody.Part,
        @Part("vc_room") vc_room: String,
        @Part("user_type") user_type: String,
        @Part("who") who: String,
        @Query("app_version") appVersion: String
    ): UploadVcFileResponse
}