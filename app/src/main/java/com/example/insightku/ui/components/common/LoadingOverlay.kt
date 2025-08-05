package com.example.insightku.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.insightku.utils.AppConstants

@Composable
fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = AppConstants.CARD_ELEVATION.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = Color(AppConstants.PRIMARY_COLOR)
                )
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
