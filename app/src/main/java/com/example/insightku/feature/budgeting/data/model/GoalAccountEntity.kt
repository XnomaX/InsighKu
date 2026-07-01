package com.example.insightku.feature.budgeting.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * GoalAccount entity linking goals to accounts with allocation settings.
 * One goal can link to multiple accounts, one account can fund multiple goals.
 */
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
    val allocationPercent: Double = 100.0, // % of contributions to this goal from this account
    @ColumnInfo(defaultValue = "1")
    val isPrimary: Boolean = true // Primary funding account for this goal
)
