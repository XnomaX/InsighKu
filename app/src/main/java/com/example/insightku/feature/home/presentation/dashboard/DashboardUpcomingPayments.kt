package com.example.insightku.feature.home.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.insightku.core.data.model.Installment
import com.example.insightku.core.data.model.RecurringBudget
import com.example.insightku.core.ui.theme.*
import com.example.insightku.feature.home.presentation.formatCurrencyShort

@Composable
fun UpcomingPaymentsSection(
    recurringBudgets: List<RecurringBudget>,
    installments: List<Installment>,
    onMarkRecurringPaid: (RecurringBudget) -> Unit,
    onMarkInstallmentPaid: (Installment) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val fourteenDays = 14L * 24 * 60 * 60 * 1000
    val upcomingRecurring = recurringBudgets.filter { it.isActive && it.nextDue <= now + fourteenDays }.sortedBy { it.nextDue }
    val upcomingInstallments = installments.filter { it.isActive && !it.isCompleted }.sortedBy { it.nextDueDate }

    val isEmpty = upcomingRecurring.isEmpty() && upcomingInstallments.isEmpty()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Upcoming",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary
                )
                Text(
                    if (isEmpty) "No payments due soon"
                    else "${upcomingRecurring.size + upcomingInstallments.size} payments due soon",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }
        }

        if (isEmpty) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(Dimens.CardRadiusLarge),
                color    = AppPalette.card,
                border   = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(CircleShape).background(NavPurple.copy(alpha = 0.07f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = NavPurple.copy(alpha = 0.45f), modifier = Modifier.size(26.dp))
                    }
                    Text("You're all caught up.", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary)
                    Text("No recurring or installment payments due in the next 14 days.", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted, textAlign = TextAlign.Center)
                }
            }
        } else {
            upcomingRecurring.forEach { budget ->
                UpcomingRecurringRow(budget = budget, now = now, onMarkPaid = { onMarkRecurringPaid(budget) })
            }
            upcomingInstallments.take(3).forEach { inst ->
                UpcomingInstallmentRow(installment = inst, now = now, onMarkPaid = { onMarkInstallmentPaid(inst) })
            }
        }
    }
}

@Composable
private fun UpcomingRecurringRow(
    budget: RecurringBudget,
    now: Long,
    onMarkPaid: () -> Unit
) {
    val daysUntil = ((budget.nextDue - now) / 86400000L).toInt().coerceAtLeast(0)
    val dueBadgeColor = when {
        daysUntil <= 2 -> ExpenseRed
        daysUntil <= 7 -> WarningYellow
        else           -> IncomeGreen
    }
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(Dimens.CardRadius),
        color           = AppPalette.card,
        tonalElevation  = 0.dp,
        shadowElevation = Dimens.ElevationSmall,
        border          = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.CardInnerPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NavPurple.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Repeat, null, tint = NavPurple, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    budget.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary,
                    maxLines = 1
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50.dp), color = dueBadgeColor.copy(alpha = 0.10f)) {
                        Text(
                            if (daysUntil == 0) "Today" else "in ${daysUntil}d",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = dueBadgeColor
                        )
                    }
                    Surface(shape = RoundedCornerShape(50.dp), color = NavPurple.copy(alpha = 0.08f)) {
                        Text(
                            "Recurring",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = NavPurple
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    formatCurrencyShort(budget.amount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary
                )
                Surface(
                    shape    = RoundedCornerShape(50.dp),
                    color    = AppPalette.card,
                    border   = BorderStroke(1.dp, IncomeGreen),
                    modifier = Modifier.clickable { onMarkPaid() }
                ) {
                    Text(
                        "Mark Paid",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = IncomeGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingInstallmentRow(
    installment: Installment,
    now: Long,
    onMarkPaid: () -> Unit
) {
    val daysUntil = ((installment.nextDueDate - now) / 86400000L).toInt().coerceAtLeast(0)
    val dueBadgeColor = when {
        daysUntil <= 2 -> ExpenseRed
        daysUntil <= 7 -> WarningYellow
        else           -> IncomeGreen
    }
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(Dimens.CardRadius),
        color           = AppPalette.card,
        tonalElevation  = 0.dp,
        shadowElevation = Dimens.ElevationSmall,
        border          = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.CardInnerPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF4A90E2).copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CreditCard, null, tint = Color(0xFF4A90E2), modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    installment.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary,
                    maxLines = 1
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50.dp), color = dueBadgeColor.copy(alpha = 0.10f)) {
                        Text(
                            if (daysUntil == 0) "Today" else "in ${daysUntil}d",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = dueBadgeColor
                        )
                    }
                    Surface(shape = RoundedCornerShape(50.dp), color = Color(0xFF4A90E2).copy(alpha = 0.08f)) {
                        Text(
                            "${installment.paidMonths}/${installment.totalMonths} paid",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4A90E2)
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    formatCurrencyShort(installment.monthlyPayment),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary
                )
                Surface(
                    shape    = RoundedCornerShape(50.dp),
                    color    = AppPalette.card,
                    border   = BorderStroke(1.dp, IncomeGreen),
                    modifier = Modifier.clickable { onMarkPaid() }
                ) {
                    Text(
                        "Mark Paid",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = IncomeGreen
                    )
                }
            }
        }
    }
}
