package com.example.insightku.feature.budgeting.presentation
import com.example.insightku.core.ui.components.dialogs.CategoryIconResolver

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.core.data.model.BudgetFrequency
import com.example.insightku.core.data.model.Installment
import com.example.insightku.core.data.model.RecurringBudget
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.ui.theme.formatCurrency
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ─── Design tokens (theme-aware — react to light/dark + accent from Settings) ───

private val Purple: Color     @Composable get() = LocalAccent.current
private val Green      = Color(0xFF10B981)
private val Orange     = Color(0xFFFF9800)
private val Red        = Color(0xFFE57373)
private val Border: Color     @Composable get() = AppPalette.cardBorder
private val BgSurface: Color  @Composable get() = AppPalette.background

// ─── Recurring Payments Section ───────────────────────────────────────────────

@Composable
fun RecurringSection(
    recurringBudgets: List<RecurringBudget>,
    onAdd: () -> Unit,
    onEdit: (RecurringBudget) -> Unit,
    onDelete: (RecurringBudget) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Recurring Payments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary
                )
                Text(
                    text = if (recurringBudgets.isEmpty()) "No recurring payments"
                           else "${recurringBudgets.size} active • ${formatCurrency(recurringBudgets.sumOf { it.amount })} / mo",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }
            Surface(
                modifier = Modifier.clickable(onClick = onAdd),
                shape = RoundedCornerShape(50.dp),
                color = AppPalette.card,
                border = BorderStroke(1.dp, Purple)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Purple)
                    Text("Add", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Purple)
                }
            }
        }

        if (recurringBudgets.isEmpty()) {
            RecurringEmptyState(onAdd = onAdd)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recurringBudgets.forEach { budget ->
                    RecurringPaymentCard(
                        budget = budget,
                        onEdit = { onEdit(budget) },
                        onDelete = { onDelete(budget) }
                    )
                }
            }
        }
    }
}

@Composable
fun RecurringPaymentCard(
    budget: RecurringBudget,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysUntilDue = ((budget.nextDue - System.currentTimeMillis()) /
            (1000 * 60 * 60 * 24)).toInt()
    val dueStatus = dueStatusFor(daysUntilDue)
    val iconInfo = CategoryIconResolver.resolve(budget.name)

    var pressed by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label         = "card_scale"
    )

    if (showDeleteConfirm) {
        com.example.insightku.core.ui.components.dialogs.PremiumDeleteConfirmDialog(
            itemName = budget.name,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = { showDeleteConfirm = false; onDelete() },
            message = "\"${budget.name}\" will be removed from your recurring payments."
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                pressed = true
                onEdit()
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconInfo.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconInfo.icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = iconInfo.color
                )
            }

            // Name + category + due
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = budget.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FrequencyChip(budget.frequency)
                    DueChip(daysUntilDue, dueStatus)
                }
            }

            // Amount + actions
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = formatCurrency(budget.amount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp), tint = Color(0xFFB39DDB))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = Red.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringEmptyState(onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(Purple.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Repeat, contentDescription = null, tint = Purple.copy(alpha = 0.5f), modifier = Modifier.size(26.dp))
            }
            Text("No recurring payments yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary)
            Text("Track subscriptions, rent, and bills.", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
            Surface(
                modifier = Modifier.clickable(onClick = onAdd),
                shape = RoundedCornerShape(50.dp),
                color = AppPalette.card,
                border = BorderStroke(1.dp, Purple)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Purple)
                    Text("Add Recurring Payment", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Purple)
                }
            }
        }
    }
}

// ─── Installments Section ─────────────────────────────────────────────────────

@Composable
fun InstallmentsSection(
    installments: List<Installment>,
    onAdd: () -> Unit,
    onEdit: (Installment) -> Unit,
    onDelete: (Installment) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Installments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary
                )
                Text(
                    text = if (installments.isEmpty()) "No active installments"
                           else "${installments.size} active • ${formatCurrency(installments.sumOf { it.remainingBalance })} remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }
            Surface(
                modifier = Modifier.clickable(onClick = onAdd),
                shape = RoundedCornerShape(50.dp),
                color = AppPalette.card,
                border = BorderStroke(1.dp, Color(0xFF06B6D4))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF06B6D4))
                    Text("Add", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF06B6D4))
                }
            }
        }

        if (installments.isEmpty()) {
            InstallmentEmptyState(onAdd = onAdd)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                installments.forEach { installment ->
                    InstallmentCard(
                        installment = installment,
                        onEdit = { onEdit(installment) },
                        onDelete = { onDelete(installment) }
                    )
                }
            }
        }
    }
}

