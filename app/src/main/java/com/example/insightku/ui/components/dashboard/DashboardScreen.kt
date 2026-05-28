package com.example.insightku.ui.components.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.BorderStroke
import com.example.insightku.data.model.Installment
import com.example.insightku.data.model.RecurringBudget
import com.example.insightku.ui.theme.Dimens
import com.example.insightku.ui.theme.GradientEnd
import com.example.insightku.ui.theme.GradientStart
import com.example.insightku.ui.theme.IncomeGreen
import com.example.insightku.ui.theme.ExpenseRed
import com.example.insightku.ui.theme.WarningYellow
import com.example.insightku.ui.theme.PurpleViolet
import com.example.insightku.ui.theme.formatCurrency
import com.example.insightku.ui.dialogs.CategoryIconResolver
import com.example.insightku.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun getTimeGreeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 0..11  -> "Good morning"
    in 12..16 -> "Good afternoon"
    else      -> "Good evening"
}

private fun String.firstName(): String = split(" ").firstOrNull()?.ifBlank { this } ?: this

private fun getContextualSubtitle(savings: Double, streak: Int): String = when {
    streak >= 30  -> "Remarkable consistency. Keep the momentum."
    streak >= 7   -> "You're building healthy spending habits."
    savings > 0   -> "Your savings are staying on track."
    else          -> "Small mindful steps create strong finances."
}

// NavPurple — matches bottom nav primary action color exactly
private val NavPurple = Color(0xFF7C4DFF)

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
    val density = LocalDensity.current
    var showStreakPopup by remember { mutableStateOf(false) }
    var showStreakDetail by remember { mutableStateOf(false) }

    // Sticky bar threshold — show after scrolling ~260dp past top
    val stickyThresholdPx = with(density) { 260.dp.toPx() }
    var scrollOffsetPx by remember { mutableFloatStateOf(0f) }
    val showStickyBar by remember { derivedStateOf { scrollOffsetPx < -stickyThresholdPx } }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                scrollOffsetPx = (scrollOffsetPx + available.y).coerceAtMost(0f)
                return Offset.Zero
            }
        }
    }

    // Streak popup trigger
    val prevTracked = remember { mutableStateOf(uiState.hasTrackedToday) }
    LaunchedEffect(uiState.hasTrackedToday) {
        if (uiState.hasTrackedToday && !prevTracked.value) showStreakPopup = true
        prevTracked.value = uiState.hasTrackedToday
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF9FE))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(bottom = Dimens.ContentBottomPadding)
        ) {
            // 1. Greeting header
            item {
                DashboardHeader(
                    userName           = uiState.userName,
                    monthlySavings     = uiState.monthlySavings,
                    currentStreak      = uiState.currentStreak,
                    isBalanceVisible   = uiState.isBalanceVisible,
                    isLoading          = uiState.isLoading,
                    onToggleVisibility = { onEvent(DashboardEvent.ToggleBalanceVisibility) }
                )
            }
            // 2. Hero balance card
            item {
                HeroBalanceCard(
                    totalBalance     = uiState.totalBalance,
                    monthlyIncome    = uiState.monthlyIncome,
                    monthlyExpenses  = uiState.monthlyExpenses,
                    monthlySavings   = uiState.monthlySavings,
                    isBalanceVisible = uiState.isBalanceVisible,
                    modifier         = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = 4.dp
                    )
                )
            }
            // 3. Streak card
            item {
                DailyStreakCard(
                    currentStreak    = uiState.currentStreak,
                    hasTrackedToday  = uiState.hasTrackedToday,
                    onCardClick      = { showStreakDetail = true },
                    onAddTransaction = onAddTransactionForStreak,
                    modifier         = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = Dimens.CardSpacing
                    )
                )
            }
            // 4. Insights section
            item {
                InsightsSection(
                    insightMessages = uiState.insightMessages,
                    modifier        = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = Dimens.CardSpacing
                    )
                )
            }
            // 5. AI Forecast card
            item {
                AiForecastCard(
                    weeklyData     = uiState.weeklyForecastData,
                    monthlyData    = uiState.monthlyForecastData,
                    aiInsight      = uiState.aiInsightMessage,
                    selectedPeriod = uiState.forecastPeriod,
                    onPeriodChange = { onEvent(DashboardEvent.ToggleForecastPeriod(it)) },
                    modifier       = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = Dimens.CardSpacing
                    )
                )
            }
            // 6. Upcoming payments
            item {
                UpcomingPaymentsSection(
                    recurringBudgets      = uiState.recurringBudgets,
                    installments          = uiState.installments,
                    onMarkRecurringPaid   = { onEvent(DashboardEvent.MarkRecurringPaid(it)) },
                    onMarkInstallmentPaid = { onEvent(DashboardEvent.MarkInstallmentPaid(it)) },
                    modifier              = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = Dimens.CardSpacing
                    )
                )
            }
            // 7. Recent transactions preview
            item {
                RecentTransactionsPreview(
                    transactions   = uiState.recentTransactions,
                    onViewAllClick = onNavigateToTransactionDetails,
                    modifier       = Modifier.padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical   = Dimens.CardSpacing
                    )
                )
            }
        }

        // Sticky finance status bar — slides in below header on scroll
        AnimatedVisibility(
            visible  = showStickyBar,
            enter    = fadeIn(tween(250)) + slideInVertically(tween(250)) { -it },
            exit     = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            StickyFinanceStatusBar(
                monthlyIncome   = uiState.monthlyIncome,
                monthlyExpenses = uiState.monthlyExpenses,
                currentStreak   = uiState.currentStreak,
                hasTrackedToday = uiState.hasTrackedToday,
                freezeCount     = uiState.freezeCount
            )
        }
    }

    if (showStreakPopup) {
        StreakCelebrationDialog(streak = uiState.currentStreak, onDismiss = { showStreakPopup = false })
    }
    if (showStreakDetail) {
        StreakDetailSheet(
            currentStreak   = uiState.currentStreak,
            bestStreak      = uiState.bestStreak,
            hasTrackedToday = uiState.hasTrackedToday,
            onDismiss       = { showStreakDetail = false }
        )
    }
}



// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
fun DashboardHeader(
    userName: String,
    monthlySavings: Double,
    currentStreak: Int,
    isBalanceVisible: Boolean,
    isLoading: Boolean,
    onToggleVisibility: () -> Unit
) {
    val initial = userName.firstOrNull()?.uppercaseChar() ?: 'U'

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAF9FE))
            .statusBarsPadding()
            .padding(horizontal = Dimens.ScreenHorizontalPadding)
            .padding(top = 20.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF9E9E9E),
            letterSpacing = 0.5.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${getTimeGreeting()}, ${userName.firstName()}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A2E)
                )
                Text(
                    text = getContextualSubtitle(monthlySavings, currentStreak),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                // Visibility toggle
                Surface(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { onToggleVisibility() },
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFECE7F6))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isBalanceVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Toggle Balance",
                            tint = Color(0xFF9E9E9E),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                // Notification bell
                Surface(
                    modifier = Modifier.size(38.dp).clip(CircleShape),
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFECE7F6))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color(0xFF9E9E9E),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                // Avatar
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(GradientStart, GradientEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ─── Hero Balance Card ────────────────────────────────────────────────────────

@Composable
fun HeroBalanceCard(
    totalBalance: Double,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    monthlySavings: Double,
    isBalanceVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier        = modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(Dimens.CardRadiusLarge),
        color           = Color.White,
        tonalElevation  = 0.dp,
        shadowElevation = 4.dp,
        border          = BorderStroke(1.dp, Color(0xFFECE7F6))
    ) {
        Column(
            modifier = Modifier.padding(Dimens.CardInnerPaddingLarge),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Balance label + amount
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Total Balance",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9E9E9E),
                    letterSpacing = 0.5.sp
                )
                AnimatedContent(
                    targetState = isBalanceVisible,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                    label = "balance_visibility"
                ) { visible ->
                    Text(
                        text = if (visible) formatCurrency(totalBalance) else "••••••••",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A2E)
                    )
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFECE7F6))
            )

            // 3-stat row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HeroStatItem(
                    label  = "Income",
                    value  = if (isBalanceVisible) formatCurrencyShort(monthlyIncome) else "••••",
                    icon   = Icons.Default.ArrowUpward,
                    tint   = IncomeGreen,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(Color(0xFFECE7F6))
                        .align(Alignment.CenterVertically)
                )
                HeroStatItem(
                    label  = "Expenses",
                    value  = if (isBalanceVisible) formatCurrencyShort(monthlyExpenses) else "••••",
                    icon   = Icons.Default.ArrowDownward,
                    tint   = ExpenseRed,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(Color(0xFFECE7F6))
                        .align(Alignment.CenterVertically)
                )
                HeroStatItem(
                    label  = "Savings",
                    value  = if (isBalanceVisible) formatCurrencyShort(abs(monthlySavings)) else "••••",
                    icon   = if (monthlySavings >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    tint   = if (monthlySavings >= 0) IncomeGreen else ExpenseRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroStatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(11.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF9E9E9E),
                fontSize = 10.sp
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A1A2E),
            maxLines = 1
        )
    }
}

