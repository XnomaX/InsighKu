package com.example.insightku.ui.components.budgeting

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.data.model.Category
import com.example.insightku.data.model.CategoryType
import com.example.insightku.data.model.Installment
import com.example.insightku.data.model.RecurringBudget
import com.example.insightku.ui.dialogs.AddCategoryDialog
import com.example.insightku.ui.dialogs.CategoryIconResolver
import com.example.insightku.ui.dialogs.EditCategoryDialog
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

    Box(modifier = Modifier.fillMaxSize()) {
        BudgetingScreenContent(
            uiState = uiState,
            onEvent = viewModel::onEvent
        )

        HandleDialogs(uiState = uiState, onEvent = viewModel::onEvent)
    }
}

@Composable
fun BudgetingScreenContent(
    uiState: BudgetingUiState,
    onEvent: (BudgetingEvent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF9FE))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Dimens.ContentBottomPadding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Premium header
            item {
                BudgetGradientHeader(
                    month = currentMonthLabel(),
                    isLoading = uiState.isLoading
                )
            }

            // Error card
            if (uiState.error != null) {
                item {
                    BudgetErrorCard(
                        message = uiState.error,
                        onDismiss = { onEvent(BudgetingEvent.ClearError) },
                        modifier = Modifier.padding(
                            horizontal = Dimens.ScreenHorizontalPadding,
                            vertical = 8.dp
                        )
                    )
                }
            }

            // Overview card
            item {
                BudgetStatusCard(
                    remaining = uiState.remainingBudget,
                    percentage = uiState.budgetUtilizationPercentage,
                    totalBudget = uiState.totalBudget,
                    limitedSpent = uiState.limitedSpent,
                    riskyCount = uiState.budgetCategories.count { it.health == BudgetHealth.Warning },
                    overBudgetCount = uiState.overBudgetCategories.size,
                    modifier = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical = 12.dp
                    )
                )
            }

            // ── EXPENSE BUDGETS SECTION ──────────────────────────────────────
            item {
                BudgetSectionLabel(
                    title = "Expense Budgets",
                    subtitle = categoryListSubtitle(uiState),
                    onAddCategory = { onEvent(BudgetingEvent.ShowAddBudgetDialog(CategoryType.EXPENSE)) },
                    modifier = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical = 8.dp
                    )
                )
            }

            if (!uiState.isLoading && uiState.budgetCategories.isEmpty()) {
                item {
                    EmptyBudgetState(
                        onAddCategory = { onEvent(BudgetingEvent.ShowAddBudgetDialog(CategoryType.EXPENSE)) },
                        modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)
                    )
                }
            } else {
                items(
                    items = uiState.budgetCategories,
                    key = { it.id }
                ) { category ->
                    BudgetCategoryCard(
                        category = category,
                        onEdit = { onEvent(BudgetingEvent.ShowEditBudgetDialog(category)) },
                        onDelete = { onEvent(BudgetingEvent.ShowDeleteConfirmDialog(category)) },
                        modifier = Modifier.padding(
                            horizontal = Dimens.ScreenHorizontalPadding,
                            vertical = 6.dp
                        )
                    )
                }
            }

            // Insights
            if (uiState.budgetCategories.isNotEmpty()) {
                item {
                    BudgetInsightsSection(
                        categories = uiState.budgetCategories,
                        modifier = Modifier.padding(
                            horizontal = Dimens.ScreenHorizontalPadding,
                            vertical = 12.dp
                        )
                    )
                }
            }

            // ── INCOME SOURCES SECTION ───────────────────────────────────────
            item {
                IncomeSectionLabel(
                    title = "Income Sources",
                    subtitle = if (uiState.incomeCategories.isEmpty()) "No income sources yet"
                               else "${uiState.incomeCategories.size} sources tracked",
                    onAddCategory = { onEvent(BudgetingEvent.ShowAddBudgetDialog(CategoryType.INCOME)) },
                    modifier = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical = 8.dp
                    )
                )
            }

            if (uiState.incomeCategories.isEmpty()) {
                item {
                    EmptyIncomeState(
                        onAddCategory = { onEvent(BudgetingEvent.ShowAddBudgetDialog(CategoryType.INCOME)) },
                        modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)
                    )
                }
            } else {
                items(
                    items = uiState.incomeCategories,
                    key = { "income-${it.id}" }
                ) { category ->
                    IncomeCategoryCard(
                        category = category,
                        onEdit = { onEvent(BudgetingEvent.ShowEditBudgetDialog(category)) },
                        onDelete = { onEvent(BudgetingEvent.ShowDeleteConfirmDialog(category)) },
                        modifier = Modifier.padding(
                            horizontal = Dimens.ScreenHorizontalPadding,
                            vertical = 6.dp
                        )
                    )
                }
            }

            // Recurring Payments section
            item {
                RecurringSection(
                    recurringBudgets = uiState.recurringBudgets,
                    onAdd    = { onEvent(BudgetingEvent.ShowAddRecurringDialog) },
                    onEdit   = { onEvent(BudgetingEvent.ShowEditRecurringDialog(it)) },
                    onDelete = { onEvent(BudgetingEvent.DeleteRecurringBudget(it)) },
                    modifier = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical = 4.dp
                    )
                )
            }

            // Installments section
            item {
                InstallmentsSection(
                    installments = uiState.installments,
                    onAdd    = { onEvent(BudgetingEvent.ShowAddInstallmentDialog) },
                    onEdit   = { onEvent(BudgetingEvent.ShowEditInstallmentDialog(it)) },
                    onDelete = { onEvent(BudgetingEvent.DeleteInstallment(it.id)) },
                    modifier = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical = 4.dp
                    )
                )
            }
        }

        if (uiState.isLoading && uiState.budgetCategories.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF7C4DFF)
            )
        }
    }
}

