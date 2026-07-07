package com.example.insightku.feature.planning.goal.domain.model

import com.example.insightku.feature.planning.goal.data.model.AllocationTriggerType
import com.example.insightku.feature.planning.goal.data.model.AllocationValueType
import com.example.insightku.feature.planning.goal.data.model.AutoAllocationRuleEntity
import com.example.insightku.feature.planning.goal.data.model.ConfirmationMode
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
    val scheduledDayOfWeek: Int = 1, val scheduledDayOfMonth: Int = 1, val lastExecutedAt: Instant? = null
) {
    val description: String get() = when (triggerType) {
        AllocationTriggerType.INCOME_RECEIVED -> {
            val base = if (allocationType == AllocationValueType.PERCENT) "Save ${allocationValue.toInt()}% of income" else "Save ${formatAmount(allocationValue)} from income"
            if (minIncomeAmount > 0) "$base (min ${formatAmount(minIncomeAmount)})" else base
        }
        AllocationTriggerType.SPENDING_CATEGORY -> if (allocationType == AllocationValueType.PERCENT) "Save ${allocationValue.toInt()}% from category spending" else "Save ${formatAmount(allocationValue)} from category spending"
        AllocationTriggerType.ROUND_UP -> "Round up to nearest ${formatAmount(roundUpIncrement)}"
        AllocationTriggerType.DAILY -> if (allocationType == AllocationValueType.PERCENT) "Save ${allocationValue.toInt()}% daily" else "Save ${formatAmount(allocationValue)} daily"
        AllocationTriggerType.WEEKLY -> if (allocationType == AllocationValueType.PERCENT) "Save ${allocationValue.toInt()}% weekly" else "Save ${formatAmount(allocationValue)} weekly"
        AllocationTriggerType.BIWEEKLY -> if (allocationType == AllocationValueType.PERCENT) "Save ${allocationValue.toInt()}% biweekly" else "Save ${formatAmount(allocationValue)} biweekly"
        AllocationTriggerType.MONTHLY -> if (allocationType == AllocationValueType.PERCENT) "Save ${allocationValue.toInt()}% monthly" else "Save ${formatAmount(allocationValue)} monthly"
        AllocationTriggerType.BALANCE_ABOVE -> {
            val thresh = triggerParams?.threshold ?: 0.0
            if (allocationType == AllocationValueType.PERCENT) "Save ${allocationValue.toInt()}% when balance > ${formatAmount(thresh)}" else "Save ${formatAmount(allocationValue)} when balance > ${formatAmount(thresh)}"
        }
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

    private fun formatAmount(amount: Double): String = "Rp ${"%,.0f".format(amount).replace(",", ".")}"

    companion object {
        fun fromEntity(entity: AutoAllocationRuleEntity, goalName: String = ""): AutoAllocationRule {
            val categoryIds = try {
                val arr = JSONArray(entity.incomeCategoryIds)
                (0 until arr.length()).map { arr.getString(it) }
            } catch (e: Exception) { emptyList() }

            return AutoAllocationRule(
                id = entity.id, goalId = entity.goalId, goalName = goalName,
                triggerType = entity.trigger, triggerParams = AllocationTriggerParams.fromJson(entity.triggerParams),
                allocationType = entity.valueType, allocationValue = entity.allocationValue,
                isEnabled = entity.isEnabled, createdAt = Instant.ofEpochMilli(entity.createdAt),
                updatedAt = Instant.ofEpochMilli(entity.updatedAt),
                sourceAccountId = entity.sourceAccountId,
                confirmationMode = ConfirmationMode.fromString(entity.confirmationMode),
                incomeCategoryIds = categoryIds, minIncomeAmount = entity.minIncomeAmount,
                roundUpEnabled = entity.roundUpEnabled, roundUpIncrement = entity.roundUpIncrement,
                scheduledFrequency = ScheduledFrequency.fromString(entity.scheduledFrequency),
                scheduledDayOfWeek = entity.scheduledDayOfWeek, scheduledDayOfMonth = entity.scheduledDayOfMonth,
                lastExecutedAt = if (entity.lastExecutedAt > 0) Instant.ofEpochMilli(entity.lastExecutedAt) else null
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
            lastExecutedAt = lastExecutedAt?.toEpochMilli() ?: 0L
        )
    }
}

data class AllocationSuggestion(
    val goalId: String, val goalName: String,
    val amount: Double, val sourceAccountId: String, val sourceAccountName: String,
    val triggerDescription: String, val ruleDescription: String,
    val id: String = UUID.randomUUID().toString(), val createdAt: Instant = Instant.now()
)

data class AutoAllocationResult(
    val suggestions: List<AllocationSuggestion>, val autoExecuted: List<AllocationSuggestion>,
    val processedTransactionId: String?, val processedAmount: Double?
)
