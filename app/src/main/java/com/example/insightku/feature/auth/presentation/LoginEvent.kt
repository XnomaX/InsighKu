package com.example.insightku.feature.auth.presentation

sealed class LoginEvent {
    data class EmailChanged(val value: String) : LoginEvent()
    data class PasswordChanged(val value: String) : LoginEvent()
    object Submit : LoginEvent()
    object ClearError : LoginEvent()
    data class GoogleSignIn(val idToken: String) : LoginEvent()
    data class GoogleSignInFailed(val message: String) : LoginEvent()
}

