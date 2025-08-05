package com.example.insightku.ui.components.dashboard

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.ui.theme.InsightKuTheme
import com.example.insightku.viewmodel.DashboardViewModel
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToTransactionDetails: () -> Unit = {}
) {
    val uiState = viewModel.uiState
    var balanceVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.onEvent(DashboardEvent.LoadDashboardData)
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                DashboardHeader(
                    balance = uiState.totalBalance,
                    monthlyIncome = uiState.monthlyIncome,
                    monthlyExpenses = uiState.monthlyExpenses,
                    balanceVisible = balanceVisible,
                    onToggleVisibility = { balanceVisible = !balanceVisible }
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    AiForecastCard(
                        weeklyData = uiState.weeklyForecastData,
                        monthlyData = uiState.monthlyForecastData,
                        aiInsight = uiState.aiInsightMessage
                    )
                    RecentTransactionsCard(
                        transactions = uiState.recentTransactions,
                        onViewAllClick = onNavigateToTransactionDetails
                    )
                    DailyStreakCard(
                        currentStreak = uiState.currentStreak,
                        hasTrackedToday = uiState.hasTrackedToday,
                        onAddTransaction = { /* TODO: Navigate to add transaction */ }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardHeader(
    balance: Double,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    balanceVisible: Boolean,
    onToggleVisibility: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 64.dp)
    ) {
        Text(
            text = "Good afternoon!",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Here's your financial overview",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        BalanceCard(balance, monthlyIncome, monthlyExpenses, balanceVisible, onToggleVisibility)
    }
}

@Composable
fun BalanceCard(
    balance: Double,
    income: Double,
    expense: Double,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-US"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isVisible) formatter.format(balance) else "••••••",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "Wallet",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(12.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            IconButton(
                onClick = onToggleVisibility,
                modifier = Modifier.offset(x = (-12).dp)
            ) {
                Icon(
                    imageVector = if (isVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = "Toggle Visibility",
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Income",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        formatter.format(income),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Expenses",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        formatter.format(expense),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun AiForecastCard(
    weeklyData: List<Float> = emptyList(),
    monthlyData: List<Float> = emptyList(),
    aiInsight: String = "",
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf("week") }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(all = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Forecast",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Predicted expenses for the next ${if (selectedPeriod == "week") "7 days" else "12 months"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { /* Container for the toggle buttons */ },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row {
                        Button(
                            onClick = { selectedPeriod = "week" },
                            modifier = Modifier.height(36.dp),
                            elevation = if (selectedPeriod == "week") ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedPeriod == "week") MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (selectedPeriod == "week") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = CircleShape
                        ) {
                            Text("Week")
                        }
                        Button(
                            onClick = { selectedPeriod = "month" },
                            modifier = Modifier.height(36.dp),
                            elevation = if (selectedPeriod == "month") ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedPeriod == "month") MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (selectedPeriod == "month") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = CircleShape
                        ) {
                            Text("Month")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val labels = if (selectedPeriod == "week") {
                listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
            } else {
                listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            }

            LineChart(
                data = if (selectedPeriod == "week") weeklyData else monthlyData,
                labels = labels,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (aiInsight.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "AI Insight",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = aiInsight,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun LineChart(
    data: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
) {
    val animatedProgress = remember { Animatable(0f) }
    var selectedPoint by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(durationMillis = 1000))
        selectedPoint = null
    }

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelTextStyle = TextStyle(
        color = labelColor,
        fontSize = 12.sp,
        textAlign = TextAlign.Center
    )

    Box(modifier = modifier.pointerInput(data) {
        detectTapGestures { offset ->
            val stepX = if (data.size > 1) size.width / (data.size - 1).toFloat() else 0f
            val tappedIndex = if (stepX > 0) (offset.x / stepX).roundToInt().coerceIn(0, data.size - 1) else 0
            selectedPoint = if (selectedPoint == tappedIndex) null else tappedIndex
        }
    }) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val labelHeight = with(density) { 20.dp.toPx() }
            val chartHeight = size.height - labelHeight
            val maxValue = data.maxOrNull() ?: 0f
            if (maxValue == 0f || data.size < 2) return@Canvas

            val stepX = size.width / (data.size - 1).toFloat()
            val animatedValue = animatedProgress.value

            // Draw X-axis labels
            val labelInterval = if (labels.size > 10) 2 else 1
            labels.forEachIndexed { index, label ->
                if (index % labelInterval == 0) {
                    val textLayoutResult = textMeasurer.measure(
                        text = AnnotatedString(label),
                        style = labelTextStyle
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = index * stepX - (textLayoutResult.size.width / 2f),
                            y = chartHeight + with(density) { 4.dp.toPx() }
                        )
                    )
                }
            }

            // Prepare paths
            val linePath = Path()
            val fillPath = Path()
            val firstY = chartHeight * (1 - (data.first() / maxValue) * animatedValue)
            fillPath.moveTo(0f, chartHeight)
            fillPath.lineTo(0f, firstY)
            linePath.moveTo(0f, firstY)

            // Draw line and fill
            data.forEachIndexed { index, value ->
                val x = index * stepX
                val y = chartHeight * (1 - (value / maxValue) * animatedValue)

                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            fillPath.lineTo(size.width, chartHeight)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(fillColor, Color.Transparent)
                )
            )

            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 4.dp.toPx())
            )

            // Draw points and highlight selected
            data.forEachIndexed { index, value ->
                val x = index * stepX
                val y = chartHeight * (1 - (value / maxValue) * animatedValue)
                val isSelected = selectedPoint == index

                drawCircle(
                    color = lineColor,
                    radius = if (isSelected) 8.dp.toPx() else 4.dp.toPx(),
                    center = Offset(x, y)
                )

                if (isSelected) {
                    val popupText = "$${value.roundToInt()}"
                    val textLayoutResult = textMeasurer.measure(
                        text = AnnotatedString(popupText),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 12.sp,
                            background = Color.Black.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    )
                    val popupX = (x - textLayoutResult.size.width / 2f).coerceIn(0f, size.width - textLayoutResult.size.width)
                    val popupY = (y - textLayoutResult.size.height - 8.dp.toPx()).coerceAtLeast(0f)

                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(popupX, popupY)
                    )
                }
            }
        }
    }
}

