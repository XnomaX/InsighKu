package com.example.insightku.core.ui.components.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.core.ui.components.splash.SplashEvent
import com.example.insightku.core.ui.components.splash.SplashState
import com.example.insightku.core.ui.components.splash.SplashUiEvent
import com.example.insightku.core.data.local.preferences.SessionManager
import com.example.insightku.feature.auth.data.AuthRepository
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
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository  // BUG9 FIX: Auth via repository (Firebase as source of truth)
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

                // BUG9 FIX: Gunakan Firebase (via AuthRepository) sebagai sumber kebenaran utama.
                // DataStore saja tidak cukup karena:
                // 1. Token Firebase bisa expired tapi DataStore masih isLoggedIn=true
                //    → user masuk Home tapi semua Firestore request gagal
                // 2. Jika clear app data, DataStore terhapus tapi Firebase token masih ada
                //    → user harus login ulang padahal token masih valid
                val isLoggedIn = authRepository.isAuthenticated()

                // Sync DataStore agar konsisten dengan Firebase state
                if (!isLoggedIn) {
                    // Firebase tidak punya user aktif → pastikan DataStore juga clear
                    sessionManager.clearSession()
                }

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



