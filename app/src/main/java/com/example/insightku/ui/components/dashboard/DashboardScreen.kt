package com.example.insightku.ui.components.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.ui.theme.Dimens
import com.example.insightku.ui.theme.LocalResponsiveDimens
import com.example.insightku.ui.theme.formatCurrency
import com.example.insightku.utils.CategoryUtils
import com.example.insightku.viewmodel.DashboardViewModel
import java.util.Calendar
import kotlin.math.abs

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun getTimeGreeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 0..11  -> "Good morning"
    in 12..16 -> "Good afternoon"
    else      -> "Good evening"
}

private fun String.firstName(): String = split(" ").firstOrNull()?.ifBlank { this } ?: this

/** Return fire emoji string scaled by streak level */
private fun streakFireEmoji(streak: Int): String = when {
    streak >= 30 -> "🔥🔥🔥"
    streak >= 10 -> "🔥🔥"
    streak >= 1  -> "🔥"
    else         -> "💤"
}

// ─── Root Screen ─────────────────────────────────────────────────────────────

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToTransactionDetails: () -> Unit,
    onAddTransaction: () -> Unit,
    onAddTransactionForStreak: () -> Unit = onAddTransaction
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        // Kondisi loading awal (sebelum ada data apapun)
        uiState.isLoading && uiState.recentTransactions.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        // Error state: Room kosong DAN Firestore refresh gagal (misal offline saat login)
        // ROOT CAUSE FIX: Tampilkan error + tombol retry alih-alih layar kosong membingungkan
        uiState.error != null && uiState.recentTransactions.isEmpty() && !uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "📡",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Text(
                        text = "Tidak dapat memuat data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = uiState.error ?: "Periksa koneksi internet dan coba lagi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.onEvent(DashboardEvent.RefreshData) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Coba Lagi", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        // Normal: tampilkan konten (termasuk jika ada error tapi data cache tersedia)
        else -> {
            DashboardScreenContent(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onNavigateToTransactionDetails = onNavigateToTransactionDetails,
                onAddTransaction = onAddTransaction,
                onAddTransactionForStreak = onAddTransactionForStreak
            )
        }
    }
}


// ─── Screen Content ───────────────────────────────────────────────────────────

@Composable
fun DashboardScreenContent(
    uiState: DashboardUiState,
    onEvent: (DashboardEvent) -> Unit,
    onNavigateToTransactionDetails: () -> Unit,
    onAddTransaction: () -> Unit,
    onAddTransactionForStreak: () -> Unit = onAddTransaction
) {
    val dimens = LocalResponsiveDimens.current
    var showStreakPopup by remember { mutableStateOf(false) }
    var showStreakDetail by remember { mutableStateOf(false) }

    // When hasTrackedToday flips true -> show popup
    val prevTracked = remember { mutableStateOf(uiState.hasTrackedToday) }
    LaunchedEffect(uiState.hasTrackedToday) {
        if (uiState.hasTrackedToday && !prevTracked.value) {
            showStreakPopup = true
        }
        prevTracked.value = uiState.hasTrackedToday
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            DashboardHeader(
                userName = uiState.userName,
                totalBalance = uiState.totalBalance,
                monthlyIncome = uiState.monthlyIncome,
                monthlyExpenses = uiState.monthlyExpenses,
                isBalanceVisible = uiState.isBalanceVisible,
                onToggleVisibility = { onEvent(DashboardEvent.ToggleBalanceVisibility) }
            )
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = dimens.screenHorizontalPadding).padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ISSUE 3 FIX: DailyStreakCard dipindahkan ke atas AiForecastCard.
                // Urutan baru: Streak → Forecast → Recent Transactions
                // Streak di atas lebih prominent, mendorong user tracking hari ini sebelum scroll.
                DailyStreakCard(
                    currentStreak = uiState.currentStreak,
                    hasTrackedToday = uiState.hasTrackedToday,
                    onCardClick = { showStreakDetail = true },
                    onAddTransaction = onAddTransactionForStreak
                )
                AiForecastCard(
                    weeklyData = uiState.weeklyForecastData,
                    monthlyData = uiState.monthlyForecastData,
                    aiInsight = uiState.aiInsightMessage,
                    selectedPeriod = uiState.forecastPeriod,
                    onPeriodChange = { onEvent(DashboardEvent.ToggleForecastPeriod(it)) }
                )
                RecentTransactionsCard(
                    transactions = uiState.recentTransactions,
                    onViewAllClick = onNavigateToTransactionDetails,
                    onTransactionClick = onNavigateToTransactionDetails
                )
            }
        }
    }

    if (showStreakPopup) {
        StreakCelebrationDialog(
            streak = uiState.currentStreak,
            onDismiss = { showStreakPopup = false }
        )
    }

    if (showStreakDetail) {
        StreakDetailSheet(
            currentStreak = uiState.currentStreak,
            bestStreak = uiState.bestStreak,
            hasTrackedToday = uiState.hasTrackedToday,
            onDismiss = { showStreakDetail = false }
        )
    }
}



// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
fun DashboardHeader(
    userName: String,
    totalBalance: Double,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    isBalanceVisible: Boolean,
    onToggleVisibility: () -> Unit
) {
    val dimens = LocalResponsiveDimens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))))
            .statusBarsPadding()
            .padding(horizontal = dimens.screenHorizontalPadding)
            .padding(top = 16.dp, bottom = 24.dp)
    ) {
        Text("${getTimeGreeting()}, ${userName.firstName()} 👋", style = MaterialTheme.typography.titleLarge,
            color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text("Here's your financial overview this month", style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f))
        Spacer(Modifier.height(16.dp))
        BalanceCard(totalBalance, monthlyIncome, monthlyExpenses, isBalanceVisible, onToggleVisibility)
    }
}

// ─── Balance Card ─────────────────────────────────────────────────────────────

@Composable
private fun BalanceCard(balance: Double, income: Double, expense: Double, isVisible: Boolean, onToggleVisibility: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            // Label + eye icon on same row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Balance (All Accounts)",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f))
                // Static hide/show button on the right
                IconButton(onClick = onToggleVisibility, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (isVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = "Toggle Balance",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            // Balance amount (full width, no row needed)
            Text(
                text = if (isVisible) formatCurrency(balance) else "••••••••",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                BalanceStat("Income", formatCurrency(income), Color(0xFF4ADE80), Icons.Default.ArrowUpward, Modifier.weight(1f))
                BalanceStat("Expenses", formatCurrency(expense), Color(0xFFF87171), Icons.Default.ArrowDownward, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BalanceStat(label: String, amount: String, tint: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(tint.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
            Text(amount, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Segmented Control ────────────────────────────────────────────────────────

@Composable
private fun SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(3.dp)) {
            options.forEachIndexed { index, label ->
                val sel = index == selectedIndex
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(if (sel) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ─── AI Forecast Card ────────────────────────────────────────────────────────

@Composable
fun AiForecastCard(
    weeklyData: List<Float>,
    monthlyData: List<Float>,
    aiInsight: String,
    selectedPeriod: ForecastPeriod,
    onPeriodChange: (String) -> Unit
) {
    val isWeekly = selectedPeriod == ForecastPeriod.WEEKLY
    val data = if (isWeekly) weeklyData else monthlyData
    val labels = if (isWeekly) listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
    else listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    val barColor = MaterialTheme.colorScheme.primary
    val barBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Spending Forecast", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("AI-powered analysis", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SegmentedControl(listOf("Weekly","Monthly"), if (isWeekly) 0 else 1) {
                    onPeriodChange(if (it == 0) "week" else "month")
                }
            }
            Spacer(Modifier.height(16.dp))
            if (data.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BarChart, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No data yet", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Start adding transactions to see insights.", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            } else {
                val maxValue = data.maxOrNull()?.takeIf { it > 0f } ?: 1f
                Row(modifier = Modifier.fillMaxWidth().height(108.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                    data.forEachIndexed { i, value ->
                        val fraction = (value / maxValue).coerceIn(0f, 1f)
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            Canvas(Modifier.fillMaxWidth().height(88.dp)) {
                                val fh = size.height * fraction
                                drawRoundRect(barBg, Offset.Zero, Size(size.width, size.height), CornerRadius(6f))
                                drawRoundRect(barColor, Offset(0f, size.height - fh), Size(size.width, fh), CornerRadius(6f))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(labels.getOrNull(i) ?: "", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1)
                        }
                    }
                }
            }
            if (aiInsight.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
                    Text(aiInsight, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(10.dp))
                }
            }
            // Extra bottom space so card doesn't feel flush
            Spacer(Modifier.height(4.dp))
        }
    }
}

// ─── Recent Transactions Card ─────────────────────────────────────────────────

@Composable
fun RecentTransactionsCard(
    transactions: List<TransactionItem>,
    onViewAllClick: () -> Unit,
    onTransactionClick: () -> Unit = {}
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Latest financial activity", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onViewAllClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("View All", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                    Text("No recent transactions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    transactions.take(3).forEach { TransactionItemRow(it, onTransactionClick) }
                }
            }
        }
    }
}

@Composable
private fun TransactionItemRow(transaction: TransactionItem, onClick: () -> Unit) {
    val icon  = CategoryUtils.getIconForCategoryName(transaction.category)
    val color = CategoryUtils.getColorForCategoryName(transaction.category)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ikon kategori (kiri)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))

        // Judul + Kategori (tengah, weight=1f agar mengisi ruang tersisa)
        Column(Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = transaction.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))

        // ISSUE 4 FIX: Amount (kanan-atas) + Timestamp (kanan-bawah)
        // Column dengan horizontalAlignment=End memastikan keduanya rata kanan.
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (transaction.isIncome) "+" else "") +
                        formatCurrency(kotlin.math.abs(transaction.amount)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (transaction.isIncome) Color(0xFF10B981)
                        else MaterialTheme.colorScheme.onSurface
            )
            // ISSUE 3+4: Timestamp sekarang berisi "2h ago" / "Just now" (bukan angka raw)
            // dan ditampilkan di bawah amount, rata kanan
            Text(
                text = transaction.time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}


