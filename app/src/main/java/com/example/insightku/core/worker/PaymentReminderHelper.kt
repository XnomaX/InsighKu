package com.example.insightku.core.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.insightku.R
import com.example.insightku.core.i18n.LocaleHelper
import com.example.insightku.core.utils.CurrencyUtils
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
    private val CHANNEL_NAME_RES = R.string.payment_channel_name
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
        val ctx = LocaleHelper.wrapContext(context)
        val formattedAmount = CurrencyUtils.formatAmount(amount)
        val reminderData = when {
            daysUntilDue < 0 -> Triple(
                "overdue",
                ctx.getString(R.string.payment_overdue_title, paymentName),
                ctx.resources.getQuantityString(
                    R.plurals.payment_overdue_body,
                    -daysUntilDue,
                    paymentName,
                    -daysUntilDue,
                    formattedAmount
                )
            )
            daysUntilDue == 0 -> Triple(
                "today",
                ctx.getString(R.string.payment_due_today_title, paymentName),
                ctx.getString(R.string.payment_due_today_body, paymentName, formattedAmount)
            )
            daysUntilDue == 1 -> Triple(
                "h1",
                ctx.getString(R.string.payment_due_tomorrow_title, paymentName),
                ctx.getString(R.string.payment_due_tomorrow_body, paymentName, formattedAmount)
            )
            daysUntilDue <= 3 -> Triple(
                "h3",
                ctx.resources.getQuantityString(
                    R.plurals.payment_due_in_days_title,
                    daysUntilDue,
                    daysUntilDue,
                    paymentName
                ),
                ctx.resources.getQuantityString(
                    R.plurals.payment_due_in_days_body,
                    daysUntilDue,
                    paymentName,
                    daysUntilDue,
                    formattedAmount
                )
            )
            daysUntilDue <= 7 -> Triple(
                "h7",
                ctx.resources.getQuantityString(
                    R.plurals.payment_due_in_days_title,
                    daysUntilDue,
                    daysUntilDue,
                    paymentName
                ),
                ctx.resources.getQuantityString(
                    R.plurals.payment_due_in_days_body,
                    daysUntilDue,
                    paymentName,
                    daysUntilDue,
                    formattedAmount
                )
            )
            else -> return // Don't send reminders for payments > 7 days away
        }
        val type = reminderData.first
        val title = reminderData.second
        val body = reminderData.third

        // Duplicate prevention: only send once per calendar day per payment+type
        val todayBucket = todayDayBucket()
        val prefsKey = "${KEY_PREFIX}${paymentId}_$type"
        val storedDay = prefs(context).getInt(prefsKey, -1)
        if (storedDay == todayBucket) {
            // Already sent this reminder today — skip
            return
        }

        val typeLabel = if (isRecurring) ctx.getString(R.string.payment_recurring) else ctx.getString(R.string.payment_installment)

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("$typeLabel · $formattedAmount")
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
            prefs(context).edit { putInt(prefsKey, todayBucket) }
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
            val ctx = LocaleHelper.wrapContext(context)
            val channel = NotificationChannel(
                CHANNEL_ID,
                ctx.getString(CHANNEL_NAME_RES),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = ctx.getString(R.string.payment_channel_desc)
            }
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
