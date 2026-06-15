package com.example.insightku.core.utils

object AppConstants {

    // ── App Info ───────────────────────────────────────────────────────────────
    const val APP_NAME = "InsightKu"
    const val APP_VERSION = "1.0.0"

    // ── Database ───────────────────────────────────────────────────────────────
    const val DATABASE_NAME = "insightku_database"
    const val DATABASE_VERSION = 1

    // ── DataStore ──────────────────────────────────────────────────────────────
    const val PREFS_NAME = "insightku_prefs"

    // ── Navigation / UI Dimensions ─────────────────────────────────────────────
    const val BOTTOM_NAVIGATION_HEIGHT = 100
    const val FLOATING_BUTTON_BOTTOM_OFFSET = 24
    const val FLOATING_BUTTON_RIGHT_OFFSET = 6
    const val FLOATING_BUTTON_SIZE = 56
    const val HEADER_TOP_PADDING = 48
    const val HEADER_BOTTOM_PADDING = 48
    const val CARD_ELEVATION = 4
    const val CONTENT_HORIZONTAL_PADDING = 16
    const val CONTENT_VERTICAL_SPACING = 16
    const val CARD_VERTICAL_OFFSET = -24

    // ── Animation / Timing ─────────────────────────────────────────────────────
    const val LOADING_DELAY = 3000L
    const val ERROR_SNACKBAR_DELAY = 3000L

    // ── Colors ─────────────────────────────────────────────────────────────────
    const val PRIMARY_COLOR = 0xFF5A2A82
    const val SUCCESS_COLOR = 0xFF10B981
    const val ERROR_COLOR = 0xFFEF4444
    const val WARNING_COLOR = 0xFFF59E0B

    // ── Date Formats ───────────────────────────────────────────────────────────
    const val DATE_FORMAT_DISPLAY = "MMM dd, yyyy"
    const val DATE_FORMAT_API = "yyyy-MM-dd"
    const val TIME_FORMAT_DISPLAY = "HH:mm"
    const val DATETIME_FORMAT_DISPLAY = "MMM dd, yyyy HH:mm"

    // ── Currency ───────────────────────────────────────────────────────────────
    const val CURRENCY_SYMBOL = "$"
    const val CURRENCY_CODE = "USD"

    // ── Streak / Notifications ─────────────────────────────────────────────────
    const val MAX_STREAK_DAYS = 365
    const val NOTIFICATION_TIME_HOUR = 19
    const val NOTIFICATION_TIME_MINUTE = 0
    const val NOTIFICATION_ID_STREAK_REMINDER = 1001
    const val NOTIFICATION_ID_BUDGET_ALERT = 1002
    const val NOTIFICATION_CHANNEL_ID = "insightku_notifications"
    const val NOTIFICATION_CHANNEL_NAME = "InsightKu Notifications"

    // ── Budget Periods ─────────────────────────────────────────────────────────
    const val BUDGET_PERIOD_DAILY = "DAILY"
    const val BUDGET_PERIOD_WEEKLY = "WEEKLY"
    const val BUDGET_PERIOD_MONTHLY = "MONTHLY"
    const val BUDGET_PERIOD_YEARLY = "YEARLY"

    // ── Intent / Navigation Keys ───────────────────────────────────────────────
    const val EXTRA_TRANSACTION_ID = "transaction_id"
    const val EXTRA_CATEGORY_ID = "category_id"
    const val EXTRA_BUDGET_ID = "budget_id"

    // ── File Paths ─────────────────────────────────────────────────────────────
    const val RECEIPTS_FOLDER = "receipts"
    const val BACKUP_FOLDER = "backups"

    // ── API ────────────────────────────────────────────────────────────────────
    const val BASE_URL = "https://api.insightku.com/"
    const val API_VERSION = "v1"

    // ── Chart Colors ───────────────────────────────────────────────────────────
    val CHART_COLORS = listOf(
        "#8B5CF6", "#06B6D4", "#84CC16", "#F59E0B", "#EF4444",
        "#8B5A2B", "#2563EB", "#DC2626", "#059669", "#7C3AED"
    )

    // ── OCR / Camera ───────────────────────────────────────────────────────────
    const val OCR_MIN_CONFIDENCE = 0.7f
    const val OCR_MAX_RESULTS = 10
    const val CAMERA_PERMISSION_REQUEST_CODE = 100
    const val STORAGE_PERMISSION_REQUEST_CODE = 101

    // ── Demo / Dev ─────────────────────────────────────────────────────────────
    const val DEMO_USER_EMAIL = "user@insightku.com"
    const val DEMO_USER_NAME = "Demo User"
    const val TOKEN_PREFIX = "demo-token-"

    // ── Default Categories ─────────────────────────────────────────────────────
    val DEFAULT_CATEGORIES = listOf(
        "Food & Dining", "Transportation", "Shopping", "Entertainment",
        "Bills & Utilities", "Healthcare", "Education", "Travel",
        "Investment", "Other"
    )
}
