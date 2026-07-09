package com.example.insightku.feature.analytics.domain

import com.example.insightku.core.i18n.AnalyticsStrings
import java.util.Calendar

/**
 * Represents the selectable period type for analytics view.
 * These are user-selectable time scopes for viewing analytics.
 */
enum class AnalyticsPeriodType {
    WEEKLY,
    MONTHLY,
    ANNUAL;

    /** Locale-aware display name resolved at call time. */
    val displayName: String
        get() = when (this) {
            WEEKLY -> AnalyticsStrings.periodWeekly()
            MONTHLY -> AnalyticsStrings.periodMonthly()
            ANNUAL -> AnalyticsStrings.periodAnnual()
        }
}

/**
 * Half-open time window `[startMs, endMs)` used to scope analytics derivations.
 *
 * Uses `java.util.Calendar` (not `java.time`) so it runs on `minSdk 24` without core-library
 * desugaring, matching the streak math already in `DashboardViewModel`. `nowMillis` is passed in
 * rather than read from the system clock, keeping the engine deterministic for unit/property tests.
 */
data class AnalyticsPeriod(val startMs: Long, val endMs: Long) {

    /** True when [ms] falls in `[startMs, endMs)`. */
    fun contains(ms: Long): Boolean = ms in startMs until endMs

    companion object {
        private fun monthStart(nowMillis: Long): Calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        /** The current calendar month: first-of-month 00:00 up to (but excluding) next first-of-month. */
        fun currentMonth(nowMillis: Long): AnalyticsPeriod {
            val start = monthStart(nowMillis)
            val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            return AnalyticsPeriod(start.timeInMillis, end.timeInMillis)
        }

        /** The month immediately before the current one — used for spotlight deltas. */
        fun previousMonth(nowMillis: Long): AnalyticsPeriod {
            val curStart = monthStart(nowMillis)
            val prevStart = (curStart.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            return AnalyticsPeriod(prevStart.timeInMillis, curStart.timeInMillis)
        }

        /** A window spanning the trailing [months] calendar months, ending at the end of this month. */
        fun trailingMonths(nowMillis: Long, months: Int): AnalyticsPeriod {
            val cur = currentMonth(nowMillis)
            val start = Calendar.getInstance().apply {
                timeInMillis = cur.startMs
                add(Calendar.MONTH, -(months - 1).coerceAtLeast(0))
            }
            return AnalyticsPeriod(start.timeInMillis, cur.endMs)
        }

        // ── Weekly ────────────────────────────────────────────────────────────

        /** Current ISO week: Monday 00:00 to next Monday 00:00. */
        fun currentWeek(nowMillis: Long): AnalyticsPeriod {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            val end = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 7) }
            return AnalyticsPeriod(cal.timeInMillis, end.timeInMillis)
        }

        /** Previous ISO week. */
        fun previousWeek(nowMillis: Long): AnalyticsPeriod {
            val cur = currentWeek(nowMillis)
            return AnalyticsPeriod(cur.startMs - 7 * 86_400_000L, cur.startMs)
        }

        /** Trailing N weeks ending at the end of the current week. */
        fun trailingWeeks(nowMillis: Long, weeks: Int): AnalyticsPeriod {
            val cur = currentWeek(nowMillis)
            return AnalyticsPeriod(cur.startMs - (weeks - 1).coerceAtLeast(0) * 7 * 86_400_000L, cur.endMs)
        }

        // ── Annual ───────────────────────────────────────────────────────────

        /** Current calendar year: Jan 1 00:00 to next Jan 1 00:00. */
        fun currentYear(nowMillis: Long): AnalyticsPeriod {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val end = (cal.clone() as Calendar).apply { add(Calendar.YEAR, 1) }
            return AnalyticsPeriod(cal.timeInMillis, end.timeInMillis)
        }

        /** Previous calendar year. */
        fun previousYear(nowMillis: Long): AnalyticsPeriod {
            val cur = currentYear(nowMillis)
            return AnalyticsPeriod(cur.startMs - 366 * 86_400_000L, cur.startMs)
        }

        /** Trailing N years ending at the end of the current year. */
        fun trailingYears(nowMillis: Long, years: Int): AnalyticsPeriod {
            val cur = currentYear(nowMillis)
            return AnalyticsPeriod(cur.startMs - (years - 1).coerceAtLeast(0) * 365 * 86_400_000L, cur.endMs)
        }
    }
}

