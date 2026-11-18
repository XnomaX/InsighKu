package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.feature.planning.goal.domain.model.Goal

@Composable
internal fun ProgressSection(goal: Goal, goalColor: Color, animatedProgress: Float, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    val progress = animatedProgress.coerceIn(0f, 1f)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(modifier = Modifier.padding(Dimens.CardInnerPaddingLarge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.goal_summary_saved),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppPalette.textMuted
                    )
                    Text(
                        text = NumberFormatter.formatCurrencyCompact(goal.currentAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = AppPalette.textPrimary
                    )
                }
                Text(
                    text = "${goal.progressPercent.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = AppPalette.textMuted
                )
            }
            Spacer(Modifier.height(Dimens.PaddingLarge))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.ProgressBarHeight)
                    .clip(RoundedCornerShape(Dimens.PaddingSmall))
                    .background(AppPalette.cardBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(Dimens.PaddingSmall))
                        .background(accent)
                )
            }
            Spacer(Modifier.height(Dimens.PaddingMedium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_of_amount, NumberFormatter.formatCurrencyCompact(goal.targetAmount)),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppPalette.textMuted
                )
                // Signature: remaining amount closes the progress row.
                Text(
                    text = if (goal.isCompleted) {
                        stringResource(R.string.goal_card_completed)
                    } else {
                        stringResource(R.string.goal_card_left, NumberFormatter.formatCurrencyCompact(goal.remainingAmount))
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = AppPalette.textPrimary
                )
            }
        }
    }
}
