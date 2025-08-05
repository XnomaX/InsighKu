package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.data.model.Category
import com.example.insightku.data.model.RecurringBudget
import com.example.insightku.ui.components.budgeting.BudgetCategory
import com.example.insightku.ui.components.budgeting.BudgetingEvent
import com.example.insightku.ui.components.budgeting.BudgetingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetingViewModel @Inject constructor(
    // TODO: Inject repository when available
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetingState())
    val uiState = _uiState.asStateFlow()

    init {
        loadBudgetData()
    }

    fun onEvent(event: BudgetingEvent) {
        when (event) {
            // General
            is BudgetingEvent.LoadBudgetData -> loadBudgetData()
            is BudgetingEvent.RefreshData -> loadBudgetData()
            is BudgetingEvent.ClearError -> _uiState.update { it.copy(error = null) }
            is BudgetingEvent.ChangePeriod -> { /* Not implemented yet */ }

            // Category Dialogs
            is BudgetingEvent.ShowAddBudgetDialog -> _uiState.update { it.copy(showAddBudgetDialog = true) }
            is BudgetingEvent.HideAddBudgetDialog -> _uiState.update { it.copy(showAddBudgetDialog = false) }
            is BudgetingEvent.AddCategory -> addCategory(event.category)
            is BudgetingEvent.ShowEditBudgetDialog -> {
                val categoryToEdit = Category(
                    id = event.category.id,
                    name = event.category.name,
                    budgetLimit = event.category.budgetedAmount,
                    color = event.category.color,
                    icon = event.category.icon
                )
                _uiState.update { it.copy(editingCategory = categoryToEdit) }
            }
            is BudgetingEvent.HideEditBudgetDialog -> _uiState.update { it.copy(editingCategory = null) }
            is BudgetingEvent.UpdateCategory -> updateCategory(event.category)
            is BudgetingEvent.DeleteCategory -> deleteCategory(event.categoryId)

            // Recurring Budgets Dialogs
            is BudgetingEvent.ShowRecurringBudgetsDialog -> _uiState.update { it.copy(showRecurringBudgetsDialog = true) }
            is BudgetingEvent.HideRecurringBudgetsDialog -> _uiState.update { it.copy(showRecurringBudgetsDialog = false) }
            is BudgetingEvent.AddRecurringBudget -> addRecurringBudget(event.budget)
            is BudgetingEvent.UpdateRecurringBudget -> updateRecurringBudget(event.budget)
            is BudgetingEvent.DeleteRecurringBudget -> deleteRecurringBudget(event.budget)
        }
    }

    private fun loadBudgetData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                delay(1000)

                val mockBudgetCategories = listOf(
                    BudgetCategory("1", "Food & Dining", 2000000.0, 1650000.0, "#FF6B6B", "restaurant"),
                    BudgetCategory("2", "Transportation", 1000000.0, 750000.0, "#4ECDC4", "directions_car"),
                    BudgetCategory("3", "Shopping", 1500000.0, 1800000.0, "#45B7D1", "shopping_bag"),
                    BudgetCategory("4", "Entertainment", 500000.0, 320000.0, "#96CEB4", "movie"),
                    BudgetCategory("5", "Bills & Utilities", 1200000.0, 1150000.0, "#FECA57", "receipt")
                )

                val mockRecurringBudgets = listOf(
                    RecurringBudget(id = 1, name = "Netflix", amount = 186000.0, frequency = com.example.insightku.data.model.BudgetFrequency.MONTHLY, nextDue = System.currentTimeMillis() + 86400000 * 5, reminderDaysBefore = 3),
                    RecurringBudget(id = 2, name = "Spotify", amount = 54000.0, frequency = com.example.insightku.data.model.BudgetFrequency.MONTHLY, nextDue = System.currentTimeMillis() + 86400000 * 10, reminderDaysBefore = 7)
                )

                _uiState.update { it.copy(recurringBudgets = mockRecurringBudgets) }
                updateStateWith(mockBudgetCategories)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to load budget data: ${e.message}")
                }
            }
        }
    }

    private fun addCategory(category: Category) {
        viewModelScope.launch {
            try {
                val newBudgetCategory = BudgetCategory(
                    id = category.id,
                    name = category.name,
                    budgetedAmount = category.budgetLimit ?: 0.0,
                    spentAmount = 0.0,
                    color = category.color,
                    icon = category.icon ?: "category"
                )
                val updatedCategories = _uiState.value.budgetCategories + newBudgetCategory
                updateStateWith(updatedCategories)
                _uiState.update { it.copy(showAddBudgetDialog = false) }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to add budget category: ${e.message}") }
            }
        }
    }

    private fun updateCategory(category: Category) {
        viewModelScope.launch {
            try {
                val updatedCategories = _uiState.value.budgetCategories.map {
                    if (it.id == category.id) {
                        it.copy(
                            name = category.name,
                            budgetedAmount = category.budgetLimit ?: it.budgetedAmount,
                            color = category.color,
                            icon = category.icon ?: it.icon
                        )
                    } else {
                        it
                    }
                }
                updateStateWith(updatedCategories)
                _uiState.update { it.copy(editingCategory = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update category: ${e.message}") }
            }
        }
    }

    private fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                val updatedCategories = _uiState.value.budgetCategories.filter { it.id != categoryId }
                updateStateWith(updatedCategories)
                _uiState.update { it.copy(editingCategory = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete budget category: ${e.message}") }
            }
        }
    }

    private fun addRecurringBudget(budget: RecurringBudget) {
        viewModelScope.launch {
            val newBudget = budget.copy(id = (_uiState.value.recurringBudgets.maxOfOrNull { it.id } ?: 0) + 1)
            val updatedBudgets = _uiState.value.recurringBudgets + newBudget
            _uiState.update { it.copy(recurringBudgets = updatedBudgets) }
        }
    }

    private fun updateRecurringBudget(budget: RecurringBudget) {
        viewModelScope.launch {
            val updatedBudgets = _uiState.value.recurringBudgets.map {
                if (it.id == budget.id) budget else it
            }
            _uiState.update { it.copy(recurringBudgets = updatedBudgets) }
        }
    }

    private fun deleteRecurringBudget(budget: RecurringBudget) {
        viewModelScope.launch {
            val updatedBudgets = _uiState.value.recurringBudgets.filter { it.id != budget.id }
            _uiState.update { it.copy(recurringBudgets = updatedBudgets) }
        }
    }

    // --- REUSABLE STATE UPDATE LOGIC ---
    private fun updateStateWith(categories: List<BudgetCategory>) {
        val totalBudget = categories.sumOf { it.budgetedAmount }
        val totalSpent = categories.sumOf { it.spentAmount }
        val remainingBudget = totalBudget - totalSpent
        val utilizationPercentage = if (totalBudget > 0) (totalSpent / totalBudget) * 100 else 0.0
        val overBudgetCategories = categories.filter { it.isOverBudget }

        _uiState.update {
            it.copy(
                isLoading = false,
                totalBudget = totalBudget,
                totalSpent = totalSpent,
                remainingBudget = remainingBudget,
                budgetCategories = categories,
                budgetUtilizationPercentage = utilizationPercentage,
                overBudgetCategories = overBudgetCategories
            )
        }
    }
}
