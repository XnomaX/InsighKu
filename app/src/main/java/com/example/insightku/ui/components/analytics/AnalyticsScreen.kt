package com.example.insightku.ui.components.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.insightku.viewmodel.AnalyticsViewModel
import java.text.NumberFormat
import java.util.*

// Data models
data class ExpenseCategory(
    val name: String,
    val value: Double,
    val color: Color,
    val icon: ImageVector
)

data class IncomeCategory(
    val name: String,
    val value: Double,
    val color: Color,
    val icon: ImageVector
)

data class MonthlyData(
    val totalIncome: Double,
    val totalExpenses: Double,
    val expenseCategories: List<ExpenseCategory>,
    val incomeCategories: List<IncomeCategory>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    
    // Sample data (replace with actual data from viewModel)
    val monthlyData = remember {
        mapOf(
            "2024-06" to MonthlyData(
                totalIncome = 3500.0,
                totalExpenses = 2650.0,
                expenseCategories = listOf(
                    ExpenseCategory("Food & Drinks", 850.0, Color(0xFFF59E0B), Icons.Default.Restaurant),
                    ExpenseCategory("Transportation", 650.0, Color(0xFF3B82F6), Icons.Default.DirectionsCar),
                    ExpenseCategory("Entertainment", 420.0, Color(0xFF8B5CF6), Icons.Default.SportsEsports),
                    ExpenseCategory("Shopping", 380.0, Color(0xFFEC4899), Icons.Default.ShoppingBag),
                    ExpenseCategory("Coffee", 280.0, Color(0xFF92400E), Icons.Default.LocalCafe),
                    ExpenseCategory("Housing", 1070.0, Color(0xFFEF4444), Icons.Default.Home)
                ),
                incomeCategories = listOf(
                    IncomeCategory("Salary", 2800.0, Color(0xFF10B981), Icons.Default.Work),
                    IncomeCategory("Freelance", 400.0, Color(0xFF06B6D4), Icons.Default.Computer),
                    IncomeCategory("Investment", 200.0, Color(0xFF8B5CF6), Icons.Default.TrendingUp),
                    IncomeCategory("Other", 100.0, Color(0xFFF59E0B), Icons.Default.AccountBalanceWallet)
                )
            )
        )
    }
    
    var selectedMonth by remember { mutableStateOf("2024-06") }
    var selectedExpenseCategory by remember { mutableStateOf<String?>(null) }
    var selectedIncomeCategory by remember { mutableStateOf<String?>(null) }
    
    val currentData = monthlyData[selectedMonth] ?: monthlyData.values.first()
    val totalExpenses = currentData.expenseCategories.sumOf { it.value }
    val totalIncome = currentData.incomeCategories.sumOf { it.value }
    val savings = totalIncome - totalExpenses
    val savingsRate = (savings / totalIncome * 100)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // Header with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF5A2A82),
                            Color(0xFF7C3AED),
                            Color(0xFF4A90E2)
                        )
                    )
                )
                .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 48.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Financial Analytics",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Insights for June 2024",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-24).dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Statistics Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Income",
                    amount = totalIncome,
                    icon = Icons.Default.TrendingUp,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Expenses", 
                    amount = totalExpenses,
                    icon = Icons.Default.TrendingDown,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Savings",
                    amount = kotlin.math.abs(savings),
                    icon = Icons.Default.Savings,
                    color = Color(0xFF5A2A82),
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Savings Rate Card
            SavingsRateCard(
                savingsRate = savingsRate,
                targetRate = 20.0
            )
            
            // Expense Categories Donut Chart
            InteractiveDonutChart(
                title = "💸 Expense Categories - June 2024",
                categories = currentData.expenseCategories.map { category ->
                    ChartCategory(
                        name = category.name,
                        value = category.value,
                        color = category.color,
                        icon = category.icon
                    )
                },
                selectedCategory = selectedExpenseCategory,
                onCategoryClick = { categoryName ->
                    selectedExpenseCategory = if (selectedExpenseCategory == categoryName) {
                        null
                    } else {
                        categoryName
                    }
                },
                isExpense = true
            )
            
            // Income Categories Donut Chart
            InteractiveDonutChart(
                title = "💰 Income Categories - June 2024",
                categories = currentData.incomeCategories.map { category ->
                    ChartCategory(
                        name = category.name,
                        value = category.value,
                        color = category.color,
                        icon = category.icon
                    )
                },
                selectedCategory = selectedIncomeCategory,
                onCategoryClick = { categoryName ->
                    selectedIncomeCategory = if (selectedIncomeCategory == categoryName) {
                        null
                    } else {
                        categoryName
                    }
                },
                isExpense = false
            )
        }
        
        // Add bottom padding for bottom navigation
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun StatCard(
    title: String,
    amount: Double,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = formatCurrency(amount),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
        }
    }
}

