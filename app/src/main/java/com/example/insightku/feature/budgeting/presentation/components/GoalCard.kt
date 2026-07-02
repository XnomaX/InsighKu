package com.example.insightku.feature.budgeting.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.feature.budgeting.data.model.GoalStatus
import com.example.insightku.feature.budgeting.domain.model.Goal
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Premium GoalCard with improved visual hierarchy and modern design.
 *
 * Features:
 * - Soft elevation with subtle shadow
 * - Clear typography hierarchy
 * - Beautiful progress visualization
 * - Goal color theming
 * - Smooth animations on state changes
 */
@Composable
fun GoalCard(
    goal: Goal,
    onClick: () -> Unit,
    onContribute: () -> Unit,
    onWithdraw: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goalColor = try {
        Color(android.graphics.Color.parseColor(goal.color))
    } catch (e: Exception) {
        AppPalette.textPrimary
    }

    val isCompleted = goal.status == GoalStatus.COMPLETED
    val isPaused = goal.status == GoalStatus.PAUSED
    val isOverdue = goal.isOverdue

    // Animation for progress
    val animatedProgress by animateFloatAsState(
        targetValue = goal.progressPercent.toFloat() / 100f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
        label = "progress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        border = null
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // ── Header Row ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Premium Icon Container with gradient background
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(14.dp),
                                spotColor = goalColor.copy(alpha = 0.3f)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        goalColor.copy(alpha = 0.15f),
                                        goalColor.copy(alpha = 0.05f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getGoalIcon(goal.iconName),
                            contentDescription = null,
                            tint = goalColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppPalette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Deadline badge (if set and not completed)
                        if (goal.deadline != null && !isCompleted) {
                            Spacer(modifier = Modifier.height(4.dp))
                            DeadlineBadge(
                                daysLeft = goal.daysRemaining ?: 0,
                                deadline = goal.deadline.format(DateTimeFormatter.ofPattern("d MMM")),
                                isOverdue = isOverdue
                            )
                        }
                    }
                }

                // Status badge or edit button
                if (isCompleted || isPaused) {
                    StatusBadge(
                        isCompleted = isCompleted,
                        isPaused = isPaused
                    )
                } else {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "More options",
                            tint = AppPalette.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Progress Section ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Current amount
                Column {
                    Text(
                        text = formatCurrencyCompact(goal.currentAmount),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) SuccessColor else AppPalette.textPrimary
                    )
                    Text(
                        text = "of ${formatCurrencyCompact(goal.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                }

                // Progress percentage with color
                Text(
                    text = "${goal.progressPercent.toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isCompleted -> SuccessColor
                        goal.progressPercent >= 75 -> SuccessColor
                        goal.progressPercent >= 50 -> goalColor
                        else -> goalColor
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Premium Progress Bar ────────────────────────────────────────────
            PremiumProgressBar(
                progress = animatedProgress,
                goalColor = if (isCompleted) SuccessColor else goalColor
            )

            // ── Bottom Section ──────────────────────────────────────────────────
            if (!isPaused) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Remaining amount badge
                    if (!isCompleted && goal.remainingAmount > 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = goalColor.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Flag,
                                    contentDescription = null,
                                    tint = goalColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${formatCurrencyCompact(goal.remainingAmount)} to go",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = goalColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Withdraw button (secondary)
                        if (goal.currentAmount > 0) {
                            Surface(
                                onClick = onWithdraw,
                                shape = RoundedCornerShape(50.dp),
                                color = AppPalette.cardElevated,
                                border = BorderStroke(1.dp, AppPalette.cardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowUpward,
                                        contentDescription = null,
                                        tint = AppPalette.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Withdraw",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AppPalette.textMuted
                                    )
                                }
                            }
                        }

                        // Save button (primary)
                        Button(
                            onClick = onContribute,
                            modifier = Modifier.height(38.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = goalColor
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Save",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Premium Progress Bar ───────────────────────────────────────────────────────

@Composable
private fun PremiumProgressBar(
    progress: Float,
    goalColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(AppPalette.cardBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(5.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            goalColor.copy(alpha = 0.8f),
                            goalColor
                        )
                    )
                )
        )
    }
}

// ─── Deadline Badge ────────────────────────────────────────────────────────────

@Composable
private fun DeadlineBadge(
    daysLeft: Int,
    deadline: String,
    isOverdue: Boolean
) {
    val badgeColor = when {
        isOverdue -> ExpenseRed
        daysLeft <= 7 -> WarningYellow
        else -> AppPalette.textMuted
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = badgeColor.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = when {
                    isOverdue -> "Overdue"
                    daysLeft <= 0 -> "Due today"
                    daysLeft <= 7 -> "$daysLeft days left"
                    else -> deadline
                },
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─── Status Badge ──────────────────────────────────────────────────────────────

@Composable
private fun StatusBadge(
    isCompleted: Boolean,
    isPaused: Boolean
) {
    val (color, text, icon) = when {
        isCompleted -> Triple(SuccessColor, "Completed", Icons.Outlined.CheckCircle)
        else -> Triple(AppPalette.textMuted, "Paused", Icons.Outlined.Pause)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

// ─── Compact Goal Card ─────────────────────────────────────────────────────────

@Composable
fun CompactGoalCard(
    goal: Goal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goalColor = try {
        Color(android.graphics.Color.parseColor(goal.color))
    } catch (e: Exception) {
        AppPalette.textPrimary
    }

    val isCompleted = goal.status == GoalStatus.COMPLETED

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(goalColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getGoalIcon(goal.iconName),
                        contentDescription = null,
                        tint = goalColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "${goal.progressPercent.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) SuccessColor else goalColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Name
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppPalette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Amounts
            Text(
                text = formatCurrencyCompact(goal.currentAmount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textPrimary
            )
            Text(
                text = "of ${formatCurrencyCompact(goal.targetAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AppPalette.cardBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((goal.progressPercent / 100).toFloat())
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isCompleted) SuccessColor else goalColor)
                )
            }
        }
    }
}

// ─── Helper Functions ──────────────────────────────────────────────────────────

private fun getGoalIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "savings", "piggy bank" -> Icons.Outlined.Savings
        "wallet", "account balance wallet" -> Icons.Outlined.AccountBalanceWallet
        "cash", "money", "paid" -> Icons.Outlined.Paid
        "flight", "airplane" -> Icons.Outlined.Flight
        "car", "directions car" -> Icons.Outlined.DirectionsCar
        "home", "house" -> Icons.Outlined.Home
        "school", "education", "graduation" -> Icons.Outlined.School
        "health", "health and safety" -> Icons.Outlined.HealthAndSafety
        "warning", "emergency" -> Icons.Outlined.Warning
        "trending up", "investment", "stocks" -> Icons.Outlined.TrendingUp
        "card giftcard", "gift" -> Icons.Outlined.CardGiftcard
        "celebration" -> Icons.Outlined.Celebration
        "star" -> Icons.Outlined.Star
        "flag", "target", "gps fixed" -> Icons.Outlined.Flag
        "beach", "travel" -> Icons.Outlined.BeachAccess
        "hotel", "suitcase" -> Icons.Outlined.Luggage
        "laptop", "technology" -> Icons.Outlined.Laptop
        "phone", "smartphone" -> Icons.Outlined.Smartphone
        "diamond", "gold", "investment" -> Icons.Outlined.Diamond
        else -> Icons.Outlined.Savings
    }
}

private fun formatCurrencyCompact(amount: Double): String {
    return when {
        amount >= 1_000_000_000 -> {
            val formatted = NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount / 1_000_000_000)
            "Rp$formatted M"
        }
        amount >= 1_000_000 -> {
            val formatted = NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount / 1_000_000)
            "Rp$formatted M"
        }
        amount >= 1_000 -> {
            val formatted = NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount / 1_000)
            "Rp$formatted K"
        }
        else -> "Rp${NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount.toLong())}"
    }
}

// Color references
private val ExpenseRed = Color(0xFFEF4444)
private val WarningYellow = Color(0xFFF59E0B)
