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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens

// ─── Color Utilities ──────────────────────────────────────────────────────────

/**
 * Safely parse a hex color string (e.g. "#7C4DFF" or "7C4DFF") into a Compose Color.
 * Returns a fallback color if parsing fails.
 */
fun parseCategoryColor(value: String, fallback: Color = Color(0xFF79747E)): Color {
    return runCatching {
        Color(value.ifBlank { "#79747E" }.toColorInt())
    }.getOrDefault(fallback)
}

// ─── Locale-proof text ────────────────────────────────────────────────────────

/** Lower bound for automatic shrinking, as a fraction of the label's design size. */
const val FIT_TEXT_MIN_SCALE = 0.7f

/**
 * [Text] that shrinks its font until the string fits the width it was given.
 *
 * Switching locale changes word length ("Add Category" → "Tambah Kategori",
 * "Expense Budgets" → "Anggaran Pengeluaran") while the container width does not, so a
 * label in a bounded box would otherwise clip or push its siblings out of place.
 *
 * Use only where the width is **bounded**: pills, chips, badges, section titles, tab
 * labels, fixed-width button labels. Not for body copy or list rows — auto-sizing runs
 * several layout passes (`TextAutoSize.StepBased` does a binary search), and text with an
 * unbounded width never overflows, so applying it there is pure cost with no effect.
 *
 * The maximum is derived from the caller's own style, so this can only shrink a label —
 * it never grows one past its designed size.
 *
 * @param maxLines lines the text may use before shrinking kicks in. `1` for pills and
 *   titles, `2` for subtitles that should wrap first.
 * @param minScale smallest allowed size as a fraction of [style]'s font size. Below it the
 *   label ellipsizes rather than shrinking further.
 */
@Composable
fun FitText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = 1,
    minScale: Float = FIT_TEXT_MIN_SCALE,
    style: TextStyle = LocalTextStyle.current
) {
    // LocalTextStyle can carry an Unspecified fontSize, and TextAutoSize.StepBased throws
    // IllegalArgumentException on an Unspecified bound — hence the literal fallback.
    val base = (fontSize.takeIf { it.isSpecified } ?: style.fontSize)
        .takeIf { it.isSpecified && it.type == TextUnitType.Sp }
        ?: MaterialTheme.typography.bodyMedium.fontSize

    // Remembered: AutoSizeStepBased is value-compared on a layout hot path, so a fresh
    // instance per recomposition would defeat that.
    val autoSize = remember(base, minScale) {
        TextAutoSize.StepBased(
            minFontSize = base * minScale,
            maxFontSize = base,
            stepSize = 0.25.sp
        )
    }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        autoSize = autoSize,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
        style = style
    )
}

// ─── Add pill ─────────────────────────────────────────────────────────────────

/**
 * The uniform section-header "add" action, used by Expense Budgets, Income
 * Sources, Recurring Payments and Installments.
 * Quiet by design: no pill, no border. `FitText` + a bounded width keeps the
 * longest Indonesian label ("Tambah Kategori") on the title row instead of
 * pushing the action off to the right or wrapping to a second line.
 */
@Composable
fun AddPillButton(
    label: String,
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = contentDescription,
                modifier = Modifier.size(16.dp),
                tint = tint
            )
            FitText(
                text = label,
                modifier = Modifier.widthIn(max = 120.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = tint,
                maxLines = 1
            )
        }
    }
}

/**
 * Section header for a list with an add action, used by Expense Budgets, Income Sources,
 * Recurring Payments and Installments.
 *
 * One component for all four so the title styles and the add action cannot drift
 * apart. The title column takes `weight(1f)` so a long localized title keeps the
 * add action on the same row instead of wrapping to a second line.
 */
@Composable
fun SectionHeaderWithAdd(
    title: String,
    subtitle: String,
    addLabel: String,
    tint: Color,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    addContentDescription: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            FitText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = AppPalette.textPrimary,
                // One line: wrapping to a second line grew the header row and pushed
                // the add action and the list below it down, which read as a broken
                // layout. Shrinking is the locale-proof behaviour; minScale 0.7 keeps
                // the longest Indonesian title legible at 320dp.
                maxLines = 1,
                minScale = 0.7f
            )
            if (subtitle.isNotEmpty()) {
                FitText(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted,
                    maxLines = 1
                )
            }
        }
        AddPillButton(
            label = addLabel,
            onClick = onAdd,
            tint = tint,
            contentDescription = addContentDescription
        )
    }
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
        // weight(fill = false) caps the title at what remains after the badge, so a long
        // localized account-type label can wrap instead of leaving the badge a coerced-to-zero
        // width (RowColumnMeasurePolicy measures unweighted children first).
        Text(
            text = title.uppercase(),
            modifier = Modifier.weight(1f, fill = false),
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
    modifier: Modifier = Modifier,
    hint: String? = null,
    isPlaceholder: Boolean = false
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
                    FitText(
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