@Composable
fun RecentTransactionsCard(transactions: List<TransactionItem> = emptyList(), onViewAllClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Latest financial activity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onViewAllClick
                ) {
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                transactions.take(3).forEach { transaction ->
                    TransactionItemRow(transaction)
                }
                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent transactions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItemRow(transaction: TransactionItem) {
    val isPositive = transaction.isIncome
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-US"))

    // Convert icon name string to ImageVector
    val icon = when (transaction.iconName.lowercase()) {
        "shopping" -> Icons.Default.ShoppingCart
        "food" -> Icons.Default.Restaurant
        "transport" -> Icons.Default.DirectionsCar
        "entertainment" -> Icons.Default.Movie
        "health" -> Icons.Default.LocalHospital
        "salary" -> Icons.Default.AttachMoney
        "investment" -> Icons.Default.TrendingUp
        else -> Icons.Default.AccountBalanceWallet
    }

    // Convert color hex to Color
    val color = try {
        Color(android.graphics.Color.parseColor(transaction.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transaction.title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (isPositive) "+" else "") + formatter.format(kotlin.math.abs(transaction.amount)),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = transaction.time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Data class for Transaction
data class Transaction(
    val id: Int,
    val title: String,
    val category: String,
    val amount: Double,
    val time: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun DailyStreakCard(
    currentStreak: Int = 5,
    hasTrackedToday: Boolean = false,
    onAddTransaction: () -> Unit = {}
) {
    // Generate weekly activity data (last 7 days)
    val weeklyActivity = remember {
        // Demo pattern: last 5 days active, today depends on tracking status
        listOf(true, true, true, true, true, false, hasTrackedToday)
    }

    val dayNames = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

    // Get week dates
    val weekDates = remember {
        val today = Calendar.getInstance()
        val dates = mutableListOf<Int>()

        for (i in 6 downTo 0) {
            val date = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            dates.add(date.get(Calendar.DAY_OF_MONTH))
        }
        dates
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Streak Harian",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$currentStreak hari berturut-turut",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Streak Counter Badge
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$currentStreak",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "days",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Weekly Calendar Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dayNames.forEachIndexed { index, day ->
                    val isToday = index == 6 // Last day is today
                    val isActive = weeklyActivity[index]

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Day Name
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Streak Indicator
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = when {
                                        isActive -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = CircleShape
                                )
                                .then(
                                    if (isToday && !isActive) {
                                        Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            CircleShape
                                        )
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak Day",
                                modifier = Modifier.size(24.dp),
                                tint = when {
                                    isActive -> Color.White
                                    isToday -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Date
                        Text(
                            text = "${weekDates[index]}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (isToday) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Warning Message (when not tracked today)
            if (!hasTrackedToday) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFFFF9E6), // Yellow-50
                                    Color(0xFFFFF1E6)  // Orange-50
                                )
                            ),
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            Color(0xFFFED7AA), // Yellow-200
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFD97706) // Yellow-600
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Streak kamu akan di-reset jika tidak melakukan input hari ini. Jangan lewatkan!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF92400E), // Yellow-800
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Action Button or Success State
            if (!hasTrackedToday) {
                Button(
                    onClick = onAddTransaction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lanjutkan Hari Ini",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Success State
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFECFDF5), // Green-50
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Success",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF059669) // Green-600
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Streak hari ini sudah tercatat!",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF059669) // Green-600
                    )
                }
            }
        }
    }
}

