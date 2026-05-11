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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

@Composable
fun BudgetingScreen(
    viewModel: BudgetingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BudgetingScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )

    HandleDialogs(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
fun BudgetingScreenContent(
    uiState: BudgetingUiState,
    onEvent: (BudgetingEvent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimens.PaddingLarge,
                top = Dimens.PaddingLarge,
                end = Dimens.PaddingLarge,
                bottom = Dimens.PaddingExtraLarge
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
        ) {
            item {
                BudgetHeader(
                    month = currentMonthLabel(),
                    isLoading = uiState.isLoading,
                    onRefresh = { onEvent(BudgetingEvent.RefreshData) }
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
                BudgetSummaryCard(
                    totalBudget = uiState.totalBudget,
                    totalSpent = uiState.totalSpent,
                    limitedSpent = uiState.limitedSpent,
                    unlimitedSpent = uiState.unlimitedSpent,
                    remaining = uiState.remainingBudget,
                    percentage = uiState.budgetUtilizationPercentage.toFloat(),
                    overBudgetCategories = uiState.overBudgetCategories,
                    onAddCategory = { onEvent(BudgetingEvent.ShowAddBudgetDialog) }
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
                    CategoryBudgetCard(
                        category = category,
                        onEdit = { onEvent(BudgetingEvent.ShowEditBudgetDialog(category)) }
                    )
                }
            }
        }

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
    isLoading: Boolean,
    onRefresh: () -> Unit
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

        OutlinedButton(
            onClick = onRefresh,
            enabled = !isLoading,
            shape = RoundedCornerShape(Dimens.CornerRadiusMedium)
        ) {
            Text(if (isLoading) "Syncing" else "Refresh")
        }
    }
}

@Composable
private fun BudgetErrorCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        shape = RoundedCornerShape(Dimens.CornerRadiusMedium)
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
fun BudgetSummaryCard(
    totalBudget: Double,
    totalSpent: Double,
    limitedSpent: Double = totalSpent,
    unlimitedSpent: Double = 0.0,
    remaining: Double,
    percentage: Float,
    overBudgetCategories: List<BudgetCategory>,
    onAddCategory: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(Dimens.PaddingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
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
                        color = if (remaining >= 0) budgetStatusColor(BudgetHealth.Good) else budgetStatusColor(BudgetHealth.Over)
                    )
                }

                Button(
                    onClick = onAddCategory,
                    shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
                    contentPadding = PaddingValues(horizontal = Dimens.PaddingMedium, vertical = Dimens.PaddingMedium)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(Dimens.IconSizeMedium))
                    Spacer(modifier = Modifier.width(Dimens.PaddingSmall))
                    Text("Category")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Limited budget used",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${percentage.coerceAtLeast(0f).formatPercent()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { (percentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = budgetProgressColor(percentage.toDouble()),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
            ) {
                SummaryMetric(
                    label = "Budget",
                    value = formatCurrency(totalBudget),
                    modifier = Modifier.weight(1f)
                )
                SummaryMetric(
                    label = "Spent",
                    value = formatCurrency(totalSpent),
                    modifier = Modifier.weight(1f)
                )
                SummaryMetric(
                    label = "No limit",
                    value = formatCurrency(unlimitedSpent),
                    modifier = Modifier.weight(1f)
                )
            }

            if (overBudgetCategories.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.CornerRadiusSmall),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(Dimens.PaddingMedium),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(Dimens.IconSizeMedium))
                        Text(
                            text = "${overBudgetCategories.size} categories are over budget",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CategoryBudgetCard(
    category: BudgetCategory,
    onEdit: () -> Unit
) {
    val statusColor = budgetStatusColor(category.health)
    val categoryColor = parseCategoryColor(category.color)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(Dimens.PaddingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = categoryColor.copy(alpha = 0.16f),
                    contentColor = categoryColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = categoryIcon(category),
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.IconSizeLarge)
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
                        text = if (category.hasLimit) {
                            "${formatCurrency(category.spentAmount)} of ${formatCurrency(category.limitAmount)}"
                        } else {
                            "${formatCurrency(category.spentAmount)} spent, no limit set"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit ${category.name}")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (category.hasLimit) "Remaining" else "Remaining",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (category.hasLimit) formatCurrency(category.remainingAmount) else "Unlimited",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (category.hasLimit) statusColor else MaterialTheme.colorScheme.primary
                    )
                }

                BudgetStatusPill(category = category, color = statusColor)
            }

            if (category.hasLimit) {
                LinearProgressIndicator(
                    progress = { category.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BudgetStatusPill(
    category: BudgetCategory,
    color: Color
) {
    val text = when (category.health) {
        BudgetHealth.Unlimited -> "No limit"
        BudgetHealth.Good -> "On track"
        BudgetHealth.Warning -> "${category.utilizationPercentage.formatPercent()}%"
        BudgetHealth.Over -> "${category.utilizationPercentage.formatPercent()}%"
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
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
        shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.PaddingExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                }
            }
            Text(
                text = "No budgets yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Add a category limit when you need control. Leave it unset when tracking is enough.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onAddCategory,
                shape = RoundedCornerShape(Dimens.CornerRadiusMedium)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(Dimens.IconSizeMedium))
                Spacer(modifier = Modifier.width(Dimens.PaddingSmall))
                Text("Add category")
            }
        }
    }
}

@Composable
private fun budgetStatusColor(health: BudgetHealth): Color {
    return when (health) {
        BudgetHealth.Unlimited -> MaterialTheme.colorScheme.primary
        BudgetHealth.Good -> Color(0xFF16A34A)
        BudgetHealth.Warning -> Color(0xFFEAB308)
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

private fun Float.formatPercent(): String = toDouble().formatPercent()
