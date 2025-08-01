package com.example.insightku.ui.components.budgeting

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.insightku.viewmodel.BudgetingViewModel
import java.text.NumberFormat
import java.util.*

// Data models
data class BudgetCategory(
    val id: String,
    val name: String,
    val budgetAmount: Double,
    val spentAmount: Double,
    val icon: ImageVector,
    val color: Color
) {
    val remaining: Double get() = budgetAmount - spentAmount
    val percentage: Float get() = (spentAmount / budgetAmount).toFloat().coerceAtMost(1f)
    val isOverBudget: Boolean get() = spentAmount > budgetAmount
}

data class RecurringPayment(
    val id: String,
    val name: String,
    val amount: Double,
    val dueDate: String,
    val icon: ImageVector,
    val color: Color
)

data class BudgetSummary(
    val totalBudget: Double,
    val totalSpent: Double,
    val remaining: Double,
    val percentage: Float,
    val overBudgetCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetingScreen(
    modifier: Modifier = Modifier,
    onNavigateToAddCategory: () -> Unit = {},
    onNavigateToAddRecurring: () -> Unit = {},
    onEditCategory: (BudgetCategory) -> Unit = {},
    onManageRecurring: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    
    // Sample data (replace with actual data from viewModel)
    val budgetCategories = remember {
        listOf(
            BudgetCategory(
                id = "1",
                name = "Food & Drinks",
                budgetAmount = 800.0,
                spentAmount = 650.0,
                icon = Icons.Default.Restaurant,
                color = Color(0xFFF59E0B)
            ),
            BudgetCategory(
                id = "2", 
                name = "Transportation",
                budgetAmount = 400.0,
                spentAmount = 520.0,
                icon = Icons.Default.DirectionsCar,
                color = Color(0xFF3B82F6)
            ),
            BudgetCategory(
                id = "3",
                name = "Entertainment",
                budgetAmount = 300.0,
                spentAmount = 180.0,
                icon = Icons.Default.SportsEsports,
                color = Color(0xFF8B5CF6)
            ),
            BudgetCategory(
                id = "4",
                name = "Shopping",
                budgetAmount = 600.0,
                spentAmount = 480.0,
                icon = Icons.Default.ShoppingBag,
                color = Color(0xFFEC4899)
            ),
            BudgetCategory(
                id = "5",
                name = "Housing",
                budgetAmount = 1200.0,
                spentAmount = 1200.0,
                icon = Icons.Default.Home,
                color = Color(0xFFEF4444)
            )
        )
    }
    
    val recurringPayments = remember {
        listOf(
            RecurringPayment(
                id = "1",
                name = "Netflix Subscription",
                amount = 159900.0,
                dueDate = "Jun 28",
                icon = Icons.Default.Subscriptions,
                color = Color(0xFFEF4444)
            ),
            RecurringPayment(
                id = "2",
                name = "Rent Payment", 
                amount = 12000000.0,
                dueDate = "Jul 1",
                icon = Icons.Default.Home,
                color = Color(0xFF3B82F6)
            ),
            RecurringPayment(
                id = "3",
                name = "Gym Membership",
                amount = 450000.0,
                dueDate = "Jul 5",
                icon = Icons.Default.FitnessCenter,
                color = Color(0xFF10B981)
            )
        )
    }
    
    val budgetSummary = remember(budgetCategories) {
        val totalBudget = budgetCategories.sumOf { it.budgetAmount }
        val totalSpent = budgetCategories.sumOf { it.spentAmount }
        val remaining = totalBudget - totalSpent
        val percentage = (totalSpent / totalBudget).toFloat()
        val overBudgetCount = budgetCategories.count { it.isOverBudget }
        
        BudgetSummary(
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            remaining = remaining,
            percentage = percentage,
            overBudgetCount = overBudgetCount
        )
    }

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
                            Color(0xFF7C3AED)
                        )
                    )
                )
                .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 48.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.TrackChanges,
                        contentDescription = "Budget",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Budget Overview",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Track your spending goals for June 2024",
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
            // Budget Summary Card
            BudgetSummaryCard(
                summary = budgetSummary
            )
            
            // Category Budgets
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
                        text = "Category Budgets",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        budgetCategories.forEach { category ->
                            CategoryCard(
                                category = category,
                                onEdit = { onEditCategory(category) }
                            )
                        }
                        
                        // Add Category Button
                        OutlinedButton(
                            onClick = onNavigateToAddCategory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF5A2A82)
                            ),
                            border = BorderStroke(
                                width = 1.dp, 
                                color = MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Category",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tambah Kategori Baru",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            // Scheduled Payments
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = "Scheduled Payments",
                                    tint = Color(0xFF5A2A82),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Scheduled Payments",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Recurring subscriptions and bills",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        TextButton(
                            onClick = onManageRecurring,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF5A2A82)
                            )
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Manage",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Manage")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recurringPayments.forEach { payment ->
                            RecurringPaymentItem(payment = payment)
                        }
                        
                        // Add Payment Button
                        OutlinedButton(
                            onClick = onNavigateToAddRecurring,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF5A2A82)
                            ),
                            border = BorderStroke(
                                width = 1.dp, 
                                color = MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Payment",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tambah Pembayaran Berulang",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
        
        // Add bottom padding for bottom navigation
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun BudgetSummaryCard(
    summary: BudgetSummary
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
                text = "Budget Summary",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Budget Progress",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(summary.percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (summary.percentage > 1f) Color(0xFFEF4444) else Color(0xFF5A2A82)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { summary.percentage.coerceAtMost(1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (summary.percentage > 1f) Color(0xFFEF4444) else Color(0xFF5A2A82),
                trackColor = Color(0xFF5A2A82).copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Summary stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(summary.totalBudget),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = if (summary.remaining >= 0) "Remaining" else "Over Budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(kotlin.math.abs(summary.remaining)),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (summary.remaining >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }
            
            if (summary.overBudgetCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${summary.overBudgetCount} categories over budget",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: BudgetCategory,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                category.color.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            category.icon,
                            contentDescription = category.name,
                            tint = category.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${formatCurrency(category.spentAmount)} / ${formatCurrency(category.budgetAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = if (category.isOverBudget) "Over Budget" else "Remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(kotlin.math.abs(category.remaining)),
                        style = MaterialTheme.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (category.isOverBudget) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { category.percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (category.isOverBudget) Color(0xFFEF4444) else category.color,
                trackColor = category.color.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun RecurringPaymentItem(
    payment: RecurringPayment
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            payment.color.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        payment.icon,
                        contentDescription = payment.name,
                        tint = payment.color,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = payment.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Due ${payment.dueDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = formatCurrency(payment.amount),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = payment.color
            )
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val locale = Locale.Builder().setLanguage("id").setRegion("ID").build()
    val formatter = NumberFormat.getCurrencyInstance(locale)
    return formatter.format(amount)
}