package com.example.insightku.feature.planning.goal.presentation

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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.core.ui.components.dialogs.PremiumDialog
import com.example.insightku.core.ui.components.dialogs.PremiumDialogType
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.feature.planning.goal.domain.model.AutoAllocationRule
import com.example.insightku.feature.planning.goal.domain.model.Goal
import com.example.insightku.feature.planning.goal.domain.model.GoalSummary

@Composable
fun GoalsScreen(viewModel: GoalsViewModel = hiltViewModel(), onNavigateToGoalDetail: ((String) -> Unit)? = null) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.dialogState) { if (uiState.dialogState is GoalsDialogState.GoalDetail) { val goalId = (uiState.dialogState as GoalsDialogState.GoalDetail).goalId; if (onNavigateToGoalDetail != null) { onNavigateToGoalDetail(goalId); viewModel.onEvent(GoalsEvent.DismissDialog) } } }
    Box(modifier = Modifier.fillMaxSize().background(AppPalette.background)) { Column(modifier = Modifier.fillMaxSize()) { if (uiState.isLoading) { Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = LocalAccent.current, modifier = Modifier.size(28.dp), strokeWidth = 2.dp) } } else { GoalsContent(uiState = uiState, onEvent = viewModel::onEvent, modifier = Modifier.weight(1f)) } }; SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter)) }
    LaunchedEffect(uiState.snackbarMessage) { uiState.snackbarMessage?.let { message -> snackbarHostState.showSnackbar(message); viewModel.onEvent(GoalsEvent.ClearSnackbar) } }
    LaunchedEffect(uiState.error) { uiState.error?.let { error -> snackbarHostState.showSnackbar(error); viewModel.onEvent(GoalsEvent.ClearError) } }
    DialogHost(uiState.dialogState, viewModel::onEvent, uiState)
}

@Composable private fun GoalsContent(uiState: GoalsUiState, onEvent: (GoalsEvent) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (uiState.hasGoals && uiState.goalSummary != null) { item { GoalsSummaryStrip(summary = uiState.goalSummary!!) } }
        if (uiState.goals.isEmpty()) { item { EmptyGoalsState(onCreateGoal = { onEvent(GoalsEvent.ShowAddGoalDialog()) }) } } else { item { Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(text = "Your Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary); Text(text = "${uiState.goals.size} goal${if (uiState.goals.size > 1) "s" else ""}", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted) } }; items(uiState.goals, key = { it.id }) { goal -> GoalCard(goal = goal, onClick = { onEvent(GoalsEvent.ShowGoalDetail(goal.id)) }, onContribute = { onEvent(GoalsEvent.ShowContributeDialog(goal.id)) }, onWithdraw = { onEvent(GoalsEvent.ShowWithdrawDialog(goal.id)) }, onEdit = { onEvent(GoalsEvent.ShowEditGoalDialog(goal.id)) }) } }
        if (uiState.autoAllocationRules.isNotEmpty()) { item { Spacer(modifier = Modifier.height(8.dp)); Text(text = "Auto-Allocation Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary) }; item { AutoAllocationRulesSection(rules = uiState.autoAllocationRules, onToggle = { ruleId, enabled -> onEvent(GoalsEvent.ToggleAutoAllocationRule(ruleId, enabled)) }, onEdit = { rule -> onEvent(GoalsEvent.ShowEditAutoAllocationRuleDialog(rule)) }, onDelete = { ruleId -> onEvent(GoalsEvent.DeleteAutoAllocationRule(ruleId)) }) } }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable fun GoalsSummaryStrip(summary: GoalSummary, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationSmall), border = androidx.compose.foundation.BorderStroke(1.dp, AppPalette.cardBorder)) {
        Row(modifier = Modifier.fillMaxWidth().padding(Dimens.CardInnerPaddingLarge), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            SummaryMetric(value = formatCompact(summary.totalSaved), label = "Saved", icon = Icons.Outlined.Savings, valueColor = SuccessColor)
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(AppPalette.cardBorder))
            SummaryMetric(value = "${summary.overallProgress.toInt()}%", label = "Progress", icon = Icons.Outlined.TrendingUp, valueColor = LocalAccent.current)
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(AppPalette.cardBorder))
            SummaryMetric(value = "${summary.activeGoals}", label = "Active", icon = Icons.Outlined.Flag, valueColor = AppPalette.textPrimary)
        }
    }
}

@Composable private fun SummaryMetric(value: String, label: String, icon: ImageVector, valueColor: Color = AppPalette.textPrimary) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) { Icon(imageVector = icon, contentDescription = null, tint = LocalAccent.current, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.height(6.dp)); Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor); Text(text = label, style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted) } }
private fun formatCompact(amount: Double): String = NumberFormatter.formatCurrencyCompact(amount)

