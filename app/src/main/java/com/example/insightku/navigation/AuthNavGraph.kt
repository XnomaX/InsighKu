package com.example.insightku.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.insightku.ui.components.auth.forgotpassword.ForgotPasswordScreen
import com.example.insightku.ui.components.auth.login.LoginScreen
import com.example.insightku.ui.components.auth.signup.SignUpScreen

fun NavGraphBuilder.authNavGraph(navController: NavHostController) {
    navigation(
        route = Route.AUTH_GRAPH,
        startDestination = Route.SIGN_IN
    ) {

        // ── Login ─────────────────────────────────────────────────────────────
        composable(Route.SIGN_IN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Route.MAIN_GRAPH) {
                        popUpTo(Route.AUTH_GRAPH) { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate(Route.SIGN_UP) },
                onNavigateToForgotPassword = { navController.navigate(Route.FORGOT_PASSWORD) }
            )
        }

        // ── Sign Up ───────────────────────────────────────────────────────────
        composable(Route.SIGN_UP) {
            SignUpScreen(
                onNavigateToLogin = {
                    navController.navigate(Route.SIGN_IN) {
                        popUpTo(Route.SIGN_IN) { inclusive = true }
                    }
                },
                onSignUpSuccess = {
                    navController.navigate(Route.MAIN_GRAPH) {
                        popUpTo(Route.AUTH_GRAPH) { inclusive = true }
                    }
                }
            )
        }

        // ── Forgot Password ───────────────────────────────────────────────────
        //
        // Flow:
        // 1. User masukkan email → ForgotPasswordViewModel.sendResetEmail()
        // 2. Firebase kirim email → link ke halaman reset resmi Firebase (browser)
        // 3. User klik link di email → browser terbuka → reset password selesai
        // 4. Setelah selesai di browser, user kembali ke app dan login seperti biasa
        //
        // Tidak perlu ResetPasswordScreen di dalam app.
        composable(Route.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
    }
}
