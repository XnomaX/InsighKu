package com.example.insightku.core.utils

import com.example.insightku.core.i18n.DateFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object TimeUtils {

    /**
     * Whole calendar days from [fromMillis] to [toMillis], ignoring time of day.
     * Result is negative when [toMillis] is the earlier day.
     *
     * Prefer this over `(b - a) / 86_400_000`. Raw millisecond division truncates
     * toward zero, so a due date at midnight tomorrow viewed at 14:00 today
     * yields 0 instead of 1 — payments were reported "due today" a full day
     * early, and a transaction from yesterday 23:00 viewed at 01:00 missed the
     * YESTERDAY grouping. Flooring both instants to their local date first makes
     * the result depend on the calendar, not the clock.
     */
    fun daysBetween(fromMillis: Long, toMillis: Long): Int {
        val zone = ZoneId.systemDefault()
        val fromDate = Instant.ofEpochMilli(fromMillis).atZone(zone).toLocalDate()
        val toDate = Instant.ofEpochMilli(toMillis).atZone(zone).toLocalDate()
        return ChronoUnit.DAYS.between(fromDate, toDate).toInt()
    }

    /**
     * Full relative time — verbose, for detail views.
     * Delegates to [DateFormatter.formatRelativeTime] for locale-aware output.
     */
    fun toRelativeTime(timestamp: Long): String {
        return DateFormatter.formatRelativeTime(timestamp)
    }

    /**
     * Compact relative time — premium fintech style.
     * Delegates to [DateFormatter.formatShortRelativeTime].
     */
    fun toShortRelativeTime(timestamp: Long): String {
        return DateFormatter.formatShortRelativeTime(timestamp)
    }

    /** Format full: "1 May 2025, 3:45 PM" (locale-aware) */
    fun toFullDateTime(timestamp: Long): String =
        DateFormatter.formatShortDateTime(timestamp)

    /** Format date only: "1 May 2025" (locale-aware) */
    fun toDateOnly(timestamp: Long): String =
        DateFormatter.formatShortDate(timestamp)

    /** Format time only: "3:45 PM" (locale-aware) */
    fun toTimeOnly(timestamp: Long): String =
        DateFormatter.formatTime(timestamp)
}
