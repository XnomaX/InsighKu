package com.example.insightku.feature.home.presentation.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.ui.theme.*
import com.example.insightku.feature.home.presentation.PremiumFlameIcon
import com.example.insightku.feature.home.presentation.flameConfig
import java.util.Calendar
import java.util.Locale

@Composable
fun DailyStreakCard(
    currentStreak: Int,
    hasTrackedToday: Boolean,
    onCardClick: () -> Unit = {},
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayedStreak = if (hasTrackedToday) currentStreak else 0
    val config = flameConfig(displayedStreak)
    val weekDays = listOf("M", "T", "W", "T", "F", "S", "S")
    val activeDays = minOf(currentStreak, 7).let { if (hasTrackedToday) it.coerceAtLeast(1) else it }

    val statusLabel = when {
        !hasTrackedToday && currentStreak > 0 -> stringResource(R.string.streak_log_today_restore)
        !hasTrackedToday                      -> stringResource(R.string.streak_start_today)
        else                                  -> config.statusCopy
    }

    val motivationalText = when {
        !hasTrackedToday && currentStreak > 0 ->
            stringResource(R.string.streak_waiting, currentStreak)
        currentStreak == 0  -> stringResource(R.string.streak_log_your_first)
        currentStreak < 3   -> stringResource(R.string.streak_every_habit)
        currentStreak < 7   -> stringResource(R.string.streak_momentum_building)
        currentStreak < 14  -> stringResource(R.string.streak_one_week)
        currentStreak < 30  -> stringResource(R.string.streak_two_weeks)
        currentStreak < 100 -> stringResource(R.string.streak_remarkable)
        else                -> stringResource(R.string.streak_100_days)
    }

    Surface(
        modifier        = modifier.fillMaxWidth().clickable { onCardClick() },
        shape           = RoundedCornerShape(Dimens.CardRadiusLarge),
        color           = AppPalette.card,
        tonalElevation  = 0.dp,
        shadowElevation = 4.dp,
        border          = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.CardInnerPaddingLarge, vertical = Dimens.CardInnerPaddingLarge),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // -- Hero row ------------------------------------------------------
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

                    // Streak count
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AnimatedContent(
                            targetState = displayedStreak,
                            transitionSpec = {
                                (slideInVertically(tween(350)) { it / 2 } + fadeIn(tween(350))) togetherWith
                                    (slideOutVertically(tween(250)) { -it / 2 } + fadeOut(tween(250)))
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
                                    !hasTrackedToday && currentStreak > 0 -> AppPalette.textMuted
                                    else -> AppPalette.textMuted
                                }
                            )
                        }
                        Column(modifier = Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(stringResource(R.string.streak_day), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                            Text(stringResource(R.string.streak_streak), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
                        }
                    }

                    Text(
                        text = motivationalText,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted,
                        lineHeight = 19.sp
                    )
                }

                // Lottie flame
                PremiumFlameIcon(active = hasTrackedToday, size = 96.dp, streak = displayedStreak)
            }

            // -- Weekly rhythm track -------------------------------------------
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {                    Text(
                        stringResource(R.string.streak_this_week),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textMuted,
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
                                    .background(if (active) NavPurple else AppPalette.cardBorder),
                                contentAlignment = Alignment.Center
                            ) {
                                if (active)
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                else
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted, fontSize = 10.sp)
                            }
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (active) NavPurple else AppPalette.textMuted
                            )
                        }
                    }
                }
            }

            // -- Today status / CTA --------------------------------------------
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
                            Text(stringResource(R.string.streak_habit_intact), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AppPalette.successChipBg)
                            Text(stringResource(R.string.streak_tracked_today), style = MaterialTheme.typography.labelSmall, color = AppPalette.successChipBg.copy(alpha = 0.65f))
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
                            if (currentStreak == 0) stringResource(R.string.streak_start_your_streak) else stringResource(R.string.streak_log_today),
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

// --- Streak Celebration Dialog ------------------------------------------------

