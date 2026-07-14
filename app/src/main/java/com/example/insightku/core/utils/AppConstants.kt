package com.example.insightku.core.utils

object AppConstants {

    // ── Animation / Timing ─────────────────────────────────────────────────────
    const val ERROR_SNACKBAR_DELAY = 3000L

    // ── Colors ─────────────────────────────────────────────────────────────────
    const val ERROR_COLOR = 0xFFEF4444

    // ── Date Validation ────────────────────────────────────────────────────────
    /**
     * Epoch cutoff in milliseconds (Jan 1, 2000).
     * Timestamps before this are treated as invalid/missing.
     */
    const val EPOCH_CUTOFF_MS = 946684800000L
}
