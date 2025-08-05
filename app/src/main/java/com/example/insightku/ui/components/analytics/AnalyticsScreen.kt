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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.viewmodel.AnalyticsViewModel
import com.example.insightku.ui.components.analytics.model.*
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.abs

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onEvent(AnalyticsEvent.LoadAnalytics)
    }

    // Main container dengan background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF5A2A82),
                                Color(0xFF7C3AED)
                            )
                        )
                    )
            ) {
                // Text di tengah dengan styling yang lebih elegant
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Financial Analytics",
                        style = MaterialTheme.typography.headlineMedium, // Font yang lebih bagus dan minimalis
                        fontWeight = FontWeight.Bold, // Tidak terlalu bold
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Insights for ${AnalyticsUtils.getMonthDisplayName(uiState.selectedMonth)}",
                        style = MaterialTheme.typography.bodyLarge, // Ukuran yang tepat
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 0.25.sp
                    )
                }
            }

            // Content area dengan negative margin untuk month selector di tengah
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-20).dp) // Dikecilkan untuk proporsi yang lebih baik
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Month Selector Card - di posisi tengah antara ungu dan putih, ukuran kecil
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    shape = RoundedCornerShape(8.dp) // Dikecilkan
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp), // Dikecilkan
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous month button
                        IconButton(
                            onClick = { viewModel.onEvent(AnalyticsEvent.PreviousMonth) },
                            modifier = Modifier
                                .size(28.dp) // Dikecilkan
                                .background(
                                    Color(0xFF5A2A82).copy(alpha = 0.1f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "Previous month",
                                tint = Color(0xFF5A2A82),
                                modifier = Modifier.size(18.dp) // Diperbesar untuk arrow
                            )
                        }

                        // Center content dengan Calendar icon dan month name
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFF5A2A82),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = AnalyticsUtils.getMonthDisplayName(uiState.selectedMonth),
                                style = MaterialTheme.typography.bodyMedium, // Font minimalis dan kecil
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5A2A82),
                                letterSpacing = 0.3.sp
                            )
                        }

                        // Next month button
                        IconButton(
                            onClick = { viewModel.onEvent(AnalyticsEvent.NextMonth) },
                            modifier = Modifier
                                .size(28.dp) // Dikecilkan
                                .background(
                                    Color(0xFF5A2A82).copy(alpha = 0.1f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Next month",
                                tint = Color(0xFF5A2A82),
                                modifier = Modifier.size(18.dp) // Diperbesar untuk arrow
                            )
                        }
                    }
                }

                // Statistics Cards - dengan font yang lebih kecil
                StatisticsCardsSection(
                    totalIncome = uiState.totalIncome,
                    totalExpenses = uiState.totalExpenses,
                    savings = uiState.savings
                )

                // Savings Rate Card
                SavingsRateSection(
                    savingsRate = uiState.savingsRate,
                    selectedMonth = uiState.selectedMonth
                )

                // Budget Performance Card - gunakan style persis seperti di BudgetPerformanceCard
                BudgetPerformanceCard(
                    budgetPeriod = uiState.budgetPeriod,
                    onPeriodChange = { period ->
                        viewModel.onEvent(AnalyticsEvent.ChangeBudgetPeriod(period))
                    },
                    modifier = Modifier.fillMaxWidth()
                    // No extra padding here, let the card handle its own padding for consistency
                )

                // Income vs Expenses Chart
                IncomeExpensesChart(
                    timePeriod = uiState.timePeriod,
                    onPeriodChange = { period ->
                        viewModel.onEvent(AnalyticsEvent.ChangeTimePeriod(period))
                    }
                )

                // Interactive Donut Charts
                val currentData = uiState.currentMonthData
                if (currentData != null) {
                    InteractiveDonutChart(
                        title = "Expense Categories",
                        titleIcon = "💸",
                        subtitle = "Monthly expenses by category",
                        categories = currentData.expenseCategories,
                        selectedCategory = uiState.selectedExpenseCategory,
                        onCategoryClick = { category ->
                            viewModel.onEvent(AnalyticsEvent.SelectExpenseCategory(category))
                        },
                        centerColor = Color(0xFFEF4444),
                        monthName = AnalyticsUtils.getMonthDisplayName(uiState.selectedMonth)
                    )

                    if (currentData.incomeCategories.isNotEmpty()) {
                        InteractiveDonutChart(
                            title = "Income Sources",
                            titleIcon = "💰",
                            subtitle = "Monthly income breakdown",
                            categories = currentData.incomeCategories,
                            selectedCategory = uiState.selectedIncomeCategory,
                            onCategoryClick = { category ->
                                viewModel.onEvent(AnalyticsEvent.SelectIncomeCategory(category))
                            },
                            centerColor = Color(0xFF10B981),
                            monthName = AnalyticsUtils.getMonthDisplayName(uiState.selectedMonth)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsCardsSection(
    totalIncome: Double,
    totalExpenses: Double,
    savings: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Income",
            amount = totalIncome,
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            color = Color(0xFF10B981),
            backgroundColor = Color(0xFF10B981).copy(alpha = 0.1f)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Expenses",
            amount = totalExpenses,
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            color = Color(0xFFEF4444),
            backgroundColor = Color(0xFFEF4444).copy(alpha = 0.1f)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Savings",
            amount = abs(savings),
            icon = Icons.Default.Savings,
            color = if (savings >= 0) Color(0xFF5A2A82) else Color(0xFFEF4444),
            backgroundColor = if (savings >= 0) Color(0xFF5A2A82).copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f)
        )
    }
}

