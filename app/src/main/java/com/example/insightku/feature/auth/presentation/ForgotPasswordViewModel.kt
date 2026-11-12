package com.example.insightku.feature.auth.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.R
import com.example.insightku.core.i18n.LocaleHelper
import com.example.insightku.core.utils.ErrorBus
import com.example.insightku.feature.auth.domain.ForgotPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    @ApplicationContext private val context: Context,
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
                    val errorMessage = exception.message
                        ?: LocaleHelper.wrapContext(context).getString(R.string.error_reset_email_failed)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    errorBus.send(errorMessage)
                }
        }
    }
}


// --- ForgotPassword MVI contract (Event.kt, State.kt) --------------------------

sealed class ForgotPasswordEvent {
    data class EmailChanged(val email: String) : ForgotPasswordEvent()
    object Submit : ForgotPasswordEvent()
    object Reset : ForgotPasswordEvent()
}

data class ForgotPasswordState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isEmailSent: Boolean = false,
    val error: String? = null
)
