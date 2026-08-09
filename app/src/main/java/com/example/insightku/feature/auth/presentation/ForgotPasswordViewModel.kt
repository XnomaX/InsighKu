package com.example.insightku.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.utils.ErrorBus
import com.example.insightku.feature.auth.domain.ForgotPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ForgotPasswordViewModel — refactored.
 *
 * MASALAH SEBELUMNYA:
 * 1. Tidak ada @HiltViewModel → tidak bisa inject dependency
 * 2. Menggunakan `delay(1500)` sebagai simulasi → tidak ada email yang benar-benar terkirim
 * 3. Tidak ada error handling → jika Firebase gagal, tidak ada feedback ke user
 *
 * SEKARANG:
 * - @HiltViewModel dengan ForgotPasswordUseCase
 * - Email reset sungguhan via Firebase Auth
 * - Error ditampilkan di UI dan dilaporkan ke global snackbar
 */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val errorBus: ErrorBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordState())
    val uiState: StateFlow<ForgotPasswordState> = _uiState.asStateFlow()

    fun onEvent(event: ForgotPasswordEvent) {
        when (event) {
            is ForgotPasswordEvent.EmailChanged -> {
                _uiState.value = _uiState.value.copy(email = event.email)
            }
            is ForgotPasswordEvent.Submit -> {
                sendResetEmail()
            }
            is ForgotPasswordEvent.Reset -> {
                _uiState.value = ForgotPasswordState()
            }
        }
    }

    private fun sendResetEmail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            forgotPasswordUseCase(_uiState.value.email)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, isEmailSent = true)
                }
                .onFailure { exception ->
                    val errorMessage = exception.message ?: "Gagal mengirim email reset"
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    errorBus.send(errorMessage)
                }
        }
    }
}

