package com.example.insightku.feature.budgeting.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.insightku.core.ui.theme.SuccessColor
import com.example.insightku.feature.budgeting.data.model.GoalStatus
import com.example.insightku.feature.budgeting.domain.model.Goal

/**
 * Card displaying a single goal with progress.
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
        MaterialTheme.colorScheme.primary
    }

    val isCompleted = goal.status == GoalStatus.COMPLETED
    val isPaused = goal.status == GoalStatus.PAUSED

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(goalColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getGoalIcon(goal.iconName),
                            contentDescription = null,
                            tint = goalColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (goal.deadline != null && !isCompleted) {
                            val daysLeft = goal.daysRemaining ?: 0
                            Text(
                                text = if (daysLeft > 0) "$daysLeft days left" else "Due today",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (daysLeft <= 7) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Status Badge
                if (isCompleted || isPaused) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCompleted) SuccessColor.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isCompleted) "Completed" else "Paused",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCompleted) SuccessColor
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // More options
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "More options"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "$${formatAmount(goal.currentAmount)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) SuccessColor else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "of $${formatAmount(goal.targetAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${goal.progressPercent.toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) SuccessColor else goalColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            GoalProgressBar(
                progress = goal.progressPercent.toFloat(),
                progressColor = goalColor,
                showPercentage = false
            )

            // Action Buttons
            if (!isPaused) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onWithdraw,
                        modifier = Modifier.weight(1f),
                        enabled = goal.currentAmount > 0
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Withdraw")
                    }

                    Button(
                        onClick = onContribute,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * Compact goal card for grid display.
 */
@Composable
fun CompactGoalCard(
    goal: Goal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goalColor = try {
        Color(android.graphics.Color.parseColor(goal.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(goalColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getGoalIcon(goal.iconName),
                        contentDescription = null,
                        tint = goalColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "${goal.progressPercent.toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = goalColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = goal.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$${formatAmount(goal.currentAmount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "of $${formatAmount(goal.targetAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            CompactProgressBar(
                progress = goal.progressPercent.toFloat(),
                progressColor = goalColor
            )
        }
    }
}

/**
 * Get icon for goal based on icon name.
 */
private fun getGoalIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "savings" -> Icons.Outlined.Savings
        "vacation" -> Icons.Outlined.Flight
        "car" -> Icons.Outlined.DirectionsCar
        "home" -> Icons.Outlined.Home
        "education" -> Icons.Outlined.School
        "health" -> Icons.Outlined.HealthAndSafety
        "emergency" -> Icons.Outlined.Warning
        "investment" -> Icons.Outlined.TrendingUp
        "gift" -> Icons.Outlined.CardGiftcard
        "celebration" -> Icons.Outlined.Celebration
        else -> Icons.Outlined.Savings
    }
}

/**
 * Format amount for display.
 */
private fun formatAmount(amount: Double): String {
    return "%.2f".format(amount).replace(".00", "")
}
