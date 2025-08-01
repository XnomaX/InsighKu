package com.example.insightku.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

data class StreakData(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalDays: Int = 0,
    val level: String = "Beginner"
)

@Composable
fun StreakCalendar(
    currentStreak: Int,
    modifier: Modifier = Modifier
) {
    val streakData = StreakData(currentStreak = currentStreak)
    val calendar = Calendar.getInstance()
    val today = calendar.time

    // Generate last 7 days
    val weekDays = (0 until 7).map { index ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(6 - index))
        cal.time
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with fire emoji and streak info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = "Fire",
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        text = "Daily Streak",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "$currentStreak days",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF5A2A82)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Weekly calendar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(weekDays) { date ->
                    val isToday = isSameDay(date, today)
                    val daysBetween = daysBetween(date, today)
                    val hasStreak = daysBetween <= currentStreak - 1 && daysBetween >= 0

                    DayItem(
                        date = date,
                        isToday = isToday,
                        hasStreak = hasStreak
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Streak stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StreakStat(
                    label = "Current",
                    value = streakData.currentStreak.toString(),
                    modifier = Modifier.weight(1f)
                )

                StreakStat(
                    label = "Best",
                    value = streakData.bestStreak.toString(),
                    modifier = Modifier.weight(1f)
                )

                StreakStat(
                    label = "Total Days",
                    value = streakData.totalDays.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DayItem(
    date: Date,
    isToday: Boolean,
    hasStreak: Boolean
) {
    val calendar = Calendar.getInstance()
    calendar.time = date

    val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(date).uppercase()
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(40.dp)
    ) {
        // Day name
        Text(
            text = dayOfWeek,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Day number with streak indicator
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = when {
                        isToday && hasStreak -> Color(0xFF5A2A82)
                        isToday -> MaterialTheme.colorScheme.primary
                        hasStreak -> Color(0xFF5A2A82).copy(alpha = 0.8f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                ),
                color = when {
                    isToday || hasStreak -> Color.White
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        // Streak indicator
        if (hasStreak) {
            Text(
                text = "🔥",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun StreakStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFF5A2A82)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Helper functions using Calendar instead of LocalDate
private fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun daysBetween(date1: Date, date2: Date): Long {
    val cal1 = Calendar.getInstance().apply {
        time = date1
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val cal2 = Calendar.getInstance().apply {
        time = date2
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return (cal2.timeInMillis - cal1.timeInMillis) / (1000 * 60 * 60 * 24)
}
