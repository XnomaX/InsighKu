package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.insightku.R
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.components.FitText
import com.example.insightku.core.ui.components.dialogs.PremiumDialog
import com.example.insightku.core.ui.components.dialogs.PremiumDialogType
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
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
                        .weight(1f), contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LocalAccent.current, modifier = Modifier.size(Dimens.IconSizeLarge + Dimens.PaddingSmall), strokeWidth = 2.dp)
                }
            } else {
                GoalsContent(uiState = uiState, onEvent = viewModel::onEvent, modifier = Modifier.weight(1f))
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    LaunchedEffect(uiState.snackbarMessage) { uiState.snackbarMessage?.let { message -> snackbarHostState.showSnackbar(message); viewModel.onEvent(GoalsEvent.ClearSnackbar) } }
    LaunchedEffect(uiState.error) { uiState.error?.let { error -> snackbarHostState.showSnackbar(error); viewModel.onEvent(GoalsEvent.ClearError) } }

    if (uiState.showActionsSheet && uiState.actionsGoalId != null) {
        val actionsGoal = uiState.goals.find { it.id == uiState.actionsGoalId }
            ?: uiState.activeGoals.find { it.id == uiState.actionsGoalId }
            ?: uiState.pausedGoals.find { it.id == uiState.actionsGoalId }
        if (actionsGoal != null) {
            GoalActionsBottomSheet(
                goal = actionsGoal,
                onPause = { viewModel.onEvent(GoalsEvent.PauseGoal(actionsGoal.id)) },
                onResume = { viewModel.onEvent(GoalsEvent.ResumeGoal(actionsGoal.id)) },
                onComplete = { viewModel.onEvent(GoalsEvent.RequestCompleteGoal(actionsGoal.id)) },
                onArchive = { viewModel.onEvent(GoalsEvent.ArchiveGoal(actionsGoal.id)) },
                onEdit = { viewModel.onEvent(GoalsEvent.ShowEditGoalDialog(actionsGoal.id)) },
                onDismiss = { viewModel.onEvent(GoalsEvent.DismissActionsSheet) }
            )
        }
    }

    if (uiState.showArchivedSheet) {
        ArchivedGoalsSheet(
            archivedGoals = uiState.archivedGoals,
            onRestore = { goalId -> viewModel.onEvent(GoalsEvent.ShowRestoreGoalConfirm(goalId)) },
            onDelete = { goalId -> viewModel.onEvent(GoalsEvent.ShowDeleteGoalConfirm(goalId)) },
            onClick = { goalId -> onNavigateToGoalDetail?.invoke(goalId) },
            onDismiss = { viewModel.onEvent(GoalsEvent.DismissArchivedSheet) }
        )
    }

    if (uiState.showCompletionCelebration) {
        CompletionCelebrationOverlay(
            goalName = uiState.completionGoalName,
            onDismiss = { viewModel.onEvent(GoalsEvent.DismissCompletionCelebration) }
        )
    }

    DialogHost(uiState.dialogState, viewModel::onEvent, uiState)
}

// ─── Main Content ─────────────────────────────────────────────────────────────

@Composable
private fun GoalsContent(uiState: GoalsUiState, onEvent: (GoalsEvent) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.ScreenHorizontalPadding,
            end = Dimens.ScreenHorizontalPadding,
            top = Dimens.PaddingLarge,
            bottom = Dimens.ContentBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing)
    ) {
        // One aggregate figure leads the page. It already exists in GoalsUiState.
        uiState.goalSummary?.let { summary ->
            if (summary.totalGoals > 0) {
                item { GoalsAggregate(summary) }
            }
        }

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

        if (uiState.goals.isEmpty()) {
            item { EmptyGoalsState(onCreateGoal = { onEvent(GoalsEvent.ShowAddGoalDialog()) }) }
        } else if (uiState.displayedGoals.isEmpty()) {
            item {
                EmptyGoalsState(
                    tab = uiState.selectedTab,
                    onCreateGoal = { onEvent(GoalsEvent.ShowAddGoalDialog()) }
                )
            }
        } else {
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

        if (uiState.autoAllocationRules.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.auto_allocation_rules),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = AppPalette.textPrimary
                )
            }
            item {
                AutoAllocationRulesSection(
                    rules = uiState.autoAllocationRules,
                    onToggle = { ruleId, enabled -> onEvent(GoalsEvent.ToggleAutoAllocationRule(ruleId, enabled)) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(Dimens.PaddingLarge)) }
    }
}

