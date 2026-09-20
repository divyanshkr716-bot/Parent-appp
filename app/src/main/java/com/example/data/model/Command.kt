package com.example.data.model

data class Command(
    val id: String,
    val parentId: String,
    val childId: String,
    val type: String,
    val payload: Map<String, Any?> = emptyMap(),
    val status: String = "pending", // "pending", "received", "running", "completed", "failed"
    val result: String? = null,
    val createdAt: String = "",
    val completedAt: String? = null
) {
    val isPending: Boolean get() = status.equals("pending", ignoreCase = true)
    val isReceived: Boolean get() = status.equals("received", ignoreCase = true)
    val isRunning: Boolean get() = status.equals("running", ignoreCase = true)
    val isCompleted: Boolean get() = status.equals("completed", ignoreCase = true)
    val isFailed: Boolean get() = status.equals("failed", ignoreCase = true)

    companion object {
        const val TYPE_REQUEST_STATUS = "REQUEST_STATUS"
        const val TYPE_REQUEST_LOCATION = "REQUEST_LOCATION"
        const val TYPE_START_SCREEN_SHARE = "START_SCREEN_SHARE"
        const val TYPE_STOP_SCREEN_SHARE = "STOP_SCREEN_SHARE"
        const val TYPE_START_CAMERA = "START_CAMERA"
        const val TYPE_STOP_CAMERA = "STOP_CAMERA"
        const val TYPE_START_AUDIO = "START_AUDIO"
        const val TYPE_STOP_AUDIO = "STOP_AUDIO"
        const val TYPE_BROWSE_FILES = "BROWSE_FILES"
        const val TYPE_REQUEST_NOTIFICATIONS = "REQUEST_NOTIFICATIONS"
        const val TYPE_REFRESH_DEVICE = "REFRESH_DEVICE"
        const val TYPE_PING = "PING"
    }
}
