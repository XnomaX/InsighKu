package com.example.insightku.feature.budgeting.presentation.event

/**
 * User events for the Contribution screen.
 */
sealed class ContributionEvent {

    // ── Initialization ────────────────────────────────────────────────────────────

    data class Initialize(
        val goalId: String,
        val prefillAmount: Double? = null
    ) : ContributionEvent()

    // ── Amount Input ────────────────────────────────────────────────────────────

    data class UpdateAmount(val rawAmount: String) : ContributionEvent()

    data class SelectQuickAmount(val amount: Long) : ContributionEvent()

    data object ClearAmount : ContributionEvent()

    // ── Account Selection ──────────────────────────────────────────────────

    data class SelectAccount(val accountId: String) : ContributionEvent()

    data object ShowAccountPicker : ContributionEvent()

    data object HideAccountPicker : ContributionEvent()

    // ── Notes ───────────────────────────────────────────────────────────────

    data class UpdateNotes(val notes: String) : ContributionEvent()

    // ── Submission ────────────────────────────────────────────────────────

    data object SubmitContribution : ContributionEvent()

    // ── Navigation ───────────────────────────────────────────────────

    data object NavigateBack : ContributionEvent()

    data object NavigateToSuccess : ContributionEvent()

    // ── Error Handling ───────────────────────────────────────────────

    data object ClearError : ContributionEvent()

    data object ClearSnackbar : ContributionEvent()
}
