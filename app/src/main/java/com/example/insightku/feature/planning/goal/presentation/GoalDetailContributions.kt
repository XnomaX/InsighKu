package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.PurpleViolet
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.core.ui.theme.formatCurrencyCompactIDR
import com.example.insightku.feature.planning.goal.domain.model.Contribution
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun ContributionSummarySection(uiState: GoalDetailUiState, goalColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Savings Overview", subtitle = "Track your progress towards this goal")
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContributionMiniCard(label = "Deposits", value = "${uiState.totalContributions}", subtitle = "total deposits", icon = Icons.Outlined.Savings, color = goalColor, modifier = Modifier.weight(1f))
                uiState.latestContribution?.let { latest ->
                    ContributionMiniCard(label = "Latest", value = formatCurrencyCompactIDR(kotlin.math.abs(latest.amount)), subtitle = "last deposit", icon = Icons.Outlined.TrendingDown, color = SuccessColor, modifier = Modifier.weight(1f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.averageContribution > 0) {
                    ContributionMiniCard(label = "Average", value = formatCurrencyCompactIDR(uiState.averageContribution), subtitle = "per deposit", icon = Icons.Outlined.Equalizer, color = PurpleViolet, modifier = Modifier.weight(1f))
                }
                uiState.lastActivityDate?.let { date ->
                    ContributionMiniCard(label = "Last Deposit", value = date.atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("d MMM")), subtitle = "most recent", icon = Icons.Outlined.Event, color = AppPalette.textMuted, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun ContributionMiniCard(label: String, value: String, subtitle: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = BorderStroke(1.dp, AppPalette.cardBorder)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = AppPalette.textPrimary)
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
internal fun ContributionHistorySection(contributions: List<Contribution>, accountMap: Map<String, Account>, goalColor: Color, isLoadingMore: Boolean, hasMore: Boolean, onLoadMore: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Savings Activity", subtitle = "All deposits and withdrawals")
        Spacer(Modifier.height(12.dp))
        if (contributions.isEmpty()) {
            EmptyContributionsCard(goalColor)
        } else {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = BorderStroke(1.dp, AppPalette.cardBorder)) {
                Column {
                    contributions.forEachIndexed { index, contribution ->
                        val isWithdrawal = contribution.isWithdrawal
                        val itemColor = if (isWithdrawal) ExpenseRed else SuccessColor
                        val account = accountMap[contribution.accountId]
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(itemColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(if (isWithdrawal) Icons.Outlined.ArrowUpward else Icons.Outlined.Add, null, tint = itemColor, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (isWithdrawal) "Withdrawal" else "Deposit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary)
                                Text(account?.name ?: "Unknown Account", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                                val date = contribution.createdAt.atZone(ZoneId.systemDefault()).toLocalDateTime()
                                Text("${date.dayOfMonth} ${date.month.name.take(3)} · ${date.hour.toString().padStart(2, '0')}:${date.minute.toString().padStart(2, '0')}", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${if (isWithdrawal) "-" else "+"}${formatCurrencyFull(kotlin.math.abs(contribution.amount))}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = itemColor)
                                if (contribution.notes.isNotBlank()) { Text(contribution.notes, style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted, maxLines = 1) }
                            }
                        }
                        if (index < contributions.lastIndex) { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = AppPalette.cardBorder) }
                    }
                    if (hasMore) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            if (isLoadingMore) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = goalColor, strokeWidth = 2.dp) }
                            else { TextButton(onClick = onLoadMore) { Text("Load More", color = goalColor) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EmptyContributionsCard(goalColor: Color) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.CardRadius), colors = CardDefaults.cardColors(containerColor = AppPalette.card), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = BorderStroke(1.dp, AppPalette.cardBorder)) {
        Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(goalColor.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Savings, null, tint = goalColor.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("No deposits yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary, textAlign = TextAlign.Center)
            Text("Make your first deposit to start building this goal", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted, textAlign = TextAlign.Center)
        }
    }
}
