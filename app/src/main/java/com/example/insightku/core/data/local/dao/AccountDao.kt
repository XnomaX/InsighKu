package com.example.insightku.core.data.local.dao

import androidx.room.*
import com.example.insightku.core.data.model.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 AND accountType = :type ORDER BY name ASC")
    fun getAccountsByType(type: String): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 AND accountType IN ('CASH', 'BANK_ACCOUNT', 'E_WALLET') ORDER BY name ASC")
    fun getMoneyAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 AND accountType = 'CREDIT_CARD' ORDER BY name ASC")
    fun getCreditCardAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: String): Account?

    @Query("SELECT * FROM accounts WHERE isDefault = 1 AND isActive = 1 LIMIT 1")
    suspend fun getDefaultAccount(): Account?

    @Query("SELECT SUM(CASE WHEN accountType = 'CREDIT_CARD' THEN -balance ELSE balance END) FROM accounts WHERE isActive = 1")
    fun getTotalNetWorth(): Flow<Double?>

    @Query("SELECT SUM(balance) FROM accounts WHERE isActive = 1 AND accountType IN ('CASH', 'BANK_ACCOUNT', 'E_WALLET')")
    fun getTotalAssets(): Flow<Double?>

    @Query("SELECT SUM(balance) FROM accounts WHERE isActive = 1 AND accountType = 'CREDIT_CARD'")
    fun getTotalLiabilities(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account)

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("UPDATE accounts SET isActive = 0 WHERE id = :accountId")
    suspend fun deactivateAccount(accountId: String)

    @Query("UPDATE accounts SET isActive = 0")
    suspend fun deactivateAllAccounts()

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()

    @Query("UPDATE accounts SET isDefault = 0")
    suspend fun clearAllDefaults()

    // ── Balance Update Methods ─────────────────────────────────────────────────

    /**
     * Update account balance by adding a delta amount.
     * Positive delta for income, negative for expenses.
     */
    @Query("UPDATE accounts SET balance = balance + :delta, updatedAt = :timestamp WHERE id = :accountId")
    suspend fun updateBalance(accountId: String, delta: Double, timestamp: Long = System.currentTimeMillis())

    /**
     * Get all unique account IDs from transactions.
     * Used for recalculating account balances.
     */
    @Query("SELECT DISTINCT accountId FROM transactions WHERE accountId IS NOT NULL AND accountId != ''")
    suspend fun getAllAccountIdsWithTransactions(): List<String>

    /**
     * Get the total balance delta for an account based on its transactions.
     * INCOME adds to balance, EXPENSE subtracts from balance.
     */
    @Query("""
        SELECT COALESCE(
            SUM(
                CASE
                    WHEN type IN ('INCOME', 'TRANSFER_IN', 'GOAL_WITHDRAWAL') THEN amount
                    WHEN type IN ('EXPENSE', 'TRANSFER_OUT', 'GOAL_CONTRIBUTION', 'AUTO_ALLOCATION') THEN -amount
                    WHEN type = 'BALANCE_ADJUSTMENT' THEN amount
                    ELSE 0
                END
            ), 0.0
        ) FROM transactions WHERE accountId = :accountId
    """)
    suspend fun calculateBalanceFromTransactions(accountId: String): Double

    /**
     * Recalculate and update an account's balance based on all its transactions.
     * This ensures balance stays in sync with the transaction history.
     */
    @Query("UPDATE accounts SET balance = :newBalance, updatedAt = :timestamp WHERE id = :accountId")
    suspend fun setBalance(accountId: String, newBalance: Double, timestamp: Long = System.currentTimeMillis())
}