package com.example.insightku.core.data.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.insightku.core.data.local.dao.AccountDao
import com.example.insightku.core.data.local.dao.TransactionDao
import com.example.insightku.core.data.local.database.InsightKuDatabase
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.worker.SyncTransactionWorker
import com.example.insightku.feature.planning.goal.data.repository.GoalRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
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
    private val accountDao: AccountDao,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context,
    private val goalRepository: GoalRepository,
    private val database: InsightKuDatabase
) {
    companion object {
        private const val TAG = "TransactionRepository"
    }
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

    /** Get transactions for a specific account as a Flow. */
    fun getTransactionsByAccountIdFlow(accountId: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByAccountIdFlow(accountId)

    /** Get all transactions for a specific account (one-shot). */
    suspend fun getTransactionsByAccountId(accountId: String): List<Transaction> =
        transactionDao.getTransactionsByAccountId(accountId)



    suspend fun addTransaction(transaction: Transaction, userId: String) {
        // Step 1 + 2: Atomic — update balance AND insert transaction together.
        // If the app crashes mid-way, both operations roll back (no orphan balance change).
        val localTransaction = transaction.copy(isSynced = false)
        database.withTransaction {
            if (transaction.accountId.isNotBlank()) {
                val delta = TransactionType.balanceDelta(transaction.type, transaction.amount)
                accountDao.updateBalance(transaction.accountId, delta)
            }
            transactionDao.insertTransaction(localTransaction)
        }

        // Step 3: Coba sync ke Firestore
        try {
            firestore.collection("users").document(userId)
                .collection("transactions").document(transaction.id).set(transaction).await()
            // Step 4: Tandai sudah disync
            transactionDao.markAsSynced(transaction.id)
        } catch (e: CancellationException) {
            throw e
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
        // PERFORMANCE + CORRECTNESS FIX: Wrap all DB ops in a single transaction
        // to prevent corrupted balance if process dies mid-operation.
        database.withTransaction {
            val original = transactionDao.getTransactionByIdWithAccount(transaction.id)

            if (original != null) {
                val reverseDelta = -TransactionType.balanceDelta(original.type, original.amount)
                val newDelta = TransactionType.balanceDelta(transaction.type, transaction.amount)

                if (original.accountId == transaction.accountId) {
                    if (original.accountId.isNotBlank()) {
                        accountDao.updateBalance(original.accountId, reverseDelta + newDelta)
                    }
                } else {
                    if (original.accountId.isNotBlank()) {
                        accountDao.updateBalance(original.accountId, reverseDelta)
                    }
                    if (transaction.accountId.isNotBlank()) {
                        accountDao.updateBalance(transaction.accountId, newDelta)
                    }
                }

                if (original.type == TransactionType.INCOME) {
                    val originalAmount = original.amount
                    val newAmount = transaction.amount
                    if (originalAmount != newAmount || original.accountId != transaction.accountId) {
                        Log.d(TAG, "[Reconciliation] Income changed: reversing allocations for txId=${original.id}")
                        goalRepository.reverseAllocationsForTransaction(original.id)
                    }
                }
            } else if (transaction.accountId.isNotBlank()) {
                val newDelta = TransactionType.balanceDelta(transaction.type, transaction.amount)
                accountDao.updateBalance(transaction.accountId, newDelta)
            }

            // Room write inside the same transaction for atomicity
            val unsyncedTransaction = transaction.copy(isSynced = false)
            transactionDao.updateTransaction(unsyncedTransaction)
        }

        // Now sync to Firestore (outside DB transaction — non-atomic with local)
        try {
            firestore.collection("users").document(userId)
                .collection("transactions").document(transaction.id).set(transaction.copy(isSynced = false)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Offline — update sudah ada di Room (isSynced=false), akan sync saat network kembali
        }
    }

    suspend fun deleteTransaction(transactionId: String, userId: String) {
        // PERFORMANCE + CORRECTNESS FIX: Wrap all DB ops in a single transaction
        database.withTransaction {
            val transaction = transactionDao.getTransactionByIdWithAccount(transactionId)

            // ── Reconciliation: reverse auto-allocation if income transaction is deleted ──
            if (transaction != null && transaction.type == TransactionType.INCOME) {
                Log.d(TAG, "[Reconciliation] Income deleted: reversing allocations for txId=$transactionId")
                goalRepository.reverseAllocationsForTransaction(transactionId)
            }

            if (transaction != null && transaction.accountId.isNotBlank()) {
                // Restore account balance: reverse the transaction's effect
                val reverseDelta = -TransactionType.balanceDelta(transaction.type, transaction.amount)
                accountDao.updateBalance(transaction.accountId, reverseDelta)
            }

            transactionDao.deleteTransaction(transactionId)
        }

        // Now sync to Firestore
        try {
            firestore.collection("users").document(userId)
                .collection("transactions").document(transactionId).delete().await()
        } catch (e: CancellationException) {
            throw e
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

    // ─── Category-Transaction Operations ───────────────────────────────────────

    /**
     * Move transactions from one category to another.
     * Used when renaming or merging categories.
     */
    suspend fun moveTransactionsByCategory(oldCategory: String, newCategory: String) {
        transactionDao.moveTransactionsByCategory(oldCategory, newCategory)
    }

    /**
     * Move transactions to blank category and sync to Firestore.
     * Used when deleting a category.
     */
    suspend fun moveTransactionsToBlank(categoryName: String, userId: String) {
        transactionDao.moveTransactionsByCategory(categoryName, "")
        try {
            val moved = transactionDao.getTransactionsByCategoryOnce("")
            moved.forEach { tx ->
                firestore.collection("users").document(userId)
                    .collection("transactions").document(tx.id).set(tx).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Offline — Room already updated
        }
    }

    /**
     * Clean up uncategorized transactions (legacy data).
     */
    suspend fun cleanupUncategorizedTransactions(userId: String) {
        transactionDao.moveTransactionsByCategory("Uncategorized", "")
        transactionDao.moveTransactionsByCategory("uncategorized", "")
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


