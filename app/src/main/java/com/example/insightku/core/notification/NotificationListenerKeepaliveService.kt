package com.example.insightku.core.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

private const val TAG = "KeepaliveService"
private const val CHANNEL_ID = "keepalive_channel"
private const val NOTIF_ID = 9999

class NotificationListenerKeepaliveService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        // START_STICKY (returned from onStartCommand) handles OS-level restart automatically.
        // Manual self-restart was removed: it caused rapid kill/restart cycles on OEMs with
        // aggressive power management, and is redundant with START_STICKY on stock Android.
        Log.d(TAG, "onDestroy")
    }

    private fun buildNotification(): Notification {
        createChannelIfNeeded()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("InsighKu aktif")
            .setContentText("Memantau notifikasi transaksi bank")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "InsighKu Background",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                description = "Diperlukan agar deteksi transaksi bank berjalan di background"
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, NotificationListenerKeepaliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NotificationListenerKeepaliveService::class.java))
        }
    }
}
