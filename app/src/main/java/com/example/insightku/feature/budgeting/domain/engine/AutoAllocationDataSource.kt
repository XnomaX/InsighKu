package com.example.insightku.feature.budgeting.domain.engine

import com.example.insightku.core.data.model.Category
import com.example.insightku.feature.budgeting.data.model.AutoAllocationRuleEntity
import com.example.insightku.feature.budgeting.data.model.GoalEntity
import kotlinx.coroutines.flow.Flow

/**
 * AutoAllocationDataSource — domain-level interface for data access needed by [AutoAllocationEngine].
 *
 * Abstracts the DAO layer so the engine remains independent of Room.
 * The data-layer implementation delegates to the actual DAOs.
 */
interface AutoAllocationDataSource {

    /** Get all enabled auto-allocation rules. */
    suspend fun getEnabledRules(): List<AutoAllocationRuleEntity>

    /** Get a goal by ID, or null if not found. */
    suspend fun getGoalById(id: String): GoalEntity?

    /** Get total amount contributed to a goal. */
    suspend fun getTotalContributed(goalId: String): Double

    /** Get all categories as a Flow. */
    fun getAllCategories(): Flow<List<Category>>
}
