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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

// ─── Design tokens ────────────────────────────────────────────────────────────
private val Purple      = Color(0xFF7C4DFF)
private val PurpleTint  = Color(0xFFEDE9FE)
private val Surface     = Color(0xFFFAF9FE)
private val Border      = Color(0xFFECE7F6)
private val TextPrimary = Color(0xFF1A1A2E)
private val TextMuted   = Color(0xFF9E9E9E)

private val DAYS   = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
private val MONTHS = listOf(
    "January","February","March","April","May","June",
    "July","August","September","October","November","December"
)

/**
 * Premium custom date picker dialog.
 * Replaces the default Material3 DatePickerDialog with a calm, elegant design.
 *
 * @param initialMillis  Pre-selected date in epoch ms
 * @param onDateSelected Called with the selected epoch ms (start of day)
 * @param onDismiss      Called when user taps outside or Cancel
 */
@Composable
fun PremiumDatePicker(
    initialMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initCal = Calendar.getInstance().apply { timeInMillis = initialMillis }
    var displayYear  by remember { mutableIntStateOf(initCal.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableIntStateOf(initCal.get(Calendar.MONTH)) }
    var selectedYear  by remember { mutableIntStateOf(initCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(initCal.get(Calendar.MONTH)) }
    var selectedDay   by remember { mutableIntStateOf(initCal.get(Calendar.DAY_OF_MONTH)) }

    // Animate month transitions
    var slideDir by remember { mutableIntStateOf(0) } // -1 prev, +1 next

    // Scrim + card entrance
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scrimAlpha by animateFloatAsState(
        targetValue   = if (visible) 0.5f else 0f,
        animationSpec = tween(250),
        label         = "dp_scrim"
    )
    fun prevMonth() {
        slideDir = -1
        if (displayMonth == 0) { displayMonth = 11; displayYear-- }
        else displayMonth--
    }

    fun nextMonth() {
        slideDir = 1
        if (displayMonth == 11) { displayMonth = 0; displayYear++ }
        else displayMonth++
    }

    fun daysInMonth(year: Int, month: Int): Int =
        Calendar.getInstance().apply { set(year, month, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)

    fun firstDayOfWeek(year: Int, month: Int): Int =
        Calendar.getInstance().apply { set(year, month, 1) }.get(Calendar.DAY_OF_WEEK) - 1

    val today = Calendar.getInstance()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .border(1.dp, Border, RoundedCornerShape(28.dp))
                .pointerInput(Unit) { detectTapGestures { /* consume */ } }
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
                        "Select Date",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                    Text(
                        "Choose a date for this transaction",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                // ── Selected date pill ────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PurpleTint
                ) {
                    Text(
                        text = buildString {
                            append(DAYS[(Calendar.getInstance().apply {
                                set(selectedYear, selectedMonth, selectedDay)
                            }.get(Calendar.DAY_OF_WEEK) - 1)])
                            append(", ")
                            append(selectedDay)
                            append(" ")
                            append(MONTHS[selectedMonth].take(3))
                            append(" ")
                            append(selectedYear)
                        },
                        modifier   = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = Purple
                    )
                }

                // ── Month navigation ──────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PurpleTint)
                            .clickable { prevMonth() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous month",
                            tint     = Purple,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedContent(
                        targetState  = "$displayMonth/$displayYear",
                        transitionSpec = {
                            if (slideDir >= 0) {
                                slideInHorizontally(tween(220)) { it / 2 } + fadeIn(tween(220)) togetherWith
                                slideOutHorizontally(tween(220)) { -it / 2 } + fadeOut(tween(220))
                            } else {
                                slideInHorizontally(tween(220)) { -it / 2 } + fadeIn(tween(220)) togetherWith
                                slideOutHorizontally(tween(220)) { it / 2 } + fadeOut(tween(220))
                            }
                        },
                        label = "month_anim"
                    ) { key ->
                        val parts = key.split("/")
                        val m = parts[0].toInt()
                        val y = parts[1].toInt()
                        Text(
                            "${MONTHS[m]} $y",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = TextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PurpleTint)
                            .clickable { nextMonth() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next month",
                            tint     = Purple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // ── Day-of-week headers ───────────────────────────────────
                Row(modifier = Modifier.fillMaxWidth()) {
                    DAYS.forEach { day ->
                        Text(
                            day,
                            modifier   = Modifier.weight(1f),
                            textAlign  = TextAlign.Center,
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextMuted
                        )
                    }
                }

                // ── Calendar grid ─────────────────────────────────────────
                AnimatedContent(
                    targetState  = "$displayMonth/$displayYear",
                    transitionSpec = {
                        if (slideDir >= 0) {
                            slideInHorizontally(tween(220)) { it / 2 } + fadeIn(tween(220)) togetherWith
                            slideOutHorizontally(tween(220)) { -it / 2 } + fadeOut(tween(220))
                        } else {
                            slideInHorizontally(tween(220)) { -it / 2 } + fadeIn(tween(220)) togetherWith
                            slideOutHorizontally(tween(220)) { it / 2 } + fadeOut(tween(220))
                        }
                    },
                    label = "grid_anim"
                ) { key ->
                    val parts  = key.split("/")
                    val m      = parts[0].toInt()
                    val y      = parts[1].toInt()
                    val days   = daysInMonth(y, m)
                    val offset = firstDayOfWeek(y, m)
                    val cells  = offset + days
                    val rows   = (cells + 6) / 7

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (row in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (col in 0 until 7) {
                                    val cellIndex = row * 7 + col
                                    val day = cellIndex - offset + 1
                                    val isValid   = day in 1..days
                                    val isSelected = isValid && day == selectedDay &&
                                                     m == selectedMonth && y == selectedYear
                                    val isToday   = isValid &&
                                                    day == today.get(Calendar.DAY_OF_MONTH) &&
                                                    m   == today.get(Calendar.MONTH) &&
                                                    y   == today.get(Calendar.YEAR)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> Purple
                                                    isToday    -> PurpleTint
                                                    else       -> Color.Transparent
                                                }
                                            )
                                            .then(
                                                if (isValid && !isSelected) Modifier.clickable {
                                                    selectedDay   = day
                                                    selectedMonth = m
                                                    selectedYear  = y
                                                } else Modifier
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
                                                    isToday    -> Purple
                                                    else       -> TextPrimary
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
                        border   = androidx.compose.foundation.BorderStroke(1.dp, Border)
                    ) {
                        Text(
                            "Cancel",
                            fontWeight = FontWeight.SemiBold,
                            color      = TextMuted
                        )
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
                        colors   = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) {
                        Text("Confirm", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

