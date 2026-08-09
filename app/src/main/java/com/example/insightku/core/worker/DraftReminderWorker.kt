package com.example.insightku.core.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.insightku.MainActivity
import com.example.insightku.R
import com.example.insightku.core.data.repository.DraftTransactionRepository
import com.example.insightku.core.i18n.LocaleHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "DraftReminderWorker"
private const val CHANNEL_ID = "draft_reminder_channel"
private val CHANNEL_NAME_RES = R.string.draft_reminder_channel_name
private const val REMINDER_NOTIF_ID = 90_210
private const val MIN_AGE_MS = 18L * 60 * 60 * 1000 // 18 jam

/**
 * DraftReminderWorker — pengingat harian yang ringan & tidak menghakimi.
 *
 * Aturan (sesuai strategi reminder desain):
 * - Hanya muncul jika ADA draft PENDING dan yang tertua sudah berusia > 18 jam.
 *   Draft yang baru beberapa jam tidak perlu diingatkan.
 * - Tidak ada draft → tidak ada notifikasi (reminder tidak boleh hampa).
 * - Prioritas LOW/silent — ini bukan transaksi gagal, bukan darurat.
 * - Menyebut JUMLAH saja ("3 hal menunggu"), bukan nominal/merchant — jaga
 *   privasi di lockscreen dan hindari nada transaksional.
 * - Dijadwalkan periodik harian (lihat InsightKuApplication); WorkManager yang
 *   menentukan waktu pasti, kita hanya pastikan kondisinya terpenuhi.
 */
@HiltWorker
class DraftReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val draftRepository: DraftTransactionRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // ── TTL: auto-expire stale allocation drafts (>48 hours) ──
        val expired = draftRepository.expireStaleAllocationDrafts()
        if (expired > 0) {
            Log.d(TAG, "Auto-expired $expired stale allocation draft(s)")
        }

        val count = draftRepository.getPendingCountOnce()
        if (count <= 0) {
            Log.d(TAG, "No pending drafts — no reminder")
            return Result.success()
        }

        val oldest = draftRepository.getOldestPending() ?: return Result.success()
        val age = System.currentTimeMillis() - oldest.detectedAt
        if (age < MIN_AGE_MS) {
            Log.d(TAG, "Oldest draft only ${age}ms old (< 18h) — too soon to remind")
            return Result.success()
        }

        sendReminder(count)
        return Result.success()
    }

    private fun sendReminder(count: Int) {
        createChannelIfNeeded()

        val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            applicationContext, REMINDER_NOTIF_ID, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Sebut jumlah saja — tanpa nominal/merchant. Nada ajakan ringan, bukan tagihan.
        val ctx = LocaleHelper.wrapContext(applicationContext)
        val body =
            ctx.resources.getQuantityString(R.plurals.draft_reminder_body_plural, count, count)

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(ctx.getString(R.string.draft_reminder_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    Log.w(TAG, "POST_NOTIFICATIONS not granted — reminder suppressed")
                    return
                }
            }
            NotificationManagerCompat.from(applicationContext).notify(REMINDER_NOTIF_ID, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
        }
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ctx = LocaleHelper.wrapContext(applicationContext)
            val channel = NotificationChannel(
                CHANNEL_ID, ctx.getString(CHANNEL_NAME_RES), NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = ctx.getString(R.string.draft_reminder_channel_desc)
            }
            (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        const val WORK_NAME = "DraftReminderWorker"
    }
}
