package com.example.insightku.ui.components.dashboard

import com.example.insightku.data.model.Transaction

sealed class DashboardEvent {
    object LoadDashboardData : DashboardEvent()
    object RefreshData : DashboardEvent()
    object ClearError : DashboardEvent()
    object ToggleBalanceVisibility : DashboardEvent()
    data class AddTransaction(val transaction: Transaction) : DashboardEvent()
    data class ToggleForecastPeriod(val period: String) : DashboardEvent()
}