// ─── Aggregate ────────────────────────────────────────────────────────────────

@Composable
private fun GoalsAggregate(summary: com.example.insightku.feature.planning.goal.domain.model.GoalSummary) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.goals_total_saved),
            style = MaterialTheme.typography.labelSmall,
            color = AppPalette.textMuted
        )
        Text(
            text = NumberFormatter.formatCurrencyCompact(summary.totalSaved),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color = AppPalette.textPrimary
        )
        Text(
            text = stringResource(
                R.string.goals_total_of,
                NumberFormatter.formatCurrencyCompact(summary.totalTarget),
                summary.activeGoals
            ),
            style = MaterialTheme.typography.bodySmall,
            color = AppPalette.textMuted
        )
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
        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
        ) {
            item {
                GoalFilterChip(stringResource(R.string.goals_active), activeCount, selectedTab == GoalFilterTab.ACTIVE) { onTabSelected(GoalFilterTab.ACTIVE) }
            }
            item {
                GoalFilterChip(stringResource(R.string.goals_paused_tab), pausedCount, selectedTab == GoalFilterTab.PAUSED) { onTabSelected(GoalFilterTab.PAUSED) }
            }
            item {
                GoalFilterChip(stringResource(R.string.goals_completed_tab), completedCount, selectedTab == GoalFilterTab.COMPLETED) { onTabSelected(GoalFilterTab.COMPLETED) }
            }
        }

        Surface(
            onClick = onArchivedClick,
            shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
            color = Color.Transparent,
            border = BorderStroke(1.dp, AppPalette.cardBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Dimens.PaddingMedium, vertical = Dimens.PaddingMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
            ) {
                Icon(Icons.Outlined.Archive, null, tint = AppPalette.textMuted, modifier = Modifier.size(Dimens.IconSizeSmall))
                if (archivedCount > 0) {
                    Text(
                        pluralStringResource(R.plurals.goals_archived_count, archivedCount, archivedCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppPalette.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalFilterChip(label: String, count: Int, isSelected: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
        color = if (isSelected) accent.copy(alpha = 0.12f) else Color.Transparent,
        border = BorderStroke(1.dp, if (isSelected) accent.copy(alpha = 0.3f) else AppPalette.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
        ) {
            FitText(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = if (isSelected) AppPalette.textPrimary else AppPalette.textMuted)
            if (count > 0) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppPalette.textMuted
                )
            }
        }
    }
}

// ─── Empty State (one component for all cases) ───────────────────────────────

@Composable
private fun EmptyGoalsState(onCreateGoal: () -> Unit, tab: GoalFilterTab? = null) {
    val icon = when (tab) {
        GoalFilterTab.PAUSED -> Icons.Outlined.PauseCircleOutline
        GoalFilterTab.COMPLETED -> Icons.Outlined.Check
        else -> Icons.Outlined.Flag
    }
    val (title, desc) = when (tab) {
        GoalFilterTab.PAUSED -> stringResource(R.string.goals_no_paused) to stringResource(R.string.goals_no_paused_desc)
        GoalFilterTab.ACTIVE -> stringResource(R.string.goals_no_active) to stringResource(R.string.goals_no_active_desc)
        GoalFilterTab.COMPLETED -> stringResource(R.string.goals_no_completed) to stringResource(R.string.goals_no_completed_desc)
        null -> stringResource(R.string.goals_empty_title) to stringResource(R.string.goals_empty_desc)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.PaddingExtraLarge + Dimens.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // An empty progress track, not a stock illustration: the product's own
        // object, drawn at rest, with the target it is waiting for.
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(Dimens.ProgressBarHeight)
                .clip(RoundedCornerShape(Dimens.PaddingSmall))
                .background(AppPalette.cardBorder)
        )
        Spacer(modifier = Modifier.height(Dimens.PaddingExtraLarge))
        Icon(imageVector = icon, contentDescription = null, tint = AppPalette.textMuted, modifier = Modifier.size(Dimens.IconSizeLarge))
        Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = AppPalette.textPrimary
        )
        Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.textMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimens.PaddingExtraLarge))
        Button(
            onClick = onCreateGoal,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(Dimens.ButtonHeightPrimary),
            shape = RoundedCornerShape(Dimens.ButtonRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = LocalAccent.current,
                contentColor = AppPalette.textPrimary
            )
        ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(Dimens.IconSizeMedium))
            Spacer(modifier = Modifier.width(Dimens.PaddingMedium))
            Text(stringResource(R.string.goals_create), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        }
    }
}

