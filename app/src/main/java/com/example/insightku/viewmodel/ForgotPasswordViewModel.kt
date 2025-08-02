package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.ui.components.auth.forgotpassword.ForgotPasswordEvent
import com.example.insightku.ui.components.auth.forgotpassword.ForgotPasswordState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ForgotPasswordViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordState())
    val uiState: StateFlow<ForgotPasswordState> = _uiState

    fun onEvent(event: ForgotPasswordEvent) {
        when (event) {
            is ForgotPasswordEvent.EmailChanged -> {
                _uiState.value = _uiState.value.copy(email = event.email)
            }

            is ForgotPasswordEvent.Submit -> {
                if (_uiState.value.email.isBlank()) return

                _uiState.value = _uiState.value.copy(isLoading = true)

                viewModelScope.launch {
                    delay(1500)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isEmailSent = true
                    )
                }
            }

            is ForgotPasswordEvent.Reset -> {
                _uiState.value = ForgotPasswordState()
            }
        }
    }
}