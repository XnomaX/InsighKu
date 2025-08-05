package com.example.insightku.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    CategoryIconInfo("Food & Drinks", Icons.Default.Restaurant, Color(0xFFF59E0B)),
    CategoryIconInfo("Transportation", Icons.Default.DirectionsCar, Color(0xFF3B82F6)),
    CategoryIconInfo("Housing", Icons.Default.Home, Color(0xFFEF4444)),
    CategoryIconInfo("Shopping", Icons.Default.ShoppingBag, Color(0xFFEC4899)),
    CategoryIconInfo("Entertainment", Icons.Default.SportsEsports, Color(0xFF8B5CF6)),
    CategoryIconInfo("Coffee & Cafes", Icons.Default.Coffee, Color(0xFFF59E0B)),
    CategoryIconInfo("Healthcare", Icons.Default.LocalHospital, Color(0xFF10B981)),
    CategoryIconInfo("Utilities", Icons.Default.Bolt, Color(0xFFF59E0B))
)

// --- SHARED COMPOSABLES ---

@Composable
internal fun IconOption(
    iconData: CategoryIconInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .border(
                2.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconData.color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                iconData.icon,
                contentDescription = iconData.name,
                tint = iconData.color,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(iconData.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
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
