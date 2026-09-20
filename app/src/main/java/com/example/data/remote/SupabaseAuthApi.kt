package com.example.data.remote

import com.example.auth.AuthSession
import com.example.auth.AuthUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SupabaseAuthApi(private val client: SupabaseClient) {

    suspend fun signUp(email: String, password: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        try {
            val url = "${client.baseUrl}/auth/v1/signup"
            val json = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }
            val request = client.buildRequest(url = url, method = "POST", bodyJson = json.toString())
            val response = client.okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(body, "Sign up failed (${response.code})")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val session = parseSession(body, email)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        try {
            val url = "${client.baseUrl}/auth/v1/token?grant_type=password"
            val json = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }
            val request = client.buildRequest(url = url, method = "POST", bodyJson = json.toString())
            val response = client.okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(body, "Login failed (${response.code})")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val session = parseSession(body, email)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recoverPassword(email: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "${client.baseUrl}/auth/v1/recover"
            val json = JSONObject().apply {
                put("email", email.trim())
            }
            val request = client.buildRequest(url = url, method = "POST", bodyJson = json.toString())
            val response = client.okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(body, "Password recovery failed (${response.code})")
                return@withContext Result.failure(Exception(errorMsg))
            }
            Result.success("Password recovery email sent successfully.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(token: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        try {
            val url = "${client.baseUrl}/auth/v1/user"
            val request = client.buildRequest(url = url, method = "GET", token = token)
            val response = client.okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch user session: ${response.code}"))
            }

            val json = JSONObject(body)
            val user = AuthUser(
                id = json.optString("id", ""),
                email = json.optString("email", ""),
                createdAt = json.optString("created_at", ""),
                lastSignInAt = json.optString("last_sign_in_at", "")
            )
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseSession(jsonStr: String, fallbackEmail: String): AuthSession {
        val json = JSONObject(jsonStr)
        val accessToken = json.optString("access_token", "")
        val refreshToken = json.optString("refresh_token", "")
        val expiresIn = json.optLong("expires_in", 3600L)
        val expiresAt = System.currentTimeMillis() + (expiresIn * 1000L)

        val userObj = json.optJSONObject("user")
        val userId = userObj?.optString("id") ?: json.optString("id", "")
        val userEmail = userObj?.optString("email") ?: fallbackEmail

        val user = AuthUser(
            id = userId,
            email = userEmail,
            createdAt = userObj?.optString("created_at") ?: "",
            lastSignInAt = userObj?.optString("last_sign_in_at") ?: ""
        )

        return AuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = user,
            expiresAt = expiresAt
        )
    }

    private fun parseErrorMessage(jsonStr: String, fallback: String): String {
        return try {
            val json = JSONObject(jsonStr)
            json.optString("error_description", json.optString("msg", json.optString("message", fallback)))
        } catch (_: Exception) {
            fallback
        }
    }
}
