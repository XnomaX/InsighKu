package com.example.insightku.feature.analytics.presentation

import com.example.insightku.feature.analytics.domain.AnalyticsInsights
import com.example.insightku.feature.analytics.domain.AnalyticsPeriodType

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: AnalyticsPeriodType = AnalyticsPeriodType.WEEKLY,
    val insights: AnalyticsInsights? = null,
    val error: String? = null
)
