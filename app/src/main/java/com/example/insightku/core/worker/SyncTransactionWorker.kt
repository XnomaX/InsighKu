package com.example.insightku.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.insightku.feature.auth.data.AuthRepository
import com.example.insightku.core.data.repository.TransactionRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * SyncTransactionWorker — Worker untuk sync transaksi lokal ke Firestore di background.
 *
 * ARSITEKTUR OFFLINE-FIRST (Issue 2):
 * ─────────────────────────────────────────────────────────────────────────────
 * Ketika user menambah transaksi saat offline:
 * 1. Transaksi disimpan ke Room dengan isSynced=false
 * 2. Upload ke Firestore gagal → WorkManager menjadwalkan retry dengan NetworkConstraint
 * 3. Saat network kembali tersedia → sistem trigger Worker ini
 * 4. Worker query Room untuk semua isSynced=false
 * 5. Upload satu per satu ke Firestore
 * 6. Setelah berhasil → markAsSynced(id) di Room
 *
 * Constraint: Hanya berjalan saat network CONNECTED (via WorkManager constraint).
 * Retry policy: RETRY jika gagal, MAX_ATTEMPTS ditangani WorkManager secara otomatis.
 *
 * Kenapa HiltWorker dan bukan Worker biasa?
 * HiltWorker memungkinkan inject dependency (Repository, Auth) melalui Hilt,
 * konsisten dengan pattern DI yang sudah dipakai di ViewModel dan UseCase.
 */
@HiltWorker
class SyncTransactionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure() // Tidak ada user — skip, bukan error Worker

        return try {
            val unsyncedTransactions = transactionRepository.getUnsyncedTransactions()

            if (unsyncedTransactions.isEmpty()) return Result.success()

            var allSuccess = true
            for (transaction in unsyncedTransactions) {
                try {
                    firestore
                        .collection("users")
                        .document(userId)
                        .collection("transactions")
                        .document(transaction.id)
                        .set(transaction)
                        .await()
                    transactionRepository.markTransactionSynced(transaction.id)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Satu transaksi gagal — catat, lanjut yang lain
                    allSuccess = false
                }
            }

            if (allSuccess) Result.success() else Result.retry()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Error umum — minta WorkManager untuk retry
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "SyncTransactionWorker"
    }
}