@Composable
private fun HandleDialogs(uiState: BudgetingUiState, onEvent: (BudgetingEvent) -> Unit) {
    when (val dialogState = uiState.dialogState) {
        is DialogState.AddBudget -> {
            AddCategoryDialog(
                isOpen          = true,
                onDismiss       = { onEvent(BudgetingEvent.HideAddBudgetDialog) },
                onCategoryAdded = { category -> onEvent(BudgetingEvent.AddCategory(category)) },
                initialType     = dialogState.categoryType
            )
        }

        is DialogState.EditBudget -> {
            EditCategoryDialog(
                isOpen = true,
                onDismiss = { onEvent(BudgetingEvent.HideEditBudgetDialog) },
                category = Category(
                    id               = dialogState.category.id,
                    name             = dialogState.category.name,
                    budgetLimit      = dialogState.category.budgetedAmount,
                    color            = dialogState.category.color,
                    icon             = dialogState.category.icon,
                    recurringPeriod  = dialogState.category.recurringPeriod,
                    categoryType     = dialogState.category.categoryType.name,
                    isSystemCategory = dialogState.category.isSystemCategory
                ),
                onCategoryEdited  = { onEvent(BudgetingEvent.UpdateCategory(it)) },
                onCategoryDeleted = { onEvent(BudgetingEvent.ShowDeleteConfirmDialog(dialogState.category)) }
            )
        }

        is DialogState.DeleteConfirm -> {
            DeleteCategoryDialog(
                categoryName = dialogState.category.name,
                onDismiss    = { onEvent(BudgetingEvent.HideDeleteConfirmDialog) },
                onConfirm    = {
                    onEvent(BudgetingEvent.ConfirmDeleteCategory(
                        categoryId   = dialogState.category.id,
                        categoryName = dialogState.category.name
                    ))
                }
            )
        }

        is DialogState.ManageRecurring -> {
            // Legacy — redirect to new add dialog
            AddRecurringPaymentDialog(
                isOpen    = true,
                onDismiss = { onEvent(BudgetingEvent.HideRecurringDialog) },
                onSave    = { onEvent(BudgetingEvent.AddRecurringBudget(it)) }
            )
        }

        is DialogState.AddRecurringPayment -> {
            AddRecurringPaymentDialog(
                isOpen    = true,
                onDismiss = { onEvent(BudgetingEvent.HideRecurringDialog) },
                onSave    = { onEvent(BudgetingEvent.AddRecurringBudget(it)) }
            )
        }

        is DialogState.EditRecurringPayment -> {
            AddRecurringPaymentDialog(
                isOpen    = true,
                onDismiss = { onEvent(BudgetingEvent.HideRecurringDialog) },
                onSave    = { onEvent(BudgetingEvent.UpdateRecurringBudget(it)) },
                editing   = dialogState.budget
            )
        }

        is DialogState.AddInstallment -> {
            AddInstallmentDialog(
                isOpen    = true,
                onDismiss = { onEvent(BudgetingEvent.HideInstallmentDialog) },
                onSave    = { onEvent(BudgetingEvent.AddInstallment(it)) }
            )
        }

        is DialogState.EditInstallment -> {
            AddInstallmentDialog(
                isOpen    = true,
                onDismiss = { onEvent(BudgetingEvent.HideInstallmentDialog) },
                onSave    = { onEvent(BudgetingEvent.UpdateInstallment(it)) },
                editing   = dialogState.installment
            )
        }

        is DialogState.None -> Unit
    }
}

