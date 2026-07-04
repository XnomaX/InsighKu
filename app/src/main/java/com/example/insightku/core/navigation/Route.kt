package com.example.insightku.core.navigation

object Route {
    // Navigation graph routes
    const val AUTH_GRAPH = "auth_graph"
    const val MAIN_GRAPH = "main_graph"
    const val MAIN_SCREEN = "main_screen"
    const val SPLASH = "splash"

    // Screen routes
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"

    const val EDIT_TRANSACTION = "edit_transaction/{transactionId}"
    const val ANALYSIS = "analysis"
    const val ADD_TRANSACTION = "add_transaction"
    const val BUDGETING = "budgeting"
    const val SETTINGS = "settings"
    const val ACCOUNTS = "accounts"
    const val TRANSACTION_DETAILS = "transaction_details"
    const val BANK_WHITELIST = "bank_whitelist"
    const val AUTO_DETECTION_ONBOARDING = "auto_detection_onboarding"

    // Detail routes for allocations
    const val GOAL_DETAIL = "goal_detail/{goalId}"
    const val BUDGET_DETAIL = "budget_detail/{budgetId}"

    // Add transaction pre-filled from notification deep link
    const val ADD_FROM_NOTIFICATION = "add_from_notification?amount={amount}&title={title}&bankName={bankName}&type={type}&timestamp={timestamp}&description={description}"

    // Debug (debug builds only)
    const val NOTIFICATION_DEBUG = "notification_debug"

    // Deep link scheme
    const val DEEP_LINK_SCHEME = "insightku"
    const val DEEP_LINK_ADD_TRANSACTION = "insightku://add-transaction"

    // Helper functions for detail routes
    fun goalDetailRoute(goalId: String) = "goal_detail/$goalId"
    fun budgetDetailRoute(budgetId: String) = "budget_detail/$budgetId"
}
