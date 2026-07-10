package com.example.insightku.feature.planning.goal.domain.engine

import com.example.insightku.core.data.repository.AccountRepository
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.feature.planning.goal.data.model.AllocationTriggerType
import com.example.insightku.feature.planning.goal.data.model.AllocationValueType
import com.example.insightku.feature.planning.goal.data.model.GoalStatus
import com.example.insightku.feature.planning.goal.domain.model.AllocationSuggestion
import com.example.insightku.feature.planning.goal.domain.model.AutoAllocationResult
import com.example.insightku.feature.planning.goal.data.model.CategoryBasedExecutionMode
import com.example.insightku.feature.planning.goal.domain.model.AutoAllocationRule
import com.example.insightku.core.i18n.NumberFormatter
import android.util.Log
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.min

@Singleton
class AutoAllocationEngine @Inject constructor(
    private val dataSource: AutoAllocationDataSource,
    private val accountRepository: AccountRepository
) {
    companion object {
        private const val TAG = "AutoAllocationEngine"
    }
    suspend fun processTransaction(transaction: Transaction): AutoAllocationResult {
        val enabledRules = dataSource.getEnabledRules()
        if (enabledRules.isEmpty()) {
            return AutoAllocationResult(emptyList(), emptyList(), transaction.id, transaction.amount)
        }

        val categoryIdToName = mutableMapOf<String, String>()
        val nameToCategoryId = mutableMapOf<String, String>()
        try {
            val categories = dataSource.getAllCategories().first()
            categories.forEach { cat ->
                categoryIdToName[cat.id] = cat.name
                nameToCategoryId[cat.name.trim().lowercase()] = cat.id
            }
        } catch (e: Exception) { Log.e(TAG, "Failed to load categories: ${e.message}") }

        val suggestions = mutableListOf<AllocationSuggestion>()
        val autoExecuted = mutableListOf<AllocationSuggestion>()

        for (entity in enabledRules) {
            val rule = AutoAllocationRule.fromEntity(entity, dataSource.getGoalById(entity.goalId)?.name ?: "Unknown Goal")
            try {
                val result = evaluateRule(rule, transaction, categoryIdToName, nameToCategoryId)
                if (result != null) {
                    when (rule.confirmationMode) {
                        com.example.insightku.feature.planning.goal.data.model.ConfirmationMode.AUTO -> autoExecuted.add(result)
                        com.example.insightku.feature.planning.goal.data.model.ConfirmationMode.CONFIRMATION_REQUIRED -> suggestions.add(result)
                    }
                }
            } catch (e: Exception) { Log.e(TAG, "[TransactionEval] Rule=${rule.id} EXCEPTION — ${e.message}", e) }
        }

        return AutoAllocationResult(suggestions, autoExecuted, transaction.id, transaction.amount)
    }

    suspend fun processScheduledAllocations(): AutoAllocationResult {
        val enabledRules = dataSource.getEnabledRules()
        Log.d(TAG, "processScheduledAllocations: ${enabledRules.size} enabled rules loaded")

        val scheduledRules = enabledRules.filter { rule ->
            val trigger = AllocationTriggerType.fromString(rule.triggerType)
            trigger in listOf(AllocationTriggerType.DAILY, AllocationTriggerType.WEEKLY, AllocationTriggerType.BIWEEKLY, AllocationTriggerType.MONTHLY)
        }
        Log.d(TAG, "processScheduledAllocations: ${scheduledRules.size} scheduled rules (daily/weekly/biweekly/monthly)")

        if (scheduledRules.isEmpty()) return AutoAllocationResult(emptyList(), emptyList(), null, null)

        val suggestions = mutableListOf<AllocationSuggestion>()
        val autoExecuted = mutableListOf<AllocationSuggestion>()

        for (entity in scheduledRules) {
            val rule = AutoAllocationRule.fromEntity(entity, dataSource.getGoalById(entity.goalId)?.name ?: "Unknown Goal")
            if (!shouldExecuteScheduled(rule)) {
                Log.d(TAG, "[ScheduledSkip] Rule=${rule.id} Goal=${rule.goalName} — shouldExecuteScheduled=false (already fired or wrong day)")
                continue
            }
            try {
                val result = evaluateScheduledRule(rule)
                if (result != null) {
                    Log.d(TAG, "[ScheduledEval] Rule=${rule.id} Goal=${rule.goalName} mode=${rule.confirmationMode.value} → ${result.amount}")
                    when (rule.confirmationMode) {
                        com.example.insightku.feature.planning.goal.data.model.ConfirmationMode.AUTO -> autoExecuted.add(result)
                        com.example.insightku.feature.planning.goal.data.model.ConfirmationMode.CONFIRMATION_REQUIRED -> suggestions.add(result)
                    }
                } else {
                    Log.d(TAG, "[ScheduledEval] Rule=${rule.id} Goal=${rule.goalName} → evaluateScheduledRule returned null (validation failed)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[ScheduledEval] Rule=${rule.id} EXCEPTION — ${e.message}", e)
            }
        }

        Log.d(TAG, "processScheduledAllocations complete: ${autoExecuted.size} auto, ${suggestions.size} confirm-first")
        return AutoAllocationResult(suggestions, autoExecuted, null, null)
    }

    suspend fun processBalanceAboveRules(): AutoAllocationResult {
        val enabledRules = dataSource.getEnabledRules()
        val balanceRules = enabledRules.filter { AllocationTriggerType.fromString(it.triggerType) == AllocationTriggerType.BALANCE_ABOVE }
        val suggestions = mutableListOf<AllocationSuggestion>()
        val autoExecuted = mutableListOf<AllocationSuggestion>()

        for (entity in balanceRules) {
            val rule = AutoAllocationRule.fromEntity(entity, dataSource.getGoalById(entity.goalId)?.name ?: "Unknown Goal")
            try {
                val result = evaluateBalanceAboveRule(rule)
                if (result != null) {
                    when (rule.confirmationMode) {
                        com.example.insightku.feature.planning.goal.data.model.ConfirmationMode.AUTO -> autoExecuted.add(result)
                        com.example.insightku.feature.planning.goal.data.model.ConfirmationMode.CONFIRMATION_REQUIRED -> suggestions.add(result)
                    }
                }
            } catch (e: Exception) { Log.e(TAG, "[BalanceAboveEval] EXCEPTION — ${e.message}", e) }
        }

        return AutoAllocationResult(suggestions, autoExecuted, null, null)
    }

    private suspend fun evaluateRule(rule: AutoAllocationRule, transaction: Transaction, categoryIdToName: Map<String, String>, nameToCategoryId: Map<String, String>): AllocationSuggestion? {
        val matches = when (rule.triggerType) {
            AllocationTriggerType.INCOME_RECEIVED -> transaction.type == TransactionType.INCOME
            AllocationTriggerType.ROUND_UP -> transaction.type == TransactionType.EXPENSE
            AllocationTriggerType.SPENDING_CATEGORY -> transaction.type == TransactionType.EXPENSE
            AllocationTriggerType.BALANCE_ABOVE, AllocationTriggerType.DAILY, AllocationTriggerType.WEEKLY, AllocationTriggerType.BIWEEKLY, AllocationTriggerType.MONTHLY -> return null
        }
        if (!matches) return null
        return when (rule.triggerType) {
            AllocationTriggerType.INCOME_RECEIVED -> evaluateIncomeRule(rule, transaction, categoryIdToName)
            AllocationTriggerType.ROUND_UP -> evaluateRoundUpRule(rule, transaction)
            AllocationTriggerType.SPENDING_CATEGORY -> evaluateSpendingCategoryRule(rule, transaction, nameToCategoryId)
            else -> null
        }
    }

    private suspend fun evaluateIncomeRule(rule: AutoAllocationRule, transaction: Transaction, categoryIdToName: Map<String, String>): AllocationSuggestion? {
        val goal = dataSource.getGoalById(rule.goalId) ?: return null
        if (goal.goalStatus != GoalStatus.ACTIVE) return null
        val currentAmount = dataSource.getTotalContributed(rule.goalId)
        if (currentAmount >= goal.targetAmount) return null
        if (rule.incomeCategoryIds.isNotEmpty()) {
            val allowedCategoryNames = rule.incomeCategoryIds.mapNotNull { id -> categoryIdToName[id] }
            if (allowedCategoryNames.isNotEmpty() && transaction.category !in allowedCategoryNames) return null
        }
        if (rule.minIncomeAmount > 0 && transaction.amount < rule.minIncomeAmount) return null
        val sourceAccountId = rule.sourceAccountId ?: transaction.accountId
        if (sourceAccountId.isBlank()) return null
        val account = accountRepository.getAccountById(sourceAccountId) ?: return null
        val allocationAmount = calculateAllocationAmount(rule, transaction.amount)
        if (allocationAmount <= 0) return null
        val remainingToGoal = goal.targetAmount - currentAmount
        val actualAmount = min(allocationAmount, remainingToGoal)
        if (account.balance < actualAmount) return null
        return AllocationSuggestion(rule.goalId, goal.name, actualAmount, sourceAccountId, account.name, "Income detected: ${formatCurrency(transaction.amount)}", rule.description, ruleId = rule.id)
    }

    private suspend fun evaluateRoundUpRule(rule: AutoAllocationRule, transaction: Transaction): AllocationSuggestion? {
        if (!rule.roundUpEnabled) return null
        val goal = dataSource.getGoalById(rule.goalId) ?: return null
        if (goal.goalStatus != GoalStatus.ACTIVE) return null
        val currentAmount = dataSource.getTotalContributed(rule.goalId)
        if (currentAmount >= goal.targetAmount) return null
        val roundedUp = ceil(transaction.amount / rule.roundUpIncrement) * rule.roundUpIncrement
        val roundUpAmount = roundedUp - transaction.amount
        if (roundUpAmount <= 0 || roundUpAmount >= rule.roundUpIncrement) return null
        val sourceAccountId = rule.sourceAccountId ?: transaction.accountId
        if (sourceAccountId.isBlank()) return null
        val account = accountRepository.getAccountById(sourceAccountId) ?: return null
        val remainingToGoal = goal.targetAmount - currentAmount
        val actualAmount = min(roundUpAmount, remainingToGoal)
        if (account.balance < actualAmount) return null
        return AllocationSuggestion(rule.goalId, goal.name, actualAmount, sourceAccountId, account.name, "Round-up: ${formatCurrency(transaction.amount)} → ${formatCurrency(roundedUp)}", rule.description, ruleId = rule.id)
    }

    private suspend fun evaluateSpendingCategoryRule(rule: AutoAllocationRule, transaction: Transaction, nameToCategoryId: Map<String, String>): AllocationSuggestion? {
        // ── Category matching: check if transaction belongs to one of the selected categories ──
        if (rule.categoryBasedCategoryIds.isNotEmpty()) {
            val transactionCategoryId = nameToCategoryId[transaction.category.trim().lowercase()]
            if (transactionCategoryId == null || transactionCategoryId !in rule.categoryBasedCategoryIds) return null
        }

        // ── Execution mode gate: only EVERY_TRANSACTION fires in the per-transaction flow ──
        if (rule.categoryBasedExecutionMode != CategoryBasedExecutionMode.EVERY_TRANSACTION) return null

        val goal = dataSource.getGoalById(rule.goalId) ?: return null
        if (goal.goalStatus != GoalStatus.ACTIVE) return null
        val currentAmount = dataSource.getTotalContributed(rule.goalId)
        if (currentAmount >= goal.targetAmount) return null
        val allocationAmount = calculateAllocationAmount(rule, transaction.amount)
        if (allocationAmount <= 0) return null
        val sourceAccountId = rule.sourceAccountId ?: transaction.accountId
        if (sourceAccountId.isBlank()) return null
        val account = accountRepository.getAccountById(sourceAccountId) ?: return null
        val remainingToGoal = goal.targetAmount - currentAmount
        val actualAmount = min(allocationAmount, remainingToGoal)
        if (account.balance < actualAmount) return null
        return AllocationSuggestion(rule.goalId, goal.name, actualAmount, sourceAccountId, account.name, "Spending in ${transaction.category}: ${formatCurrency(transaction.amount)}", rule.description, ruleId = rule.id)
    }

    private suspend fun evaluateScheduledRule(rule: AutoAllocationRule): AllocationSuggestion? {
        val goal = dataSource.getGoalById(rule.goalId) ?: return null
        if (goal.goalStatus != GoalStatus.ACTIVE) return null
        val currentAmount = dataSource.getTotalContributed(rule.goalId)
        if (currentAmount >= goal.targetAmount) return null
        val sourceAccountId = rule.sourceAccountId ?: return null
        val account = accountRepository.getAccountById(sourceAccountId) ?: return null
        val allocationAmount = calculateAllocationAmount(rule, account.balance)
        if (allocationAmount <= 0) return null
        val remainingToGoal = goal.targetAmount - currentAmount
        val actualAmount = min(allocationAmount, remainingToGoal)
        if (account.balance < actualAmount) return null
        return AllocationSuggestion(rule.goalId, goal.name, actualAmount, sourceAccountId, account.name, "Scheduled ${rule.scheduledFrequency.value} allocation", rule.description, ruleId = rule.id)
    }

    private suspend fun evaluateBalanceAboveRule(rule: AutoAllocationRule): AllocationSuggestion? {
        val goal = dataSource.getGoalById(rule.goalId) ?: return null
        if (goal.goalStatus != GoalStatus.ACTIVE) return null
        val currentAmount = dataSource.getTotalContributed(rule.goalId)
        if (currentAmount >= goal.targetAmount) return null
        val threshold = rule.triggerParams?.threshold ?: return null
        val accountId = rule.triggerParams.accountId ?: rule.sourceAccountId ?: return null
        val account = accountRepository.getAccountById(accountId) ?: return null
        if (account.balance <= threshold) return null
        
        val excess = account.balance - threshold
        val allocationAmount = calculateAllocationAmount(rule, excess)
        if (allocationAmount <= 0) return null
        
        // Respect minRemainingBalance: ensure account balance doesn't fall below this amount
        val minRemaining = rule.minRemainingBalance
        val maxAllocatable = if (minRemaining > 0) {
            (account.balance - minRemaining).coerceAtLeast(0.0)
        } else {
            account.balance
        }
        
        val remainingToGoal = goal.targetAmount - currentAmount
        val actualAmount = min(min(allocationAmount, remainingToGoal), maxAllocatable)
        if (actualAmount <= 0) return null
        
        return AllocationSuggestion(rule.goalId, goal.name, actualAmount, accountId, account.name, "Balance ${formatCurrency(account.balance)} exceeds ${formatCurrency(threshold)}", rule.description, ruleId = rule.id)
    }

    private fun shouldExecuteScheduled(rule: AutoAllocationRule): Boolean {
        val now = java.time.LocalDateTime.now()
        val lastExecuted = rule.lastExecutedAt?.let {
            java.time.Instant.ofEpochMilli(it.toEpochMilli()).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        }
        if (lastExecuted != null) {
            when (rule.scheduledFrequency) {
                com.example.insightku.feature.planning.goal.data.model.ScheduledFrequency.DAILY -> if (lastExecuted.toLocalDate() == now.toLocalDate()) return false
                com.example.insightku.feature.planning.goal.data.model.ScheduledFrequency.WEEKLY -> if (lastExecuted.toLocalDate().plusWeeks(1).isAfter(now.toLocalDate())) return false
                com.example.insightku.feature.planning.goal.data.model.ScheduledFrequency.BIWEEKLY -> if (lastExecuted.toLocalDate().plusWeeks(2).isAfter(now.toLocalDate())) return false
                com.example.insightku.feature.planning.goal.data.model.ScheduledFrequency.MONTHLY -> if (lastExecuted.toLocalDate().plusMonths(1).isAfter(now.toLocalDate())) return false
            }
        }
        // Check day match first
        val dayMatches = when (rule.scheduledFrequency) {
            com.example.insightku.feature.planning.goal.data.model.ScheduledFrequency.DAILY -> true
            com.example.insightku.feature.planning.goal.data.model.ScheduledFrequency.WEEKLY, com.example.insightku.feature.planning.goal.data.model.ScheduledFrequency.BIWEEKLY -> now.dayOfWeek.value == rule.scheduledDayOfWeek
            com.example.insightku.feature.planning.goal.data.model.ScheduledFrequency.MONTHLY -> now.dayOfMonth == rule.scheduledDayOfMonth
        }
        if (!dayMatches) return false
        // Check execution time: worker runs every 6 hours, so allow execution if current hour is within [scheduledHour, scheduledHour + 6)
        val scheduledHour = rule.executionHour
        val currentHour = now.hour
        val hoursSinceScheduledHour = (currentHour - scheduledHour + 24) % 24
        return hoursSinceScheduledHour < 6
    }

    /**
     * Process Category Based rules with AFTER_DAILY_TOTAL or AFTER_MONTHLY_TOTAL execution modes.
     * These modes aggregate spending in selected categories over a period, then allocate a percentage/fixed amount.
     */
    suspend fun processCategoryBasedPeriodicRules(): AutoAllocationResult {
        val enabledRules = dataSource.getEnabledRules()
        val categoryRules = enabledRules.filter { rule ->
            val trigger = AllocationTriggerType.fromString(rule.triggerType)
            trigger == AllocationTriggerType.SPENDING_CATEGORY &&
            CategoryBasedExecutionMode.fromString(rule.categoryBasedExecutionMode) != CategoryBasedExecutionMode.EVERY_TRANSACTION
        }

        Log.d(TAG, "processCategoryBasedPeriodicRules: ${categoryRules.size} periodic category rules")

        if (categoryRules.isEmpty()) return AutoAllocationResult(emptyList(), emptyList(), null, null)

        val suggestions = mutableListOf<AllocationSuggestion>()
        val autoExecuted = mutableListOf<AllocationSuggestion>()

        for (entity in categoryRules) {
            val rule = AutoAllocationRule.fromEntity(entity, dataSource.getGoalById(entity.goalId)?.name ?: "Unknown Goal")
            try {
                val result = evaluatePeriodicCategoryRule(rule)
                if (result != null) {
                    Log.d(TAG, "[PeriodicCategoryEval] Rule=${rule.id} Goal=${rule.goalName} mode=${rule.categoryBasedExecutionMode.value} → ${result.amount}")
                    when (rule.confirmationMode) {
                        com.example.insightku.feature.planning.goal.data.model.ConfirmationMode.AUTO -> autoExecuted.add(result)
                        com.example.insightku.feature.planning.goal.data.model.ConfirmationMode.CONFIRMATION_REQUIRED -> suggestions.add(result)
                    }
                } else {
                    Log.d(TAG, "[PeriodicCategoryEval] Rule=${rule.id} Goal=${rule.goalName} → returned null (no spending or validation failed)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[PeriodicCategoryEval] Rule=${rule.id} EXCEPTION — ${e.message}", e)
            }
        }

        Log.d(TAG, "processCategoryBasedPeriodicRules complete: ${autoExecuted.size} auto, ${suggestions.size} confirm-first")
        return AutoAllocationResult(suggestions, autoExecuted, null, null)
    }

    /**
     * Evaluate a Category Based rule with AFTER_DAILY_TOTAL or AFTER_MONTHLY_TOTAL mode.
     * Calculates total spending in selected categories over the period, then allocates.
     */
    private suspend fun evaluatePeriodicCategoryRule(rule: AutoAllocationRule): AllocationSuggestion? {
        // Only process AFTER_DAILY_TOTAL and AFTER_MONTHLY_TOTAL modes
        val execMode = rule.categoryBasedExecutionMode
        if (execMode == CategoryBasedExecutionMode.EVERY_TRANSACTION) return null

        val goal = dataSource.getGoalById(rule.goalId) ?: return null
        if (goal.goalStatus != GoalStatus.ACTIVE) return null
        val currentAmount = dataSource.getTotalContributed(rule.goalId)
        if (currentAmount >= goal.targetAmount) return null

        // Calculate spending total for the selected categories over the period
        val totalSpending = calculateCategorySpendingTotal(rule, execMode)
        if (totalSpending <= 0) {
            Log.d(TAG, "[PeriodicCategoryEval] Rule=${rule.id} — no spending in selected categories")
            return null
        }

        // Calculate allocation amount based on the total spending
        val allocationAmount = calculateAllocationAmount(rule, totalSpending)
        if (allocationAmount <= 0) return null

        val sourceAccountId = rule.sourceAccountId ?: return null
        val account = accountRepository.getAccountById(sourceAccountId) ?: return null

        val remainingToGoal = goal.targetAmount - currentAmount
        val actualAmount = min(allocationAmount, remainingToGoal)
        if (account.balance < actualAmount) return null

        val periodLabel = if (execMode == CategoryBasedExecutionMode.AFTER_DAILY_TOTAL) "today" else "this month"
        return AllocationSuggestion(
            rule.goalId, goal.name, actualAmount, sourceAccountId, account.name,
            "Total spending $periodLabel: ${formatCurrency(totalSpending)}",
            rule.description, ruleId = rule.id
        )
    }

    /**
     * Calculate total spending in selected categories for the specified period.
     */
    private suspend fun calculateCategorySpendingTotal(
        rule: AutoAllocationRule,
        execMode: CategoryBasedExecutionMode
    ): Double {
        val now = java.time.LocalDateTime.now()
        val categoryIds = rule.categoryBasedCategoryIds
        if (categoryIds.isEmpty()) return 0.0

        // Calculate date range for the period
        val (startTime, endTime) = when (execMode) {
            CategoryBasedExecutionMode.AFTER_DAILY_TOTAL -> {
                val dayStart = now.toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault())
                val dayEnd = dayStart.plusDays(1)
                Pair(dayStart.toInstant().toEpochMilli(), dayEnd.toInstant().toEpochMilli())
            }
            CategoryBasedExecutionMode.AFTER_MONTHLY_TOTAL -> {
                val monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
                val monthEnd = monthStart.plusMonths(1)
                Pair(monthStart.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), monthEnd.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
            }
            else -> return 0.0
        }

        // Get transactions for the period
        val transactions = dataSource.getTransactionsByDateRange(startTime, endTime)
        
        // Load categories once and build lookup map for efficiency
        val categoryMap = try {
            dataSource.getAllCategories().first().associateBy { it.name.trim().lowercase() }
        } catch (e: Exception) { emptyMap() }
        
        // Filter by categories and sum expenses
        return transactions.filter { tx ->
            tx.type == TransactionType.EXPENSE && categoryIds.contains(
                categoryMap[tx.category.trim().lowercase()]?.id
            )
        }.sumOf { it.amount }
    }

    private fun calculateAllocationAmount(rule: AutoAllocationRule, sourceAmount: Double): Double {
        return when (rule.allocationType) {
            AllocationValueType.PERCENT -> sourceAmount * (rule.allocationValue / 100.0)
            AllocationValueType.FIXED -> rule.allocationValue
        }
    }

    private fun formatCurrency(amount: Double): String = NumberFormatter.formatCurrency(amount)
}
