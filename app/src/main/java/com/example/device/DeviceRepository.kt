package com.example.device

import com.example.auth.AuthRepository
import com.example.data.model.Device
import com.example.data.model.DeviceEvent
import com.example.data.model.Pairing
import com.example.data.remote.SupabaseRestApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceRepository(
    private val restApi: SupabaseRestApi,
    private val authRepository: AuthRepository
) {
    suspend fun getPairedDevices(pairings: List<Pairing>): Result<List<Device>> = withContext(Dispatchers.IO) {
        val token = authRepository.currentToken ?: return@withContext Result.failure(Exception("Not authenticated"))
        val pairedList = pairings.filter { it.isPaired && !it.childDeviceId.isNullOrBlank() }

        if (pairedList.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        val deviceIds = pairedList.mapNotNull { it.childDeviceId }.distinct()
        val devicesResult = restApi.getDevices(deviceIds, token)

        devicesResult.map { fetchedDevices ->
            pairedList.map { pairing ->
                val matching = fetchedDevices.find { it.id == pairing.childDeviceId }
                if (matching != null) {
                    matching.copy(
                        childName = pairing.childName ?: matching.childName,
                        pairingId = pairing.id
                    )
                } else {
                    // Placeholder device constructed from pairing record until heartbeat arrives
                    Device(
                        id = pairing.childDeviceId ?: "",
                        childName = pairing.childName ?: "Child Device",
                        status = "connecting",
                        pairingId = pairing.id
                    )
                }
            }
        }
    }

    suspend fun getDeviceDetails(deviceId: String): Result<Device?> = withContext(Dispatchers.IO) {
        val token = authRepository.currentToken ?: return@withContext Result.failure(Exception("Not authenticated"))
        restApi.getDevice(deviceId, token)
    }

    suspend fun getDeviceEvents(deviceId: String): Result<List<DeviceEvent>> = withContext(Dispatchers.IO) {
        val token = authRepository.currentToken ?: return@withContext Result.failure(Exception("Not authenticated"))
        restApi.getDeviceEvents(deviceId, token)
    }
}
