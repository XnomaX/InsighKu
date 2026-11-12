package com.example.insightku.feature.planning.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.ui.components.FitText
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent

@Composable
fun PlanningHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onAddClick: (() -> Unit)? = null,
    addLabel: String = ""
) {
    val accent = LocalAccent.current
    val displayLabel = addLabel.ifEmpty { stringResource(R.string.add) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Opaque base first, then the accent wash on top. This header is rendered inside
            // an AnimatedContent crossfade (BudgetingScreen), so a translucent-only background
            // would let the outgoing and incoming variants composite together mid-transition.
            // The solid base makes each variant self-contained.
            .background(AppPalette.background)
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.14f), accent.copy(alpha = 0.0f))
                )
            )
            .padding(horizontal = Dimens.ScreenHorizontalPadding)
            .padding(top = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fixed-height accent spine. Deliberately not IntrinsicSize/fillMaxHeight: the
            // sibling text is auto-sizing FitText, whose intrinsic height is not reliable,
            // and a collapsed spine would render as nothing. A set height always draws.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.35f)))
                    )
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                FitText(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
                FitText(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = AppPalette.textMuted, maxLines = 2)
            }
        }
        if (onAddClick != null) {
            Surface(modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onAddClick() }, shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.10f)) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
                    FitText(
                        text = displayLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
