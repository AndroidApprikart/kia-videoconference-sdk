package com.app.vc.network

import com.app.vc.virtualchattoken.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

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
}

data class TokenVerifyRequest(val token: String)
data class TokenRefreshRequest(val refresh: String)