// ─── Auto-Allocation Rules ────────────────────────────────────────────────────

@Composable
private fun AutoAllocationRulesSection(
    rules: List<AutoAllocationRule>,
    onToggle: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column {
            rules.forEachIndexed { index, rule ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.CardInnerPaddingLarge), verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = rule.goalName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary)
                        Text(text = rule.description(context), style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                    }
                    Switch(checked = rule.isEnabled, onCheckedChange = { onToggle(rule.id, it) })
                }
                if (index < rules.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.CardInnerPaddingLarge), color = AppPalette.cardBorder)
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
        is GoalsDialogState.AddGoal -> { AddGoalDialog(initialDeadline = dialogState.deadline, onDismiss = { onEvent(GoalsEvent.DismissDialog) }, onCreateGoal = { name, amount, deadline, icon, color, notes, reminder -> onEvent(GoalsEvent.CreateGoal(name, amount, deadline, icon, color, notes, reminder)) }) }
        is GoalsDialogState.EditGoal -> {
            EditGoalDialog(
                goal = dialogState.goal,
                onDismiss = { onEvent(GoalsEvent.DismissDialog) },
                onSave = { name, amount, deadline, icon, color, notes, reminder ->
                    onEvent(GoalsEvent.UpdateGoal(dialogState.goal.id, name, amount, deadline, icon, color, notes, reminder))
                })
        }
        is GoalsDialogState.ArchiveGoal -> {
            PremiumDialog(type = PremiumDialogType.ARCHIVE, customIcon = Icons.Outlined.Archive, title = stringResource(R.string.goals_archive),
                message = stringResource(R.string.archive_goal_desc, dialogState.goalName),
                confirmText = stringResource(R.string.goals_archive_confirm),
                dismissText = stringResource(R.string.cancel), onConfirm = { onEvent(GoalsEvent.ArchiveGoal(dialogState.goalId)) }, onDismiss = { onEvent(GoalsEvent.DismissDialog) })
        }
        is GoalsDialogState.Contribute -> { val goal = uiState.goals.find { it.id == dialogState.goalId }; if (goal != null) { ContributionBottomSheet(goal = goal, accounts = uiState.accounts, accountAllocations = uiState.accountAllocations, initialAccountId = dialogState.selectedAccountId, onDismiss = { onEvent(GoalsEvent.DismissDialog) }, onContribute = { accountId, amount -> onEvent(GoalsEvent.Contribute(dialogState.goalId, accountId, amount)) }) } }
        is GoalsDialogState.Withdraw -> {
            val goal = uiState.goals.find { it.id == dialogState.goalId }; if (goal != null) {
                WithdrawalBottomSheet(
                    goal = goal,
                    accounts = uiState.accounts,
                    onDismiss = { onEvent(GoalsEvent.DismissDialog) },
                    onWithdraw = { accountId: String, amount: Double -> onEvent(GoalsEvent.Withdraw(dialogState.goalId, accountId, amount)) })
            }
        }
        is GoalsDialogState.SetDailyTarget -> { SetDailyTargetDialog(currentAmount = dialogState.amount, onDismiss = { onEvent(GoalsEvent.DismissDialog) }, onSet = { amount -> onEvent(GoalsEvent.SetDailyTarget(amount)) }, onClear = { onEvent(GoalsEvent.ClearDailyTarget) }) }
        is GoalsDialogState.AddAutoAllocationRule, is GoalsDialogState.EditAutoAllocationRule -> { val rule = (dialogState as? GoalsDialogState.EditAutoAllocationRule)?.rule; val goalId = rule?.goalId ?: (dialogState as? GoalsDialogState.AddAutoAllocationRule)?.goalId; val goal = goalId?.let { id -> uiState.goals.find { it.id == id } }; AutoAllocationDialog(rule = rule, goal = goal, accounts = uiState.accounts, expenseCategories = uiState.expenseCategories, onDismiss = { onEvent(GoalsEvent.DismissDialog) }, onSave = { newRule -> if (rule != null) { onEvent(GoalsEvent.UpdateAutoAllocationRule(newRule)) } else { onEvent(GoalsEvent.AddAutoAllocationRule(newRule)) } }, onDelete = if (rule != null) { ruleId -> onEvent(GoalsEvent.DeleteAutoAllocationRule(ruleId)) } else null, onNavigateToAccounts = null) }
        is GoalsDialogState.GoalDetail -> {
            val goalId = dialogState.goalId; GoalDetailScreen(
                goalId = goalId,
                onBack = { onEvent(GoalsEvent.DismissDialog) },
                onNavigateToAccounts = { onEvent(GoalsEvent.DismissDialog) },
                onGoalArchived = { onEvent(GoalsEvent.DismissDialog) })
        }
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
        is GoalsDialogState.CompleteGoalFunds -> {
            GoalFundsBottomSheet(goalName = dialogState.goalName, goalIcon = dialogState.goalIcon, goalColor = dialogState.goalColor,
                totalFunds = dialogState.fundsByAccount.values.sum(), fundsByAccount = dialogState.fundsByAccount,
                accounts = uiState.accounts, isDelete = false,
                onDismiss = { onEvent(GoalsEvent.DismissDialog) },
                onKeepOrReturn = { onEvent(GoalsEvent.CompleteGoalKeepFunds(dialogState.goalId)) },
                onTransfer = { targetId -> onEvent(GoalsEvent.CompleteGoalTransferFunds(dialogState.goalId, targetId)) })
        }
        is GoalsDialogState.DeleteGoalFunds -> {
            GoalFundsBottomSheet(goalName = dialogState.goalName, goalIcon = dialogState.goalIcon, goalColor = dialogState.goalColor,
                totalFunds = dialogState.fundsByAccount.values.sum(), fundsByAccount = dialogState.fundsByAccount,
                accounts = uiState.accounts, isDelete = true,
                onDismiss = { onEvent(GoalsEvent.DismissDialog) },
                onKeepOrReturn = { onEvent(GoalsEvent.DeleteGoalReturnFunds(dialogState.goalId)) },
                onTransfer = { targetId -> onEvent(GoalsEvent.DeleteGoalTransferFunds(dialogState.goalId, targetId)) })
        }
    }
}

