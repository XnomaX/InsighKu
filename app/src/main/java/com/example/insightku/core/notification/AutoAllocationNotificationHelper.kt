package com.example.insightku.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.insightku.R
import com.example.insightku.core.utils.CurrencyUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * AutoAllocationNotificationHelper — handles all notifications for the Smart Auto Allocation system.
 *
 * Notification types:
 * 1. Success: "Rp500.000 has been automatically allocated to your Emergency Fund."
 * 2. Skipped: "Auto Allocation skipped because the selected account has insufficient balance."
 * 3. Goal Completed: "Congratulations! Auto Allocation has stopped because your Emergency Fund goal has been completed."
 * 4. Suggestion: "New auto-allocation suggestion: Save Rp1.000.000 to Emergency Fund."
 */
@Singleton
class AutoAllocationNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID_AUTO_ALLOCATION = "auto_allocation_channel"
        const val CHANNEL_NAME_AUTO_ALLOCATION = "Auto Allocation"
        const val NOTIFICATION_ID_ALLOCATION_SUCCESS = 5001
        const val NOTIFICATION_ID_ALLOCATION_SKIPPED = 5002
        const val NOTIFICATION_ID_GOAL_COMPLETED = 5003

    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_AUTO_ALLOCATION,
                CHANNEL_NAME_AUTO_ALLOCATION,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.alloc_notif_channel_desc)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show notification for successful auto-allocation.
     */
    fun showAllocationSuccessNotification(goalName: String, amount: Double) {
        val formattedAmount = CurrencyUtils.formatAmount(amount)
        val title = context.getString(R.string.alloc_notif_success_title)
        val message = context.getString(R.string.alloc_notif_success_msg, formattedAmount, goalName)

        showNotification(
            notificationId = NOTIFICATION_ID_ALLOCATION_SUCCESS + abs(goalName.hashCode()),
            title = title,
            message = message,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    /**
     * Show notification when allocation is skipped due to insufficient balance.
     */
    fun showAllocationSkippedNotification(goalName: String, accountName: String) {
        val title = context.getString(R.string.alloc_notif_skipped_title)
        val message = context.getString(R.string.alloc_notif_skipped_msg, goalName, accountName)

        showNotification(
            notificationId = NOTIFICATION_ID_ALLOCATION_SKIPPED + abs(goalName.hashCode()),
            title = title,
            message = message,
            priority = NotificationCompat.PRIORITY_LOW
        )
    }

    /**
     * Show notification when a goal is completed and auto-allocation stops.
     */
    fun showGoalCompletedNotification(goalName: String) {
        val title = context.getString(R.string.alloc_notif_completed_title)
        val message = context.getString(R.string.alloc_notif_completed_msg, goalName)

        showNotification(
            notificationId = NOTIFICATION_ID_GOAL_COMPLETED + abs(goalName.hashCode()),
            title = title,
            message = message,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    private fun showNotification(
        notificationId: Int,
        title: String,
        message: String,
        priority: Int
    ) {
        // Check notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        // Create intent to open the app when notification is tapped
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_AUTO_ALLOCATION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, notification)
    }
}
