package com.example.insightku.core.data.repository

import android.util.Log
import com.example.insightku.core.data.local.dao.CategoryDao
import com.example.insightku.core.data.local.dao.TransactionDao
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.CategoryType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CategoryRepository — single source of truth for all category operations.
 *
 * Handles Room (offline cache) + Firestore (remote sync) for categories.
 * Extracted from TransactionRepository to follow single-responsibility principle.
 */
@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val firestore: FirebaseFirestore
) {
    // ─── Query ─────────────────────────────────────────────────────────────────

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun hasAnyCategories(): Boolean = categoryDao.countActiveCategories() > 0

    fun getExpenseCategories(): Flow<List<Category>> =
        categoryDao.getUserCategoriesByType(CategoryType.EXPENSE.name)

    fun getIncomeCategories(): Flow<List<Category>> =
        categoryDao.getUserCategoriesByType(CategoryType.INCOME.name)

    // ─── Write (Room + Firestore) ─────────────────────────────────────────────

    suspend fun insertCategory(category: Category, userId: String) {
        Log.d("InsightKu", "insertCategory: id=${category.id} name=${category.name} userId=$userId")
        categoryDao.insertCategory(category)
        firestore.collection("users").document(userId)
            .collection("categories").document(category.id).set(category).await()
        Log.d("InsightKu", "insertCategory: Firestore write SUCCESS id=${category.id}")
    }

    suspend fun updateCategory(category: Category, userId: String) {
        Log.d("InsightKu", "updateCategory: id=${category.id} name=${category.name} userId=$userId")
        categoryDao.updateCategory(category)
        firestore.collection("users").document(userId)
            .collection("categories").document(category.id).set(category).await()
        Log.d("InsightKu", "updateCategory: Firestore write SUCCESS id=${category.id}")
    }

    // ─── Firestore Sync ───────────────────────────────────────────────────────

    suspend fun refreshCategories(userId: String) {
        val snapshot = firestore.collection("users").document(userId)
            .collection("categories").get().await()
        val categories = snapshot.toObjects(Category::class.java)
        // IGNORE strategy — never restore a category deleted locally
        categoryDao.insertCategoriesFromRemote(categories)
    }

    // ─── Category Deletion with Transaction Migration ─────────────────────────

    /**
     * Delete a category and move all its transactions to blank (uncategorized).
     * Handles Room deletion, Firestore deletion, and transaction migration in one atomic operation.
     */
    suspend fun deleteCategoryAndMigrateTransactions(
        categoryId: String,
        categoryName: String,
        userId: String
    ) {
        // 1. Move all transactions from this category to blank
        transactionDao.moveTransactionsByCategory(categoryName, "")
        // 2. Delete category from Room
        categoryDao.deleteCategory(categoryId)
        // 3. Sync both changes to Firestore
        try {
            // Sync moved transactions
            val moved = transactionDao.getTransactionsByCategoryOnce("")
            moved.forEach { tx ->
                firestore.collection("users").document(userId)
                    .collection("transactions").document(tx.id).set(tx).await()
            }
            // Delete category from Firestore
            firestore.collection("users").document(userId)
                .collection("categories").document(categoryId).delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline — Room already updated
        }
    }

    // ─── Cleanup ──────────────────────────────────────────────────────────────

    suspend fun cleanupVirtualCategoryDocuments(userId: String) {
        try {
            val snapshot = firestore.collection("users").document(userId)
                .collection("categories").get().await()
            snapshot.documents
                .filter { it.id.startsWith("transaction-only-") }
                .forEach { doc ->
                    Log.d("InsightKu", "cleanupVirtual: deleting stale doc id=${doc.id}")
                    firestore.collection("users").document(userId)
                        .collection("categories").document(doc.id).delete().await()
                    categoryDao.deleteCategory(doc.id)
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("InsightKu", "cleanupVirtual: failed silently", e)
        }
    }

    suspend fun deleteAllLocalCategories() {
        categoryDao.deleteAllCategories()
    }
}
