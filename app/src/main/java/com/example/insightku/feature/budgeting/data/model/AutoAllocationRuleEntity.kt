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
    INCOME_RECEIVED("income_received"),       // Any incoming transaction (Pay Yourself First)
    SPENDING_CATEGORY("spending_category"),    // Spending in specific category
    ROUND_UP("round_up"),                      // Round up expense to nearest increment
    DAILY("daily"),                             // Daily recurring
    WEEKLY("weekly"),                           // Weekly recurring
    BIWEEKLY("biweekly"),                       // Biweekly recurring
    MONTHLY("monthly"),                         // Monthly recurring
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
 * Confirmation mode for auto-allocation rules.
 * AUTO: Execute immediately without user confirmation.
 * CONFIRMATION_REQUIRED: Show a confirmation dialog before allocating.
 */
enum class ConfirmationMode(val value: String) {
    AUTO("auto"),
    CONFIRMATION_REQUIRED("confirmation_required");

    companion object {
        fun fromString(value: String): ConfirmationMode =
            entries.find { it.value == value } ?: AUTO
    }
}

/**
 * Scheduled frequency for recurring allocations.
 */
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

/**
 * AutoAllocationRule entity for automatic savings rules.
 * Rules generate suggestions (not auto-execute) that require user confirmation.
 *
 * Enhanced for Smart Auto Allocation:
 * - sourceAccountId: which account to draw from
 * - confirmationMode: AUTO or CONFIRMATION_REQUIRED
 * - incomeCategoryIds: JSON array of category IDs to filter income sources
 * - minIncomeAmount: minimum income threshold to trigger allocation
 * - roundUpEnabled: for ROUND_UP trigger, whether rounding is active
 * - roundUpIncrement: rounding increment (1000, 5000, 10000)
 * - scheduledFrequency: for scheduled triggers
 * - scheduledDayOfWeek / scheduledDayOfMonth: when to execute scheduled allocations
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
    val allocationValue: Double = 10.0, // 10% or Rp10 based on allocationType
    val isEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = 0L,

    // ── Smart Auto Allocation Fields ──────────────────────────────────────────
    /** Source account for the allocation. If null, uses the transaction's account. */
    val sourceAccountId: String? = null,

    /** Confirmation mode: AUTO = execute immediately, CONFIRMATION_REQUIRED = show dialog. */
    @ColumnInfo(defaultValue = "'auto'")
    val confirmationMode: String = "auto",

    /** JSON array of income category IDs to filter (empty = all categories). */
    @ColumnInfo(defaultValue = "'[]'")
    val incomeCategoryIds: String = "[]",

    /** Minimum income amount to trigger allocation. 0 = no minimum. */
    @ColumnInfo(defaultValue = "0")
    val minIncomeAmount: Double = 0.0,

    /** For ROUND_UP trigger: whether rounding is enabled. */
    @ColumnInfo(defaultValue = "0")
    val roundUpEnabled: Boolean = false,

    /** For ROUND_UP trigger: rounding increment in Rupiah (1000, 5000, 10000). */
    @ColumnInfo(defaultValue = "5000")
    val roundUpIncrement: Double = 5000.0,

    /** For scheduled triggers: frequency of allocation. */
    @ColumnInfo(defaultValue = "'daily'")
    val scheduledFrequency: String = "daily",

    /** For scheduled triggers: day of week (1=Monday..7=Sunday). */
    @ColumnInfo(defaultValue = "1")
    val scheduledDayOfWeek: Int = 1,

    /** For scheduled triggers: day of month (1-31). */
    @ColumnInfo(defaultValue = "1")
    val scheduledDayOfMonth: Int = 1,

    /** For scheduled triggers: last time the allocation was executed. */
    @ColumnInfo(defaultValue = "0")
    val lastExecutedAt: Long = 0L
) {
    val trigger: AllocationTriggerType get() = AllocationTriggerType.fromString(triggerType)
    val valueType: AllocationValueType get() = AllocationValueType.fromString(allocationType)
    val mode: ConfirmationMode get() = ConfirmationMode.fromString(confirmationMode)
    val frequency: ScheduledFrequency get() = ScheduledFrequency.fromString(scheduledFrequency)
}