@Composable private fun EmptyGoalsState(onCreateGoal: () -> Unit) { Column(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(modifier = Modifier.size(88.dp).clip(CircleShape).background(LocalAccent.current.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Outlined.Savings, contentDescription = null, tint = LocalAccent.current, modifier = Modifier.size(44.dp)) }; Spacer(modifier = Modifier.height(20.dp)); Text(text = "No Goals Yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary); Spacer(modifier = Modifier.height(8.dp)); Text(text = "Set your first savings goal and start\ntracking your progress toward financial freedom", style = MaterialTheme.typography.bodyMedium, color = AppPalette.textMuted, textAlign = TextAlign.Center, lineHeight = TextUnit(20f, TextUnitType.Sp)); Spacer(modifier = Modifier.height(28.dp)); Button(onClick = onCreateGoal, modifier = Modifier.fillMaxWidth(0.8f).height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = LocalAccent.current)) { Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Create Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) } } }

@Composable private fun AutoAllocationRulesSection(rules: List<AutoAllocationRule>, onToggle: (String, Boolean) -> Unit, onEdit: (AutoAllocationRule) -> Unit, onDelete: (String) -> Unit) { Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AppPalette.card), border = androidx.compose.foundation.BorderStroke(1.dp, AppPalette.cardBorder)) { Column { rules.forEachIndexed { index, rule -> Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1f)) { Text(text = rule.goalName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary); Text(text = rule.description, style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted) }; Switch(checked = rule.isEnabled, onCheckedChange = { onToggle(rule.id, it) }) }; if (index < rules.lastIndex) { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = AppPalette.cardBorder) } } } } }

