package com.example.insightku.core.data.repository

import android.util.Log
import com.example.insightku.core.data.local.dao.RecurringBudgetDao
import com.example.insightku.core.data.model.RecurringBudget
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RecurringBudgetRepository — single source of truth for recurring budget operations.
 *
 * Handles Room (offline cache) + Firestore (remote sync) for recurring budgets.
 * Extracted from TransactionRepository to follow single-responsibility principle.
 */
@Singleton
class RecurringBudgetRepository @Inject constructor(
    private val recurringBudgetDao: RecurringBudgetDao,
    private val firestore: FirebaseFirestore
) {
    // ─── Query ─────────────────────────────────────────────────────────────────

    fun getAllRecurringBudgets(): Flow<List<RecurringBudget>> =
        recurringBudgetDao.getAllRecurringBudgets()

    suspend fun hasAnyRecurringBudgets(): Boolean = recurringBudgetDao.countActiveRecurringBudgets() > 0

    suspend fun getDueRecurringBudgets(currentTime: Long): List<RecurringBudget> =
        recurringBudgetDao.getDueRecurringBudgets(currentTime)

    // ─── Write (Room + Firestore) ─────────────────────────────────────────────

    suspend fun insertRecurringBudget(budget: RecurringBudget, userId: String) {
        recurringBudgetDao.insertRecurringBudget(budget)
        firestore.collection("users").document(userId)
            .collection("recurring_budgets").document(budget.id.toString()).set(budget).await()
    }

    suspend fun updateRecurringBudget(budget: RecurringBudget, userId: String) {
        recurringBudgetDao.updateRecurringBudget(budget)
        firestore.collection("users").document(userId)
            .collection("recurring_budgets").document(budget.id.toString()).set(budget).await()
    }

    suspend fun deleteRecurringBudget(budgetId: String, userId: String) {
        recurringBudgetDao.deleteRecurringBudget(budgetId)
        firestore.collection("users").document(userId)
            .collection("recurring_budgets").document(budgetId).delete().await()
    }

    // ─── Firestore Sync ───────────────────────────────────────────────────────

    suspend fun refreshRecurringBudgets(userId: String) {
        val snapshot = firestore.collection("users").document(userId)
            .collection("recurring_budgets").get().await()
        val budgets = snapshot.toObjects(RecurringBudget::class.java)

        Log.d("InsightKu_Recurring", "=== refreshRecurringBudgets ===")
        Log.d("InsightKu_Recurring", "Firestore docs count: ${snapshot.documents.size}")

        // Filter out invalid/test entries: must have a real name and non-zero id
        val validBudgets = budgets.filter { it.name.isNotBlank() && it.id != 0 }

        // Delete invalid documents from Firestore so they don't come back
        val invalidDocs = snapshot.documents.filter { doc ->
            val budget = doc.toObject(RecurringBudget::class.java)
            budget == null || budget.name.isBlank() || budget.id == 0
        }
        invalidDocs.forEach { doc ->
            try {
                firestore.collection("users").document(userId)
                    .collection("recurring_budgets").document(doc.id).delete().await()
            } catch (e: Exception) {
                Log.e("InsightKu_Recurring", "Failed to delete invalid doc ${doc.id}: ${e.message}")
            }
        }

        if (validBudgets.isNotEmpty()) {
            recurringBudgetDao.insertRecurringBudgets(validBudgets)
        }
    }

    // ─── Cleanup ──────────────────────────────────────────────────────────────

    suspend fun cleanupInvalidRecurringBudgets() {
        recurringBudgetDao.deleteInvalidRecurringBudgets()
    }

    suspend fun deleteAllLocalRecurringBudgets() {
        recurringBudgetDao.deleteAllRecurringBudgets()
    }
}
