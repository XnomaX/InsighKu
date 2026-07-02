package com.example.insightku.core.domain.model

import com.example.insightku.core.data.model.Account

/**
 * Represents the allocation breakdown of an Account.
 *
 * This model provides a complete view of how an Account's balance is distributed,
 * serving as the single source of truth for allocation calculations.
 *
 * Invariant:
 * currentBalance == availableCash + allocatedToGoals + allocatedToBudgets
 *
 * @param account The parent account
 * @param allocatedToGoals Total amount allocated to all goals (from contributions)
 * @param allocatedToBudgets Total amount allocated to all budgets
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
     * Available cash = Account balance - all allocations
     * This is the money that can be freely spent
     */
    val availableCash: Double
        get() = (account.balance - allocatedToGoals - allocatedToBudgets).coerceAtLeast(0.0)

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
 * @param allocatedAmount Total amount allocated to this goal from this account
 * @param targetAmount The goal's target amount
 * @param progressPercent Progress percentage (0-100)
 */
data class GoalAllocationDetail(
    val goalId: String,
    val goalName: String,
    val goalIcon: String,
    val goalColor: String,
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
}

/**
 * Detailed allocation information for a specific Budget.
 *
 * @param budgetId The budget's ID
 * @param budgetName The budget's name
 * @param allocatedAmount Total amount allocated to this budget from this account
 * @param budgetLimit The budget's limit
 */
data class BudgetAllocationDetail(
    val budgetId: String,
    val budgetName: String,
    val allocatedAmount: Double,
    val budgetLimit: Double
) {
    /**
     * Remaining budget
     */
    val remaining: Double
        get() = (budgetLimit - allocatedAmount).coerceAtLeast(0.0)

    /**
     * Usage percentage (0-100)
     */
    val usagePercent: Double
        get() = if (budgetLimit > 0) {
            (allocatedAmount / budgetLimit * 100).coerceIn(0.0, 100.0)
        } else 0.0

    /**
     * Check if budget is exceeded
     */
    val isExceeded: Boolean
        get() = allocatedAmount > budgetLimit
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
