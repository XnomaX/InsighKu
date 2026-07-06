package com.example.insightku.core.data.repository

import com.example.insightku.core.data.local.dao.AccountDao
import com.example.insightku.core.data.local.dao.TransactionDao
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
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
    private val firestore: FirebaseFirestore
) {
    // ─── Query ─────────────────────────────────────────────────────────────────

    fun getAllAccounts(): Flow<List<Account>> = accountDao.getAllAccounts()

    fun getAccountsByType(type: String): Flow<List<Account>> = accountDao.getAccountsByType(type)

    fun getMoneyAccounts(): Flow<List<Account>> = accountDao.getMoneyAccounts()

    fun getCreditCardAccounts(): Flow<List<Account>> = accountDao.getCreditCardAccounts()

    fun getTotalNetWorth(): Flow<Double?> = accountDao.getTotalNetWorth()

    fun getTotalAssets(): Flow<Double?> = accountDao.getTotalAssets()

    fun getTotalLiabilities(): Flow<Double?> = accountDao.getTotalLiabilities()

    suspend fun getAccountById(id: String): Account? = accountDao.getAccountById(id)

    suspend fun getDefaultAccount(): Account? = accountDao.getDefaultAccount()

    /** Get transactions for a specific account as a Flow. */
    fun getTransactionsByAccountIdFlow(accountId: String) =
        transactionDao.getTransactionsByAccountIdFlow(accountId)

    /** Get all transactions for a specific account (one-shot). */
    suspend fun getTransactionsByAccountId(accountId: String): List<Transaction> =
        transactionDao.getTransactionsByAccountId(accountId)

    // ─── Write (Room + Firestore) ─────────────────────────────────────────────

    suspend fun insertAccount(account: Account) {
        accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account)
    }

    suspend fun clearAllDefaults() {
        accountDao.clearAllDefaults()
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
        // Step 1: Restore account balances for all transactions in this account
        val transactions = transactionDao.getTransactionsByAccountId(accountId)
        for (tx in transactions) {
            if (tx.accountId.isNotBlank()) {
                val reverseDelta = -TransactionType.balanceDelta(tx.type, tx.amount)
                accountDao.updateBalance(tx.accountId, reverseDelta)
            }
        }

        // Step 2: Delete all transactions in this account from Room
        transactionDao.deleteTransactionsByAccountId(accountId)

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

        // Step 4: Deactivate the account
        accountDao.deactivateAccount(accountId)
    }

    suspend fun deactivateAccount(accountId: String) {
        accountDao.deactivateAccount(accountId)
    }

    // ─── Balance ──────────────────────────────────────────────────────────────

    suspend fun updateBalance(accountId: String, delta: Double) {
        accountDao.updateBalance(accountId, delta)
    }

    suspend fun recalculateAccountBalance(accountId: String) {
        val calculatedBalance = accountDao.calculateBalanceFromTransactions(accountId)
        accountDao.setBalance(accountId, calculatedBalance)
    }

    suspend fun recalculateAllAccountBalances() {
        val accountIds = accountDao.getAllAccountIdsWithTransactions()
        for (accountId in accountIds) {
            recalculateAccountBalance(accountId)
        }
    }
}


