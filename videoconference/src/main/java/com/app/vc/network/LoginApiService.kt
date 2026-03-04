package com.app.vc.network

import com.app.vc.virtualchatroom.FileUploadResponse
import com.app.vc.virtualchattoken.LoginResponse
import com.app.vc.virtualroomlist.GroupResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface LoginApiService {
    @FormUrlEncoded
    @POST("api/token/")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<LoginResponse>

    @POST("api/token/verify/")
    suspend fun verifyToken(
        @Body request: TokenVerifyRequest
    ): Response<Unit>

    @POST("api/token/refresh/")
    suspend fun refreshToken(
        @Body request: TokenRefreshRequest
    ): Response<LoginResponse>

    @GET("api/groups/")
    suspend fun getGroups(
        @Header("Authorization") token: String
    ): Response<List<GroupResponse>>

    @Multipart
    @POST("api/groups/{slug}/upload/")
    suspend fun uploadFile(
        @Header("Authorization") token: String,
        @Path("slug") slug: String,
        @Part file: MultipartBody.Part,
        @Part("caption") caption: RequestBody?
    ): Response<FileUploadResponse>
}

data class TokenVerifyRequest(val token: String)
data class TokenRefreshRequest(val refresh: String)