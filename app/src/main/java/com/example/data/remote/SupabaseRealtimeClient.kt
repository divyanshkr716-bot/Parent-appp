package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

enum class RealtimeConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class RealtimeMessage(
    val topic: String,
    val event: String,
    val payload: JSONObject,
    val ref: String? = null
)

class SupabaseRealtimeClient(
    private val client: SupabaseClient,
    private val coroutineScope: CoroutineScope
) {
    private val tag = "SupabaseRealtime"
    private var webSocket: WebSocket? = null
    private val refCounter = AtomicInteger(1)
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    private val _connectionState = MutableStateFlow(RealtimeConnectionState.DISCONNECTED)
    val connectionState: StateFlow<RealtimeConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<RealtimeMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<RealtimeMessage> = _incomingMessages.asSharedFlow()

    private val subscribedTopics = mutableSetOf<String>()

    fun connect() {
        if (_connectionState.value == RealtimeConnectionState.CONNECTING ||
            _connectionState.value == RealtimeConnectionState.CONNECTED
        ) {
            return
        }

        val baseHttp = client.baseUrl
        if (baseHttp.isBlank() || baseHttp.contains("your-project")) {
            _connectionState.value = RealtimeConnectionState.DISCONNECTED
            return
        }

        val wsUrl = baseHttp
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .plus("/realtime/v1/websocket?apikey=${client.anonKey}&vsn=1.0.0")

        _connectionState.value = RealtimeConnectionState.CONNECTING

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(tag, "Realtime WebSocket connected")
                _connectionState.value = RealtimeConnectionState.CONNECTED
                startHeartbeat()
                // Re-subscribe to any previously active topics
                val topicsToJoin = synchronized(subscribedTopics) { subscribedTopics.toList() }
                topicsToJoin.forEach { topic ->
                    sendJoinTopic(topic)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val topic = json.optString("topic", "")
                    val event = json.optString("event", "")
                    val payload = json.optJSONObject("payload") ?: JSONObject()
                    val ref = json.optString("ref", null)

                    _incomingMessages.tryEmit(RealtimeMessage(topic, event, payload, ref))
                } catch (e: Exception) {
                    Log.e(tag, "Failed to parse realtime msg: $text", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(tag, "Realtime WebSocket error: ${t.message}")
                _connectionState.value = RealtimeConnectionState.ERROR
                cleanup()
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "Realtime WebSocket closed: $code / $reason")
                _connectionState.value = RealtimeConnectionState.DISCONNECTED
                cleanup()
            }
        })
    }

    fun subscribeTable(tableName: String) {
        val topic = "realtime:public:$tableName"
        synchronized(subscribedTopics) {
            subscribedTopics.add(topic)
        }
        if (_connectionState.value == RealtimeConnectionState.CONNECTED) {
            sendJoinTopic(topic)
        }
    }

    private fun sendJoinTopic(topic: String) {
        val ref = refCounter.getAndIncrement().toString()
        val joinMsg = JSONObject().apply {
            put("topic", topic)
            put("event", "phx_join")
            put("payload", JSONObject().apply {
                put("config", JSONObject().apply {
                    put("broadcast", JSONObject().apply { put("ack", false); put("self", false) })
                    put("presence", JSONObject().apply { put("key", "") })
                })
            })
            put("ref", ref)
        }
        webSocket?.send(joinMsg.toString())
        Log.d(tag, "Joined topic $topic")
    }

    fun sendBroadcast(topic: String, eventName: String, payload: JSONObject) {
        val ref = refCounter.getAndIncrement().toString()
        val msg = JSONObject().apply {
            put("topic", topic)
            put("event", "broadcast")
            put("payload", JSONObject().apply {
                put("type", "broadcast")
                put("event", eventName)
                put("payload", payload)
            })
            put("ref", ref)
        }
        webSocket?.send(msg.toString())
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = coroutineScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(30_000)
                val ref = refCounter.getAndIncrement().toString()
                val heartbeatMsg = JSONObject().apply {
                    put("topic", "phoenix")
                    put("event", "heartbeat")
                    put("payload", JSONObject())
                    put("ref", ref)
                }
                webSocket?.send(heartbeatMsg.toString())
            }
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = coroutineScope.launch(Dispatchers.IO) {
            delay(5_000)
            if (_connectionState.value != RealtimeConnectionState.CONNECTED) {
                connect()
            }
        }
    }

    fun disconnect() {
        cleanup()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.value = RealtimeConnectionState.DISCONNECTED
    }

    private fun cleanup() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
}
