package com.example.insightku.feature.auth.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.R
import com.example.insightku.core.i18n.LocaleHelper
import com.example.insightku.core.utils.ErrorBus
import com.example.insightku.feature.auth.domain.GoogleSignInUseCase
import com.example.insightku.feature.auth.domain.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
            is LoginEvent.GoogleSignInFailed -> {
                val message = LocaleHelper.wrapContext(context).getString(R.string.error_google_signin_failed)
                _uiState.update { it.copy(isLoading = false, error = message) }
            }
            is LoginEvent.GoogleAccountUnavailable -> {
                val message = LocaleHelper.wrapContext(context).getString(R.string.error_google_account_unavailable)
                _uiState.update { it.copy(isLoading = false, error = message) }
            }
        }
    }

    private fun validateEmail(email: String): String? {
        if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return context.getString(R.string.error_invalid_email)
        }
        return null
    }

    private fun validatePassword(password: String): String? {
        if (password.isNotBlank() && password.length < 6) {
            return context.getString(R.string.error_password_length)
        }
        return null
    }

    private fun validateAllFields(): Boolean {
        val emailError = if (_uiState.value.email.isBlank()) context.getString(R.string.error_email_empty) else validateEmail(_uiState.value.email)
        val passwordError = if (_uiState.value.password.isBlank()) context.getString(R.string.error_password_empty) else validatePassword(_uiState.value.password)
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
                    val errorMessage = exception.message ?: context.getString(R.string.error_something_went_wrong)
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
                .onFailure {
                    val errorMessage = LocaleHelper.wrapContext(context).getString(R.string.error_google_signin_failed)
                    _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                    errorBus.send(errorMessage)
                }
        }
    }
}


// --- Login MVI contract (Event.kt, State.kt) -----------------------------------

sealed class LoginEvent {
    data class EmailChanged(val value: String) : LoginEvent()
    data class PasswordChanged(val value: String) : LoginEvent()
    object Submit : LoginEvent()
    object ClearError : LoginEvent()
    data class GoogleSignIn(val idToken: String) : LoginEvent()
    object GoogleSignInFailed : LoginEvent()
    object GoogleAccountUnavailable : LoginEvent()
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null
) {
    val isLoginEnabled: Boolean get() = email.isNotBlank() && password.isNotBlank() && emailError == null && passwordError == null
}
