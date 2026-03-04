package com.app.vc.virtualchatroom

import com.google.gson.annotations.SerializedName

data class FileUploadResponse(
    @SerializedName("message_id") val messageId: Int,
    @SerializedName("message_type") val messageType: String,
    @SerializedName("caption") val caption: String?,
    @SerializedName("attachment") val attachment: AttachmentDetails
)

data class AttachmentDetails(
    @SerializedName("id") val id: Int,
    @SerializedName("file_type") val fileType: String,
    @SerializedName("file_name") val fileName: String,
    @SerializedName("file_size") val fileSize: Long,
    @SerializedName("mime_type") val mimeType: String,
    @SerializedName("file_url") val fileUrl: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    @SerializedName("uploaded_at") val uploadedAt: String
)