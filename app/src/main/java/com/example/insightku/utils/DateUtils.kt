package com.example.insightku.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    
    fun formatDate(timestamp: Long, pattern: String = Constants.DATE_FORMAT_DISPLAY): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat(Constants.TIME_FORMAT_DISPLAY, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat(Constants.DATETIME_FORMAT_DISPLAY, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    fun getCurrentTimestamp(): Long = System.currentTimeMillis()
    
    fun getStartOfDay(timestamp: Long = getCurrentTimestamp()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
    
    fun getEndOfDay(timestamp: Long = getCurrentTimestamp()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }
    
    fun getStartOfWeek(timestamp: Long = getCurrentTimestamp()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
    
    fun getStartOfMonth(timestamp: Long = getCurrentTimestamp()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
    
    fun getStartOfYear(timestamp: Long = getCurrentTimestamp()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
    
    fun addDays(timestamp: Long, days: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            add(Calendar.DAY_OF_YEAR, days)
        }
        return calendar.timeInMillis
    }
    
    fun addWeeks(timestamp: Long, weeks: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            add(Calendar.WEEK_OF_YEAR, weeks)
        }
        return calendar.timeInMillis
    }
    
    fun addMonths(timestamp: Long, months: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            add(Calendar.MONTH, months)
        }
        return calendar.timeInMillis
    }
    
    fun getDaysBetween(startTimestamp: Long, endTimestamp: Long): Int {
        val startDate = getStartOfDay(startTimestamp)
        val endDate = getStartOfDay(endTimestamp)
        val diffInMillis = endDate - startDate
        return (diffInMillis / (24 * 60 * 60 * 1000)).toInt()
    }
    
    fun isToday(timestamp: Long): Boolean {
        val today = getStartOfDay()
        val targetDay = getStartOfDay(timestamp)
        return today == targetDay
    }
    
    fun isYesterday(timestamp: Long): Boolean {
        val yesterday = getStartOfDay(addDays(getCurrentTimestamp(), -1))
        val targetDay = getStartOfDay(timestamp)
        return yesterday == targetDay
    }
    
    fun getRelativeDateString(timestamp: Long): String {
        return when {
            isToday(timestamp) -> "Today"
            isYesterday(timestamp) -> "Yesterday"
            else -> formatDate(timestamp)
        }
    }
    
    fun getWeekDayName(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    fun getMonthName(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMMM", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}