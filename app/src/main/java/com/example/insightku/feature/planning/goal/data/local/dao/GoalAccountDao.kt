package com.example.insightku.feature.planning.goal.data.local.dao

import androidx.room.*
import com.example.insightku.feature.planning.goal.data.model.GoalAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalAccountDao {

    @Query("SELECT * FROM goal_accounts")
    fun getAllGoalAccounts(): Flow<List<GoalAccountEntity>>

    @Query("SELECT * FROM goal_accounts WHERE goalId = :goalId")
    fun getGoalAccountsByGoal(goalId: String): Flow<List<GoalAccountEntity>>

    @Query("SELECT * FROM goal_accounts WHERE accountId = :accountId")
    fun getGoalAccountsByAccount(accountId: String): Flow<List<GoalAccountEntity>>

    @Query("SELECT * FROM goal_accounts WHERE goalId = :goalId AND accountId = :accountId")
    suspend fun getGoalAccount(goalId: String, accountId: String): GoalAccountEntity?

    @Query("SELECT * FROM goal_accounts WHERE goalId = :goalId AND isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryAccountForGoal(goalId: String): GoalAccountEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM goal_accounts WHERE goalId = :goalId AND accountId = :accountId)")
    suspend fun isAccountLinkedToGoal(goalId: String, accountId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalAccount(goalAccount: GoalAccountEntity)

    @Update
    suspend fun updateGoalAccount(goalAccount: GoalAccountEntity)

    @Delete
    suspend fun deleteGoalAccount(goalAccount: GoalAccountEntity)

    @Query("DELETE FROM goal_accounts WHERE goalId = :goalId AND accountId = :accountId")
    suspend fun unlinkAccountFromGoal(goalId: String, accountId: String)

    @Query("DELETE FROM goal_accounts WHERE goalId = :goalId")
    suspend fun unlinkAllAccountsFromGoal(goalId: String)

    @Query("UPDATE goal_accounts SET isPrimary = 0 WHERE goalId = :goalId")
    suspend fun clearPrimaryForGoal(goalId: String)

    @Query("UPDATE goal_accounts SET isPrimary = 1 WHERE goalId = :goalId AND accountId = :accountId")
    suspend fun setPrimaryAccount(goalId: String, accountId: String)

    @Query("SELECT COUNT(*) FROM goal_accounts WHERE goalId = :goalId")
    suspend fun getLinkedAccountCount(goalId: String): Int

    @Query("SELECT COUNT(*) FROM goal_accounts WHERE accountId = :accountId")
    suspend fun getLinkedGoalCount(accountId: String): Int
}
