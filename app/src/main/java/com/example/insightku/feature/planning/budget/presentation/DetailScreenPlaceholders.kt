package com.example.insightku.feature.planning.budget.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette

@Composable
fun GoalDetailScreenPlaceholder(goalId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(AppPalette.background)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = AppPalette.textPrimary) }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Goal Detail", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AppPalette.card)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Goal Detail", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Goal ID: $goalId", style = MaterialTheme.typography.bodyMedium, color = AppPalette.textMuted)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Full goal detail screen with contributions, progress, and actions will be shown here.", style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted)
                }
            }
        }
    }
}

