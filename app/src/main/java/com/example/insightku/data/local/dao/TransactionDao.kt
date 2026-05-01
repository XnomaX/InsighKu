package com.example.insightku.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.insightku.data.model.Transaction
import com.example.insightku.data.model.TransactionType

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

    @Query("SELECT SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END) FROM transactions")
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
}

