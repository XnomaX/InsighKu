package com.example.insightku.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens

// ─── Color Utilities ──────────────────────────────────────────────────────────

/**
 * Safely parse a hex color string (e.g. "#7C4DFF" or "7C4DFF") into a Compose Color.
 * Returns a fallback color if parsing fails.
 */
fun parseCategoryColor(value: String, fallback: Color = Color(0xFF79747E)): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(value.ifBlank { "#79747E" }))
    }.getOrDefault(fallback)
}

// ─── Section Header with Count ────────────────────────────────────────────────

/**
 * Section header with a badge count — used in accounts and similar list screens.
 * Uses Dimens.ScreenHorizontalPadding for consistent horizontal spacing.
 */
@Composable
fun SectionHeaderWithCount(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenHorizontalPadding)
            .padding(top = 24.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
            color = AppPalette.textMuted
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(AppPalette.cardBorder)
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textMuted
            )
        }
    }
}

// ─── Detail Row ───────────────────────────────────────────────────────────────

/**
 * A label-value row used in detail sheets and info panels.
 */
@Composable
fun DetailRow(
    label: String,
    value: String,
    hint: String? = null,
    isPlaceholder: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted
            )
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppPalette.textMuted.copy(alpha = 0.6f)
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isPlaceholder) AppPalette.textMuted.copy(alpha = 0.5f) else AppPalette.textPrimary
        )
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

/**
 * Reusable empty state composable with icon, title, description, and optional CTA button.
 * Used across Accounts, Goals, Budget, and other list screens.
 */
@Composable
fun EmptyStateSection(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionColor: Color = AppPalette.accent,
    trailingContent: @Composable (() -> Unit)? = null,
    onAction: (() -> Unit)? = null
) {
    val accent = actionColor

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Layered circles for depth
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.06f))
            )
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.10f))
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppPalette.textPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp),
            lineHeight = 22.sp
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onAction),
                shape = RoundedCornerShape(14.dp),
                color = accent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }

        // Optional trailing content (e.g. extra hint text)
        if (trailingContent != null) {
            Spacer(modifier = Modifier.height(16.dp))
            trailingContent()
        }
    }
}
