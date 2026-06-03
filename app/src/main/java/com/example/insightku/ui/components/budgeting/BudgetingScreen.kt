package com.example.insightku.ui.components.budgeting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.insightku.ui.theme.AppPalette
import com.example.insightku.ui.theme.ExpenseRed
import com.example.insightku.ui.theme.IncomeGreen
import com.example.insightku.ui.theme.IncomeMid
import com.example.insightku.ui.theme.InsightTone
import com.example.insightku.ui.theme.LocalAccent
import com.example.insightku.ui.theme.LocalComfortMode
import com.example.insightku.ui.theme.LocalInsightTone
import com.example.insightku.ui.theme.PurpleTint
import com.example.insightku.ui.theme.PurpleViolet
import com.example.insightku.ui.theme.WarningYellow
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
            .background(AppPalette.background)
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
                BudgetHealthCard(
                    percentage      = uiState.budgetUtilizationPercentage,
                    totalBudget     = uiState.totalBudget,
                    limitedSpent    = uiState.limitedSpent,
                    remaining       = uiState.remainingBudget,
                    riskyCount      = uiState.budgetCategories.count { it.health == BudgetHealth.Warning },
                    overBudgetCount = uiState.overBudgetCategories.size,
                    safeCount       = uiState.budgetCategories.count { it.health == BudgetHealth.Good },
                    modifier        = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = 12.dp
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
                color = LocalAccent.current
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
            AddRecurringPaymentDialog(
                isOpen              = true,
                onDismiss           = { onEvent(BudgetingEvent.HideRecurringDialog) },
                onSave              = { onEvent(BudgetingEvent.AddRecurringBudget(it)) },
                availableCategories = uiState.allCategoriesForPicker
            )
        }

        is DialogState.AddRecurringPayment -> {
            AddRecurringPaymentDialog(
                isOpen              = true,
                onDismiss           = { onEvent(BudgetingEvent.HideRecurringDialog) },
                onSave              = { onEvent(BudgetingEvent.AddRecurringBudget(it)) },
                availableCategories = uiState.allCategoriesForPicker
            )
        }

        is DialogState.EditRecurringPayment -> {
            AddRecurringPaymentDialog(
                isOpen              = true,
                onDismiss           = { onEvent(BudgetingEvent.HideRecurringDialog) },
                onSave              = { onEvent(BudgetingEvent.UpdateRecurringBudget(it)) },
                editing             = dialogState.budget,
                availableCategories = uiState.allCategoriesForPicker
            )
        }

        is DialogState.AddInstallment -> {
            AddInstallmentDialog(
                isOpen              = true,
                onDismiss           = { onEvent(BudgetingEvent.HideInstallmentDialog) },
                onSave              = { onEvent(BudgetingEvent.AddInstallment(it)) },
                availableCategories = uiState.allCategoriesForPicker
            )
        }

        is DialogState.EditInstallment -> {
            AddInstallmentDialog(
                isOpen              = true,
                onDismiss           = { onEvent(BudgetingEvent.HideInstallmentDialog) },
                onSave              = { onEvent(BudgetingEvent.UpdateInstallment(it)) },
                editing             = dialogState.installment,
                availableCategories = uiState.allCategoriesForPicker
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
    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(28.dp),
        containerColor   = AppPalette.card,
        icon             = {
            Box(
                modifier         = Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFFFF5F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(26.dp))
            }
        },
        title = {
            Text(
                text       = "Delete Category?",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = AppPalette.textPrimary
            )
        },
        text = {
            Text(
                text  = "\"$categoryName\" will be removed. Its transactions will be unlinked.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.textMuted
            )
        },
        dismissButton = {
            Surface(
                modifier = Modifier.height(42.dp).clickable(onClick = onDismiss),
                shape    = RoundedCornerShape(50.dp),
                color    = AppPalette.card,
                border   = androidx.compose.foundation.BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Box(modifier = Modifier.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                    Text("Cancel", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color(0xFF6B6B8A))
                }
            }
        },
        confirmButton = {
            Surface(
                modifier = Modifier.height(42.dp).clickable(onClick = onConfirm),
                shape    = RoundedCornerShape(50.dp),
                color    = Color(0xFFE57373)
            ) {
                Box(modifier = Modifier.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                    Text("Delete", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    )
}

@Composable
private fun BudgetGradientHeader(
    month: String,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPalette.background)
            .statusBarsPadding()
            .padding(horizontal = Dimens.ScreenHorizontalPadding)
            .padding(top = 20.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = month,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Budget",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Spending control center",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isLoading) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = PurpleViolet.copy(alpha = 0.10f),
                    contentColor = PurpleViolet
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
    }
}

// ─── Budget Health Card (new hero) ───────────────────────────────────────────

@Composable
fun BudgetHealthCard(
    percentage: Double,
    totalBudget: Double,
    limitedSpent: Double,
    remaining: Double,
    riskyCount: Int,
    overBudgetCount: Int,
    safeCount: Int,
    modifier: Modifier = Modifier
) {
    // Comfort/gentle softens alert intensity (lower saturation) so red reads as "over", not "danger".
    val tone = LocalInsightTone.current
    val soften = LocalComfortMode.current || tone == InsightTone.GENTLE
    val overColor = if (soften) Color(0xFFE57373) else ExpenseRed
    val warnColor = if (soften) Color(0xFFFFD699) else WarningYellow
    val progressColor by animateColorAsState(
        targetValue = when {
            percentage >= 100.0 -> overColor
            percentage >= 70.0  -> warnColor
            else                -> IncomeGreen
        },
        animationSpec = tween(400),
        label = "healthColor"
    )

    var animated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animated = true }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animated) (percentage / 100.0).coerceIn(0.0, 1.0).toFloat() else 0f,
        animationSpec = tween(900, easing = androidx.compose.animation.core.EaseOutCubic),
        label = "circularProgress"
    )

    // Tone-aware, emotionally-safe budget language. Gentle/comfort never shames an overspend;
    // direct stays concise. Reuses the tone/soften signals read above.
    val healthLabel = when {
        totalBudget <= 0.0  -> "No limits set"
        percentage >= 100.0 -> if (soften) "A little past plan" else "Over budget"
        percentage >= 70.0  -> if (soften) "Getting close" else "Watch spending"
        else                -> if (tone == InsightTone.DIRECT) "Within budget" else "On track"
    }
    val healthSubtitle = when {
        totalBudget <= 0.0  -> "Set category limits to start tracking"
        remaining < 0.0 -> when {
            soften          -> "You've gone a little past your plan — that's okay, here's where things stand."
            tone == InsightTone.DIRECT -> "Over your monthly budget."
            else            -> "You've passed your monthly budget."
        }
        percentage >= 70.0 -> when {
            soften          -> "You're getting close to your plan — no rush, just a heads-up."
            tone == InsightTone.DIRECT -> "Near your monthly limit."
            else            -> "You're approaching your monthly limit."
        }
        else -> when {
            tone == InsightTone.DIRECT -> "Spending within budget."
            else            -> "You're spending within healthy limits."
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.07f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top row: circular progress + stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular progress
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(88.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(88.dp),
                        color = progressColor,
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${percentage.toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = progressColor
                        )
                        Text(
                            text = "used",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Right side stats
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = healthLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = progressColor
                        )
                        Text(
                            text = healthSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BudgetMiniStat(
                            label = "Spent",
                            value = formatCurrencyPlain(limitedSpent),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        BudgetMiniStat(
                            label = "Left",
                            value = if (totalBudget > 0) formatCurrencyPlain(remaining.coerceAtLeast(0.0)) else "—",
                            color = if (remaining < 0) ExpenseRed else IncomeGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.07f))
            )

            // Category health chips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BudgetInsightChip(
                    label = "$safeCount safe",
                    icon = Icons.Outlined.Shield,
                    color = IncomeGreen,
                    modifier = Modifier.weight(1f)
                )
                BudgetInsightChip(
                    label = "$riskyCount warning",
                    icon = Icons.Outlined.Warning,
                    color = WarningYellow,
                    modifier = Modifier.weight(1f)
                )
                BudgetInsightChip(
                    label = "$overBudgetCount over",
                    icon = Icons.Outlined.PieChart,
                    color = ExpenseRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun BudgetInsightChip(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BudgetMiniStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
                    color = AppPalette.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }
            if (onAddCategory != null) {
                Surface(
                    modifier = Modifier.clickable(onClick = onAddCategory),
                    shape = RoundedCornerShape(50.dp),
                    color = AppPalette.card,
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
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
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
                color = AppPalette.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Track where your money comes from.",
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted,
                textAlign = TextAlign.Center
            )
            Surface(
                modifier = Modifier.clickable(onClick = onAddCategory),
                shape = RoundedCornerShape(50.dp),
                color = AppPalette.card,
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
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
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
                    color = AppPalette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (category.spentAmount > 0)
                        "${formatCurrency(category.spentAmount)} this month"
                    else "No income recorded",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
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
                    color = AppPalette.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }
            if (onAddCategory != null) {
                Surface(
                    modifier = Modifier.clickable(onClick = onAddCategory),
                    shape = RoundedCornerShape(50.dp),
                    color = AppPalette.card,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LocalAccent.current)
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
                            tint = LocalAccent.current
                        )
                        Text(
                            text = "Add Category",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = LocalAccent.current
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
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
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
                        color = AppPalette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = budgetSpentText(category),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Remaining
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (category.hasLimit) "Remaining" else "Tracked",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppPalette.textMuted
                    )
                    Text(
                        text = if (category.hasLimit) formatCurrency(category.remainingAmount) else "No limit",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (category.hasLimit) statusColor else LocalAccent.current,
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
                    trackColor = AppPalette.cardBorder
                )
                if (!category.recurringPeriod.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = LocalAccent.current.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = category.recurringPeriod,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = LocalAccent.current
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
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
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
                    .background(LocalAccent.current.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = LocalAccent.current.copy(alpha = 0.5f)
                )
            }
            Text(
                text = "No budget categories yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppPalette.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Start with a few monthly limits. Categories without limits still track spending.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.textMuted,
                textAlign = TextAlign.Center
            )
            Surface(
                modifier = Modifier.clickable(onClick = onAddCategory),
                shape = RoundedCornerShape(50.dp),
                color = AppPalette.card,
                border = BorderStroke(1.dp, LocalAccent.current)
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
                        tint = LocalAccent.current
                    )
                    Text(
                        "Add Category",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = LocalAccent.current
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
            color = AppPalette.textPrimary
        )
        insights.forEach { insight ->
            InsightCard(
                text = insight.text,
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
    iconTint: Color,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppPalette.card,
        border = BorderStroke(1.dp, iconTint.copy(alpha = 0.30f))
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
                color = AppPalette.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun budgetStatusColor(health: BudgetHealth): Color = when (health) {
    BudgetHealth.Unlimited -> LocalAccent.current
    BudgetHealth.Good -> Color(0xFF81C784)
    BudgetHealth.Warning -> Color(0xFFFFB74D)
    BudgetHealth.Over -> Color(0xFFE57373)
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
