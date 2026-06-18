package com.example.insightku.feature.analytics.presentation

import com.example.insightku.feature.analytics.domain.AnalyticsPeriodType

sealed class AnalyticsEvent {
    object LoadAnalytics : AnalyticsEvent()
    object RefreshData   : AnalyticsEvent()
    data class SelectPeriod(val period: AnalyticsPeriodType) : AnalyticsEvent()
}