// ─── Daily Streak Card ────────────────────────────────────────────────────────

@Composable
fun DailyStreakCard(
    currentStreak: Int,
    hasTrackedToday: Boolean,
    onCardClick: () -> Unit = {},
    onAddTransaction: () -> Unit
) {
    val milestone = when {
        currentStreak >= 30 -> "🏆 30-Day Champion!"
        currentStreak >= 10 -> "⭐ 10-Day Streak Pro!"
        currentStreak >= 5  -> "🔥 5-Day Streak!"
        else -> null
    }

    // Infinite pulse animation for the fire bubble
    val infiniteTransition = rememberInfiniteTransition(label = "fire_pulse")
    val fireScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fireScale"
    )
    // Fire glow alpha
    val fireAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fireAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Daily Streak", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Tap to see your track record", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Animated fire bubble
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .scale(fireScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF6B35).copy(alpha = fireAlpha),
                                    Color(0xFFFF3D00).copy(alpha = fireAlpha * 0.8f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = streakFireEmoji(currentStreak),
                            fontSize = when {
                                currentStreak >= 30 -> 10.sp
                                currentStreak >= 10 -> 12.sp
                                else -> 18.sp
                            }
                        )
                        Text(
                            "$currentStreak",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 7-day dots
            val activeDays = minOf(currentStreak, 7).let {
                if (hasTrackedToday) it.coerceAtLeast(1) else it
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("M","T","W","T","F","S","S").forEachIndexed { index, label ->
                    val active = index < activeDays
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(34.dp).clip(CircleShape)
                                .background(if (active) Color(0xFFFF6B35) else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (active) Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            else Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
                            color = if (active) Color(0xFFFF6B35) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Status banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (hasTrackedToday) Color(0xFF10B981).copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (hasTrackedToday) "✅" else "⚠️", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (hasTrackedToday) "Great job! You've tracked today 🎉"
                        else "Log a transaction to keep your streak going!",
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                        color = if (hasTrackedToday) Color(0xFF065F46) else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (milestone != null) {
                Spacer(Modifier.height(8.dp))
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Color(0xFFFFF3CD)) {
                    Text(milestone, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                        color = Color(0xFF7D5A00), modifier = Modifier.padding(10.dp), textAlign = TextAlign.Center)
                }
            }

            if (!hasTrackedToday) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onAddTransaction,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35))
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Log Transaction Today", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


// ─── Streak Celebration Popup ─────────────────────────────────────────────────

@Composable
fun StreakCelebrationDialog(streak: Int, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
            Column(
                Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "popup_fire")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(tween(600, easing = EaseInOutSine), RepeatMode.Reverse),
                    label = "scale"
                )
                Text(streakFireEmoji(streak), fontSize = 56.sp, modifier = Modifier.scale(scale))
                Text("Streak updated!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(
                    "You're on a $streak-day streak.\nKeep it up tomorrow!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35))
                ) {
                    Text("Awesome! 🎉", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Streak Detail Sheet ──────────────────────────────────────────────────────

@Composable
fun StreakDetailSheet(currentStreak: Int, bestStreak: Int, hasTrackedToday: Boolean, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Track Record", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Last 30 days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                // Streak summary — 3 stat cards yang konsisten
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StreakStatCard(
                        icon       = "🔥",
                        value      = "$currentStreak",
                        unit       = "days",
                        label      = "Current",
                        modifier   = Modifier.weight(1f)
                    )
                    StreakStatCard(
                        icon       = "📅",
                        value      = if (hasTrackedToday) "✓" else "✗",
                        unit       = if (hasTrackedToday) "Done" else "Missed",
                        label      = "Today",
                        highlight  = hasTrackedToday,
                        modifier   = Modifier.weight(1f)
                    )
                    StreakStatCard(
                        icon       = "🏆",
                        value      = "$bestStreak",
                        unit       = "days",
                        label      = "Best",
                        modifier   = Modifier.weight(1f)
                    )
                }

                // Day labels row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("M","T","W","T","F","S","S").forEach { day ->
                        Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold)
                    }
                }

                // Build real calendar grid aligned to weekdays
                // today = column index for current day of week (Mon=0..Sun=6)
                val today = Calendar.getInstance()
                val todayDayOfWeek = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0..Sun=6
                val todayDate = today.get(Calendar.DAY_OF_MONTH)
                val todayMonth = today.get(Calendar.MONTH)

                // We show 5 rows × 7 cols, ending today
                // Calculate the date for cell [0,0]
                val totalCells = 5 * 7  // 35 cells
                val startCal = today.clone() as Calendar
                startCal.add(Calendar.DAY_OF_YEAR, -(totalCells - 1) + (0 - ((todayDayOfWeek + (totalCells - 1)) % 7 + 7) % 7))
                // Simpler approach: start from (today - 34 days), then pad start
                val gridStartCal = today.clone() as Calendar
                gridStartCal.add(Calendar.DAY_OF_YEAR, -(4 * 7 + todayDayOfWeek))

                val primaryColor = MaterialTheme.colorScheme.primary
                val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
                val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (week in 0 until 5) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (col in 0 until 7) {
                                val cellCal = gridStartCal.clone() as Calendar
                                cellCal.add(Calendar.DAY_OF_YEAR, week * 7 + col)
                                val cellDate = cellCal.get(Calendar.DAY_OF_MONTH)
                                val daysFromToday = ((today.timeInMillis - cellCal.timeInMillis) / 86400000L).toInt()
                                val isToday = daysFromToday == 0
                                val isFuture = daysFromToday < 0
                                val tracked = when {
                                    isFuture -> false
                                    isToday -> hasTrackedToday
                                    daysFromToday in 1..currentStreak -> true
                                    else -> false
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                tracked -> Color(0xFFFF6B35)
                                                isFuture -> Color.Transparent
                                                else -> surfaceVariant
                                            }
                                        )
                                        .then(
                                            if (isToday) Modifier.border(
                                                width = 2.dp,
                                                color = Color(0xFFFF6B35),
                                                shape = CircleShape
                                            ) else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!isFuture) {
                                        Text(
                                            text = "$cellDate",
                                            fontSize = 9.sp,
                                            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
                                            color = when {
                                                tracked -> Color.White
                                                isToday -> Color(0xFFFF6B35)
                                                else -> onSurfaceVariant.copy(alpha = 0.6f)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Legend
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendItem(Color(0xFFFF6B35).copy(alpha = 0.7f), "Tracked")
                    LegendItem(MaterialTheme.colorScheme.surfaceVariant, "Missed")
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35))
                ) {
                    Text("Got it!", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StreakStatCard(
    icon: String,
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(14.dp),
        color    = if (highlight)
                       Color(0xFF10B981).copy(alpha = 0.1f)
                   else
                       Color(0xFFFF6B35).copy(alpha = 0.07f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Icon
            Text(
                text     = icon,
                fontSize = 20.sp
            )
            // Value — paling menonjol
            Text(
                text       = value,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color      = if (highlight) Color(0xFF065F46)
                             else Color(0xFFFF6B35)
            )
            // Unit — satu tingkat di bawah value
            Text(
                text  = unit,
                style = MaterialTheme.typography.labelSmall,
                color = if (highlight) Color(0xFF065F46).copy(alpha = 0.8f)
                        else Color(0xFFFF6B35).copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
            // Label — paling kecil, secondary
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}