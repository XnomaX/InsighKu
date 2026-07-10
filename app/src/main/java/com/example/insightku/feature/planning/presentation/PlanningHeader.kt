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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
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
        modifier = modifier.fillMaxWidth().background(AppPalette.background).padding(horizontal = Dimens.ScreenHorizontalPadding).padding(top = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = AppPalette.textMuted)
        }
        if (onAddClick != null) {
            Surface(modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onAddClick() }, shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.10f)) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
                    Text(text = displayLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = accent)
                }
            }
        }
    }
}
