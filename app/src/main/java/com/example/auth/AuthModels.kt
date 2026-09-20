package com.example.auth

data class AuthUser(
    val id: String,
    val email: String,
    val createdAt: String = "",
    val lastSignInAt: String = ""
)

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthUser,
    val expiresAt: Long = 0L
) {
    val isValid: Boolean get() = accessToken.isNotBlank() && (expiresAt == 0L || System.currentTimeMillis() < expiresAt)
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Authenticated(val session: AuthSession) : AuthState()
    data class Unauthenticated(val message: String? = null) : AuthState()
    data class Error(val error: String) : AuthState()
}
