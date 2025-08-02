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
        composable(Route.SIGN_IN) {
            LoginScreen(
                onNavigateToSignUp = {
                    navController.navigate(Route.SIGN_UP)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Route.FORGOT_PASSWORD)
                },
                onLoginSuccess = {
                    // Navigasi ke main graph dan hapus auth graph dari backstack
                    navController.navigate(Route.MAIN_GRAPH) {
                        popUpTo(Route.AUTH_GRAPH) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable(Route.SIGN_UP) {
            SignUpScreen(
                onNavigateToLogin = {
                    navController.navigate(Route.SIGN_IN) {
                        popUpTo(Route.SIGN_IN) {
                            inclusive = true
                        }
                    }
                },
                onSignUpSuccess = {
                    navController.navigate(Route.MAIN_GRAPH) {
                        popUpTo(Route.AUTH_GRAPH) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable(Route.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
    }
}
