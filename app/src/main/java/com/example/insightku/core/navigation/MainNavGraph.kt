package com.example.insightku.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.insightku.core.notification.NotificationTransactionData
import com.example.insightku.core.ui.components.MainScreen

fun NavGraphBuilder.mainNavGraph(
    navController: NavHostController,
    notificationData: NotificationTransactionData? = null,
    allocationDraftId: String? = null
) {
    navigation(
        route            = Route.MAIN_GRAPH,
        startDestination = Route.MAIN_SCREEN
    ) {
        composable(Route.MAIN_SCREEN) {
            MainScreen(
                rootNavController = navController,
                notificationData  = notificationData,
                allocationDraftId = allocationDraftId
            )
        }
    }
}

