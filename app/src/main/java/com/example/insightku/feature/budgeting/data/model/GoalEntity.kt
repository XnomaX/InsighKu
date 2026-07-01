package com.example.insightku.feature.budgeting.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Goal status enum representing the lifecycle states of a goal.
 */
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

/**
 * Goal entity representing a savings target.
 * Stored locally in Room and synced to Firestore.
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetAmount: Double,
    val deadline: Long? = null, // Epoch millis
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
    val isActive: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = 0L
) {
    val goalStatus: GoalStatus get() = GoalStatus.fromString(status)
}
