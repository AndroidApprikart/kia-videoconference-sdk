package com.app.vc.network

import com.app.vc.models.GroupMemberResponse
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
    @POST("api/auth/participant-token/")
    suspend fun login(
        @Field("name") name: String,
        @Field("unique_id") unique_id: String,
        @Field("role") role: String,
        @Field("dealer_code") dealer_code: String,
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

    @GET("api/groups/{slug}/members/")
    suspend fun getGroupMembers(
        @Header("Authorization") token: String,
        @Path("slug") slug: String
    ): Response<List<GroupMemberResponse>>

    @GET("api/groups/{slug}/service-lifecycle/current/")
    suspend fun getServiceLifecycleCurrent(
        @Header("Authorization") token: String,
        @Path("slug") slug: String
    ): Response<ServiceLifecycleCurrentResponse>

    @GET("api/templates/")
    suspend fun getTemplates(
        @Header("Authorization") token: String
    ): Response<List<TemplateResponse>>
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
    @SerializedName("sender") val sender: ApiSenderResponse?,
    @SerializedName("attachments") val attachments: List<ApiAttachmentResponse>?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("receipts") val receipts: List<ApiReceiptResponse>?
)

data class ApiSenderResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String
)

data class ApiAttachmentResponse(
    @SerializedName("file_url") val fileUrl: String,
    @SerializedName("file_name") val fileName: String,
    @SerializedName("mime_type") val mimeType: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null
)

data class ApiReceiptResponse(
    @SerializedName("user") val user: ApiSenderResponse
)

data class ServiceLifecycleCurrentResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("group") val group: Int,
    @SerializedName("status") val status: String,
    @SerializedName("status_label") val statusLabel: String?,
    @SerializedName("previous_status") val previousStatus: String?,
    @SerializedName("previous_status_label") val previousStatusLabel: String?,
    @SerializedName("updated_by") val updatedBy: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("timestamp") val timestamp: String?
)

data class TemplateResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("key") val key: String,
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)
