package com.example.insightku.feature.home.presentation

data class TransactionData(
    val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val description: String,
    val date: String,
    val isIncome: Boolean
)
