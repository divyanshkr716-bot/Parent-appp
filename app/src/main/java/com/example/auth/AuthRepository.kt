package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.data.remote.SupabaseAuthApi
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class AuthRepository(
    private val context: Context,
    private val supabaseClient: SupabaseClient
) {
    private val authApi = SupabaseAuthApi(supabaseClient)
    private val prefs: SharedPreferences = context.getSharedPreferences("parent_auth_prefs", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val KEY_ACCESS_TOKEN = "auth_access_token"
    private val KEY_REFRESH_TOKEN = "auth_refresh_token"
    private val KEY_EXPIRES_AT = "auth_expires_at"
    private val KEY_USER_ID = "auth_user_id"
    private val KEY_USER_EMAIL = "auth_user_email"

    init {
        restoreSession()
    }

    fun restoreSession() {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val userId = prefs.getString(KEY_USER_ID, null)
        val email = prefs.getString(KEY_USER_EMAIL, null)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, "") ?: ""
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)

        if (!token.isNullOrBlank() && !userId.isNullOrBlank() && !email.isNullOrBlank()) {
            val session = AuthSession(
                accessToken = token,
                refreshToken = refreshToken,
                user = AuthUser(id = userId, email = email),
                expiresAt = expiresAt
            )
            _authState.value = AuthState.Authenticated(session)
        } else {
            _authState.value = AuthState.Unauthenticated()
        }
    }

    suspend fun signIn(email: String, password: String): Result<AuthSession> {
        _authState.value = AuthState.Loading
        val result = authApi.signIn(email, password)
        result.onSuccess { session ->
            persistSession(session)
            _authState.value = AuthState.Authenticated(session)
        }.onFailure { error ->
            _authState.value = AuthState.Error(error.localizedMessage ?: "Login failed")
        }
        return result
    }

    suspend fun signUp(email: String, password: String): Result<AuthSession> {
        _authState.value = AuthState.Loading
        val result = authApi.signUp(email, password)
        result.onSuccess { session ->
            if (session.accessToken.isNotBlank()) {
                persistSession(session)
                _authState.value = AuthState.Authenticated(session)
            } else {
                _authState.value = AuthState.Unauthenticated("Registration successful. Please verify your email if required, then sign in.")
            }
        }.onFailure { error ->
            _authState.value = AuthState.Error(error.localizedMessage ?: "Sign up failed")
        }
        return result
    }

    suspend fun recoverPassword(email: String): Result<String> {
        return authApi.recoverPassword(email)
    }

    fun logout() {
        prefs.edit().clear().apply()
        _authState.value = AuthState.Unauthenticated()
    }

    val currentSession: AuthSession?
        get() = (_authState.value as? AuthState.Authenticated)?.session

    val currentUserId: String?
        get() = currentSession?.user?.id

    val currentToken: String?
        get() = currentSession?.accessToken

    private fun persistSession(session: AuthSession) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_EXPIRES_AT, session.expiresAt)
            .putString(KEY_USER_ID, session.user.id)
            .putString(KEY_USER_EMAIL, session.user.email)
            .apply()
    }
}
