package com.example.insightku.core.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.insightku.core.data.model.Budget

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY startDate DESC")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND isActive = 1")
    fun getBudgetsByCategory(categoryId: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: String): Budget?

    @Query("SELECT * FROM budgets WHERE endDate >= :currentTime AND startDate <= :currentTime AND isActive = 1")
    fun getCurrentBudgets(currentTime: Long): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<Budget>)

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :budgetId")
    suspend fun deleteBudget(budgetId: String)

    @Query("UPDATE budgets SET isActive = 0 WHERE id = :budgetId")
    suspend fun deactivateBudget(budgetId: String)
}


