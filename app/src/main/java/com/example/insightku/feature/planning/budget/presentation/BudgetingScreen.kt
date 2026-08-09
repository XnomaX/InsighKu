package com.example.insightku.feature.planning.budget.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.insightku.R
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.CategoryType
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.feature.planning.goal.presentation.GoalsEvent
import com.example.insightku.feature.planning.goal.presentation.GoalsScreen
import com.example.insightku.feature.planning.goal.presentation.GoalsViewModel
import com.example.insightku.feature.planning.presentation.PlanningHeader
import kotlinx.coroutines.launch

// ─── Tab Enum ─────────────────────────────────────────────────────────────────

private enum class PlanningTab {
    BUDGETING,
    GOALS
}

// ─── Main Screen ──────────────────────────────────────────────────────────────

@Composable
fun BudgetingScreen(
    viewModel: BudgetingViewModel = hiltViewModel(),
    onNavigateToGoalDetail: ((String) -> Unit)? = null,
    initialAction: BudgetingAction? = null,
    onActionConsumed: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tabs = PlanningTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val goalsViewModel: GoalsViewModel = hiltViewModel()

    // Handle initial action from Home screen CTAs
    LaunchedEffect(initialAction) {
        when (initialAction) {
            is BudgetingAction.OpenCreateGoal -> {
                pagerState.animateScrollToPage(1)
                goalsViewModel.onEvent(GoalsEvent.ShowAddGoalDialog())
                onActionConsumed()
            }
            is BudgetingAction.NavigateToGoals -> {
                pagerState.animateScrollToPage(1)
                onActionConsumed()
            }
            is BudgetingAction.OpenCreateBudget -> {
                viewModel.onEvent(BudgetingEvent.ShowAddBudgetDialog(initialAction.categoryType))
                onActionConsumed()
            }
            null -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPalette.background)
            .statusBarsPadding()
    ) {
        // Header above tabs
        AnimatedContent(
            targetState = pagerState.currentPage,
            transitionSpec = {
                fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) togetherWith
                        fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
            },
            label = "header_animation"
        ) { pageIndex ->
            when (tabs.getOrNull(pageIndex)) {
                PlanningTab.BUDGETING -> PlanningHeader(
                    title = stringResource(R.string.budgeting_title),
                    subtitle = stringResource(R.string.budgeting_subtitle)
                )
                PlanningTab.GOALS -> PlanningHeader(
                    title = stringResource(R.string.goals_title),
                    subtitle = stringResource(R.string.goals_subtitle),
                    onAddClick = { goalsViewModel.onEvent(GoalsEvent.ShowAddGoalDialog()) }
                )
                null -> Box(modifier = Modifier.fillMaxWidth())
            }
        }

        // Tab Row Navigation
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = AppPalette.background,
            contentColor = LocalAccent.current,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = pagerState.currentPage == index
                Tab(
                    selected = selected,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {                val tabLabel = stringResource(when (tab) {
                    PlanningTab.BUDGETING -> R.string.budgeting_title
                    PlanningTab.GOALS -> R.string.goals_title
                })
                val tabIcon = when (tab) {
                    PlanningTab.BUDGETING -> Icons.Outlined.AccountBalanceWallet
                    PlanningTab.GOALS -> Icons.Outlined.Savings
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                            Icon(
                                imageVector = tabIcon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (selected) LocalAccent.current else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = tabLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) LocalAccent.current else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    selectedContentColor = LocalAccent.current,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Pager content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            when (tabs.getOrNull(pageIndex)) {
                PlanningTab.BUDGETING -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        BudgetingScreenContent(
                            uiState = uiState,
                            onEvent = viewModel::onEvent
                        )
                        HandleDialogs(uiState = uiState, onEvent = viewModel::onEvent)
                    }
                }
                PlanningTab.GOALS -> {
                    GoalsScreen(viewModel = goalsViewModel, onNavigateToGoalDetail = onNavigateToGoalDetail)
                }
                null -> {}
            }
        }
    }
}

// ─── Screen Content ───────────────────────────────────────────────────────────

@Composable
fun BudgetingScreenContent(
    uiState: BudgetingUiState,
    onEvent: (BudgetingEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPalette.background),
        contentPadding = PaddingValues(bottom = Dimens.ContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
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

        // ── EXPENSE BUDGETS SECTION ──────────────────────────────────────
        item {
            BudgetSectionLabel(
                title = stringResource(R.string.budget_expense_budgets),
                subtitle = categoryListSubtitle(uiState),
                onAddCategory = { onEvent(BudgetingEvent.ShowAddBudgetDialog(CategoryType.EXPENSE)) },
                modifier = Modifier.padding(
                    horizontal = Dimens.ScreenHorizontalPadding,
                    vertical = 8.dp
                )
            )
        }

        if (!uiState.isLoading && !uiState.hasExpenseCategories) {
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
        }

        // ── INCOME SOURCES SECTION ───────────────────────────────────────
        item {
            IncomeSectionLabel(
                title = stringResource(R.string.budget_income_sources),
                subtitle = if (uiState.incomeCategories.isEmpty()) stringResource(R.string.no_income_sources)
                else pluralStringResource(
                    R.plurals.income_sources_count,
                    uiState.incomeCategories.size
                ),
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
                onAdd = { onEvent(BudgetingEvent.ShowAddRecurringDialog) },
                onEdit = { onEvent(BudgetingEvent.ShowEditRecurringDialog(it)) },
                onDelete = { onEvent(BudgetingEvent.DeleteRecurringBudget(it)) },
                categories = uiState.rawCategories,
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
                onAdd = { onEvent(BudgetingEvent.ShowAddInstallmentDialog) },
                onEdit = { onEvent(BudgetingEvent.ShowEditInstallmentDialog(it)) },
                onDelete = { onEvent(BudgetingEvent.DeleteInstallment(it.id)) },
                categories = uiState.rawCategories,
                modifier = Modifier.padding(
                    horizontal = Dimens.ScreenHorizontalPadding,
                    vertical = 4.dp
                )
            )
        }

        // Loading indicator
        item {
            if (uiState.isLoading && uiState.budgetCategories.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = LocalAccent.current
                    )
                }
            }
        }
    }
}

