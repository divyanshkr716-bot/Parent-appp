package com.example.commands

import com.example.data.model.Command
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CommandSender(
    private val commandRepository: CommandRepository,
    private val coroutineScope: CoroutineScope
) {
    private val _commands = MutableStateFlow<List<Command>>(emptyList())
    val commands: StateFlow<List<Command>> = _commands.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private var pollJob: Job? = null

    fun loadHistory(childId: String) {
        coroutineScope.launch {
            val result = commandRepository.getCommandHistory(childId)
            result.onSuccess { list ->
                _commands.value = list
            }
        }
    }

    fun startPollingHistory(childId: String, intervalMs: Long = 4000L) {
        pollJob?.cancel()
        pollJob = coroutineScope.launch(Dispatchers.IO) {
            while (isActive) {
                val result = commandRepository.getCommandHistory(childId)
                result.onSuccess { list ->
                    _commands.value = list
                }
                delay(intervalMs)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    suspend fun dispatchCommand(
        childId: String,
        type: String,
        payload: Map<String, Any?> = emptyMap()
    ): Result<Command> {
        _isSending.value = true
        val result = commandRepository.sendCommand(childId, type, payload)
        _isSending.value = false
        result.onSuccess { newCmd ->
            _commands.value = listOf(newCmd) + _commands.value.filter { it.id != newCmd.id }
        }
        return result
    }

    suspend fun requestStatus(childId: String): Result<Command> =
        dispatchCommand(childId, Command.TYPE_REQUEST_STATUS)

    suspend fun requestLocation(childId: String): Result<Command> =
        dispatchCommand(childId, Command.TYPE_REQUEST_LOCATION)

    suspend fun startScreenShare(childId: String, sessionId: String): Result<Command> =
        dispatchCommand(childId, Command.TYPE_START_SCREEN_SHARE, mapOf("session_id" to sessionId))

    suspend fun stopScreenShare(childId: String, sessionId: String): Result<Command> =
        dispatchCommand(childId, Command.TYPE_STOP_SCREEN_SHARE, mapOf("session_id" to sessionId))

    suspend fun startCamera(childId: String, sessionId: String, lens: String = "back"): Result<Command> =
        dispatchCommand(childId, Command.TYPE_START_CAMERA, mapOf("session_id" to sessionId, "lens" to lens))

    suspend fun stopCamera(childId: String, sessionId: String): Result<Command> =
        dispatchCommand(childId, Command.TYPE_STOP_CAMERA, mapOf("session_id" to sessionId))

    suspend fun startAudio(childId: String, sessionId: String): Result<Command> =
        dispatchCommand(childId, Command.TYPE_START_AUDIO, mapOf("session_id" to sessionId))

    suspend fun stopAudio(childId: String, sessionId: String): Result<Command> =
        dispatchCommand(childId, Command.TYPE_STOP_AUDIO, mapOf("session_id" to sessionId))

    suspend fun browseFiles(childId: String, directoryPath: String = "/"): Result<Command> =
        dispatchCommand(childId, Command.TYPE_BROWSE_FILES, mapOf("path" to directoryPath))

    suspend fun requestNotifications(childId: String): Result<Command> =
        dispatchCommand(childId, Command.TYPE_REQUEST_NOTIFICATIONS)

    suspend fun ping(childId: String): Result<Command> =
        dispatchCommand(childId, Command.TYPE_PING)

    suspend fun cancel(commandId: String): Result<Boolean> {
        return commandRepository.cancelCommand(commandId)
    }
}
