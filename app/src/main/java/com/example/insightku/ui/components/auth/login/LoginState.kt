package com.example.insightku.ui.components.auth.login

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val isLoginEnabled: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null
)
