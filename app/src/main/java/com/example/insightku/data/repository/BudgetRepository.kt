package com.example.insightku.data.repository

import com.example.insightku.data.local.dao.BudgetDao
import com.example.insightku.data.local.dao.RecurringBudgetDao
import com.example.insightku.data.model.Budget
import com.example.insightku.data.model.RecurringBudget
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val recurringBudgetDao: RecurringBudgetDao
) {
    // Budget operations
    fun getAllBudgets(): Flow<List<Budget>> = budgetDao.getAllBudgets()
    
    fun getBudgetsByCategory(categoryId: String): Flow<List<Budget>> = 
        budgetDao.getBudgetsByCategory(categoryId)
    
    fun getCurrentBudgets(): Flow<List<Budget>> = 
        budgetDao.getCurrentBudgets(System.currentTimeMillis())
    
    suspend fun getBudgetById(id: String): Budget? = budgetDao.getBudgetById(id)
    
    suspend fun insertBudget(budget: Budget) = budgetDao.insertBudget(budget)
    
    suspend fun updateBudget(budget: Budget) = budgetDao.updateBudget(budget)
    
    suspend fun deleteBudget(budgetId: String) = budgetDao.deleteBudget(budgetId)
    
    suspend fun deactivateBudget(budgetId: String) = budgetDao.deactivateBudget(budgetId)

    // Recurring budget operations
    fun getAllRecurringBudgets(): Flow<List<RecurringBudget>> = 
        recurringBudgetDao.getAllRecurringBudgets()
    
    suspend fun getDueRecurringBudgets(): List<RecurringBudget> = 
        recurringBudgetDao.getDueRecurringBudgets(System.currentTimeMillis())
    
    suspend fun getRecurringBudgetById(id: String): RecurringBudget? = 
        recurringBudgetDao.getRecurringBudgetById(id)
    
    suspend fun insertRecurringBudget(budget: RecurringBudget) = 
        recurringBudgetDao.insertRecurringBudget(budget)
    
    suspend fun updateRecurringBudget(budget: RecurringBudget) = 
        recurringBudgetDao.updateRecurringBudget(budget)
    
    suspend fun deleteRecurringBudget(budgetId: String) = 
        recurringBudgetDao.deleteRecurringBudget(budgetId)
    
    suspend fun deactivateRecurringBudget(budgetId: String) = 
        recurringBudgetDao.deactivateRecurringBudget(budgetId)
}