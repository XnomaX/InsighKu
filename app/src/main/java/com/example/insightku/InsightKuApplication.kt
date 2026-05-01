package com.example.insightku

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.example.insightku.worker.SyncTransactionWorker
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
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
    }

    /**
     * Schedule SyncTransactionWorker sebagai PeriodicWork (setiap 15 menit minimum).
     * Hanya berjalan saat network CONNECTED — WorkManager mengurus constraint ini.
     * KEEP_EXISTING: jika sudah terjadwal sebelumnya, tidak di-reset (idempotent).
     */
    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncTransactionWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncTransactionWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Jangan reset jika sudah ada
            syncRequest
        )
    }
}

