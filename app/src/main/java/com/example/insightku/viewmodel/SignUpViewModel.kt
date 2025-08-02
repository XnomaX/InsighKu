package com.example.insightku.viewmodel

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.ui.components.auth.signup.SignUpEvent
import com.example.insightku.ui.components.auth.signup.SignUpState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class
SignUpViewModel @Inject constructor() : ViewModel() {

    var uiState by mutableStateOf(SignUpState())
        private set

    fun onEvent(event: SignUpEvent) {
        when (event) {
            is SignUpEvent.NameChanged -> {
                uiState = uiState.copy(
                    name = event.value,
                    nameError = validateName(event.value)
                )
            }
            is SignUpEvent.EmailChanged -> {
                uiState = uiState.copy(
                    email = event.value,
                    emailError = validateEmail(event.value)
                )
            }
            is SignUpEvent.PasswordChanged -> {
                uiState = uiState.copy(
                    password = event.value,
                    passwordError = validatePassword(event.value),
                    confirmPasswordError = if (uiState.confirmPassword.isNotEmpty())
                        validateConfirmPassword(event.value, uiState.confirmPassword) else null
                )
            }
            is SignUpEvent.ConfirmPasswordChanged -> {
                uiState = uiState.copy(
                    confirmPassword = event.value,
                    confirmPasswordError = validateConfirmPassword(uiState.password, event.value)
                )
            }
            is SignUpEvent.Submit -> {
                if (validateInput()) {
                    signUp()
                }
            }
        }
    }

    private fun validateName(name: String): String? {
        return when {
            name.isBlank() -> "Nama tidak boleh kosong"
            name.length < 2 -> "Nama minimal 2 karakter"
            !name.matches(Regex("^[a-zA-Z\\s]+$")) -> "Nama hanya boleh berisi huruf dan spasi"
            else -> null
        }
    }

    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email tidak boleh kosong"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Format email tidak valid"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password tidak boleh kosong"
            password.length < 6 -> "Password minimal 6 karakter"
            !password.any { it.isDigit() } -> "Password harus mengandung minimal 1 angka"
            !password.any { it.isLetter() } -> "Password harus mengandung minimal 1 huruf"
            else -> null
        }
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isBlank() -> "Konfirmasi password tidak boleh kosong"
            password != confirmPassword -> "Password tidak cocok"
            else -> null
        }
    }

    private fun validateInput(): Boolean {
        val name = uiState.name.trim()
        val email = uiState.email.trim()
        val password = uiState.password
        val confirmPassword = uiState.confirmPassword

        return when {
            name.isBlank() -> {
                uiState = uiState.copy(error = "Nama tidak boleh kosong")
                false
            }
            email.isBlank() -> {
                uiState = uiState.copy(error = "Email tidak boleh kosong")
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                uiState = uiState.copy(error = "Format email tidak valid")
                false
            }
            password.isBlank() -> {
                uiState = uiState.copy(error = "Password tidak boleh kosong")
                false
            }
            password.length < 6 -> {
                uiState = uiState.copy(error = "Password minimal 6 karakter")
                false
            }
            confirmPassword.isBlank() -> {
                uiState = uiState.copy(error = "Konfirmasi password tidak boleh kosong")
                false
            }
            password != confirmPassword -> {
                uiState = uiState.copy(error = "Password tidak cocok")
                false
            }
            else -> {
                uiState = uiState.copy(error = null)
                true
            }
        }
    }

    private fun signUp() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            try {
                delay(1500) // simulasi sign up ke server

                // TODO: Replace with actual sign up logic
                // For now, we'll just simulate a successful sign up
                uiState = uiState.copy(isLoading = false, success = true)

            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }

    fun resetState() {
        uiState = SignUpState()
    }
}