package com.example.insightku.feature.planning.goal.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_targets")
data class DailyTargetEntity(
    @PrimaryKey @ColumnInfo(defaultValue = "'global_daily_target'")
    val id: String = "global_daily_target",
    val targetAmount: Double = 0.0,
    @ColumnInfo(defaultValue = "0")
    val date: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = 0L
) {
    val isSet: Boolean get() = targetAmount > 0
}
