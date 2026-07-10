package com.example.insightku.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.LocalAccent
import java.util.Calendar

@Composable
private fun getDayAbbreviations(): List<String> = listOf(
    stringResource(R.string.date_picker_day_su),
    stringResource(R.string.date_picker_day_mo),
    stringResource(R.string.date_picker_day_tu),
    stringResource(R.string.date_picker_day_we),
    stringResource(R.string.date_picker_day_th),
    stringResource(R.string.date_picker_day_fr),
    stringResource(R.string.date_picker_day_sa)
)

@Composable
private fun getMonthName(monthIndex: Int): String = when (monthIndex) {
    0 -> stringResource(R.string.date_picker_month_january)
    1 -> stringResource(R.string.date_picker_month_february)
    2 -> stringResource(R.string.date_picker_month_march)
    3 -> stringResource(R.string.date_picker_month_april)
    4 -> stringResource(R.string.date_picker_month_may)
    5 -> stringResource(R.string.date_picker_month_june)
    6 -> stringResource(R.string.date_picker_month_july)
    7 -> stringResource(R.string.date_picker_month_august)
    8 -> stringResource(R.string.date_picker_month_september)
    9 -> stringResource(R.string.date_picker_month_october)
    10 -> stringResource(R.string.date_picker_month_november)
    11 -> stringResource(R.string.date_picker_month_december)
    else -> ""
}

/**
 * Premium custom date picker dialog.
 *
 * Uses a Compose [Dialog] window so it renders above ModalBottomSheets and other
 * overlays regardless of where it is called in the composition tree.
 * Colors are resolved from [AppPalette] and [LocalAccent] for full dark/light support.
 *
 * @param initialMillis  Pre-selected date in epoch ms
 * @param onDateSelected Called with the selected epoch ms (start of day)
 * @param onDismiss      Called when user taps the scrim or Cancel
 */
