package com.example.insightku.feature.analytics.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Previews driven by the same synthetic scenarios as the in-app debug mode, so Studio and device
 * stay in sync. Light + dark, plus empty/error.
 */
private val previewState = AnalyticsDebugScenarios.all[2] // "high activity"

@Preview(showBackground = true, name = "Analytics — Content (Light)", heightDp = 1600)
@Composable
private fun AnalyticsContentPreview() {
    MaterialTheme { AnalyticsContent(uiState = previewState, onEvent = {}) }
}

@Preview(
    showBackground = true,
    name = "Analytics — Content (Dark)",
    heightDp = 1600,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AnalyticsContentDarkPreview() {
    MaterialTheme { AnalyticsContent(uiState = previewState, onEvent = {}) }
}

@Preview(showBackground = true, name = "Analytics — Empty")
@Composable
private fun AnalyticsEmptyPreview() {
    MaterialTheme { AnalyticsEmpty() }
}

@Preview(showBackground = true, name = "Analytics — Error")
@Composable
private fun AnalyticsErrorPreview() {
    MaterialTheme { AnalyticsErrorState(message = "Couldn't load your insights", onRetry = {}) }
}

