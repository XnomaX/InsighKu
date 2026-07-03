package com.example.insightku.core.data.repository

import com.example.insightku.core.data.local.dao.AccountDao
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.domain.model.AccountAllocation
import com.example.insightku.core.domain.model.BudgetAllocationDetail
import com.example.insightku.core.domain.model.ContributionDetail
import com.example.insightku.core.domain.model.GoalAllocationDetail
import com.example.insightku.feature.budgeting.data.local.dao.ContributionDao
import com.example.insightku.feature.budgeting.data.local.dao.GoalDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    private val goalDao: com.example.insightku.feature.budgeting.data.local.dao.GoalDao,
    private val contributionDao: ContributionDao
) {

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
            val goalEntity = goalDao.getGoalById(allocation.goalId) ?: return@mapNotNull null
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

        // For now, budget allocations are not tracked in this repository
        // They'll be added when the Budget feature is refactored
        val budgetAllocations = emptyList<BudgetAllocationDetail>()

        return AccountAllocation(
            account = account,
            allocatedToGoals = totalAllocatedToGoals,
            allocatedToBudgets = 0.0, // TODO: Add budget tracking
            goalAllocations = goalDetails,
            budgetAllocations = budgetAllocations
        )
    }

    /**
     * Get account allocation as a Flow for reactive updates.
     *
     * @param accountId The account ID
     * @return Flow of AccountAllocation, or empty flow if account not found
     */
    fun getAccountAllocationFlow(accountId: String): Flow<AccountAllocation?> {
        return combine(
            accountDao.getAllAccounts(),
            contributionDao.getTotalAllocatedFromAccountFlow(accountId)
        ) { accounts, totalAllocated ->
            val account = accounts.find { it.id == accountId }
            account?.let { acc ->
                // Fetch goal allocations (suspend operation wrapped in flow)
                val goalAllocList = contributionDao.getGoalAllocationsFromAccount(accountId)
                val goalDetails = goalAllocList.mapNotNull { ga ->
                    val goalEntity = goalDao.getGoalById(ga.goalId) ?: return@mapNotNull null
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
                AccountAllocation(
                    account = acc,
                    allocatedToGoals = totalAllocated,
                    allocatedToBudgets = 0.0,
                    goalAllocations = goalDetails,
                    budgetAllocations = emptyList()
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
        return accountDao.getAllAccounts().map { accounts ->
            accounts.mapNotNull { account ->
                val totalAllocated = contributionDao.getTotalAllocatedFromAccount(account.id)
                val goalAllocList = contributionDao.getGoalAllocationsFromAccount(account.id)
                val goalDetails = goalAllocList.mapNotNull { ga ->
                    val goalEntity = goalDao.getGoalById(ga.goalId) ?: return@mapNotNull null
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
                AccountAllocation(
                    account = account,
                    allocatedToGoals = totalAllocated,
                    allocatedToBudgets = 0.0,
                    goalAllocations = goalDetails,
                    budgetAllocations = emptyList()
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
        val goalEntity = goalDao.getGoalById(goalId) ?: return emptyList()

        // Get all contributions for this goal and group by account
        val contributions = contributionDao.getContributionsByGoal(goalId).first()

        return contributions
            .filter { it.amount > 0 } // Only positive contributions
            .groupBy { it.accountId }
            .mapNotNull { (accountId, accountContributions) ->
                val totalAllocated = accountContributions.sumOf { it.amount }
                val account = accountDao.getAccountById(accountId)
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
