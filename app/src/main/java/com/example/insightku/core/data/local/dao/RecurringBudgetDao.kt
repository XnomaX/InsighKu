package com.example.insightku.core.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.insightku.core.data.model.RecurringBudget

@Dao
interface RecurringBudgetDao {
    @Query("DELETE FROM recurring_budgets WHERE id = 0 OR name = '' OR name IS NULL")
    suspend fun deleteInvalidRecurringBudgets()

    @Query("DELETE FROM recurring_budgets")
    suspend fun deleteAllRecurringBudgets()

    @Query("SELECT COUNT(*) FROM recurring_budgets WHERE isActive = 1")
    suspend fun countActiveRecurringBudgets(): Int

    @Query("SELECT * FROM recurring_budgets WHERE isActive = 1 ORDER BY name ASC")
    fun getAllRecurringBudgets(): Flow<List<RecurringBudget>>

    @Query("SELECT * FROM recurring_budgets WHERE nextDue <= :currentTime AND isActive = 1")
    suspend fun getDueRecurringBudgets(currentTime: Long): List<RecurringBudget>

    @Query("SELECT * FROM recurring_budgets WHERE id = :id")
    suspend fun getRecurringBudgetById(id: String): RecurringBudget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringBudget(budget: RecurringBudget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringBudgets(budgets: List<RecurringBudget>)

    @Update
    suspend fun updateRecurringBudget(budget: RecurringBudget)

    @Delete
    suspend fun deleteRecurringBudget(budget: RecurringBudget)

    @Query("DELETE FROM recurring_budgets WHERE id = :budgetId")
    suspend fun deleteRecurringBudget(budgetId: String)

    @Query("UPDATE recurring_budgets SET isActive = 0 WHERE id = :budgetId")
    suspend fun deactivateRecurringBudget(budgetId: String)
}


