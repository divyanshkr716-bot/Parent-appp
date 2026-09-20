package com.example.data.remote

import android.content.Context
import com.example.SupabaseConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class SupabaseClient(private val context: Context) {
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val baseUrl: String
        get() = SupabaseConfig.getSupabaseUrl(context).trim().removeSuffix("/")

    val anonKey: String
        get() = SupabaseConfig.getSupabaseAnonKey(context)

    fun createAuthHeaders(token: String? = null): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        val key = anonKey
        headers["apikey"] = key
        val bearer = if (!token.isNullOrBlank()) token else key
        headers["Authorization"] = "Bearer $bearer"
        headers["Content-Type"] = "application/json"
        headers["Accept"] = "application/json"
        return headers
    }

    fun buildRequest(
        url: String,
        method: String = "GET",
        bodyJson: String? = null,
        token: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): Request {
        val builder = Request.Builder().url(url)
        val headers = createAuthHeaders(token)

        headers.forEach { (k, v) -> builder.header(k, v) }
        extraHeaders.forEach { (k, v) -> builder.header(k, v) }

        val requestBody: RequestBody? = bodyJson?.toRequestBody("application/json; charset=utf-8".toMediaType())

        when (method.uppercase()) {
            "GET" -> builder.get()
            "POST" -> builder.post(requestBody ?: "".toRequestBody("application/json; charset=utf-8".toMediaType()))
            "PUT" -> builder.put(requestBody ?: "".toRequestBody("application/json; charset=utf-8".toMediaType()))
            "PATCH" -> builder.patch(requestBody ?: "".toRequestBody("application/json; charset=utf-8".toMediaType()))
            "DELETE" -> builder.delete(requestBody)
        }

        return builder.build()
    }
}
