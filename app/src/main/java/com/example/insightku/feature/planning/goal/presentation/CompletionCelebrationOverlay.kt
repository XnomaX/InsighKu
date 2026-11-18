package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent

/**
 * Quiet confirmation shown when a goal is completed. No trophy, pulse, particles
 * or forced auto-dismiss: the user closes it when ready.
 */
@Composable
fun CompletionCelebrationOverlay(
    goalName: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(Dimens.PaddingExtraLarge),
            shape = RoundedCornerShape(Dimens.CardRadius),
            colors = CardDefaults.cardColors(containerColor = AppPalette.card),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.PaddingExtraLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.StatCardIconBoxSize)
                        .clip(CircleShape)
                        .background(LocalAccent.current.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = AppPalette.textPrimary,
                        modifier = Modifier.size(Dimens.IconSizeLarge)
                    )
                }
                Spacer(Modifier.height(Dimens.PaddingLarge))
                Text(
                    text = stringResource(R.string.goal_celebration_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = AppPalette.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(Dimens.PaddingMedium))
                Text(
                    text = stringResource(R.string.goal_celebration_desc, goalName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppPalette.textMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(Dimens.PaddingExtraLarge))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.ButtonHeightPrimary),
                    shape = RoundedCornerShape(Dimens.ButtonRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalAccent.current,
                        contentColor = AppPalette.textPrimary
                    )
                ) {
                    Text(
                        stringResource(R.string.goal_celebration_dismiss),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
