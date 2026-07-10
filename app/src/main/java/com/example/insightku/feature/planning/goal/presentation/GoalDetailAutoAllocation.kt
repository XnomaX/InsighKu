package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.feature.planning.goal.data.model.AllocationTriggerType
import com.example.insightku.feature.planning.goal.domain.model.AutoAllocationRule
import com.example.insightku.feature.planning.goal.domain.model.Goal

@Composable
internal fun AutoAllocationSection(goal: Goal, rules: List<AutoAllocationRule>, goalColor: Color, onAddRule: () -> Unit, onEditRule: (AutoAllocationRule) -> Unit, onToggleRule: (String, Boolean) -> Unit, onDeleteRule: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = stringResource(R.string.goal_auto_allocation), subtitle = if (rules.isEmpty()) stringResource(R.string.goal_auto_allocation_empty) else stringResource(R.string.goal_auto_allocation_count, rules.size))
        Spacer(Modifier.height(12.dp))
        if (rules.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onAddRule), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = BorderStroke(1.dp, AppPalette.cardBorder)) {
                Row(modifier = Modifier.fillMaxWidth().padding(Dimens.CardInnerPadding), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = goalColor.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.goal_auto_automatic_savings), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary)
                        Text(stringResource(R.string.goal_auto_set_up_rules), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                    }
                    Icon(Icons.Outlined.Add, null, tint = goalColor, modifier = Modifier.size(20.dp))
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = BorderStroke(1.dp, AppPalette.cardBorder)) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    rules.forEachIndexed { index, rule ->
                        AutoAllocationRuleItem(rule = rule, goalColor = goalColor, onClick = { onEditRule(rule) }, onToggle = { enabled -> onToggleRule(rule.id, enabled) })
                        if (index < rules.lastIndex) { HorizontalDivider(color = AppPalette.cardBorder, thickness = 1.dp, modifier = Modifier.padding(horizontal = Dimens.CardInnerPadding)) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onAddRule), shape = RoundedCornerShape(Dimens.CardRadius), color = goalColor.copy(alpha = 0.08f), border = BorderStroke(1.dp, goalColor.copy(alpha = 0.2f))) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Outlined.Add, null, tint = goalColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.goal_auto_add_rule), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = goalColor)
                }
            }
        }
    }
}

@Composable
internal fun AutoAllocationRuleItem(rule: AutoAllocationRule, goalColor: Color, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val triggerIcon = when (rule.triggerType) {
        AllocationTriggerType.INCOME_RECEIVED -> Icons.Outlined.TrendingUp
        AllocationTriggerType.SPENDING_CATEGORY -> Icons.Outlined.Category
        AllocationTriggerType.ROUND_UP -> Icons.Outlined.ChangeHistory
        AllocationTriggerType.DAILY, AllocationTriggerType.WEEKLY, AllocationTriggerType.BIWEEKLY, AllocationTriggerType.MONTHLY -> Icons.Outlined.Schedule
        AllocationTriggerType.BALANCE_ABOVE -> Icons.Outlined.AccountBalance
    }
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Dimens.CardInnerPadding, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(goalColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(triggerIcon, null, tint = goalColor, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = rule.triggerLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = rule.description, style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Switch(checked = rule.isEnabled, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SuccessColor, uncheckedThumbColor = AppPalette.textMuted, uncheckedTrackColor = AppPalette.cardBorder))
    }
}
