package com.example.insightku.core.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.insightku.R
import com.example.insightku.core.i18n.LocaleHelper
import com.example.insightku.core.i18n.NumberFormatter
import java.util.concurrent.TimeUnit

/**
 * Handles goal deadline reminder scheduling and notification delivery.
 * Mirrors [PaymentReminderHelper].
 *
 * Reminder intervals: H-7, H-3, H-1, Due Today, Overdue (once per day).
 * Duplicate prevention: SharedPreferences day-bucket per (goalId + reminderType).
 */
object GoalReminderHelper {

    private const val CHANNEL_ID = "goal_reminder_channel"
    private val CHANNEL_NAME_RES = R.string.goal_reminder_channel_name
    private const val WORK_NAME = "GoalReminderWorker"
    private const val PREFS_NAME = "goal_reminder_state"
    private const val KEY_PREFIX = "last_sent_"

    fun notificationId(goalId: String, type: String): Int = "$goalId:$type".hashCode()

    /** Schedule the periodic reminder check worker. Called on app startup. */
    fun scheduleDailyCheck(context: Context) {
        val request = PeriodicWorkRequestBuilder<GoalReminderWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .setInitialDelay(1, TimeUnit.HOURS)
            .addTag("goal_reminders")
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }

    /** Cancel all reminders for a goal (e.g. when completed/archived/deleted). */
    fun cancelRemindersForGoal(context: Context, goalId: String) {
        val manager = NotificationManagerCompat.from(context)
        listOf("h7", "h3", "h1", "today", "overdue").forEach { type ->
            manager.cancel(notificationId(goalId, type))
        }
        prefs(context).edit().apply {
            listOf("h7", "h3", "h1", "today", "overdue").forEach { type ->
                remove("${KEY_PREFIX}${goalId}_$type")
            }
            apply()
        }
    }

    /**
     * Send a goal deadline reminder, once per calendar day per reminder type.
     *
     * @param daysUntilDue days until deadline (negative = overdue)
     * @param remainingAmount amount still needed to reach the target
     */
    fun sendReminder(
        context: Context,
        goalId: String,
        goalName: String,
        remainingAmount: Double,
        daysUntilDue: Int
    ) {
        createChannelIfNeeded(context)

        val ctx = LocaleHelper.wrapContext(context)
        val formattedAmount = NumberFormatter.formatCurrency(remainingAmount.coerceAtLeast(0.0))
        val reminderData = when {
            daysUntilDue < 0 -> Triple(
                "overdue",
                ctx.getString(R.string.goal_reminder_overdue_title, goalName),
                ctx.getString(R.string.goal_reminder_overdue_body, goalName, -daysUntilDue, formattedAmount)
            )
            daysUntilDue == 0 -> Triple(
                "today",
                ctx.getString(R.string.goal_reminder_today_title, goalName),
                ctx.getString(R.string.goal_reminder_today_body, goalName, formattedAmount)
            )
            daysUntilDue == 1 -> Triple(
                "tomorrow",
                ctx.getString(R.string.goal_reminder_tomorrow_title, goalName),
                ctx.getString(R.string.goal_reminder_tomorrow_body, goalName, formattedAmount)
            )
            daysUntilDue <= 7 -> Triple(
                "h7",
                ctx.getString(R.string.goal_reminder_in_days_title, daysUntilDue, goalName),
                ctx.getString(R.string.goal_reminder_in_days_body, goalName, daysUntilDue, formattedAmount)
            )
            else -> return // Don't send reminders for deadlines > 7 days away
        }
        val type = reminderData.first
        val title = reminderData.second
        val body = reminderData.third

        // Duplicate prevention: only send once per calendar day per goal+type
        val todayBucket = todayDayBucket()
        val prefsKey = "${KEY_PREFIX}${goalId}_$type"
        if (prefs(context).getInt(prefsKey, -1) == todayBucket) return

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (daysUntilDue <= 1) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId(goalId, type), notification)
            prefs(context).edit().putInt(prefsKey, todayBucket).apply()
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS permission not granted
        }
    }

    private fun todayDayBucket(): Int {
        val cal = java.util.Calendar.getInstance()
        return cal.get(java.util.Calendar.YEAR) * 10000 + cal.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ctx = LocaleHelper.wrapContext(context)
            val channel = NotificationChannel(
                CHANNEL_ID,
                ctx.getString(CHANNEL_NAME_RES),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = ctx.getString(R.string.goal_reminder_channel_desc) }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
