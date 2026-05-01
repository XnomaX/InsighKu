package com.example.insightku.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * TimeUtils — utilitas terpusat untuk formatting timestamp ke string human-readable.
 *
 * ROOT CAUSE (Issue 3):
 * DashboardViewModel mengisi TransactionItem.time dengan `t.date.toString()` yang
 * menghasilkan angka panjang (mis. "1777595336895"). UI langsung render angka ini.
 *
 * ARSITEKTUR:
 * Konversi terjadi di layer mapping (ViewModel/UseCase), bukan di UI.
 * UI hanya menerima String yang sudah siap tampil.
 * Composable tidak perlu tahu timestamp itu apa — cukup tampilkan String.
 *
 * CARA PAKAI:
 * ```kotlin
 * // Di ViewModel:
 * time = t.date.toRelativeTime()
 *
 * // Langsung:
 * TimeUtils.toRelativeTime(timestamp)
 * ```
 */
object TimeUtils {

    private val displayDateFormat = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)
    private val displayTimeFormat = SimpleDateFormat("h:mm a", Locale.ENGLISH)

    /**
     * Konversi [timestamp] (millis) ke string relatif yang human-readable.
     *
     * | Delta          | Output contoh        |
     * |----------------|----------------------|
     * | < 1 menit      | "Just now"           |
     * | < 1 jam        | "5 minutes ago"      |
     * | < 2 jam        | "1 hour ago"         |
     * | < 24 jam       | "3 hours ago"        |
     * | Kemarin        | "Yesterday, 3:45 PM" |
     * | < 7 hari       | "3 days ago"         |
     * | Lebih lama     | "12 Jan 2025"        |
     */
    fun toRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val delta = now - timestamp

        // Jika timestamp di masa depan (edge case: clock skew), tampilkan waktu saja
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
     * Format singkat untuk daftar: "Just now", "5m ago", "2h ago", "3d ago", "12 Jan"
     * Cocok untuk list yang dense agar tidak terlalu panjang.
     */
    fun toShortRelativeTime(timestamp: Long): String {
        val now   = System.currentTimeMillis()
        val delta = now - timestamp
        if (delta < 0) return displayTimeFormat.format(Date(timestamp))

        val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
        val hours   = TimeUnit.MILLISECONDS.toHours(delta)
        val days    = TimeUnit.MILLISECONDS.toDays(delta)

        return when {
            minutes < 1   -> "Just now"
            minutes < 60  -> "${minutes}m ago"
            hours   < 24  -> "${hours}h ago"
            days    < 7   -> "${days}d ago"
            else          -> SimpleDateFormat("d MMM", Locale.ENGLISH).format(Date(timestamp))
        }
    }

    /** Format full: "1 May 2025, 3:45 PM" */
    fun toFullDateTime(timestamp: Long): String {
        val fmt = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.ENGLISH)
        return fmt.format(Date(timestamp))
    }

    /** Format tanggal saja: "1 May 2025" */
    fun toDateOnly(timestamp: Long): String =
        displayDateFormat.format(Date(timestamp))

    /** Format waktu saja: "3:45 PM" */
    fun toTimeOnly(timestamp: Long): String =
        displayTimeFormat.format(Date(timestamp))

    private fun isYesterdayTimestamp(timestamp: Long): Boolean {
        val cal      = Calendar.getInstance().apply { timeInMillis = timestamp }
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return cal.get(Calendar.YEAR)       == yesterday.get(Calendar.YEAR) &&
               cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
    }
}
