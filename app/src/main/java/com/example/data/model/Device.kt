package com.example.data.model

data class Device(
    val id: String,
    val childName: String = "Child Device",
    val model: String = "Android Device",
    val osVersion: String = "Android",
    val batteryLevel: Int = -1,
    val isCharging: Boolean = false,
    val status: String = "offline", // "online", "offline", "connecting"
    val lastSeen: String = "",
    val ipAddress: String = "",
    val permissions: Map<String, Boolean> = emptyMap(),
    val pairingId: String = ""
) {
    val isOnline: Boolean get() = status.equals("online", ignoreCase = true)
    val isConnecting: Boolean get() = status.equals("connecting", ignoreCase = true)
}
