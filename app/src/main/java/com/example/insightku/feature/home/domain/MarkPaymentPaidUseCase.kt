package com.example.insightku.feature.home.domain

import android.content.Context
import com.example.insightku.core.data.model.BudgetFrequency
import com.example.insightku.core.data.model.Installment
import com.example.insightku.core.data.model.RecurringBudget
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.data.repository.CategoryRepository
import com.example.insightku.core.data.repository.InstallmentRepository
import com.example.insightku.core.data.repository.RecurringBudgetRepository
import com.example.insightku.core.data.repository.TransactionRepository
import com.example.insightku.core.utils.normalizedCategoryName
import com.example.insightku.core.worker.PaymentReminderHelper
import com.example.insightku.feature.auth.data.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

/**
 * MarkPaymentPaidUseCase — records a recurring or installment payment as paid.
 *
 * Extracted from DashboardViewModel so the presentation layer only coordinates:
 * it creates the ledger [Transaction], advances the next-due date, and cancels the
 * payment reminder. All business rules live here, not in the ViewModel.
 */
class MarkPaymentPaidUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val recurringBudgetRepository: RecurringBudgetRepository,
    private val installmentRepository: InstallmentRepository,
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) {

    /**
     * Resolve a real category name from a stored category id, matching by id or by
     * normalized name, falling back to the payment's own name.
     */
    private suspend fun resolveCategory(categoryId: String?, fallbackName: String): String {
        if (categoryId == null) return fallbackName
        val categories = categoryRepository.getAllCategories().first()
        return categories.firstOrNull { it.id == categoryId }?.name
            ?: categories.firstOrNull { it.name.normalizedCategoryName() == categoryId.normalizedCategoryName() }?.name
            ?: fallbackName
    }

    /**
     * Mark a recurring payment as paid: add the expense transaction, advance the
     * next-due date by the payment frequency, and cancel its reminder.
     * Returns success (no-op) when there is no signed-in user.
     */
    suspend fun markRecurringPaid(budget: RecurringBudget): Result<Unit> = try {
        val userId = authRepository.getCurrentUserId() ?: return Result.success(Unit)

        val tx = Transaction(
            title = budget.name,
            amount = budget.amount,
            category = resolveCategory(budget.categoryId, budget.name),
            type = TransactionType.EXPENSE,
            date = System.currentTimeMillis(),
            description = "Recurring payment: ${budget.name}",
            accountId = budget.accountId ?: ""
        )
        transactionRepository.addTransaction(tx, userId)

        val cal = Calendar.getInstance().apply { timeInMillis = budget.nextDue }
        when (budget.frequency) {
            BudgetFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            BudgetFrequency.BIWEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 2)
            BudgetFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            BudgetFrequency.QUARTERLY -> cal.add(Calendar.MONTH, 3)
            BudgetFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        val updated = budget.copy(
            nextDue = cal.timeInMillis,
            lastProcessed = System.currentTimeMillis()
        )
        recurringBudgetRepository.updateRecurringBudget(updated, userId)
        PaymentReminderHelper.cancelRemindersForPayment(context, "recurring_${budget.id}")
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Mark an installment payment as paid: add the expense transaction, advance the
     * month counter and next-due date, deactivate when complete, and cancel its reminder.
     * Returns success (no-op) when there is no signed-in user.
     */
    suspend fun markInstallmentPaid(installment: Installment): Result<Unit> = try {
        val userId = authRepository.getCurrentUserId() ?: return Result.success(Unit)

        val tx = Transaction(
            title = installment.name,
            amount = installment.monthlyPayment,
            category = resolveCategory(installment.categoryId, installment.name),
            type = TransactionType.EXPENSE,
            date = System.currentTimeMillis(),
            description = "Installment payment: ${installment.name} (${installment.paidMonths + 1}/${installment.totalMonths})",
            accountId = installment.accountId ?: ""
        )
        transactionRepository.addTransaction(tx, userId)

        val cal = Calendar.getInstance().apply { timeInMillis = installment.nextDueDate }
        cal.add(Calendar.MONTH, 1)
        val newPaid = (installment.paidMonths + 1).coerceAtMost(installment.totalMonths)
        val isNowComplete = newPaid >= installment.totalMonths
        val updated = installment.copy(
            paidMonths = newPaid,
            nextDueDate = cal.timeInMillis,
            isActive = newPaid < installment.totalMonths
        )
        installmentRepository.updateInstallment(updated, userId)
        if (isNowComplete) PaymentReminderHelper.cancelRemindersForPayment(context, "installment_${installment.id}")
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
