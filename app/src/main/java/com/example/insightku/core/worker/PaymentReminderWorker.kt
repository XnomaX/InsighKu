package com.example.insightku.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.insightku.core.data.repository.InstallmentRepository
import com.example.insightku.core.data.repository.RecurringBudgetRepository
import com.example.insightku.feature.auth.data.AuthRepository
import com.example.insightku.core.i18n.DateFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

private const val TAG = "PaymentReminderWorker"

/**
 * Periodic worker that checks for upcoming and overdue payments
 * and sends reminder notifications at the correct intervals.
 *
 * Reminder schedule:
 * - H-7 (7 days before due)
 * - H-3 (3 days before due)
 * - H-1 (1 day before due)
 * - Due Today
 * - Overdue (once per day until paid)
 *
 * Duplicate prevention:
 * Uses notification IDs derived from paymentId + reminder type,
 * so re-sending the same reminder within the same day replaces the old one.
 * Overdue reminders use a daily suffix so users get one reminder per day.
 */
@HiltWorker
class PaymentReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val recurringBudgetRepository: RecurringBudgetRepository,
    private val installmentRepository: InstallmentRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            Log.d(TAG, "No authenticated user — skipping")
            return Result.success()
        }

        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        Log.d(TAG, "Worker started at ${DateFormatter.formatDateTimeFull(now)}")

        try {
            // ── Recurring payments ──────────────────────────────────────────
            val allRecurring = recurringBudgetRepository.getAllRecurringBudgets().first()
            val activeRecurring = allRecurring.filter { it.isActive }

            for (budget in activeRecurring) {
                val daysUntilDue = ((budget.nextDue - now) / dayMs).toInt()
                PaymentReminderHelper.sendReminder(
                    context = applicationContext,
                    paymentId = "recurring_${budget.id}",
                    paymentName = budget.name,
                    amount = budget.amount,
                    daysUntilDue = daysUntilDue,
                    isRecurring = true
                )
            }

            // ── Installments ────────────────────────────────────────────────
            val allInstallments = installmentRepository.getAllInstallments().first()
            val activeInstallments = allInstallments.filter { it.isActive && !it.isCompleted }

            for (installment in activeInstallments) {
                val daysUntilDue = ((installment.nextDueDate - now) / dayMs).toInt()
                PaymentReminderHelper.sendReminder(
                    context = applicationContext,
                    paymentId = "installment_${installment.id}",
                    paymentName = installment.name,
                    amount = installment.monthlyPayment,
                    daysUntilDue = daysUntilDue,
                    isRecurring = false
                )
            }

            Log.d(TAG, "Checked ${activeRecurring.size} recurring + ${activeInstallments.size} installments")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error checking payments: ${e.message}")
            return Result.retry()
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "PaymentReminderWorker"
    }
}
