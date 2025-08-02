package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// UI State data classes
data class MainUiState(
    val isLoggedIn: Boolean = false,
    val currentScreen: Screen = Screen.Dashboard,
    val showAuthFlow: Boolean = false,
    val showStreakDetails: Boolean = false,
    val showTransactionDetails: Boolean = false,
    val showAddTransactionDialog: Boolean = false,
    val showEditCategoryDialog: Boolean = false,
    val showAddCategoryDialog: Boolean = false,
    val showRecurringBudgetsDialog: Boolean = false,
    val showWhatsAppSetupDialog: Boolean = false,
    val showReceiptScanner: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class UserData(
    val email: String = "",
    val name: String = "",
    val token: String = ""
)

data class AppSettings(
    val theme: String = "light",
    val defaultInputMode: String = "ocr",
    val whatsappEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val budgetAlertsEnabled: Boolean = true,
    val biometricsEnabled: Boolean = false
)

sealed class Screen {
    object Dashboard : Screen()
    object Analytics : Screen()
    object Budgeting : Screen()
    object Settings : Screen()
    object Auth : Screen()
    object StreakDetails : Screen()
    object TransactionDetails : Screen()
}

class MainViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val _userData = MutableStateFlow(UserData())
    val userData: StateFlow<UserData> = _userData.asStateFlow()
    
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()
    
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()
    
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    
    private val _recurringBudgets = MutableStateFlow<List<RecurringBudget>>(emptyList())
    val recurringBudgets: StateFlow<List<RecurringBudget>> = _recurringBudgets.asStateFlow()
    
    private val _editingCategory = MutableStateFlow<Category?>(null)
    val editingCategory: StateFlow<Category?> = _editingCategory.asStateFlow()
    
    init {
        checkLoginStatus()
        loadInitialData()
    }
    
    private fun checkLoginStatus() {
        viewModelScope.launch {
            // Simulate checking stored login token
            // In real implementation, check SharedPreferences or DataStore
            val hasToken = false // Check actual stored token
            
            _uiState.value = _uiState.value.copy(
                isLoggedIn = hasToken,
                showAuthFlow = !hasToken
            )
        }
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Load user data, transactions, categories, etc.
                // This would typically come from repository/database
                
                // Load settings
                _appSettings.value = AppSettings(
                    theme = "light",
                    defaultInputMode = "ocr",
                    whatsappEnabled = false,
                    notificationsEnabled = true,
                    budgetAlertsEnabled = true,
                    biometricsEnabled = false
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    // Navigation
    fun navigateToScreen(screen: Screen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }
    
    fun showAuthFlow(show: Boolean = true) {
        _uiState.value = _uiState.value.copy(showAuthFlow = show)
    }
    
    fun showStreakDetails(show: Boolean = true) {
        _uiState.value = _uiState.value.copy(showStreakDetails = show)
    }
    
    fun showTransactionDetails(show: Boolean = true) {
        _uiState.value = _uiState.value.copy(showTransactionDetails = show)
    }
    
    // Dialog Management
    fun showAddTransactionDialog(show: Boolean = true) {
        _uiState.value = _uiState.value.copy(showAddTransactionDialog = show)
    }
    
    fun showEditCategoryDialog(show: Boolean = true, category: Category? = null) {
        _editingCategory.value = category
        _uiState.value = _uiState.value.copy(showEditCategoryDialog = show)
    }
    
    fun showAddCategoryDialog(show: Boolean = true) {
        _uiState.value = _uiState.value.copy(showAddCategoryDialog = show)
    }
    
    fun showRecurringBudgetsDialog(show: Boolean = true) {
        _uiState.value = _uiState.value.copy(showRecurringBudgetsDialog = show)
    }
    
    fun showWhatsAppSetupDialog(show: Boolean = true) {
        _uiState.value = _uiState.value.copy(showWhatsAppSetupDialog = show)
    }
    
    fun showReceiptScanner(show: Boolean = true) {
        _uiState.value = _uiState.value.copy(showReceiptScanner = show)
    }
    
    fun showLogoutDialog(show: Boolean = true) {
        _uiState.value = _uiState.value.copy(showLogoutDialog = show)
    }
    
    // Auth Functions
    fun handleAuthSuccess(userData: UserData) {
        viewModelScope.launch {
            _userData.value = userData
            _uiState.value = _uiState.value.copy(
                isLoggedIn = true,
                showAuthFlow = false,
                currentScreen = Screen.Dashboard
            )
            
            // Save login token
            // In real implementation, save to SharedPreferences or DataStore
        }
    }
    
    fun handleLogout() {
        viewModelScope.launch {
            // Clear user data
            _userData.value = UserData()
            _transactions.value = emptyList()
            _categories.value = emptyList()
            _recurringBudgets.value = emptyList()
            
            // Reset UI state
            _uiState.value = MainUiState(
                showAuthFlow = true,
                currentScreen = Screen.Auth
            )
            
            // Clear stored data
            // In real implementation, clear SharedPreferences or DataStore
        }
    }
    
    // Transaction Management
    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val currentTransactions = _transactions.value.toMutableList()
            currentTransactions.add(0, transaction)
            _transactions.value = currentTransactions
            
            // Close dialog
            _uiState.value = _uiState.value.copy(showAddTransactionDialog = false)
        }
    }
    
    fun editTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val currentTransactions = _transactions.value.toMutableList()
            val index = currentTransactions.indexOfFirst { it.id == transaction.id }
            if (index != -1) {
                currentTransactions[index] = transaction
                _transactions.value = currentTransactions
            }
        }
    }
    
    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            val currentTransactions = _transactions.value.toMutableList()
            currentTransactions.removeAll { it.id == transactionId }
            _transactions.value = currentTransactions
        }
    }
    
    // Category Management
    fun addCategory(category: Category) {
        viewModelScope.launch {
            val currentCategories = _categories.value.toMutableList()
            currentCategories.add(category)
            _categories.value = currentCategories
            
            _uiState.value = _uiState.value.copy(showAddCategoryDialog = false)
        }
    }
    
    fun editCategory(category: Category) {
        viewModelScope.launch {
            val currentCategories = _categories.value.toMutableList()
            val index = currentCategories.indexOfFirst { it.id == category.id }
            if (index != -1) {
                currentCategories[index] = category
                _categories.value = currentCategories
            }
            
            _editingCategory.value = null
            _uiState.value = _uiState.value.copy(showEditCategoryDialog = false)
        }
    }
    
    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            val currentCategories = _categories.value.toMutableList()
            currentCategories.removeAll { it.id == categoryId }
            _categories.value = currentCategories
            
            _editingCategory.value = null
            _uiState.value = _uiState.value.copy(showEditCategoryDialog = false)
        }
    }
    
    // Recurring Budget Management
    fun addRecurringBudget(budget: RecurringBudget) {
        viewModelScope.launch {
            val currentBudgets = _recurringBudgets.value.toMutableList()
            currentBudgets.add(budget)
            _recurringBudgets.value = currentBudgets
            
            _uiState.value = _uiState.value.copy(showRecurringBudgetsDialog = false)
        }
    }
    
    // Settings Management
    fun updateTheme(theme: String) {
        viewModelScope.launch {
            _appSettings.value = _appSettings.value.copy(theme = theme)
            // Save to persistent storage
        }
    }
    
    fun updateDefaultInputMode(mode: String) {
        viewModelScope.launch {
            _appSettings.value = _appSettings.value.copy(defaultInputMode = mode)
            // Save to persistent storage
        }
    }
    
    fun updateWhatsAppEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _appSettings.value = _appSettings.value.copy(whatsappEnabled = enabled)
            // Save to persistent storage
        }
    }
    
    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _appSettings.value = _appSettings.value.copy(notificationsEnabled = enabled)
            // Save to persistent storage
        }
    }
    
    fun updateBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _appSettings.value = _appSettings.value.copy(budgetAlertsEnabled = enabled)
            // Save to persistent storage
        }
    }
    
    fun updateBiometricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _appSettings.value = _appSettings.value.copy(biometricsEnabled = enabled)
            // Save to persistent storage
        }
    }
    
    // Error Handling
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun showError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }
    
    // Receipt Scanner
    fun handleReceiptScanned(extractedData: Map<String, String>) {
        viewModelScope.launch {
            // Process OCR data and create transaction
            val transaction = Transaction(
                id = System.currentTimeMillis().toString(),
                title = extractedData["merchant"] ?: "Unknown Merchant",
                category = extractedData["category"] ?: "Others",
                amount = -(extractedData["amount"]?.toDoubleOrNull() ?: 0.0),
                description = extractedData["description"] ?: "",
                date = extractedData["date"] ?: java.time.LocalDate.now().toString(),
                isIncome = false
            )
            
            addTransaction(transaction)
            _uiState.value = _uiState.value.copy(showReceiptScanner = false)
        }
    }
    
    // WhatsApp Setup
    fun handleWhatsAppSetupComplete() {
        viewModelScope.launch {
            _appSettings.value = _appSettings.value.copy(whatsappEnabled = true)
            _uiState.value = _uiState.value.copy(showWhatsAppSetupDialog = false)
        }
    }
}

// Data classes
data class Transaction(
    val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val description: String,
    val date: String,
    val isIncome: Boolean
)

data class Category(
    val id: String,
    val name: String,
    val budgetAmount: Double,
    val spentAmount: Double,
    val color: String,
    val icon: String
)

data class RecurringBudget(
    val id: String,
    val name: String,
    val amount: Double,
    val frequency: String,
    val nextDate: String,
    val category: String,
    val isActive: Boolean
)
