package com.example.insightku.feature.budgeting.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Contribution type enum representing how money was added/removed from a goal.
 */
enum class ContributionType(val value: String) {
    MANUAL("manual"),
    AUTO_ALLOCATION("auto_allocation"),
    WITHDRAWAL("withdrawal");

    companion object {
        fun fromString(value: String): ContributionType =
            entries.find { it.value == value } ?: MANUAL
    }
}

/**
 * Contribution entity representing money movement to/from a goal.
 * Positive amounts add to goal, negative amounts are withdrawals.
 * This is an immutable ledger entry - corrections are made via reversal entries.
 */
@Entity(
    tableName = "contributions",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["goalId"]),
        Index(value = ["accountId"]),
        Index(value = ["transactionId"]),
        Index(value = ["createdAt"])
    ]
)
data class ContributionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val goalId: String,
    val accountId: String,
    val amount: Double, // Positive = add, Negative = withdrawal
    @ColumnInfo(defaultValue = "manual")
    val type: String = "manual",
    val transactionId: String? = null,
    @ColumnInfo(defaultValue = "''")
    val notes: String = "",
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = 0L
) {
    val contributionType: ContributionType get() = ContributionType.fromString(type)
}
