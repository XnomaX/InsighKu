package com.example.insightku.ui.components.analytics

import com.example.insightku.ui.components.analytics.model.TimePeriod

sealed class AnalyticsEvent {
    object LoadAnalytics : AnalyticsEvent()
    object RefreshData : AnalyticsEvent()
    data class SelectMonth(val month: String) : AnalyticsEvent()
    object PreviousMonth : AnalyticsEvent()
    object NextMonth : AnalyticsEvent()
    data class SelectExpenseCategory(val category: String?) : AnalyticsEvent()
    data class SelectIncomeCategory(val category: String?) : AnalyticsEvent()
    data class ChangeTimePeriod(val period: TimePeriod) : AnalyticsEvent()
    data class ChangeBudgetPeriod(val period: TimePeriod) : AnalyticsEvent()
}