@Composable
private fun SetDailyTargetDialog(currentAmount: String, onDismiss: () -> Unit, onSet: (Double) -> Unit, onClear: () -> Unit) {
    var amount by remember { mutableStateOf(currentAmount) }
    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(Dimens.BottomSheetRadius),
        containerColor = AppPalette.card,
        title = { Text(text = stringResource(R.string.daily_saving_target), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) },
        text = {
            Column {
                Text(text = stringResource(R.string.daily_target_set_desc), style = MaterialTheme.typography.bodyMedium, color = AppPalette.textMuted)
                Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(stringResource(R.string.daily_amount)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, prefix = { Text("${NumberFormatter.getCurrencySymbol()} ") })
                Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)) {
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
private fun EditGoalDialog(
    goal: Goal,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Double, deadline: java.time.LocalDate, icon: String, color: String, notes: String, reminderEnabled: Boolean) -> Unit
) {
    AddGoalDialog(
        initialDeadline = goal.deadline,
        initialName = goal.name,
        initialAmount = goal.targetAmount,
        initialIcon = goal.iconName,
        initialColor = goal.color,
        initialNotes = goal.notes,
        initialReminderEnabled = goal.reminderEnabled,
        currentAmount = goal.currentAmount,
        isEditing = true,
        onDismiss = onDismiss,
        onCreateGoal = { name, amount, deadline, icon, color, notes, reminder -> onSave(name, amount, deadline, icon, color, notes, reminder) }
    )
}
