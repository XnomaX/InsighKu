package com.example.insightku.feature.planning.goal.domain

import com.example.insightku.feature.planning.goal.data.model.ContributionType
import com.example.insightku.feature.planning.goal.domain.model.Contribution
import java.time.Instant
import kotlin.math.abs

/**
 * Aggregated statistics over a goal's contributions.
 * Moved out of the presentation layer so the math is domain-owned and testable.
 */
data class ContributionSummary(
    val totalCount: Int,
    val latest: Contribution?,
    val average: Double,
    val lastActivity: Instant?
)

/**
 * Pure contribution/goal-progress calculations, extracted from GoalDetailViewModel.
 */
object GoalContributionStats {

    /** Summarize a goal's contributions: count of additions, latest, average addition, last activity. */
    fun summarize(contributions: List<Contribution>): ContributionSummary {
        if (contributions.isEmpty()) return ContributionSummary(0, null, 0.0, null)
        val additions = contributions.filter { it.isAddition }
        val totalCount = additions.size
        val latest = contributions.sortedByDescending { it.createdAt }.firstOrNull()
        val lastActivity = latest?.createdAt
        val average = if (additions.isNotEmpty()) additions.sumOf { abs(it.amount) } / additions.size else 0.0
        return ContributionSummary(totalCount = totalCount, latest = latest, average = average, lastActivity = lastActivity)
    }

    /**
     * Progress percentage toward [targetAmount] as of a given contribution's time,
     * counting only non-withdrawal contributions up to that point.
     */
    fun progressAtTime(contribution: Contribution, targetAmount: Double, allContributions: List<Contribution>): Double {
        val contributionsUpTo = allContributions.filter { it.createdAt <= contribution.createdAt && it.type != ContributionType.WITHDRAWAL }
        val amount = contributionsUpTo.sumOf { abs(it.amount) }
        return if (targetAmount > 0) (amount / targetAmount) * 100 else 0.0
    }
}
