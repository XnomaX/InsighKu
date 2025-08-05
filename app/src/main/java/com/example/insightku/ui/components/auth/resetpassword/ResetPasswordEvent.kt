package com.example.insightku.ui.components.auth.resetpassword

sealed class ResetPasswordEvent {
    data class NewPasswordChanged(val value: String) : ResetPasswordEvent()
    data class ConfirmPasswordChanged(val value: String) : ResetPasswordEvent()
    data class Submit(val oobCode: String) : ResetPasswordEvent()
    object SnackbarShown : ResetPasswordEvent()
}
