package com.example.insightku.feature.budgeting.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Trigger types for auto-allocation rules.
 */
enum class AllocationTriggerType(val value: String) {
    INCOME_RECEIVED("income_received"),       // Any incoming transaction
    SPENDING_CATEGORY("spending_category"),    // Spending in specific category
    DAILY("daily"),                             // Daily recurring
    BALANCE_ABOVE("balance_above");            // Account balance exceeds threshold

    companion object {
        fun fromString(value: String): AllocationTriggerType =
            entries.find { it.value == value } ?: INCOME_RECEIVED
    }
}

/**
 * Allocation value types.
 */
enum class AllocationValueType(val value: String) {
    PERCENT("percent"),
    FIXED("fixed");

    companion object {
        fun fromString(value: String): AllocationValueType =
            entries.find { it.value == value } ?: PERCENT
    }
}

/**
 * AutoAllocationRule entity for automatic savings rules.
 * Rules generate suggestions (not auto-execute) that require user confirmation.
 */
@Entity(
    tableName = "auto_allocation_rules",
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
        Index(value = ["isEnabled"])
    ]
)
data class AutoAllocationRuleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val goalId: String,
    @ColumnInfo(defaultValue = "'income_received'")
    val triggerType: String = "income_received",
    // JSON-encoded trigger parameters:
    // - For SPENDING_CATEGORY: {"categoryId": "xxx"}
    // - For BALANCE_ABOVE: {"accountId": "xxx", "threshold": 1000.0}
    // - For DAILY: {} (no additional params)
    val triggerParams: String? = null,
    @ColumnInfo(defaultValue = "'percent'")
    val allocationType: String = "percent",
    val allocationValue: Double = 10.0, // 10% or $10 based on allocationType
    val isEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = 0L
) {
    val trigger: AllocationTriggerType get() = AllocationTriggerType.fromString(triggerType)
    val valueType: AllocationValueType get() = AllocationValueType.fromString(allocationType)
}
