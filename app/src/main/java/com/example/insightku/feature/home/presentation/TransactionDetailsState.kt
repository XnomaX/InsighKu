package com.example.insightku.feature.home.presentation

import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.Transaction

/**
 * UI state for the transaction details (ledger) screen.
 * Consolidates the previously separate transactions/categories/isLoading flows
 * into a single state holder, consistent with the other features.
 */
data class TransactionDetailsUiState(
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true
)
