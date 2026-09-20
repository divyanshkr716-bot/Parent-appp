package com.example.webrtc

import com.example.auth.AuthRepository
import com.example.data.model.WebRtcSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

enum class WebRtcSessionState {
    IDLE,
    CONNECTING,
    CONNECTED,
    PERMISSION_REQUIRED,
    DISCONNECTED,
    ERROR
}

data class WebRtcSessionInfo(
    val sessionId: String = "",
    val childId: String = "",
    val sessionType: String = "", // "screen", "camera", "audio"
    val state: WebRtcSessionState = WebRtcSessionState.IDLE,
    val remoteSdpAnswer: String? = null,
    val iceCandidateCount: Int = 0,
    val statusMessage: String = "Ready",
    val latencyMs: Long = 0L,
    val bytesReceived: Long = 0L,
    val audioLevelDb: Float = 0f
)

class ParentWebRtcManager(
    private val signalingRepository: WebRtcSignalingRepository,
    private val authRepository: AuthRepository,
    private val coroutineScope: CoroutineScope
) {
    private val _session = MutableStateFlow(WebRtcSessionInfo())
    val session: StateFlow<WebRtcSessionInfo> = _session.asStateFlow()

    private var pollJob: Job? = null
    private var telemetryJob: Job? = null
    private val processedSignalIds = mutableSetOf<String>()

    fun startSession(childId: String, sessionType: String) {
        endSession()
        val sessionId = UUID.randomUUID().toString()
        val parentId = authRepository.currentUserId ?: "parent"

        _session.value = WebRtcSessionInfo(
            sessionId = sessionId,
            childId = childId,
            sessionType = sessionType,
            state = WebRtcSessionState.CONNECTING,
            statusMessage = "Creating SDP Offer for $sessionType..."
        )

        coroutineScope.launch(Dispatchers.IO) {
            // 1. Generate SDP Offer
            val sdpOffer = generateLocalSdpOffer(sessionType)

            // 2. Post Offer to public.webrtc_signals
            val offerSignal = WebRtcSignal(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                senderId = parentId,
                receiverId = childId,
                type = "offer",
                sdp = sdpOffer,
                data = JSONObject().apply {
                    put("type", "offer")
                    put("media", sessionType)
                    put("protocol", "webrtc-stream")
                }.toString()
            )

            val postResult = signalingRepository.sendSignal(offerSignal)
            if (postResult.isFailure) {
                _session.value = _session.value.copy(
                    state = WebRtcSessionState.ERROR,
                    statusMessage = "Signaling failed: ${postResult.exceptionOrNull()?.localizedMessage}"
                )
                return@launch
            }

            _session.value = _session.value.copy(
                statusMessage = "SDP Offer sent. Waiting for child consent & answer..."
            )

            // 3. Start listening for Answer and ICE Candidates
            startSignalingListener(sessionId, childId, parentId)
        }
    }

    private fun startSignalingListener(sessionId: String, childId: String, parentId: String) {
        pollJob?.cancel()
        pollJob = coroutineScope.launch(Dispatchers.IO) {
            var answerReceived = false
            var attempts = 0

            while (isActive && _session.value.state != WebRtcSessionState.DISCONNECTED) {
                delay(1500)
                attempts++

                val signalsResult = signalingRepository.getSignals(sessionId)
                signalsResult.onSuccess { signals ->
                    signals.forEach { signal ->
                        if (processedSignalIds.add(signal.id)) {
                            handleIncomingSignal(signal)
                            if (signal.type == "answer") {
                                answerReceived = true
                            }
                        }
                    }
                }

                // If no answer after 30 seconds, inform user
                if (!answerReceived && attempts > 20) {
                    _session.value = _session.value.copy(
                        state = WebRtcSessionState.PERMISSION_REQUIRED,
                        statusMessage = "Waiting for child device authorization / MediaProjection consent..."
                    )
                }
            }
        }
    }

    private fun handleIncomingSignal(signal: WebRtcSignal) {
        when (signal.type.lowercase()) {
            "answer" -> {
                _session.value = _session.value.copy(
                    state = WebRtcSessionState.CONNECTED,
                    remoteSdpAnswer = signal.sdp,
                    statusMessage = "WebRTC Connected. Live stream active."
                )
                startTelemetry()
            }
            "ice_candidate", "candidate" -> {
                _session.value = _session.value.copy(
                    iceCandidateCount = _session.value.iceCandidateCount + 1
                )
            }
            "permission_denied", "denied" -> {
                _session.value = _session.value.copy(
                    state = WebRtcSessionState.PERMISSION_REQUIRED,
                    statusMessage = "Child device denied capture permission or consent required."
                )
            }
            "bye", "close" -> {
                _session.value = _session.value.copy(
                    state = WebRtcSessionState.DISCONNECTED,
                    statusMessage = "Remote stream closed by child."
                )
                stopTelemetry()
            }
        }
    }

    private fun startTelemetry() {
        telemetryJob?.cancel()
        telemetryJob = coroutineScope.launch(Dispatchers.IO) {
            var simulatedBytes = 120_000L
            while (isActive && _session.value.state == WebRtcSessionState.CONNECTED) {
                delay(1000)
                simulatedBytes += (80_000..160_000).random()
                val latency = (25..65).random().toLong()
                val db = (-40..-10).random().toFloat()

                _session.value = _session.value.copy(
                    bytesReceived = simulatedBytes,
                    latencyMs = latency,
                    audioLevelDb = db
                )
            }
        }
    }

    private fun stopTelemetry() {
        telemetryJob?.cancel()
        telemetryJob = null
    }

    fun endSession() {
        val current = _session.value
        if (current.sessionId.isNotBlank()) {
            coroutineScope.launch(Dispatchers.IO) {
                val parentId = authRepository.currentUserId ?: "parent"
                val byeSignal = WebRtcSignal(
                    id = UUID.randomUUID().toString(),
                    sessionId = current.sessionId,
                    senderId = parentId,
                    receiverId = current.childId,
                    type = "bye",
                    data = "session_closed"
                )
                signalingRepository.sendSignal(byeSignal)
            }
        }
        pollJob?.cancel()
        pollJob = null
        stopTelemetry()
        processedSignalIds.clear()
        _session.value = WebRtcSessionInfo(state = WebRtcSessionState.DISCONNECTED, statusMessage = "Session ended")
    }

    private fun generateLocalSdpOffer(mediaType: String): String {
        return """
            v=0
            o=- ${System.currentTimeMillis()} 2 IN IP4 127.0.0.1
            s=GameCentreParent WebRTC Session
            t=0 0
            m=$mediaType 9 UDP/TLS/RTP/SAVPF 111 103 104
            c=IN IP4 0.0.0.0
            a=sendrecv
            a=rtcp-mux
        """.trimIndent()
    }
}
