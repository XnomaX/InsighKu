package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.core.ui.components.dialogs.PremiumDialog
import com.example.insightku.core.ui.components.dialogs.PremiumDialogType
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.feature.planning.goal.domain.model.AutoAllocationRule
import com.example.insightku.feature.planning.goal.domain.model.Goal

@Composable
fun GoalsScreen(viewModel: GoalsViewModel = hiltViewModel(), onNavigateToGoalDetail: ((String) -> Unit)? = null) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.dialogState) {
        if (uiState.dialogState is GoalsDialogState.GoalDetail) {
            val goalId = (uiState.dialogState as GoalsDialogState.GoalDetail).goalId
            if (onNavigateToGoalDetail != null) { onNavigateToGoalDetail(goalId); viewModel.onEvent(GoalsEvent.DismissDialog) }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppPalette.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LocalAccent.current, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                }
            } else {
                GoalsContent(uiState = uiState, onEvent = viewModel::onEvent, modifier = Modifier.weight(1f))
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    LaunchedEffect(uiState.snackbarMessage) { uiState.snackbarMessage?.let { message -> snackbarHostState.showSnackbar(message); viewModel.onEvent(GoalsEvent.ClearSnackbar) } }
    LaunchedEffect(uiState.error) { uiState.error?.let { error -> snackbarHostState.showSnackbar(error); viewModel.onEvent(GoalsEvent.ClearError) } }

    // ── Goal Actions Bottom Sheet ─────────────────────────────────────────
    if (uiState.showActionsSheet && uiState.actionsGoalId != null) {
        val actionsGoal = uiState.goals.find { it.id == uiState.actionsGoalId }
            ?: uiState.activeGoals.find { it.id == uiState.actionsGoalId }
            ?: uiState.pausedGoals.find { it.id == uiState.actionsGoalId }
        if (actionsGoal != null) {
            GoalActionsBottomSheet(
                goal = actionsGoal,
                onPause = { viewModel.onEvent(GoalsEvent.PauseGoal(actionsGoal.id)) },
                onResume = { viewModel.onEvent(GoalsEvent.ResumeGoal(actionsGoal.id)) },
                onComplete = { viewModel.onEvent(GoalsEvent.UpdateGoalStatus(actionsGoal.id, com.example.insightku.feature.planning.goal.data.model.GoalStatus.COMPLETED)) },
                onArchive = { viewModel.onEvent(GoalsEvent.ArchiveGoal(actionsGoal.id)) },
                onEdit = { viewModel.onEvent(GoalsEvent.ShowEditGoalDialog(actionsGoal.id)) },
                onDismiss = { viewModel.onEvent(GoalsEvent.DismissActionsSheet) }
            )
        }
    }

    // ── Archived Goals Bottom Sheet ───────────────────────────────────────
    if (uiState.showArchivedSheet) {
        ArchivedGoalsSheet(
            archivedGoals = uiState.archivedGoals,
            onRestore = { goalId -> viewModel.onEvent(GoalsEvent.ShowRestoreGoalConfirm(goalId)) },
            onDelete = { goalId -> viewModel.onEvent(GoalsEvent.ShowDeleteGoalConfirm(goalId)) },
            onClick = { goalId -> onNavigateToGoalDetail?.invoke(goalId) },
            onDismiss = { viewModel.onEvent(GoalsEvent.DismissArchivedSheet) }
        )
    }

    // ── Completion Celebration Overlay ────────────────────────────────────
    if (uiState.showCompletionCelebration) {
        CompletionCelebrationOverlay(
            goalName = uiState.completionGoalName,
            onDismiss = { viewModel.onEvent(GoalsEvent.DismissCompletionCelebration) }
        )
    }

    // ── Dialog Host ───────────────────────────────────────────────────────
    DialogHost(uiState.dialogState, viewModel::onEvent, uiState)
}

// ─── Main Content ─────────────────────────────────────────────────────────────

