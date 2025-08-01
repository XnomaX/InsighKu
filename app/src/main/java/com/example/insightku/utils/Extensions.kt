package com.example.insightku.utils

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*

// Context Extensions
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

// String Extensions
fun String.toTitleCase(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase() else it.toString() 
        }
    }
}

fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.truncate(maxLength: Int): String {
    return if (this.length <= maxLength) this else "${this.take(maxLength)}..."
}

// Double Extensions
fun Double.toCurrency(): String = CurrencyUtils.formatAmount(this)

fun Double.toCurrencyCompact(): String = CurrencyUtils.formatAmountCompact(this)

fun Double.toPercentage(): String = CurrencyUtils.formatPercentage(this)

// Long Extensions (for timestamps)
fun Long.toDateString(): String = DateUtils.formatDate(this)

fun Long.toTimeString(): String = DateUtils.formatTime(this)

fun Long.toDateTimeString(): String = DateUtils.formatDateTime(this)

fun Long.toRelativeDateString(): String = DateUtils.getRelativeDateString(this)

fun Long.isToday(): Boolean = DateUtils.isToday(this)

fun Long.isYesterday(): Boolean = DateUtils.isYesterday(this)

// List Extensions
fun <T> List<T>.safeGet(index: Int): T? {
    return if (index in 0 until size) this[index] else null
}

// Composable Extensions
@Composable
fun showToast(message: String) {
    val context = LocalContext.current
    context.showToast(message)
}

// Date Extensions
fun Date.toTimestamp(): Long = this.time

fun Date.isToday(): Boolean = DateUtils.isToday(this.time)

fun Date.formatToString(pattern: String = Constants.DATE_FORMAT_DISPLAY): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(this)
}

// Calendar Extensions
fun Calendar.setToStartOfDay(): Calendar {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    return this
}

fun Calendar.setToEndOfDay(): Calendar {
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59)
    set(Calendar.MILLISECOND, 999)
    return this
}

// Number Extensions
fun Int.formatWithCommas(): String {
    return NumberFormat.getNumberInstance(Locale.US).format(this)
}

fun Long.formatWithCommas(): String {
    return NumberFormat.getNumberInstance(Locale.US).format(this)
}

// Boolean Extensions
fun Boolean.toInt(): Int = if (this) 1 else 0

fun Boolean.toYesNo(): String = if (this) "Yes" else "No"

fun Boolean.toOnOff(): String = if (this) "On" else "Off"