package com.example.insightku.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    object Login : Screen("login", "Login")
    object SignUp : Screen("signup", "Sign Up")
    object ForgotPassword : Screen("forgot_password", "Forgot Password")

    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.Analytics)
    object Budgeting : Screen("budgeting", "Budgeting", Icons.Default.AccountBalance)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object AddTransaction : Screen("add_transaction", "Add Transaction", Icons.Default.Add)
}
