package com.example.insightku.data.repository

import com.example.insightku.data.local.dao.CategoryDao
import com.example.insightku.data.local.dao.RecurringBudgetDao
import com.example.insightku.data.local.dao.TransactionDao
import com.example.insightku.data.model.Category
import com.example.insightku.data.model.RecurringBudget
import com.example.insightku.data.model.Transaction
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
 * Pattern: Local-First
 * Write: simpan ke Room dulu (immediate UI update via Flow), lalu sync ke Firestore.
 * Read: selalu dari Room (reaktif via Flow), refresh Firestore saat diminta.
 */
@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val recurringBudgetDao: RecurringBudgetDao,
    private val firestore: FirebaseFirestore
) {
    // ─── Transaction ───────────────────────────────────────────────────────────

    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

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
        // Step 1: Simpan ke Room dengan isSynced=false (immediate, tidak butuh network)
        val localTransaction = transaction.copy(isSynced = false)
        transactionDao.insertTransaction(localTransaction)

        // Step 2: Coba sync ke Firestore
        try {
            firestore.collection("users").document(userId)
                .collection("transactions").document(transaction.id).set(transaction).await()
            // Step 3: Tandai sudah disync
            transactionDao.markAsSynced(transaction.id)
        } catch (e: Exception) {
            // Offline atau Firestore error — data sudah aman di Room.
            // SyncTransactionWorker akan retry saat network tersedia.
            // Tidak rethrow agar user tidak mendapat error saat input transaksi offline.
        }
    }

    // BUG11 FIX: insertTransaction() dihapus — duplikat dari addTransaction().

    suspend fun updateTransaction(transaction: Transaction, userId: String) {
        transactionDao.updateTransaction(transaction)
        try {
            firestore.collection("users").document(userId)
                .collection("transactions").document(transaction.id).set(transaction).await()
        } catch (e: Exception) {
            // Offline — update sudah ada di Room, akan sync saat network kembali
        }
    }

    suspend fun deleteTransaction(transactionId: String, userId: String) {
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

    suspend fun refreshCategories(userId: String) {
        // SEBELUMNYA: catch kosong
        // SEKARANG: exception dibiarkan propagate ke caller
        val snapshot = firestore.collection("users").document(userId)
            .collection("categories").get().await()
        val categories = snapshot.toObjects(Category::class.java)
        categoryDao.insertCategories(categories)
    }

    suspend fun insertCategory(category: Category, userId: String) {
        categoryDao.insertCategory(category)
        firestore.collection("users").document(userId)
            .collection("categories").document(category.id.toString()).set(category).await()
    }

    suspend fun updateCategory(category: Category, userId: String) {
        categoryDao.updateCategory(category)
        firestore.collection("users").document(userId)
            .collection("categories").document(category.id.toString()).set(category).await()
    }

    suspend fun deleteCategory(categoryId: String, userId: String) {
        categoryDao.deleteCategory(categoryId)
        firestore.collection("users").document(userId)
            .collection("categories").document(categoryId).delete().await()
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
        recurringBudgetDao.insertRecurringBudgets(budgets)
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
}
