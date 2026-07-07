package com.example.insightku.feature.planning.goal.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "goal_accounts",
    primaryKeys = ["goalId", "accountId"],
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
        Index(value = ["accountId"])
    ]
)
data class GoalAccountEntity(
    val goalId: String,
    val accountId: String,
    @ColumnInfo(defaultValue = "100.0")
    val allocationPercent: Double = 100.0,
    @ColumnInfo(defaultValue = "1")
    val isPrimary: Boolean = true
)
