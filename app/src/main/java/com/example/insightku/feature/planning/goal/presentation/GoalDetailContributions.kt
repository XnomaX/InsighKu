package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.feature.planning.goal.domain.model.Contribution
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
internal fun ContributionSummarySection(uiState: GoalDetailUiState, goalColor: Color, modifier: Modifier = Modifier) {
    val lastDate = uiState.lastActivityDate?.let { DateFormatter.formatShortDate(it.toEpochMilli()) }
        ?: stringResource(R.string.goal_contrib_none)
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = stringResource(R.string.goal_savings_overview), subtitle = stringResource(R.string.goal_savings_overview_desc))
        Spacer(Modifier.height(Dimens.PaddingLarge))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.CardRadius),
            colors = CardDefaults.cardColors(containerColor = AppPalette.card),
            border = BorderStroke(1.dp, AppPalette.cardBorder)
        ) {
            Column(modifier = Modifier.padding(Dimens.CardInnerPaddingLarge)) {
                StatRow(stringResource(R.string.goal_deposits), "${uiState.totalContributions}")
                StatRow(
                    stringResource(R.string.goal_average),
                    if (uiState.averageContribution > 0) NumberFormatter.formatCurrencyCompact(uiState.averageContribution)
                    else stringResource(R.string.goal_contrib_none)
                )
                StatRow(stringResource(R.string.goal_last_deposit_date), lastDate)
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.PaddingMedium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AppPalette.textMuted)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary)
    }
}

@Composable
internal fun ContributionHistoryContent(
    contributions: List<Contribution>,
    accountMap: Map<String, Account>,
    goalColor: Color,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (contributions.isEmpty()) {
        EmptyContributionsCard(goalColor)
    } else {
        Column(modifier = modifier) {
            contributions.forEachIndexed { index, contribution ->
                val isWithdrawal = contribution.isWithdrawal
                val account = accountMap[contribution.accountId]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.CardInnerPaddingLarge),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isWithdrawal) stringResource(R.string.goal_contrib_withdrawal) else stringResource(R.string.goal_contrib_deposit),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = AppPalette.textPrimary
                        )
                        Text(account?.name ?: stringResource(R.string.goal_contrib_unknown_account), style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                        val date = contribution.createdAt.atZone(ZoneId.systemDefault()).toLocalDateTime()
                        Text("${date.dayOfMonth} ${date.month.name.take(3)} · ${date.hour.toString().padStart(2, '0')}:${date.minute.toString().padStart(2, '0')}", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                    }
                    Text(
                        "${if (isWithdrawal) "−" else "+"}${formatCurrencyFull(kotlin.math.abs(contribution.amount))}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = AppPalette.textPrimary
                    )
                }
                if (index < contributions.lastIndex) {
                    HorizontalDivider(color = AppPalette.cardBorder, modifier = Modifier.padding(horizontal = Dimens.CardInnerPaddingLarge))
                }
            }
            if (hasMore) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.CardInnerPaddingLarge), contentAlignment = Alignment.Center
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator(modifier = Modifier.size(Dimens.IconSizeLarge), color = LocalAccent.current, strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = onLoadMore) { Text(stringResource(R.string.goal_load_more), color = AppPalette.textMuted) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EmptyContributionsCard(goalColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.PaddingExtraLarge + Dimens.PaddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.goal_no_deposits),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = AppPalette.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Dimens.PaddingSmall))
            Text(
                stringResource(R.string.goal_no_deposits_desc),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Combined Savings Activity Card ───────────────────────────────────────────────

@Composable
internal fun SavingsActivityCombinedCard(
    contributions: List<Contribution>,
    goalColor: Color,
    goalStartDate: Instant,
    goalDeadline: LocalDate?,
    targetAmount: Double,
    accountMap: Map<String, Account>,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.goal_savings_activity),
            subtitle = stringResource(R.string.goal_savings_activity_desc),
        )
        Spacer(Modifier.height(Dimens.PaddingLarge))
        if (contributions.isEmpty()) {
            EmptyContributionsCard(goalColor)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                border = BorderStroke(1.dp, AppPalette.cardBorder),
            ) {
                Column {
                    ChartCardContent(
                        contributions = contributions,
                        goalColor = goalColor,
                        goalStartDate = goalStartDate,
                        goalDeadline = goalDeadline,
                        targetAmount = targetAmount,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = Dimens.CardInnerPaddingLarge),
                        color = AppPalette.cardBorder,
                    )
                    ContributionHistoryContent(
                        contributions = contributions,
                        accountMap = accountMap,
                        goalColor = goalColor,
                        isLoadingMore = isLoadingMore,
                        hasMore = hasMore,
                        onLoadMore = onLoadMore,
                    )
                }
            }
        }
    }
}
