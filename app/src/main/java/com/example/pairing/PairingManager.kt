package com.example.pairing

import com.example.data.model.Pairing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class PairingFlowState {
    object Idle : PairingFlowState()
    object Generating : PairingFlowState()
    data class CodeReady(val pairing: Pairing) : PairingFlowState()
    data class PairedSuccessfully(val pairing: Pairing) : PairingFlowState()
    data class Error(val message: String) : PairingFlowState()
}

class PairingManager(
    private val pairingRepository: PairingRepository,
    private val coroutineScope: CoroutineScope
) {
    private val _pairingState = MutableStateFlow<PairingFlowState>(PairingFlowState.Idle)
    val pairingState: StateFlow<PairingFlowState> = _pairingState.asStateFlow()

    private var pollJob: Job? = null

    fun generateNewPairingCode() {
        coroutineScope.launch {
            _pairingState.value = PairingFlowState.Generating
            val result = pairingRepository.generateAndCreatePairing()
            result.onSuccess { pairing ->
                _pairingState.value = PairingFlowState.CodeReady(pairing)
                startWaitingForClaim(pairing.id)
            }.onFailure { error ->
                _pairingState.value = PairingFlowState.Error(error.localizedMessage ?: "Failed to generate pairing code")
            }
        }
    }

    private fun startWaitingForClaim(pairingId: String) {
        pollJob?.cancel()
        pollJob = coroutineScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(3_000)
                val result = pairingRepository.getPairings()
                result.onSuccess { pairings ->
                    val match = pairings.find { it.id == pairingId }
                    if (match != null && match.isPaired) {
                        _pairingState.value = PairingFlowState.PairedSuccessfully(match)
                        pollJob?.cancel()
                    }
                }
            }
        }
    }

    fun dismiss() {
        pollJob?.cancel()
        _pairingState.value = PairingFlowState.Idle
    }
}
