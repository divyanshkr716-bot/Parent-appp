package com.example.telegram

import com.example.auth.AuthRepository
import com.example.data.model.MediaObject
import com.example.data.model.TelegramFileJob
import com.example.data.remote.SupabaseRestApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TelegramMediaRepository(
    private val restApi: SupabaseRestApi,
    private val authRepository: AuthRepository
) {
    suspend fun getMediaObjects(deviceId: String): Result<List<MediaObject>> = withContext(Dispatchers.IO) {
        val token = authRepository.currentToken ?: return@withContext Result.failure(Exception("Not authenticated"))
        restApi.getMediaObjects(deviceId, token)
    }

    suspend fun getTelegramJobs(deviceId: String): Result<List<TelegramFileJob>> = withContext(Dispatchers.IO) {
        val token = authRepository.currentToken ?: return@withContext Result.failure(Exception("Not authenticated"))
        restApi.getTelegramFileJobs(deviceId, token)
    }
}
