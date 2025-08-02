/*
package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class BudgetingData(
    val budgetCategories: List<BudgetCategory> = emptyList(),
    val recurringPayments: List<RecurringPayment> = emptyList(),
    val budgetSummary: BudgetSummary = BudgetSummary()
)

data class BudgetCategory(
    val id: String,
    val name: String,
    val budgetAmount: Double,
    val spentAmount: Double,
    val alertThreshold: Double = 80.0,
    val color: String,
    val iconName: String,
    val lastMonthSpent: Double = 0.0
) {
    val remaining: Double get() = budgetAmount - spentAmount
    val percentage: Double get() = (spentAmount / budgetAmount) * 100
    val isOverBudget: Boolean get() = spentAmount > budgetAmount
    val isAlert: Boolean get() = percentage >= alertThreshold && !isOverBudget
    val isOnTrack: Boolean get() = percentage < alertThreshold
}

data class RecurringPayment(
    val id: String,
    val name: String,
    val amount: Double,
    val dueDate: String,
    val frequency: PaymentFrequency,
    val categoryId: String,
    val isActive: Boolean = true,
    val color: String,
    val iconName: String
)

enum class PaymentFrequency {
    WEEKLY, MONTHLY, QUARTERLY, YEARLY
}

data class BudgetSummary(
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val remaining: Double = 0.0,
    val percentage: Double = 0.0,
    val overBudgetCategoriesCount: Int = 0,
    val alertCategoriesCount: Int = 0,
    val onTrackCategoriesCount: Int = 0
)

class BudgetingViewModel : ViewModel() {

    private val _budgetingData = MutableStateFlow(BudgetingData())
    val budgetingData: StateFlow<BudgetingData> = _budgetingData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadBudgetingData()
    }

    fun loadBudgetingData() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Simulate API call or database query
                val budgetCategories = listOf(
                    BudgetCategory(
                        id = "1",
                        name = "Food & Drinks",
                        budgetAmount = 800000.0,
                        spentAmount = 650000.0,
                        color = "#F59E0B",
                        iconName = "restaurant",
                        lastMonthSpent = 580000.0
                    ),
                    BudgetCategory(
                        id = "2",
                        name = "Transportation",
                        budgetAmount = 400000.0,
                        spentAmount = 520000.0,
                        color = "#3B82F6",
                        iconName = "directions_car",
                        lastMonthSpent = 380000.0
                    ),
                    BudgetCategory(
                        id = "3",
                        name = "Entertainment",
                        budgetAmount = 300000.0,
                        spentAmount = 180000.0,
                        color = "#8B5CF6",
                        iconName = "sports_esports",
                        lastMonthSpent = 220000.0
                    ),
                    BudgetCategory(
                        id = "4",
                        name = "Shopping",
                        budgetAmount = 600000.0,
                        spentAmount = 480000.0,
                        color = "#EC4899",
                        iconName = "shopping_bag",
                        lastMonthSpent = 450000.0
                    ),
                    BudgetCategory(
                        id = "5",
                        name = "Housing",
                        budgetAmount = 1200000.0,
                        spentAmount = 1200000.0,
                        color = "#EF4444",
                        iconName = "home",
                        lastMonthSpent = 1200000.0
                    ),
                    BudgetCategory(
                        id = "6",
                        name = "Healthcare",
                        budgetAmount = 500000.0,
                        spentAmount = 120000.0,
                        color = "#10B981",
                        iconName = "local_hospital",
                        lastMonthSpent = 80000.0
                    )
                )

                val recurringPayments = listOf(
                    RecurringPayment(
                        id = "1",
                        name = "Netflix Subscription",
                        amount = 159900.0,
                        dueDate = "Jun 28",
                        frequency = PaymentFrequency.MONTHLY,
                        categoryId = "entertainment",
                        color = "#EF4444",
                        iconName = "subscriptions"
                    ),
                    RecurringPayment(
                        id = "2",
                        name = "Rent Payment",
                        amount = 12000000.0,
                        dueDate = "Jul 1",
                        frequency = PaymentFrequency.MONTHLY,
                        categoryId = "housing",
                        color = "#3B82F6",
                        iconName = "home"
                    ),
                    RecurringPayment(
                        id = "3",
                        name = "Gym Membership",
                        amount = 450000.0,
                        dueDate = "Jul 5",
                        frequency = PaymentFrequency.MONTHLY,
                        categoryId = "health",
                        color = "#10B981",
                        iconName = "fitness_center"
                    ),
                    RecurringPayment(
                        id = "4",
                        name = "Spotify Premium",
                        amount = 54900.0,
                        dueDate = "Jul 8",
                        frequency = PaymentFrequency.MONTHLY,
                        categoryId = "entertainment",
                        color = "#22C55E",
                        iconName = "music_note"
                    ),
                    RecurringPayment(
                        id = "5",
                        name = "Phone Bill",
                        amount = 200000.0,
                        dueDate = "Jul 15",
                        frequency = PaymentFrequency.MONTHLY,
                        categoryId = "utilities",
                        color = "#8B5CF6",
                        iconName = "phone"
                    )
                )

                val totalBudget = budgetCategories.sumOf { it.budgetAmount }
                val totalSpent = budgetCategories.sumOf { it.spentAmount }
                val remaining = totalBudget - totalSpent
                val percentage = (totalSpent / totalBudget) * 100

                val overBudgetCount = budgetCategories.count { it.isOverBudget }
                val alertCount = budgetCategories.count { it.isAlert }
                val onTrackCount = budgetCategories.count { it.isOnTrack }

                val budgetSummary = BudgetSummary(
                    totalBudget = totalBudget,
                    totalSpent = totalSpent,
                    remaining = remaining,
                    percentage = percentage,
                    overBudgetCategoriesCount = overBudgetCount,
                    alertCategoriesCount = alertCount,
                    onTrackCategoriesCount = onTrackCount
                )

                _budgetingData.value = BudgetingData(
                    budgetCategories = budgetCategories,
                    recurringPayments = recurringPayments,
                    budgetSummary = budgetSummary
                )

            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addCategory(category: BudgetCategory) {
        viewModelScope.launch {
            // Simulate adding category
            val currentData = _budgetingData.value
            val updatedCategories = currentData.budgetCategories + category

            updateBudgetingData(updatedCategories, currentData.recurringPayments)
        }
    }

    fun updateCategory(category: BudgetCategory) {
        viewModelScope.launch {
            val currentData = _budgetingData.value
            val updatedCategories = currentData.budgetCategories.map {
                if (it.id == category.id) category else it
            }

            updateBudgetingData(updatedCategories, currentData.recurringPayments)
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            val currentData = _budgetingData.value
            val updatedCategories = currentData.budgetCategories.filter { it.id != categoryId }

            updateBudgetingData(updatedCategories, currentData.recurringPayments)
        }
    }

    fun addRecurringPayment(payment: RecurringPayment) {
        viewModelScope.launch {
            val currentData = _budgetingData.value
            val updatedPayments = currentData.recurringPayments + payment

            updateBudgetingData(currentData.budgetCategories, updatedPayments)
        }
    }

    fun updateRecurringPayment(payment: RecurringPayment) {
        viewModelScope.launch {
            val currentData = _budgetingData.value
            val updatedPayments = currentData.recurringPayments.map {
                if (it.id == payment.id) payment else it
            }

            updateBudgetingData(currentData.budgetCategories, updatedPayments)
        }
    }

    fun deleteRecurringPayment(paymentId: String) {
        viewModelScope.launch {
            val currentData = _budgetingData.value
            val updatedPayments = currentData.recurringPayments.filter { it.id != paymentId }

            updateBudgetingData(currentData.budgetCategories, updatedPayments)
        }
    }

    private fun updateBudgetingData(
        categories: List<BudgetCategory>,
        payments: List<RecurringPayment>
    ) {
        val totalBudget = categories.sumOf { it.budgetAmount }
        val totalSpent = categories.sumOf { it.spentAmount }
        val remaining = totalBudget - totalSpent
        val percentage = if (totalBudget > 0) (totalSpent / totalBudget) * 100 else 0.0

        val overBudgetCount = categories.count { it.isOverBudget }
        val alertCount = categories.count { it.isAlert }
        val onTrackCount = categories.count { it.isOnTrack }

        val budgetSummary = BudgetSummary(
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            remaining = remaining,
            percentage = percentage,
            overBudgetCategoriesCount = overBudgetCount,
            alertCategoriesCount = alertCount,
            onTrackCategoriesCount = onTrackCount
        )

        _budgetingData.value = BudgetingData(
            budgetCategories = categories,
            recurringPayments = payments,
            budgetSummary = budgetSummary
        )
    }

    fun refreshData() {
        loadBudgetingData()
    }
}*/
