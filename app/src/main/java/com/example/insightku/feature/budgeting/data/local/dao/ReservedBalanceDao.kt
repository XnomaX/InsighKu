package com.example.insightku.feature.budgeting.data.local.dao

import androidx.room.*
import com.example.insightku.feature.budgeting.data.model.ReservedBalanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReservedBalanceDao {

    @Query("SELECT * FROM reserved_balances")
    fun getAllReservedBalances(): Flow<List<ReservedBalanceEntity>>

    @Query("SELECT * FROM reserved_balances WHERE accountId = :accountId")
    fun getReservedBalanceByAccount(accountId: String): Flow<ReservedBalanceEntity?>

    @Query("SELECT * FROM reserved_balances WHERE accountId = :accountId")
    suspend fun getReservedBalanceByAccountSync(accountId: String): ReservedBalanceEntity?

    @Query("SELECT * FROM reserved_balances WHERE goalId = :goalId")
    fun getReservedBalanceByGoal(goalId: String): Flow<List<ReservedBalanceEntity>>

    @Query("SELECT * FROM reserved_balances WHERE goalId IS NULL")
    fun getGeneralPoolReservations(): Flow<List<ReservedBalanceEntity>>

    /**
     * Get total reserved for an account (all goals combined).
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM reserved_balances WHERE accountId = :accountId")
    suspend fun getTotalReservedForAccount(accountId: String): Double

    /**
     * Get total reserved for an account as Flow.
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM reserved_balances WHERE accountId = :accountId")
    fun getTotalReservedForAccountFlow(accountId: String): Flow<Double>

    /**
     * Get total reserved for a specific goal.
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM reserved_balances WHERE goalId = :goalId")
    suspend fun getTotalReservedForGoal(goalId: String): Double

    /**
     * Get available balance (balance - reserved).
     */
    @Query("SELECT COALESCE(balance, 0.0) - COALESCE((SELECT SUM(amount) FROM reserved_balances WHERE accountId = :accountId), 0.0) FROM accounts WHERE id = :accountId")
    suspend fun getAvailableBalance(accountId: String): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservedBalance(reservedBalance: ReservedBalanceEntity)

    @Update
    suspend fun updateReservedBalance(reservedBalance: ReservedBalanceEntity)

    /**
     * Increase reservation for an account/goal.
     */
    @Query("""
        INSERT INTO reserved_balances (accountId, amount, goalId, updatedAt)
        VALUES (:accountId, :amount, :goalId, :updatedAt)
        ON CONFLICT(accountId) DO UPDATE SET
            amount = amount + :amount,
            updatedAt = :updatedAt
    """)
    suspend fun increaseReservation(accountId: String, amount: Double, goalId: String?, updatedAt: Long = System.currentTimeMillis())

    /**
     * Decrease reservation for an account/goal.
     */
    @Query("""
        UPDATE reserved_balances
        SET amount = MAX(0, amount - :amount), updatedAt = :updatedAt
        WHERE accountId = :accountId
    """)
    suspend fun decreaseReservation(accountId: String, amount: Double, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteReservedBalance(reservedBalance: ReservedBalanceEntity)

    @Query("DELETE FROM reserved_balances WHERE accountId = :accountId AND goalId = :goalId")
    suspend fun deleteReservation(accountId: String, goalId: String?)

    @Query("DELETE FROM reserved_balances WHERE accountId = :accountId")
    suspend fun deleteAllReservationsForAccount(accountId: String)

    @Query("DELETE FROM reserved_balances")
    suspend fun deleteAllReservations()
}