@Composable private fun DialogHost(dialogState: GoalsDialogState, onEvent: (GoalsEvent) -> Unit, uiState: GoalsUiState) {
    when (dialogState) {
        is GoalsDialogState.None -> {}
        is GoalsDialogState.AddGoal -> { AddGoalDialog(initialDeadline = dialogState.deadline, onDismiss = { onEvent(GoalsEvent.DismissDialog) }, onCreateGoal = { name, amount, deadline, icon, color -> onEvent(GoalsEvent.CreateGoal(name, amount, deadline, icon, color)) }) }
        is GoalsDialogState.EditGoal -> { EditGoalDialog(goal = dialogState.goal, onDismiss = { onEvent(GoalsEvent.DismissDialog) }, onSave = { name, amount, deadline, icon, color, notes -> onEvent(GoalsEvent.UpdateGoal(dialogState.goal.id, name, amount, deadline, icon, color, notes)) }, onArchive = { onEvent(GoalsEvent.ShowArchiveGoalDialog(dialogState.goal.id)) }, onPause = { onEvent(GoalsEvent.PauseGoal(dialogState.goal.id)) }, onResume = { onEvent(GoalsEvent.ResumeGoal(dialogState.goal.id)) }) }
        is GoalsDialogState.ArchiveGoal -> { PremiumDialog(type = PremiumDialogType.ARCHIVE, customIcon = Icons.Outlined.Archive, title = "Archive Goal?", message = "Are you sure you want to archive \"${dialogState.goalName}\"? You can still view its history but it won't appear in your active goals.", confirmText = "Archive", dismissText = "Cancel", onConfirm = { onEvent(GoalsEvent.ArchiveGoal(dialogState.goalId)) }, onDismiss = { onEvent(GoalsEvent.DismissDialog) }) }
        is GoalsDialogState.Contribute -> { val goal = uiState.goals.find { it.id == dialogState.goalId }; if (goal != null) { ContributionBottomSheet(goal = goal, accounts = uiState.accounts, accountAllocations = uiState.accountAllocations, initialAccountId = dialogState.selectedAccountId, onDismiss = { onEvent(GoalsEvent.DismissDialog) }, onContribute = { accountId, amount -> onEvent(GoalsEvent.Contribute(dialogState.goalId, accountId, amount)) }) } }
        is GoalsDialogState.Withdraw -> { val goal = uiState.goals.find { it.id == dialogState.goalId }; if (goal != null) { WithdrawalBottomSheet(goal = goal, accounts = uiState.accounts, accountAllocations = uiState.accountAllocations, onDismiss = { onEvent(GoalsEvent.DismissDialog) }, onWithdraw = { accountId: String, amount: Double -> onEvent(GoalsEvent.Withdraw(dialogState.goalId, accountId, amount)) }) } }
        is GoalsDialogState.SetDailyTarget -> { SetDailyTargetDialog(currentAmount = dialogState.amount, onDismiss = { onEvent(GoalsEvent.DismissDialog) }, onSet = { amount -> onEvent(GoalsEvent.SetDailyTarget(amount)) }, onClear = { onEvent(GoalsEvent.ClearDailyTarget) }) }
        is GoalsDialogState.AddAutoAllocationRule, is GoalsDialogState.EditAutoAllocationRule -> { val rule = (dialogState as? GoalsDialogState.EditAutoAllocationRule)?.rule; val goalId = rule?.goalId ?: (dialogState as? GoalsDialogState.AddAutoAllocationRule)?.goalId; val goal = goalId?.let { id -> uiState.goals.find { it.id == id } }; AutoAllocationDialog(rule = rule, goal = goal, accounts = uiState.accounts, expenseCategories = uiState.expenseCategories, onDismiss = { onEvent(GoalsEvent.DismissDialog) }, onSave = { newRule -> if (rule != null) { onEvent(GoalsEvent.UpdateAutoAllocationRule(newRule)) } else { onEvent(GoalsEvent.AddAutoAllocationRule(newRule)) } }, onDelete = if (rule != null) { ruleId -> onEvent(GoalsEvent.DeleteAutoAllocationRule(ruleId)) } else null, onNavigateToAccounts = null) }
        is GoalsDialogState.GoalDetail -> { val goal = uiState.goals.find { it.id == dialogState.goalId }; if (goal != null) { GoalDetailScreen(goal = goal, dailyTarget = uiState.dailyTarget, contributions = uiState.selectedGoalContributions, onBack = { onEvent(GoalsEvent.DismissDialog) }, onEdit = { onEvent(GoalsEvent.ShowEditGoalDialog(goal.id)) }, onSetDailyTarget = { onEvent(GoalsEvent.ShowSetDailyTargetDialog) }, onSave = { onEvent(GoalsEvent.ShowContributeDialog(goal.id)) }, onWithdraw = { onEvent(GoalsEvent.ShowWithdrawDialog(goal.id)) }) } }
        is GoalsDialogState.LinkAccount, is GoalsDialogState.SelectAccount -> {}
    }
}

@Composable private fun SetDailyTargetDialog(currentAmount: String, onDismiss: () -> Unit, onSet: (Double) -> Unit, onClear: () -> Unit) { var amount by remember { mutableStateOf(currentAmount) }; val parsedAmount = amount.toDoubleOrNull() ?: 0.0; AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(28.dp), containerColor = AppPalette.card, title = { Text(text = "Daily Saving Target", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }, text = { Column { Text(text = "Set an amount to save each day", style = MaterialTheme.typography.bodyMedium, color = AppPalette.textMuted); Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Daily Amount") }, modifier = Modifier.fillMaxWidth(), singleLine = true, prefix = { Text("${NumberFormatter.getCurrencySymbol()} ") }); Spacer(modifier = Modifier.height(12.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("25000", "50000", "100000", "200000").forEach { value -> FilterChip(selected = amount == value, onClick = { amount = value }, label = { Text("${NumberFormatter.getCurrencySymbol()} ${NumberFormatter.formatNumber(value.toDouble())}") }) } } } }, confirmButton = { Button(onClick = { onSet(parsedAmount) }, enabled = parsedAmount > 0) { Text("Set Target") } }, dismissButton = { Row { if (currentAmount.isNotBlank()) { TextButton(onClick = onClear) { Text("Clear") } }; TextButton(onClick = onDismiss) { Text("Cancel") } } }) }

@Composable private fun EditGoalDialog(goal: Goal, onDismiss: () -> Unit, onSave: (name: String, amount: Double, deadline: java.time.LocalDate?, icon: String, color: String, notes: String) -> Unit, onArchive: () -> Unit, onPause: () -> Unit, onResume: () -> Unit) { AddGoalDialog(initialDeadline = goal.deadline, onDismiss = onDismiss, onCreateGoal = { name, amount, deadline, icon, color -> onSave(name, amount, deadline, icon, color, goal.notes) }) }
