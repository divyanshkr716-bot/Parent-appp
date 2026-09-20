package com.example.data.model

data class Pairing(
    val id: String,
    val parentId: String,
    val pairingCode: String,
    val childDeviceId: String? = null,
    val childName: String? = null,
    val status: String = "pending", // "waiting", "paired", "revoked"
    val createdAt: String = "",
    val pairedAt: String? = null
) {
    val isWaiting: Boolean get() = status.equals("waiting", ignoreCase = true)
    val isPaired: Boolean get() = status.equals("paired", ignoreCase = true)
    val isRevoked: Boolean get() = status.equals("revoked", ignoreCase = true)
}
