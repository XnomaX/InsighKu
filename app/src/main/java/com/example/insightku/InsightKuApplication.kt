package com.example.insightku

import android.app.Application
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.example.insightku.core.notification.BankNotificationListenerService
import com.example.insightku.core.worker.AutoTransactionWorker
import com.example.insightku.core.worker.DraftReminderWorker
import com.example.insightku.core.worker.SyncTransactionWorker
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * InsightKuApplication — Application class dengan Hilt + WorkManager integration.
 *
 * ISSUE 2 FIX: Implement Configuration.Provider agar WorkManager menggunakan
 * HiltWorkerFactory. Tanpa ini, HiltWorker tidak bisa menerima @Inject dependency
 * (Repository, Auth, dll.) dan akan crash dengan "No default constructor found".
 *
 * Alur Offline-First Sync:
 * App start → scheduleSyncWorker() → WorkManager register PeriodicWork
 * Saat network tersedia → SyncTransactionWorker.doWork() → upload isSynced=false → markSynced
 */
@HiltAndroidApp
class InsightKuApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        scheduleSyncWorker()
        scheduleAutoTransactionWorker()
        scheduleDraftReminderWorker()
        NotificationListenerService.requestRebind(
            ComponentName(this, BankNotificationListenerService::class.java)
        )
    }

    /**
     * Schedule SyncTransactionWorker sebagai PeriodicWork (setiap 4 jam).
     * Ini adalah safety-net saja — recovery utama kini dilakukan oleh one-shot
     * worker yang dijadwalkan langsung saat Firestore sync gagal di addTransaction().
     *
     * UPDATE (bukan KEEP): memaksa pembaruan interval untuk existing users yang
     * masih memiliki jadwal lama 15 menit. WorkManager 2.8+ mendukung UPDATE.
     */
    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncTransactionWorker>(
            repeatInterval = 4,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncTransactionWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }

    /**
     * AutoTransactionWorker — berjalan setiap hari, tidak butuh network.
     * Query recurring + installment yang jatuh tempo, buat transaksi otomatis,
     * backfill transaksi yang terlewat, kirim notifikasi dengan action hapus.
     */
    private fun scheduleAutoTransactionWorker() {
        val request = PeriodicWorkRequestBuilder<AutoTransactionWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(Constraints.Builder().build()) // no network needed
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            AutoTransactionWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * DraftReminderWorker — pengingat harian yang ringan saat ada draft menunggu.
     * Periodik 1 hari, tanpa network. Initial delay diarahkan ke jendela sore/malam
     * (~19:00) agar muncul saat orang santai mereview hari, bukan saat sibuk.
     * Worker sendiri yang memutuskan apakah benar mengirim (cek ada draft & usia >18 jam).
     */
    private fun scheduleDraftReminderWorker() {
        val request = PeriodicWorkRequestBuilder<DraftReminderWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(Constraints.Builder().build()) // no network needed
            .setInitialDelay(millisUntilEveningWindow(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DraftReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** Milidetik dari sekarang sampai jam 19:00 berikutnya. */
    private fun millisUntilEveningWindow(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}


