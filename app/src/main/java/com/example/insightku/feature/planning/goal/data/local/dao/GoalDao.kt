package com.example.insightku.feature.planning.goal.data.local.dao

import androidx.room.*
import com.example.insightku.feature.planning.goal.data.model.GoalEntity
import com.example.insightku.feature.planning.goal.data.model.GoalStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals WHERE isActive = 1 AND status != :archivedStatus ORDER BY createdAt DESC")
    fun getAllActiveGoals(archivedStatus: String = GoalStatus.ARCHIVED.value): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: String): GoalEntity?

    @Query("SELECT * FROM goals WHERE id = :id")
    fun getGoalByIdFlow(id: String): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE status = :status AND isActive = 1 ORDER BY allocationPriority ASC")
    fun getGoalsByStatus(status: String): Flow<List<GoalEntity>>

    @Query("SELECT COUNT(*) FROM goals WHERE isActive = 1 AND status != :archivedStatus")
    suspend fun getActiveGoalCount(archivedStatus: String = GoalStatus.ARCHIVED.value): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("UPDATE goals SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateGoalStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE goals SET autoAllocate = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateAutoAllocate(id: String, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE goals SET isActive = 0, status = :archivedStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archiveGoal(id: String, archivedStatus: String = GoalStatus.ARCHIVED.value, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: String)

    // ── Offline-First Sync Support ──────────────────────────────────────────────

    /** Mark a goal as synced to Firestore. */
    @Query("UPDATE goals SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    /** Get all goals that haven't been synced to Firestore yet. */
    @Query("SELECT * FROM goals WHERE isSynced = 0")
    suspend fun getUnsyncedGoals(): List<GoalEntity>

    /** Get all unsynced goals as Flow. */
    @Query("SELECT * FROM goals WHERE isSynced = 0")
    fun getUnsyncedGoalsFlow(): Flow<List<GoalEntity>>

    /** Insert or update a goal from Firestore (remote refresh). Uses IGNORE to preserve local unsynced data. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGoalFromRemote(goal: GoalEntity)

    /** Bulk insert/update goals from Firestore remote sync. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGoalsFromRemote(goals: List<GoalEntity>)
}