@Composable
fun InstallmentCard(
    installment: Installment,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysUntilDue = ((installment.nextDueDate - System.currentTimeMillis()) /
            (1000 * 60 * 60 * 24)).toInt()
    val dueStatus = dueStatusFor(daysUntilDue)
    val iconInfo = CategoryIconResolver.resolve(installment.name)

    var progressAnimated by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(installment.id) { progressAnimated = true }
    val animatedProgress by animateFloatAsState(
        targetValue   = if (progressAnimated) installment.progressFraction else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 150f),
        label         = "installment_progress"
    )

    if (showDeleteConfirm) {
        com.example.insightku.core.ui.components.dialogs.PremiumDeleteConfirmDialog(
            itemName = installment.name,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = { showDeleteConfirm = false; onDelete() },
            message = "\"${installment.name}\" will be removed from your installments."
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top row: icon + name + actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF06B6D4).copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconInfo.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF06B6D4)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = installment.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatCurrency(installment.monthlyPayment)} / month",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(15.dp), tint = Color(0xFFB39DDB))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(15.dp), tint = Red.copy(alpha = 0.7f))
                    }
                }
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${installment.paidMonths} / ${installment.totalMonths} months",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppPalette.textMuted
                    )
                    Text(
                        text = "${installment.progressPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Purple
                    )
                }
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50.dp)),
                    color = Purple,
                    trackColor = Border
                )
            }

            // Bottom row: remaining + due
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Remaining", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                    Text(
                        text = formatCurrency(installment.remainingBalance),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary
                    )
                }
                DueChip(daysUntilDue, dueStatus)
            }
        }
    }
}

@Composable
private fun InstallmentEmptyState(onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF06B6D4).copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CreditScore, contentDescription = null, tint = Color(0xFF06B6D4).copy(alpha = 0.5f), modifier = Modifier.size(26.dp))
            }
            Text("No installments yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary)
            Text("Track your cicilan and PayLater payments.", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
            Surface(
                modifier = Modifier.clickable(onClick = onAdd),
                shape = RoundedCornerShape(50.dp),
                color = AppPalette.card,
                border = BorderStroke(1.dp, Color(0xFF06B6D4))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF06B6D4))
                    Text("Add Installment", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF06B6D4))
                }
            }
        }
    }
}

// ─── Shared chips ─────────────────────────────────────────────────────────────

@Composable
private fun FrequencyChip(frequency: BudgetFrequency) {
    val label = when (frequency) {
        BudgetFrequency.WEEKLY    -> "Weekly"
        BudgetFrequency.BIWEEKLY  -> "Biweekly"
        BudgetFrequency.MONTHLY   -> "Monthly"
        BudgetFrequency.QUARTERLY -> "Quarterly"
        BudgetFrequency.YEARLY    -> "Yearly"
    }
    Surface(shape = RoundedCornerShape(50), color = Purple.copy(alpha = 0.08f)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Purple
        )
    }
}

private enum class DueStatus { OVERDUE, TODAY, SOON, NORMAL }

private fun dueStatusFor(daysUntilDue: Int): DueStatus = when {
    daysUntilDue < 0  -> DueStatus.OVERDUE
    daysUntilDue == 0 -> DueStatus.TODAY
    daysUntilDue <= 3 -> DueStatus.SOON
    else              -> DueStatus.NORMAL
}

@Composable
private fun DueChip(daysUntilDue: Int, status: DueStatus) {
    val (label, bg, fg) = when (status) {
        DueStatus.OVERDUE -> Triple("Overdue",          Red.copy(alpha = 0.12f),    Red)
        DueStatus.TODAY   -> Triple("Due Today",        Orange.copy(alpha = 0.12f), Orange)
        DueStatus.SOON    -> Triple("Due in ${daysUntilDue}d", Orange.copy(alpha = 0.08f), Orange)
        DueStatus.NORMAL  -> Triple(
            SimpleDateFormat("d MMM", Locale.ENGLISH).format(Date(
                System.currentTimeMillis() + TimeUnit.DAYS.toMillis(daysUntilDue.toLong())
            )),
            AppPalette.cardBorder, AppPalette.textMuted
        )
    }
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}





