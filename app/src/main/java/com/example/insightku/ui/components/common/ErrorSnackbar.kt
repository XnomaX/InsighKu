package com.example.insightku.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.insightku.utils.AppConstants

@Composable
fun ErrorSnackbar(
    message: String,
    onDismiss: () -> Unit
) {
    LaunchedEffect(message) {
        kotlinx.coroutines.delay(AppConstants.ERROR_SNACKBAR_DELAY)
        onDismiss()
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(AppConstants.ERROR_COLOR)
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = Color.White
        )
    }
}
