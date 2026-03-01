package com.example.ourmajor.auth

data class AuthUiState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val verificationEmailSentTo: String? = null,
    val needsEmailVerification: Boolean = false
)
