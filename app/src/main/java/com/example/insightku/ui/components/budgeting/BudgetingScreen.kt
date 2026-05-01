package com.example.insightku.ui.components.budgeting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.R
import com.example.insightku.data.model.Category
import com.example.insightku.ui.dialogs.AddCategoryDialog
import com.example.insightku.ui.dialogs.EditCategoryDialog
import com.example.insightku.ui.dialogs.RecurringBudgetsDialog
import com.example.insightku.ui.theme.Dimens
import com.example.insightku.ui.theme.LocalResponsiveDimens
import com.example.insightku.viewmodel.BudgetingViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs

// --- Main Composable ---

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
    val dimens = LocalResponsiveDimens.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Dimens.PaddingExtraLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
        ) {
            item {
                val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
                Header(month = currentMonth)
            }

            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = dimens.screenHorizontalPadding)
                        .offset(y = (-60).dp),
                    verticalArrangement = Arrangement.spacedBy(dimens.itemSpacing)
                ) {
                    BudgetSummaryCard(
                        totalBudget = uiState.totalBudget,
                        totalSpent = uiState.totalSpent,
                        remaining = uiState.remainingBudget,
                        percentage = uiState.budgetUtilizationPercentage.toFloat(),
                        overBudgetCategories = uiState.overBudgetCategories
                    )

                    CategoryBudgetsCard(
                        categories = uiState.budgetCategories,
                        onAddCategory = { onEvent(BudgetingEvent.ShowAddBudgetDialog) },
                        onEditCategory = { onEvent(BudgetingEvent.ShowEditBudgetDialog(it)) }
                    )

                    ScheduledPaymentsCard(
                        payments = emptyList(),
                        onManageRecurring = { onEvent(BudgetingEvent.ShowRecurringBudgetsDialog) }
                    )
                }
            }
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
            val categoryToEdit = Category(
                id = dialogState.category.id,
                name = dialogState.category.name,
                budgetLimit = dialogState.category.budgetedAmount,
                color = dialogState.category.color,
                icon = dialogState.category.icon
            )
            EditCategoryDialog(
                isOpen = true,
                onDismiss = { onEvent(BudgetingEvent.HideEditBudgetDialog) },
                category = categoryToEdit,
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
        DialogState.None -> {}
    }
}

// --- UI Components ---

@Composable
private fun Header(month: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.GpsFixed,
                contentDescription = stringResource(R.string.budget_overview),
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
            Text(
                text = stringResource(R.string.budget_overview),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
            Text(
                text = stringResource(R.string.budget_overview_subtitle, month),
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun BudgetSummaryCard(
    totalBudget: Double,
    totalSpent: Double,
    remaining: Double,
    percentage: Float,
    overBudgetCategories: List<BudgetCategory>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(Dimens.PaddingLarge)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.PaddingLarge),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SummaryItem(stringResource(R.string.total_budget), totalBudget, color = MaterialTheme.colorScheme.primary)
                SummaryItem(stringResource(R.string.spent), totalSpent, color = MaterialTheme.colorScheme.onSurface)
                SummaryItem(stringResource(R.string.remaining), remaining, color = if (remaining >= 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.error)
            }

            Column(modifier = Modifier.padding(bottom = Dimens.PaddingLarge)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = stringResource(R.string.overall_progress), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${"%.1f".format(percentage)}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
                LinearProgressIndicator(
                    progress = { percentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (percentage > 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            if (overBudgetCategories.isNotEmpty()) {
                BudgetAlert(overBudgetCategories)
            }
        }
    }
}

@Composable
private fun SummaryItem(title: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
        Text(
            text = formatCurrencyAbbreviated(amount),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun BudgetAlert(overBudgetCategories: List<BudgetCategory>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CornerRadiusSmall),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(Dimens.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = stringResource(R.string.budget_alert),
                modifier = Modifier.size(Dimens.IconSizeLarge),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(Dimens.PaddingMedium))
            Column {
                Text(text = stringResource(R.string.budget_alert), fontWeight = FontWeight.Bold)
                val categoryText = if (overBudgetCategories.size == 1) "category has" else "categories have"
                Text(
                    text = stringResource(R.string.budget_alert_message, overBudgetCategories.size, categoryText),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun CategoryBudgetsCard(
    categories: List<BudgetCategory>,
    onAddCategory: () -> Unit,
    onEditCategory: (BudgetCategory) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(Dimens.PaddingLarge)) {
            Text(
                text = stringResource(R.string.category_budgets),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)) {
                categories.forEach { category ->
                    CategoryCard(category = category, onEdit = { onEditCategory(category) })
                }
            }
            Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
            DashedButton(text = stringResource(R.string.add_new_category), onClick = onAddCategory)
        }
    }
}

@Composable
private fun CategoryCard(category: BudgetCategory, onEdit: (BudgetCategory) -> Unit) {
    // ... Implementation remains the same, but ensure all text is from resources
}

@Composable
fun ScheduledPaymentsCard(
    payments: List<ScheduledPayment>,
    onManageRecurring: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(Dimens.PaddingLarge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = stringResource(R.string.scheduled_payments), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.scheduled_payments_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onManageRecurring) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.manage), modifier = Modifier.size(Dimens.IconSizeMedium))
                    Spacer(modifier = Modifier.width(Dimens.PaddingSmall))
                    Text(stringResource(R.string.manage))
                }
            }
            Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
            payments.forEach { payment ->
                PaymentItem(payment = payment)
                Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
            }
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
            DashedButton(text = stringResource(R.string.add_recurring_payment), onClick = onManageRecurring)
        }
    }
}

@Composable
private fun PaymentItem(payment: ScheduledPayment) {
    // ... Implementation remains the same
}

@Composable
private fun DashedButton(text: String, onClick: () -> Unit) {
    // ... Implementation remains the same
}

// --- Data Models and Utils for Preview ---

data class ScheduledPayment(
    val name: String,
    val amount: Double,
    val date: String,
    val icon: ImageVector,
    val color: Color
)

fun formatCurrency(amount: Double, fractionDigits: Int = 2): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    format.maximumFractionDigits = fractionDigits
    return format.format(amount)
}

fun formatCurrencyAbbreviated(amount: Double): String {
    val absAmount = abs(amount)
    val sign = if (amount < 0) "-" else ""

    return when {
        absAmount >= 1_000_000 -> {
            val value = "%.1f".format(absAmount / 1_000_000).removeSuffix(".0")
            "${sign}Rp${value}jt"
        }
        absAmount >= 1_000 -> {
            val value = "%.0f".format(absAmount / 1_000)
            "${sign}Rp${value}rb"
        }
        else -> formatCurrency(amount, 0)
    }
}
