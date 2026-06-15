package com.example.insightku.feature.auth.presentation

sealed class ForgotPasswordEvent {
    data class EmailChanged(val email: String) : ForgotPasswordEvent()
    object Submit : ForgotPasswordEvent()
    object Reset : ForgotPasswordEvent()
}
