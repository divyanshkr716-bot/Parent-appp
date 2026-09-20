package com.example.commands

import com.example.auth.AuthRepository
import com.example.data.model.Command
import com.example.data.remote.SupabaseRestApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommandRepository(
    private val restApi: SupabaseRestApi,
    private val authRepository: AuthRepository
) {
    suspend fun sendCommand(
        childId: String,
        type: String,
        payload: Map<String, Any?> = emptyMap()
    ): Result<Command> = withContext(Dispatchers.IO) {
        val parentId = authRepository.currentUserId ?: return@withContext Result.failure(Exception("Not authenticated"))
        val token = authRepository.currentToken ?: return@withContext Result.failure(Exception("No auth token"))
        restApi.sendCommand(parentId = parentId, childId = childId, type = type, payload = payload, token = token)
    }

    suspend fun getCommandHistory(childId: String, limit: Int = 50): Result<List<Command>> = withContext(Dispatchers.IO) {
        val token = authRepository.currentToken ?: return@withContext Result.failure(Exception("Not authenticated"))
        restApi.getCommands(childId, token, limit)
    }

    suspend fun cancelCommand(commandId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val token = authRepository.currentToken ?: return@withContext Result.failure(Exception("No auth token"))
        restApi.cancelCommand(commandId, token)
    }
}