// ─── Section Labels ───────────────────────────────────────────────────────────

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
                    modifier = Modifier.clickable { onAddCategory() },
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
                            contentDescription = stringResource(R.string.cd_add_category),
                            modifier = Modifier.size(14.dp),
                            tint = LocalAccent.current
                        )
                        Text(
                            text = stringResource(R.string.add_category),
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
                    modifier = Modifier.clickable { onAddCategory() },
                    shape = RoundedCornerShape(50.dp),
                    color = AppPalette.card,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppPalette.success)
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
                            tint = AppPalette.success
                        )
                        Text(
                            text = stringResource(R.string.add_source),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppPalette.success
                        )
                    }
                }
            }
        }
    }
}

// ─── Dialogs ──────────────────────────────────────────────────────────────────

@Composable
private fun HandleDialogs(uiState: BudgetingUiState, onEvent: (BudgetingEvent) -> Unit) {
    when (val dialogState = uiState.dialogState) {
        is DialogState.AddBudget -> {
            AddCategoryDialog(
                isOpen = true,
                onDismiss = { onEvent(BudgetingEvent.HideAddBudgetDialog) },
                onCategoryAdded = { category -> onEvent(BudgetingEvent.AddCategory(category)) },
                initialType = dialogState.categoryType
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
                    icon = dialogState.category.icon,
                    recurringPeriod = dialogState.category.recurringPeriod,
                    categoryType = dialogState.category.categoryType.name,
                    isSystemCategory = dialogState.category.isSystemCategory
                ),
                onCategoryEdited = { onEvent(BudgetingEvent.UpdateCategory(it)) },
                onCategoryDeleted = { onEvent(BudgetingEvent.ShowDeleteConfirmDialog(dialogState.category)) }
            )
        }

        is DialogState.DeleteConfirm -> {
            DeleteCategoryDialog(
                categoryName = dialogState.category.name,
                onDismiss = { onEvent(BudgetingEvent.HideDeleteConfirmDialog) },
                onConfirm = {
                    onEvent(
                        BudgetingEvent.ConfirmDeleteCategory(
                            categoryId = dialogState.category.id,
                            categoryName = dialogState.category.name
                        )
                    )
                }
            )
        }

        is DialogState.ManageRecurring,
        is DialogState.AddRecurringPayment -> {
            AddRecurringPaymentDialog(
                isOpen = true,
                onDismiss = { onEvent(BudgetingEvent.HideRecurringDialog) },
                onSave = { onEvent(BudgetingEvent.AddRecurringBudget(it)) },
                availableCategories = uiState.allCategoriesForPicker,
                accounts = uiState.accounts
            )
        }

        is DialogState.EditRecurringPayment -> {
            AddRecurringPaymentDialog(
                isOpen = true,
                onDismiss = { onEvent(BudgetingEvent.HideRecurringDialog) },
                onSave = { onEvent(BudgetingEvent.UpdateRecurringBudget(it)) },
                editing = dialogState.budget,
                availableCategories = uiState.allCategoriesForPicker,
                accounts = uiState.accounts
            )
        }

        is DialogState.AddInstallment -> {
            AddInstallmentDialog(
                isOpen = true,
                onDismiss = { onEvent(BudgetingEvent.HideInstallmentDialog) },
                onSave = { onEvent(BudgetingEvent.AddInstallment(it)) },
                availableCategories = uiState.allCategoriesForPicker,
                accounts = uiState.accounts
            )
        }

        is DialogState.EditInstallment -> {
            AddInstallmentDialog(
                isOpen = true,
                onDismiss = { onEvent(BudgetingEvent.HideInstallmentDialog) },
                onSave = { onEvent(BudgetingEvent.UpdateInstallment(it)) },
                editing = dialogState.installment,
                availableCategories = uiState.allCategoriesForPicker,
                accounts = uiState.accounts
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
    com.example.insightku.core.ui.components.dialogs.PremiumDeleteConfirmDialog(
        itemName = categoryName,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        message = stringResource(R.string.budget_category_will_be_removed, categoryName)
    )
}
