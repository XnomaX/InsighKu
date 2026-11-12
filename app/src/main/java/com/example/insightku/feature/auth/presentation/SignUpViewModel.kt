package com.example.insightku.feature.auth.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.R
import com.example.insightku.core.i18n.LocaleHelper
import com.example.insightku.core.utils.ErrorBus
import com.example.insightku.feature.auth.domain.SignUpUseCase
import com.example.insightku.feature.auth.presentation.SignUpEvent
import com.example.insightku.feature.auth.presentation.SignUpState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * SignUpViewModel — full refactor dari versi lama.
 *
 * MASALAH SEBELUMNYA:
 * 1. `var uiState by mutableStateOf(SignUpState())` — Compose state di ViewModel (SALAH)
 *    ViewModel tidak boleh tahu tentang Compose runtime (@Composable, State, dll.).
 *    Jika ditest dengan unit test biasa (tanpa Compose), ini akan crash.
 *
 * 2. `FirebaseAuth.getInstance()` langsung di ViewModel — SALAH
 *    Dependency konkret di ViewModel = tidak bisa ditest, tidak bisa diganti implementasi.
 *
 * 3. Callback-based Firebase (.addOnCompleteListener) di dalam coroutine — SALAH
 *    Campur dua model async (callback dan coroutine) menyebabkan:
 *    - State update (`uiState = ...`) dari dalam callback bisa terjadi di thread yang salah
 *    - viewModelScope sudah selesai saat callback dipanggil jika ViewModel di-clear
 *
 * 4. Tidak ada @HiltViewModel — tidak bisa inject dependency
 *
 * SEKARANG:
 * - MutableStateFlow (pure Kotlin, tidak bergantung Compose)
 * - SignUpUseCase menggantikan Firebase langsung
 * - Async via suspend function, bukan callback
 * - @HiltViewModel untuk dependency injection
 */
@HiltViewModel
class SignUpViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val signUpUseCase: SignUpUseCase,
    private val errorBus: ErrorBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpState())
    val uiState = _uiState.asStateFlow()

    // @ApplicationContext carries the system locale, not the in-app one; wrap so
    // validation messages follow the user's chosen language (see LocaleHelper).
    private fun str(resId: Int): String = LocaleHelper.wrapContext(context).getString(resId)

    fun onEvent(event: SignUpEvent) {
        when (event) {
            is SignUpEvent.NameChanged -> {
                _uiState.update { it.copy(name = event.value, nameError = validateName(event.value)) }
            }
            is SignUpEvent.EmailChanged -> {
                _uiState.update { it.copy(email = event.value, emailError = validateEmail(event.value)) }
            }
            is SignUpEvent.PasswordChanged -> {
                _uiState.update {
                    it.copy(
                        password = event.value,
                        passwordError = validatePassword(event.value),
                        confirmPasswordError = if (it.confirmPassword.isNotEmpty())
                            validateConfirmPassword(event.value, it.confirmPassword) else null
                    )
                }
            }
            is SignUpEvent.ConfirmPasswordChanged -> {
                _uiState.update {
                    it.copy(
                        confirmPassword = event.value,
                        confirmPasswordError = validateConfirmPassword(it.password, event.value)
                    )
                }
            }
            is SignUpEvent.Submit -> {
                if (validateInput()) signUp()
            }
            is SignUpEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    // ─── Validation ────────────────────────────────────────────────────────────

    private fun validateName(name: String): String? = when {
        name.isBlank() -> str(R.string.error_name_empty)
        name.length < 2 -> str(R.string.error_name_short)
        !name.matches(Regex("^[a-zA-Z\\s]+$")) -> str(R.string.error_name_letters)
        else -> null
    }

    private fun validateEmail(email: String): String? = when {
        email.isBlank() -> str(R.string.error_email_empty)
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> str(R.string.error_invalid_email)
        else -> null
    }

    private fun validatePassword(password: String): String? = when {
        password.isBlank() -> str(R.string.error_password_empty)
        password.length < 6 -> str(R.string.error_password_length)
        !password.any { it.isDigit() } -> str(R.string.error_password_digit)
        !password.any { it.isLetter() } -> str(R.string.error_password_letter)
        else -> null
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): String? = when {
        confirmPassword.isBlank() -> str(R.string.error_confirm_password_empty)
        password != confirmPassword -> str(R.string.error_password_mismatch)
        else -> null
    }

    private fun validateInput(): Boolean {
        val nameError = validateName(_uiState.value.name.trim())
        val emailError = validateEmail(_uiState.value.email.trim())
        val passwordError = validatePassword(_uiState.value.password)
        val confirmError = validateConfirmPassword(_uiState.value.password, _uiState.value.confirmPassword)

        _uiState.update {
            it.copy(
                nameError = nameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmError,
                error = null
            )
        }
        return listOf(nameError, emailError, passwordError, confirmError).all { it == null }
    }

    // ─── Sign Up ───────────────────────────────────────────────────────────────

    private fun signUp() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            signUpUseCase(
                email = _uiState.value.email.trim(),
                password = _uiState.value.password,
                name = _uiState.value.name.trim()
            )
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, success = true) }
                }
                .onFailure { exception ->
                    val errorMessage = exception.message ?: str(R.string.error_registration_failed)
                    _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                    errorBus.send(errorMessage)
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetState() {
        _uiState.value = SignUpState()
    }
}


// --- SignUp MVI contract (Event.kt, State.kt) ----------------------------------

sealed class SignUpEvent {
    data class NameChanged(val value: String) : SignUpEvent()
    data class EmailChanged(val value: String) : SignUpEvent()
    data class PasswordChanged(val value: String) : SignUpEvent()
    data class ConfirmPasswordChanged(val value: String) : SignUpEvent()
    object Submit : SignUpEvent()
    object ClearError : SignUpEvent()
}

data class SignUpState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
) {
    val isSignUpEnabled: Boolean
        get() = name.isNotBlank()
            && email.isNotBlank()
            && password.isNotBlank()
            && confirmPassword.isNotBlank()
            && nameError == null
            && emailError == null
            && passwordError == null
            && confirmPasswordError == null
}
