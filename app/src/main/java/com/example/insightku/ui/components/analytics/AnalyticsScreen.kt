package com.example.insightku.ui.components.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.ui.components.analytics.model.AnalyticsUtils
import com.example.insightku.ui.components.analytics.model.CategoryData
import com.example.insightku.ui.components.analytics.model.TimePeriod
import com.example.insightku.ui.theme.Dimens
import com.example.insightku.ui.theme.LocalResponsiveDimens
import com.example.insightku.viewmodel.AnalyticsViewModel

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onEvent(AnalyticsEvent.LoadAnalytics)
    }

    AnalyticsScreenContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
fun AnalyticsScreenContent(
    uiState: AnalyticsUiState,
    onEvent: (AnalyticsEvent) -> Unit
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
            contentPadding = PaddingValues(bottom = Dimens.PaddingLarge)
        ) {
            item {
                HeaderSection(uiState.selectedMonth)
            }

            item {
                Column(
                    modifier = Modifier
                        .offset(y = Dimens.HeaderVerticalOffset)
                        .padding(horizontal = dimens.screenHorizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(dimens.itemSpacing)
                ) {
                    MonthSelector(
                        selectedMonth = AnalyticsUtils.getMonthDisplayName(uiState.selectedMonth),
                        onPreviousMonth = { onEvent(AnalyticsEvent.PreviousMonth) },
                        onNextMonth = { onEvent(AnalyticsEvent.NextMonth) }
                    )

                    StatisticsSection(
                        totalIncome = uiState.totalIncome,
                        totalExpenses = uiState.totalExpenses,
                        savings = uiState.savings
                    )

                    SavingsRateSection(
                        savingsRate = uiState.savingsRate,
                        selectedMonth = AnalyticsUtils.getMonthDisplayName(uiState.selectedMonth)
                    )

                    BudgetPerformanceCard(
                        budgetPeriod = uiState.budgetTimePeriod,
                        onPeriodChange = { period -> onEvent(AnalyticsEvent.ChangeBudgetPeriod(period)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    IncomeExpensesChart(
                        timePeriod = uiState.chartTimePeriod,
                        onPeriodChange = { period -> onEvent(AnalyticsEvent.ChangeTimePeriod(period)) }
                    )

                    uiState.currentMonthData?.let {
                        DonutChartsSection(
                            monthlyData = it,
                            selectedExpenseCategory = uiState.selectedExpenseCategory,
                            selectedIncomeCategory = uiState.selectedIncomeCategory,
                            onEvent = onEvent,
                            selectedMonth = AnalyticsUtils.getMonthDisplayName(uiState.selectedMonth)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(selectedMonth: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.HeaderHeight)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.PaddingExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Financial Analytics", // R.string.financial_analytics
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
            Text(
                text = "Insights for ${AnalyticsUtils.getMonthDisplayName(selectedMonth)}", // R.string.insights_for_month
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun MonthSelector(
    selectedMonth: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationMedium),
        shape = RoundedCornerShape(Dimens.CornerRadiusSmall)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.MonthSelectorPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier
                    .size(Dimens.MonthSelectorButtonSize)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous month", // R.string.previous_month
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimens.IconSizeMedium)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimens.IconSizeSmall)
                )
                Text(
                    text = selectedMonth,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier
                    .size(Dimens.MonthSelectorButtonSize)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next month", // R.string.next_month
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimens.IconSizeMedium)
                )
            }
        }
    }
}

@Composable
fun StatisticsSection(
    totalIncome: Double,
    totalExpenses: Double,
    savings: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Income", // R.string.income
            amount = totalIncome,
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            color = MaterialTheme.colorScheme.secondary, // Custom color
            backgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Expenses", // R.string.expenses
            amount = totalExpenses,
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            color = MaterialTheme.colorScheme.error, // Custom color
            backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Savings", // R.string.savings
            amount = savings,
            icon = Icons.Default.Savings,
            color = if (savings >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            backgroundColor = (if (savings >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error).copy(alpha = 0.1f)
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    icon: ImageVector,
    color: Color,
    backgroundColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(Dimens.CornerRadiusMedium)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Dimens.StatCardPaddingHorizontal, vertical = Dimens.StatCardPaddingVertical)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.StatCardIconBoxSize)
                    .background(backgroundColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(Dimens.StatCardIconSize)
                )
            }
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
            Text(
                text = AnalyticsUtils.formatCurrencyShort(amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SavingsRateSection(
    savingsRate: Double,
    selectedMonth: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationSmall),
        shape = RoundedCornerShape(Dimens.CornerRadiusMedium)
    ) {
        Column(
            modifier = Modifier.padding(Dimens.PaddingLarge)
        ) {
            Text(
                text = "Savings Rate", // R.string.savings_rate
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
            Text(
                text = "Monthly savings performance for $selectedMonth", // R.string.savings_performance_for_month
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Target: 20%", // R.string.savings_target
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${"%.1f".format(savingsRate)}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))

            LinearProgressIndicator(
                progress = { (savingsRate / 100.0).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.LinearProgressHeight),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )

            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))

            val (text, color) = when {
                savingsRate >= 20 -> "🎉 Congratulations! Target achieved" to MaterialTheme.colorScheme.secondary // R.string.savings_target_achieved
                savingsRate >= 0 -> "💪 Keep saving to reach your target" to MaterialTheme.colorScheme.onSurface // R.string.savings_keep_going
                else -> "⚠️ Expenses exceeded income this month" to MaterialTheme.colorScheme.error // R.string.savings_negative
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

@Composable
private fun DonutChartsSection(
    monthlyData: com.example.insightku.ui.components.analytics.model.MonthlyData,
    selectedExpenseCategory: String?,
    selectedIncomeCategory: String?,
    onEvent: (AnalyticsEvent) -> Unit,
    selectedMonth: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)) {
        InteractiveDonutChart(
            title = "Expense Categories", // R.string.expense_categories
            titleIcon = "💸",
            subtitle = "Monthly expenses by category", // R.string.expense_categories_subtitle
            categories = monthlyData.expenseCategories,
            selectedCategory = selectedExpenseCategory,
            onCategoryClick = { category -> onEvent(AnalyticsEvent.SelectExpenseCategory(category)) },
            centerColor = MaterialTheme.colorScheme.error,
            monthName = selectedMonth
        )

        if (monthlyData.incomeCategories.isNotEmpty()) {
            InteractiveDonutChart(
                title = "Income Sources", // R.string.income_sources
                titleIcon = "💰",
                subtitle = "Monthly income breakdown", // R.string.income_sources_subtitle
                categories = monthlyData.incomeCategories,
                selectedCategory = selectedIncomeCategory,
                onCategoryClick = { category -> onEvent(AnalyticsEvent.SelectIncomeCategory(category)) },
                centerColor = MaterialTheme.colorScheme.secondary,
                monthName = selectedMonth
            )
        }
    }
}