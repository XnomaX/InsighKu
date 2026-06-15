package com.example.insightku.feature.auth.presentation

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null
) {
    val isLoginEnabled: Boolean get() = email.isNotBlank() && password.isNotBlank() && emailError == null && passwordError == null
}
