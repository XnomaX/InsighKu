package com.example.insightku.feature.budgeting.domain.model

import com.example.insightku.feature.budgeting.data.model.ContributionEntity
import com.example.insightku.feature.budgeting.data.model.ContributionType
import java.time.Instant

/**
 * Domain model representing a contribution to a goal.
 */
data class Contribution(
    val id: String,
    val goalId: String,
    val accountId: String,
    val amount: Double,
    val type: ContributionType,
    val transactionId: String?,
    val notes: String,
    val createdAt: Instant
) {
    val isWithdrawal: Boolean
        get() = type == ContributionType.WITHDRAWAL || amount < 0

    val isAddition: Boolean
        get() = type != ContributionType.WITHDRAWAL && amount > 0

    companion object {
        fun fromEntity(entity: ContributionEntity): Contribution {
            return Contribution(
                id = entity.id,
                goalId = entity.goalId,
                accountId = entity.accountId,
                amount = entity.amount,
                type = entity.contributionType,
                transactionId = entity.transactionId,
                notes = entity.notes,
                createdAt = Instant.ofEpochMilli(entity.createdAt)
            )
        }
    }

    fun toEntity(): ContributionEntity {
        return ContributionEntity(
            id = id,
            goalId = goalId,
            accountId = accountId,
            amount = amount,
            type = type.value,
            transactionId = transactionId,
            notes = notes,
            createdAt = createdAt.toEpochMilli()
        )
    }
}

/**
 * Goal progress metrics.
 */
data class GoalProgress(
    val goalId: String,
    val currentAmount: Double,
    val targetAmount: Double,
    val totalWithdrawn: Double,
    val progressPercent: Double,
    val remainingAmount: Double,
    val daysRemaining: Int?,
    val isOnTrack: Boolean,
    val averageDailyContribution: Double,
    val contributionCount: Int
) {
    val isCompleted: Boolean
        get() = currentAmount >= targetAmount

    companion object {
        fun empty(goalId: String) = GoalProgress(
            goalId = goalId,
            currentAmount = 0.0,
            targetAmount = 0.0,
            totalWithdrawn = 0.0,
            progressPercent = 0.0,
            remainingAmount = 0.0,
            daysRemaining = null,
            isOnTrack = true,
            averageDailyContribution = 0.0,
            contributionCount = 0
        )
    }
}
