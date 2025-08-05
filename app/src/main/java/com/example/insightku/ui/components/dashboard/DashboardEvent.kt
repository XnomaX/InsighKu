package com.example.insightku.ui.components.dashboard

sealed class DashboardEvent {
    object LoadDashboardData : DashboardEvent()
    object RefreshData : DashboardEvent()
    object ClearError : DashboardEvent()
    object AddTransaction : DashboardEvent()
    data class ToggleForecastPeriod(val period: String) : DashboardEvent()
}
