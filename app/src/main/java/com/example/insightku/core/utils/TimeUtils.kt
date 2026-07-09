package com.example.insightku.core.utils

import com.example.insightku.core.i18n.DateFormatter
import java.util.Locale

object TimeUtils {

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
