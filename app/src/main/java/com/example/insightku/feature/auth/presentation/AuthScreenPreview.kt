
package com.example.insightku.feature.auth.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen(
            onLoginSuccess = {},
            onNavigateToSignUp = {},
            onNavigateToForgotPassword = {},
            onBack = {}
        )
    }
}