@Composable
private fun SavingsRateCard(
    savingsRate: Double,
    targetRate: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Savings Rate",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "Monthly savings performance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Target: ${targetRate.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${kotlin.math.abs(savingsRate).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF5A2A82)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { (kotlin.math.abs(savingsRate) / 100f).toFloat().coerceAtMost(1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF5A2A82),
                trackColor = Color(0xFF5A2A82).copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = when {
                    savingsRate >= targetRate -> "🎉 Congratulations! Target achieved"
                    savingsRate >= 0 -> "${(targetRate - savingsRate).toInt()}% more to reach your goal"
                    else -> "⚠️ Spending exceeded income this month"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class ChartCategory(
    val name: String,
    val value: Double,
    val color: Color,
    val icon: ImageVector
)

@Composable
private fun InteractiveDonutChart(
    title: String,
    categories: List<ChartCategory>,
    selectedCategory: String?,
    onCategoryClick: (String) -> Unit,
    isExpense: Boolean
) {
    val totalValue = categories.sumOf { it.value }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = if (selectedCategory != null) {
                    val selectedValue = categories.find { it.name == selectedCategory }?.value ?: 0.0
                    "$selectedCategory - ${formatCurrency(selectedValue)}"
                } else {
                    "Total ${if (isExpense) "Expenses" else "Income"}: ${formatCurrency(totalValue)}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Simple donut chart representation using progress indicators
            DonutChartComposable(
                categories = categories,
                selectedCategory = selectedCategory,
                totalValue = totalValue,
                isExpense = isExpense
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Category Legend
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.chunked(2).forEach { rowCategories ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCategories.forEach { category ->
                            CategoryLegendItem(
                                category = category,
                                totalValue = totalValue,
                                isSelected = selectedCategory == category.name,
                                onClick = { onCategoryClick(category.name) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Add empty space if odd number of categories
                        if (rowCategories.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutChartComposable(
    categories: List<ChartCategory>,
    selectedCategory: String?,
    totalValue: Double,
    isExpense: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Center text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val selectedValue = if (selectedCategory != null) {
                categories.find { it.name == selectedCategory }?.value ?: totalValue
            } else {
                totalValue
            }
            
            Text(
                text = formatCurrency(selectedValue),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (isExpense) Color(0xFFEF4444) else Color(0xFF10B981)
            )
            
            Text(
                text = selectedCategory ?: if (isExpense) "Total Expenses" else "Total Income",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Simple circular progress representation
        categories.forEachIndexed { index, category ->
            val percentage = (category.value / totalValue).toFloat()
            val rotation = (categories.take(index).sumOf { it.value } / totalValue * 360).toFloat()
            
            CircularProgressIndicator(
                progress = { percentage },
                modifier = Modifier
                    .size(160.dp - (index * 10).dp)
                    .graphicsLayer {
                        rotationZ = rotation
                    },
                color = category.color.copy(
                    alpha = if (selectedCategory == null || selectedCategory == category.name) 1f else 0.3f
                ),
                strokeWidth = 12.dp,
                trackColor = Color.Transparent
            )
        }
    }
}

@Composable
private fun CategoryLegendItem(
    category: ChartCategory,
    totalValue: Double,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percentage = ((category.value / totalValue) * 100).toInt()
    
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0xFF5A2A82).copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, Color(0xFF5A2A82))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        category.color.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    category.icon,
                    contentDescription = category.name,
                    tint = category.color,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = formatCurrency(category.value),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
    return formatter.format(amount).replace("IDR", "Rp")
}