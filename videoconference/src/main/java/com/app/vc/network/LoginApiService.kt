package com.app.vc.network

import com.app.vc.virtualchatroom.FileUploadResponse
import com.app.vc.virtualchattoken.LoginResponse
import com.app.vc.virtualroomlist.GroupResponse
import com.google.gson.annotations.SerializedName
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
import retrofit2.http.Query

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

    @GET("api/quick-replies/")
    suspend fun getQuickReplies(
        @Query("role") role: String
    ): Response<List<QuickReplyResponse>>

    @GET("api/groups/{slug}/messages/")
    suspend fun getMessages(
        @Header("Authorization") token: String,
        @Path("slug") slug: String
    ): Response<List<ApiMessageResponse>>
}

data class TokenVerifyRequest(val token: String)
data class TokenRefreshRequest(val refresh: String)

data class QuickReplyResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("text") val text: String,
    @SerializedName("role") val role: String,
    @SerializedName("display_order") val displayOrder: Int
)

data class ApiMessageResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("content") val content: String,
    @SerializedName("message_type") val messageType: String,
    @SerializedName("sender") val sender: ApiSenderResponse,
    @SerializedName("attachments") val attachments: List<ApiAttachmentResponse>,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("receipts") val receipts: List<ApiReceiptResponse>
)

data class ApiSenderResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String
)

data class ApiAttachmentResponse(
    @SerializedName("file_url") val fileUrl: String,
    @SerializedName("file_name") val fileName: String,
    @SerializedName("mime_type") val mimeType: String
)

data class ApiReceiptResponse(
    @SerializedName("user") val user: ApiSenderResponse
)
