package com.example.insightku.feature.home.domain

import com.example.insightku.core.data.model.Transaction
import com.example.insightku.feature.home.presentation.StreakMilestone
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * CalculateStreakUseCase — pure business logic for streak calculation.
 *
 * Extracted from DashboardViewModel.updateStateFromTransactions() to follow
 * MVVM architecture: ViewModels should not contain business logic.
 *
 * This use case is a suspend function (not a Flow) because it computes
 * a snapshot result from the current transaction data and user preferences.
 */
class CalculateStreakUseCase @Inject constructor(
    private val prefs: StreakPreferences
) {
    /**
     * Calculate streak data from transactions and user preferences.
     *
     * @param transactions All transactions (used to determine tracked days)
     * @return StreakResult containing streak values and any preference updates needed
     */
    suspend operator fun invoke(transactions: List<Transaction>): StreakResult {
        val dayFmt = SimpleDateFormat("yyyyMMdd", Locale.US)
        val trackedDayKeys: Set<String> = transactions.map { dayFmt.format(Date(it.date)) }.toSet()

        fun dayKey(cal: Calendar): String = dayFmt.format(cal.time)

        val today = Calendar.getInstance()
        val todayKey = dayKey(today)
        val hasTrackedToday = todayKey in trackedDayKeys

        // Current streak — walk backwards from today
        var streak = 0
        val check = today.clone() as Calendar
        if (!hasTrackedToday) check.add(Calendar.DAY_OF_YEAR, -1)
        while (dayKey(check) in trackedDayKeys) {
            streak++
            check.add(Calendar.DAY_OF_YEAR, -1)
        }

        // Best streak — scan all tracked days
        val sortedDays = trackedDayKeys.sorted()
        var bestStreak = 0
        var runStreak = 0
        var prevCal: Calendar? = null
        for (key in sortedDays) {
            val cal = Calendar.getInstance().apply {
                time = SimpleDateFormat("yyyyMMdd", Locale.US).parse(key) ?: return@apply
            }
            if (prevCal == null) {
                runStreak = 1
            } else {
                val prev = prevCal.clone() as Calendar
                prev.add(Calendar.DAY_OF_YEAR, 1)
                runStreak = if (dayKey(prev) == key) runStreak + 1 else 1
            }
            if (runStreak > bestStreak) bestStreak = runStreak
            prevCal = cal
        }

        // Read current prefs
        val freezeCount = prefs.freezeCount.first()
        val lastFreezeDate = prefs.lastFreezeDate.first()
        val isPerfect = prefs.isPerfectStreak.first()
        val streakGoal = prefs.streakGoal.first()
        val repairAvail = prefs.repairAvailable.first()
        val repairExpiry = prefs.repairExpiry.first()

        // Yesterday key — used to detect missed day
        val yesterday = today.clone() as Calendar
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayKey = dayKey(yesterday)

        // Auto-consume freeze if yesterday was missed and freeze available
        var updatedFreezeCount = freezeCount
        var updatedPerfect = isPerfect
        var repairAvailable = repairAvail
        var repairExpiryMs = repairExpiry

        val missedYesterday = yesterdayKey !in trackedDayKeys && streak == 0 && !hasTrackedToday
        if (missedYesterday && updatedFreezeCount > 0 && lastFreezeDate != yesterdayKey) {
            updatedFreezeCount--
            updatedPerfect = false
            // Return preference update actions for ViewModel to execute
        } else if (streak == 0 && !hasTrackedToday) {
            // Repair is offered as a fallback whenever the streak is broken and no freeze was
            // consumed above (freeze takes precedence via the preceding branch).
            if (!repairAvailable) {
                repairExpiryMs = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
                repairAvailable = true
            }
        }

        // Expire repair if window passed
        if (repairAvailable && System.currentTimeMillis() > repairExpiryMs) {
            repairAvailable = false
        }

        // Perfect streak — true only if streak > 0 and no freeze ever used
        if (hasTrackedToday && streak > 0 && lastFreezeDate.isEmpty()) {
            updatedPerfect = true
        }

        // Award freeze at milestones (7, 30 days) — max 2
        if (hasTrackedToday && (streak == 7 || streak == 30) && updatedFreezeCount < 2) {
            updatedFreezeCount = (updatedFreezeCount + 1).coerceAtMost(2)
        }

        val milestone = StreakMilestone.forStreak(streak)

        return StreakResult(
            currentStreak = streak,
            bestStreak = bestStreak,
            hasTrackedToday = hasTrackedToday,
            freezeCount = updatedFreezeCount,
            isPerfectStreak = updatedPerfect,
            streakGoal = streakGoal,
            repairAvailable = repairAvailable,
            repairExpiryMs = repairExpiryMs,
            streakMilestone = milestone,
            // Preference updates that should be persisted
            prefUpdates = buildList {
                if (missedYesterday && freezeCount > updatedFreezeCount) {
                    add(PrefUpdate.FreezeConsumed(updatedFreezeCount, yesterdayKey))
                }
                if (repairAvailable && !repairAvail) {
                    add(PrefUpdate.RepairEnabled(repairExpiryMs))
                }
                if (!repairAvailable && repairAvail) {
                    add(PrefUpdate.RepairDisabled)
                }
                if (updatedPerfect && !isPerfect) {
                    add(PrefUpdate.PerfectStreakEnabled)
                }
                if (updatedFreezeCount > freezeCount) {
                    add(PrefUpdate.FreezeAwarded(updatedFreezeCount))
                }
            }
        )
    }

    /**
     * Result of streak calculation.
     */
    data class StreakResult(
        val currentStreak: Int,
        val bestStreak: Int,
        val hasTrackedToday: Boolean,
        val freezeCount: Int,
        val isPerfectStreak: Boolean,
        val streakGoal: Int,
        val repairAvailable: Boolean,
        val repairExpiryMs: Long,
        val streakMilestone: StreakMilestone?,
        val prefUpdates: List<PrefUpdate>
    )

    /**
     * Preference updates that should be persisted by the ViewModel.
     * This keeps the use case pure — it computes what should change,
     * but the ViewModel executes the actual persistence.
     */
    sealed class PrefUpdate {
        data class FreezeConsumed(val newCount: Int, val lastFreezeDate: String) : PrefUpdate()
        data class RepairEnabled(val expiryMs: Long) : PrefUpdate()
        data object RepairDisabled : PrefUpdate()
        data object PerfectStreakEnabled : PrefUpdate()
        data class FreezeAwarded(val newCount: Int) : PrefUpdate()
    }
}
