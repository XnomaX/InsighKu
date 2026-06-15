package com.example.insightku.feature.auth.presentation

data class ForgotPasswordState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isEmailSent: Boolean = false,
    val error: String? = null
)
