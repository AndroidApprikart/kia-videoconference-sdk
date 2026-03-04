package com.app.vc.virtualroomlist

import com.google.gson.annotations.SerializedName

data class GroupResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("description") val description: String,
    @SerializedName("member_count") val memberCount: Int,
    @SerializedName("members") val members: List<GroupMember>,
    @SerializedName("created_at") val createdAt: String
)

data class GroupMember(
    @SerializedName("id") val id: Int,
    @SerializedName("user") val user: GroupUser,
    @SerializedName("role") val role: String,
    @SerializedName("joined_at") val joinedAt: String
)

data class GroupUser(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String
)