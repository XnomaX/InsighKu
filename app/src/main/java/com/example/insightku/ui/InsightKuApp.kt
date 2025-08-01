package com.example.insightku.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.insightku.ui.components.analytics.AnalyticsScreen
import com.example.insightku.ui.components.auth.UnifiedAuthFlow
import com.example.insightku.ui.components.budgeting.BudgetingScreen
import com.example.insightku.ui.components.common.*
import com.example.insightku.ui.components.dashboard.DashboardScreen
import com.example.insightku.ui.components.details.*
import com.example.insightku.ui.components.settings.SettingsScreen
import com.example.insightku.ui.dialogs.*
import com.example.insightku.utils.AppConstants
import com.example.insightku.viewmodel.*

@Composable
fun InsightKuApp(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // Collect UI state
    val uiState by viewModel.uiState.collectAsState()
    val userData by viewModel.userData.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val recurringBudgets by viewModel.recurringBudgets.collectAsState()
    val editingCategory by viewModel.editingCategory.collectAsState()
    
    // Show auth flow if not logged in
    if (!uiState.isLoggedIn || uiState.showAuthFlow) {
        UnifiedAuthFlow(
            onAuthSuccess = { userData ->
                viewModel.handleAuthSuccess(userData)
            },
            onBack = if (uiState.showAuthFlow && uiState.isLoggedIn) {
                { viewModel.showAuthFlow(false) }
            } else null
        )
        return
    }
    
    // Show detail screens if requested
    when {
        uiState.showStreakDetails -> {
            StreakDetailsScreen(
                onBack = { viewModel.showStreakDetails(false) }
            )
            return
        }
        
        uiState.showTransactionDetails -> {
            TransactionDetailsScreen(
                transactions = transactions,
                onBack = { viewModel.showTransactionDetails(false) },
                onEditTransaction = { transaction ->
                    viewModel.editTransaction(transaction)
                },
                onDeleteTransaction = { transactionId ->
                    viewModel.deleteTransaction(transactionId)
                }
            )
            return
        }
    }
    
    // Main app content
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main content area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                MainContent(
                    currentScreen = uiState.currentScreen,
                    transactions = transactions,
                    recurringBudgets = recurringBudgets,
                    viewModel = viewModel
                )
            }
            
            // Bottom Navigation
            BottomNavigationBar(
                currentScreen = uiState.currentScreen,
                onScreenSelected = { screen ->
                    if (screen is Screen.AddTransaction) {
                        viewModel.showAddTransactionDialog()
                    } else {
                        viewModel.navigateToScreen(screen)
                    }
                }
            )
        }
        
        // Floating AI Button (only show on dashboard)
        if (uiState.currentScreen is Screen.Dashboard) {
            FloatingAIButton(
                onClick = { viewModel.showReceiptScanner() },
                modifier = Modifier
            )
        }
        
        // Modals and Dialogs
        AppDialogs(
            uiState = uiState,
            appSettings = appSettings,
            editingCategory = editingCategory,
            recurringBudgets = recurringBudgets,
            viewModel = viewModel
        )
        
        // Loading and Error States
        AppOverlays(
            uiState = uiState,
            viewModel = viewModel
        )
    }
}

