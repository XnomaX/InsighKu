package com.example.insightku.core.domain.model

import com.example.insightku.core.data.model.Account

/**
 * Represents the allocation breakdown of an Account (envelope model).
 *
 * `account.balance` is the total pool; goal contributions set money aside
 * without changing the balance. Budget allocations are informational only
 * (budget spending already reduced the balance as expense transactions).
 *
 * Invariant:
 * availableCash == balance − allocatedToGoals (set-aside for non-completed goals)
 *
 * @param account The parent account
 * @param allocatedToGoals Total amount set aside for goals (net contributions)
 * @param allocatedToBudgets Informational: current-month budget spending
 * @param goalAllocations Detailed breakdown of allocations per goal
 * @param budgetAllocations Detailed breakdown of allocations per budget
 */
data class AccountAllocation(
    val account: Account,
    val allocatedToGoals: Double = 0.0,
    val allocatedToBudgets: Double = 0.0,
    val goalAllocations: List<GoalAllocationDetail> = emptyList(),
    val budgetAllocations: List<BudgetAllocationDetail> = emptyList()
) {
    /**
     * Available cash = Account balance − funds set aside in goals.
     * Budget spending is NOT subtracted: it already reduced the balance.
     */
    val availableCash: Double
        get() = (account.balance - allocatedToGoals).coerceAtLeast(0.0)

    /**
     * Total allocated amount across all categories
     */
    val totalAllocated: Double
        get() = allocatedToGoals + allocatedToBudgets

    /**
     * Allocation percentage (0-100)
     */
    val allocationPercent: Double
        get() = if (account.balance > 0) {
            (totalAllocated / account.balance * 100).coerceIn(0.0, 100.0)
        } else 0.0

    /**
     * Check if account has any allocations
     */
    val hasAllocations: Boolean
        get() = allocatedToGoals > 0 || allocatedToBudgets > 0

    /**
     * Check if account has available cash
     */
    val hasAvailableCash: Boolean
        get() = availableCash > 0

    /**
     * Check if allocations exceed balance (shouldn't happen, but defensive check)
     */
    val isOverAllocated: Boolean
        get() = totalAllocated > account.balance

    /**
     * Get allocation summary as a map for display
     */
    fun getAllocationSummary(): Map<String, Double> = buildMap {
        if (availableCash > 0) {
            put("Available Cash", availableCash)
        }
        if (allocatedToGoals > 0) {
            put("Goals", allocatedToGoals)
        }
        if (allocatedToBudgets > 0) {
            put("Budgets", allocatedToBudgets)
        }
    }
}

/**
 * Detailed allocation information for a specific Goal.
 *
 * @param goalId The goal's ID
 * @param goalName The goal's name
 * @param goalIcon The goal's icon name
 * @param goalColor The goal's color
 * @param goalStatus The goal's status ("active", "paused", "completed", "archived")
 * @param allocatedAmount Total amount allocated to this goal from this account
 * @param targetAmount The goal's target amount
 * @param progressPercent Progress percentage (0-100)
 */
data class GoalAllocationDetail(
    val goalId: String,
    val goalName: String,
    val goalIcon: String,
    val goalColor: String,
    val goalStatus: String = "active",
    val allocatedAmount: Double,
    val targetAmount: Double,
    val progressPercent: Double
) {
    /**
     * Remaining amount to reach target
     */
    val remainingAmount: Double
        get() = (targetAmount - allocatedAmount).coerceAtLeast(0.0)

    /**
     * Check if goal is fully funded from this account
     */
    val isFullyFunded: Boolean
        get() = allocatedAmount >= targetAmount

    /**
     * Allocation percentage from this account toward goal target
     */
    val allocationPercentOfTarget: Double
        get() = if (targetAmount > 0) {
            (allocatedAmount / targetAmount * 100).coerceIn(0.0, 100.0)
        } else 0.0

    /**
     * Progress as a fraction (0.0 to 1.0) for UI components
     */
    val progressFraction: Float
        get() = (progressPercent / 100.0).toFloat().coerceIn(0f, 1f)
}

/**
 * Detailed allocation information for a specific Budget.
 *
 * @param budgetId The budget's ID
 * @param budgetName The budget's name
 * @param allocatedAmount Total amount allocated to this budget from this account
 * @param budgetLimit The budget's limit
 * @param budgetIcon The budget/category icon
 * @param budgetColor The budget/category color
 * @param usagePercent Usage percentage (0-100)
 * @param remaining Remaining budget amount
 * @param isOverBudget Whether spending exceeds the budget limit
 */
data class BudgetAllocationDetail(
    val budgetId: String,
    val budgetName: String,
    val allocatedAmount: Double,
    val budgetLimit: Double,
    val budgetIcon: String? = null,
    val budgetColor: String? = null,
    val usagePercent: Double = 0.0,
    val remaining: Double = 0.0,
    val isOverBudget: Boolean = false
) {
    /**
     * Remaining budget
     */
    val remainingAmount: Double
        get() = (budgetLimit - allocatedAmount).coerceAtLeast(0.0)

    /**
     * Usage percentage (0-100)
     */
    val usagePercentCalculated: Double
        get() = if (budgetLimit > 0) {
            (allocatedAmount / budgetLimit * 100).coerceIn(0.0, 100.0)
        } else 0.0

    /**
     * Check if budget is exceeded
     */
    val isExceeded: Boolean
        get() = allocatedAmount > budgetLimit

    /**
     * Progress as a fraction (0.0 to 1.0) for UI components
     */
    val progressFraction: Float
        get() {
            val percent = if (usagePercent > 0) usagePercent else usagePercentCalculated
            return (percent / 100.0).toFloat().coerceIn(0f, 1f)
        }
}

/**
 * Contribution detail for history/reporting.
 *
 * @param contributionId Unique contribution ID
 * @param goalId The goal the contribution was made to
 * @param goalName Name of the goal
 * @param amount Contribution amount
 * @param date Date of contribution
 * @param type Type of contribution (manual, auto_allocation, withdrawal)
 */
data class ContributionDetail(
    val contributionId: String,
    val goalId: String,
    val goalName: String,
    val goalColor: String,
    val amount: Double,
    val date: Long,
    val type: String,
    val notes: String = ""
) {
    val isWithdrawal: Boolean
        get() = type == "withdrawal" || amount < 0

    val displayAmount: Double
        get() = if (isWithdrawal) -amount else amount
}
