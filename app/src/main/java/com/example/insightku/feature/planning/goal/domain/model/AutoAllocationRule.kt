package com.example.insightku.feature.planning.goal.domain.model

import android.content.Context
import com.example.insightku.R
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.i18n.LocaleHelper
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.feature.planning.goal.data.model.AllocationTriggerType
import com.example.insightku.feature.planning.goal.data.model.AllocationValueType
import com.example.insightku.feature.planning.goal.data.model.AutoAllocationRuleEntity
import com.example.insightku.feature.planning.goal.data.model.CategoryBasedExecutionMode
import com.example.insightku.feature.planning.goal.data.model.ConfirmationMode
import com.example.insightku.feature.planning.goal.data.model.RoundUpMode
import com.example.insightku.feature.planning.goal.data.model.ScheduledFrequency
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
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
            } catch (_: Exception) {
                null
            }
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
    val roundUpEnabled: Boolean = false, val roundUpIncrement: Double = 5000.0, val roundUpMode: RoundUpMode = RoundUpMode.ROUND_UP,
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

    /**
     * Localized copy for the rule list and persisted allocation drafts.
     *
     * Takes a [Context] because this domain model has no dependency-injection
     * access; the two callers are a composable (`GoalDetailAutoAllocation`) and
     * `AutoAllocationEngine`, which receives an `@ApplicationContext`. The copy
     * is resolved at render/persist time — never cached on the model — so a
     * language switch is reflected on the next render.
     */
    fun description(context: Context): String = descriptionInternal(context)

    private fun descriptionInternal(context: Context): String = when (triggerType) {
        AllocationTriggerType.INCOME_RECEIVED -> {
            val base = if (allocationType == AllocationValueType.PERCENT) {
                context.getString(R.string.auto_alloc_desc_income_percent, allocationValue.toInt())
            } else {
                context.getString(R.string.auto_alloc_desc_income_amount, formatAmount(allocationValue))
            }
            if (minIncomeAmount > 0) {
                "$base " + context.getString(R.string.auto_alloc_desc_min_income, formatAmount(minIncomeAmount))
            } else base
        }
        AllocationTriggerType.SPENDING_CATEGORY ->
            if (allocationType == AllocationValueType.PERCENT) {
                context.getString(R.string.auto_alloc_desc_category_percent, allocationValue.toInt())
            } else {
                context.getString(R.string.auto_alloc_desc_category_amount, formatAmount(allocationValue))
            }
        AllocationTriggerType.ROUND_UP -> when (roundUpMode) {
            RoundUpMode.ROUND_UP -> context.getString(R.string.auto_alloc_desc_round_up, formatAmount(roundUpIncrement))
            RoundUpMode.ROUND_DOWN -> context.getString(R.string.auto_alloc_desc_round_down, formatAmount(roundUpIncrement))
            RoundUpMode.ROUND_NEAREST -> context.getString(R.string.auto_alloc_desc_round_nearest, formatAmount(roundUpIncrement))
        }
        AllocationTriggerType.DAILY -> context.getString(R.string.auto_alloc_desc_daily, formatTime())
        AllocationTriggerType.WEEKLY -> context.getString(R.string.auto_alloc_desc_weekly, dayOfWeekName(context), formatTime())
        AllocationTriggerType.BIWEEKLY -> context.getString(R.string.auto_alloc_desc_biweekly, biweeklyStartDateText(context), formatTime())
        AllocationTriggerType.MONTHLY -> context.getString(R.string.auto_alloc_desc_monthly, dayOfMonthText(context), formatTime())
        AllocationTriggerType.BALANCE_ABOVE -> {
            val thresh = triggerParams?.threshold ?: 0.0
            val base = if (allocationType == AllocationValueType.PERCENT) {
                context.getString(R.string.auto_alloc_desc_balance_percent, allocationValue.toInt(), formatAmount(thresh))
            } else {
                context.getString(R.string.auto_alloc_desc_balance_amount, formatAmount(allocationValue), formatAmount(thresh))
            }
            if (minRemainingBalance > 0) {
                "$base " + context.getString(R.string.auto_alloc_desc_keep_balance, formatAmount(minRemainingBalance))
            } else base
        }
    }

    fun triggerLabel(context: Context): String = when (triggerType) {
        AllocationTriggerType.INCOME_RECEIVED -> context.getString(R.string.auto_alloc_trigger_income)
        AllocationTriggerType.SPENDING_CATEGORY -> context.getString(R.string.auto_alloc_trigger_category)
        AllocationTriggerType.ROUND_UP -> when (roundUpMode) {
            // The rounding-mode selector (AutoAllocationDialog) labels these same
            // enum values with this family; the trigger chip there labels the
            // trigger type, so the two surfaces answer different questions.
            RoundUpMode.ROUND_UP -> context.getString(R.string.auto_alloc_round_up)
            RoundUpMode.ROUND_DOWN -> context.getString(R.string.auto_alloc_round_down)
            RoundUpMode.ROUND_NEAREST -> context.getString(R.string.auto_alloc_round_nearest)
        }
        AllocationTriggerType.DAILY -> context.getString(R.string.auto_alloc_trigger_daily)
        AllocationTriggerType.WEEKLY -> context.getString(R.string.auto_alloc_trigger_weekly)
        AllocationTriggerType.BIWEEKLY -> context.getString(R.string.auto_alloc_trigger_biweekly)
        AllocationTriggerType.MONTHLY -> context.getString(R.string.auto_alloc_trigger_monthly)
        AllocationTriggerType.BALANCE_ABOVE -> context.getString(R.string.auto_alloc_trigger_balance)
    }

    private fun dayOfWeekName(context: Context): String = when (scheduledDayOfWeek) {
        1 -> context.getString(R.string.auto_alloc_day_monday)
        2 -> context.getString(R.string.auto_alloc_day_tuesday)
        3 -> context.getString(R.string.auto_alloc_day_wednesday)
        4 -> context.getString(R.string.auto_alloc_day_thursday)
        5 -> context.getString(R.string.auto_alloc_day_friday)
        6 -> context.getString(R.string.auto_alloc_day_saturday)
        7 -> context.getString(R.string.auto_alloc_day_sunday)
        else -> context.getString(R.string.auto_alloc_day_monday)
    }

    private fun dayOfMonthText(context: Context): String =
        if (scheduledDayOfMonth == -1) context.getString(R.string.auto_alloc_desc_last_day)
        else context.getString(
            R.string.auto_alloc_day_ordinal,
            scheduledDayOfMonth,
            ordinalSuffix(context, scheduledDayOfMonth)
        )

    private fun ordinalSuffix(context: Context, n: Int): String = when {
        n in 11..13 -> context.getString(R.string.auto_alloc_ordinal_th)
        n % 10 == 1 -> context.getString(R.string.auto_alloc_ordinal_st)
        n % 10 == 2 -> context.getString(R.string.auto_alloc_ordinal_nd)
        n % 10 == 3 -> context.getString(R.string.auto_alloc_ordinal_rd)
        else -> context.getString(R.string.auto_alloc_ordinal_th)
    }

    private fun biweeklyStartDateText(context: Context): String {
        if (biweeklyStartDate <= 0) return context.getString(R.string.auto_alloc_not_set)
        return DateFormatter.formatShortDate(biweeklyStartDate, LocaleHelper.currentLocale())
    }

    private fun formatAmount(amount: Double): String =
        NumberFormatter.formatCurrency(amount, locale = LocaleHelper.currentLocale())

    companion object {
        fun fromEntity(entity: AutoAllocationRuleEntity, goalName: String = ""): AutoAllocationRule {
            val categoryIds = try {
                val arr = JSONArray(entity.incomeCategoryIds)
                (0 until arr.length()).map { arr.getString(it) }
            } catch (_: Exception) {
                emptyList()
            }
            val catBasedIds = try {
                val arr = JSONArray(entity.categoryBasedCategoryIds)
                (0 until arr.length()).map { arr.getString(it) }
            } catch (_: Exception) {
                emptyList()
            }

            return AutoAllocationRule(
                id = entity.id, goalId = entity.goalId, goalName = goalName,
                triggerType = entity.trigger, triggerParams = AllocationTriggerParams.fromJson(entity.triggerParams),
                allocationType = entity.valueType, allocationValue = entity.allocationValue,
                isEnabled = entity.isEnabled, createdAt = Instant.ofEpochMilli(entity.createdAt),
                updatedAt = Instant.ofEpochMilli(entity.updatedAt), sourceAccountId = entity.sourceAccountId,
                confirmationMode = ConfirmationMode.fromString(entity.confirmationMode),
                incomeCategoryIds = categoryIds, minIncomeAmount = entity.minIncomeAmount,
                roundUpEnabled = entity.roundUpEnabled, roundUpIncrement = entity.roundUpIncrement, roundUpMode = entity.roundMode,
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
            roundUpIncrement = roundUpIncrement, roundUpMode = roundUpMode.value, scheduledFrequency = scheduledFrequency.value,
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
    val triggerDescription: String,
    val ruleId: String = "",
    val id: String = UUID.randomUUID().toString(), val createdAt: Instant = Instant.now()
)

data class AutoAllocationResult(
    val suggestions: List<AllocationSuggestion>, val autoExecuted: List<AllocationSuggestion>,
    val processedTransactionId: String?, val processedAmount: Double?
)
