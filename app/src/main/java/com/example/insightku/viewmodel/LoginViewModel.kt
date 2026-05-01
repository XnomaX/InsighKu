package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.domain.usecase.auth.GoogleSignInUseCase
import com.example.insightku.domain.usecase.auth.LoginUseCase
import com.example.insightku.ui.components.auth.login.LoginEvent
import com.example.insightku.ui.components.auth.login.LoginUiState
import com.example.insightku.utils.ErrorBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val googleSignInUseCase: GoogleSignInUseCase,
    private val errorBus: ErrorBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> {
                val emailError = validateEmail(event.value)
                _uiState.update { it.copy(email = event.value, emailError = emailError, error = null) }
            }
            is LoginEvent.PasswordChanged -> {
                val passwordError = validatePassword(event.value)
                _uiState.update { it.copy(password = event.value, passwordError = passwordError, error = null) }
            }
            is LoginEvent.Submit -> {
                if (validateAllFields()) login()
            }
            is LoginEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
            }
            is LoginEvent.GoogleSignIn -> {
                signInWithGoogle(event.idToken)
            }
        }
    }

    private fun validateEmail(email: String): String? {
        if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Please enter a valid email address"
        }
        return null
    }

    private fun validatePassword(password: String): String? {
        if (password.isNotBlank() && password.length < 6) {
            return "Password must be at least 6 characters"
        }
        return null
    }

    private fun validateAllFields(): Boolean {
        val emailError = if (_uiState.value.email.isBlank()) "Email is required" else validateEmail(_uiState.value.email)
        val passwordError = if (_uiState.value.password.isBlank()) "Password is required" else validatePassword(_uiState.value.password)
        _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
        return emailError == null && passwordError == null
    }

    private fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            loginUseCase(_uiState.value.email, _uiState.value.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                }
                .onFailure { exception ->
                    val errorMessage = exception.message ?: "An unknown error occurred"
                    _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                    errorBus.send(errorMessage)
                }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            googleSignInUseCase(idToken)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                }
                .onFailure { exception ->
                    val errorMessage = exception.message ?: "Google Sign-In gagal"
                    _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                    errorBus.send(errorMessage)
                }
        }
    }
}