@Composable
private fun GoalsContent(uiState: GoalsUiState, onEvent: (GoalsEvent) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Tab Filter (Active / Paused) + Archive Access ─────────────────
        if (uiState.hasGoals) {
            item {
                GoalFilterTabs(
                    selectedTab = uiState.selectedTab,
                    activeCount = uiState.activeGoalCount,
                    pausedCount = uiState.pausedGoalCount,
                    completedCount = uiState.completedGoalCount,
                    archivedCount = uiState.archivedGoalCount,
                    onTabSelected = { onEvent(GoalsEvent.SelectTab(it)) },
                    onArchivedClick = { onEvent(GoalsEvent.ShowArchivedGoals) }
                )
            }
        }

        // ── Empty States ──────────────────────────────────────────────────
        if (uiState.goals.isEmpty()) {
            item { EmptyGoalsState(onCreateGoal = { onEvent(GoalsEvent.ShowAddGoalDialog()) }) }
        } else if (uiState.displayedGoals.isEmpty()) {
            item {
                EmptyTabState(
                    tab = uiState.selectedTab,
                    onCreateGoal = { onEvent(GoalsEvent.ShowAddGoalDialog()) }
                )
            }
        } else {
            // ── Goal Cards ────────────────────────────────────────────────
            items(uiState.displayedGoals, key = { it.id }) { goal ->
                GoalCard(
                    goal = goal,
                    onClick = { onEvent(GoalsEvent.ShowGoalDetail(goal.id)) },
                    onContribute = { onEvent(GoalsEvent.ShowContributeDialog(goal.id)) },
                    onWithdraw = { onEvent(GoalsEvent.ShowWithdrawDialog(goal.id)) },
                    onEdit = { onEvent(GoalsEvent.ShowGoalActionsSheet(goal.id)) }
                )
            }
        }

        // ── Auto-allocation rules ─────────────────────────────────────────
        if (uiState.autoAllocationRules.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(8.dp)); Text(text = stringResource(R.string.auto_allocation_rules), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary) }
            item { AutoAllocationRulesSection(rules = uiState.autoAllocationRules, onToggle = { ruleId, enabled -> onEvent(GoalsEvent.ToggleAutoAllocationRule(ruleId, enabled)) }, onEdit = { rule -> onEvent(GoalsEvent.ShowEditAutoAllocationRuleDialog(rule)) }, onDelete = { ruleId -> onEvent(GoalsEvent.DeleteAutoAllocationRule(ruleId)) }) }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ─── Filter Tabs ──────────────────────────────────────────────────────────────

@Composable
private fun GoalFilterTabs(
    selectedTab: GoalFilterTab,
    activeCount: Int,
    pausedCount: Int,
    completedCount: Int,
    archivedCount: Int,
    onTabSelected: (GoalFilterTab) -> Unit,
    onArchivedClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GoalFilterChip(
                label = stringResource(R.string.goals_active),
                count = activeCount,
                isSelected = selectedTab == GoalFilterTab.ACTIVE,
                onClick = { onTabSelected(GoalFilterTab.ACTIVE) }
            )
            GoalFilterChip(
                label = stringResource(R.string.goals_paused_tab),
                count = pausedCount,
                isSelected = selectedTab == GoalFilterTab.PAUSED,
                onClick = { onTabSelected(GoalFilterTab.PAUSED) }
            )
            GoalFilterChip(
                label = stringResource(R.string.goals_completed_tab),
                count = completedCount,
                isSelected = selectedTab == GoalFilterTab.COMPLETED,
                onClick = { onTabSelected(GoalFilterTab.COMPLETED) }
            )
        }

        // Archive access button (always visible)
        Surface(
            onClick = onArchivedClick,
            shape = RoundedCornerShape(12.dp),
            color = AppPalette.cardElevated,
            border = BorderStroke(1.dp, AppPalette.cardBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Outlined.Archive, null, tint = AppPalette.textMuted, modifier = Modifier.size(16.dp))
                if (archivedCount > 0) {
                    Text(stringResource(R.string.goals_archived_count, archivedCount), style = MaterialTheme.typography.labelMedium, color = AppPalette.textMuted, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun GoalFilterChip(label: String, count: Int, isSelected: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) accent.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(200), label = "chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) accent else AppPalette.textMuted,
        animationSpec = tween(200), label = "chip_text"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accent.copy(alpha = 0.3f) else AppPalette.cardBorder,
        animationSpec = tween(200), label = "chip_border"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = textColor)
            if (count > 0) {
                Surface(shape = CircleShape, color = textColor.copy(alpha = 0.15f)) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ─── Empty Tab State ──────────────────────────────────────────────────────────

@Composable
private fun EmptyTabState(tab: GoalFilterTab, onCreateGoal: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val icon = when (tab) {
            GoalFilterTab.PAUSED -> Icons.Outlined.PauseCircleOutline
            GoalFilterTab.ACTIVE -> Icons.Outlined.Flag
            GoalFilterTab.COMPLETED -> Icons.Outlined.CheckCircleOutline
        }
        Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(AppPalette.cardElevated), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = AppPalette.textMuted, modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (tab) {
                GoalFilterTab.PAUSED -> stringResource(R.string.goals_no_paused)
                GoalFilterTab.ACTIVE -> stringResource(R.string.goals_no_active)
                GoalFilterTab.COMPLETED -> stringResource(R.string.goals_no_completed)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppPalette.textPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when (tab) {
                GoalFilterTab.PAUSED -> stringResource(R.string.goals_no_paused_desc)
                GoalFilterTab.ACTIVE -> stringResource(R.string.goals_no_active_desc)
                GoalFilterTab.COMPLETED -> stringResource(R.string.goals_no_completed_desc)
            },
            style = MaterialTheme.typography.bodySmall,
            color = AppPalette.textMuted,
            textAlign = TextAlign.Center
        )
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyGoalsState(onCreateGoal: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(88.dp).clip(CircleShape).background(LocalAccent.current.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Outlined.Savings, contentDescription = null, tint = LocalAccent.current, modifier = Modifier.size(44.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = stringResource(R.string.goals_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.goals_empty_desc), style = MaterialTheme.typography.bodyMedium, color = AppPalette.textMuted, textAlign = TextAlign.Center, lineHeight = TextUnit(20f, TextUnitType.Sp))
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onCreateGoal,
            modifier = Modifier.fillMaxWidth(0.8f).height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LocalAccent.current)
        ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.goals_create), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Auto-Allocation Rules ────────────────────────────────────────────────────

@Composable
private fun AutoAllocationRulesSection(rules: List<AutoAllocationRule>, onToggle: (String, Boolean) -> Unit, onEdit: (AutoAllocationRule) -> Unit, onDelete: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AppPalette.card), border = BorderStroke(1.dp, AppPalette.cardBorder)) {
        Column {
            rules.forEachIndexed { index, rule ->
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = rule.goalName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary)
                        Text(text = rule.description, style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                    }
                    Switch(checked = rule.isEnabled, onCheckedChange = { onToggle(rule.id, it) })
                }
                if (index < rules.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = AppPalette.cardBorder)
                }
            }
        }
    }
}

