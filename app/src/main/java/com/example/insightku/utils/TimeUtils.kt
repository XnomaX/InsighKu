package com.example.insightku.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object TimeUtils {

    private val displayDateFormat = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)
    private val displayTimeFormat = SimpleDateFormat("h:mm a", Locale.ENGLISH)

    /**
     * Full relative time — verbose, for detail views.
     */
    fun toRelativeTime(timestamp: Long): String {
        val now   = System.currentTimeMillis()
        val delta = now - timestamp
        if (delta < 0) return displayTimeFormat.format(Date(timestamp))

        val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
        val hours   = TimeUnit.MILLISECONDS.toHours(delta)
        val days    = TimeUnit.MILLISECONDS.toDays(delta)

        return when {
            minutes < 1   -> "Just now"
            minutes < 60  -> if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
            hours   < 2   -> "1 hour ago"
            hours   < 24  -> "$hours hours ago"
            isYesterdayTimestamp(timestamp) -> "Yesterday, ${displayTimeFormat.format(Date(timestamp))}"
            days    < 7   -> "$days days ago"
            else          -> displayDateFormat.format(Date(timestamp))
        }
    }

    /**
     * Compact relative time — premium fintech style, no "ago" suffix.
     *
     * | Delta        | Output |
     * |--------------|--------|
     * | < 60s        | now    |
     * | < 60m        | 1m     |
     * | < 24h        | 2h     |
     * | < 7d         | 3d     |
     * | < 4w         | 1w     |
     * | < 12mo       | 1mo    |
     * | else         | 1y     |
     */
    fun toShortRelativeTime(timestamp: Long): String {
        val now   = System.currentTimeMillis()
        val delta = now - timestamp
        if (delta < 0) return "now"

        val seconds = TimeUnit.MILLISECONDS.toSeconds(delta)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
        val hours   = TimeUnit.MILLISECONDS.toHours(delta)
        val days    = TimeUnit.MILLISECONDS.toDays(delta)
        val weeks   = days / 7
        val months  = days / 30
        val years   = days / 365

        return when {
            seconds < 60  -> "now"
            minutes < 60  -> "${minutes}m"
            hours   < 24  -> "${hours}h"
            days    < 7   -> "${days}d"
            weeks   < 4   -> "${weeks}w"
            months  < 12  -> "${months}mo"
            else          -> "${years}y"
        }
    }

    /** Format full: "1 May 2025, 3:45 PM" */
    fun toFullDateTime(timestamp: Long): String =
        SimpleDateFormat("d MMM yyyy, h:mm a", Locale.ENGLISH).format(Date(timestamp))

    /** Format date only: "1 May 2025" */
    fun toDateOnly(timestamp: Long): String =
        displayDateFormat.format(Date(timestamp))

    /** Format time only: "3:45 PM" */
    fun toTimeOnly(timestamp: Long): String =
        displayTimeFormat.format(Date(timestamp))

    private fun isYesterdayTimestamp(timestamp: Long): Boolean {
        val cal       = Calendar.getInstance().apply { timeInMillis = timestamp }
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return cal.get(Calendar.YEAR)        == yesterday.get(Calendar.YEAR) &&
               cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
    }
}
