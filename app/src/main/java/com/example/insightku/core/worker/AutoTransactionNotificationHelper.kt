package com.example.insightku.core.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.insightku.R
import com.example.insightku.core.i18n.LocaleHelper
import com.example.insightku.core.i18n.NumberFormatter

object AutoTransactionNotificationHelper {

    private const val CHANNEL_ID   = "auto_transaction_channel"
    private const val CHANNEL_NAME = "Transaksi Otomatis"

    fun notify(
        context: Context,
        txId: String,
        title: String,
        amount: Double,
        isRecurring: Boolean
    ) {
        createChannelIfNeeded(context)

        val notifId = txId.hashCode()

        // Delete action — fires DeleteTransactionReceiver without opening the app
        val deleteIntent = Intent(context, DeleteTransactionReceiver::class.java).apply {
            putExtra(DeleteTransactionReceiver.EXTRA_TX_ID,   txId)
            putExtra(DeleteTransactionReceiver.EXTRA_NOTIF_ID, notifId)
        }
        val deletePending = PendingIntent.getBroadcast(
            context,
            notifId,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ctx = LocaleHelper.wrapContext(context)
        val typeLabel  = if (isRecurring) ctx.getString(R.string.auto_tx_recurring) else ctx.getString(R.string.auto_tx_installment)
        val amountText = NumberFormatter.formatCurrency(amount)

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(ctx.getString(R.string.auto_tx_title, typeLabel))
            .setContentText("$title · $amountText")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$title · $amountText\n${ctx.getString(R.string.auto_tx_big_body)}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_delete,
                ctx.getString(R.string.auto_tx_delete_action),
                deletePending
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted — skip silently
        }
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ctx = LocaleHelper.wrapContext(context)
            val channel = NotificationChannel(
                CHANNEL_ID,
                ctx.getString(R.string.auto_tx_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = ctx.getString(R.string.auto_tx_channel_desc)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
