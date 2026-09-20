package com.example.data.model

data class DeviceEvent(
    val id: String,
    val deviceId: String,
    val eventType: String,
    val data: Map<String, Any?> = emptyMap(),
    val createdAt: String = ""
) {
    companion object {
        const val EVENT_ONLINE = "ONLINE"
        const val EVENT_OFFLINE = "OFFLINE"
        const val EVENT_PAIRING_COMPLETED = "PAIRING_COMPLETED"
        const val EVENT_PERMISSION_CHANGED = "PERMISSION_CHANGED"
        const val EVENT_SCREEN_SESSION_START = "SCREEN_SESSION_START"
        const val EVENT_SCREEN_SESSION_STOP = "SCREEN_SESSION_STOP"
        const val EVENT_CAMERA_SESSION_START = "CAMERA_SESSION_START"
        const val EVENT_CAMERA_SESSION_STOP = "CAMERA_SESSION_STOP"
        const val EVENT_AUDIO_SESSION_START = "AUDIO_SESSION_START"
        const val EVENT_AUDIO_SESSION_STOP = "AUDIO_SESSION_STOP"
        const val EVENT_FILE_UPLOAD_COMPLETED = "FILE_UPLOAD_COMPLETED"
        const val EVENT_FILE_OP_RESULT = "FILE_OP_RESULT"
        const val EVENT_CALL_EVENT = "CALL_EVENT"
        const val EVENT_BATTERY_UPDATE = "BATTERY_UPDATE"
        const val EVENT_LOCATION_UPDATE = "LOCATION_UPDATE"
    }
}