@Composable
private fun SavingsRateSection(
    savingsRate: Double,
    selectedMonth: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Savings Rate",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Monthly savings performance for ${AnalyticsUtils.getMonthDisplayName(selectedMonth)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Target: 20%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${"%.1f".format(abs(savingsRate))}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5A2A82)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
            progress = { (abs(savingsRate) / 100.0).coerceIn(0.0, 1.0).toFloat() },
            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
            color = Color(0xFF5A2A82),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    savingsRate >= 20 -> "🎉 Congratulations! Target achieved"
                    savingsRate >= 0 -> "💪 Keep saving to reach your target"
                    else -> "⚠️ Expenses exceeded income this month"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    savingsRate >= 20 -> Color(0xFF10B981)
                    savingsRate >= 0 -> MaterialTheme.colorScheme.onSurface
                    else -> Color(0xFFEF4444)
                }
            )
        }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp) // Diperbesar kembali
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 16.dp) // Tambah padding horizontal
                .fillMaxWidth(), // Pastikan full width
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp) // Diperbesar icon box
                    .background(backgroundColor, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp) // Diperbesar icon
                )
            }
            Spacer(modifier = Modifier.height(12.dp)) // Diperbesar spacing
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall, // Diperbesar font
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = AnalyticsUtils.formatCurrencyShort(amount),
                style = MaterialTheme.typography.titleSmall, // Diperbesar font angka
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnalyticsScreenPreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header dengan gradient ungu yang diperkecil
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF5A2A82),
                                    Color(0xFF7C3AED)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Financial Analytics",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Insights for June 2024",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 0.25.sp
                        )
                    }
                }

                // Content area dengan month selector di tengah
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = (-20).dp)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Month Selector Card - kecil dan di tengah
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous month button
                            IconButton(
                                onClick = { },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        Color(0xFF5A2A82).copy(alpha = 0.1f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    contentDescription = "Previous month",
                                    tint = Color(0xFF5A2A82),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Center content dengan Calendar icon dan month name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = Color(0xFF5A2A82),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "June 2024",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5A2A82),
                                    letterSpacing = 0.3.sp
                                )
                            }

                            // Next month button
                            IconButton(
                                onClick = { },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        Color(0xFF5A2A82).copy(alpha = 0.1f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Next month",
                                    tint = Color(0xFF5A2A82),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(0.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Statistics Cards Preview
                        item {
                            StatisticsCardsSectionPreview()
                        }

                        // Savings Rate Preview
                        item {
                            SavingsRateSectionPreview()
                        }

                        // Budget Performance Card dengan toggle yang sebenarnya
                        item {
                            BudgetPerformanceCardPreview()
                        }

                        // Income vs Expenses Chart yang sebenarnya
                        item {
                            IncomeExpensesChartPreview()
                        }

                        // Interactive Donut Charts Preview
                        item {
                            // Expenses Donut Chart
                            InteractiveDonutChart(
                                title = "Expense Categories",
                                titleIcon = "💸",
                                subtitle = "Monthly expenses by category",
                                categories = listOf(
                                    CategoryData("Food", 1500000.0, Color(0xFFEF4444), Icons.Default.Fastfood),
                                    CategoryData("Transport", 500000.0, Color(0xFF10B981), Icons.Default.DirectionsCar),
                                    CategoryData("Entertainment", 300000.0, Color(0xFF3B82F6), Icons.Default.Movie)
                                ),
                                selectedCategory = null,
                                onCategoryClick = {},
                                centerColor = Color(0xFFEF4444),
                                monthName = "June 2024"
                            )
                        }
                        item {
                            InteractiveDonutChartsIncome()
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatisticsCardsSectionPreview() {
    MaterialTheme {
        StatisticsCardsSection(
            totalIncome = 3200000.0,
            totalExpenses = 2650000.0,
            savings = 550000.0
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SavingsRateSectionPreview() {
    MaterialTheme {
        SavingsRateSection(
            savingsRate = 17.2,
            selectedMonth = "2024-06"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StatCardPreview() {
    MaterialTheme {
        StatCard(
            title = "Income",
            amount = 3200000.0,
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            color = Color(0xFF10B981),
            backgroundColor = Color(0xFF10B981).copy(alpha = 0.1f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MonthSelectorPreview() {
    MaterialTheme {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            Color(0xFF5A2A82).copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous month",
                        tint = Color(0xFF5A2A82),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF5A2A82),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "June 2024",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5A2A82),
                        letterSpacing = 0.3.sp
                    )
                }

                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            Color(0xFF5A2A82).copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next month",
                        tint = Color(0xFF5A2A82),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BudgetPerformanceCardPreview() {
    MaterialTheme {
        BudgetPerformanceCard(
            budgetPeriod = TimePeriod.MONTHLY,
            onPeriodChange = { /* Handle period change */ },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun IncomeExpensesChartPreview() {
    MaterialTheme {
        IncomeExpensesChart(
            timePeriod = TimePeriod.MONTHLY,
            onPeriodChange = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InteractiveDonutChartsIncome() {
    MaterialTheme {
        InteractiveDonutChart(
            title = "Income Categories",
            titleIcon = "💰",
            subtitle = "Monthly expenses by category",
            categories = listOf(
                CategoryData("Food", 1500000.0, Color(0xFFEF4444), Icons.Default.Fastfood),
                CategoryData("Transport", 500000.0, Color(0xFF10B981), Icons.Default.DirectionsCar),
                CategoryData("Entertainment", 300000.0, Color(0xFF3B82F6), Icons.Default.Movie)
            ),
            selectedCategory = null,
            onCategoryClick = {},
            centerColor = Color(0xFFEF4444),
            monthName = "June 2024"
        )
    }
}
