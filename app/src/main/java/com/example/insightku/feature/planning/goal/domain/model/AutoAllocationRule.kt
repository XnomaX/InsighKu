package com.example.insightku.feature.planning.goal.domain.model

import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.feature.planning.goal.data.model.AllocationTriggerType
import com.example.insightku.feature.planning.goal.data.model.AllocationValueType
import com.example.insightku.feature.planning.goal.data.model.AutoAllocationRuleEntity
import com.example.insightku.feature.planning.goal.data.model.CategoryBasedExecutionMode
import com.example.insightku.feature.planning.goal.data.model.ConfirmationMode
import com.example.insightku.feature.planning.goal.data.model.ScheduledFrequency
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class AllocationTriggerParams(
    val categoryId: String? = null, val accountId: String? = null, val threshold: Double? = null
) {
    fun toJson(): String = JSONObject().apply {
        categoryId?.let { put("categoryId", it) }
        accountId?.let { put("accountId", it) }
        threshold?.let { put("threshold", it) }
    }.toString()

    companion object {
        fun fromJson(json: String?): AllocationTriggerParams? {
            if (json.isNullOrBlank()) return null
            return try {
                val obj = JSONObject(json)
                AllocationTriggerParams(
                    categoryId = obj.optString("categoryId").takeIf { it.isNotBlank() },
                    accountId = obj.optString("accountId").takeIf { it.isNotBlank() },
                    threshold = if (obj.has("threshold")) obj.getDouble("threshold") else null
                )
            } catch (e: Exception) { null }
        }
    }
}

