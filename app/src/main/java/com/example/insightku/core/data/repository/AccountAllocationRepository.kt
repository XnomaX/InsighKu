package com.example.insightku.core.data.repository

import com.example.insightku.core.data.local.dao.AccountDao
import com.example.insightku.core.data.local.dao.BudgetAllocationDao
import com.example.insightku.core.data.local.dao.BudgetDao
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.domain.model.AccountAllocation
import com.example.insightku.core.domain.model.BudgetAllocationDetail
import com.example.insightku.core.domain.model.ContributionDetail
import com.example.insightku.core.domain.model.GoalAllocationDetail
import com.example.insightku.feature.planning.goal.data.local.dao.ContributionDao
import com.example.insightku.feature.planning.goal.data.local.dao.GoalDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

        return allocations.mapNotNull { allocation ->
            // Get the budget for this category
            val budgets = budgetDao.getBudgetsByCategory(allocation.categoryId).first()
            val budget = budgets.firstOrNull { it.isActive && it.startDate <= periodEnd && it.endDate >= periodStart }

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
     * Get complete allocation information for an account.
     *
     * @param accountId The account ID
     * @return AccountAllocation with full breakdown, or null if account not found
     */
    suspend fun getAccountAllocation(accountId: String): AccountAllocation? {
        val account = accountDao.getAccountById(accountId) ?: return null

        // Get total allocated to goals
        val totalAllocatedToGoals = contributionDao.getTotalAllocatedFromAccount(accountId)

        // Get per-goal allocation details
        val goalAllocations = contributionDao.getGoalAllocationsFromAccount(accountId)
        val goalDetails = goalAllocations.mapNotNull { allocation ->
            val goalEntity = goalDao.getGoalByIdActive(allocation.goalId) ?: return@mapNotNull null
            GoalAllocationDetail(
                goalId = allocation.goalId,
                goalName = goalEntity.name,
                goalIcon = goalEntity.iconName,
                goalColor = goalEntity.color,
                allocatedAmount = allocation.total,
                targetAmount = goalEntity.targetAmount,
                progressPercent = if (goalEntity.targetAmount > 0) {
                    (allocation.total / goalEntity.targetAmount * 100).coerceIn(0.0, 100.0)
                } else 0.0
            )
        }

        // Get budget allocation details
        val budgetDetails = getBudgetAllocationDetails(accountId)
        val totalAllocatedToBudgets = budgetDetails.sumOf { it.allocatedAmount }

        return AccountAllocation(
            account = account,
            allocatedToGoals = totalAllocatedToGoals,
            allocatedToBudgets = totalAllocatedToBudgets,
            goalAllocations = goalDetails,
            budgetAllocations = budgetDetails
        )
    }

    /**
     * Get account allocation as a Flow for reactive updates.
     *
     * @param accountId The account ID
     * @return Flow of AccountAllocation, or empty flow if account not found
     */
    fun getAccountAllocationFlow(accountId: String): Flow<AccountAllocation?> {
        val (periodStart, periodEnd) = getCurrentBudgetPeriod()

        return combine(
            accountDao.getAllAccounts(),
            contributionDao.getTotalAllocatedFromAccountFlow(accountId),
            budgetAllocationDao.getTotalBudgetSpentFromAccountFlow(accountId, periodStart, periodEnd)
        ) { accounts, totalAllocatedToGoals, totalAllocatedToBudgets ->
            val account = accounts.find { it.id == accountId }
            account?.let { acc ->
                // Fetch goal allocations (suspend operation wrapped in flow)
                val goalAllocList = contributionDao.getGoalAllocationsFromAccount(accountId)
                val goalDetails = goalAllocList.mapNotNull { ga ->
                    val goalEntity = goalDao.getGoalByIdActive(ga.goalId) ?: return@mapNotNull null
                    GoalAllocationDetail(
                        goalId = ga.goalId,
                        goalName = goalEntity.name,
                        goalIcon = goalEntity.iconName,
                        goalColor = goalEntity.color,
                        allocatedAmount = ga.total,
                        targetAmount = goalEntity.targetAmount,
                        progressPercent = if (goalEntity.targetAmount > 0) {
                            (ga.total / goalEntity.targetAmount * 100).coerceIn(0.0, 100.0)
                        } else 0.0
                    )
                }

                // Get budget allocations
                val budgetAllocList = budgetAllocationDao.getBudgetAllocationsByCategoryFlow(
                    accountId, periodStart, periodEnd
                ).first()
                val budgetDetails = budgetAllocList.mapNotNull { allocation ->
                    val budgets = budgetDao.getBudgetsByCategory(allocation.categoryId).first()
                    val budget = budgets.firstOrNull { it.isActive && it.startDate <= periodEnd && it.endDate >= periodStart }
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

                AccountAllocation(
                    account = acc,
                    allocatedToGoals = totalAllocatedToGoals,
                    allocatedToBudgets = totalAllocatedToBudgets,
                    goalAllocations = goalDetails,
                    budgetAllocations = budgetDetails
                )
            }
        }
    }

    /**
     * Get all accounts with their allocation information.
     *
     * @return Flow of list of AccountAllocation
     */
    fun getAllAccountAllocations(): Flow<List<AccountAllocation>> {
        val (periodStart, periodEnd) = getCurrentBudgetPeriod()

        return combine(
            accountDao.getAllAccounts(),
            contributionDao.getTotalAllocatedFromAccountFlow("")
        ) { accounts, _ ->
            accounts.mapNotNull { account ->
                val totalAllocatedToGoals = contributionDao.getTotalAllocatedFromAccount(account.id)
                val goalAllocList = contributionDao.getGoalAllocationsFromAccount(account.id)
                val goalDetails = goalAllocList.mapNotNull { ga ->
                    val goalEntity = goalDao.getGoalByIdActive(ga.goalId) ?: return@mapNotNull null
                    GoalAllocationDetail(
                        goalId = ga.goalId,
                        goalName = goalEntity.name,
                        goalIcon = goalEntity.iconName,
                        goalColor = goalEntity.color,
                        allocatedAmount = ga.total,
                        targetAmount = goalEntity.targetAmount,
                        progressPercent = if (goalEntity.targetAmount > 0) {
                            (ga.total / goalEntity.targetAmount * 100).coerceIn(0.0, 100.0)
                        } else 0.0
                    )
                }

                // Get budget allocations
                val budgetAllocList = budgetAllocationDao.getBudgetAllocationsByCategoryFlow(
                    account.id, periodStart, periodEnd
                ).first()
                val budgetDetails = budgetAllocList.mapNotNull { allocation ->
                    val budgets = budgetDao.getBudgetsByCategory(allocation.categoryId).first()
                    val budget = budgets.firstOrNull { it.isActive && it.startDate <= periodEnd && it.endDate >= periodStart }
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
                val totalAllocatedToBudgets = budgetDetails.sumOf { it.allocatedAmount }

                AccountAllocation(
                    account = account,
                    allocatedToGoals = totalAllocatedToGoals,
                    allocatedToBudgets = totalAllocatedToBudgets,
                    goalAllocations = goalDetails,
                    budgetAllocations = budgetDetails
                )
            }
        }
    }

    /**
     * Get contribution history for an account.
     *
     * @param accountId The account ID
     * @param limit Maximum number of contributions to return
     * @return List of ContributionDetail sorted by date (newest first)
     */
    suspend fun getAccountContributionHistory(
        accountId: String,
        limit: Int = 50
    ): List<ContributionDetail> {
        val contributions = contributionDao.getContributionsFromAccount(accountId).first()
        return contributions.take(limit).mapNotNull { entity ->
            val goalEntity = goalDao.getGoalById(entity.goalId)
            ContributionDetail(
                contributionId = entity.id,
                goalId = entity.goalId,
                goalName = goalEntity?.name ?: "Unknown Goal",
                goalColor = goalEntity?.color ?: "#7C4DFF",
                amount = entity.amount,
                date = entity.createdAt,
                type = entity.type,
                notes = entity.notes
            )
        }
    }

    /**
     * Get contribution history for an account as a Flow.
     */
    fun getAccountContributionHistoryFlow(accountId: String): Flow<List<ContributionDetail>> {
        return contributionDao.getContributionsFromAccount(accountId).map { entities ->
            entities.mapNotNull { entity ->
                val goalEntity = goalDao.getGoalById(entity.goalId)
                ContributionDetail(
                    contributionId = entity.id,
                    goalId = entity.goalId,
                    goalName = goalEntity?.name ?: "Unknown Goal",
                    goalColor = goalEntity?.color ?: "#7C4DFF",
                    amount = entity.amount,
                    date = entity.createdAt,
                    type = entity.type,
                    notes = entity.notes
                )
            }
        }
    }

    /**
     * Get total available cash across all accounts.
     */
    fun getTotalAvailableCash(): Flow<Double> {
        return getAllAccountAllocations().map { allocations ->
            allocations.sumOf { it.availableCash }
        }
    }

    /**
     * Get total allocated to goals across all accounts.
     */
    fun getTotalAllocatedToGoals(): Flow<Double> {
        return getAllAccountAllocations().map { allocations ->
            allocations.sumOf { it.allocatedToGoals }
        }
    }

    /**
     * Get detailed allocation breakdown for a specific goal.
     *
     * @param goalId The goal ID
     * @return List of per-account allocations for this goal
     */
    suspend fun getGoalAllocationDetails(goalId: String): List<GoalAllocationDetail> {
        val goalEntity = goalDao.getGoalByIdActive(goalId) ?: return emptyList()

        // Get all contributions for this goal and group by account
        val contributions = contributionDao.getContributionsByGoal(goalId).first()

        return contributions
            .filter { it.amount > 0 } // Only positive contributions
            .groupBy { it.accountId }
            .mapNotNull { (accountId, accountContributions) ->
                val totalAllocated = accountContributions.sumOf { it.amount }
                GoalAllocationDetail(
                    goalId = goalId,
                    goalName = goalEntity.name,
                    goalIcon = goalEntity.iconName,
                    goalColor = goalEntity.color,
                    allocatedAmount = totalAllocated,
                    targetAmount = goalEntity.targetAmount,
                    progressPercent = if (goalEntity.targetAmount > 0) {
                        (totalAllocated / goalEntity.targetAmount * 100).coerceIn(0.0, 100.0)
                    } else 0.0
                )
            }
    }
}