private fun formatCurrencyShort(amount: Double): String {
    return when {
        amount >= 1_000_000_000 -> "Rp${String.format(Locale.getDefault(), "%.1f", amount / 1_000_000_000)}B"
        amount >= 1_000_000     -> "Rp${String.format(Locale.getDefault(), "%.1f", amount / 1_000_000)}M"
        amount >= 1_000         -> "Rp${String.format(Locale.getDefault(), "%.0f", amount / 1_000)}K"
        else                    -> "Rp${amount.toInt()}"
    }
}

// ─── Sticky Finance Status Bar ────────────────────────────────────────────────

@Composable
fun StickyFinanceStatusBar(
    monthlyIncome: Double,
    monthlyExpenses: Double,
    currentStreak: Int,
    hasTrackedToday: Boolean = false,
    freezeCount: Int = 0
) {
    val displayedStreak = if (hasTrackedToday) currentStreak else 0
    val isFrozen = freezeCount > 0 && !hasTrackedToday

    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = Color.White.copy(alpha = 0.96f),
        shadowElevation = 4.dp,
        border          = BorderStroke(1.dp, Color(0xFFECE7F6))
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Dimens.ScreenHorizontalPadding, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            StickyStatItem(Icons.Default.ArrowUpward, IncomeGreen, formatCurrencyShort(monthlyIncome))
            Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFECE7F6)))
            StickyStatItem(Icons.Default.ArrowDownward, ExpenseRed, formatCurrencyShort(monthlyExpenses))
            Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFECE7F6)))
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PremiumFlameIcon(
                    active  = hasTrackedToday,
                    streak  = displayedStreak,
                    size    = 28.dp,
                    frozen  = isFrozen
                )
                androidx.compose.animation.AnimatedContent(
                    targetState = displayedStreak,
                    transitionSpec = {
                        androidx.compose.animation.fadeIn(
                            androidx.compose.animation.core.tween(300)
                        ) togetherWith androidx.compose.animation.fadeOut(
                            androidx.compose.animation.core.tween(200)
                        )
                    },
                    label = "streak_count"
                ) { streak ->
                    Text(
                        text       = if (streak > 0) "$streak days" else "—",
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (hasTrackedToday) Color(0xFF7C3AED) else Color(0xFF9E9E9E)
                    )
                }
            }
        }
    }
}

@Composable
private fun StickyStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    value: String
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(11.dp))
        Text(
            value,
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color      = Color(0xFF1A1A2E)
        )
    }
}

// ─── Segmented Control ────────────────────────────────────────────────────────

