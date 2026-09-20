package com.example.data.remote

import com.example.data.model.Command
import com.example.data.model.Device
import com.example.data.model.DeviceEvent
import com.example.data.model.MediaObject
import com.example.data.model.Pairing
import com.example.data.model.TelegramFileJob
import com.example.data.model.WebRtcSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.UUID

class SupabaseRestApi(private val client: SupabaseClient) {

    // --- PAIRINGS ---
    suspend fun getPairings(parentId: String, token: String): Result<List<Pairing>> = withContext(Dispatchers.IO) {
        try {
            val encodedParentId = URLEncoder.encode(parentId, "UTF-8")
            val url = "${client.baseUrl}/rest/v1/pairings?select=*&or=(parent_id.eq.$encodedParentId,parent_user_id.eq.$encodedParentId)&order=created_at.desc"
            val request = client.buildRequest(url = url, method = "GET", token = token)
            val response = client.okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: "[]"

            if (!response.isSuccessful) {
                // Try fallback query without or condition
                val fallbackUrl = "${client.baseUrl}/rest/v1/pairings?select=*&parent_id=eq.$encodedParentId&order=created_at.desc"
                val fallbackReq = client.buildRequest(url = fallbackUrl, method = "GET", token = token)
                val fallbackResp = client.okHttpClient.newCall(fallbackReq).execute()
                val fallbackBody = fallbackResp.body?.string() ?: "[]"
                if (!fallbackResp.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to fetch pairings: ${fallbackResp.code}"))
                }
                return@withContext Result.success(parsePairings(fallbackBody))
            }

            Result.success(parsePairings(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPairing(parentId: String, pairingCode: String, token: String): Result<Pairing> = withContext(Dispatchers.IO) {
        try {
            val url = "${client.baseUrl}/rest/v1/pairings"
            val id = UUID.randomUUID().toString()
            val json = JSONObject().apply {
                put("id", id)
                put("parent_id", parentId)
                put("parent_user_id", parentId)
                put("pairing_code", pairingCode)
                put("status", "waiting")
            }

            val extraHeaders = mapOf("Prefer" to "return=representation")
            val request = client.buildRequest(url = url, method = "POST", bodyJson = json.toString(), token = token, extraHeaders = extraHeaders)
            val response = client.okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                // If insertion with both fields failed due to schema constraint, try with just parent_id
                val fallbackJson = JSONObject().apply {
                    put("id", id)
                    put("parent_id", parentId)
                    put("pairing_code", pairingCode)
                    put("status", "waiting")
                }
                val fallbackReq = client.buildRequest(url = url, method = "POST", bodyJson = fallbackJson.toString(), token = token, extraHeaders = extraHeaders)
                val fallbackResp = client.okHttpClient.newCall(fallbackReq).execute()
                val fallbackBody = fallbackResp.body?.string() ?: ""
                if (!fallbackResp.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to create pairing: ${fallbackResp.code} - $fallbackBody"))
                }
                val createdList = parsePairings(fallbackBody)
                return@withContext Result.success(createdList.firstOrNull() ?: Pairing(id = id, parentId = parentId, pairingCode = pairingCode))
            }

            val createdList = parsePairings(body)
            Result.success(createdList.firstOrNull() ?: Pairing(id = id, parentId = parentId, pairingCode = pairingCode))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePairingStatus(pairingId: String, status: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val encodedId = URLEncoder.encode(pairingId, "UTF-8")
            val url = "${client.baseUrl}/rest/v1/pairings?id=eq.$encodedId"
            val json = JSONObject().apply {
                put("status", status)
            }
            val request = client.buildRequest(url = url, method = "PATCH", bodyJson = json.toString(), token = token)
            val response = client.okHttpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- DEVICES ---
    suspend fun getDevices(deviceIds: List<String>, token: String): Result<List<Device>> = withContext(Dispatchers.IO) {
        try {
            if (deviceIds.isEmpty()) return@withContext Result.success(emptyList())
            val inClause = deviceIds.joinToString(",") { "\"$it\"" }
            val url = "${client.baseUrl}/rest/v1/devices?id=in.($inClause)&select=*"
            val request = client.buildRequest(url = url, method = "GET", token = token)
            val response = client.okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: "[]"

            if (!response.isSuccessful) {
                // Try device_id column
                val fallbackUrl = "${client.baseUrl}/rest/v1/devices?device_id=in.($inClause)&select=*"
                val fallbackReq = client.buildRequest(url = fallbackUrl, method = "GET", token = token)
                val fallbackResp = client.okHttpClient.newCall(fallbackReq).execute()
                val fallbackBody = fallbackResp.body?.string() ?: "[]"
                if (!fallbackResp.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to fetch devices: ${fallbackResp.code}"))
                }
                return@withContext Result.success(parseDevices(fallbackBody))
            }

            Result.success(parseDevices(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDevice(deviceId: String, token: String): Result<Device?> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(deviceId, "UTF-8")
            val url = "${client.baseUrl}/rest/v1/devices?id=eq.$encoded&select=*"
            val request = client.buildRequest(url = url, method = "GET", token = token)
            val response = client.okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: "[]"

            if (!response.isSuccessful) {
                val fallbackUrl = "${client.baseUrl}/rest/v1/devices?device_id=eq.$encoded&select=*"
                val fallbackReq = client.buildRequest(url = fallbackUrl, method = "GET", token = token)
                val fallbackResp = client.okHttpClient.newCall(fallbackReq).execute()
                val fallbackBody = fallbackResp.body?.string() ?: "[]"
                return@withContext Result.success(parseDevices(fallbackBody).firstOrNull())
            }

            Result.success(parseDevices(body).firstOrNull())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- COMMANDS ---
    suspend fun getCommands(childId: String, token: String, limit: Int = 50): Result<List<Command>> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(childId, "UTF-8")
            val url = "${client.baseUrl}/rest/v1/commands?or=(child_id.eq.$encoded,device_id.eq.$encoded)&select=*&order=created_at.desc&limit=$limit"
            val request = client.buildRequest(url = url, method = "GET", token = token)
            val response = client.okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: "[]"

            if (!response.isSuccessful) {
                val fallbackUrl = "${client.baseUrl}/rest/v1/commands?child_id=eq.$encoded&select=*&order=created_at.desc&limit=$limit"
                val fallbackReq = client.buildRequest(url = fallbackUrl, method = "GET", token = token)
                val fallbackResp = client.okHttpClient.newCall(fallbackReq).execute()
                val fallbackBody = fallbackResp.body?.string() ?: "[]"
                if (!fallbackResp.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to fetch commands: ${fallbackResp.code}"))
                }
                return@withContext Result.success(parseCommands(fallbackBody))
            }

            Result.success(parseCommands(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendCommand(
        parentId: String,
        childId: String,
        type: String,
        payload: Map<String, Any?>,
        token: String
    ): Result<Command> = withContext(Dispatchers.IO) {
        try {
            val url = "${client.baseUrl}/rest/v1/commands"
            val id = UUID.randomUUID().toString()
            val payloadObj = JSONObject()
            payload.forEach { (k, v) -> payloadObj.put(k, v) }

            val json = JSONObject().apply {
                put("id", id)
                put("parent_id", parentId)
                put("child_id", childId)
                put("device_id", childId)
                put("type", type)
                put("payload", payloadObj)
                put("status", "pending")
            }

            val extraHeaders = mapOf("Prefer" to "return=representation")
            val request = client.buildRequest(url = url, method = "POST", bodyJson = json.toString(), token = token, extraHeaders = extraHeaders)
            val response = client.okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                // Fallback without duplicate device_id if schema rejected it
                val fallbackJson = JSONObject().apply {
                    put("id", id)
                    put("parent_id", parentId)
                    put("child_id", childId)
                    put("type", type)
                    put("payload", payloadObj)
                    put("status", "pending")
                }
                val fallbackReq = client.buildRequest(url = url, method = "POST", bodyJson = fallbackJson.toString(), token = token, extraHeaders = extraHeaders)
                val fallbackResp = client.okHttpClient.newCall(fallbackReq).execute()
                val fallbackBody = fallbackResp.body?.string() ?: ""
                if (!fallbackResp.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to send command: ${fallbackResp.code} - $fallbackBody"))
                }
                val created = parseCommands(fallbackBody).firstOrNull() ?: Command(id = id, parentId = parentId, childId = childId, type = type, payload = payload)
                return@withContext Result.success(created)
            }

            val created = parseCommands(body).firstOrNull() ?: Command(id = id, parentId = parentId, childId = childId, type = type, payload = payload)
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelCommand(commandId: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val encodedId = URLEncoder.encode(commandId, "UTF-8")
            val url = "${client.baseUrl}/rest/v1/commands?id=eq.$encodedId"
            val json = JSONObject().apply {
                put("status", "failed")
                put("result", "Cancelled by parent")
            }
            val request = client.buildRequest(url = url, method = "PATCH", bodyJson = json.toString(), token = token)
            val response = client.okHttpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- EVENTS & NOTIFICATIONS ---
    suspend fun getDeviceEvents(deviceId: String, token: String, limit: Int = 100): Result<List<DeviceEvent>> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(deviceId, "UTF-8")
            // Try device_events first
            val url = "${client.baseUrl}/rest/v1/device_events?or=(device_id.eq.$encoded,child_id.eq.$encoded)&select=*&order=created_at.desc&limit=$limit"
            val request = client.buildRequest(url = url, method = "GET", token = token)
            val response = client.okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: "[]"

            if (response.isSuccessful) {
                return@withContext Result.success(parseDeviceEvents(body))
            }

            // Fallback to child_data table
            val fallbackUrl = "${client.baseUrl}/rest/v1/child_data?device_id=eq.$encoded&select=*&order=created_at.desc&limit=$limit"
            val fallbackReq = client.buildRequest(url = fallbackUrl, method = "GET", token = token)
            val fallbackResp = client.okHttpClient.newCall(fallbackReq).execute()
            val fallbackBody = fallbackResp.body?.string() ?: "[]"
            if (fallbackResp.isSuccessful) {
                return@withContext Result.success(parseDeviceEvents(fallbackBody))
            }

            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- WEBRTC SIGNALS ---
    suspend fun getWebrtcSignals(sessionId: String, token: String): Result<List<WebRtcSignal>> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(sessionId, "UTF-8")
            val url = "${client.baseUrl}/rest/v1/webrtc_signals?session_id=eq.$encoded&select=*&order=created_at.asc"
            val request = client.buildRequest(url = url, method = "GET", token = token)
            val response = client.okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: "[]"

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch WebRTC signals: ${response.code}"))
            }

            Result.success(parseWebRtcSignals(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendWebrtcSignal(signal: WebRtcSignal, token: String): Result<WebRtcSignal> = withContext(Dispatchers.IO) {
        try {
            val url = "${client.baseUrl}/rest/v1/webrtc_signals"
            val id = if (signal.id.isNotBlank()) signal.id else UUID.randomUUID().toString()
            val json = JSONObject().apply {
                put("id", id)
                put("session_id", signal.sessionId)
                put("sender_id", signal.senderId)
                put("receiver_id", signal.receiverId)
                put("type", signal.type)
                signal.sdp?.let { put("sdp", it) }
                signal.candidate?.let { put("candidate", it) }
                signal.data?.let { put("data", it) }
            }

            val extraHeaders = mapOf("Prefer" to "return=representation")
            val request = client.buildRequest(url = url, method = "POST", bodyJson = json.toString(), token = token, extraHeaders = extraHeaders)
            val response = client.okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to post WebRTC signal: ${response.code} - $body"))
            }

            val created = parseWebRtcSignals(body).firstOrNull() ?: signal.copy(id = id)
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- TELEGRAM JOBS & MEDIA OBJECTS ---
    suspend fun getMediaObjects(deviceId: String, token: String): Result<List<MediaObject>> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(deviceId, "UTF-8")
            val url = "${client.baseUrl}/rest/v1/media_objects?device_id=eq.$encoded&select=*&order=created_at.desc"
            val request = client.buildRequest(url = url, method = "GET", token = token)
            val response = client.okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: "[]"

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch media objects: ${response.code}"))
            }

            Result.success(parseMediaObjects(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTelegramFileJobs(deviceId: String, token: String): Result<List<TelegramFileJob>> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(deviceId, "UTF-8")
            val url = "${client.baseUrl}/rest/v1/telegram_file_jobs?device_id=eq.$encoded&select=*&order=created_at.desc"
            val request = client.buildRequest(url = url, method = "GET", token = token)
            val response = client.okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: "[]"

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch Telegram jobs: ${response.code}"))
            }

            Result.success(parseTelegramJobs(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- PARSERS ---
    private fun parsePairings(jsonStr: String): List<Pairing> {
        val list = mutableListOf<Pairing>()
        val array = try { JSONArray(jsonStr) } catch (_: Exception) {
            try { JSONArray().put(JSONObject(jsonStr)) } catch (_: Exception) { return emptyList() }
        }
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString("id", "")
            val parentId = item.optString("parent_id", item.optString("parent_user_id", ""))
            val code = item.optString("pairing_code", "")
            val childDeviceId = item.optString("child_device_id", item.optString("device_id", "")).takeIf { it.isNotBlank() }
            val childName = item.optString("child_name", item.optString("name", "")).takeIf { it.isNotBlank() }
            val status = item.optString("status", "waiting")
            val createdAt = item.optString("created_at", "")
            val pairedAt = item.optString("paired_at", "").takeIf { it.isNotBlank() }
            list.add(Pairing(id, parentId, code, childDeviceId, childName, status, createdAt, pairedAt))
        }
        return list
    }

    private fun parseDevices(jsonStr: String): List<Device> {
        val list = mutableListOf<Device>()
        val array = try { JSONArray(jsonStr) } catch (_: Exception) { return emptyList() }
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString("id", item.optString("device_id", ""))
            val childName = item.optString("child_name", item.optString("name", "Child Device"))
            val model = item.optString("model", "Android Device")
            val osVersion = item.optString("os_version", "Android")
            val battery = item.optInt("battery_level", -1)
            val isCharging = item.optBoolean("is_charging", false)
            val status = item.optString("status", item.optString("connection_state", "offline"))
            val lastSeen = item.optString("last_seen", "")
            val ipAddress = item.optString("ip_address", "")

            val permsMap = mutableMapOf<String, Boolean>()
            val permsObj = item.optJSONObject("permissions")
            permsObj?.keys()?.forEach { key ->
                permsMap[key] = permsObj.optBoolean(key)
            }

            list.add(
                Device(
                    id = id,
                    childName = childName,
                    model = model,
                    osVersion = osVersion,
                    batteryLevel = battery,
                    isCharging = isCharging,
                    status = status,
                    lastSeen = lastSeen,
                    ipAddress = ipAddress,
                    permissions = permsMap
                )
            )
        }
        return list
    }

    private fun parseCommands(jsonStr: String): List<Command> {
        val list = mutableListOf<Command>()
        val array = try { JSONArray(jsonStr) } catch (_: Exception) {
            try { JSONArray().put(JSONObject(jsonStr)) } catch (_: Exception) { return emptyList() }
        }
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString("id", "")
            val parentId = item.optString("parent_id", "")
            val childId = item.optString("child_id", item.optString("device_id", ""))
            val type = item.optString("type", "")
            val status = item.optString("status", "pending")
            val result = item.optString("result", "").takeIf { it.isNotBlank() }
            val createdAt = item.optString("created_at", "")
            val completedAt = item.optString("completed_at", "").takeIf { it.isNotBlank() }

            val payloadMap = mutableMapOf<String, Any?>()
            val payloadObj = item.optJSONObject("payload")
            payloadObj?.keys()?.forEach { key ->
                payloadMap[key] = payloadObj.opt(key)
            }

            list.add(Command(id, parentId, childId, type, payloadMap, status, result, createdAt, completedAt))
        }
        return list
    }

    private fun parseDeviceEvents(jsonStr: String): List<DeviceEvent> {
        val list = mutableListOf<DeviceEvent>()
        val array = try { JSONArray(jsonStr) } catch (_: Exception) { return emptyList() }
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString("id", "")
            val deviceId = item.optString("device_id", item.optString("child_id", ""))
            val eventType = item.optString("event_type", item.optString("type", ""))
            val createdAt = item.optString("created_at", "")

            val dataMap = mutableMapOf<String, Any?>()
            val dataObj = item.optJSONObject("data") ?: item.optJSONObject("payload")
            dataObj?.keys()?.forEach { key ->
                dataMap[key] = dataObj.opt(key)
            }

            list.add(DeviceEvent(id, deviceId, eventType, dataMap, createdAt))
        }
        return list
    }

    private fun parseWebRtcSignals(jsonStr: String): List<WebRtcSignal> {
        val list = mutableListOf<WebRtcSignal>()
        val array = try { JSONArray(jsonStr) } catch (_: Exception) {
            try { JSONArray().put(JSONObject(jsonStr)) } catch (_: Exception) { return emptyList() }
        }
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            list.add(
                WebRtcSignal(
                    id = item.optString("id", ""),
                    sessionId = item.optString("session_id", ""),
                    senderId = item.optString("sender_id", ""),
                    receiverId = item.optString("receiver_id", ""),
                    type = item.optString("type", ""),
                    sdp = item.optString("sdp", "").takeIf { it.isNotBlank() },
                    candidate = item.optString("candidate", "").takeIf { it.isNotBlank() },
                    data = item.optString("data", "").takeIf { it.isNotBlank() },
                    createdAt = item.optString("created_at", "")
                )
            )
        }
        return list
    }

    private fun parseMediaObjects(jsonStr: String): List<MediaObject> {
        val list = mutableListOf<MediaObject>()
        val array = try { JSONArray(jsonStr) } catch (_: Exception) { return emptyList() }
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            list.add(
                MediaObject(
                    id = item.optString("id", ""),
                    deviceId = item.optString("device_id", ""),
                    fileName = item.optString("file_name", ""),
                    fileSize = item.optLong("file_size", 0L),
                    mimeType = item.optString("mime_type", ""),
                    storagePath = item.optString("storage_path", "").takeIf { it.isNotBlank() },
                    telegramMessageId = item.optString("telegram_message_id", "").takeIf { it.isNotBlank() },
                    telegramFileId = item.optString("telegram_file_id", "").takeIf { it.isNotBlank() },
                    status = item.optString("status", "ready"),
                    createdAt = item.optString("created_at", "")
                )
            )
        }
        return list
    }

    private fun parseTelegramJobs(jsonStr: String): List<TelegramFileJob> {
        val list = mutableListOf<TelegramFileJob>()
        val array = try { JSONArray(jsonStr) } catch (_: Exception) { return emptyList() }
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            list.add(
                TelegramFileJob(
                    id = item.optString("id", ""),
                    deviceId = item.optString("device_id", ""),
                    fileName = item.optString("file_name", ""),
                    status = item.optString("status", "uploading"),
                    telegramMessageId = item.optString("telegram_message_id", "").takeIf { it.isNotBlank() },
                    metadata = item.optString("metadata", "").takeIf { it.isNotBlank() },
                    createdAt = item.optString("created_at", "")
                )
            )
        }
        return list
    }
}