@Composable
private fun MainContent(
    currentScreen: Screen,
    transactions: List<Transaction>,
    recurringBudgets: List<RecurringBudget>,
    viewModel: MainViewModel
) {
    when (currentScreen) {
        is Screen.Dashboard -> {
            DashboardScreen(
                onScanReceipt = { viewModel.showReceiptScanner() },
                onAddTransaction = { viewModel.showAddTransactionDialog() },
                onViewAllTransactions = { viewModel.showTransactionDetails() },
                onViewStreakDetails = { viewModel.showStreakDetails() },
                onManageRecurring = { viewModel.showRecurringBudgetsDialog() },
                newTransactions = transactions,
                recurringBudgets = recurringBudgets
            )
        }
        
        is Screen.Analytics -> {
            AnalyticsScreen()
        }
        
        is Screen.Budgeting -> {
            BudgetingScreen(
                onAddCategory = { viewModel.showAddCategoryDialog() },
                onEditCategory = { category ->
                    viewModel.showEditCategoryDialog(true, category)
                },
                onManageRecurring = { viewModel.showRecurringBudgetsDialog() }
            )
        }
        
        is Screen.Settings -> {
            SettingsScreen(
                onLogout = { viewModel.showLogoutDialog() },
                onWhatsAppSetup = { viewModel.showWhatsAppSetupDialog() }
            )
        }
        
        else -> {
            // Default to dashboard
            DashboardScreen(
                onScanReceipt = { viewModel.showReceiptScanner() },
                onAddTransaction = { viewModel.showAddTransactionDialog() },
                onViewAllTransactions = { viewModel.showTransactionDetails() },
                onViewStreakDetails = { viewModel.showStreakDetails() },
                onManageRecurring = { viewModel.showRecurringBudgetsDialog() },
                newTransactions = transactions,
                recurringBudgets = recurringBudgets
            )
        }
    }
}

@Composable
private fun AppDialogs(
    uiState: MainUiState,
    appSettings: AppSettings,
    editingCategory: Category?,
    recurringBudgets: List<RecurringBudget>,
    viewModel: MainViewModel
) {
    // Add Transaction Dialog
    if (uiState.showAddTransactionDialog) {
        AddTransactionDialog(
            isOpen = true,
            onDismiss = { viewModel.showAddTransactionDialog(false) },
            onTransactionAdded = { transaction ->
                viewModel.addTransaction(transaction)
            },
            onOpenScanner = {
                viewModel.showAddTransactionDialog(false)
                viewModel.showReceiptScanner()
            },
            defaultMode = appSettings.defaultInputMode
        )
    }
    
    // Receipt Scanner Dialog
    if (uiState.showReceiptScanner) {
        ReceiptScannerDialog(
            isOpen = true,
            onDismiss = { viewModel.showReceiptScanner(false) },
            onTransactionAdded = { transaction ->
                viewModel.addTransaction(transaction)
            }
        )
    }
    
    // Edit Category Dialog
    if (uiState.showEditCategoryDialog && editingCategory != null) {
        EditCategoryDialog(
            isOpen = true,
            onDismiss = { viewModel.showEditCategoryDialog(false) },
            category = editingCategory,
            onCategoryEdited = { category ->
                viewModel.editCategory(category)
            },
            onCategoryDeleted = { categoryId ->
                viewModel.deleteCategory(categoryId)
            }
        )
    }
    
    // Add Category Dialog
    if (uiState.showAddCategoryDialog) {
        AddCategoryDialog(
            isOpen = true,
            onDismiss = { viewModel.showAddCategoryDialog(false) },
            onCategoryAdded = { category ->
                viewModel.addCategory(category)
            }
        )
    }
    
    // Recurring Budgets Dialog
    if (uiState.showRecurringBudgetsDialog) {
        RecurringBudgetsDialog(
            isOpen = true,
            onDismiss = { viewModel.showRecurringBudgetsDialog(false) },
            recurringBudgets = recurringBudgets,
            onBudgetAdded = { budget ->
                viewModel.addRecurringBudget(budget)
            }
        )
    }
    
    // WhatsApp Setup Dialog
    if (uiState.showWhatsAppSetupDialog) {
        WhatsAppSetupDialog(
            isOpen = true,
            onDismiss = { viewModel.showWhatsAppSetupDialog(false) },
            onSetupComplete = {
                viewModel.handleWhatsAppSetupComplete()
            }
        )
    }
    
    // Logout Confirmation Dialog
    if (uiState.showLogoutDialog) {
        LogoutConfirmationDialog(
            isOpen = true,
            onDismiss = { viewModel.showLogoutDialog(false) },
            onConfirmLogout = {
                viewModel.handleLogout()
            }
        )
    }
}

@Composable
private fun AppOverlays(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    // Loading indicator
    if (uiState.isLoading) {
        LoadingOverlay()
    }
    
    // Error messages
    uiState.errorMessage?.let { message ->
        ErrorSnackbar(
            message = message,
            onDismiss = { viewModel.clearError() }
        )
    }
}