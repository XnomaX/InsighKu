package com.example.insightku.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.insightku.ui.components.main.MainScreen

fun NavGraphBuilder.mainNavGraph(navController: NavHostController) {
    navigation(
        route = Route.MAIN_GRAPH,
        startDestination = Route.MAIN_SCREEN // Single destination for the main part of the app
    ) {
        composable(Route.MAIN_SCREEN) {
            MainScreen(rootNavController = navController) // Pass the root NavController
        }
        // Other main-related destinations like a full-screen transaction details page can go here
        // composable(Route.TRANSACTION_DETAILS) { ... }
    }
}