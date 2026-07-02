package com.example.insightku.feature.budgeting.presentation.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.feature.budgeting.domain.model.Goal
import com.example.insightku.feature.budgeting.presentation.components.*
import com.example.insightku.feature.budgeting.presentation.event.GoalsEvent
import com.example.insightku.feature.budgeting.presentation.state.GoalsDialogState
import com.example.insightku.feature.budgeting.presentation.viewmodel.GoalsViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * Main Goals screen with Add button in content.
 */
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPalette.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = LocalAccent.current,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else {
                GoalsContent(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Handle snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(GoalsEvent.ClearSnackbar)
        }
    }

    // Handle errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(GoalsEvent.ClearError)
        }
    }

    // Dialogs
    DialogHost(uiState.dialogState, viewModel::onEvent, uiState)
}

/**
 * Main content for Goals screen.
 */
@Composable
private fun GoalsContent(
    uiState: com.example.insightku.feature.budgeting.presentation.state.GoalsUiState,
    onEvent: (GoalsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 100.dp // Extra space for Bottom Navigation + FAB
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Suggestions Banner (if any pending)
        if (uiState.hasPendingSuggestions) {
            item {
                SuggestionsBanner(
                    count = uiState.pendingSuggestionsCount,
                    onClick = { onEvent(GoalsEvent.ShowPendingSuggestions) }
                )
            }
        }


        // Summary Strip - compact 3-metric overview
        if (uiState.hasGoals && uiState.goalSummary != null) {
            item {
                GoalsSummaryStrip(summary = uiState.goalSummary!!)
            }
        }

        // Goals List or Empty State
        if (uiState.goals.isEmpty()) {
            item {
                EmptyGoalsState(
                    onCreateGoal = { onEvent(GoalsEvent.ShowAddGoalDialog()) }
                )
            }
        } else {
            // Goals Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Goals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary
                    )
                    Text(
                        text = "${uiState.goals.size} goal${if (uiState.goals.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                }
            }

            items(uiState.goals, key = { it.id }) { goal ->
                GoalCard(
                    goal = goal,
                    onClick = { onEvent(GoalsEvent.ShowGoalDetail(goal.id)) },
                    onContribute = { onEvent(GoalsEvent.ShowContributeDialog(goal.id)) },
                    onWithdraw = { onEvent(GoalsEvent.ShowWithdrawDialog(goal.id)) },
                    onEdit = { onEvent(GoalsEvent.ShowEditGoalDialog(goal.id)) }
                )
            }
        }

        // Auto-Allocation Rules Section
        if (uiState.autoAllocationRules.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Auto-Allocation Rules",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary
                )
            }

            item {
                AutoAllocationRulesSection(
                    rules = uiState.autoAllocationRules,
                    onToggle = { ruleId, enabled ->
                        onEvent(GoalsEvent.ToggleAutoAllocationRule(ruleId, enabled))
                    },
                    onEdit = { rule ->
                        onEvent(GoalsEvent.ShowEditAutoAllocationRuleDialog(rule))
                    },
                    onDelete = { ruleId ->
                        onEvent(GoalsEvent.DeleteAutoAllocationRule(ruleId))
                    }
                )
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Compact summary strip showing 3 key metrics.
 */
@Composable
fun GoalsSummaryStrip(
    summary: com.example.insightku.feature.budgeting.domain.model.GoalSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationSmall),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardInnerPaddingLarge),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Total Saved
            SummaryMetric(
                value = formatCompact(summary.totalSaved),
                label = "Saved",
                icon = Icons.Outlined.Savings,
                valueColor = SuccessColor
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(AppPalette.cardBorder)
            )

            // Overall Progress
            SummaryMetric(
                value = "${summary.overallProgress.toInt()}%",
                label = "Progress",
                icon = Icons.Outlined.TrendingUp,
                valueColor = LocalAccent.current
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(AppPalette.cardBorder)
            )

            // Active Goals
            SummaryMetric(
                value = "${summary.activeGoals}",
                label = "Active",
                icon = Icons.Outlined.Flag,
                valueColor = AppPalette.textPrimary
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    value: String,
    label: String,
    icon: ImageVector,
    valueColor: Color = AppPalette.textPrimary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LocalAccent.current,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppPalette.textMuted
        )
    }
}

private fun formatCompact(amount: Double): String {
    return when {
        amount >= 1_000_000 -> {
            val formatted = NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(amount / 1_000_000)
            "${formatted}M"
        }
        amount >= 1_000 -> {
            val formatted = NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(amount / 1_000)
            "${formatted}K"
        }
        else -> NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(amount.toLong())
    }
}