@Composable
private fun DeleteCategoryDialog(
    categoryName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = true, onBack = onDismiss)

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(150))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)) + fadeIn()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    shadowElevation = 24.dp,
                    border = BorderStroke(1.dp, Color(0xFFECE7F6))
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF5F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE57373),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Delete Category?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A2E)
                            )
                            Text(
                                text = "\"$categoryName\" will be removed. Its transactions will be unlinked.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF9E9E9E),
                                textAlign = TextAlign.Center
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clickable(onClick = onDismiss),
                                shape = RoundedCornerShape(50.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFECE7F6))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Cancel",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF6B6B8A)
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clickable(onClick = onConfirm),
                                shape = RoundedCornerShape(50.dp),
                                color = Color(0xFFE57373)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Delete",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetGradientHeader(
    month: String,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAF9FE))
            .statusBarsPadding()
            .padding(horizontal = Dimens.ScreenHorizontalPadding)
            .padding(top = 28.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "Stay mindful with your spending",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.5.sp,
            color = Color(0xFFB39DDB)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Budgeting",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A2E)
            )
            if (isLoading) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF7C4DFF).copy(alpha = 0.10f),
                    contentColor = Color(0xFF7C4DFF)
                ) {
                    Text(
                        text = "Syncing",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Text(
            text = month,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9E9E9E)
        )
    }
}

@Composable
private fun BudgetStatusCard(
    remaining: Double,
    percentage: Double,
    totalBudget: Double,
    limitedSpent: Double,
    riskyCount: Int,
    overBudgetCount: Int,
    modifier: Modifier = Modifier
) {
    val status = monthlyStatus(remaining, percentage, totalBudget)
    val statusColor by animateColorAsState(
        targetValue = budgetStatusColor(status.health),
        animationSpec = tween(300),
        label = "statusColor"
    )

    var progressAnimated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { progressAnimated = true }
    val animatedProgress by animateFloatAsState(
        targetValue = if (progressAnimated) (percentage / 100.0).coerceIn(0.0, 1.0).toFloat() else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
        label = "statusProgress"
    )
    val progressColor by animateColorAsState(
        targetValue = budgetProgressColor(percentage),
        animationSpec = tween(300),
        label = "progressColor"
    )

    val motivationalText = when {
        totalBudget <= 0.0 -> "Set limits to start tracking your budget"
        remaining < 0.0 -> "You've exceeded your budget this month"
        percentage >= 70.0 -> "Watch your spending — you're getting close"
        else -> "You're staying on track this month"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFECE7F6))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top row: totals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OverviewStatItem(
                    label = "Total Budget",
                    value = if (totalBudget > 0) formatCurrency(totalBudget) else "—",
                    valueColor = Color(0xFF1A1A2E)
                )
                OverviewStatItem(
                    label = "Spent",
                    value = formatCurrency(limitedSpent),
                    valueColor = Color(0xFF1A1A2E),
                    align = Alignment.CenterHorizontally
                )
                OverviewStatItem(
                    label = "Remaining",
                    value = if (totalBudget > 0) formatCurrency(remaining) else "—",
                    valueColor = statusColor,
                    align = Alignment.End
                )
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50.dp)),
                    color = progressColor,
                    trackColor = Color(0xFFECE7F6)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = motivationalText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${percentage.formatPercent()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            RiskSignalRow(
                riskyCount = riskyCount,
                overBudgetCount = overBudgetCount
            )
        }
    }
}

