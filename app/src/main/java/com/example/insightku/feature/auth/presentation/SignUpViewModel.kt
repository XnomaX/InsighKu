package com.example.insightku.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.feature.auth.domain.SignUpUseCase
import com.example.insightku.feature.auth.presentation.SignUpEvent
import com.example.insightku.feature.auth.presentation.SignUpState
import com.example.insightku.core.utils.ErrorBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    private val signUpUseCase: SignUpUseCase,
    private val errorBus: ErrorBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpState())
    val uiState = _uiState.asStateFlow()

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
        }
    }

    // ─── Validation ────────────────────────────────────────────────────────────

    private fun validateName(name: String): String? = when {
        name.isBlank() -> "Nama tidak boleh kosong"
        name.length < 2 -> "Nama minimal 2 karakter"
        !name.matches(Regex("^[a-zA-Z\\s]+$")) -> "Nama hanya boleh berisi huruf dan spasi"
        else -> null
    }

    private fun validateEmail(email: String): String? = when {
        email.isBlank() -> "Email tidak boleh kosong"
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Format email tidak valid"
        else -> null
    }

    private fun validatePassword(password: String): String? = when {
        password.isBlank() -> "Password tidak boleh kosong"
        password.length < 6 -> "Password minimal 6 karakter"
        !password.any { it.isDigit() } -> "Password harus mengandung minimal 1 angka"
        !password.any { it.isLetter() } -> "Password harus mengandung minimal 1 huruf"
        else -> null
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): String? = when {
        confirmPassword.isBlank() -> "Konfirmasi password tidak boleh kosong"
        password != confirmPassword -> "Password tidak cocok"
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
                    val errorMessage = exception.message ?: "Gagal mendaftar"
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

