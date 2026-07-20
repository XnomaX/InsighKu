package com.example.insightku.feature.home.presentation.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.ui.theme.*
import com.example.insightku.core.utils.CurrencyUtils
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.feature.home.presentation.PremiumFlameIcon
import com.example.insightku.feature.home.presentation.formatCurrencyShort

@Composable
private fun getTimeGreetingRes(): Int {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> R.string.dashboard_greeting_morning
        hour < 17 -> R.string.dashboard_greeting_afternoon
        else -> R.string.dashboard_greeting_evening
    }
}



@Composable
fun DashboardHeader(
    userName: String,
    monthlySavings: Double,
    currentStreak: Int,
    isBalanceVisible: Boolean,
    isLoading: Boolean,
    onToggleVisibility: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
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
            text = DateFormatter.formatMonthYear(System.currentTimeMillis()),
            style = MaterialTheme.typography.labelSmall,
            color = AppPalette.textMuted,
            letterSpacing = 0.5.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${stringResource(getTimeGreetingRes())}, ${userName.firstName()}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textPrimary
                )
                Text(
                    text = stringResource(getContextualSubtitleRes(monthlySavings, currentStreak)),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
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
                            text = stringResource(R.string.dashboard_syncing),
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
                    color = AppPalette.card,
                    border = BorderStroke(1.dp, AppPalette.cardBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isBalanceVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(R.string.dashboard_toggle_balance),
                            tint = AppPalette.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                // Notification bell
                Surface(
                    modifier = Modifier.size(38.dp).clip(CircleShape),
                    shape = CircleShape,
                    color = AppPalette.card,
                    border = BorderStroke(1.dp, AppPalette.cardBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = stringResource(R.string.dashboard_notifications),
                            tint = AppPalette.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                // Settings
                Surface(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { onNavigateToSettings() },
                    shape = CircleShape,
                    color = AppPalette.card,
                    border = BorderStroke(1.dp, AppPalette.cardBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.dashboard_settings),
                            tint = AppPalette.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- Sticky Finance Status Bar ------------------------------------------------

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
        color           = AppPalette.card,
        shadowElevation = 4.dp,
        border          = BorderStroke(1.dp, AppPalette.cardBorder)
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
            Box(Modifier.size(4.dp).clip(CircleShape).background(AppPalette.cardBorder))
            StickyStatItem(Icons.Default.ArrowDownward, ExpenseRed, formatCurrencyShort(monthlyExpenses))
            Box(Modifier.size(4.dp).clip(CircleShape).background(AppPalette.cardBorder))
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
                AnimatedContent(
                    targetState = displayedStreak,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                    },
                    label = "streak_count"
                ) { streak ->
                    Text(
                        text       = if (streak > 0) stringResource(R.string.dashboard_streak_days, streak) else "—",
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (hasTrackedToday) NavPurple else AppPalette.textMuted
                    )
                }
            }
        }
    }
}

@Composable
internal fun StickyStatItem(
    icon: ImageVector,
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
            color      = AppPalette.textPrimary
        )
    }
}
