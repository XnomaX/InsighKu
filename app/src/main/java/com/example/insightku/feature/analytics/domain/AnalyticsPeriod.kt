package com.example.insightku.feature.analytics.domain

import java.util.Calendar

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
    }
}

