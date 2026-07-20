package com.example.insightku.feature.planning.goal.data.repository

import com.example.insightku.core.data.local.dao.AccountDao
import com.example.insightku.core.data.local.dao.CategoryDao
import com.example.insightku.core.data.local.dao.TransactionDao
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.feature.planning.goal.data.local.dao.AutoAllocationRuleDao
import com.example.insightku.feature.planning.goal.data.local.dao.ContributionDao
import com.example.insightku.feature.planning.goal.data.local.dao.GoalDao
import com.example.insightku.feature.planning.goal.data.model.AutoAllocationRuleEntity
import com.example.insightku.feature.planning.goal.data.model.GoalEntity
import com.example.insightku.feature.planning.goal.domain.engine.AutoAllocationDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoAllocationDataSourceImpl @Inject constructor(
    private val autoAllocationRuleDao: AutoAllocationRuleDao,
    private val goalDao: GoalDao,
    private val contributionDao: ContributionDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) : AutoAllocationDataSource {

    override suspend fun getEnabledRules(): List<AutoAllocationRuleEntity> =
        autoAllocationRuleDao.getEnabledRulesSync()

    override suspend fun getGoalById(id: String): GoalEntity? =
        goalDao.getGoalById(id)

    override suspend fun getTotalContributed(goalId: String): Double =
        contributionDao.getTotalContributed(goalId)

    override fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories()

    override suspend fun getTransactionsByDateRange(startTime: Long, endTime: Long): List<Transaction> =
        transactionDao.getTransactionsByDateRange(startTime, endTime).first()

    override suspend fun getCategoryIdByName(categoryName: String): String? =
        categoryDao.getCategoryByName(categoryName)?.id

    override suspend fun getAccountById(id: String): Account? =
        accountDao.getAccountById(id)?.takeIf { it.isActive }

    override suspend fun getAvailableCash(accountId: String): Double {
        val account = accountDao.getAccountById(accountId) ?: return 0.0
        return (account.balance - contributionDao.getSetAsideByAccount(accountId)).coerceAtLeast(0.0)
    }
}
