package com.example.insightku.ui.components.splash

sealed class SplashEvent {
    object CheckAuthStatus : SplashEvent()
    object NavigationComplete : SplashEvent()
}

sealed class SplashUiEvent {
    object NavigateToAuth : SplashUiEvent()
    object NavigateToHome : SplashUiEvent()
}