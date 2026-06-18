package com.example.insightku.feature.analytics.presentation

import com.example.insightku.feature.analytics.domain.AnalyticsPeriodType

data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val selectedPeriod: AnalyticsPeriodType = AnalyticsPeriodType.WEEKLY
)
