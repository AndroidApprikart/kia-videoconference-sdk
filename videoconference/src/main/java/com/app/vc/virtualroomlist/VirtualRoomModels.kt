package com.app.vc.virtualroomlist

enum class UserRole {
    CUSTOMER,
    SERVICE_ADVISOR,
    MANAGER
}

enum class RoomStatus {
    OPEN,
    IN_PROGRESS,
    CLOSED,
    REOPENED,
    CANCELLED
}

data class VirtualRoomDto(
    val roNumber: String,
    val subject: String,
    val status: String,
    val dayLabel: String,
    val timeLabel: String,
    val unreadCount: Int,
    val customerName: String,
    val contactNumber: String
)

data class VirtualRoomUiModel(
    val roNumber: String,
    val subject: String,
    val status: RoomStatus,
    val dayLabel: String,
    val timeLabel: String,
    val unreadCount: Int,
    val customerName: String,
    val contactNumber: String
)

internal fun VirtualRoomDto.toUiModel(): VirtualRoomUiModel {
    val safeStatus = try {
        RoomStatus.valueOf(status)
    } catch (_: IllegalArgumentException) {
        RoomStatus.OPEN
    }
    return VirtualRoomUiModel(
        roNumber = roNumber,
        subject = subject,
        status = safeStatus,
        dayLabel = dayLabel,
        timeLabel = timeLabel,
        unreadCount = unreadCount,
        customerName = customerName,
        contactNumber = contactNumber
    )
}

