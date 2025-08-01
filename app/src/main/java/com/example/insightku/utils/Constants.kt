package com.example.insightku.utils

object Constants {
    // App Info
    const val APP_NAME = "InsightKu"
    const val APP_VERSION = "1.0.0"
    
    // Database
    const val DATABASE_NAME = "insightku_database"
    const val DATABASE_VERSION = 1
    
    // SharedPreferences
    const val PREFS_NAME = "insightku_prefs"
    
    // Date Formats
    const val DATE_FORMAT_DISPLAY = "MMM dd, yyyy"
    const val DATE_FORMAT_API = "yyyy-MM-dd"
    const val TIME_FORMAT_DISPLAY = "HH:mm"
    const val DATETIME_FORMAT_DISPLAY = "MMM dd, yyyy HH:mm"
    
    // Currency
    const val CURRENCY_SYMBOL = "$"
    const val CURRENCY_CODE = "USD"
    
    // Streak
    const val MAX_STREAK_DAYS = 365
    const val NOTIFICATION_TIME_HOUR = 19 // 7 PM
    const val NOTIFICATION_TIME_MINUTE = 0
    
    // Transaction Categories
    val DEFAULT_CATEGORIES = listOf(
        "Food & Dining",
        "Transportation",
        "Shopping",
        "Entertainment",
        "Bills & Utilities",
        "Healthcare",
        "Education",
        "Travel",
        "Investment",
        "Other"
    )
    
    // Budget Periods
    const val BUDGET_PERIOD_DAILY = "DAILY"
    const val BUDGET_PERIOD_WEEKLY = "WEEKLY"
    const val BUDGET_PERIOD_MONTHLY = "MONTHLY"
    const val BUDGET_PERIOD_YEARLY = "YEARLY"
    
    // Notification IDs
    const val NOTIFICATION_ID_STREAK_REMINDER = 1001
    const val NOTIFICATION_ID_BUDGET_ALERT = 1002
    const val NOTIFICATION_CHANNEL_ID = "insightku_notifications"
    const val NOTIFICATION_CHANNEL_NAME = "InsightKu Notifications"
    
    // Intent Extra Keys
    const val EXTRA_TRANSACTION_ID = "transaction_id"
    const val EXTRA_CATEGORY_ID = "category_id"
    const val EXTRA_BUDGET_ID = "budget_id"
    
    // File Paths
    const val RECEIPTS_FOLDER = "receipts"
    const val BACKUP_FOLDER = "backups"
    
    // API Endpoints (if needed in future)
    const val BASE_URL = "https://api.insightku.com/"
    const val API_VERSION = "v1"
    
    // Chart Colors
    val CHART_COLORS = listOf(
        "#8B5CF6", "#06B6D4", "#84CC16", "#F59E0B", "#EF4444",
        "#8B5A2B", "#2563EB", "#DC2626", "#059669", "#7C3AED"
    )
    
    // OCR Settings
    const val OCR_MIN_CONFIDENCE = 0.7f
    const val OCR_MAX_RESULTS = 10
    
    // Camera Settings
    const val CAMERA_PERMISSION_REQUEST_CODE = 100
    const val STORAGE_PERMISSION_REQUEST_CODE = 101
}