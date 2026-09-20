package com.example.data.model

data class WebRtcSignal(
    val id: String,
    val sessionId: String,
    val senderId: String,
    val receiverId: String,
    val type: String, // "offer", "answer", "ice_candidate", "bye"
    val sdp: String? = null,
    val candidate: String? = null,
    val data: String? = null,
    val createdAt: String = ""
)
