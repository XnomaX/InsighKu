package com.example.insightku.ui.components.auth.forgotpassword

sealed class ForgotPasswordEvent {
    data class EmailChanged(val email: String) : ForgotPasswordEvent()
    object Submit : ForgotPasswordEvent()
    object Reset : ForgotPasswordEvent()
}