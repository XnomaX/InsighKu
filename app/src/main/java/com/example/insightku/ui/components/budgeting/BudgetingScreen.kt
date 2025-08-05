package com.example.insightku.ui.components.budgeting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.data.model.RecurringBudget
import com.example.insightku.ui.dialogs.AddCategoryDialog
import com.example.insightku.ui.dialogs.EditCategoryDialog
import com.example.insightku.ui.dialogs.RecurringBudgetsDialog
import com.example.insightku.viewmodel.BudgetingViewModel
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs
import androidx.core.graphics.toColorInt

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
}

@Composable
fun BudgetingScreenContent(
    uiState: BudgetingState,
    onEvent: (BudgetingEvent) -> Unit
) {
     val nextRecurringPayments = listOf(
        ScheduledPayment("Netflix Subscription", 250000.0, "Jun 28", Icons.Default.Repeat, Color(0xFFEF4444)),
        ScheduledPayment("Rent Payment", 2500000.0, "Jul 1", Icons.Default.Home, Color(0xFF3B82F6)),
        ScheduledPayment("Gym Membership", 350000.0, "Jul 5", Icons.Default.FitnessCenter, Color(0xFF10B981))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Header() }

            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .offset(y = (-60).dp), // Adjust overlap
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        payments = nextRecurringPayments,
                        onManageRecurring = { onEvent(BudgetingEvent.ShowRecurringBudgetsDialog) }
                    )
                }
            }
        }

        // --- DIALOG INTEGRATION ---
        if (uiState.showAddBudgetDialog) {
            AddCategoryDialog(
                isOpen = true,
                onDismiss = { onEvent(BudgetingEvent.HideAddBudgetDialog) },
                onCategoryAdded = { category ->
                    onEvent(BudgetingEvent.AddCategory(category))
                }
            )
        }
        if (uiState.editingCategory != null) {
            EditCategoryDialog(
                isOpen = true,
                onDismiss = { onEvent(BudgetingEvent.HideEditBudgetDialog) },
                category = uiState.editingCategory,
                onCategoryEdited = { updatedCategory ->
                    onEvent(BudgetingEvent.UpdateCategory(updatedCategory))
                },
                onCategoryDeleted = { categoryId ->
                    onEvent(BudgetingEvent.DeleteCategory(categoryId))
                }
            )
        }
        if (uiState.showRecurringBudgetsDialog) {
            RecurringBudgetsDialog(
                isOpen = true,
                onDismiss = { onEvent(BudgetingEvent.HideRecurringBudgetsDialog) },
                recurringBudgets = uiState.recurringBudgets,
                onBudgetAdded = { onEvent(BudgetingEvent.AddRecurringBudget(it)) },
                onBudgetEdited = { onEvent(BudgetingEvent.UpdateRecurringBudget(it)) },
                onBudgetDeleted = { onEvent(BudgetingEvent.DeleteRecurringBudget(it)) }
            )
        }
    }
}

// --- UI Components ---

@Composable
private fun Header() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp) // Increase height for better visual balance
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF5A2A82), Color(0xFF7C3AED))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp), // Space for the overlapping cards
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "Budget Target",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Budget Overview",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Track your spending goals for June 2024",
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun BudgetSummaryCard(
    totalBudget: Double,
    totalSpent: Double,
    remaining: Double,
    percentage: Float,
    overBudgetCategories: List<BudgetCategory>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // New 3-column layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SummaryItem("Total Budget", totalBudget, color = MaterialTheme.colorScheme.primary)
                SummaryItem("Spent", totalSpent, color = MaterialTheme.colorScheme.onSurface)
                SummaryItem("Remaining", remaining, color = if (remaining >= 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.error)
            }

            // Overall Progress
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Overall Progress", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${"%.1f".format(percentage)}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
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


            // Budget Alert Section
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
        Spacer(modifier = Modifier.height(4.dp))
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
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Alert",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = "Budget Alert", fontWeight = FontWeight.Bold)
                val categoryText = if (overBudgetCategories.size == 1) "category has" else "categories have"
                Text(
                    text = "${overBudgetCategories.size} $categoryText reached the limit",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


@Composable
private fun CategoryBudgetsCard(
    categories: List<BudgetCategory>,
    onAddCategory: () -> Unit,
    onEditCategory: (BudgetCategory) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Category Budgets",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                categories.forEach { category ->
                    CategoryCard(category = category, onEdit = { onEditCategory(category) })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            DashedButton(text = "Tambah Kategori Baru", onClick = onAddCategory)
        }
    }
}

