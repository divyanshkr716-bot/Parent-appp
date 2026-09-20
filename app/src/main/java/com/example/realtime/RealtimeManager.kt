package com.example.realtime

import com.example.data.remote.RealtimeConnectionState
import com.example.data.remote.RealtimeMessage
import com.example.data.remote.SupabaseRealtimeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RealtimeManager(
    private val realtimeClient: SupabaseRealtimeClient,
    private val coroutineScope: CoroutineScope
) {
    val connectionState: StateFlow<RealtimeConnectionState> = realtimeClient.connectionState
    val incomingMessages: SharedFlow<RealtimeMessage> = realtimeClient.incomingMessages

    fun start() {
        realtimeClient.connect()
        realtimeClient.subscribeTable("devices")
        realtimeClient.subscribeTable("pairings")
        realtimeClient.subscribeTable("commands")
        realtimeClient.subscribeTable("device_events")
        realtimeClient.subscribeTable("webrtc_signals")
    }

    fun stop() {
        realtimeClient.disconnect()
    }
}
