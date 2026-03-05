package com.app.vc.models

import com.google.gson.annotations.SerializedName

data class GroupMemberResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("user") val user: User,
    @SerializedName("role") val role: String,
    @SerializedName("joined_at") val joinedAt: String
)

data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String
)