// Preview untuk setiap card
@Preview(name = "Balance Card Light", showBackground = true)
@Preview(name = "Balance Card Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun BalanceCardPreview() {
    InsightKuTheme {
        Surface {
            BalanceCard(
                balance = 4256.80,
                income = 3200.0,
                expense = 2650.0,
                isVisible = true,
                onToggleVisibility = {}
            )
        }
    }
}

@Preview(name = "AI Forecast Card Light", showBackground = true)
@Preview(name = "AI Forecast Card Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun AiForecastCardPreview() {
    InsightKuTheme {
        Surface {
            AiForecastCard()
        }
    }
}

@Preview(name = "Recent Transactions Card Light", showBackground = true)
@Preview(name = "Recent Transactions Card Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun RecentTransactionsCardPreview() {
    val dummyTransactions = listOf(
        TransactionItem(
            id = "1",
            title = "Grocery Shopping",
            category = "Food & Dining",
            amount = 85.50,
            time = "2 hours ago",
            isIncome = false,
            iconName = "shopping",
            colorHex = "#FF6B6B"
        ),
        TransactionItem(
            id = "2",
            title = "Salary Payment",
            category = "Income",
            amount = 3500.00,
            time = "1 day ago",
            isIncome = true,
            iconName = "salary",
            colorHex = "#4ECDC4"
        ),
        TransactionItem(
            id = "3",
            title = "Coffee Shop",
            category = "Food & Dining",
            amount = 12.75,
            time = "3 hours ago",
            isIncome = false,
            iconName = "food",
            colorHex = "#45B7D1"
        ),
        TransactionItem(
            id = "4",
            title = "Uber Ride",
            category = "Transportation",
            amount = 25.30,
            time = "5 hours ago",
            isIncome = false,
            iconName = "transport",
            colorHex = "#96CEB4"
        )
    )

    InsightKuTheme {
        Surface {
            RecentTransactionsCard(transactions = dummyTransactions)
        }
    }
}

@Preview(name = "Daily Streak Card Active Light", showBackground = true)
@Preview(name = "Daily Streak Card Active Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun DailyStreakCardActivePreview() {
    InsightKuTheme {
        Surface {
            DailyStreakCard(
                currentStreak = 5,
                hasTrackedToday = false,
                onAddTransaction = {}
            )
        }
    }
}

@Preview(name = "Daily Streak Card Completed", showBackground = true)
@Composable
fun DailyStreakCardCompletedPreview() {
    InsightKuTheme {
        Surface {
            DailyStreakCard(
                currentStreak = 6,
                hasTrackedToday = true,
                onAddTransaction = {}
            )
        }
    }
}

@Preview(name = "Dashboard Header Light", showBackground = true)
@Preview(name = "Dashboard Header Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun DashboardHeaderPreview() {
    InsightKuTheme {
        DashboardHeader(
            balance = 4256.80,
            monthlyIncome = 3200.0,
            monthlyExpenses = 2650.0,
            balanceVisible = true,
            onToggleVisibility = {}
        )
    }
}
