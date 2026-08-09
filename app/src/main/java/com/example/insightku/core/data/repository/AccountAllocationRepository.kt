package com.example.insightku.core.data.repository

import com.example.insightku.core.data.local.dao.AccountDao
import com.example.insightku.core.data.local.dao.BudgetAllocationDao
import com.example.insightku.core.data.local.dao.BudgetDao
import com.example.insightku.core.domain.model.AccountAllocation
import com.example.insightku.core.domain.model.BudgetAllocationDetail
import com.example.insightku.core.domain.model.GoalAllocationDetail
import com.example.insightku.feature.planning.goal.data.local.dao.ContributionDao
import com.example.insightku.feature.planning.goal.data.model.GoalStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for account allocation operations.
 *
 * This repository provides a unified view of how an Account's balance is distributed
 * across different financial objectives (goals, budgets, etc.).
 *
 * Key principles:
 * - Account.balance is the single source of truth for money
 * - Allocations are calculated from Contribution records
 * - Available cash = Account.balance - All allocations
 */
@Singleton
class AccountAllocationRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val goalDao: com.example.insightku.feature.planning.goal.data.local.dao.GoalDao,
    private val contributionDao: ContributionDao,
    private val budgetDao: BudgetDao,
    private val budgetAllocationDao: BudgetAllocationDao
) {

    /**
     * Get the current budget period boundaries (monthly).
     * Returns Pair of (periodStart, periodEnd) timestamps.
     */
    private fun getCurrentBudgetPeriod(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // Start of current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val periodStart = calendar.timeInMillis

        // End of current month
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val periodEnd = calendar.timeInMillis

        return Pair(periodStart, periodEnd)
    }

    /**
     * Get budget allocation details for an account.
     */
    private suspend fun getBudgetAllocationDetails(accountId: String): List<BudgetAllocationDetail> {
        val (periodStart, periodEnd) = getCurrentBudgetPeriod()

        val allocations = budgetAllocationDao.getBudgetAllocationsByCategoryFlow(
            accountId, periodStart, periodEnd
        ).first()

        // Load all budgets once instead of N+1 queries
        val allBudgets = budgetDao.getAllBudgets().first()

        return allocations.mapNotNull { allocation ->
            // Find budget for this category in memory
            val budget = allBudgets.firstOrNull {
                it.categoryId == allocation.categoryId &&
                it.isActive &&
                it.startDate <= periodEnd &&
                it.endDate >= periodStart
            }

            budget?.let {
                BudgetAllocationDetail(
                    budgetId = it.id,
                    budgetName = it.name,
                    allocatedAmount = allocation.totalSpent,
                    budgetLimit = it.amount,
                    budgetIcon = allocation.categoryIcon,
                    budgetColor = allocation.categoryColor,
                    usagePercent = if (it.amount > 0) {
                        (allocation.totalSpent / it.amount * 100).coerceIn(0.0, 100.0)
                    } else 0.0,
                    remaining = (it.amount - allocation.totalSpent).coerceAtLeast(0.0),
                    isOverBudget = allocation.totalSpent > it.amount
                )
            }
        }
    }

    /**
     * Get all accounts with their allocation information.
     *
     * Fully Flow-driven: re-emits on any account, contribution, or goal change
     * (goal renames/recolors included). Envelope model — completed goals release
     * their set-aside and drop out of the breakdown automatically.
     *
     * @return Flow of list of AccountAllocation
     */
    fun getAllAccountAllocations(): Flow<List<AccountAllocation>> {
        return combine(
            accountDao.getAllAccounts(),
            contributionDao.getTotalAllocatedFromAccountFlow(""),  // invalidation trigger
            goalDao.getAllGoalsIncludingArchived()
        ) { accounts, _, goals ->
            val goalMap = goals.associateBy { it.id }
            accounts.map { account ->
                // Per-account goal allocations via targeted query (not full table scan)
                val goalAllocations = contributionDao.getGoalAllocationsFromAccount(account.id)
                val goalDetails = goalAllocations.mapNotNull { ga ->
                    val goal = goalMap[ga.goalId] ?: return@mapNotNull null
                    if (ga.total <= 0 || goal.goalStatus == GoalStatus.COMPLETED) return@mapNotNull null
                    GoalAllocationDetail(
                        goalId = ga.goalId,
                        goalName = goal.name,
                        goalIcon = goal.iconName,
                        goalColor = goal.color,
                        goalStatus = goal.status,
                        allocatedAmount = ga.total,
                        targetAmount = goal.targetAmount,
                        progressPercent = if (goal.targetAmount > 0) {
                            (ga.total / goal.targetAmount * 100).coerceIn(0.0, 100.0)
                        } else 0.0
                    )
                }.sortedByDescending { it.allocatedAmount }

                val budgetDetails = getBudgetAllocationDetails(account.id)

                AccountAllocation(
                    account = account,
                    allocatedToGoals = goalDetails.sumOf { it.allocatedAmount },
                    allocatedToBudgets = budgetDetails.sumOf { it.allocatedAmount },
                    goalAllocations = goalDetails,
                    budgetAllocations = budgetDetails
                )
            }
        }.flowOn(Dispatchers.IO)
    }

}