@Composable
private fun CategoryCard(category: BudgetCategory, onEdit: (BudgetCategory) -> Unit) {
    val percentage = category.utilizationPercentage
    val remaining = category.remainingAmount
    val lastMonthSpent = category.budgetedAmount * 0.8 // Dummy data

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(category.color.toColorInt()).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconForCategory(category.icon),
                            contentDescription = category.name,
                            tint = Color(category.color.toColorInt()),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(text = category.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "${formatCurrency(category.spentAmount, 0)} of ${formatCurrency(category.budgetedAmount, 0)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatusBadge(percentage = percentage)
                    IconButton(onClick = { onEdit(category) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column {
                LinearProgressIndicator(
                    progress = { (percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (percentage > 100) MaterialTheme.colorScheme.error else Color(
                        category.color.toColorInt()),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "${"%.1f".format(percentage)}% used",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (remaining >= 0) "${formatCurrency(remaining, 0)} left" else "${formatCurrency(abs(remaining), 0)} over",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            val difference = category.spentAmount - lastMonthSpent
            if (difference != 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isUp = difference > 0
                    val icon = if (isUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown
                    val color = if (isUp) MaterialTheme.colorScheme.error else Color(0xFF16A34A)
                    Icon(
                        imageVector = icon,
                        contentDescription = "Trend",
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${formatCurrency(abs(difference), 0)} vs last month",
                        style = MaterialTheme.typography.labelSmall,
                        color = color
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(percentage: Double) {
    val (text, color) = when {
        percentage > 100 -> "Over Budget" to MaterialTheme.colorScheme.errorContainer
        percentage > 80 -> "Alert" to Color(0xFFFFFBEB)
        else -> "On Track" to Color(0xFFF0FDF4)
    }
    val contentColor = when {
        percentage > 100 -> MaterialTheme.colorScheme.onErrorContainer
        percentage > 80 -> Color(0xFFB45309)
        else -> Color(0xFF15803D)
    }
    Surface(
        color = color,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}


@Composable
private fun ScheduledPaymentsCard(
    payments: List<ScheduledPayment>,
    onManageRecurring: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Scheduled Payments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Recurring subscriptions and bills", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onManageRecurring) {
                    Icon(Icons.Default.Settings, contentDescription = "Manage", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manage")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            payments.forEach { payment ->
                PaymentItem(payment = payment)
                Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            DashedButton(text = "Tambah Pembayaran Berulang", onClick = onManageRecurring)
        }
    }
}

@Composable
private fun PaymentItem(payment: ScheduledPayment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(payment.color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = payment.icon, contentDescription = null, tint = payment.color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = payment.name, fontWeight = FontWeight.SemiBold)
            Text(text = "Due ${payment.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = formatCurrency(payment.amount), fontWeight = FontWeight.Bold)
            Icon(Icons.Default.Notifications, contentDescription = "Reminder", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun DashedButton(text: String, onClick: () -> Unit) {
    val strokeWidth = with(LocalDensity.current) { 2.dp.toPx() }
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    val cornerRadius = with(LocalDensity.current) { 12.dp.toPx() }
    val interactionSource = remember { MutableInteractionSource()}
    val borderColor = Color.Gray

    val isPressed by interactionSource.collectIsPressedAsState()
    val backgroundColor = if (isPressed) Color(0xFF7C3AED).copy(alpha = 0.1f) else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = borderColor,
                style = Stroke(width = strokeWidth, pathEffect = pathEffect),
                cornerRadius = CornerRadius(cornerRadius)
            )
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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

fun getIconForCategory(iconName: String): ImageVector {
    return when (iconName) {
        "restaurant" -> Icons.Default.Restaurant
        "directions_car" -> Icons.Default.DirectionsCar
        "shopping_bag" -> Icons.Default.ShoppingBag
        "movie" -> Icons.Default.Movie
        "receipt" -> Icons.Default.Receipt
        else -> Icons.Default.Category
    }
}

// --- Previews ---
@Preview(showBackground = true, name = "Full Budgeting Screen")
@Composable
fun BudgetingScreenPreview() {
    val fakeState = BudgetingState(
        totalBudget = 5000000.0,
        totalSpent = 3750000.0,
        remainingBudget = 1250000.0,
        budgetUtilizationPercentage = 75.0,
        budgetCategories = listOf(
            BudgetCategory("1", "Food & Dining", 2000000.0, 1650000.0, "#FF6B6B", "restaurant"),
            BudgetCategory("2", "Transportation", 1000000.0, 750000.0, "#4ECDC4", "directions_car"),
            BudgetCategory("3", "Shopping", 1500000.0, 1800000.0, "#45B7D1", "shopping_bag")
        ),
        overBudgetCategories = listOf(
            BudgetCategory("3", "Shopping", 1500000.0, 1800000.0, "#45B7D1", "shopping_bag")
        ),
        recurringBudgets = emptyList()
    )

    MaterialTheme {
        BudgetingScreenContent(uiState = fakeState, onEvent = {})
    }
}

@Preview(showBackground = true, name = "Budget Summary Card")
@Composable
private fun BudgetSummaryCardPreview() {
    MaterialTheme {
        BudgetSummaryCard(
            totalBudget = 15000000.0,
            totalSpent = 7850000.0,
            remaining = 7150000.0,
            percentage = 52.3f,
            overBudgetCategories = emptyList()
        )
    }
}

@Preview(showBackground = true, name = "Budget Summary With Alert")
@Composable
private fun BudgetSummaryCardWithAlertPreview() {
    MaterialTheme {
        BudgetSummaryCard(
            totalBudget = 15000000.0,
            totalSpent = 16000000.0,
            remaining = -1000000.0,
            percentage = 106.7f,
            overBudgetCategories = listOf(
                BudgetCategory("1", "Shopping", 0.0,0.0,"","")
            )
        )
    }
}

@Preview(showBackground = true, name = "Category Budgets Card")
@Composable
private fun CategoryBudgetsCardPreview() {
    val categories = listOf(
        BudgetCategory("1", "Food & Dining", 2000000.0, 1650000.0, "#FF6B6B", "restaurant"),
        BudgetCategory("2", "Transportation", 1000000.0, 750000.0, "#4ECDC4", "directions_car"),
        BudgetCategory("3", "Shopping", 1500000.0, 1800000.0, "#45B7D1", "shopping_bag")
    )
    MaterialTheme {
        CategoryBudgetsCard(categories = categories, onAddCategory = {}, onEditCategory = {})
    }
}

@Preview(showBackground = true, name = "Scheduled Payments Card")
@Composable
private fun ScheduledPaymentsCardPreview() {
     val payments = listOf(
        ScheduledPayment("Netflix Subscription", 250000.0, "Jun 28", Icons.Default.Repeat, Color(0xFFEF4444)),
        ScheduledPayment("Rent Payment", 2500000.0, "Jul 1", Icons.Default.Home, Color(0xFF3B82F6)),
    )
    MaterialTheme {
        ScheduledPaymentsCard(payments = payments, onManageRecurring = {})
    }
}
