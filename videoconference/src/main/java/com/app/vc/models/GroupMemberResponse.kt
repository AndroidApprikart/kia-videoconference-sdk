package com.app.vc.models

import com.google.gson.annotations.SerializedName

data class GroupMemberResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("chat_role") val chatRole: String,
    @SerializedName("participant_role") val participantRole: String?,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("joined_at") val joinedAt: String
)
