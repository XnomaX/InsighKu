package com.example.insightku.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.insightku.ui.components.auth.login.LoginScreen
import com.example.insightku.data.model.UserData

@Composable
fun InsightKuApp(
    // viewModel: MainViewModel = viewModel() // COMMENTED OUT: ViewModel not ready
) {
    // TEMPORARY: Simple state management for auth
    var isLoggedIn by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<UserData?>(null) }

    // Show auth flow if not logged in
    if (!isLoggedIn) {
        // TEMPORARY: Simple login screen
        LoginScreen(
            onLoginSuccess = { userData ->
                currentUser = userData
                isLoggedIn = true
            },
            onNavigateToSignUp = {
                // TODO: Implement signup navigation
            },
            onNavigateToForgotPassword = {
                // TODO: Implement forgot password navigation
            }
        )
        return
    }

    // TEMPORARY: Simple dashboard instead of complex navigation
    SimpleDashboard(
        user = currentUser,
        onLogout = {
            isLoggedIn = false
            currentUser = null
        }
    )

    // COMMENTED OUT: Detail screens navigation - Components not ready
    // Show detail screens if requested
    // when {
    //     uiState.showStreakDetails -> {
    //         StreakDetailsScreen(
    //             onBack = { viewModel.showStreakDetails(false) },
    //             onStreakUpdated = { viewModel.refreshData() }
    //         )
    //         return
    //     }
    //     uiState.selectedTransaction != null -> {
    //         TransactionDetailsScreen(
    //             transaction = uiState.selectedTransaction!!,
    //             onBack = { viewModel.selectTransaction(null) },
    //             onEditTransaction = { transaction ->
    //                 viewModel.selectTransaction(null)
    //                 viewModel.showAddTransactionDialog(true, transaction)
    //             },
    //             onDeleteTransaction = { transactionId ->
    //                 viewModel.selectTransaction(null)
    //                 viewModel.deleteTransaction(transactionId)
    //             }
    //         )
    //         return
    //     }
    // }

    // COMMENTED OUT: Main content with navigation - Components not ready
    // Box(modifier = Modifier.fillMaxSize()) {
    //     Column(modifier = Modifier.fillMaxSize()) {
    //         // Main content based on current screen
    //         Box(
    //             modifier = Modifier
    //                 .fillMaxWidth()
    //                 .weight(1f)
    //         ) {
    //             when (uiState.currentScreen) {
    //                 Screen.Dashboard -> {
    //                     DashboardScreen(
    //                         transactions = transactions,
    //                         categories = categories,
    //                         userData = userData,
    //                         onTransactionClick = { transaction ->
    //                             viewModel.selectTransaction(transaction)
    //                         },
    //                         onStreakClick = {
    //                             viewModel.showStreakDetails(true)
    //                         }
    //                     )
    //                 }
    //                 Screen.Analytics -> {
    //                     AnalyticsScreen()
    //                 }
    //
    //                 Screen.Budgeting -> {
    //                     BudgetingScreen(
    //                         categories = categories,
    //                         onEditCategory = { category ->
    //                             viewModel.setEditingCategory(category)
    //                         },
    //                         onAddCategory = {
    //                             viewModel.showAddCategoryDialog(true)
    //                         }
    //                     )
    //                 }
    //
    //                 Screen.Settings -> {
    //                     SettingsScreen(
    //                         appSettings = appSettings,
    //                         onSettingChanged = { setting, value ->
    //                             viewModel.updateSetting(setting, value)
    //                         }
    //                     )
    //                 }
    //
    //                 else -> {
    //                     DashboardScreen(
    //                         transactions = transactions,
    //                         categories = categories,
    //                         userData = userData,
    //                         onTransactionClick = { transaction ->
    //                             viewModel.selectTransaction(transaction)
    //                         },
    //                         onStreakClick = {
    //                             viewModel.showStreakDetails(true)
    //                         }
    //                     )
    //                 }
    //             }
    //         }
    //
    //         // Bottom Navigation
    //         BottomNavigationBar(
    //             currentScreen = uiState.currentScreen,
    //             onScreenSelected = { screen ->
    //                 if (screen is Screen.AddTransaction) {
    //                     viewModel.showAddTransactionDialog(true)
    //                 } else {
    //                     viewModel.navigateToScreen(screen)
    //                 }
    //             }
    //         )
    //     }
    //
    //     // Floating AI Assistant Button
    //     FloatingAIButton(
    //         modifier = Modifier
    //             .align(Alignment.BottomEnd)
    //             .padding(16.dp),
    //         onClick = {
    //             // Handle AI assistant click
    //         }
    //     )
    // }

    // COMMENTED OUT: Dialogs - Components not ready
    // Dialogs
    // if (uiState.showAddTransactionDialog) {
    //     AddTransactionDialog(
    //         categories = categories,
    //         editingTransaction = uiState.editingTransaction,
    //         onTransactionAdded = { transaction ->
    //             viewModel.addTransaction(transaction)
    //             viewModel.showAddTransactionDialog(false)
    //         },
    //         onDismiss = {
    //             viewModel.showAddTransactionDialog(false)
    //         }
    //     )
    // }

    // if (uiState.showReceiptScanner) {
    //     ReceiptScannerDialog(
    //         categories = categories,
    //         onTransactionAdded = { transaction ->
    //             viewModel.addTransaction(transaction)
    //             viewModel.showReceiptScanner(false)
    //         },
    //         onDismiss = {
    //             viewModel.showReceiptScanner(false)
    //         }
    //     )
    // }

    // editingCategory?.let { category ->
    //     EditCategoryDialog(
    //         category = category,
    //         onCategoryEdited = { category ->
    //             viewModel.editCategory(category)
    //             viewModel.setEditingCategory(null)
    //         },
    //         onCategoryDeleted = { categoryId ->
    //             viewModel.deleteCategory(categoryId)
    //             viewModel.setEditingCategory(null)
    //         },
    //         onDismiss = {
    //             viewModel.setEditingCategory(null)
    //         }
    //     )
    // }

    // if (uiState.showAddCategoryDialog) {
    //     AddCategoryDialog(
    //         onCategoryAdded = { category ->
    //             viewModel.addCategory(category)
    //             viewModel.showAddCategoryDialog(false)
    //         },
    //         onDismiss = {
    //             viewModel.showAddCategoryDialog(false)
    //         }
    //     )
    // }

    // if (uiState.showRecurringBudgetsDialog) {
    //     RecurringBudgetsDialog(
    //         categories = categories,
    //         recurringBudgets = recurringBudgets,
    //         onBudgetAdded = { budget ->
    //             viewModel.addRecurringBudget(budget)
    //         },
    //         onDismiss = {
    //             viewModel.showRecurringBudgetsDialog(false)
    //         }
    //     )
    // }

    // if (uiState.showWhatsAppSetup) {
    //     WhatsAppSetupDialog(
    //         onSetupComplete = {
    //             viewModel.showWhatsAppSetup(false)
    //         },
    //         onDismiss = {
    //             viewModel.showWhatsAppSetup(false)
    //         }
    //     )
    // }

    // if (uiState.showLogoutConfirmation) {
    //     LogoutConfirmationDialog(
    //         onConfirm = {
    //             viewModel.logout()
    //         },
    //         onDismiss = {
    //             viewModel.showLogoutConfirmation(false)
    //         }
    //     )
    // }

    // Loading overlay
    // if (uiState.isLoading) {
    //     LoadingOverlay()
    // }

    // Error handling
    // uiState.errorMessage?.let { message ->
    //     ErrorSnackbar(
    //         message = message,
    //         onDismiss = { viewModel.clearError() }
    //     )
    // }
}

@Composable
private fun SimpleDashboard(
    user: UserData?,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Welcome message
        Text(
            text = "Welcome back, ${user?.name ?: "User"}!",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Email: ${user?.email ?: "No email"}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Simple dashboard content
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "InsightKu Dashboard",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Your financial insights will appear here.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Logout button
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }
}
