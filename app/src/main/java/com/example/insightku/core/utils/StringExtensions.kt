package com.example.insightku.core.utils

/**
 * Normalizes a category or merchant name for consistent comparison.
 * Trims whitespace and converts to lowercase.
 *
 * Used across features (Budgeting, Dashboard, Goals, AutoAllocation)
 * to match category names case-insensitively.
 */
fun String.normalizedCategoryName(): String = trim().lowercase()
