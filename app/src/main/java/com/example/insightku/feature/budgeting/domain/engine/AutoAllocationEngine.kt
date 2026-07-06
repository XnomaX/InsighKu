package com.example.insightku.feature.budgeting.domain.engine

import com.example.insightku.core.data.repository.AccountRepository
import com.example.insightku.core.data.model.Transaction
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.feature.budgeting.data.model.AllocationTriggerType
import com.example.insightku.feature.budgeting.data.model.AutoAllocationRuleEntity
import com.example.insightku.feature.budgeting.data.model.GoalEntity
import com.example.insightku.feature.budgeting.data.model.AllocationValueType
import com.example.insightku.feature.budgeting.data.model.GoalStatus
import com.example.insightku.feature.budgeting.domain.model.AllocationSuggestion
import com.example.insightku.feature.budgeting.domain.model.AutoAllocationResult
import com.example.insightku.feature.budgeting.domain.model.AutoAllocationRule
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.min

/**
 * AutoAllocationEngine — the core brain for Smart Auto Allocation.
 *
 * Evaluates all enabled auto-allocation rules against incoming transactions
 * and performs or suggests allocations based on rule configuration.
 *
 * Trigger types:
 * - INCOME_RECEIVED: "Pay Yourself First" — allocate from income transactions
 * - ROUND_UP: Round up expense transactions to nearest increment
 * - SPENDING_CATEGORY: Allocate based on spending in a category
 * - DAILY/WEEKLY/BIWEEKLY/MONTHLY: Scheduled allocations (processed by worker)
 * - BALANCE_ABOVE: Allocate when account balance exceeds threshold
 *
 * Flow:
 * 1. Transaction is created (INCOME or EXPENSE)
 * 2. Engine evaluates all enabled rules
 * 3. For each matching rule:
 *    a. Check if goal is still active (not completed)
 *    b. Check if source account has sufficient balance
 *    c. Check income category filtering (for INCOME_RECEIVED)
 *    d. Check minimum income threshold (for INCOME_RECEIVED)
 *    e. Calculate allocation amount (PERCENT or FIXED)
 *    f. If CONFIRMATION_REQUIRED → create suggestion
 *    g. If AUTO → return for immediate execution
 * 4. Return results (suggestions + auto-executed allocations)
 */
