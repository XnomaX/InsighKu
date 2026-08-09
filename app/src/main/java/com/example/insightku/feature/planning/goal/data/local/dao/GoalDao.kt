package com.example.insightku.feature.planning.goal.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.insightku.feature.planning.goal.data.model.GoalEntity
import com.example.insightku.feature.planning.goal.data.model.GoalStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals WHERE isActive = 1 AND status != :archivedStatus ORDER BY createdAt DESC")
    fun getAllActiveGoals(archivedStatus: String = GoalStatus.ARCHIVED.value): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    /** Every goal including archived — for allocation joins (archived goals keep their set-aside). */
    @Query("SELECT * FROM goals")
    fun getAllGoalsIncludingArchived(): Flow<List<GoalEntity>>

    /** Get only goals with ACTIVE status. */
    @Query("SELECT * FROM goals WHERE status = 'active' AND isActive = 1 ORDER BY createdAt DESC")
    fun getActiveStatusGoals(): Flow<List<GoalEntity>>

    /** Get only goals with PAUSED status. */
    @Query("SELECT * FROM goals WHERE status = 'paused' AND isActive = 1 ORDER BY updatedAt DESC")
    fun getPausedGoals(): Flow<List<GoalEntity>>

    /** Get only goals with COMPLETED status. */
    @Query("SELECT * FROM goals WHERE status = 'completed' AND isActive = 1 ORDER BY updatedAt DESC")
    fun getCompletedGoals(): Flow<List<GoalEntity>>

    /** Get only archived goals. */
    @Query("SELECT * FROM goals WHERE status = 'archived' ORDER BY updatedAt DESC")
    fun getArchivedGoals(): Flow<List<GoalEntity>>

    /** Restore an archived goal back to active. */
    @Query("UPDATE goals SET isActive = 1, status = 'active', updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreGoal(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: String): GoalEntity?

    /** Get a goal only if it is active (not archived/deleted). */
    @Query("SELECT * FROM goals WHERE id = :id AND isActive = 1")
    suspend fun getGoalByIdActive(id: String): GoalEntity?

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
