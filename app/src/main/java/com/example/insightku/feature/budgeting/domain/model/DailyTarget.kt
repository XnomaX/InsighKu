package com.example.insightku.feature.budgeting.domain.model

import com.example.insightku.feature.budgeting.data.model.DailyTargetEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Domain model for the daily saving target.
 */
data class DailyTarget(
    val isSet: Boolean,
    val targetAmount: Double,
    val currentAmount: Double,
    val date: LocalDate
) {
    val progressPercent: Double
        get() = if (targetAmount > 0) ((currentAmount / targetAmount) * 100).coerceIn(0.0, 100.0) else 0.0

    val remainingAmount: Double
        get() = (targetAmount - currentAmount).coerceAtLeast(0.0)

    val isCompleted: Boolean
        get() = currentAmount >= targetAmount

    val progressDescription: String
        get() = "$${"%.2f".format(currentAmount)} of $${"%.2f".format(targetAmount)} saved"

    companion object {
        fun empty() = DailyTarget(
            isSet = false,
            targetAmount = 0.0,
            currentAmount = 0.0,
            date = LocalDate.now()
        )

        fun fromEntity(entity: DailyTargetEntity?, currentAmount: Double = 0.0): DailyTarget {
            return if (entity != null) {
                DailyTarget(
                    isSet = entity.isSet,
                    targetAmount = entity.targetAmount,
                    currentAmount = currentAmount,
                    date = Instant.ofEpochMilli(entity.date)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                )
            } else {
                empty()
            }
        }
    }

    fun toEntity(): DailyTargetEntity {
        return DailyTargetEntity(
            targetAmount = targetAmount,
            date = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
