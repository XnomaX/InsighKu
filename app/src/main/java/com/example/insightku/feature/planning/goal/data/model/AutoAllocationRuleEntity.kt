package com.example.insightku.feature.planning.goal.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class AllocationTriggerType(val value: String) {
    INCOME_RECEIVED("income_received"),
    SPENDING_CATEGORY("spending_category"),
    ROUND_UP("round_up"),
    DAILY("daily"),
    WEEKLY("weekly"),
    BIWEEKLY("biweekly"),
    MONTHLY("monthly"),
    BALANCE_ABOVE("balance_above");

    companion object {
        fun fromString(value: String): AllocationTriggerType =
            entries.find { it.value == value } ?: INCOME_RECEIVED
    }
}

enum class AllocationValueType(val value: String) {
    PERCENT("percent"),
    FIXED("fixed");

    companion object {
        fun fromString(value: String): AllocationValueType =
            entries.find { it.value == value } ?: PERCENT
    }
}

enum class ConfirmationMode(val value: String) {
    AUTO("auto"),
    CONFIRMATION_REQUIRED("confirmation_required");

    companion object {
        fun fromString(value: String): ConfirmationMode =
            entries.find { it.value == value } ?: AUTO
    }
}

enum class ScheduledFrequency(val value: String) {
    DAILY("daily"),
    WEEKLY("weekly"),
    BIWEEKLY("biweekly"),
    MONTHLY("monthly");

    companion object {
        fun fromString(value: String): ScheduledFrequency =
            entries.find { it.value == value } ?: DAILY
    }
}

enum class CategoryBasedExecutionMode(val value: String) {
    EVERY_TRANSACTION("every_transaction"),
    AFTER_DAILY_TOTAL("after_daily_total"),
    AFTER_MONTHLY_TOTAL("after_monthly_total");

    companion object {
        fun fromString(value: String): CategoryBasedExecutionMode =
            entries.find { it.value == value } ?: EVERY_TRANSACTION
    }
}

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
    val triggerParams: String? = null,
    @ColumnInfo(defaultValue = "'percent'")
    val allocationType: String = "percent",
    val allocationValue: Double = 10.0,
    val isEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = 0L,
    val sourceAccountId: String? = null,
    @ColumnInfo(defaultValue = "'auto'")
    val confirmationMode: String = "auto",
    @ColumnInfo(defaultValue = "'[]'")
    val incomeCategoryIds: String = "[]",
    @ColumnInfo(defaultValue = "0")
    val minIncomeAmount: Double = 0.0,
    @ColumnInfo(defaultValue = "0")
    val roundUpEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "5000")
    val roundUpIncrement: Double = 5000.0,
    @ColumnInfo(defaultValue = "'daily'")
    val scheduledFrequency: String = "daily",
    @ColumnInfo(defaultValue = "1")
    val scheduledDayOfWeek: Int = 1,
    @ColumnInfo(defaultValue = "1")
    val scheduledDayOfMonth: Int = 1,
    @ColumnInfo(defaultValue = "0")
    val lastExecutedAt: Long = 0L,
    // ── P1.1: Trigger-specific configuration ────────────────────────────────
    @ColumnInfo(defaultValue = "8")
    val executionHour: Int = 8,
    @ColumnInfo(defaultValue = "0")
    val executionMinute: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val biweeklyStartDate: Long = 0,
    @ColumnInfo(defaultValue = "0")
    val minRemainingBalance: Double = 0.0,
    @ColumnInfo(defaultValue = "'[]'")
    val categoryBasedCategoryIds: String = "[]",
    @ColumnInfo(defaultValue = "'every_transaction'")
    val categoryBasedExecutionMode: String = "every_transaction"
) {
    val trigger: AllocationTriggerType get() = AllocationTriggerType.fromString(triggerType)
    val valueType: AllocationValueType get() = AllocationValueType.fromString(allocationType)
    val mode: ConfirmationMode get() = ConfirmationMode.fromString(confirmationMode)
    val frequency: ScheduledFrequency get() = ScheduledFrequency.fromString(scheduledFrequency)
    val catExecMode: CategoryBasedExecutionMode get() = CategoryBasedExecutionMode.fromString(categoryBasedExecutionMode)
}