@Composable
private fun OverviewStatItem(
    label: String,
    value: String,
    valueColor: Color,
    align: Alignment.Horizontal = Alignment.Start
) {
    Column(horizontalAlignment = align) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF9E9E9E),
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RiskSignalRow(
    riskyCount: Int,
    overBudgetCount: Int
) {
    val color by animateColorAsState(
        targetValue = when {
            overBudgetCount > 0 -> Color(0xFFE57373)
            riskyCount > 0 -> Color(0xFFFFB74D)
            else -> Color(0xFF81C784)
        },
        animationSpec = tween(300),
        label = "riskColor"
    )
    val text = when {
        overBudgetCount > 0 -> "$overBudgetCount over budget"
        riskyCount > 0 -> "$riskyCount need attention"
        else -> "All limited categories are safe"
    }
    val icon = when {
        overBudgetCount > 0 -> Icons.Default.Warning
        riskyCount > 0 -> Icons.Default.Warning
        else -> Icons.Default.CheckCircle
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.10f),
        contentColor = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
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
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF5F5),
            contentColor = Color(0xFFE57373)
        ),
        border = BorderStroke(1.dp, Color(0xFFE57373).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.PaddingLarge),
            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
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
private fun IncomeSectionLabel(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onAddCategory: (() -> Unit)? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E)
                )
            }
            if (onAddCategory != null) {
                Surface(
                    modifier = Modifier.clickable(onClick = onAddCategory),
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF10B981)
                        )
                        Text(
                            text = "Add Source",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyIncomeState(
    onAddCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFECE7F6))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981).copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color(0xFF10B981).copy(alpha = 0.5f)
                )
            }
            Text(
                text = "No income sources yet",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A2E),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Track where your money comes from.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center
            )
            Surface(
                modifier = Modifier.clickable(onClick = onAddCategory),
                shape = RoundedCornerShape(50.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFF10B981))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF10B981)
                    )
                    Text(
                        "Add Income Source",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomeCategoryCard(
    category: BudgetCategory,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val categoryColor = parseCategoryColor(category.color)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFECE7F6))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(categoryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon(category),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = categoryColor
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (category.spentAmount > 0)
                        "${formatCurrency(category.spentAmount)} this month"
                    else "No income recorded",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E)
                )
            }
            if (category.spentAmount > 0) {
                Text(
                    text = formatCurrency(category.spentAmount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFB39DDB)
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFE57373).copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetSectionLabel(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onAddCategory: (() -> Unit)? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E)
                )
            }
            if (onAddCategory != null) {
                Surface(
                    modifier = Modifier.clickable(onClick = onAddCategory),
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C4DFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add category",
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF7C4DFF)
                        )
                        Text(
                            text = "Add Category",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF7C4DFF)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetCategoryCard(
    category: BudgetCategory,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = budgetStatusColor(category.health),
        animationSpec = tween(300),
        label = "catStatusColor"
    )
    val categoryColor = parseCategoryColor(category.color)

    var progressAnimated by remember { mutableStateOf(false) }
    LaunchedEffect(category.id) { progressAnimated = true }
    val animatedProgress by animateFloatAsState(
        targetValue = if (progressAnimated) category.progressFraction else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
        label = "catProgress"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "cardScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFECE7F6))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(categoryColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon(category),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = categoryColor
                    )
                }

                // Name + spent
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A2E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = budgetSpentText(category),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Remaining
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (category.hasLimit) "Remaining" else "Tracked",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9E9E9E)
                    )
                    Text(
                        text = if (category.hasLimit) formatCurrency(category.remainingAmount) else "No limit",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (category.hasLimit) statusColor else Color(0xFF7C4DFF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Edit icon
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit ${category.name}",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFB39DDB)
                    )
                }

                // Delete icon
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete ${category.name}",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFE57373).copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Progress bar + status pill + recurring badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50.dp)),
                    color = statusColor,
                    trackColor = Color(0xFFECE7F6)
                )
                if (!category.recurringPeriod.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF7C4DFF).copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = category.recurringPeriod,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF7C4DFF)
                        )
                    }
                }
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
        color = color.copy(alpha = 0.12f),
        contentColor = color
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyBudgetState(
    onAddCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFECE7F6))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7C4DFF).copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = Color(0xFF7C4DFF).copy(alpha = 0.5f)
                )
            }
            Text(
                text = "No budget categories yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A2E),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Start with a few monthly limits. Categories without limits still track spending.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center
            )
            Surface(
                modifier = Modifier.clickable(onClick = onAddCategory),
                shape = RoundedCornerShape(50.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFF7C4DFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF7C4DFF)
                    )
                    Text(
                        "Add Category",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF7C4DFF)
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetInsightsSection(
    categories: List<BudgetCategory>,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) return

    val insights = buildInsights(categories)
    if (insights.isEmpty()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Budget Insights",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )
        insights.forEach { insight ->
            InsightCard(
                text = insight.text,
                bgColor = insight.bgColor,
                iconTint = insight.iconTint,
                icon = insight.icon
            )
        }
    }
}

