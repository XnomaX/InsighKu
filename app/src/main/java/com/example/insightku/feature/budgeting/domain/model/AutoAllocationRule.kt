package com.example.insightku.feature.budgeting.domain.model

import com.example.insightku.feature.budgeting.data.model.AllocationTriggerType
import com.example.insightku.feature.budgeting.data.model.AllocationValueType
import com.example.insightku.feature.budgeting.data.model.AutoAllocationRuleEntity
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

/**
 * Trigger parameters for auto-allocation rules.
 */
data class AllocationTriggerParams(
    val categoryId: String? = null,
    val accountId: String? = null,
    val threshold: Double? = null
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
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Auto-allocation rule domain model.
 */
data class AutoAllocationRule(
    val id: String,
    val goalId: String,
    val goalName: String = "",
    val triggerType: AllocationTriggerType,
    val triggerParams: AllocationTriggerParams?,
    val allocationType: AllocationValueType,
    val allocationValue: Double,
    val isEnabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val description: String
        get() = when (triggerType) {
            AllocationTriggerType.INCOME_RECEIVED -> {
                if (allocationType == AllocationValueType.PERCENT) {
                    "Save ${allocationValue.toInt()}% of income"
                } else {
                    "Save $${"%.2f".format(allocationValue)} from income"
                }
            }
            AllocationTriggerType.SPENDING_CATEGORY -> {
                if (allocationType == AllocationValueType.PERCENT) {
                    "Save ${allocationValue.toInt()}% from category spending"
                } else {
                    "Save $${"%.2f".format(allocationValue)} from category spending"
                }
            }
            AllocationTriggerType.DAILY -> {
                if (allocationType == AllocationValueType.PERCENT) {
                    "Save ${allocationValue.toInt()}% daily"
                } else {
                    "Save $${"%.2f".format(allocationValue)} daily"
                }
            }
            AllocationTriggerType.BALANCE_ABOVE -> {
                val thresh = triggerParams?.threshold ?: 0.0
                if (allocationType == AllocationValueType.PERCENT) {
                    "Save ${allocationValue.toInt()}% when balance > $${"%.0f".format(thresh)}"
                } else {
                    "Save $${"%.2f".format(allocationValue)} when balance > $${"%.0f".format(thresh)}"
                }
            }
        }

    companion object {
        fun fromEntity(entity: AutoAllocationRuleEntity, goalName: String = ""): AutoAllocationRule {
            return AutoAllocationRule(
                id = entity.id,
                goalId = entity.goalId,
                goalName = goalName,
                triggerType = entity.trigger,
                triggerParams = AllocationTriggerParams.fromJson(entity.triggerParams),
                allocationType = entity.valueType,
                allocationValue = entity.allocationValue,
                isEnabled = entity.isEnabled,
                createdAt = Instant.ofEpochMilli(entity.createdAt),
                updatedAt = Instant.ofEpochMilli(entity.updatedAt)
            )
        }
    }

    fun toEntity(): AutoAllocationRuleEntity {
        return AutoAllocationRuleEntity(
            id = id,
            goalId = goalId,
            triggerType = triggerType.value,
            triggerParams = triggerParams?.toJson(),
            allocationType = allocationType.value,
            allocationValue = allocationValue,
            isEnabled = isEnabled,
            createdAt = createdAt.toEpochMilli(),
            updatedAt = System.currentTimeMillis()
        )
    }
}

/**
 * Allocation suggestion pending user confirmation.
 * Created when an auto-allocation rule triggers.
 */
data class AllocationSuggestion(
    val id: String = UUID.randomUUID().toString(),
    val goalId: String,
    val goalName: String,
    val amount: Double,
    val sourceAccountId: String,
    val sourceAccountName: String,
    val triggerDescription: String,
    val ruleDescription: String,
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant = Instant.now().plusSeconds(86400) // 24 hours
) {
    val isExpired: Boolean
        get() = Instant.now().isAfter(expiresAt)
}

/**
 * Result of calculating auto-allocation suggestions.
 */
data class AutoAllocationResult(
    val suggestions: List<AllocationSuggestion>,
    val processedTransactionId: String?,
    val processedAmount: Double?
)
