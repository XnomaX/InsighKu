package com.example.insightku.feature.budgeting.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ReservedBalance entity tracking funds earmarked for goals.
 * Prevents overspending when auto-allocation is pending.
 *
 * @param accountId The account this reservation applies to
 * @param amount Currently reserved amount (always >= 0)
 * @param goalId Optional goal ID if reserved for specific goal, null = general pool
 * @param updatedAt Last update timestamp
 */
@Entity(
    tableName = "reserved_balances",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["goalId"])
    ]
)
data class ReservedBalanceEntity(
    @PrimaryKey val accountId: String,
    val amount: Double = 0.0,
    val goalId: String? = null, // null = general pool reservation
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = 0L
)
