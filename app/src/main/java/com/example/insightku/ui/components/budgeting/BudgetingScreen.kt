package com.example.insightku.ui.components.budgeting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.data.model.Category
import com.example.insightku.ui.dialogs.AddCategoryDialog
import com.example.insightku.ui.dialogs.EditCategoryDialog
import com.example.insightku.ui.dialogs.RecurringBudgetsDialog
import com.example.insightku.ui.theme.Dimens
import com.example.insightku.ui.theme.formatCurrency
import com.example.insightku.viewmodel.BudgetingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val BudgetScreenPadding = 18.dp
private val BudgetCardRadius = 20.dp
private val BudgetItemRadius = 18.dp
private val SafeGreen = Color(0xFF16A34A)
private val WarningYellow = Color(0xFFEAB308)

@Composable
fun BudgetingScreen(
    viewModel: BudgetingViewModel = hiltViewModel(),
    onAddTransaction: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    BudgetingScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onAddTransaction = onAddTransaction
    )

    HandleDialogs(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
fun BudgetingScreenContent(
    uiState: BudgetingUiState,
    onEvent: (BudgetingEvent) -> Unit,
    onAddTransaction: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = BudgetScreenPadding,
                top = Dimens.PaddingLarge,
                end = BudgetScreenPadding,
                bottom = 104.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                BudgetHeader(
                    month = currentMonthLabel(),
                    isLoading = uiState.isLoading
                )
            }

            if (uiState.error != null) {
                item {
                    BudgetErrorCard(
                        message = uiState.error,
                        onDismiss = { onEvent(BudgetingEvent.ClearError) }
                    )
                }
            }

            item {
                BudgetStatusCard(
                    remaining = uiState.remainingBudget,
                    percentage = uiState.budgetUtilizationPercentage,
                    totalBudget = uiState.totalBudget,
                    limitedSpent = uiState.limitedSpent,
                    riskyCount = uiState.budgetCategories.count { it.health == BudgetHealth.Warning },
                    overBudgetCount = uiState.overBudgetCategories.size
                )
            }

            item {
                SectionTitle(
                    title = "Category budgets",
                    subtitle = categoryListSubtitle(uiState)
                )
            }

            if (!uiState.isLoading && uiState.budgetCategories.isEmpty()) {
                item {
                    EmptyBudgetState(onAddCategory = { onEvent(BudgetingEvent.ShowAddBudgetDialog) })
                }
            } else {
                items(
                    items = uiState.budgetCategories,
                    key = { it.id }
                ) { category ->
                    BudgetCategoryCard(
                        category = category,
                        onEdit = { onEvent(BudgetingEvent.ShowEditBudgetDialog(category)) }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAddTransaction,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Add transaction") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = BudgetScreenPadding, bottom = Dimens.PaddingLarge),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(18.dp)
        )

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun HandleDialogs(uiState: BudgetingUiState, onEvent: (BudgetingEvent) -> Unit) {
    when (val dialogState = uiState.dialogState) {
        is DialogState.AddBudget -> {
            AddCategoryDialog(
                isOpen = true,
                onDismiss = { onEvent(BudgetingEvent.HideAddBudgetDialog) },
                onCategoryAdded = { category -> onEvent(BudgetingEvent.AddCategory(category)) }
            )
        }

        is DialogState.EditBudget -> {
            EditCategoryDialog(
                isOpen = true,
                onDismiss = { onEvent(BudgetingEvent.HideEditBudgetDialog) },
                category = Category(
                    id = dialogState.category.id,
                    name = dialogState.category.name,
                    budgetLimit = dialogState.category.budgetedAmount,
                    color = dialogState.category.color,
                    icon = dialogState.category.icon
                ),
                onCategoryEdited = { onEvent(BudgetingEvent.UpdateCategory(it)) },
                onCategoryDeleted = { onEvent(BudgetingEvent.DeleteCategory(dialogState.category.id)) }
            )
        }

        is DialogState.ManageRecurring -> {
            RecurringBudgetsDialog(
                isOpen = true,
                onDismiss = { onEvent(BudgetingEvent.HideRecurringBudgetsDialog) },
                recurringBudgets = dialogState.budgets,
                onBudgetAdded = { onEvent(BudgetingEvent.AddRecurringBudget(it)) },
                onBudgetEdited = { onEvent(BudgetingEvent.UpdateRecurringBudget(it)) },
                onBudgetDeleted = { onEvent(BudgetingEvent.DeleteRecurringBudget(it)) }
            )
        }

        DialogState.None -> Unit
    }
}

@Composable
private fun BudgetHeader(
    month: String,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Budgeting",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = month,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isLoading) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "Syncing",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BudgetStatusCard(
    remaining: Double,
    percentage: Double,
    totalBudget: Double,
    limitedSpent: Double,
    riskyCount: Int,
    overBudgetCount: Int
) {
    val status = monthlyStatus(remaining, percentage, totalBudget)
    val statusColor = budgetStatusColor(status.health)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BudgetCardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Remaining this month",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(remaining),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                BudgetStatePill(text = status.label, color = statusColor)
            }

            LinearProgressIndicator(
                progress = { (percentage / 100.0).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = budgetProgressColor(percentage),
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (totalBudget > 0) {
                        "${formatCurrency(limitedSpent)} of ${formatCurrency(totalBudget)} used"
                    } else {
                        "No monthly limits set yet"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(Dimens.PaddingMedium))
                Text(
                    text = "${percentage.formatPercent()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            RiskSignalRow(
                riskyCount = riskyCount,
                overBudgetCount = overBudgetCount
            )
        }
    }
}

@Composable
private fun RiskSignalRow(
    riskyCount: Int,
    overBudgetCount: Int
) {
    val color = when {
        overBudgetCount > 0 -> MaterialTheme.colorScheme.error
        riskyCount > 0 -> WarningYellow
        else -> SafeGreen
    }
    val text = when {
        overBudgetCount > 0 -> "$overBudgetCount over budget"
        riskyCount > 0 -> "$riskyCount need attention"
        else -> "All limited categories are safe"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.10f),
        contentColor = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (overBudgetCount > 0) Icons.Default.Warning else Icons.Default.TrendingFlat,
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconSizeMedium)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun BudgetErrorCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.PaddingLarge),
            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null)
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Dismiss",
                modifier = Modifier.clickable(onClick = onDismiss),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BudgetCategoryCard(
    category: BudgetCategory,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = budgetStatusColor(category.health)
    val categoryColor = parseCategoryColor(category.color)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BudgetItemRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = categoryColor.copy(alpha = 0.14f),
                    contentColor = categoryColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = categoryIcon(category),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = budgetSpentText(category),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (category.hasLimit) "Remaining" else "Tracked",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (category.hasLimit) formatCurrency(category.remainingAmount) else "No limit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (category.hasLimit) statusColor else MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit ${category.name}",
                        modifier = Modifier.size(Dimens.IconSizeMedium),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { category.progressFraction },
                    modifier = Modifier
                        .weight(1f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
                BudgetStatePill(
                    text = categoryStatusText(category),
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun BudgetStatePill(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.11f),
        contentColor = color
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyBudgetState(onAddCategory: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BudgetItemRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
        ) {
            Text(
                text = "No budget categories yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Start with a few monthly limits. Categories without limits still track spending.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Surface(
                modifier = Modifier.clickable(onClick = onAddCategory),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(Dimens.IconSizeMedium))
                    Text("Add category", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun budgetStatusColor(health: BudgetHealth): Color {
    return when (health) {
        BudgetHealth.Unlimited -> MaterialTheme.colorScheme.primary
        BudgetHealth.Good -> SafeGreen
        BudgetHealth.Warning -> WarningYellow
        BudgetHealth.Over -> MaterialTheme.colorScheme.error
    }
}

@Composable
private fun budgetProgressColor(percentage: Double): Color {
    return when {
        percentage < 70.0 -> budgetStatusColor(BudgetHealth.Good)
        percentage <= 100.0 -> budgetStatusColor(BudgetHealth.Warning)
        else -> budgetStatusColor(BudgetHealth.Over)
    }
}

private data class MonthlyBudgetStatus(
    val label: String,
    val health: BudgetHealth
)

private fun monthlyStatus(
    remaining: Double,
    percentage: Double,
    totalBudget: Double
): MonthlyBudgetStatus {
    return when {
        totalBudget <= 0.0 -> MonthlyBudgetStatus("No limits", BudgetHealth.Unlimited)
        remaining < 0.0 -> MonthlyBudgetStatus("Over budget", BudgetHealth.Over)
        percentage >= 70.0 -> MonthlyBudgetStatus("Watch closely", BudgetHealth.Warning)
        else -> MonthlyBudgetStatus("Safe", BudgetHealth.Good)
    }
}

private fun categoryListSubtitle(uiState: BudgetingUiState): String {
    val total = uiState.budgetCategories.size
    val over = uiState.overBudgetCategories.size
    val warning = uiState.budgetCategories.count { it.health == BudgetHealth.Warning }
    return when {
        total == 0 -> "No categories to monitor yet"
        over > 0 -> "$over over budget, $total total"
        warning > 0 -> "$warning close to limit, $total total"
        else -> "$total categories monitored"
    }
}

private fun budgetSpentText(category: BudgetCategory): String {
    return if (category.hasLimit) {
        "${formatCurrencyPlain(category.spentAmount)} spent of ${formatCurrencyPlain(category.limitAmount)}"
    } else {
        "${formatCurrencyPlain(category.spentAmount)} spent, unlimited"
    }
}

private fun categoryStatusText(category: BudgetCategory): String {
    return when (category.health) {
        BudgetHealth.Unlimited -> "No limit"
        BudgetHealth.Good -> "Safe"
        BudgetHealth.Warning -> "${category.utilizationPercentage.formatPercent()}%"
        BudgetHealth.Over -> "Over"
    }
}

private fun formatCurrencyPlain(amount: Double): String {
    val absAmount = abs(amount)
    val sign = if (amount < 0) "-" else ""
    return when {
        absAmount >= 1_000_000 -> "${sign}Rp${String.format(Locale.getDefault(), "%.1f", absAmount / 1_000_000)}M"
        absAmount >= 1_000 -> "${sign}Rp${String.format(Locale.getDefault(), "%.0f", absAmount / 1_000)}K"
        else -> "${sign}Rp${absAmount.toInt()}"
    }
}

private fun parseCategoryColor(value: String): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(value.ifBlank { "#79747E" }))
    }.getOrDefault(Color(0xFF79747E))
}

private fun categoryIcon(category: BudgetCategory): ImageVector {
    val icon = category.icon.lowercase(Locale.getDefault())
    val name = category.name.lowercase(Locale.getDefault())
    return when {
        "food" in icon || "drink" in icon || "restaurant" in icon || "food" in name -> Icons.Default.Restaurant
        "transport" in icon || "car" in icon || "transport" in name -> Icons.Default.DirectionsCar
        "bill" in icon || "receipt" in icon || "bill" in name -> Icons.Default.Receipt
        "utility" in icon || "bolt" in icon || "utility" in name -> Icons.Default.Bolt
        "shopping" in icon || "shopping" in name -> Icons.Default.ShoppingBag
        "entertainment" in icon || "game" in icon || "movie" in icon || "lifestyle" in name -> Icons.Default.SportsEsports
        "health" in icon || "health" in name -> Icons.Default.LocalHospital
        "housing" in icon || "home" in icon || "rent" in name -> Icons.Default.Home
        "coffee" in icon || "cafe" in name -> Icons.Default.Coffee
        category.icon.isBlank() -> Icons.Default.Category
        else -> Icons.Default.MoreHoriz
    }
}

private fun currentMonthLabel(): String {
    return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
}

private fun Double.formatPercent(): String {
    return if (abs(this - toInt()) < 0.05) {
        toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", this)
    }
}
