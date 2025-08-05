package com.example.insightku.ui.components.auth.resetpassword

data class ResetPasswordState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