@Composable
fun PremiumDatePicker(
    initialMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val accent      = LocalAccent.current
    val accentTint  = AppPalette.cardElevated
    val cardBg      = AppPalette.card
    val cardBorder  = AppPalette.cardBorder
    val textPrimary = AppPalette.textPrimary
    val textMuted   = AppPalette.textMuted

    val initCal = Calendar.getInstance().apply { timeInMillis = initialMillis }
    var displayYear  by remember { mutableIntStateOf(initCal.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableIntStateOf(initCal.get(Calendar.MONTH)) }
    var selectedYear  by remember { mutableIntStateOf(initCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(initCal.get(Calendar.MONTH)) }
    var selectedDay   by remember { mutableIntStateOf(initCal.get(Calendar.DAY_OF_MONTH)) }

    var slideDir by remember { mutableIntStateOf(0) }

    fun prevMonth() {
        slideDir = -1
        if (displayMonth == 0) { displayMonth = 11; displayYear-- } else displayMonth--
    }
    fun nextMonth() {
        slideDir = 1
        if (displayMonth == 11) { displayMonth = 0; displayYear++ } else displayMonth++
    }
    fun daysInMonth(year: Int, month: Int): Int =
        Calendar.getInstance().apply { set(year, month, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
    fun firstDayOfWeek(year: Int, month: Int): Int =
        Calendar.getInstance().apply { set(year, month, 1) }.get(Calendar.DAY_OF_WEEK) - 1

    val today = Calendar.getInstance()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Disable the Dialog's own native dim — our scrim covers it.
        val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
        SideEffect { dialogWindowProvider?.window?.setDimAmount(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(28.dp))
                    .pointerInput(Unit) { detectTapGestures { /* consume taps so they don't dismiss */ } }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // ── Header ────────────────────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(R.string.date_picker_title),
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = textPrimary
                    )
                    Text(
                        stringResource(R.string.date_picker_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = textMuted
                    )
                    }

                    // ── Selected date pill ────────────────────────────────────
                    val dayAbbreviations = getDayAbbreviations()
                    Surface(shape = RoundedCornerShape(14.dp), color = accentTint) {
                        Text(
                            text = buildString {
                                append(dayAbbreviations[Calendar.getInstance().apply {
                                    set(selectedYear, selectedMonth, selectedDay)
                                }.get(Calendar.DAY_OF_WEEK) - 1])
                                append(", $selectedDay ")
                                append(getMonthName(selectedMonth).take(3))
                                append(" $selectedYear")
                            },
                            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color      = accent
                        )
                    }

                    // ── Month navigation ──────────────────────────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(accentTint).clickable { prevMonth() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.date_picker_prev_month), tint = accent, modifier = Modifier.size(20.dp))
                        }

                        AnimatedContent(
                            targetState = "$displayMonth/$displayYear",
                            transitionSpec = {
                                if (slideDir >= 0)
                                    slideInHorizontally(tween(220)) { it / 2 } + fadeIn(tween(220)) togetherWith
                                    slideOutHorizontally(tween(220)) { -it / 2 } + fadeOut(tween(220))
                                else
                                    slideInHorizontally(tween(220)) { -it / 2 } + fadeIn(tween(220)) togetherWith
                                    slideOutHorizontally(tween(220)) { it / 2 } + fadeOut(tween(220))
                            },
                            label = "month_anim"
                        ) { key ->
                            val parts = key.split("/")
                            Text(
                                "${getMonthName(parts[0].toInt())} ${parts[1]}",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = textPrimary
                            )
                        }

                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(accentTint).clickable { nextMonth() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.date_picker_next_month), tint = accent, modifier = Modifier.size(20.dp))
                        }
                    }

                    // ── Day-of-week headers ───────────────────────────────────
                    Row(modifier = Modifier.fillMaxWidth()) {
                        dayAbbreviations.forEach { day ->
                            Text(
                                day,
                                modifier   = Modifier.weight(1f),
                                textAlign  = TextAlign.Center,
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color      = textMuted
                            )
                        }
                    }

                    // ── Calendar grid ─────────────────────────────────────────
                    AnimatedContent(
                        targetState = "$displayMonth/$displayYear",
                        transitionSpec = {
                            if (slideDir >= 0)
                                slideInHorizontally(tween(220)) { it / 2 } + fadeIn(tween(220)) togetherWith
                                slideOutHorizontally(tween(220)) { -it / 2 } + fadeOut(tween(220))
                            else
                                slideInHorizontally(tween(220)) { -it / 2 } + fadeIn(tween(220)) togetherWith
                                slideOutHorizontally(tween(220)) { it / 2 } + fadeOut(tween(220))
                        },
                        label = "grid_anim"
                    ) { key ->
                        val parts  = key.split("/")
                        val m      = parts[0].toInt()
                        val y      = parts[1].toInt()
                        val days   = daysInMonth(y, m)
                        val offset = firstDayOfWeek(y, m)
                        val rows   = (offset + days + 6) / 7

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (row in 0 until rows) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    for (col in 0 until 7) {
                                        val day        = row * 7 + col - offset + 1
                                        val isValid    = day in 1..days
                                        val isSelected = isValid && day == selectedDay && m == selectedMonth && y == selectedYear
                                        val isToday    = isValid &&
                                            day == today.get(Calendar.DAY_OF_MONTH) &&
                                            m   == today.get(Calendar.MONTH) &&
                                            y   == today.get(Calendar.YEAR)

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                                .background(when {
                                                    isSelected -> accent
                                                    isToday    -> accentTint
                                                    else       -> Color.Transparent
                                                })
                                                .then(
                                                    if (isValid && !isSelected)
                                                        Modifier.clickable {
                                                            selectedDay = day; selectedMonth = m; selectedYear = y
                                                        }
                                                    else Modifier
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isValid) {
                                                Text(
                                                    "$day",
                                                    style      = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                    color      = when {
                                                        isSelected -> Color.White
                                                        isToday    -> accent
                                                        else       -> textPrimary
                                                    },
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Action buttons ────────────────────────────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick  = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = RoundedCornerShape(14.dp),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                        ) {
                            Text(stringResource(R.string.date_picker_cancel), fontWeight = FontWeight.SemiBold, color = textMuted)
                        }
                        Button(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                onDateSelected(cal.timeInMillis)
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = accent)
                        ) {
                            Text(stringResource(R.string.date_picker_confirm), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