@Composable
fun StreakCelebrationDialog(streak: Int, onDismiss: () -> Unit) {
    val config = flameConfig(streak)

    val headline = when {
        streak >= 100 -> stringResource(R.string.celebration_legendary)
        streak >= 30  -> stringResource(R.string.celebration_on_fire)
        streak >= 14  -> stringResource(R.string.celebration_two_weeks)
        streak >= 7   -> stringResource(R.string.celebration_one_week)
        streak >= 3   -> stringResource(R.string.celebration_habit_forming)
        else          -> stringResource(R.string.celebration_streak_started)
    }
    val subtext = when {
        streak >= 100 -> stringResource(R.string.celebration_100_body)
        streak >= 30  -> stringResource(R.string.celebration_30_body)
        streak >= 14  -> stringResource(R.string.celebration_14_body)
        streak >= 7   -> stringResource(R.string.celebration_7_body)
        streak >= 3   -> stringResource(R.string.celebration_3_body)
        else          -> stringResource(R.string.celebration_1_body)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape           = RoundedCornerShape(Dimens.BottomSheetRadius),
            color           = AppPalette.card,
            tonalElevation  = 0.dp,
            shadowElevation = 8.dp,
            border          = BorderStroke(1.dp, AppPalette.cardBorder)
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
                        color = AppPalette.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = NavPurple.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = stringResource(R.string.streak_day_streak, streak),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NavPurple
                        )
                    }
                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppPalette.textMuted,
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
                        Text(stringResource(R.string.streak_keep_going), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

// --- Streak Detail Sheet ------------------------------------------------------

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
            color           = AppPalette.card,
            tonalElevation  = 0.dp,
            shadowElevation = 8.dp,
            border          = BorderStroke(1.dp, AppPalette.cardBorder),
            modifier        = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // -- Header ----------------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(R.string.streak_habit_journey), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                        Text(stringResource(R.string.streak_momentum_over_time), style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(AppPalette.cardBorder)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(15.dp), tint = AppPalette.textMuted)
                    }
                }

                // -- Flame + stats ---------------------------------------------
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
                            border = BorderStroke(1.dp, AppPalette.cardBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("$currentStreak", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp,
                                    color = if (currentStreak > 0) config.flamePrimary else AppPalette.textMuted)
                                Text(config.statusCopy, style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted, lineHeight = 14.sp)
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(Dimens.CardRadius),
                            color = AppPalette.background,
                            border = BorderStroke(1.dp, AppPalette.cardBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("$bestStreak", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp, color = NavPurple.copy(alpha = 0.75f))
                                Text(stringResource(R.string.streak_personal_best), style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted, lineHeight = 14.sp)
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
                            if (hasTrackedToday) stringResource(R.string.streak_today_complete)
                            else stringResource(R.string.streak_log_today_keep),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasTrackedToday) AppPalette.successChipBg else ExpenseRed
                        )
                    }
                }

                // -- Momentum calendar -----------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.streak_history), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                        color = AppPalette.textMuted, letterSpacing = 0.5.sp)

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimens.CardRadius),
                        color = AppPalette.background,
                        border = BorderStroke(1.dp, AppPalette.cardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Day-of-week header
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("M","T","W","T","F","S","S").forEach { day ->
                                    Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp, color = AppPalette.textMuted)
                                }
                            }

                            val today = Calendar.getInstance()
                            val todayDayOfWeek = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7
                            val totalWeeks = 16
                            val gridStartCal = today.clone() as Calendar
                            gridStartCal.add(Calendar.DAY_OF_YEAR, -((totalWeeks - 1) * 7 + todayDayOfWeek))

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
                                                            else     -> AppPalette.cardBorder
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
                                                            else            -> AppPalette.textMuted
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
                        LegendDot(NavPurple, stringResource(R.string.streak_tracked))
                        LegendDot(AppPalette.cardBorder, stringResource(R.string.streak_missed))
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onDismiss() },
                    shape = RoundedCornerShape(Dimens.ButtonRadius),
                    color = NavPurple
                ) {
                    Box(modifier = Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.streak_got_it), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
internal fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
    }
}
