package com.example.insightku.core.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "NotificationDebug"

class DismissNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        if (notifId != -1) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(notifId)
            Log.d(TAG, "DismissNotificationReceiver: dismissed notifId=$notifId")
        }
    }

    companion object {
        const val EXTRA_NOTIF_ID = "extra_dismiss_notif_id"
    }
}
