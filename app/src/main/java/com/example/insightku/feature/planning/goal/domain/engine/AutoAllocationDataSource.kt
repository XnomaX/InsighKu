package com.example.insightku.feature.planning.goal.domain.engine

import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.feature.planning.goal.data.model.AutoAllocationRuleEntity
import com.example.insightku.feature.planning.goal.data.model.GoalEntity
import kotlinx.coroutines.flow.Flow

interface AutoAllocationDataSource {
    suspend fun getEnabledRules(): List<AutoAllocationRuleEntity>
    suspend fun getGoalById(id: String): GoalEntity?
    suspend fun getTotalContributed(goalId: String): Double
    fun getAllCategories(): Flow<List<Category>>
    suspend fun getTransactionsByDateRange(startTime: Long, endTime: Long): List<Transaction>
    suspend fun getCategoryIdByName(categoryName: String): String?
}
