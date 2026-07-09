package com.example.insightku.core.i18n

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Centralized locale-aware date and time formatter.
 *
 * This is the **single source of truth** for all date and time formatting in InsightKu.
 * Every screen should use these methods instead of custom formatting logic.
 *
 * Supports:
 *   - Indonesian date formatting (9 Juli 2026)
 *   - English date formatting (July 9, 2026)
 *   - Short date formats
 *   - Time formatting (12h/24h based on locale)
 *   - Full date-time formatting
 *   - Calendar labels (Today, Yesterday, day names)
 *   - Date ranges
 *   - Future relative dates
 *
 * Performance: Formatter instances are cached per (pattern, locale) pair to avoid
 * repeated allocation. Safe for LazyColumn and LazyGrid.
 */
object DateFormatter {

    // ══════════════════════════════════════════════════════════════════════════════
    // FORMATTER CACHE
    // ══════════════════════════════════════════════════════════════════════════════

    private val formatterCache = ConcurrentHashMap<String, SimpleDateFormat>()

    private fun getFormatter(pattern: String, locale: Locale): SimpleDateFormat {
        val key = "$pattern|${locale.language}"
        return formatterCache.getOrPut(key) { SimpleDateFormat(pattern, locale) }
    }

    /**
     * Synchronized formatting to ensure thread safety with cached SimpleDateFormat instances.
     */
    private fun formatWith(pattern: String, locale: Locale, timestamp: Long): String {
        val formatter = getFormatter(pattern, locale)
        synchronized(formatter) {
            return formatter.format(Date(timestamp))
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // FULL DATE FORMATTING
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * Format a timestamp to full date.
     *
     * Indonesian: 9 Juli 2026
     * English:    July 9, 2026
     */
    fun formatFullDate(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        val pattern = if (locale.language == "id") "d MMMM yyyy" else "MMMM d, yyyy"
        return formatWith(pattern, locale, timestamp)
    }

    /**
     * Format a timestamp to short date.
     *
     * Indonesian: 09 Jul 2026
     * English:    Jul 9, 2026
     */
    fun formatShortDate(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        val pattern = if (locale.language == "id") "dd MMM yyyy" else "MMM d, yyyy"
        return formatWith(pattern, locale, timestamp)
    }

    /**
     * Format a timestamp to numeric date.
     *
     * Indonesian: 09/07/2026
     * English:    07/09/2026
     */
    fun formatNumericDate(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        val pattern = if (locale.language == "id") "dd/MM/yyyy" else "MM/dd/yyyy"
        return formatWith(pattern, locale, timestamp)
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // TIME FORMATTING
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * Format time in 24-hour format.
     *
     * Example: 08:30, 21:45
     */
    fun formatTime24h(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        return formatWith("HH:mm", locale, timestamp)
    }

    /**
     * Format time in 24-hour format with seconds.
     *
     * Example: 08:30:15, 21:45:00
     */
    fun formatTimeFull(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        return formatWith("HH:mm:ss", locale, timestamp)
    }

    /**
     * Format date and time with seconds.
     *
     * Indonesian: 9 Juli 2026, 08:30:15
     * English:    July 9, 2026, 8:30:15 AM
     */
    fun formatDateTimeFull(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        val pattern = if (locale.language == "id") "d MMMM yyyy, HH:mm:ss" else "MMMM d, yyyy, h:mm:ss a"
        return formatWith(pattern, locale, timestamp)
    }

    /**
     * Format time in 12-hour format.
     *
     * Example: 8:30 AM, 9:45 PM
     */
    fun formatTime12h(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        return formatWith("h:mm a", locale, timestamp)
    }

    /**
     * Format time based on locale preference.
     *
     * Indonesian: 08:30 (24h default)
     * English:    8:30 AM (12h default)
     */
    fun formatTime(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        val pattern = if (locale.language == "id") "HH:mm" else "h:mm a"
        return formatWith(pattern, locale, timestamp)
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // DATE-TIME FORMATTING
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * Format a timestamp to full date and time.
     *
     * Indonesian: 9 Juli 2026, 08:30
     * English:    July 9, 2026, 8:30 AM
     */
    fun formatDateTime(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        val pattern = if (locale.language == "id") "d MMMM yyyy, HH:mm" else "MMMM d, yyyy, h:mm a"
        return formatWith(pattern, locale, timestamp)
    }

    /**
     * Format a timestamp to short date and time.
     *
     * Indonesian: 09 Jul 2026, 08:30
     * English:    Jul 9, 2026, 8:30 AM
     */
    fun formatShortDateTime(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        val pattern = if (locale.language == "id") "dd MMM yyyy, HH:mm" else "MMM d, yyyy, h:mm a"
        return formatWith(pattern, locale, timestamp)
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // CALENDAR LABELS
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * Get localized "Today" label.
     *
     * Indonesian: Hari Ini
     * English:    Today
     */
    fun getTodayLabel(locale: Locale = Locale.getDefault()): String {
        return if (locale.language == "id") "Hari Ini" else "Today"
    }

    /**
     * Get localized "Tomorrow" label.
     *
     * Indonesian: Besok
     * English:    Tomorrow
     */
    fun getTomorrowLabel(locale: Locale = Locale.getDefault()): String {
        return if (locale.language == "id") "Besok" else "Tomorrow"
    }

    /**
     * Get localized "Yesterday" label.
     *
     * Indonesian: Kemarin
     * English:    Yesterday
     */
    fun getYesterdayLabel(locale: Locale = Locale.getDefault()): String {
        return if (locale.language == "id") "Kemarin" else "Yesterday"
    }

    /**
     * Get localized day of week name.
     *
     * Indonesian: Senin, Selasa, Rabu, Kamis, Jumat, Sabtu, Minggu
     * English:    Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday
     */
    fun getDayOfWeek(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        return formatWith("EEEE", locale, timestamp)
    }

    /**
     * Get localized month name.
     *
     * Indonesian: Januari, Februari, Maret, April, Mei, Juni, Juli, Agustus, September, Oktober, November, Desember
     * English:    January, February, March, April, May, June, July, August, September, October, November, December
     */
    fun getMonthName(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        return formatWith("MMMM", locale, timestamp)
    }

    /**
     * Get short month name.
     *
     * Indonesian: Jan, Feb, Mar, Apr, Mei, Jun, Jul, Ags, Sep, Okt, Nov, Des
     * English:    Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec
     */
    fun getShortMonthName(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        return formatWith("MMM", locale, timestamp)
    }

    /**
     * Format a timestamp to month and year.
     *
     * Indonesian: Juli 2026
     * English:    July 2026
     */
    fun formatMonthYear(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        return formatWith("MMMM yyyy", locale, timestamp)
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // DATE RANGES
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * Format a date range within the same month and year.
     *
     * Indonesian: 1–31 Juli 2026
     * English:    July 1–31, 2026
     */
    fun formatDateRange(startTimestamp: Long, endTimestamp: Long, locale: Locale = Locale.getDefault()): String {
        val startDate = Instant.ofEpochMilli(startTimestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        val endDate = Instant.ofEpochMilli(endTimestamp).atZone(ZoneId.systemDefault()).toLocalDate()

        return if (startDate.month == endDate.month && startDate.year == endDate.year) {
            if (locale.language == "id") {
                "${startDate.dayOfMonth}–${endDate.dayOfMonth} ${getMonthName(startTimestamp, locale)} ${startDate.year}"
            } else {
                "${getMonthName(startTimestamp, locale)} ${startDate.dayOfMonth}–${endDate.dayOfMonth}, ${startDate.year}"
            }
        } else {
            "${formatShortDate(startTimestamp, locale)} – ${formatShortDate(endTimestamp, locale)}"
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // RELATIVE TIME (PAST)
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * Format a past timestamp as relative time.
     *
     * Indonesian: Baru saja, 1 menit lalu, 5 menit lalu, 1 jam lalu, Kemarin, 2 hari lalu, 1 minggu lalu, 1 bulan lalu, 1 tahun lalu
     * English:    Just now, 1 minute ago, 5 minutes ago, 1 hour ago, Yesterday, 2 days ago, 1 week ago, 1 month ago, 1 year ago
     */
    fun formatRelativeTime(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        val now = System.currentTimeMillis()
        val delta = now - timestamp

        if (delta < 0) {
            return formatFutureRelative(timestamp, locale)
        }

        val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
        val hours = TimeUnit.MILLISECONDS.toHours(delta)
        val days = TimeUnit.MILLISECONDS.toDays(delta)
        val weeks = days / 7
        val months = days / 30

        return when {
            minutes < 1 -> if (locale.language == "id") "Baru saja" else "Just now"
            minutes < 60 -> if (minutes == 1L) {
                if (locale.language == "id") "1 menit lalu" else "1 minute ago"
            } else {
                if (locale.language == "id") "$minutes menit lalu" else "$minutes minutes ago"
            }
            hours < 2 -> if (locale.language == "id") "1 jam lalu" else "1 hour ago"
            hours < 24 -> if (locale.language == "id") "$hours jam lalu" else "$hours hours ago"
            days == 1L -> getYesterdayLabel(locale)
            days < 7 -> if (locale.language == "id") "$days hari lalu" else "$days days ago"
            weeks < 4 -> if (weeks == 1L) {
                if (locale.language == "id") "1 minggu lalu" else "1 week ago"
            } else {
                if (locale.language == "id") "$weeks minggu lalu" else "$weeks weeks ago"
            }
            months < 12 -> if (months == 1L) {
                if (locale.language == "id") "1 bulan lalu" else "1 month ago"
            } else {
                if (locale.language == "id") "$months bulan lalu" else "$months months ago"
            }
            else -> {
                val years = months / 12
                if (years == 1L) {
                    if (locale.language == "id") "1 tahun lalu" else "1 year ago"
                } else {
                    if (locale.language == "id") "$years tahun lalu" else "$years years ago"
                }
            }
        }
    }

    /**
     * Format a past timestamp as short relative time (compact).
     *
     * Uses universal compact abbreviations: now, 1m, 2h, 3d, 1w, 2mo, 1y
     * These are internationally recognized and consistent across locales.
     */
    fun formatShortRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val delta = now - timestamp

        if (delta < 0) return "now"

        val seconds = TimeUnit.MILLISECONDS.toSeconds(delta)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
        val hours = TimeUnit.MILLISECONDS.toHours(delta)
        val days = TimeUnit.MILLISECONDS.toDays(delta)
        val weeks = days / 7
        val months = days / 30
        val years = days / 365

        return when {
            seconds < 60 -> "now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days < 7 -> "${days}d"
            weeks < 4 -> "${weeks}w"
            months < 12 -> "${months}mo"
            else -> "${years}y"
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // FUTURE RELATIVE DATES
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * Format a future timestamp as relative time.
     *
     * Indonesian: Dalam 5 menit, Besok, Dalam 2 hari, Dalam 1 minggu
     * English:    In 5 minutes, Tomorrow, In 2 days, In 1 week
     */
    fun formatFutureRelative(futureTimestamp: Long, locale: Locale = Locale.getDefault()): String {
        val now = System.currentTimeMillis()
        val delta = futureTimestamp - now

        if (delta < 0) {
            return formatRelativeTime(futureTimestamp, locale)
        }

        val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
        val hours = TimeUnit.MILLISECONDS.toHours(delta)
        val days = TimeUnit.MILLISECONDS.toDays(delta)
        val weeks = days / 7
        val months = days / 30

        return when {
            minutes < 1 -> getTodayLabel(locale)
            minutes < 60 -> if (locale.language == "id") "Dalam $minutes menit" else "In $minutes minutes"
            hours < 2 -> if (locale.language == "id") "Dalam 1 jam" else "In 1 hour"
            hours < 24 -> if (locale.language == "id") "Dalam $hours jam" else "In $hours hours"
            days == 1L -> getTomorrowLabel(locale)
            days < 7 -> if (locale.language == "id") "Dalam $days hari" else "In $days days"
            weeks < 4 -> if (weeks == 1L) {
                if (locale.language == "id") "Dalam 1 minggu" else "In 1 week"
            } else {
                if (locale.language == "id") "Dalam $weeks minggu" else "In $weeks weeks"
            }
            months < 12 -> if (months == 1L) {
                if (locale.language == "id") "Dalam 1 bulan" else "In 1 month"
            } else {
                if (locale.language == "id") "Dalam $months bulan" else "In $months months"
            }
            else -> {
                val years = months / 12
                if (years == 1L) {
                    if (locale.language == "id") "Dalam 1 tahun" else "In 1 year"
                } else {
                    if (locale.language == "id") "Dalam $years tahun" else "In $years years"
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * Get the current locale from the application context.
     * Uses the AppCompat per-app language setting.
     */
    fun getCurrentLocale(context: Context): Locale {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (!currentLocales.isEmpty) {
            return currentLocales[0] ?: Locale.getDefault()
        }
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        } ?: Locale.getDefault()
    }

    /** Check if a timestamp is today. */
    fun isToday(timestamp: Long): Boolean {
        val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        return date == LocalDate.now()
    }

    /** Check if a timestamp is yesterday. */
    fun isYesterday(timestamp: Long): Boolean {
        val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        return date == LocalDate.now().minusDays(1)
    }

    /** Check if a timestamp is tomorrow. */
    fun isTomorrow(timestamp: Long): Boolean {
        val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        return date == LocalDate.now().plusDays(1)
    }
}
