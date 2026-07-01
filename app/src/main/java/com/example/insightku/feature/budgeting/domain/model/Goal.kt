package com.example.insightku.feature.budgeting.domain.model

import com.example.insightku.feature.budgeting.data.model.GoalEntity
import com.example.insightku.feature.budgeting.data.model.GoalStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Domain model representing a savings goal.
 */
data class Goal(
    val id: String,
    val name: String,
    val targetAmount: Double,
    val deadline: LocalDate?,
    val status: GoalStatus,
    val autoAllocate: Boolean,
    val allocationPriority: Int,
    val iconName: String,
    val color: String,
    val notes: String,
    val currentAmount: Double = 0.0,
    val linkedAccountIds: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val progressPercent: Double
        get() = if (targetAmount > 0) ((currentAmount / targetAmount) * 100).coerceIn(0.0, 100.0) else 0.0

    val remainingAmount: Double
        get() = (targetAmount - currentAmount).coerceAtLeast(0.0)

    val isCompleted: Boolean
        get() = status == GoalStatus.COMPLETED

    val isActive: Boolean
        get() = status == GoalStatus.ACTIVE

    val isPaused: Boolean
        get() = status == GoalStatus.PAUSED

    val isArchived: Boolean
        get() = status == GoalStatus.ARCHIVED

    val daysRemaining: Int?
        get() = deadline?.let {
            val today = LocalDate.now()
            if (it.isBefore(today)) 0 else java.time.temporal.ChronoUnit.DAYS.between(today, it).toInt()
        }

    val isOverdue: Boolean
        get() = deadline != null && deadline.isBefore(LocalDate.now()) && !isCompleted

    val isOnTrack: Boolean
        get() {
            val days = daysRemaining ?: return true
            if (days <= 0) return currentAmount >= targetAmount
            val daysPassed = java.time.temporal.ChronoUnit.DAYS.between(
                createdAt.atZone(ZoneId.systemDefault()).toLocalDate(),
                LocalDate.now()
            ).toInt()
            if (daysPassed <= 0) return true
            val totalDays = daysPassed + days
            val expectedProgress = (daysPassed.toDouble() / totalDays) * targetAmount
            return currentAmount >= expectedProgress * 0.8 // 80% threshold
        }

    companion object {
        fun fromEntity(entity: GoalEntity, currentAmount: Double = 0.0): Goal {
            return Goal(
                id = entity.id,
                name = entity.name,
                targetAmount = entity.targetAmount,
                deadline = entity.deadline?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                },
                status = entity.goalStatus,
                autoAllocate = entity.autoAllocate,
                allocationPriority = entity.allocationPriority,
                iconName = entity.iconName,
                color = entity.color,
                notes = entity.notes,
                currentAmount = currentAmount,
                createdAt = Instant.ofEpochMilli(entity.createdAt),
                updatedAt = Instant.ofEpochMilli(entity.updatedAt)
            )
        }
    }

    fun toEntity(): GoalEntity {
        return GoalEntity(
            id = id,
            name = name,
            targetAmount = targetAmount,
            deadline = deadline?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            status = status.value,
            autoAllocate = autoAllocate,
            allocationPriority = allocationPriority,
            iconName = iconName,
            color = color,
            notes = notes,
            createdAt = createdAt.toEpochMilli(),
            updatedAt = System.currentTimeMillis()
        )
    }
}

/**
 * Summary stats for goals overview.
 */
data class GoalSummary(
    val totalGoals: Int,
    val activeGoals: Int,
    val completedGoals: Int,
    val totalSaved: Double,
    val totalTarget: Double,
    val overallProgress: Double
)