@Composable
private fun SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = Color(0xFFF3EEFF),
        border = BorderStroke(1.dp, Color(0xFFECE7F6))
    ) {
        Row(Modifier.padding(3.dp)) {
            options.forEachIndexed { index, label ->
                val sel = index == selectedIndex
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (sel) NavPurple else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        color = if (sel) Color.White else Color(0xFF9E9E9E)
                    )
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
    onPeriodChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isWeekly = selectedPeriod == ForecastPeriod.WEEKLY
    val data = if (isWeekly) weeklyData else monthlyData
    val labels = if (isWeekly) listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
    else listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    val barColor = NavPurple
    val barBg = Color(0xFFECE7F6)

    Surface(
        modifier        = modifier.fillMaxWidth().animateContentSize(),
        shape           = RoundedCornerShape(Dimens.CardRadiusLarge),
        color           = Color.White,
        tonalElevation  = 0.dp,
        shadowElevation = 4.dp,
        border          = BorderStroke(1.dp, Color(0xFFECE7F6))
    ) {
        Column(Modifier.padding(Dimens.CardInnerPaddingLarge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Spending Forecast", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
                    Text("AI-powered analysis", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9E9E9E))
                }
                SegmentedControl(listOf("Weekly","Monthly"), if (isWeekly) 0 else 1) {
                    onPeriodChange(if (it == 0) "week" else "month")
                }
            }
            Spacer(Modifier.height(16.dp))
            if (data.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.CardRadius),
                    color = Color(0xFFFAF9FE),
                    border = BorderStroke(1.dp, Color(0xFFECE7F6))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(52.dp).clip(CircleShape).background(NavPurple.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.BarChart, null, tint = NavPurple.copy(alpha = 0.45f), modifier = Modifier.size(26.dp))
                        }
                        Text("No data yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                        Text("Start adding transactions to see insights.", style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9E9E9E), textAlign = TextAlign.Center)
                    }
                }
            } else {
                val maxValue = data.maxOrNull()?.takeIf { it > 0f } ?: 1f
                Row(
                    modifier = Modifier.fillMaxWidth().height(108.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    data.forEachIndexed { i, value ->
                        val fraction = (value / maxValue).coerceIn(0f, 1f)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Canvas(Modifier.fillMaxWidth().height(88.dp)) {
                                val fh = size.height * fraction
                                drawRoundRect(barBg, Offset.Zero, Size(size.width, size.height), CornerRadius(8f))
                                drawRoundRect(barColor, Offset(0f, size.height - fh), Size(size.width, fh), CornerRadius(8f))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                labels.getOrNull(i) ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = Color(0xFF9E9E9E),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            if (aiInsight.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.CardRadius),
                    color = NavPurple.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, NavPurple.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier.size(24.dp).clip(CircleShape).background(NavPurple.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✦", fontSize = 10.sp, color = NavPurple)
                        }
                        Text(
                            aiInsight,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1A1A2E),
                            modifier = Modifier.weight(1f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

// ─── Insights Section ─────────────────────────────────────────────────────────

@Composable
fun InsightsSection(
    insightMessages: List<String>,
    modifier: Modifier = Modifier
) {
    if (insightMessages.isEmpty()) return

    val insightConfigs = listOf(
        Triple(Color(0xFFF0FFF4), IncomeGreen,   Icons.AutoMirrored.Filled.TrendingUp),
        Triple(Color(0xFFFFF8F0), WarningYellow,  Icons.AutoMirrored.Filled.TrendingDown),
        Triple(Color(0xFFF3EEFF), PurpleViolet,   Icons.Default.CheckCircle)
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E)
                )
                Text(
                    "Your financial pulse this month",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
        insightMessages.forEachIndexed { i, msg ->
            val (bg, tint, icon) = insightConfigs.getOrElse(i) { insightConfigs.last() }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(Dimens.CardRadius),
                color    = bg,
                border   = BorderStroke(1.dp, tint.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.CardInnerPadding, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(tint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text     = msg,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = Color(0xFF1A1A2E),
                        modifier = Modifier.weight(1f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// ─── Upcoming Payments Section ────────────────────────────────────────────────

@Composable
fun UpcomingPaymentsSection(
    recurringBudgets: List<RecurringBudget>,
    installments: List<Installment>,
    onMarkRecurringPaid: (RecurringBudget) -> Unit,
    onMarkInstallmentPaid: (Installment) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val fourteenDays = 14L * 24 * 60 * 60 * 1000
    val upcomingRecurring = recurringBudgets.filter { it.isActive && it.nextDue <= now + fourteenDays }.sortedBy { it.nextDue }
    val upcomingInstallments = installments.filter { it.isActive && !it.isCompleted }.sortedBy { it.nextDueDate }

    val isEmpty = upcomingRecurring.isEmpty() && upcomingInstallments.isEmpty()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Upcoming",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E)
                )
                Text(
                    if (isEmpty) "No payments due soon"
                    else "${upcomingRecurring.size + upcomingInstallments.size} payments due soon",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E)
                )
            }
        }

        if (isEmpty) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(Dimens.CardRadiusLarge),
                color    = Color.White,
                border   = BorderStroke(1.dp, Color(0xFFECE7F6))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(CircleShape).background(NavPurple.copy(alpha = 0.07f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = NavPurple.copy(alpha = 0.45f), modifier = Modifier.size(26.dp))
                    }
                    Text("You're all caught up.", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                    Text("No recurring or installment payments due in the next 14 days.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9E9E9E), textAlign = TextAlign.Center)
                }
            }
        } else {
            upcomingRecurring.forEach { budget ->
                UpcomingRecurringRow(budget = budget, now = now, onMarkPaid = { onMarkRecurringPaid(budget) })
            }
            upcomingInstallments.take(3).forEach { inst ->
                UpcomingInstallmentRow(installment = inst, now = now, onMarkPaid = { onMarkInstallmentPaid(inst) })
            }
        }
    }
}

@Composable
private fun UpcomingRecurringRow(
    budget: RecurringBudget,
    now: Long,
    onMarkPaid: () -> Unit
) {
    val daysUntil = ((budget.nextDue - now) / 86400000L).toInt().coerceAtLeast(0)
    val dueBadgeColor = when {
        daysUntil <= 2 -> ExpenseRed
        daysUntil <= 7 -> WarningYellow
        else           -> IncomeGreen
    }
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(Dimens.CardRadius),
        color           = Color.White,
        tonalElevation  = 0.dp,
        shadowElevation = Dimens.ElevationSmall,
        border          = BorderStroke(1.dp, Color(0xFFECE7F6))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.CardInnerPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF7C4DFF).copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Repeat, null, tint = Color(0xFF7C4DFF), modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    budget.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E),
                    maxLines = 1
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50.dp), color = dueBadgeColor.copy(alpha = 0.10f)) {
                        Text(
                            if (daysUntil == 0) "Today" else "in ${daysUntil}d",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = dueBadgeColor
                        )
                    }
                    Surface(shape = RoundedCornerShape(50.dp), color = Color(0xFF7C4DFF).copy(alpha = 0.08f)) {
                        Text(
                            "Recurring",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF7C4DFF)
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    formatCurrencyShort(budget.amount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E)
                )
                Surface(
                    shape    = RoundedCornerShape(50.dp),
                    color    = Color.White,
                    border   = BorderStroke(1.dp, IncomeGreen),
                    modifier = Modifier.clickable { onMarkPaid() }
                ) {
                    Text(
                        "Mark Paid",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = IncomeGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingInstallmentRow(
    installment: Installment,
    now: Long,
    onMarkPaid: () -> Unit
) {
    val daysUntil = ((installment.nextDueDate - now) / 86400000L).toInt().coerceAtLeast(0)
    val dueBadgeColor = when {
        daysUntil <= 2 -> ExpenseRed
        daysUntil <= 7 -> WarningYellow
        else           -> IncomeGreen
    }
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(Dimens.CardRadius),
        color           = Color.White,
        tonalElevation  = 0.dp,
        shadowElevation = Dimens.ElevationSmall,
        border          = BorderStroke(1.dp, Color(0xFFECE7F6))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.CardInnerPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF4A90E2).copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CreditCard, null, tint = Color(0xFF4A90E2), modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    installment.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E),
                    maxLines = 1
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50.dp), color = dueBadgeColor.copy(alpha = 0.10f)) {
                        Text(
                            if (daysUntil == 0) "Today" else "in ${daysUntil}d",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = dueBadgeColor
                        )
                    }
                    Surface(shape = RoundedCornerShape(50.dp), color = Color(0xFF4A90E2).copy(alpha = 0.08f)) {
                        Text(
                            "${installment.paidMonths}/${installment.totalMonths} paid",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4A90E2)
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    formatCurrencyShort(installment.monthlyPayment),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E)
                )
                Surface(
                    shape    = RoundedCornerShape(50.dp),
                    color    = Color.White,
                    border   = BorderStroke(1.dp, IncomeGreen),
                    modifier = Modifier.clickable { onMarkPaid() }
                ) {
                    Text(
                        "Mark Paid",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = IncomeGreen
                    )
                }
            }
        }
    }
}

