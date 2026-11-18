package com.example.insightku.feature.planning.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.insightku.R
import com.example.insightku.core.ui.components.FitText
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent

/**
 * Page header for the Planning tabs (Anggaran / Tujuan).
 *
 * Quiet by design: solid page background, no decorative wash, no accent spine.
 * The title carries the hierarchy; the accent only appears in the optional
 * add action.
 */
@Composable
fun PlanningHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onAddClick: (() -> Unit)? = null,
    addLabel: String = "",
    showAddAction: Boolean = true
) {
    val accent = LocalAccent.current
    val displayLabel = addLabel.ifEmpty { stringResource(R.string.add) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Solid page background so the AnimatedContent crossfade in
            // BudgetingScreen never composites two translucent variants.
            .background(AppPalette.background)
            .padding(horizontal = Dimens.ScreenHorizontalPadding)
            .padding(top = Dimens.CardInnerPaddingLarge, bottom = Dimens.CardInnerPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
        ) {
            FitText(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = AppPalette.textPrimary
            )
            FitText(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.textMuted,
                maxLines = 2
            )
        }
        if (onAddClick != null && showAddAction) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.CornerRadiusSmall))
                    .clickable { onAddClick() },
                shape = RoundedCornerShape(Dimens.CornerRadiusSmall),
                color = accent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Dimens.PaddingMedium, vertical = Dimens.PaddingMedium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = AppPalette.textPrimary,
                        modifier = Modifier.size(Dimens.IconSizeSmall)
                    )
                    FitText(
                        text = displayLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = AppPalette.textPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
