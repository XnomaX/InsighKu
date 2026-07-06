package com.example.insightku.feature.budgeting.data.repository

import com.example.insightku.core.data.local.dao.CategoryDao
import com.example.insightku.core.data.model.Category
import com.example.insightku.feature.budgeting.data.local.dao.AutoAllocationRuleDao
import com.example.insightku.feature.budgeting.data.local.dao.ContributionDao
import com.example.insightku.feature.budgeting.data.local.dao.GoalDao
import com.example.insightku.feature.budgeting.data.model.AutoAllocationRuleEntity
import com.example.insightku.feature.budgeting.data.model.GoalEntity
import com.example.insightku.feature.budgeting.domain.engine.AutoAllocationDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AutoAllocationDataSourceImpl — data-layer implementation of [AutoAllocationDataSource].
 *
 * Delegates to the actual Room DAOs for persistence.
 */
@Singleton
class AutoAllocationDataSourceImpl @Inject constructor(
    private val autoAllocationRuleDao: AutoAllocationRuleDao,
    private val goalDao: GoalDao,
    private val contributionDao: ContributionDao,
    private val categoryDao: CategoryDao
) : AutoAllocationDataSource {

    override suspend fun getEnabledRules(): List<AutoAllocationRuleEntity> =
        autoAllocationRuleDao.getEnabledRulesSync()

    override suspend fun getGoalById(id: String): GoalEntity? =
        goalDao.getGoalById(id)

    override suspend fun getTotalContributed(goalId: String): Double =
        contributionDao.getTotalContributed(goalId)

    override fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories()
}
