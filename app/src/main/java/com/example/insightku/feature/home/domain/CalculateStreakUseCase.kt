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
        val txDayKeys: Set<String> = transactions.map { dayFmt.format(Date(it.date)) }.toSet()

        fun dayKey(cal: Calendar): String = dayFmt.format(cal.time)

        val today = Calendar.getInstance()
        val todayKey = dayKey(today)
        val hasTrackedToday = todayKey in txDayKeys

        val yesterday = today.clone() as Calendar
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayKey = dayKey(yesterday)

        // Read current prefs
        val freezeCount = prefs.freezeCount.first()
        val lastFreezeDate = prefs.lastFreezeDate.first()
        val isPerfect = prefs.isPerfectStreak.first()
        val streakGoal = prefs.streakGoal.first()
        val repairAvail = prefs.repairAvailable.first()
        val repairExpiry = prefs.repairExpiry.first()
        val overrideDays = prefs.overrideDays.first()
        val awardedMilestones = prefs.awardedMilestones.first()

        // Determine if freeze should be consumed BEFORE computing final streak.
        // Freeze fires when yesterday was missed (no tx, no prior override), today not
        // yet tracked, and a freeze pass is available.
        var updatedFreezeCount = freezeCount
        var updatedPerfect = isPerfect
        var repairAvailable = repairAvail
        var repairExpiryMs = repairExpiry
        val newOverrides = mutableSetOf<String>()

        val needsFreeze = yesterdayKey !in txDayKeys && yesterdayKey !in overrideDays &&
            !hasTrackedToday && updatedFreezeCount > 0 && lastFreezeDate != yesterdayKey
        if (needsFreeze) {
            updatedFreezeCount--
            updatedPerfect = false
            newOverrides.add(yesterdayKey)
        }

        // Effective tracked days = transactions + persisted overrides + just-consumed freeze
        val effectiveDays = txDayKeys + overrideDays + newOverrides

        // Current streak — walk backwards from today (or yesterday if today not tracked)
        var streak = 0
        val check = today.clone() as Calendar
        if (!hasTrackedToday) check.add(Calendar.DAY_OF_YEAR, -1)
        while (dayKey(check) in effectiveDays) {
            streak++
            check.add(Calendar.DAY_OF_YEAR, -1)
        }

        // Repair: offered when streak is still broken after freeze attempt (or no freeze).
        // repairDayKey = the gap day the walk stopped at (what repair would fill).
        var repairDayKey: String? = null
        if (streak == 0 && !hasTrackedToday) {
            repairDayKey = dayKey(check)
            if (!repairAvailable) {
                repairExpiryMs = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
                repairAvailable = true
            }
        }

        // Expire repair if window passed
        if (repairAvailable && System.currentTimeMillis() > repairExpiryMs) {
            repairAvailable = false
        }

        // Best streak — scan all effective days
        val sortedDays = effectiveDays.sorted()
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

        // Perfect streak — true only if streak > 0 and no freeze ever used
        if (hasTrackedToday && streak > 0 && lastFreezeDate.isEmpty()) {
            updatedPerfect = true
        }

        // Award freeze at every multiple of 7 — cap 3 total, tracked via awardedMilestones
        var awardedMilestone: Int? = null
        if (hasTrackedToday && streak > 0 && streak % 7 == 0 && updatedFreezeCount < 3) {
            if (streak.toString() !in awardedMilestones) {
                updatedFreezeCount = (updatedFreezeCount + 1).coerceAtMost(3)
                awardedMilestone = streak
            }
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
            repairDayKey = repairDayKey,
            streakMilestone = milestone,
            prefUpdates = buildList {
                if (needsFreeze) {
                    add(PrefUpdate.FreezeConsumed(updatedFreezeCount, yesterdayKey))
                    add(PrefUpdate.OverrideDayAdded(yesterdayKey))
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
                if (awardedMilestone != null) {
                    add(PrefUpdate.MilestoneAwarded(awardedMilestone))
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
        val repairDayKey: String?,
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
        data class OverrideDayAdded(val dayKey: String) : PrefUpdate()
        data class RepairEnabled(val expiryMs: Long) : PrefUpdate()
        data object RepairDisabled : PrefUpdate()
        data object PerfectStreakEnabled : PrefUpdate()
        data class FreezeAwarded(val newCount: Int) : PrefUpdate()
        data class MilestoneAwarded(val milestone: Int) : PrefUpdate()
    }
}
