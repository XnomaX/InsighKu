package com.example.insightku.feature.home.presentation.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.insightku.R
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.i18n.LocaleHelper
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.ExpenseRed
import com.example.insightku.core.ui.theme.IncomeGreen
import com.example.insightku.feature.home.presentation.PremiumFlameIcon
import com.example.insightku.feature.home.presentation.flameConfig
import java.util.Calendar
import java.util.Locale

/**
 * Single-letter weekday labels, Monday-first, in the applied language.
 *
 * Indonesian: S S R K J S M (Senin…Minggu)
 * English:    M T W T F S S (Monday…Sunday)
 *
 * One shared source for both weekday strips in this file — the 7-day progress strip and the
 * 16×7 history grid header. They used to be two separate hardcoded English lists, so
 * localizing one left the other rendering `M T W T F S S` a few hundred lines below it: two
 * different weekday conventions visible on the same screen at once.
 *
 * The locale comes from [LocaleHelper], the same source `AnalyticsStrings.isIndonesian()` and
 * `NumberFormatter` consult, so these initials cannot disagree in language with the copy and
 * amounts rendered beside them. Keyed on the language tag so the list is recomputed on a
 * recomposition that carries a new locale.
 */
@Composable
private fun narrowWeekdayLabels(): List<String> {
    val appLocale = LocaleHelper.currentLocale()
    return remember(appLocale.toLanguageTag()) {
        val monday = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        (0..6).map { offset ->
            val day = (monday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            DateFormatter.getNarrowDayName(day.timeInMillis, appLocale)
        }
    }
}

/** One column of the "This week" strip: the calendar date and its tracked state. */
private data class WeekDayCell(val dayOfMonth: Int, val tracked: Boolean)

@Composable
fun DailyStreakCard(
    currentStreak: Int,
    hasTrackedToday: Boolean,
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier,
    trackedDayKeys: Set<String> = emptySet(),
    repairAvailable: Boolean = false,
    onCardClick: () -> Unit = {},
    onUseRepair: () -> Unit = {}
) {
    // BUG-07: show the real streak — resetting to 0 before logging today hid existing
    // history and read as a broken/competitive counter. The status/motivation copy already
    // frames "not yet today" warmly (streak_waiting / streak_start_today).
    val displayedStreak = currentStreak
    val config = flameConfig(displayedStreak)
    // One cell per day of the current week (Monday-first): its calendar date and
    // whether that exact date has a transaction. The strip used to light up the
    // first `min(streak, 7)` weekdays, which contradicted the real tracked days as
    // soon as the streak started mid-week (checks on Mon+Tue while the tracked
    // Wednesday sat unchecked).
    val weekCells = remember(trackedDayKeys) {
        val fmt = java.text.SimpleDateFormat("yyyyMMdd", Locale.US)
        val monday = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        (0..6).map { offset ->
            val day = (monday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            WeekDayCell(
                dayOfMonth = day.get(Calendar.DAY_OF_MONTH),
                tracked = fmt.format(day.time) in trackedDayKeys
            )
        }
    }

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
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape           = RoundedCornerShape(Dimens.CardRadiusLarge),
        color           = AppPalette.card,
        tonalElevation  = 0.dp,
        // Border-only like the other dashboard cards: the elevation shadow drew a
        // dark rim on the card edges.
        shadowElevation = 0.dp,
        border          = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Dimens.CardInnerPaddingLarge,
                    vertical = Dimens.CardInnerPaddingLarge
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // -- Hero row ------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
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
                    weekCells.forEach { cell ->
                        // Checked day shows only the check; an unchecked day shows its
                        // calendar date so the row reads as this week's real dates.
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (cell.tracked) NavPurple else AppPalette.cardBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cell.tracked)
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            else
                                Text("${cell.dayOfMonth}", style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted, fontSize = 10.sp)
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
                if (repairAvailable) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUseRepair() },
                        shape = RoundedCornerShape(Dimens.ButtonRadius),
                        color = NavPurple
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.streak_repair),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddTransaction() },
                    shape = RoundedCornerShape(Dimens.ButtonRadius),
                    color = if (repairAvailable) AppPalette.cardBorder else NavPurple
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, null, tint = if (repairAvailable) AppPalette.textMuted else Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (currentStreak == 0) stringResource(R.string.streak_start_your_streak) else stringResource(R.string.streak_log_today),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (repairAvailable) AppPalette.textMuted else Color.White
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() },
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
    trackedDayKeys: Set<String> = emptySet(),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
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
                            // Day-of-week header — shares the localized Monday-first labels
                            // with the progress strip in DailyStreakCard so both weekday rows
                            // on the streak card read in the same language.
                            val gridDayLabels = narrowWeekdayLabels()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                gridDayLabels.forEach { day ->
                                    Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp, color = AppPalette.textMuted)
                                }
                            }

                            val totalWeeks = 16
                            // Pre-compute the 16×7 calendar grid once per tracked-day change
                            // instead of allocating ~112 Calendar clones on every recomposition.
                            // Cells are marked from the real tracked-day set, not from the
                            // streak length — the old `daysFromToday in 1..currentStreak`
                            // heuristic painted checks on days with no transactions and
                            // contradicted the week strip on the same card.
                            val gridCells = remember(trackedDayKeys) {
                                val dayFmt = java.text.SimpleDateFormat("yyyyMMdd", Locale.US)
                                val today = Calendar.getInstance()
                                val todayDayOfWeek = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7
                                val gridStartCal = today.clone() as Calendar
                                gridStartCal.add(Calendar.DAY_OF_YEAR, -((totalWeeks - 1) * 7 + todayDayOfWeek))
                                buildList {
                                    for (i in 0 until totalWeeks * 7) {
                                        val cellCal = gridStartCal.clone() as Calendar
                                        cellCal.add(Calendar.DAY_OF_YEAR, i)
                                        val daysFromToday = ((today.timeInMillis - cellCal.timeInMillis) / 86400000L).toInt()
                                        add(StreakCell(
                                            dayOfMonth = cellCal.get(Calendar.DAY_OF_MONTH),
                                            isToday = daysFromToday == 0,
                                            isFuture = daysFromToday < 0,
                                            isFirstOfMonth = cellCal.get(Calendar.DAY_OF_MONTH) == 1,
                                            isTrackedDay = dayFmt.format(cellCal.time) in trackedDayKeys,
                                            monthLabel = cellCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, LocaleHelper.currentLocale())?.take(1) ?: ""
                                        ))
                                    }
                                }
                            }

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
                                            val cell = gridCells[week * 7 + col]
                                            val tracked  = when {
                                                cell.isFuture -> false
                                                cell.isToday  -> hasTrackedToday
                                                else          -> cell.isTrackedDay
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        when {
                                                            tracked -> NavPurple.copy(alpha = 0.85f)
                                                            cell.isFuture -> Color.Transparent
                                                            else -> AppPalette.cardBorder
                                                        }
                                                    )
                                                    .then(
                                                        if (cell.isToday) Modifier.border(
                                                            1.5.dp,
                                                            NavPurple,
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                        else Modifier
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!cell.isFuture) {
                                                    Text(
                                                        if (cell.isFirstOfMonth && cell.monthLabel.isNotEmpty()) cell.monthLabel else "${cell.dayOfMonth}",
                                                        fontSize = 8.sp,
                                                        fontWeight = if (cell.isToday || cell.isFirstOfMonth) FontWeight.ExtraBold else FontWeight.Medium,
                                                        color = when {
                                                            tracked         -> Color.White
                                                            cell.isToday    -> NavPurple
                                                            cell.isFirstOfMonth  -> NavPurple.copy(alpha = 0.6f)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() },
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
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppPalette.textMuted)
    }
}

/** Pre-computed immutable data for one cell of the streak momentum grid. */
private data class StreakCell(
    val dayOfMonth: Int,
    val isToday: Boolean,
    val isFuture: Boolean,
    val isFirstOfMonth: Boolean,
    val isTrackedDay: Boolean,
    val monthLabel: String
)
