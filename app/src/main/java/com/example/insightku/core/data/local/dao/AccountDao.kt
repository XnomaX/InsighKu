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
}