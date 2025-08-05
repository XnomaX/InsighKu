package com.example.insightku.data.repository

import com.example.insightku.data.local.dao.TransactionDao
import com.example.insightku.data.local.dao.CategoryDao
import com.example.insightku.data.local.dao.RecurringBudgetDao
import com.example.insightku.data.model.Transaction
import com.example.insightku.data.model.Category
import com.example.insightku.data.model.RecurringBudget
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val recurringBudgetDao: RecurringBudgetDao
) {
    // Transaction operations
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()
    
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByCategory(category)
    
    suspend fun insertTransaction(transaction: Transaction) = 
        transactionDao.insertTransaction(transaction)
    
    suspend fun updateTransaction(transaction: Transaction) = 
        transactionDao.updateTransaction(transaction)
    
    suspend fun deleteTransaction(transactionId: String) = 
        transactionDao.deleteTransaction(transactionId)

    // Category operations
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
    
    suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category)
    
    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)
    
    suspend fun deleteCategory(categoryId: String) = categoryDao.deleteCategory(categoryId)

    // Recurring budget operations
    fun getRecurringBudgets(): Flow<List<RecurringBudget>> = 
        recurringBudgetDao.getAllRecurringBudgets()
    
    suspend fun insertRecurringBudget(budget: RecurringBudget) = 
        recurringBudgetDao.insertRecurringBudget(budget)
    
    suspend fun updateRecurringBudget(budget: RecurringBudget) = 
        recurringBudgetDao.updateRecurringBudget(budget)
    
    suspend fun deleteRecurringBudget(budgetId: String) = 
        recurringBudgetDao.deleteRecurringBudget(budgetId)
}
