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
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val TAG = "GoalReminderWorker"

/**
 * Periodic worker that sends deadline reminders for goals with reminders enabled.
 * Mirrors [PaymentReminderWorker]. Schedule: H-7/H-3/H-1/today/overdue (handled by
 * [GoalReminderHelper]); duplicate prevention is per-goal per-day inside the helper.
 */
@HiltWorker
class GoalReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val goalRepository: GoalRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            Log.d(TAG, "No authenticated user — skipping")
            return Result.success()
        }

        return try {
            val today = LocalDate.now()
            val goals = goalRepository.getActiveGoals().first()
                .filter { it.reminderEnabled && it.deadline != null && !it.isCompleted }

            for (goal in goals) {
                val deadline = goal.deadline ?: continue
                val daysUntilDue = ChronoUnit.DAYS.between(today, deadline).toInt()
                GoalReminderHelper.sendReminder(
                    context = applicationContext,
                    goalId = goal.id,
                    goalName = goal.name,
                    remainingAmount = goal.remainingAmount,
                    daysUntilDue = daysUntilDue
                )
            }

            Log.d(TAG, "Checked ${goals.size} reminder-enabled goals")
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error checking goal reminders: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "GoalReminderWorker"
    }
}
