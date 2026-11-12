package com.example.insightku.feature.home.presentation.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.IncomeGreen
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.ui.theme.PurpleViolet
import com.example.insightku.feature.home.presentation.PremiumFlameIcon
import com.example.insightku.feature.home.presentation.formatCurrencyShort

@Composable
fun DashboardHeader(
    isBalanceVisible: Boolean,
    isLoading: Boolean,
    onToggleVisibility: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    // Null in release builds: Route.NOTIFICATION_DEBUG only exists in the debug nav graph,
    // so the bell is hidden entirely rather than navigating to a missing destination.
    onNavigateToNotifications: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPalette.background)
            .statusBarsPadding()
            .padding(horizontal = Dimens.ScreenHorizontalPadding)
            .padding(top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Month chip — the only date context kept; replaces the stacked label + greeting +
        // affirmation subtitle that read as boilerplate. Rendered as a bare Row, not a
        // bordered Surface: it carries no click action, and matching the circle buttons'
        // chrome would make it look tappable (the same "dead control" fault as the old bell).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = LocalAccent.current,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = DateFormatter.formatMonthYear(System.currentTimeMillis()),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppPalette.textPrimary
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
            // Notification bell — only rendered when a debug destination is wired.
            if (onNavigateToNotifications != null) {
                Surface(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { onNavigateToNotifications() },
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

// --- Sticky Finance Status Bar ------------------------------------------------

@Composable
fun StickyFinanceStatusBar(
    monthlyIncome: Double,
    monthlyExpenses: Double,
    currentStreak: Int,
    hasTrackedToday: Boolean = false,
    freezeCount: Int = 0
) {
    // BUG-07: show the real streak rather than resetting to 0 until today is logged.
    val displayedStreak = currentStreak
    val isFrozen = freezeCount > 0 && !hasTrackedToday

    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = AppPalette.card,
        // No elevation: the sticky bar sits flush above the scrolling cards, and its
        // cast shadow read as a dark band cutting across the card below it.
        shadowElevation = 0.dp,
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
            Box(
                Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(AppPalette.cardBorder)
            )
            StickyStatItem(Icons.Default.ArrowDownward, ExpenseRed, formatCurrencyShort(monthlyExpenses))
            Box(
                Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(AppPalette.cardBorder)
            )
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
                        // quantity selects the plural item only — the format args are
                        // separate, so the count must be passed twice or "%1$d" renders raw.
                        text = if (streak > 0) pluralStringResource(
                            R.plurals.dashboard_streak_days,
                            streak,
                            streak
                        ) else "—",
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