/**
 * Empty state when no goals exist.
 * Premium centered design with single CTA.
 */
@Composable
private fun EmptyGoalsState(
    onCreateGoal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Premium icon container
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(LocalAccent.current.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Savings,
                contentDescription = null,
                tint = LocalAccent.current,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "No Goals Yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppPalette.textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Set your first savings goal and start\ntracking your progress toward financial freedom",
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.textMuted,
            textAlign = TextAlign.Center,
            lineHeight = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Premium CTA Button
        Button(
            onClick = onCreateGoal,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LocalAccent.current
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Create Goal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Section showing auto-allocation rules.
 */
@Composable
private fun AutoAllocationRulesSection(
    rules: List<com.example.insightku.feature.budgeting.domain.model.AutoAllocationRule>,
    onToggle: (String, Boolean) -> Unit,
    onEdit: (com.example.insightku.feature.budgeting.domain.model.AutoAllocationRule) -> Unit,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column {
            rules.forEachIndexed { index, rule ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rule.goalName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = AppPalette.textPrimary
                        )
                        Text(
                            text = rule.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppPalette.textMuted
                        )
                    }

                    Switch(
                        checked = rule.isEnabled,
                        onCheckedChange = { onToggle(rule.id, it) }
                    )
                }

                if (index < rules.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = AppPalette.cardBorder
                    )
                }
            }
        }
    }
}

