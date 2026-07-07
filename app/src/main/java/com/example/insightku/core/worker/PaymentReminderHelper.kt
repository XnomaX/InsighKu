package com.example.insightku.core.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Handles payment reminder scheduling and notification delivery.
 *
 * Reminder intervals:
 * - H-7 (7 days before due)
 * - H-3 (3 days before due)
 * - H-1 (1 day before due)
 * - Due Today
 * - Overdue (once per day until paid)
 *
 * Duplicate prevention:
 * Uses a SharedPreferences day-bucket per (paymentId + reminderType).
 * A reminder is only sent once per calendar day for each combination.
 */
object PaymentReminderHelper {

    private const val CHANNEL_ID = "payment_reminder_channel"
    private const val CHANNEL_NAME = "Payment Reminders"
    private const val WORK_NAME = "PaymentReminderWorker"
    private const val PREFS_NAME = "payment_reminder_state"
    private const val KEY_PREFIX = "last_sent_"

    // Notification IDs are derived from a hash of paymentId + reminder type
    fun notificationId(paymentId: String, type: String): Int =
        "$paymentId:$type".hashCode()

    /**
     * Schedule the daily reminder check worker.
     * Called on app startup. Uses UPDATE policy so new worker logic takes effect after app updates.
     */
    fun scheduleDailyCheck(context: Context) {
        val request = PeriodicWorkRequestBuilder<PaymentReminderWorker>(
            12, TimeUnit.HOURS
        ).setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
        ).setInitialDelay(1, TimeUnit.HOURS)
            .addTag("payment_reminders")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /**
     * Cancel all reminders for a specific payment.
     * Called when a payment is marked as completed or deleted.
     */
    fun cancelRemindersForPayment(context: Context, paymentId: String) {
        val manager = NotificationManagerCompat.from(context)
        listOf("h7", "h3", "h1", "today", "overdue").forEach { type ->
            manager.cancel(notificationId(paymentId, type))
        }
        // Clear the sent-record state for this payment
        prefs(context).edit().apply {
            listOf("h7", "h3", "h1", "today", "overdue").forEach { type ->
                remove("${KEY_PREFIX}${paymentId}_$type")
            }
            apply()
        }
    }

    /**
     * Send a payment reminder notification, but only once per calendar day per reminder type.
     *
     * @param paymentId Unique ID of the payment
     * @param paymentName Display name of the payment
     * @param amount Payment amount
     * @param daysUntilDue Days until due (negative = overdue)
     * @param isRecurring true for recurring payments, false for installments
     */
    fun sendReminder(
        context: Context,
        paymentId: String,
        paymentName: String,
        amount: Double,
        daysUntilDue: Int,
        isRecurring: Boolean
    ) {
        createChannelIfNeeded(context)

        // Determine reminder type and content
        val (type, title, body) = when {
            daysUntilDue < 0 -> Triple(
                "overdue",
                "⚠️ Overdue: $paymentName",
                "\"$paymentName\" is ${-daysUntilDue} day(s) overdue. Amount: Rp ${"%,.0f".format(amount)}"
            )
            daysUntilDue == 0 -> Triple(
                "today",
                "📅 Due Today: $paymentName",
                "\"$paymentName\" is due today. Amount: Rp ${"%,.0f".format(amount)}"
            )
            daysUntilDue == 1 -> Triple(
                "h1",
                "⏰ Due Tomorrow: $paymentName",
                "\"$paymentName\" is due tomorrow. Amount: Rp ${"%,.0f".format(amount)}"
            )
            daysUntilDue <= 3 -> Triple(
                "h3",
                "📋 Due in $daysUntilDue days: $paymentName",
                "\"$paymentName\" is due in $daysUntilDue days. Amount: Rp ${"%,.0f".format(amount)}"
            )
            daysUntilDue <= 7 -> Triple(
                "h7",
                "📋 Due in $daysUntilDue days: $paymentName",
                "\"$paymentName\" is due in $daysUntilDue days. Amount: Rp ${"%,.0f".format(amount)}"
            )
            else -> return // Don't send reminders for payments > 7 days away
        }

        // Duplicate prevention: only send once per calendar day per payment+type
        val todayBucket = todayDayBucket()
        val prefsKey = "${KEY_PREFIX}${paymentId}_$type"
        val storedDay = prefs(context).getInt(prefsKey, -1)
        if (storedDay == todayBucket) {
            // Already sent this reminder today — skip
            return
        }

        val typeLabel = if (isRecurring) "Recurring" else "Installment"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("$typeLabel · Rp ${"%,.0f".format(amount)}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$body\nType: $typeLabel")
            )
            .setPriority(
                if (daysUntilDue <= 1) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(notificationId(paymentId, type), notification)
            // Record that we sent this reminder today
            prefs(context).edit().putInt(prefsKey, todayBucket).apply()
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS permission not granted
        }
    }

    /**
     * Returns an integer representing today as a day-bucket.
     * Two calls on the same calendar day return the same value.
     */
    private fun todayDayBucket(): Int {
        val cal = java.util.Calendar.getInstance()
        return cal.get(java.util.Calendar.YEAR) * 10000 +
                cal.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for upcoming and overdue payments"
            }
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