// ─── Dialog Host ──────────────────────────────────────────────────────────────

@Composable
private fun DialogHost(dialogState: GoalsDialogState, onEvent: (GoalsEvent) -> Unit, uiState: GoalsUiState) {
    when (dialogState) {
        is GoalsDialogState.None -> {}
        is GoalsDialogState.AddGoal -> { AddGoalDialog(initialDeadline = dialogState.deadline, onDismiss = { onEvent(GoalsEvent.DismissDialog) }, onCreateGoal = { name, amount, deadline, icon, color -> onEvent(GoalsEvent.CreateGoal(name, amount, deadline, icon, color)) }) }
        is GoalsDialogState.EditGoal -> { EditGoalDialog(goal = dialogState.goal, onDismiss = { onEvent(GoalsEvent.DismissDialog) }, onSave = { name, amount, deadline, icon, color, notes -> onEvent(GoalsEvent.UpdateGoal(dialogState.goal.id, name, amount, deadline, icon, color, notes)) }, onArchive = { onEvent(GoalsEvent.ShowArchiveGoalDialog(dialogState.goal.id)) }, onPause = { onEvent(GoalsEvent.PauseGoal(dialogState.goal.id)) }, onResume = { onEvent(GoalsEvent.ResumeGoal(dialogState.goal.id)) }) }
        is GoalsDialogState.ArchiveGoal -> {
            PremiumDialog(type = PremiumDialogType.ARCHIVE, customIcon = Icons.Outlined.Archive, title = stringResource(R.string.goals_archive),
                message = stringResource(R.string.archive_goal_desc, dialogState.goalName),
                confirmText = stringResource(R.string.goals_archive_confirm),
                dismissText = stringResource(R.string.cancel), onConfirm = { onEvent(GoalsEvent.ArchiveGoal(dialogState.goalId)) }, onDismiss = { onEvent(GoalsEvent.DismissDialog) })
        }
        is GoalsDialogState.Contribute -> { val goal = uiState.goals.find { it.id == dialogState.goalId }; if (goal != null) { ContributionBottomSheet(goal = goal, accounts = uiState.accounts, accountAllocations = uiState.accountAllocations, initialAccountId = dialogState.selectedAccountId, onDismiss = { onEvent(GoalsEvent.DismissDialog) }, onContribute = { accountId, amount -> onEvent(GoalsEvent.Contribute(dialogState.goalId, accountId, amount)) }) } }
        is GoalsDialogState.Withdraw -> { val goal = uiState.goals.find { it.id == dialogState.goalId }; if (goal != null) { WithdrawalBottomSheet(goal = goal, accounts = uiState.accounts, accountAllocations = uiState.accountAllocations, onDismiss = { onEvent(GoalsEvent.DismissDialog) }, onWithdraw = { accountId: String, amount: Double -> onEvent(GoalsEvent.Withdraw(dialogState.goalId, accountId, amount)) }) } }
        is GoalsDialogState.SetDailyTarget -> { SetDailyTargetDialog(currentAmount = dialogState.amount, onDismiss = { onEvent(GoalsEvent.DismissDialog) }, onSet = { amount -> onEvent(GoalsEvent.SetDailyTarget(amount)) }, onClear = { onEvent(GoalsEvent.ClearDailyTarget) }) }
        is GoalsDialogState.AddAutoAllocationRule, is GoalsDialogState.EditAutoAllocationRule -> { val rule = (dialogState as? GoalsDialogState.EditAutoAllocationRule)?.rule; val goalId = rule?.goalId ?: (dialogState as? GoalsDialogState.AddAutoAllocationRule)?.goalId; val goal = goalId?.let { id -> uiState.goals.find { it.id == id } }; AutoAllocationDialog(rule = rule, goal = goal, accounts = uiState.accounts, expenseCategories = uiState.expenseCategories, onDismiss = { onEvent(GoalsEvent.DismissDialog) }, onSave = { newRule -> if (rule != null) { onEvent(GoalsEvent.UpdateAutoAllocationRule(newRule)) } else { onEvent(GoalsEvent.AddAutoAllocationRule(newRule)) } }, onDelete = if (rule != null) { ruleId -> onEvent(GoalsEvent.DeleteAutoAllocationRule(ruleId)) } else null, onNavigateToAccounts = null) }
        is GoalsDialogState.GoalDetail -> { val goal = uiState.goals.find { it.id == dialogState.goalId }; if (goal != null) { GoalDetailScreen(goal = goal, dailyTarget = uiState.dailyTarget, contributions = uiState.selectedGoalContributions, onBack = { onEvent(GoalsEvent.DismissDialog) }, onEdit = { onEvent(GoalsEvent.ShowEditGoalDialog(goal.id)) }, onSetDailyTarget = { onEvent(GoalsEvent.ShowSetDailyTargetDialog) }, onSave = { onEvent(GoalsEvent.ShowContributeDialog(goal.id)) }, onWithdraw = { onEvent(GoalsEvent.ShowWithdrawDialog(goal.id)) }) } }
        is GoalsDialogState.LinkAccount, is GoalsDialogState.SelectAccount -> {}
        is GoalsDialogState.DeleteGoalConfirm -> {
            PremiumDialog(type = PremiumDialogType.DELETE, customIcon = Icons.Outlined.DeleteForever, title = stringResource(R.string.goal_delete_title),
                message = stringResource(R.string.goal_delete_permanent_desc, dialogState.goalName),
                confirmText = stringResource(R.string.goal_delete_permanently),
                dismissText = stringResource(R.string.cancel),
                onConfirm = { onEvent(GoalsEvent.DeleteGoalPermanently(dialogState.goalId)) },
                onDismiss = { onEvent(GoalsEvent.DismissDialog) })
        }
        is GoalsDialogState.RestoreGoalConfirm -> {
            PremiumDialog(type = PremiumDialogType.GOAL, customIcon = Icons.Outlined.Restore, title = stringResource(R.string.goal_restore_title),
                message = stringResource(R.string.goal_restore_desc, dialogState.goalName),
                confirmText = stringResource(R.string.goal_restore),
                dismissText = stringResource(R.string.cancel),
                onConfirm = { onEvent(GoalsEvent.RestoreGoal(dialogState.goalId)) },
                onDismiss = { onEvent(GoalsEvent.DismissDialog) })
        }
    }
}

