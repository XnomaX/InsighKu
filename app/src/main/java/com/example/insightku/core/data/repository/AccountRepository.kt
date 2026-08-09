package com.example.insightku.core.data.repository

import androidx.room.withTransaction
import com.example.insightku.core.data.local.dao.AccountDao
import com.example.insightku.core.data.local.dao.TransactionDao
import com.example.insightku.core.data.local.database.InsightKuDatabase
import com.example.insightku.core.data.model.Account
import com.example.insightku.feature.planning.goal.data.local.dao.GoalAccountDao
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AccountRepository — single source of truth for all account data operations.
 *
 * Wraps AccountDao (Room), TransactionDao (balance recalculation), and Firestore (remote sync).
 * ViewModels should inject this instead of accessing DAOs or Firestore directly.
 */
@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val goalAccountDao: GoalAccountDao,
    private val database: InsightKuDatabase,
    private val firestore: FirebaseFirestore
) {
    // ─── Query ─────────────────────────────────────────────────────────────────

    fun getAllAccounts(): Flow<List<Account>> = accountDao.getAllAccounts()

    fun getTotalNetWorth(): Flow<Double?> = accountDao.getTotalNetWorth()

    fun getTotalAssets(): Flow<Double?> = accountDao.getTotalAssets()

    fun getTotalLiabilities(): Flow<Double?> = accountDao.getTotalLiabilities()

    suspend fun getAccountById(id: String): Account? = accountDao.getAccountById(id)

    // ─── Write (Room + Firestore) ─────────────────────────────────────────────

    suspend fun insertAccount(account: Account) {
        accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account)
    }

    suspend fun setAsDefault(accountId: String) {
        accountDao.clearAllDefaults()
        val account = accountDao.getAccountById(accountId)
        if (account != null) {
            accountDao.updateAccount(account.copy(isDefault = true))
        }
    }

    // ─── Delete (Room + Firestore + balance restore) ──────────────────────────

    /**
     * Deactivate an account and restore all its transaction balances.
     * Also removes account data from Firestore (best-effort).
     */
    suspend fun deleteAccount(accountId: String, userId: String?) {
        // Step 1: Collect transactions for Firestore cleanup
        val transactions = transactionDao.getTransactionsByAccountId(accountId)

        // Step 2: Atomically delete transactions, goal links, and deactivate the account
        database.withTransaction {
            transactionDao.deleteTransactionsByAccountId(accountId)
            // Remove goal↔account links so the deleted account isn't referenced anywhere
            goalAccountDao.unlinkAllGoalsFromAccount(accountId)
            accountDao.deactivateAccount(accountId)
        }

        // Step 3: Also delete from Firestore (best effort)
        if (userId != null) {
            try {
                for (tx in transactions) {
                    firestore.collection("users").document(userId)
                        .collection("transactions").document(tx.id).delete().await()
                }
            } catch (_: Exception) {
                // Firestore offline — Room already deleted
            }
        }
    }

}


