package com.example.insightku.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.insightku.R
import com.example.insightku.core.data.model.BudgetFrequency
import com.example.insightku.core.data.model.Installment
import com.example.insightku.core.data.model.RecurringBudget
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.data.repository.TransactionRepository
import com.example.insightku.core.data.repository.RecurringBudgetRepository
import com.example.insightku.core.data.repository.InstallmentRepository
import com.example.insightku.feature.auth.data.AuthRepository
import com.example.insightku.core.i18n.DateFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.UUID

private const val TAG = "AutoTransactionDebug"

@HiltWorker
class AutoTransactionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val recurringBudgetRepository: RecurringBudgetRepository,
    private val installmentRepository: InstallmentRepository,
    private val transactionRepository: TransactionRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        Log.d(TAG, "Worker started")
        Log.d(TAG, "Current date = ${DateFormatter.formatDateTimeFull(now)}")

        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            Log.d(TAG, "No authenticated user — skipping")
            return Result.success()
        }
        Log.d(TAG, "UserId = $userId")

        var allSuccess = true

        // ── Recurring budgets ─────────────────────────────────────────────────
        val dueRecurring = recurringBudgetRepository.getDueRecurringBudgets(now)
        Log.d(TAG, "Active recurring due = ${dueRecurring.size}")

        for (budget in dueRecurring) {
            Log.d(TAG, "Recurring: id=${budget.id}, name=${budget.name}, amount=${budget.amount}, nextDue=${budget.nextDue}, frequency=${budget.frequency}")

            // Duplicate prevention: if lastProcessed is today, skip
            if (alreadyProcessedToday(budget.lastProcessed, now)) {
                Log.d(TAG, "Recurring id=${budget.id} already processed today — skipping")
                continue
            }

            // Backfill: process every missed occurrence up to today
            var nextDue = budget.nextDue
            var lastProcessed = budget.lastProcessed
            while (nextDue <= now) {
                Log.d(TAG, "Creating transaction from recurring id=${budget.id}, name=${budget.name}, amount=${budget.amount}, dueDate=$nextDue")
                val tx = Transaction(
                    id            = UUID.randomUUID().toString(),
                    title         = budget.name,
                    amount        = budget.amount,
                    category      = budget.name,
                    type          = TransactionType.EXPENSE,
                    date          = nextDue,
                    description   = "Auto: ${budget.name}",
                    accountId     = budget.accountId ?: "",
                    isSynced      = false
                )
                try {
                    transactionRepository.addTransaction(tx, userId)
                    Log.d(TAG, "Insert success: txId=${tx.id}, recurring=${budget.id}")

                    // Send notification with delete action
                    AutoTransactionNotificationHelper.notify(
                        context     = applicationContext,
                        txId        = tx.id,
                        title       = applicationContext.getString(R.string.notification_auto_prefix, budget.name),
                        amount      = budget.amount,
                        isRecurring = true
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Insert failed: recurring=${budget.id}, error=${e.message}")
                    allSuccess = false
                    break
                }

                lastProcessed = nextDue
                nextDue = advanceDate(nextDue, budget.frequency)
            }

            // Update nextDue and lastProcessed
            val updated = budget.copy(nextDue = nextDue, lastProcessed = lastProcessed)
            try {
                recurringBudgetRepository.updateRecurringBudget(updated, userId)
                Log.d(TAG, "Updated nextDue for recurring id=${budget.id} → $nextDue")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update recurring id=${budget.id}: ${e.message}")
                allSuccess = false
            }
        }

        // ── Installments ──────────────────────────────────────────────────────
        val allInstallments = installmentRepository.getAllInstallments().first()
        val dueInstallments = allInstallments.filter { it.isActive && it.nextDueDate <= now && !it.isCompleted }
        Log.d(TAG, "Active installments due = ${dueInstallments.size}")

        for (installment in dueInstallments) {
            Log.d(TAG, "Installment: id=${installment.id}, name=${installment.name}, monthlyPayment=${installment.monthlyPayment}, nextDueDate=${installment.nextDueDate}, paid=${installment.paidMonths}/${installment.totalMonths}")

            // Backfill: process every missed month up to today
            var nextDue = installment.nextDueDate
            var paidMonths = installment.paidMonths

            while (nextDue <= now && paidMonths < installment.totalMonths) {
                Log.d(TAG, "Creating transaction from installment id=${installment.id}, name=${installment.name}, amount=${installment.monthlyPayment}, dueDate=$nextDue")
                val tx = Transaction(
                    id            = UUID.randomUUID().toString(),
                    title         = installment.name,
                    amount        = installment.monthlyPayment,
                    category      = installment.name,
                    type          = TransactionType.EXPENSE,
                    date          = nextDue,
                    description   = "Auto cicilan: ${installment.name} (${paidMonths + 1}/${installment.totalMonths})",
                    accountId     = installment.accountId ?: "",
                    isSynced      = false
                )
                try {
                    transactionRepository.addTransaction(tx, userId)
                    Log.d(TAG, "Insert success: txId=${tx.id}, installment=${installment.id}")

                    AutoTransactionNotificationHelper.notify(
                        context     = applicationContext,
                        txId        = tx.id,
                        title       = applicationContext.getString(R.string.notification_installment_prefix, installment.name),
                        amount      = installment.monthlyPayment,
                        isRecurring = false
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Insert failed: installment=${installment.id}, error=${e.message}")
                    allSuccess = false
                    break
                }

                paidMonths++
                nextDue = advanceDate(nextDue, BudgetFrequency.MONTHLY)
            }

            // Update installment state
            val isNowComplete = paidMonths >= installment.totalMonths
            val updated = installment.copy(
                paidMonths  = paidMonths,
                nextDueDate = nextDue,
                isActive    = !isNowComplete
            )
            try {
                installmentRepository.updateInstallment(updated, userId)
                Log.d(TAG, "Updated installment id=${installment.id}: paidMonths=$paidMonths, nextDue=$nextDue, isActive=${updated.isActive}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update installment id=${installment.id}: ${e.message}")
                allSuccess = false
            }
        }

        Log.d(TAG, "Worker finished. allSuccess=$allSuccess")
        return if (allSuccess) Result.success() else Result.retry()
    }

    private fun advanceDate(fromMs: Long, frequency: BudgetFrequency): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = fromMs }
        when (frequency) {
            BudgetFrequency.WEEKLY    -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            BudgetFrequency.BIWEEKLY  -> cal.add(Calendar.WEEK_OF_YEAR, 2)
            BudgetFrequency.MONTHLY   -> cal.add(Calendar.MONTH, 1)
            BudgetFrequency.QUARTERLY -> cal.add(Calendar.MONTH, 3)
            BudgetFrequency.YEARLY    -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun alreadyProcessedToday(lastProcessed: Long?, now: Long): Boolean {
        if (lastProcessed == null) return false
        val fmt = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
        return fmt.format(java.util.Date(lastProcessed)) == fmt.format(java.util.Date(now))
    }

    companion object {
        const val WORK_NAME = "AutoTransactionWorker"
    }
}
