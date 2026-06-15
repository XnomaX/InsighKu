package com.example.insightku.feature.home.presentation

import com.example.insightku.core.data.model.Installment
import com.example.insightku.core.data.model.RecurringBudget
import com.example.insightku.core.data.model.Transaction

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
    // Draft Inbox: dismiss menandai DISMISSED (hilang dari Inbox) untuk jendela undo;
    // undo mengembalikan PENDING; commitDismiss hard-delete setelah jendela habis.
    data class DismissDraft(val draftId: String) : DashboardEvent()
    data class UndoDismissDraft(val draftId: String) : DashboardEvent()
    data class CommitDismissDraft(val draftId: String) : DashboardEvent()
}


