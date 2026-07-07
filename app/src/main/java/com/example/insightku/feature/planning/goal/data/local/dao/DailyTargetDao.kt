package com.example.insightku.feature.planning.goal.data.local.dao

import androidx.room.*
import com.example.insightku.feature.planning.goal.data.model.DailyTargetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTargetDao {

    @Query("SELECT * FROM daily_targets WHERE id = 'global_daily_target'")
    fun getGlobalDailyTarget(): Flow<DailyTargetEntity?>

    @Query("SELECT * FROM daily_targets WHERE id = 'global_daily_target'")
    suspend fun getGlobalDailyTargetSync(): DailyTargetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyTarget(dailyTarget: DailyTargetEntity)

    @Update
    suspend fun updateDailyTarget(dailyTarget: DailyTargetEntity)

    @Query("UPDATE daily_targets SET targetAmount = :amount, updatedAt = :updatedAt WHERE id = 'global_daily_target'")
    suspend fun updateTargetAmount(amount: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM daily_targets WHERE id = 'global_daily_target'")
    suspend fun clearDailyTarget()
}
