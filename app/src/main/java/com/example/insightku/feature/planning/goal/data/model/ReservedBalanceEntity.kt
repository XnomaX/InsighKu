package com.example.insightku.feature.planning.goal.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val goalId: String? = null,
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = 0L
)
