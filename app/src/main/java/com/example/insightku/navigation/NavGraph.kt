package com.example.insightku.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.insightku.ui.components.analytics.AnalyticsScreen
import com.example.insightku.ui.components.auth.LoginScreen
import com.example.insightku.ui.components.auth.SignUpScreen
import com.example.insightku.ui.components.auth.ForgotPasswordScreen
import com.example.insightku.ui.components.budgeting.BudgetingScreen
import com.example.insightku.ui.components.dashboard.DashboardScreen
import com.example.insightku.ui.components.settings.SettingsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    isUserLoggedIn: Boolean,
    onLogout: () -> Unit
) {
    // Determine start destination based on auth state
    val startDestination = if (isUserLoggedIn) Screen.Dashboard.route else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        
        // Auth Flow Screens
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // Clear auth stack and navigate to dashboard
                    navController.navigate(Screen.Dashboard.route) {
                        // Clear the entire auth stack
                        popUpTo(Screen.Login.route) { inclusive = true }
                        // Prevent back navigation to auth screens
                        launchSingleTop = true
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onBack = if (isUserLoggedIn) {
                    // Show back button only if user is already logged in (demo mode)
                    {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                } else null
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = {
                    // Clear auth stack and navigate to dashboard
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onBack = if (isUserLoggedIn) {
                    {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.SignUp.route) { inclusive = true }
                        }
                    }
                } else null
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onResetSuccess = {
                    // Navigate back to login after successful reset
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onBack = if (isUserLoggedIn) {
                    {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                        }
                    }
                } else null
            )
        }

        // Main App Screens (require authentication)
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToAuth = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen()
        }

        composable(Screen.Budgeting.route) {
            BudgetingScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onLogout = {
                    onLogout()
                    // Navigate to login and clear the main app stack
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

// Extension function to handle bottom nav navigation
fun NavHostController.navigateToBottomNavDestination(route: String) {
    navigate(route) {
        // Pop up to the start destination of the graph to
        // avoid building up a large stack of destinations
        // on the back stack as users select items
        popUpTo(Screen.Dashboard.route) {
            saveState = true
        }
        // Avoid multiple copies of the same destination when
        // re-selecting the same item
        launchSingleTop = true
        // Restore state when re-selecting a previously selected item
        restoreState = true
    }
}