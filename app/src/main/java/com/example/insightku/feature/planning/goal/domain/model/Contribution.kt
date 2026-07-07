package com.example.insightku.feature.planning.goal.domain.model

import com.example.insightku.feature.planning.goal.data.model.ContributionEntity
import com.example.insightku.feature.planning.goal.data.model.ContributionType
import java.time.Instant

data class Contribution(
    val id: String, val goalId: String, val accountId: String, val amount: Double,
    val type: ContributionType, val transactionId: String?, val notes: String, val createdAt: Instant
) {
    val isWithdrawal: Boolean get() = type == ContributionType.WITHDRAWAL || amount < 0
    val isAddition: Boolean get() = type != ContributionType.WITHDRAWAL && amount > 0

    companion object {
        fun fromEntity(entity: ContributionEntity): Contribution {
            return Contribution(entity.id, entity.goalId, entity.accountId, entity.amount,
                entity.contributionType, entity.transactionId, entity.notes, Instant.ofEpochMilli(entity.createdAt))
        }
    }

    fun toEntity(): ContributionEntity {
        return ContributionEntity(id, goalId, accountId, amount, type.value, transactionId, notes, createdAt.toEpochMilli())
    }
}

data class GoalProgress(
    val goalId: String, val currentAmount: Double, val targetAmount: Double, val totalWithdrawn: Double,
    val progressPercent: Double, val remainingAmount: Double, val daysRemaining: Int?,
    val isOnTrack: Boolean, val averageDailyContribution: Double, val contributionCount: Int
) {
    val isCompleted: Boolean get() = currentAmount >= targetAmount
    companion object {
        fun empty(goalId: String) = GoalProgress(goalId, 0.0, 0.0, 0.0, 0.0, 0.0, null, true, 0.0, 0)
    }
}
