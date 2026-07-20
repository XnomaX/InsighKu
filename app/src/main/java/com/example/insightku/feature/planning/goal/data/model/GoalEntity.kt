package com.example.insightku.feature.planning.goal.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class GoalStatus(val value: String) {
    ACTIVE("active"),
    PAUSED("paused"),
    COMPLETED("completed"),
    ARCHIVED("archived");

    companion object {
        fun fromString(value: String): GoalStatus =
            entries.find { it.value == value } ?: ACTIVE
    }
}

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetAmount: Double,
    val deadline: Long? = null,
    @ColumnInfo(defaultValue = "'active'")
    val status: String = "active",
    val autoAllocate: Boolean = false,
    val allocationPriority: Int = 0,
    @ColumnInfo(defaultValue = "'savings'")
    val iconName: String = "savings",
    @ColumnInfo(defaultValue = "'#7C4DFF'")
    val color: String = "#7C4DFF",
    @ColumnInfo(defaultValue = "''")
    val notes: String = "",
    @ColumnInfo(defaultValue = "0")
    val reminderEnabled: Boolean = false,
    val isActive: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = 0L,
    @ColumnInfo(defaultValue = "1")
    val isSynced: Boolean = true
) {
    val goalStatus: GoalStatus get() = GoalStatus.fromString(status)
}
