package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.ui.components.splash.SplashEvent
import com.example.insightku.ui.components.splash.SplashState
import com.example.insightku.ui.components.splash.SplashUiEvent
import com.example.insightku.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val uiState = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = uiState.asStateFlow()

    private val _uiEvent = Channel<SplashUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        onEvent(SplashEvent.CheckAuthStatus)
    }

    fun onEvent(event: SplashEvent) {
        when (event) {
            is SplashEvent.CheckAuthStatus -> {
                checkAuthStatus()
            }
            is SplashEvent.NavigationComplete -> {
                uiState.value = uiState.value.copy(isCheckingComplete = true)
            }
        }
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            uiState.value = uiState.value.copy(isLoading = true)

            try {
                // Loading minimal 2 detik untuk UX yang baik
                delay(2000)

                // Baca status login dari DataStore
                val isLoggedIn = sessionManager.getLoginStatus()

                uiState.value = uiState.value.copy(
                    isLoading = false,
                    isUserLoggedIn = isLoggedIn,
                    isCheckingComplete = true
                )

                // Emit navigation event berdasarkan status login
                if (isLoggedIn) {
                    _uiEvent.send(SplashUiEvent.NavigateToHome)
                } else {
                    _uiEvent.send(SplashUiEvent.NavigateToAuth)
                }

            } catch (e: Exception) {
                // Jika ada error, arahkan ke auth sebagai fallback
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    isUserLoggedIn = false,
                    isCheckingComplete = true
                )
                _uiEvent.send(SplashUiEvent.NavigateToAuth)
            }
        }
    }
}
