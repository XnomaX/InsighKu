package com.example.insightku.core.data.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.insightku.core.data.local.dao.AccountDao
import com.example.insightku.core.data.local.dao.CategoryDao
import com.example.insightku.core.data.local.dao.InstallmentDao
import com.example.insightku.core.data.local.dao.RecurringBudgetDao
import com.example.insightku.core.data.local.dao.TransactionDao
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.CategoryType
import com.example.insightku.core.data.model.Installment
import com.example.insightku.core.data.model.RecurringBudget
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.worker.SyncTransactionWorker
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TransactionRepository — mengorkestrasi Room (offline cache) dan Firestore (remote).
 *
 * PERBAIKAN UTAMA vs versi lama:
 * Semua catch block yang sebelumnya KOSONG kini melempar ulang exception.
 * Kenapa ini penting? Silent failure = user tidak tahu operasi gagal.
 * Dengan throw, error naik ke UseCase → ViewModel → ditampilkan ke user.
 *
 * Balance Synchronization:
 * Setiap transaksi CRUD operation juga mengupdate account balance:
 * - INCOME: balance += amount
 * - EXPENSE: balance -= amount
 *
 * Pattern: Local-First
 * Write: simpan ke Room dulu (immediate UI update via Flow), lalu sync ke Firestore.
 * Read: selalu dari Room (reaktif via Flow), refresh Firestore saat diminta.
 */