// ─── Recent Transactions Preview ──────────────────────────────────────────────

@Composable
fun RecentTransactionsPreview(
    transactions: List<TransactionItem>,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Recent",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E)
                )
                Text(
                    "Latest financial activity",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E)
                )
            }
            Surface(
                modifier = Modifier.clickable(onClick = onViewAllClick),
                shape    = RoundedCornerShape(50.dp),
                color    = Color.White,
                border   = BorderStroke(1.dp, Color(0xFF7C4DFF))
            ) {
                Text(
                    "View All",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF7C4DFF)
                )
            }
        }
        if (transactions.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(Dimens.CardRadius),
                color    = Color.White,
                border   = BorderStroke(1.dp, Color(0xFFECE7F6))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No recent transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E)
                    )
                }
            }
        } else {
            Surface(
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(Dimens.CardRadiusLarge),
                color           = Color.White,
                tonalElevation  = 0.dp,
                shadowElevation = Dimens.ElevationSmall,
                border          = BorderStroke(1.dp, Color(0xFFECE7F6))
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    transactions.take(5).forEachIndexed { index, tx ->
                        TransactionItemRow(tx, onViewAllClick)
                        if (index < transactions.take(5).lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Dimens.CardInnerPadding)
                                    .height(1.dp)
                                    .background(Color(0xFFECE7F6))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionItemRow(transaction: TransactionItem, onClick: () -> Unit) {
    // Resolve via CategoryIconResolver (same system as TransactionDetailsScreen):
    // 1. Try exact match on stored iconName (e.g. "Food & Drinks")
    // 2. Fuzzy keyword match on iconName
    // 3. Fuzzy keyword match on category string as fallback
    val resolved = CategoryIconResolver.resolve(
        transaction.iconName.ifBlank { transaction.category }
    )
    val icon  = resolved.icon
    val color = resolved.color
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Dimens.CardInnerPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A2E),
                maxLines = 1
            )
            Text(
                text = transaction.category,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9E9E9E)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (transaction.isIncome) "+" else "") + formatCurrencyShort(abs(transaction.amount)),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (transaction.isIncome) IncomeGreen else Color(0xFF1A1A2E)
            )
            Text(
                text = transaction.time,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF9E9E9E)
            )
        }
    }
}

