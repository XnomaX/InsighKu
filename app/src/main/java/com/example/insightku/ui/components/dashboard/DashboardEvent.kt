package com.example.insightku.ui.components.dashboard

import com.example.insightku.data.model.Installment
import com.example.insightku.data.model.RecurringBudget
import com.example.insightku.data.model.Transaction

sealed class DashboardEvent {
    object LoadDashboardData : DashboardEvent()
    object RefreshData : DashboardEvent()
    object ClearError : DashboardEvent()
    object ToggleBalanceVisibility : DashboardEvent()
    object UseStreakRepair : DashboardEvent()
    data class AddTransaction(val transaction: Transaction) : DashboardEvent()
    data class ToggleForecastPeriod(val period: String) : DashboardEvent()
    data class MarkRecurringPaid(val budget: RecurringBudget) : DashboardEvent()
    data class MarkInstallmentPaid(val installment: Installment) : DashboardEvent()
    data class SetStreakGoal(val days: Int) : DashboardEvent()
}
