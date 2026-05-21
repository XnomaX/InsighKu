package com.example.insightku.ui.dialogs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.insightku.utils.CurrencyUtils
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

// --- SHARED DATA ---

internal data class CategoryIconInfo(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

internal val defaultCategoryIcons = listOf(
    CategoryIconInfo("Food & Drinks",   Icons.Default.Restaurant,           Color(0xFFF59E0B)),
    CategoryIconInfo("Transportation",  Icons.Default.DirectionsCar,        Color(0xFF3B82F6)),
    CategoryIconInfo("Housing",         Icons.Default.Home,                 Color(0xFFEF4444)),
    CategoryIconInfo("Shopping",        Icons.Default.ShoppingBag,          Color(0xFFEC4899)),
    CategoryIconInfo("Entertainment",   Icons.Default.SportsEsports,        Color(0xFF8B5CF6)),
    CategoryIconInfo("Coffee & Cafes",  Icons.Default.Coffee,               Color(0xFFF59E0B)),
    CategoryIconInfo("Healthcare",      Icons.Default.LocalHospital,        Color(0xFF10B981)),
    CategoryIconInfo("Utilities",       Icons.Default.Bolt,                 Color(0xFFF59E0B)),
    CategoryIconInfo("Education",       Icons.Default.School,               Color(0xFF3B82F6)),
    CategoryIconInfo("Travel",          Icons.Default.Flight,               Color(0xFF06B6D4)),
    CategoryIconInfo("Fitness",         Icons.Default.FitnessCenter,        Color(0xFF10B981)),
    CategoryIconInfo("Subscriptions",   Icons.Default.Subscriptions,        Color(0xFF8B5CF6)),
    CategoryIconInfo("Loan/Cicilan",    Icons.Default.AccountBalance,       Color(0xFFEF4444)),
    CategoryIconInfo("Insurance",       Icons.Default.Security,             Color(0xFF3B82F6)),
    CategoryIconInfo("Savings",         Icons.Default.Savings,              Color(0xFF10B981)),
    CategoryIconInfo("Investment",      Icons.Default.TrendingUp,           Color(0xFF06B6D4)),
    CategoryIconInfo("Gift",            Icons.Default.CardGiftcard,         Color(0xFFEC4899)),
    CategoryIconInfo("Others",          Icons.Default.Category,             Color(0xFF79747E))
)

// --- SHARED COMPOSABLES ---

@Composable
internal fun IconOption(
    iconData: CategoryIconInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(
                if (isSelected) Color(0xFF7C4DFF).copy(alpha = 0.12f) else Color.White
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFF7C4DFF) else Color(0xFFECE7F6),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconData.color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                iconData.icon,
                contentDescription = iconData.name,
                tint = iconData.color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun SliderSection(
    title: String,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    prefix: String = "",
    suffix: String = ""
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "$prefix${value.roundToInt().formatCurrency()}$suffix",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$prefix${range.start.roundToInt().formatCurrency()}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "$prefix${range.endInclusive.roundToInt().formatCurrency()}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// Extension function for currency formatting
internal fun Int.formatCurrency(): String {
    return NumberFormat.getNumberInstance(Locale("id", "ID")).format(this)
}

// ─── Recurring Period Selector ────────────────────────────────────────────────

private val recurringPeriods = listOf("Weekly", "Monthly", "Yearly")

@Composable
internal fun RecurringPeriodSelector(
    selected: String?,
    onSelect: (String?) -> Unit
) {
    val purple = Color(0xFF7C4DFF)
    val border = Color(0xFFECE7F6)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // "None" option
        val noneSelected = selected == null
        val noneBg by animateColorAsState(
            targetValue = if (noneSelected) purple else Color.White,
            animationSpec = tween(180),
            label = "none_bg"
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(noneBg)
                .border(1.dp, if (noneSelected) purple else border, RoundedCornerShape(50.dp))
                .clickable { onSelect(null) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "None",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (noneSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (noneSelected) Color.White else Color(0xFF6B6B8A)
            )
        }

        recurringPeriods.forEach { period ->
            val isSelected = selected == period
            val bg by animateColorAsState(
                targetValue = if (isSelected) purple else Color.White,
                animationSpec = tween(180),
                label = "period_bg_$period"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(bg)
                    .border(1.dp, if (isSelected) purple else border, RoundedCornerShape(50.dp))
                    .clickable { onSelect(if (isSelected) null else period) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color(0xFF6B6B8A)
                )
            }
        }
    }
}

// ─── Budget Limit Input ───────────────────────────────────────────────────────

@Composable
internal fun BudgetLimitInput(
    budgetLimitText: String,
    onBudgetLimitTextChange: (String) -> Unit,
    alertThreshold: Float,
    onAlertThresholdChange: (Float) -> Unit
) {
    val purple = Color(0xFF7C4DFF)
    val border = Color(0xFFECE7F6)
    val parsedLimit = budgetLimitText.filter { it.isDigit() }.toLongOrNull() ?: 0L

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Budget",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B6B8A)
                )
                if (parsedLimit > 0) {
                    Text(
                        text = "Rp ${parsedLimit.formatCurrencyLong()}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = purple
                    )
                }
            }
            OutlinedTextField(
                value = CurrencyUtils.formatInputThousands(budgetLimitText),
                onValueChange = { input ->
                    onBudgetLimitTextChange(CurrencyUtils.stripThousands(input))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("e.g. 2.000.000", color = Color(0xFFBDBDBD), style = MaterialTheme.typography.bodyMedium)
                },
                leadingIcon = {
                    Text("Rp", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = purple)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = purple,
                    unfocusedBorderColor = border,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Alert at", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF6B6B8A))
                Text(
                    "${alertThreshold.roundToInt()}% of budget",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = purple
                )
            }
            if (parsedLimit > 0) {
                Text(
                    "≈ Rp ${(parsedLimit * alertThreshold / 100).toLong().formatCurrencyLong()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9E9E9E)
                )
            }
            Slider(
                value = alertThreshold,
                onValueChange = onAlertThresholdChange,
                valueRange = 50f..100f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = purple,
                    activeTrackColor = purple,
                    inactiveTrackColor = border
                )
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("50%", style = MaterialTheme.typography.labelSmall, color = Color(0xFFBDBDBD))
                Text("100%", style = MaterialTheme.typography.labelSmall, color = Color(0xFFBDBDBD))
            }
        }
    }
}

private fun Long.formatCurrencyLong(): String =
    NumberFormat.getNumberInstance(Locale("id", "ID")).format(this)