// ─── Streak Tier System ───────────────────────────────────────────────────────

// ─── Daily Streak Card ────────────────────────────────────────────────────────

@Composable
fun DailyStreakCard(
    currentStreak: Int,
    hasTrackedToday: Boolean,
    onCardClick: () -> Unit = {},
    onAddTransaction: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val displayedStreak = if (hasTrackedToday) currentStreak else 0
    val config = flameConfig(displayedStreak)
    val weekDays = listOf("M", "T", "W", "T", "F", "S", "S")
    val activeDays = minOf(currentStreak, 7).let { if (hasTrackedToday) it.coerceAtLeast(1) else it }

    val statusLabel = when {
        !hasTrackedToday && currentStreak > 0 -> "Log today to restore your streak"
        !hasTrackedToday                      -> "Start your streak today"
        else                                  -> config.statusCopy
    }

    val motivationalText = when {
        !hasTrackedToday && currentStreak > 0 ->
            "Your $currentStreak-day streak is waiting. Log a transaction to keep it alive."
        currentStreak == 0  -> "Log your first transaction to ignite your streak."
        currentStreak < 3   -> "Every habit starts with a single step. Keep going."
        currentStreak < 7   -> "Your momentum is building. Don't break the chain."
        currentStreak < 14  -> "One week of discipline. Your future self thanks you."
        currentStreak < 30  -> "Two weeks strong — this is becoming who you are."
        currentStreak < 100 -> "Remarkable consistency. Financial mastery in motion."
        else                -> "100 days. You've built something rare and lasting."
    }

    Surface(
        modifier        = modifier.fillMaxWidth().clickable { onCardClick() },
        shape           = RoundedCornerShape(Dimens.CardRadiusLarge),
        color           = Color.White,
        tonalElevation  = 0.dp,
        shadowElevation = 4.dp,
        border          = BorderStroke(1.dp, Color(0xFFECE7F6))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.CardInnerPaddingLarge, vertical = Dimens.CardInnerPaddingLarge),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Hero row ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status pill
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = NavPurple.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = statusLabel,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp,
                            color = NavPurple
                        )
                    }

                    // Streak count — show actual streak dimmed when not tracked today
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.animation.AnimatedContent(
                            targetState = displayedStreak,
                            transitionSpec = {
                                (androidx.compose.animation.slideInVertically(
                                    androidx.compose.animation.core.tween(350)
                                ) { it / 2 } + androidx.compose.animation.fadeIn(
                                    androidx.compose.animation.core.tween(350)
                                )) togetherWith (androidx.compose.animation.slideOutVertically(
                                    androidx.compose.animation.core.tween(250)
                                ) { -it / 2 } + androidx.compose.animation.fadeOut(
                                    androidx.compose.animation.core.tween(250)
                                ))
                            },
                            label = "streak_number"
                        ) { displayed ->
                            Text(
                                text = if (displayed > 0) "$displayed" else if (currentStreak > 0) "$currentStreak" else "0",
                                fontSize = 56.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 58.sp,
                                color = when {
                                    hasTrackedToday && currentStreak > 0 -> config.flamePrimary
                                    !hasTrackedToday && currentStreak > 0 -> Color(0xFF9E9E9E)
                                    else -> Color(0xFF9E9E9E)
                                }
                            )
                        }
                        Column(modifier = Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text("day", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF9E9E9E))
                            Text("streak", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9E9E9E))
                        }
                    }

                    Text(
                        text = motivationalText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E),
                        lineHeight = 19.sp
                    )
                }

                // Lottie flame — uses displayedStreak so it goes dormant until today's log
                PremiumFlameIcon(active = hasTrackedToday, size = 96.dp, streak = displayedStreak)
            }

            // ── Weekly rhythm track ───────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "This week",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9E9E9E),
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    weekDays.forEachIndexed { index, label ->
                        val active = index < activeDays
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) NavPurple else Color(0xFFECE7F6)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (active)
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                else
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9E9E9E), fontSize = 10.sp)
                            }
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (active) NavPurple else Color(0xFF9E9E9E)
                            )
                        }
                    }
                }
            }

            // ── Today status / CTA ────────────────────────────────────────────
            if (hasTrackedToday) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.CardRadius),
                    color = IncomeGreen.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.20f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                        Column {
                            Text("Habit intact.", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                            Text("You've tracked today — streak is safe.", style = MaterialTheme.typography.labelSmall, color = Color(0xFF065F46).copy(alpha = 0.65f))
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onAddTransaction() },
                    shape = RoundedCornerShape(Dimens.ButtonRadius),
                    color = NavPurple
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (currentStreak == 0) "Start Your Streak" else "Log Today's Transaction",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ─── Streak Celebration Dialog ────────────────────────────────────────────────

@Composable
fun StreakCelebrationDialog(streak: Int, onDismiss: () -> Unit) {
    val config = flameConfig(streak)

    val headline = when {
        streak >= 100 -> "Legendary."
        streak >= 30  -> "On fire."
        streak >= 14  -> "Two weeks strong."
        streak >= 7   -> "One week done."
        streak >= 3   -> "Habit forming."
        else          -> "Streak started."
    }
    val subtext = when {
        streak >= 100 -> "100 days of discipline. This is who you are now."
        streak >= 30  -> "A month of consistency. Financial mastery in motion."
        streak >= 14  -> "Two weeks of daily tracking. The habit is real."
        streak >= 7   -> "Seven days straight. Momentum is building."
        streak >= 3   -> "Three days in. The chain is forming."
        else          -> "Day one done. Every streak starts here."
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape           = RoundedCornerShape(Dimens.BottomSheetRadius),
            color           = Color.White,
            tonalElevation  = 0.dp,
            shadowElevation = 8.dp,
            border          = BorderStroke(1.dp, Color(0xFFECE7F6))
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                PremiumFlameIcon(active = streak > 0, size = 100.dp, streak = streak)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A1A2E),
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = NavPurple.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "$streak-day streak",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NavPurple
                        )
                    }
                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9E9E9E),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onDismiss() },
                    shape = RoundedCornerShape(Dimens.ButtonRadius),
                    color = NavPurple
                ) {
                    Box(modifier = Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                        Text("Keep going", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

// ─── Streak Detail Sheet ──────────────────────────────────────────────────────

@Composable
fun StreakDetailSheet(
    currentStreak: Int,
    bestStreak: Int,
    hasTrackedToday: Boolean,
    onDismiss: () -> Unit
) {
    val config = flameConfig(currentStreak)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape           = RoundedCornerShape(Dimens.BottomSheetRadius),
            color           = Color.White,
            tonalElevation  = 0.dp,
            shadowElevation = 8.dp,
            border          = BorderStroke(1.dp, Color(0xFFECE7F6)),
            modifier        = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Header ────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Habit Journey", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
                        Text("Your momentum over time", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9E9E9E))
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFECE7F6))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(15.dp), tint = Color(0xFF9E9E9E))
                    }
                }

                // ── Flame + stats ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PremiumFlameIcon(active = currentStreak > 0, size = 76.dp, streak = if (hasTrackedToday) currentStreak else 0)
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(Dimens.CardRadius),
                            color = NavPurple.copy(alpha = 0.06f),
                            border = BorderStroke(1.dp, Color(0xFFECE7F6))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("$currentStreak", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp,
                                    color = if (currentStreak > 0) config.flamePrimary else Color(0xFF9E9E9E))
                                Text(config.statusCopy, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9E9E9E), lineHeight = 14.sp)
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(Dimens.CardRadius),
                            color = Color(0xFFFAF9FE),
                            border = BorderStroke(1.dp, Color(0xFFECE7F6))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("$bestStreak", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp, color = NavPurple.copy(alpha = 0.75f))
                                Text("Personal Best", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9E9E9E), lineHeight = 14.sp)
                            }
                        }
                    }
                }

                // Today status
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.CardRadius),
                    color = if (hasTrackedToday) IncomeGreen.copy(alpha = 0.08f) else ExpenseRed.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, if (hasTrackedToday) IncomeGreen.copy(alpha = 0.20f) else ExpenseRed.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (hasTrackedToday) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            null,
                            tint = if (hasTrackedToday) IncomeGreen else ExpenseRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            if (hasTrackedToday) "Today's habit complete — streak is safe."
                            else "Log a transaction today to keep your streak.",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasTrackedToday) Color(0xFF065F46) else ExpenseRed
                        )
                    }
                }

                // ── Momentum calendar ─────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Streak history", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF9E9E9E), letterSpacing = 0.5.sp)

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimens.CardRadius),
                        color = Color(0xFFFAF9FE),
                        border = BorderStroke(1.dp, Color(0xFFECE7F6))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Day-of-week header — sticky
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("M","T","W","T","F","S","S").forEach { day ->
                                    Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp, color = Color(0xFF9E9E9E))
                                }
                            }

                            val today = Calendar.getInstance()
                            val todayDayOfWeek = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7
                            // Show 16 weeks (4 months) of history
                            val totalWeeks = 16
                            val gridStartCal = today.clone() as Calendar
                            gridStartCal.add(Calendar.DAY_OF_YEAR, -((totalWeeks - 1) * 7 + todayDayOfWeek))

                            // Scrollable column — max height shows ~5 weeks, scroll for rest
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .verticalScroll(rememberScrollState(Int.MAX_VALUE)),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (week in 0 until totalWeeks) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        for (col in 0 until 7) {
                                            val cellCal = gridStartCal.clone() as Calendar
                                            cellCal.add(Calendar.DAY_OF_YEAR, week * 7 + col)
                                            val cellDate = cellCal.get(Calendar.DAY_OF_MONTH)
                                            val daysFromToday = ((today.timeInMillis - cellCal.timeInMillis) / 86400000L).toInt()
                                            val isToday  = daysFromToday == 0
                                            val isFuture = daysFromToday < 0
                                            val tracked  = when {
                                                isFuture -> false
                                                isToday  -> hasTrackedToday
                                                daysFromToday in 1..currentStreak -> true
                                                else     -> false
                                            }
                                            // Month label on first day of month
                                            val isFirstOfMonth = cellCal.get(Calendar.DAY_OF_MONTH) == 1

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        when {
                                                            tracked  -> NavPurple.copy(alpha = 0.85f)
                                                            isFuture -> Color.Transparent
                                                            else     -> Color(0xFFECE7F6)
                                                        }
                                                    )
                                                    .then(
                                                        if (isToday) Modifier.border(1.5.dp, NavPurple, RoundedCornerShape(6.dp))
                                                        else Modifier
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!isFuture) {
                                                    Text(
                                                        if (isFirstOfMonth)
                                                            cellCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())?.take(1) ?: "$cellDate"
                                                        else "$cellDate",
                                                        fontSize = 8.sp,
                                                        fontWeight = if (isToday || isFirstOfMonth) FontWeight.ExtraBold else FontWeight.Medium,
                                                        color = when {
                                                            tracked         -> Color.White
                                                            isToday         -> NavPurple
                                                            isFirstOfMonth  -> NavPurple.copy(alpha = 0.6f)
                                                            else            -> Color(0xFF9E9E9E)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        LegendDot(NavPurple, "Tracked")
                        LegendDot(Color(0xFFECE7F6), "Missed")
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onDismiss() },
                    shape = RoundedCornerShape(Dimens.ButtonRadius),
                    color = NavPurple
                ) {
                    Box(modifier = Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                        Text("Got it", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9E9E9E))
    }
}