@Composable
private fun SetDailyTargetDialog(currentAmount: String, onDismiss: () -> Unit, onSet: (Double) -> Unit, onClear: () -> Unit) {
    var amount by remember { mutableStateOf(currentAmount) }
    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = AppPalette.card,
        title = { Text(text = stringResource(R.string.daily_saving_target), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(text = stringResource(R.string.daily_target_set_desc), style = MaterialTheme.typography.bodyMedium, color = AppPalette.textMuted)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(stringResource(R.string.daily_amount)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, prefix = { Text("${NumberFormatter.getCurrencySymbol()} ") })
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("25000", "50000", "100000", "200000").forEach { value ->
                        FilterChip(selected = amount == value, onClick = { amount = value }, label = { Text("${NumberFormatter.getCurrencySymbol()} ${NumberFormatter.formatNumber(value.toDouble())}") })
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSet(parsedAmount) }, enabled = parsedAmount > 0) { Text(stringResource(R.string.goals_set_target)) } },
        dismissButton = { Row { if (currentAmount.isNotBlank()) { TextButton(onClick = onClear) { Text(stringResource(R.string.clear)) } }; TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } } })
}

@Composable
private fun EditGoalDialog(goal: Goal, onDismiss: () -> Unit, onSave: (name: String, amount: Double, deadline: java.time.LocalDate, icon: String, color: String, notes: String) -> Unit, onArchive: () -> Unit, onPause: () -> Unit, onResume: () -> Unit) {
    val defaultDeadline = goal.deadline ?: java.time.LocalDate.now().plusMonths(3)
    AddGoalDialog(initialDeadline = defaultDeadline, onDismiss = onDismiss, onCreateGoal = { name, amount, deadline, icon, color -> onSave(name, amount, deadline, icon, color, goal.notes) })
}
