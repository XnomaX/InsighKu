package com.example.insightku.core.notification

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.util.Log

class NotificationListenerRestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") return

        Log.d("BankService", "BOOT_COMPLETED received — requesting rebind + starting keepalive")
        NotificationListenerService.requestRebind(
            ComponentName(context, BankNotificationListenerService::class.java)
        )
        NotificationListenerKeepaliveService.start(context)
    }
}
