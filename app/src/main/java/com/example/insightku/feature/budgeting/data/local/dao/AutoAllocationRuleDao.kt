package com.example.insightku.feature.budgeting.data.local.dao

import androidx.room.*
import com.example.insightku.feature.budgeting.data.model.AutoAllocationRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoAllocationRuleDao {

    @Query("SELECT * FROM auto_allocation_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<AutoAllocationRuleEntity>>

    @Query("SELECT * FROM auto_allocation_rules WHERE isEnabled = 1")
    fun getEnabledRules(): Flow<List<AutoAllocationRuleEntity>>

    @Query("SELECT * FROM auto_allocation_rules WHERE isEnabled = 1")
    suspend fun getEnabledRulesSync(): List<AutoAllocationRuleEntity>

    @Query("SELECT * FROM auto_allocation_rules WHERE goalId = :goalId")
    fun getRulesByGoal(goalId: String): Flow<List<AutoAllocationRuleEntity>>

    @Query("SELECT * FROM auto_allocation_rules WHERE goalId = :goalId AND isEnabled = 1")
    fun getEnabledRulesByGoal(goalId: String): Flow<List<AutoAllocationRuleEntity>>

    @Query("SELECT * FROM auto_allocation_rules WHERE id = :id")
    suspend fun getRuleById(id: String): AutoAllocationRuleEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM auto_allocation_rules WHERE goalId = :goalId AND isEnabled = 1)")
    suspend fun hasEnabledRuleForGoal(goalId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AutoAllocationRuleEntity)

    @Update
    suspend fun updateRule(rule: AutoAllocationRuleEntity)

    @Query("UPDATE auto_allocation_rules SET isEnabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setRuleEnabled(id: String, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE auto_allocation_rules SET lastExecutedAt = :timestamp WHERE id = :id")
    suspend fun setLastExecutedAt(id: String, timestamp: Long)

    @Delete
    suspend fun deleteRule(rule: AutoAllocationRuleEntity)

    @Query("DELETE FROM auto_allocation_rules WHERE id = :id")
    suspend fun deleteRuleById(id: String)

    @Query("DELETE FROM auto_allocation_rules WHERE goalId = :goalId")
    suspend fun deleteRulesByGoal(goalId: String)

    @Query("SELECT COUNT(*) FROM auto_allocation_rules WHERE isEnabled = 1")
    suspend fun getEnabledRuleCount(): Int
}
