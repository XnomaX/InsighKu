
package com.example.insightku.ui.components.auth.login

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
