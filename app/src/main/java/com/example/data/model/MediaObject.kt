package com.example.data.model

data class MediaObject(
    val id: String,
    val deviceId: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val storagePath: String? = null,
    val telegramMessageId: String? = null,
    val telegramFileId: String? = null,
    val status: String = "ready",
    val createdAt: String = ""
)

data class TelegramFileJob(
    val id: String,
    val deviceId: String,
    val fileName: String,
    val status: String = "uploading", // "uploading", "processing", "uploaded", "failed"
    val telegramMessageId: String? = null,
    val metadata: String? = null,
    val createdAt: String = ""
)

data class RemoteFile(
    val name: String,
    val path: String,
    val size: Long = 0L,
    val isDirectory: Boolean = false,
    val modifiedAt: String = "",
    val mimeType: String = ""
) {
    val formattedSize: String
        get() {
            if (isDirectory) return "--"
            if (size < 1024) return "$size B"
            val kb = size / 1024.0
            if (kb < 1024) return String.format("%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024) return String.format("%.1f MB", mb)
            val gb = mb / 1024.0
            return String.format("%.1f GB", gb)
        }
}

data class CallEvent(
    val type: String, // "INCOMING", "OUTGOING", "MISSED", "REJECTED"
    val number: String,
    val timestamp: String,
    val durationSeconds: Long = 0L
)
