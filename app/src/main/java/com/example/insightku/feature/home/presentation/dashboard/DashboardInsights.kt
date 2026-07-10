package com.example.insightku.feature.home.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.ui.theme.*

@Composable
fun InsightsSection(
    insightMessages: List<String>,
    modifier: Modifier = Modifier
) {
    if (insightMessages.isEmpty()) return

    val insightConfigs = listOf(
        Pair(IncomeGreen,   Icons.AutoMirrored.Filled.TrendingUp),
        Pair(WarningYellow, Icons.AutoMirrored.Filled.TrendingDown),
        Pair(PurpleViolet,  Icons.Default.CheckCircle)
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.dashboard_insights),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary
                )
                Text(
                    stringResource(R.string.dashboard_financial_pulse),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }
        }
        insightMessages.forEachIndexed { i, msg ->
            val (tint, icon) = insightConfigs.getOrElse(i) { insightConfigs.last() }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(Dimens.CardRadius),
                color    = AppPalette.card,
                border   = BorderStroke(1.dp, tint.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.CardInnerPadding, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(tint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text     = msg,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = AppPalette.textPrimary,
                        modifier = Modifier.weight(1f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
