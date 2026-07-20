package com.example.insightku.core.utils

/**
 * Normalizes a category or merchant name for consistent comparison.
 * Trims whitespace and converts to lowercase.
 *
 * Used across features (Budgeting, Dashboard, Goals, AutoAllocation)
 * to match category names case-insensitively.
 */
fun String.normalizedCategoryName(): String = trim().lowercase()

/**
 * Parse a user-entered amount string (which may contain thousands separators)
 * into a Double, keeping only digits. Returns 0.0 when empty/invalid.
 * e.g. "1.500" -> 1500.0, "" -> 0.0
 */
fun String.toAmountOrZero(): Double = filter { it.isDigit() }.toLongOrNull()?.toDouble() ?: 0.0

/**
 * Like [toAmountOrZero] but returns null when empty/invalid (for optional amounts).
 */
fun String.toAmountOrNull(): Double? = filter { it.isDigit() }.toLongOrNull()?.toDouble()

/**
 * Keep only digits and parse to Long, or [default] when empty/invalid.
 */
fun String.digitsToLong(default: Long = 0L): Long = filter { it.isDigit() }.toLongOrNull() ?: default

/**
 * Keep only digits and parse to Int, or [default] when empty/invalid.
 */
fun String.digitsToInt(default: Int = 0): Int = filter { it.isDigit() }.toIntOrNull() ?: default
