package com.example.insightku.feature.planning.goal.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class ContributionType(val value: String) {
    MANUAL("manual"),
    AUTO_ALLOCATION("auto_allocation"),
    WITHDRAWAL("withdrawal");

    companion object {
        fun fromString(value: String): ContributionType =
            entries.find { it.value == value } ?: MANUAL
    }
}

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
    val amount: Double,
    @ColumnInfo(defaultValue = "manual")
    val type: String = "manual",
    val transactionId: String? = null,
    @ColumnInfo(defaultValue = "''")
    val notes: String = "",
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = 0L,
    @ColumnInfo(defaultValue = "1")
    val isSynced: Boolean = true
) {
    val contributionType: ContributionType get() = ContributionType.fromString(type)
}
