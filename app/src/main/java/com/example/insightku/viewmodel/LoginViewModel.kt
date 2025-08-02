package com.example.insightku.viewmodel

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.ui.components.auth.login.LoginEvent
import com.example.insightku.ui.components.auth.login.LoginState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {

    private var _uiState = mutableStateOf(LoginState())
    val uiState by _uiState

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> {
                val emailError = validateEmail(event.value)
                _uiState.value = _uiState.value.copy(
                    email = event.value,
                    emailError = emailError,
                    error = null,
                    isLoginEnabled = isValidInput(event.value, _uiState.value.password)
                )
            }
            is LoginEvent.PasswordChanged -> {
                val passwordError = validatePassword(event.value)
                _uiState.value = _uiState.value.copy(
                    password = event.value,
                    passwordError = passwordError,
                    error = null,
                    isLoginEnabled = isValidInput(_uiState.value.email, event.value)
                )
            }
            is LoginEvent.Submit -> {
                if (validateAllFields()) {
                    login()
                }
            }
        }
    }

    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> null
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Please enter a valid email address"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> null
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
    }

    private fun isValidInput(email: String, password: String): Boolean {
        return email.isNotBlank() &&
               Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
               password.isNotBlank() &&
               password.length >= 6
    }

    private fun validateAllFields(): Boolean {
        val emailError = if (_uiState.value.email.isBlank()) {
            "Email is required"
        } else if (!Patterns.EMAIL_ADDRESS.matcher(_uiState.value.email).matches()) {
            "Please enter a valid email address"
        } else null

        val passwordError = if (_uiState.value.password.isBlank()) {
            "Password is required"
        } else if (_uiState.value.password.length < 6) {
            "Password must be at least 6 characters"
        } else null

        _uiState.value = _uiState.value.copy(
            emailError = emailError,
            passwordError = passwordError
        )

        return emailError == null && passwordError == null
    }

    private fun login() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                delay(1500) // Simulate API call

                if (_uiState.value.email == "test@test.com" && _uiState.value.password == "123456") {
                    handleSuccessfulLogin()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Invalid email or password. Please try again."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "An error occurred: ${e.message}"
                )
            }
        }
    }

    private suspend fun handleSuccessfulLogin() {
        try {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                success = true,
                error = null
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Login successful but failed to save data: ${e.message}"
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, emailError = null, passwordError = null)
    }

    fun resetState() {
        _uiState.value = LoginState()
    }
}