/**
 * Host for all dialogs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogHost(
    dialogState: GoalsDialogState,
    onEvent: (GoalsEvent) -> Unit,
    uiState: com.example.insightku.feature.budgeting.presentation.state.GoalsUiState
) {
    when (dialogState) {
        is GoalsDialogState.None -> { /* No dialog */ }

        is GoalsDialogState.AddGoal -> {
            AddGoalDialog(
                initialDeadline = dialogState.deadline,
                onDismiss = { onEvent(GoalsEvent.DismissDialog) },
                onCreateGoal = { name, amount, deadline, icon, color ->
                    onEvent(GoalsEvent.CreateGoal(name, amount, deadline, icon, color))
                }
            )
        }

        is GoalsDialogState.EditGoal -> {
            EditGoalDialog(
                goal = dialogState.goal,
                onDismiss = { onEvent(GoalsEvent.DismissDialog) },
                onSave = { name, amount, deadline, icon, color, notes ->
                    onEvent(GoalsEvent.UpdateGoal(dialogState.goal.id, name, amount, deadline, icon, color, notes))
                },
                onArchive = { onEvent(GoalsEvent.ShowArchiveGoalDialog(dialogState.goal.id)) },
                onPause = { onEvent(GoalsEvent.PauseGoal(dialogState.goal.id)) },
                onResume = { onEvent(GoalsEvent.ResumeGoal(dialogState.goal.id)) }
            )
        }

        is GoalsDialogState.ArchiveGoal -> {
            AlertDialog(
                onDismissRequest = { onEvent(GoalsEvent.DismissDialog) },
                title = { Text("Archive Goal?") },
                text = { Text("Are you sure you want to archive \"${dialogState.goalName}\"? You can still view its history but it won't appear in your active goals.") },
                confirmButton = {
                    TextButton(
                        onClick = { onEvent(GoalsEvent.ArchiveGoal(dialogState.goalId)) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Archive")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onEvent(GoalsEvent.DismissDialog) }) {
                        Text("Cancel")
                    }
                }
            )
        }

        is GoalsDialogState.Contribute -> {
            val goal = uiState.goals.find { it.id == dialogState.goalId }
            if (goal != null) {
                ContributionBottomSheet(
                    goal = goal,
                    accounts = uiState.accounts,
                    accountAllocations = uiState.accountAllocations,
                    initialAccountId = dialogState.selectedAccountId,
                    onDismiss = { onEvent(GoalsEvent.DismissDialog) },
                    onContribute = { accountId, amount ->
                        onEvent(GoalsEvent.Contribute(dialogState.goalId, accountId, amount))
                    }
                )
            }
        }

        is GoalsDialogState.Withdraw -> {
            val goal = uiState.goals.find { it.id == dialogState.goalId }
            if (goal != null) {
                WithdrawalBottomSheet(
                    goal = goal,
                    accounts = uiState.accounts,
                    accountAllocations = uiState.accountAllocations,
                    onDismiss = { onEvent(GoalsEvent.DismissDialog) },
                    onWithdraw = { accountId: String, amount: Double ->
                        onEvent(GoalsEvent.Withdraw(dialogState.goalId, accountId, amount))
                    }
                )
            }
        }

        is GoalsDialogState.SetDailyTarget -> {
            SetDailyTargetDialog(
                currentAmount = dialogState.amount,
                onDismiss = { onEvent(GoalsEvent.DismissDialog) },
                onSet = { amount -> onEvent(GoalsEvent.SetDailyTarget(amount)) },
                onClear = { onEvent(GoalsEvent.ClearDailyTarget) }
            )
        }

        is GoalsDialogState.AddAutoAllocationRule,
        is GoalsDialogState.EditAutoAllocationRule -> {
            val rule = (dialogState as? GoalsDialogState.EditAutoAllocationRule)?.rule
            AutoAllocationDialog(
                rule = rule,
                goals = uiState.goals,
                onDismiss = { onEvent(GoalsEvent.DismissDialog) },
                onSave = { newRule ->
                    if (rule != null) {
                        onEvent(GoalsEvent.UpdateAutoAllocationRule(newRule))
                    } else {
                        onEvent(GoalsEvent.AddAutoAllocationRule(newRule))
                    }
                },
                onDelete = if (rule != null) { ruleId ->
                    onEvent(GoalsEvent.DeleteAutoAllocationRule(ruleId))
                } else null
            )
        }

        is GoalsDialogState.PendingSuggestions -> {
            PendingSuggestionsSheet(
                suggestions = uiState.pendingSuggestions,
                onConfirm = { onEvent(GoalsEvent.ConfirmSuggestion(it)) },
                onDismiss = { onEvent(GoalsEvent.DismissSuggestion(it)) },
                onDismissAll = { onEvent(GoalsEvent.DismissAllSuggestions) },
                onDismissSheet = { onEvent(GoalsEvent.DismissDialog) }
            )
        }

        is GoalsDialogState.GoalDetail -> {
            val goal = uiState.goals.find { it.id == dialogState.goalId }
            if (goal != null) {
                GoalDetailScreen(
                    goal = goal,
                    dailyTarget = uiState.dailyTarget,
                    contributions = uiState.selectedGoalContributions,
                    onBack = { onEvent(GoalsEvent.DismissDialog) },
                    onEdit = { onEvent(GoalsEvent.ShowEditGoalDialog(goal.id)) },
                    onSetDailyTarget = { onEvent(GoalsEvent.ShowSetDailyTargetDialog) },
                    onSave = { onEvent(GoalsEvent.ShowContributeDialog(goal.id)) },
                    onWithdraw = { onEvent(GoalsEvent.ShowWithdrawDialog(goal.id)) }
                )
            }
        }

        is GoalsDialogState.LinkAccount,
        is GoalsDialogState.SelectAccount -> {
            // TODO: Implement account linking dialogs
            // For now, dismiss these dialogs
        }
    }
}

/**
 * Dialog for setting daily target amount.
 */
@Composable
private fun SetDailyTargetDialog(
    currentAmount: String,
    onDismiss: () -> Unit,
    onSet: (Double) -> Unit,
    onClear: () -> Unit
) {
    var amount by remember { mutableStateOf(currentAmount) }
    val parsedAmount = amount.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = AppPalette.card,
        title = {
            Text(
                text = "Daily Saving Target",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Set an amount to save each day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppPalette.textMuted
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Daily Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("Rp ") }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("25000", "50000", "100000", "200000").forEach { value ->
                        FilterChip(
                            selected = amount == value,
                            onClick = { amount = value },
                            label = {
                                Text(
                                    "Rp ${java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(value.toLong())}"
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSet(parsedAmount) },
                enabled = parsedAmount > 0
            ) {
                Text("Set Target")
            }
        },
        dismissButton = {
            Row {
                if (currentAmount.isNotBlank()) {
                    TextButton(onClick = onClear) {
                        Text("Clear")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

/**
 * Dialog for editing a goal.
 */
@Composable
private fun EditGoalDialog(
    goal: Goal,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Double, deadline: java.time.LocalDate?, icon: String, color: String, notes: String) -> Unit,
    onArchive: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    AddGoalDialog(
        initialDeadline = goal.deadline,
        onDismiss = onDismiss,
        onCreateGoal = { name, amount, deadline, icon, color ->
            onSave(name, amount, deadline, icon, color, goal.notes)
        }
    )
}
