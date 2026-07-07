package com.example.insightku.feature.planning.goal.presentation

sealed class ContributionEvent {
    data class Initialize(val goalId: String, val prefillAmount: Double? = null) : ContributionEvent()
    data class LoadGoal(val goalId: String) : ContributionEvent()
    data class UpdateAmount(val rawAmount: String) : ContributionEvent()
    data class SelectQuickAmount(val amount: Long) : ContributionEvent()
    data object ClearAmount : ContributionEvent()
    data class UpdateNotes(val notes: String) : ContributionEvent()
    data class SelectAccount(val accountId: String) : ContributionEvent()
    data object ShowAccountPicker : ContributionEvent()
    data object HideAccountPicker : ContributionEvent()
    data object SubmitContribution : ContributionEvent()
    data object SubmitWithdrawal : ContributionEvent()
    data object NavigateBack : ContributionEvent()
    data object NavigateToSuccess : ContributionEvent()
    data object ClearError : ContributionEvent()
    data object ClearSnackbar : ContributionEvent()
    data object Dismiss : ContributionEvent()
    data class SetMode(val isWithdraw: Boolean) : ContributionEvent()
}
