package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.ui.components.auth.resetpassword.ResetPasswordEvent
import com.example.insightku.ui.components.auth.resetpassword.ResetPasswordState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResetPasswordViewModel : ViewModel() {
    private val uiState = MutableStateFlow(ResetPasswordState())
    val state: StateFlow<ResetPasswordState> = uiState.asStateFlow()

    fun onEvent(event: ResetPasswordEvent) {
        when (event) {
            is ResetPasswordEvent.NewPasswordChanged -> {
                uiState.value = uiState.value.copy(newPassword = event.value, error = null, isSuccess = false)
            }
            is ResetPasswordEvent.ConfirmPasswordChanged -> {
                uiState.value = uiState.value.copy(confirmPassword = event.value, error = null, isSuccess = false)
            }
            is ResetPasswordEvent.Submit -> {
                submit(event.oobCode)
            }
            is ResetPasswordEvent.SnackbarShown -> {
                uiState.value = uiState.value.copy(error = null, isSuccess = false)
            }
        }
    }

    private fun submit(oobCode: String) {
        val password = uiState.value.newPassword
        val confirm = uiState.value.confirmPassword

        if (password.length < 6) {
            uiState.value = uiState.value.copy(error = "Password minimal 6 karakter")
            return
        }
        if (password != confirm) {
            uiState.value = uiState.value.copy(error = "Password dan konfirmasi tidak sama")
            return
        }

        viewModelScope.launch {
            uiState.value = uiState.value.copy(isLoading = true, error = null)

            try {
                // TODO: Implementasi Firebase Auth confirmPasswordReset
                // FirebaseAuth.getInstance().confirmPasswordReset(oobCode, password)

                // Simulasi proses reset password
                delay(2000)

                // Simulasi success (bisa diganti dengan actual Firebase call)
                uiState.value = uiState.value.copy(isLoading = false, isSuccess = true)

            } catch (e: Exception) {
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    error = "Reset password gagal: ${e.message}"
                )
            }
        }
    }
}