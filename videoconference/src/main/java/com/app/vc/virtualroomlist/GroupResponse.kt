package com.app.vc.virtualroomlist

import com.google.gson.annotations.SerializedName

data class GroupResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("description") val description: String,
    @SerializedName("unread_count") val unreadCount: Int? = 0,
    @SerializedName("vehicle_number") val vehicleNumber: String? = null,
    @SerializedName("ro_number") val roNumber: String? = null,
    @SerializedName(value = "appointment_id", alternate = ["appointment_no", "appointment_number"])
    val appointmentId: String? = null,
    @SerializedName("appointment_date") val appointmentDate: String? = null,
    @SerializedName("service_type") val serviceType: String? = null,
    @SerializedName("current_service_status") val currentServiceStatus: GroupCurrentServiceStatus? = null,
    @SerializedName("member_count") val memberCount: Int,
    @SerializedName("members") val members: List<GroupMember>,
    @SerializedName("created_at") val createdAt: String
)

data class GroupCurrentServiceStatus(
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

data class GroupMember(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int? = null,
    @SerializedName("user") val user: GroupUser? = null,
    @SerializedName("chat_role") val chatRole: String? = null,
    @SerializedName("participant_role") val participantRole: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("joined_at") val joinedAt: String
)

data class GroupUser(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String
)