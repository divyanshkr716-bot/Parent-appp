package com.example.pairing

import com.example.auth.AuthRepository
import com.example.data.model.Pairing
import com.example.data.remote.SupabaseRestApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom

class PairingRepository(
    private val restApi: SupabaseRestApi,
    private val authRepository: AuthRepository
) {
    private val random = SecureRandom()

    suspend fun getPairings(): Result<List<Pairing>> = withContext(Dispatchers.IO) {
        val parentId = authRepository.currentUserId ?: return@withContext Result.failure(Exception("Not authenticated"))
        val token = authRepository.currentToken ?: return@withContext Result.failure(Exception("No auth token"))
        restApi.getPairings(parentId, token)
    }

    suspend fun generateAndCreatePairing(): Result<Pairing> = withContext(Dispatchers.IO) {
        val parentId = authRepository.currentUserId ?: return@withContext Result.failure(Exception("Not authenticated"))
        val token = authRepository.currentToken ?: return@withContext Result.failure(Exception("No auth token"))

        // Child App contract: permanent 8-digit numeric code; initial status must be "waiting".
        val codeNumber = 10_000_000 + random.nextInt(90_000_000)
        val pairingCode = codeNumber.toString()
        check(pairingCode.matches(Regex("\\d{8}"))) { "Generated pairing code must be exactly 8 digits" }

        restApi.createPairing(parentId = parentId, pairingCode = pairingCode, token = token)
    }

    suspend fun revokePairing(pairingId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val token = authRepository.currentToken ?: return@withContext Result.failure(Exception("No auth token"))
        restApi.updatePairingStatus(pairingId, "revoked", token)
    }
}