data class AutoAllocationRule(
    val id: String, val goalId: String, val goalName: String = "",
    val triggerType: AllocationTriggerType, val triggerParams: AllocationTriggerParams?,
    val allocationType: AllocationValueType, val allocationValue: Double, val isEnabled: Boolean,
    val createdAt: Instant, val updatedAt: Instant,
    val sourceAccountId: String? = null, val confirmationMode: ConfirmationMode = ConfirmationMode.AUTO,
    val incomeCategoryIds: List<String> = emptyList(), val minIncomeAmount: Double = 0.0,
    val roundUpEnabled: Boolean = false, val roundUpIncrement: Double = 5000.0,
    val scheduledFrequency: ScheduledFrequency = ScheduledFrequency.DAILY,
    val scheduledDayOfWeek: Int = 1, val scheduledDayOfMonth: Int = 1, val lastExecutedAt: Instant? = null,
    // ── P1.1: Trigger-specific configuration ────────────────────────────────
    val executionHour: Int = 8, val executionMinute: Int = 0,
    val biweeklyStartDate: Long = 0, val minRemainingBalance: Double = 0.0,
    val categoryBasedCategoryIds: List<String> = emptyList(),
    val categoryBasedExecutionMode: CategoryBasedExecutionMode = CategoryBasedExecutionMode.EVERY_TRANSACTION
) {
    private fun formatTime(): String {
        val h = executionHour.toString().padStart(2, '0')
        val m = executionMinute.toString().padStart(2, '0')
        return "$h:$m"
    }

    private fun dayOfWeekName(): String = when (scheduledDayOfWeek) {
        1 -> "Monday"; 2 -> "Tuesday"; 3 -> "Wednesday"
        4 -> "Thursday"; 5 -> "Friday"; 6 -> "Saturday"; 7 -> "Sunday"
        else -> "Monday"
    }

    private fun dayOfMonthText(): String = if (scheduledDayOfMonth == -1) "Last day"
        else "$scheduledDayOfMonth${ordinalSuffix(scheduledDayOfMonth)}"

    private fun ordinalSuffix(n: Int): String = when {
        n in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else -> "th"
    }

    private fun biweeklyStartDateText(): String {
        if (biweeklyStartDate <= 0) return "Not set"
        return DateFormatter.formatShortDate(biweeklyStartDate)
    }

    val description: String get() = when (triggerType) {
        AllocationTriggerType.INCOME_RECEIVED -> {
            val base = if (allocationType == AllocationValueType.PERCENT) "Save ${allocationValue.toInt()}% of income" else "Save ${formatAmount(allocationValue)} from income"
            if (minIncomeAmount > 0) "$base (min ${formatAmount(minIncomeAmount)})" else base
        }
        AllocationTriggerType.SPENDING_CATEGORY -> if (allocationType == AllocationValueType.PERCENT) "Save ${allocationValue.toInt()}% from category spending" else "Save ${formatAmount(allocationValue)} from category spending"
        AllocationTriggerType.ROUND_UP -> "Round up to nearest ${formatAmount(roundUpIncrement)}"
        AllocationTriggerType.DAILY -> "Every day at ${formatTime()}"
        AllocationTriggerType.WEEKLY -> "Every ${dayOfWeekName()} at ${formatTime()}"
        AllocationTriggerType.BIWEEKLY -> "Every 2 weeks from ${biweeklyStartDateText()} at ${formatTime()}"
        AllocationTriggerType.MONTHLY -> "Every ${dayOfMonthText()} at ${formatTime()}"
        AllocationTriggerType.BALANCE_ABOVE -> {
            val thresh = triggerParams?.threshold ?: 0.0
            val base = if (allocationType == AllocationValueType.PERCENT) "Save ${allocationValue.toInt()}% when balance > ${formatAmount(thresh)}" else "Save ${formatAmount(allocationValue)} when balance > ${formatAmount(thresh)}"
            if (minRemainingBalance > 0) "$base (keep ${formatAmount(minRemainingBalance)})" else base
        }
    }

    /** Human-readable summary for the configuration preview */
    val configurationSummary: String get() = when (triggerType) {
        AllocationTriggerType.DAILY -> "Every day at ${formatTime()}"
        AllocationTriggerType.WEEKLY -> "Every ${dayOfWeekName()} at ${formatTime()}"
        AllocationTriggerType.BIWEEKLY -> "Every two weeks starting ${biweeklyStartDateText()} at ${formatTime()}"
        AllocationTriggerType.MONTHLY -> "Every ${dayOfMonthText()} of the month at ${formatTime()}"
        AllocationTriggerType.BALANCE_ABOVE -> {
            val thresh = triggerParams?.threshold ?: 0.0
            "When balance exceeds ${formatAmount(thresh)}"
        }
        AllocationTriggerType.SPENDING_CATEGORY -> {
            val modeText = when (categoryBasedExecutionMode) {
                CategoryBasedExecutionMode.EVERY_TRANSACTION -> "On every transaction"
                CategoryBasedExecutionMode.AFTER_DAILY_TOTAL -> "After daily total"
                CategoryBasedExecutionMode.AFTER_MONTHLY_TOTAL -> "After monthly total"
            }
            "$modeText in selected categories"
        }
        AllocationTriggerType.INCOME_RECEIVED -> "When income is received"
        AllocationTriggerType.ROUND_UP -> "After every expense (round-up)"
    }

    val triggerLabel: String get() = when (triggerType) {
        AllocationTriggerType.INCOME_RECEIVED -> "Income-Based"
        AllocationTriggerType.SPENDING_CATEGORY -> "Category-Based"
        AllocationTriggerType.ROUND_UP -> "Round-Up"
        AllocationTriggerType.DAILY -> "Daily"
        AllocationTriggerType.WEEKLY -> "Weekly"
        AllocationTriggerType.BIWEEKLY -> "Biweekly"
        AllocationTriggerType.MONTHLY -> "Monthly"
        AllocationTriggerType.BALANCE_ABOVE -> "Balance Threshold"
    }

    val confirmationModeLabel: String get() = when (confirmationMode) {
        ConfirmationMode.AUTO -> "Automatic"
        ConfirmationMode.CONFIRMATION_REQUIRED -> "Confirm First"
    }

    private fun formatAmount(amount: Double): String = NumberFormatter.formatCurrency(amount)

    companion object {
        fun fromEntity(entity: AutoAllocationRuleEntity, goalName: String = ""): AutoAllocationRule {
            val categoryIds = try {
                val arr = JSONArray(entity.incomeCategoryIds)
                (0 until arr.length()).map { arr.getString(it) }
            } catch (e: Exception) { emptyList() }
            val catBasedIds = try {
                val arr = JSONArray(entity.categoryBasedCategoryIds)
                (0 until arr.length()).map { arr.getString(it) }
            } catch (e: Exception) { emptyList() }

            return AutoAllocationRule(
                id = entity.id, goalId = entity.goalId, goalName = goalName,
                triggerType = entity.trigger, triggerParams = AllocationTriggerParams.fromJson(entity.triggerParams),
                allocationType = entity.valueType, allocationValue = entity.allocationValue,
                isEnabled = entity.isEnabled, createdAt = Instant.ofEpochMilli(entity.createdAt),
                updatedAt = Instant.ofEpochMilli(entity.updatedAt), sourceAccountId = entity.sourceAccountId,
                confirmationMode = ConfirmationMode.fromString(entity.confirmationMode),
                incomeCategoryIds = categoryIds, minIncomeAmount = entity.minIncomeAmount,
                roundUpEnabled = entity.roundUpEnabled, roundUpIncrement = entity.roundUpIncrement,
                scheduledFrequency = ScheduledFrequency.fromString(entity.scheduledFrequency),
                scheduledDayOfWeek = entity.scheduledDayOfWeek, scheduledDayOfMonth = entity.scheduledDayOfMonth,
                lastExecutedAt = if (entity.lastExecutedAt > 0) Instant.ofEpochMilli(entity.lastExecutedAt) else null,
                executionHour = entity.executionHour, executionMinute = entity.executionMinute,
                biweeklyStartDate = entity.biweeklyStartDate, minRemainingBalance = entity.minRemainingBalance,
                categoryBasedCategoryIds = catBasedIds,
                categoryBasedExecutionMode = CategoryBasedExecutionMode.fromString(entity.categoryBasedExecutionMode)
            )
        }
    }

    fun toEntity(): AutoAllocationRuleEntity {
        return AutoAllocationRuleEntity(
            id = id, goalId = goalId, triggerType = triggerType.value,
            triggerParams = triggerParams?.toJson(), allocationType = allocationType.value,
            allocationValue = allocationValue, isEnabled = isEnabled,
            createdAt = createdAt.toEpochMilli(), updatedAt = System.currentTimeMillis(),
            sourceAccountId = sourceAccountId, confirmationMode = confirmationMode.value,
            incomeCategoryIds = JSONArray(incomeCategoryIds).toString(),
            minIncomeAmount = minIncomeAmount, roundUpEnabled = roundUpEnabled,
            roundUpIncrement = roundUpIncrement, scheduledFrequency = scheduledFrequency.value,
            scheduledDayOfWeek = scheduledDayOfWeek, scheduledDayOfMonth = scheduledDayOfMonth,
            lastExecutedAt = lastExecutedAt?.toEpochMilli() ?: 0L,
            executionHour = executionHour, executionMinute = executionMinute,
            biweeklyStartDate = biweeklyStartDate, minRemainingBalance = minRemainingBalance,
            categoryBasedCategoryIds = JSONArray(categoryBasedCategoryIds).toString(),
            categoryBasedExecutionMode = categoryBasedExecutionMode.value
        )
    }
}

data class AllocationSuggestion(
    val goalId: String, val goalName: String,
    val amount: Double, val sourceAccountId: String, val sourceAccountName: String,
    val triggerDescription: String, val ruleDescription: String,
    val ruleId: String = "",
    val id: String = UUID.randomUUID().toString(), val createdAt: Instant = Instant.now()
)

data class AutoAllocationResult(
    val suggestions: List<AllocationSuggestion>, val autoExecuted: List<AllocationSuggestion>,
    val processedTransactionId: String?, val processedAmount: Double?
)