@Singleton
class AutoAllocationEngine @Inject constructor(
    private val dataSource: AutoAllocationDataSource,
    private val accountRepository: AccountRepository
) {
    /**
     * Process a new transaction and evaluate all matching auto-allocation rules.
     * Called after a transaction is saved to Room.
     */
    suspend fun processTransaction(transaction: Transaction): AutoAllocationResult {        val enabledRules = dataSource.getEnabledRules()
        if (enabledRules.isEmpty()) {
            return AutoAllocationResult(
                suggestions = emptyList(),
                autoExecuted = emptyList(),
                processedTransactionId = transaction.id,
                processedAmount = transaction.amount
            )
        }

        // Pre-load category ID-to-name map for filtering
        val categoryIdToName = mutableMapOf<String, String>()
        try {
            val categories = dataSource.getAllCategories().first()
            categories.forEach { cat ->
                categoryIdToName[cat.id] = cat.name
            }
        } catch (_: Exception) {
            // Failed to load categories — filtering will be skipped
        }

        val suggestions = mutableListOf<AllocationSuggestion>()
        val autoExecuted = mutableListOf<AllocationSuggestion>()

        for (entity in enabledRules) {
            val rule = AutoAllocationRule.fromEntity(
                entity,
                dataSource.getGoalById(entity.goalId)?.name ?: "Unknown Goal"
            )

            try {
                val result = evaluateRule(rule, transaction, categoryIdToName)
                if (result != null) {
                    when (rule.confirmationMode) {
                        com.example.insightku.feature.budgeting.data.model.ConfirmationMode.AUTO -> {
                            autoExecuted.add(result)
                        }
                        com.example.insightku.feature.budgeting.data.model.ConfirmationMode.CONFIRMATION_REQUIRED -> {
                            suggestions.add(result)
                        }
                    }
                }
            } catch (_: Exception) {
                // Rule evaluation error — skip this rule
            }
        }

        return AutoAllocationResult(
            suggestions = suggestions,
            autoExecuted = autoExecuted,
            processedTransactionId = transaction.id,
            processedAmount = transaction.amount
        )
    }

    /**
     * Process scheduled allocation rules (called by ScheduledAllocationWorker).
     * Evaluates DAILY, WEEKLY, BIWEEKLY, MONTHLY triggers.
     */
    suspend fun processScheduledAllocations(): AutoAllocationResult {
        val enabledRules = dataSource.getEnabledRules()
        val scheduledRules = enabledRules.filter { rule ->
            val trigger = AllocationTriggerType.fromString(rule.triggerType)
            trigger in listOf(
                AllocationTriggerType.DAILY,
                AllocationTriggerType.WEEKLY,
                AllocationTriggerType.BIWEEKLY,
                AllocationTriggerType.MONTHLY
            )
        }

        if (scheduledRules.isEmpty()) {
            return AutoAllocationResult(
                suggestions = emptyList(),
                autoExecuted = emptyList(),
                processedTransactionId = null,
                processedAmount = null
            )
        }

        val suggestions = mutableListOf<AllocationSuggestion>()
        val autoExecuted = mutableListOf<AllocationSuggestion>()

        for (entity in scheduledRules) {
            val rule = AutoAllocationRule.fromEntity(
                entity,
                dataSource.getGoalById(entity.goalId)?.name ?: "Unknown Goal"
            )

            if (!shouldExecuteScheduled(rule)) continue

            try {
                val result = evaluateScheduledRule(rule)
                if (result != null) {
                    when (rule.confirmationMode) {
                        com.example.insightku.feature.budgeting.data.model.ConfirmationMode.AUTO -> autoExecuted.add(result)
                        com.example.insightku.feature.budgeting.data.model.ConfirmationMode.CONFIRMATION_REQUIRED -> suggestions.add(result)
                    }
                }
            } catch (_: Exception) {
                // Scheduled rule evaluation error — skip this rule
            }
        }

        return AutoAllocationResult(
            suggestions = suggestions,
            autoExecuted = autoExecuted,
            processedTransactionId = null,
            processedAmount = null
        )
    }

    /**
     * Process balance-above rules.
     */
    suspend fun processBalanceAboveRules(): AutoAllocationResult {
        val enabledRules = dataSource.getEnabledRules()
        val balanceRules = enabledRules.filter {
            AllocationTriggerType.fromString(it.triggerType) == AllocationTriggerType.BALANCE_ABOVE
        }

        val suggestions = mutableListOf<AllocationSuggestion>()
        val autoExecuted = mutableListOf<AllocationSuggestion>()

        for (entity in balanceRules) {
            val rule = AutoAllocationRule.fromEntity(
                entity,
                dataSource.getGoalById(entity.goalId)?.name ?: "Unknown Goal"
            )

            try {
                val result = evaluateBalanceAboveRule(rule)
                if (result != null) {
                    when (rule.confirmationMode) {
                        com.example.insightku.feature.budgeting.data.model.ConfirmationMode.AUTO -> autoExecuted.add(result)
                        com.example.insightku.feature.budgeting.data.model.ConfirmationMode.CONFIRMATION_REQUIRED -> suggestions.add(result)
                    }
                }
            } catch (_: Exception) {
                // Balance rule evaluation error — skip this rule
            }
        }

        return AutoAllocationResult(
            suggestions = suggestions,
            autoExecuted = autoExecuted,
            processedTransactionId = null,
            processedAmount = null
        )
    }

    // ── Rule Evaluation ────────────────────────────────────────────────────────

    private suspend fun evaluateRule(
        rule: AutoAllocationRule,
        transaction: Transaction,
        categoryIdToName: Map<String, String>
    ): AllocationSuggestion? {
        val matches = when (rule.triggerType) {
            AllocationTriggerType.INCOME_RECEIVED -> transaction.type == TransactionType.INCOME
            AllocationTriggerType.ROUND_UP -> transaction.type == TransactionType.EXPENSE
            AllocationTriggerType.SPENDING_CATEGORY -> transaction.type == TransactionType.EXPENSE
            AllocationTriggerType.BALANCE_ABOVE,
            AllocationTriggerType.DAILY,
            AllocationTriggerType.WEEKLY,
            AllocationTriggerType.BIWEEKLY,
            AllocationTriggerType.MONTHLY -> return null
        }

        if (!matches) return null

        return when (rule.triggerType) {
            AllocationTriggerType.INCOME_RECEIVED -> evaluateIncomeRule(rule, transaction, categoryIdToName)
            AllocationTriggerType.ROUND_UP -> evaluateRoundUpRule(rule, transaction)
            AllocationTriggerType.SPENDING_CATEGORY -> evaluateSpendingCategoryRule(rule, transaction)
            else -> null
        }
    }

    /**
     * Evaluate an income-based allocation rule (Pay Yourself First).
     * Category filtering matches transaction.category (name) against rule.incomeCategoryIds (IDs),
     * so we resolve ID→name via the preloaded map.
     */
    private suspend fun evaluateIncomeRule(
        rule: AutoAllocationRule,
        transaction: Transaction,
        categoryIdToName: Map<String, String>
    ): AllocationSuggestion? {
        val goal = dataSource.getGoalById(rule.goalId) ?: return null
        if (goal.goalStatus != GoalStatus.ACTIVE) return null

        val currentAmount = dataSource.getTotalContributed(rule.goalId)
        if (currentAmount >= goal.targetAmount) return null

        // Category filtering: rule stores category IDs, transaction stores category name
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

        return AllocationSuggestion(
            goalId = rule.goalId,
            goalName = goal.name,
            amount = actualAmount,
            sourceAccountId = sourceAccountId,
            sourceAccountName = account.name,
            triggerDescription = "Income detected: ${formatCurrency(transaction.amount)}",
            ruleDescription = rule.description
        )
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

        return AllocationSuggestion(
            goalId = rule.goalId,
            goalName = goal.name,
            amount = actualAmount,
            sourceAccountId = sourceAccountId,
            sourceAccountName = account.name,
            triggerDescription = "Round-up: ${formatCurrency(transaction.amount)} → ${formatCurrency(roundedUp)}",
            ruleDescription = rule.description
        )
    }

    private suspend fun evaluateSpendingCategoryRule(rule: AutoAllocationRule, transaction: Transaction): AllocationSuggestion? {
        val goal = dataSource.getGoalById(rule.goalId) ?: return null
        if (goal.goalStatus != GoalStatus.ACTIVE) return null

        val currentAmount = dataSource.getTotalContributed(rule.goalId)
        if (currentAmount >= goal.targetAmount) return null

        val triggerCategoryId = rule.triggerParams?.categoryId
        if (triggerCategoryId != null && transaction.category != triggerCategoryId) return null

        val allocationAmount = calculateAllocationAmount(rule, transaction.amount)
        if (allocationAmount <= 0) return null

        val sourceAccountId = rule.sourceAccountId ?: transaction.accountId
        if (sourceAccountId.isBlank()) return null

        val account = accountRepository.getAccountById(sourceAccountId) ?: return null
        val remainingToGoal = goal.targetAmount - currentAmount
        val actualAmount = min(allocationAmount, remainingToGoal)

        if (account.balance < actualAmount) return null

        return AllocationSuggestion(
            goalId = rule.goalId,
            goalName = goal.name,
            amount = actualAmount,
            sourceAccountId = sourceAccountId,
            sourceAccountName = account.name,
            triggerDescription = "Spending in category: ${formatCurrency(transaction.amount)}",
            ruleDescription = rule.description
        )
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

        return AllocationSuggestion(
            goalId = rule.goalId,
            goalName = goal.name,
            amount = actualAmount,
            sourceAccountId = sourceAccountId,
            sourceAccountName = account.name,
            triggerDescription = "Scheduled ${rule.scheduledFrequency.value} allocation",
            ruleDescription = rule.description
        )
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

        val remainingToGoal = goal.targetAmount - currentAmount
        val actualAmount = min(allocationAmount, remainingToGoal)

        if (account.balance < actualAmount) return null

        return AllocationSuggestion(
            goalId = rule.goalId,
            goalName = goal.name,
            amount = actualAmount,
            sourceAccountId = accountId,
            sourceAccountName = account.name,
            triggerDescription = "Balance ${formatCurrency(account.balance)} exceeds ${formatCurrency(threshold)}",
            ruleDescription = rule.description
        )
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun shouldExecuteScheduled(rule: AutoAllocationRule): Boolean {
        val now = java.time.LocalDateTime.now()
        val lastExecuted = rule.lastExecutedAt?.let {
            java.time.Instant.ofEpochMilli(it.toEpochMilli()).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        }

        if (lastExecuted != null) {
            when (rule.scheduledFrequency) {
                com.example.insightku.feature.budgeting.data.model.ScheduledFrequency.DAILY -> {
                    if (lastExecuted.toLocalDate() == now.toLocalDate()) return false
                }
                com.example.insightku.feature.budgeting.data.model.ScheduledFrequency.WEEKLY -> {
                    if (lastExecuted.toLocalDate().plusWeeks(1).isAfter(now.toLocalDate())) return false
                }
                com.example.insightku.feature.budgeting.data.model.ScheduledFrequency.BIWEEKLY -> {
                    if (lastExecuted.toLocalDate().plusWeeks(2).isAfter(now.toLocalDate())) return false
                }
                com.example.insightku.feature.budgeting.data.model.ScheduledFrequency.MONTHLY -> {
                    if (lastExecuted.toLocalDate().plusMonths(1).isAfter(now.toLocalDate())) return false
                }
            }
        }

        return when (rule.scheduledFrequency) {
            com.example.insightku.feature.budgeting.data.model.ScheduledFrequency.DAILY -> true
            com.example.insightku.feature.budgeting.data.model.ScheduledFrequency.WEEKLY,
            com.example.insightku.feature.budgeting.data.model.ScheduledFrequency.BIWEEKLY -> {
                now.dayOfWeek.value == rule.scheduledDayOfWeek
            }
            com.example.insightku.feature.budgeting.data.model.ScheduledFrequency.MONTHLY -> {
                now.dayOfMonth == rule.scheduledDayOfMonth
            }
        }
    }

    private fun calculateAllocationAmount(rule: AutoAllocationRule, sourceAmount: Double): Double {
        return when (rule.allocationType) {
            AllocationValueType.PERCENT -> sourceAmount * (rule.allocationValue / 100.0)
            AllocationValueType.FIXED -> rule.allocationValue
        }
    }

    private fun formatCurrency(amount: Double): String {
        return "Rp ${"%,.0f".format(amount).replace(",", ".")}"
    }
}