private data class InsightData(
    val text: String,
    val bgColor: Color,
    val iconTint: Color,
    val icon: ImageVector
)

private fun buildInsights(categories: List<BudgetCategory>): List<InsightData> {
    val result = mutableListOf<InsightData>()
    val overBudget = categories.filter { it.health == BudgetHealth.Over }
    val warning = categories.filter { it.health == BudgetHealth.Warning }
    val safe = categories.filter { it.health == BudgetHealth.Good }

    overBudget.firstOrNull()?.let {
        result.add(InsightData(
            text = "${it.name} spending has exceeded the budget",
            bgColor = Color(0xFFFFF5F5),
            iconTint = Color(0xFFE57373),
            icon = Icons.Default.Warning
        ))
    }
    warning.firstOrNull()?.let {
        result.add(InsightData(
            text = "${it.name} budget is nearly reached",
            bgColor = Color(0xFFFFF8F0),
            iconTint = Color(0xFFFFB74D),
            icon = Icons.Default.TrendingUp
        ))
    }
    if (safe.size >= 2) {
        result.add(InsightData(
            text = "${safe.size} categories are well within budget",
            bgColor = Color(0xFFF0FFF4),
            iconTint = Color(0xFF81C784),
            icon = Icons.Default.TrendingDown
        ))
    }
    return result.take(3)
}

@Composable
private fun InsightCard(
    text: String,
    bgColor: Color,
    iconTint: Color,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, iconTint.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = iconTint
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF1A1A2E),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun budgetStatusColor(health: BudgetHealth): Color = when (health) {
    BudgetHealth.Unlimited -> Color(0xFF7C4DFF)
    BudgetHealth.Good -> Color(0xFF81C784)
    BudgetHealth.Warning -> Color(0xFFFFB74D)
    BudgetHealth.Over -> Color(0xFFE57373)
}

@Composable
private fun budgetProgressColor(percentage: Double): Color = when {
    percentage < 70.0 -> Color(0xFF81C784)
    percentage <= 100.0 -> Color(0xFFFFB74D)
    else -> Color(0xFFE57373)
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

private fun categoryIcon(category: BudgetCategory): ImageVector =
    CategoryIconResolver.resolveIcon(category.icon.ifBlank { category.name })

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
