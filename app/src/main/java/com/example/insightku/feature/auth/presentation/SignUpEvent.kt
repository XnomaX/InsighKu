package com.example.insightku.feature.auth.presentation

sealed class SignUpEvent {
    data class NameChanged(val value: String) : SignUpEvent()
    data class EmailChanged(val value: String) : SignUpEvent()
    data class PasswordChanged(val value: String) : SignUpEvent()
    data class ConfirmPasswordChanged(val value: String) : SignUpEvent()
    object Submit : SignUpEvent()
    object ClearError : SignUpEvent()
}
