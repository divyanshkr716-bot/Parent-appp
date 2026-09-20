package com.example.webrtc

import com.example.auth.AuthRepository
import com.example.data.model.WebRtcSignal
import com.example.data.remote.SupabaseRestApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WebRtcSignalingRepository(
    private val restApi: SupabaseRestApi,
    private val authRepository: AuthRepository
) {
    suspend fun getSignals(sessionId: String): Result<List<WebRtcSignal>> = withContext(Dispatchers.IO) {
        val token = authRepository.currentToken ?: return@withContext Result.failure(Exception("Not authenticated"))
        restApi.getWebrtcSignals(sessionId, token)
    }

    suspend fun sendSignal(signal: WebRtcSignal): Result<WebRtcSignal> = withContext(Dispatchers.IO) {
        val token = authRepository.currentToken ?: return@withContext Result.failure(Exception("Not authenticated"))
        restApi.sendWebrtcSignal(signal, token)
    }
}
