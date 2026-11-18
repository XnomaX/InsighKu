package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChangeHistory
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.feature.planning.goal.data.model.AllocationTriggerType
import com.example.insightku.feature.planning.goal.domain.model.AutoAllocationRule

@Composable
internal fun AutoAllocationSection(
    rules: List<AutoAllocationRule>,
    goalColor: Color,
    onAddRule: () -> Unit,
    onEditRule: (AutoAllocationRule) -> Unit,
    onToggleRule: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.goal_auto_allocation),
            subtitle = if (rules.isEmpty()) stringResource(R.string.goal_auto_allocation_empty) else pluralStringResource(
                R.plurals.goal_auto_allocation_count, rules.size, rules.size
            )
        )
        Spacer(Modifier.height(Dimens.PaddingLarge))
        if (rules.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddRule),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                border = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.CardInnerPaddingLarge),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.goal_auto_automatic_savings), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary)
                        Text(stringResource(R.string.goal_auto_set_up_rules), style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                    }
                    Icon(Icons.Outlined.Add, null, tint = AppPalette.textMuted, modifier = Modifier.size(Dimens.IconSizeMedium))
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                border = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Column(modifier = Modifier.padding(vertical = Dimens.PaddingSmall)) {
                    rules.forEachIndexed { index, rule ->
                        AutoAllocationRuleItem(rule = rule, goalColor = goalColor, onClick = { onEditRule(rule) }, onToggle = { enabled -> onToggleRule(rule.id, enabled) })
                        if (index < rules.lastIndex) {
                            HorizontalDivider(color = AppPalette.cardBorder, thickness = 1.dp, modifier = Modifier.padding(horizontal = Dimens.CardInnerPaddingLarge))
                        }
                    }
                }
            }
            Spacer(Modifier.height(Dimens.PaddingMedium))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddRule),
                shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
                color = Color.Transparent,
                border = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingMedium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.Add, null, tint = AppPalette.textMuted, modifier = Modifier.size(Dimens.IconSizeSmall))
                    Spacer(Modifier.width(Dimens.PaddingSmall))
                    Text(stringResource(R.string.goal_auto_add_rule), style = MaterialTheme.typography.labelLarge, color = AppPalette.textPrimary)
                }
            }
        }
    }
}

@Composable
internal fun AutoAllocationRuleItem(rule: AutoAllocationRule, goalColor: Color, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    val triggerIcon = when (rule.triggerType) {
        AllocationTriggerType.INCOME_RECEIVED -> Icons.AutoMirrored.Outlined.TrendingUp
        AllocationTriggerType.SPENDING_CATEGORY -> Icons.Outlined.Category
        AllocationTriggerType.ROUND_UP -> Icons.Outlined.ChangeHistory
        AllocationTriggerType.DAILY, AllocationTriggerType.WEEKLY, AllocationTriggerType.BIWEEKLY, AllocationTriggerType.MONTHLY -> Icons.Outlined.Schedule
        AllocationTriggerType.BALANCE_ABOVE -> Icons.Outlined.AccountBalance
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.CardInnerPaddingLarge, vertical = Dimens.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
    ) {
        Icon(triggerIcon, null, tint = AppPalette.textMuted, modifier = Modifier.size(Dimens.IconSizeMedium))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = rule.triggerLabel(context), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = rule.description(context), style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Switch(checked = rule.isEnabled, onCheckedChange = onToggle)
    }
}