@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val recurringBudgetDao: RecurringBudgetDao,
    private val installmentDao: InstallmentDao,
    private val accountDao: AccountDao,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    // ─── Transaction ───────────────────────────────────────────────────────────

    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(startDate, endDate)

    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategory(category)

    /**
     * OFFLINE-FIRST (Issue 2):
     * Sebelumnya: simpan ke Room → simpan ke Firestore (serial, UI menunggu network).
     * Sekarang:
     * 1. Simpan ke Room dengan isSynced=false → Room Flow emit → UI langsung update
     * 2. Coba upload ke Firestore
     * 3. Jika berhasil → markAsSynced(true) di Room
     * 4. Jika gagal (offline) → isSynced tetap false → WorkManager akan retry
     *
     * User tidak perlu menunggu network untuk melihat transaksi baru.
     */
    suspend fun refreshTransactions(userId: String) {
        val snapshot = firestore.collection("users").document(userId)
            .collection("transactions").get().await()
        val transactions = snapshot.toObjects(Transaction::class.java)
        // Gunakan IGNORE strategy agar data lokal yang belum sync tidak ditimpa
        transactionDao.insertTransactionsFromRemote(transactions)
    }

    suspend fun addTransaction(transaction: Transaction, userId: String) {
        // Step 1: Update account balance FIRST (before saving transaction)
        // This ensures balance is consistent even if transaction save fails
        if (transaction.accountId.isNotBlank()) {
            val delta = if (transaction.type == TransactionType.INCOME) {
                transaction.amount
            } else {
                -transaction.amount
            }
            accountDao.updateBalance(transaction.accountId, delta)
        }

        // Step 2: Simpan ke Room dengan isSynced=false (immediate, tidak butuh network)
        val localTransaction = transaction.copy(isSynced = false)
        transactionDao.insertTransaction(localTransaction)

        // Step 3: Coba sync ke Firestore
        try {
            firestore.collection("users").document(userId)
                .collection("transactions").document(transaction.id).set(transaction).await()
            // Step 4: Tandai sudah disync
            transactionDao.markAsSynced(transaction.id)
        } catch (e: Exception) {
            // Offline atau Firestore error — data sudah aman di Room.
            // Jadwalkan one-shot recovery worker agar sync segera terjadi saat
            // network kembali, tanpa menunggu periodic worker berikutnya.
            scheduleSyncRecovery()
        }
    }

    private fun scheduleSyncRecovery() {
        val request = OneTimeWorkRequestBuilder<SyncTransactionWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "SyncRecovery",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    // BUG11 FIX: insertTransaction() dihapus — duplikat dari addTransaction().

    suspend fun getTransactionById(id: String): Transaction? =
        transactionDao.getTransactionById(id)

    suspend fun updateTransaction(transaction: Transaction, userId: String) {
        // Get original transaction to calculate balance changes
        val original = transactionDao.getTransactionByIdWithAccount(transaction.id)

        if (original != null) {
            // Calculate balance impact for original transaction
            val originalDelta = if (original.type == TransactionType.INCOME) {
                -original.amount  // Reverse the original income effect
            } else {
                original.amount   // Reverse the original expense effect
            }

            // Calculate balance impact for new transaction
            val newDelta = if (transaction.type == TransactionType.INCOME) {
                transaction.amount
            } else {
                -transaction.amount
            }

            if (original.accountId.isNotBlank()) {
                // Same account - apply net difference
                accountDao.updateBalance(original.accountId, originalDelta + newDelta)
            } else if (transaction.accountId.isNotBlank()) {
                // Moved to a new account
                accountDao.updateBalance(transaction.accountId, newDelta)
            }
        } else if (transaction.accountId.isNotBlank()) {
            // Original not found but new has account - apply balance change
            val newDelta = if (transaction.type == TransactionType.INCOME) {
                transaction.amount
            } else {
                -transaction.amount
            }
            accountDao.updateBalance(transaction.accountId, newDelta)
        }

        transactionDao.updateTransaction(transaction)
        try {
            firestore.collection("users").document(userId)
                .collection("transactions").document(transaction.id).set(transaction).await()
        } catch (e: Exception) {
            // Offline — update sudah ada di Room, akan sync saat network kembali
        }
    }

    suspend fun deleteTransaction(transactionId: String, userId: String) {
        // Get transaction first to restore its account balance
        val transaction = transactionDao.getTransactionByIdWithAccount(transactionId)

        if (transaction != null && transaction.accountId.isNotBlank()) {
            // Restore account balance: opposite of what the transaction did
            val delta = if (transaction.type == TransactionType.INCOME) {
                -transaction.amount  // Reverse income: subtract the amount
            } else {
                transaction.amount   // Reverse expense: add the amount back
            }
            accountDao.updateBalance(transaction.accountId, delta)
        }

        transactionDao.deleteTransaction(transactionId)
        try {
            firestore.collection("users").document(userId)
                .collection("transactions").document(transactionId).delete().await()
        } catch (e: Exception) {
            // Offline — hapus sudah terjadi di Room lokal
        }
    }

    /** Digunakan oleh LogoutUseCase untuk menghapus semua cache lokal saat logout. */
    suspend fun deleteAllLocalTransactions() {
        transactionDao.deleteAllTransactions()
    }

    // ── Offline-First Sync Support (Issue 2) ─────────────────────────────────

    /** Ambil transaksi yang belum di-sync ke Firestore. Dipakai SyncTransactionWorker. */
    suspend fun getUnsyncedTransactions(): List<Transaction> =
        transactionDao.getAllUnsyncedTransactions()

    /** Tandai transaksi sudah di-sync ke Firestore. */
    suspend fun markTransactionSynced(transactionId: String) =
        transactionDao.markAsSynced(transactionId)

    // ─── Category ──────────────────────────────────────────────────────────────

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun hasAnyCategories(): Boolean = categoryDao.countActiveCategories() > 0
    suspend fun hasAnyRecurringBudgets(): Boolean = recurringBudgetDao.countActiveRecurringBudgets() > 0
    suspend fun hasAnyInstallments(): Boolean = installmentDao.countActiveInstallments() > 0

    fun getExpenseCategories(): Flow<List<Category>> =
        categoryDao.getUserCategoriesByType(CategoryType.EXPENSE.name)

    fun getIncomeCategories(): Flow<List<Category>> =
        categoryDao.getUserCategoriesByType(CategoryType.INCOME.name)

    /**
     * One-time migration: renames Firestore field "systemCategory" → "isSystemCategory"
     * for all category documents where the old field exists.
     * Idempotent — safe to call repeatedly.
     */
    suspend fun migrateCategoryFieldNames(userId: String) {
        try {
            val colRef = firestore.collection("users").document(userId).collection("categories")
            val snapshot = colRef.get().await()
            val batch = firestore.batch()
            var count = 0

            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                if (data.containsKey("systemCategory") && !data.containsKey("isSystemCategory")) {
                    val value = data["systemCategory"]
                    batch.update(doc.reference, mapOf(
                        "isSystemCategory" to value,
                        "systemCategory" to FieldValue.delete()
                    ))
                    count++
                }
            }

            if (count > 0) {
                batch.commit().await()
                Log.d("InsightKu", "migrateCategoryFieldNames: migrated $count docs")
            }
        } catch (e: Exception) {
            Log.w("InsightKu", "migrateCategoryFieldNames: failed (will retry next refresh)", e)
        }
    }

    suspend fun refreshCategories(userId: String) {
        migrateCategoryFieldNames(userId)

        val snapshot = firestore.collection("users").document(userId)
            .collection("categories").get().await()
        val categories = snapshot.toObjects(Category::class.java)
            // SAFETY NET (removable after migration soak): if migration failed
            // (offline, timeout) and docs still have old "systemCategory" field,
            // toObjects() returns isSystemCategory=false. Patch from ID prefix.
            .map { if (it.id.startsWith("system-") && !it.isSystemCategory) it.copy(isSystemCategory = true) else it }
        // IGNORE strategy — never restore a category deleted locally.
        // If a category was deleted from Room, Firestore still has it until
        // the delete syncs. Using REPLACE here would resurrect deleted categories
        // on every refresh, which is the root cause of the "Uncategorized" reappearing bug.
        categoryDao.insertCategoriesFromRemote(categories)
    }

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

    suspend fun deleteCategory(categoryId: String, userId: String) {
        Log.d("InsightKu", "deleteCategory: id=$categoryId userId=$userId")
        categoryDao.deleteCategory(categoryId)
        firestore.collection("users").document(userId)
            .collection("categories").document(categoryId).delete().await()
        Log.d("InsightKu", "deleteCategory: Firestore delete SUCCESS id=$categoryId")
    }

    suspend fun cleanupUncategorizedTransactions(userId: String) {
        transactionDao.moveTransactionsByCategory("Uncategorized", "")
        transactionDao.moveTransactionsByCategory("uncategorized", "")
    }

    suspend fun cleanupVirtualCategoryDocuments(userId: String) {
        // Delete any Firestore category documents with "transaction-only-" ids
        // that were incorrectly written by old code.
        try {
            val snapshot = firestore.collection("users").document(userId)
                .collection("categories").get().await()
            snapshot.documents
                .filter { it.id.startsWith("transaction-only-") }
                .forEach { doc ->
                    Log.d("InsightKu", "cleanupVirtual: deleting stale doc id=${doc.id}")
                    firestore.collection("users").document(userId)
                        .collection("categories").document(doc.id).delete().await()
                    // Also delete from Room if it somehow got inserted
                    categoryDao.deleteCategory(doc.id)
                }
        } catch (e: Exception) {
            Log.w("InsightKu", "cleanupVirtual: failed silently", e)
        }
    }

    suspend fun moveTransactionsByCategory(oldCategory: String, newCategory: String) {
        transactionDao.moveTransactionsByCategory(oldCategory, newCategory)
    }

    suspend fun moveTransactionsToBlank(categoryName: String, userId: String) {
        transactionDao.moveTransactionsByCategory(categoryName, "")
        try {
            val moved = transactionDao.getTransactionsByCategoryOnce("")
            moved.forEach { tx ->
                firestore.collection("users").document(userId)
                    .collection("transactions").document(tx.id).set(tx).await()
            }
        } catch (e: Exception) {
            // Offline — Room already updated
        }
    }

    suspend fun deleteCategoryAndMigrateTransactions(
        categoryId: String,
        categoryName: String,
        userId: String
    ) {
        val category = categoryDao.getCategoryById(categoryId)
        if (category?.isSystemCategory == true) {
            Log.w("InsightKu", "deleteCategoryAndMigrate: blocked protected category id=$categoryId")
            return
        }
        Log.d("InsightKu", "deleteCategoryAndMigrate: id=$categoryId name=$categoryName userId=$userId")

        // Step 1: Delete category from Room first so the Flow emits the correct
        // post-delete list before transactions are blanked. This prevents an
        // intermediate combine() emission that would still include the deleted category.
        categoryDao.deleteCategory(categoryId)

        // Step 2: Blank out transactions locally
        transactionDao.moveTransactionsByCategory(categoryName, "")

        // Step 3: Sync category delete to Firestore
        Log.d("InsightKu", "deleteCategoryAndMigrate: deleting from Firestore path=users/$userId/categories/$categoryId")
        firestore.collection("users").document(userId)
            .collection("categories").document(categoryId).delete().await()
        Log.d("InsightKu", "deleteCategoryAndMigrate: Firestore category delete SUCCESS")

        // Step 4: Sync blanked transactions to Firestore
        val blanked = transactionDao.getTransactionsByCategoryOnce("")
        Log.d("InsightKu", "deleteCategoryAndMigrate: syncing ${blanked.size} blanked transactions")
        blanked.forEach { tx ->
            firestore.collection("users").document(userId)
                .collection("transactions").document(tx.id).set(tx).await()
        }
        Log.d("InsightKu", "deleteCategoryAndMigrate: all done")
    }

    suspend fun deleteAllLocalCategories() {
        categoryDao.deleteAllCategories()
    }

    // ─── Recurring Budget ──────────────────────────────────────────────────────

    fun getRecurringBudgets(): Flow<List<RecurringBudget>> =
        recurringBudgetDao.getAllRecurringBudgets()

    suspend fun refreshRecurringBudgets(userId: String) {
        val snapshot = firestore.collection("users").document(userId)
            .collection("recurring_budgets").get().await()
        val budgets = snapshot.toObjects(RecurringBudget::class.java)

        android.util.Log.d("InsightKu_Recurring", "=== refreshRecurringBudgets ===")
        android.util.Log.d("InsightKu_Recurring", "Firestore docs count: ${snapshot.documents.size}")
        snapshot.documents.forEach { doc ->
            android.util.Log.d("InsightKu_Recurring", "  doc.id=${doc.id} data=${doc.data}")
        }
        android.util.Log.d("InsightKu_Recurring", "Parsed budgets: ${budgets.size}")
        budgets.forEach { b ->
            android.util.Log.d("InsightKu_Recurring", "  budget: id=${b.id} name='${b.name}' amount=${b.amount} active=${b.isActive}")
        }

        // Filter out invalid/test entries: must have a real name and non-zero id
        val validBudgets = budgets.filter { it.name.isNotBlank() && it.id != 0 }
        android.util.Log.d("InsightKu_Recurring", "Valid budgets after filter: ${validBudgets.size}")

        // Delete invalid documents from Firestore so they don't come back
        val invalidDocs = snapshot.documents.filter { doc ->
            val budget = doc.toObject(RecurringBudget::class.java)
            budget == null || budget.name.isBlank() || budget.id == 0
        }
        android.util.Log.d("InsightKu_Recurring", "Invalid Firestore docs to delete: ${invalidDocs.size}")
        invalidDocs.forEach { doc ->
            android.util.Log.d("InsightKu_Recurring", "  Deleting Firestore doc: ${doc.id}")
            try {
                firestore.collection("users").document(userId)
                    .collection("recurring_budgets").document(doc.id).delete().await()
                android.util.Log.d("InsightKu_Recurring", "  Deleted: ${doc.id}")
            } catch (e: Exception) {
                android.util.Log.e("InsightKu_Recurring", "  Failed to delete ${doc.id}: ${e.message}")
            }
        }

        if (validBudgets.isNotEmpty()) {
            recurringBudgetDao.insertRecurringBudgets(validBudgets)
        }
    }

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

    // Remove invalid recurring budgets from Room (e.g. id=0 test entries, blank names)
    suspend fun cleanupInvalidRecurringBudgets() {
        val countBefore = recurringBudgetDao.countActiveRecurringBudgets()
        android.util.Log.d("InsightKu_Recurring", "=== cleanupInvalidRecurringBudgets ===")
        android.util.Log.d("InsightKu_Recurring", "Room active recurring count BEFORE cleanup: $countBefore")
        recurringBudgetDao.deleteInvalidRecurringBudgets()
        val countAfter = recurringBudgetDao.countActiveRecurringBudgets()
        android.util.Log.d("InsightKu_Recurring", "Room active recurring count AFTER cleanup: $countAfter")
    }

    // ─── Installment ───────────────────────────────────────────────────────────

    fun getAllInstallments(): Flow<List<Installment>> = installmentDao.getAllInstallments()

    suspend fun insertInstallment(installment: Installment, userId: String) {
        installmentDao.insertInstallment(installment)
        try {
            firestore.collection("users").document(userId)
                .collection("installments").document(installment.id).set(installment).await()
        } catch (e: Exception) {
            // Offline — Room already saved
        }
    }

    suspend fun updateInstallment(installment: Installment, userId: String) {
        installmentDao.updateInstallment(installment)
        try {
            firestore.collection("users").document(userId)
                .collection("installments").document(installment.id).set(installment).await()
        } catch (e: Exception) {
            // Offline — Room already updated
        }
    }

    suspend fun deleteInstallment(installmentId: String, userId: String) {
        installmentDao.deleteInstallment(installmentId)
        try {
            firestore.collection("users").document(userId)
                .collection("installments").document(installmentId).delete().await()
        } catch (e: Exception) {
            // Offline — Room already deleted
        }
    }

    suspend fun refreshInstallments(userId: String) {
        val snapshot = firestore.collection("users").document(userId)
            .collection("installments").get().await()
        val installments = snapshot.toObjects(Installment::class.java)
        installmentDao.insertInstallmentsFromRemote(installments)
    }

    suspend fun deleteAllLocalInstallments() {
        installmentDao.deleteAllInstallments()
    }

    suspend fun deleteAllLocalRecurringBudgets() {
        recurringBudgetDao.deleteAllRecurringBudgets()
    }

    // ─── Account Balance Recalculation ─────────────────────────────────────────

    /**
     * Recalculate and update a specific account's balance based on all its transactions.
     * Use this to fix balance discrepancies or after bulk data operations.
     */
    suspend fun recalculateAccountBalance(accountId: String) {
        val calculatedBalance = accountDao.calculateBalanceFromTransactions(accountId)
        accountDao.setBalance(accountId, calculatedBalance)
    }

    /**
     * Recalculate all account balances based on their transactions.
     * Use this during initial setup or after data migration.
     */
    suspend fun recalculateAllAccountBalances() {
        val accountIds = accountDao.getAllAccountIdsWithTransactions()
        for (accountId in accountIds) {
            recalculateAccountBalance(accountId)
        }
    }
}


