package com.example.device

import com.example.data.model.Device
import com.example.data.model.Pairing
import com.example.pairing.PairingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DashboardState(
    val isLoading: Boolean = false,
    val pairings: List<Pairing> = emptyList(),
    val devices: List<Device> = emptyList(),
    val error: String? = null
)

class DeviceManager(
    private val pairingRepository: PairingRepository,
    private val deviceRepository: DeviceRepository,
    private val coroutineScope: CoroutineScope
) {
    private val _dashboardState = MutableStateFlow(DashboardState(isLoading = true))
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    private var pollJob: Job? = null

    fun loadData() {
        coroutineScope.launch {
            _dashboardState.value = _dashboardState.value.copy(isLoading = true, error = null)
            refreshInternal()
        }
    }

    private suspend fun refreshInternal() {
        val pairingsResult = pairingRepository.getPairings()
        pairingsResult.onSuccess { pairings ->
            val activePairings = pairings.filter { !it.isRevoked }
            val devicesResult = deviceRepository.getPairedDevices(activePairings)
            devicesResult.onSuccess { devices ->
                _dashboardState.value = DashboardState(
                    isLoading = false,
                    pairings = activePairings,
                    devices = devices,
                    error = null
                )
            }.onFailure { err ->
                _dashboardState.value = DashboardState(
                    isLoading = false,
                    pairings = activePairings,
                    devices = emptyList(),
                    error = err.localizedMessage
                )
            }
        }.onFailure { err ->
            _dashboardState.value = _dashboardState.value.copy(
                isLoading = false,
                error = err.localizedMessage ?: "Failed to load devices"
            )
        }
    }

    fun startPolling(intervalMs: Long = 5_000L) {
        pollJob?.cancel()
        pollJob = coroutineScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(intervalMs)
                refreshInternal()
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    suspend fun unpairChild(pairingId: String): Result<Boolean> {
        val result = pairingRepository.revokePairing(pairingId)
        if (result.isSuccess) {
            refreshInternal()
        }
        return result
    }
}
