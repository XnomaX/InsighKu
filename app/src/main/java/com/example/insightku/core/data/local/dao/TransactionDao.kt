package com.example.insightku.core.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY date DESC")
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): Transaction?

    @Query("SELECT SUM(CASE WHEN type IN ('INCOME','TRANSFER_IN','GOAL_WITHDRAWAL','BALANCE_ADJUSTMENT') AND amount > 0 THEN amount WHEN type IN ('EXPENSE','TRANSFER_OUT','GOAL_CONTRIBUTION','AUTO_ALLOCATION') THEN -amount ELSE 0 END) FROM transactions")
    fun getTotalBalance(): Flow<Double>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME'")
    fun getTotalIncome(): Flow<Double>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE'")
    fun getTotalExpenses(): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    // ── Offline-First Sync Methods (Issue 2) ─────────────────────────────────

    /**
     * Ambil semua transaksi yang belum disinkronkan ke Firestore.
     * Dipakai oleh SyncTransactionWorker untuk retry upload saat network tersedia.
     */
    @Query("SELECT * FROM transactions WHERE isSynced = 0 ORDER BY createdAt ASC")
    suspend fun getAllUnsyncedTransactions(): List<Transaction>

    /**
     * Update status sync setelah berhasil upload ke Firestore.
     * Hanya update kolom isSynced, tidak menyentuh data lain.
     */
    @Query("UPDATE transactions SET isSynced = 1 WHERE id = :transactionId")
    suspend fun markAsSynced(transactionId: String)

    @Query("UPDATE transactions SET category = :newCategory WHERE category = :oldCategory")
    suspend fun moveTransactionsByCategory(oldCategory: String, newCategory: String)

    @Query("SELECT * FROM transactions WHERE category = :category")
    suspend fun getTransactionsByCategoryOnce(category: String): List<Transaction>

    // ── Account Balance Sync Queries ─────────────────────────────────────────

    /**
     * Get all transactions for a specific account.
     * Used for recalculating account balances.
     */
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    suspend fun getTransactionsByAccountId(accountId: String): List<Transaction>

    /**
     * Get transaction by ID along with its account ID.
     * Used for balance sync before updates.
     */
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionByIdWithAccount(id: String): Transaction?

    /**
     * Delete all transactions for a specific account.
     * Used when deleting an account — cascades the delete to all its transactions.
     */
    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsByAccountId(accountId: String)

    /**
     * Insert data dari Firestore (remote refresh) tanpa menimpa transaksi lokal
     * yang belum tersinkron (isSynced=0). Penting untuk menghindari data loss
     * saat offline: data lokal yang belum terkirim tidak boleh ditimpa.
     *
     * Strategy: IGNORE — jika id sudah ada (data lokal), skip insert dari remote.
     * Ini aman karena: jika isSynced=true, data sudah sama. Jika isSynced=false,
     * data lokal harus dipertahankan dan dikirim ke Firestore (bukan sebaliknya).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionsFromRemote(transactions: List<Transaction>)

    // ── Unified Ledger Queries ────────────────────────────────────────────────

    /** Get all transactions for a specific account (reactive Flow). */
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsByAccountIdFlow(accountId: String): Flow<List<Transaction>>

    /** Get all transactions related to a goal (contributions, withdrawals, auto-allocations). */
    @Query("""SELECT * FROM transactions WHERE goalId = :goalId
        AND type IN ('GOAL_CONTRIBUTION','GOAL_WITHDRAWAL','AUTO_ALLOCATION')
        ORDER BY date DESC""")
    fun getTransactionsByGoalIdFlow(goalId: String): Flow<List<Transaction>>

    /** Get transfer pair by transferId. */
    @Query("SELECT * FROM transactions WHERE transferId = :transferId ORDER BY date DESC")
    suspend fun getTransactionsByTransferId(transferId: String): List<Transaction>

    /**
     * Delete auto-allocation transaction records linked to a contribution.
     * Used during allocation reversal when the triggering transaction is edited/deleted.
     */
    @Query("DELETE FROM transactions WHERE referenceId = :contributionId AND sourceModule = 'auto_allocation'")
    suspend fun deleteTransactionByReferenceId(contributionId: String)

    /** Delete all goal-related transactions (contributions/withdrawals) when a goal is deleted. */
    @Query("DELETE FROM transactions WHERE goalId = :goalId AND sourceModule IN ('goal', 'auto_allocation')")
    suspend fun deleteTransactionsByGoalId(goalId: String)
}



