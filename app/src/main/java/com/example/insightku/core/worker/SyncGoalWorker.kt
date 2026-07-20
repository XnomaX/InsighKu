package com.example.insightku.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.insightku.feature.auth.data.AuthRepository
import com.example.insightku.feature.planning.goal.data.repository.GoalRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * SyncGoalWorker — Background worker for syncing Goals and Contributions to Firestore.
 *
 * Offline-First Architecture:
 * 1. User action → Room write (isSynced=false) → UI updates instantly via Flow
 * 2. If Firestore sync fails → WorkManager schedules retry with NetworkConstraint
 * 3. When network available → this Worker uploads unsynced data
 * 4. After success → markAsSynced in Room
 *
 * Constraint: Only runs when network CONNECTED.
 * Retry policy: RETRY on failure, WorkManager handles MAX_ATTEMPTS automatically.
 */
@HiltWorker
class SyncGoalWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val goalRepository: GoalRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure() // No user — skip, not a Worker error

        return try {
            Log.d(TAG, "Starting sync for user $userId")

            // Sync unsynced goals
            goalRepository.syncUnsyncedGoals(userId)

            // Sync unsynced contributions
            goalRepository.syncUnsyncedContributions(userId)

            Log.d(TAG, "Sync completed successfully")
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Sync failed, will retry", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncGoalWorker"
        const val WORK_NAME = "SyncGoalWorker"
    }
}
