package com.example.insightku.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.insightku.ui.components.main.MainScreen

fun NavGraphBuilder.mainNavGraph(navController: NavHostController) {
    navigation(
        route = Route.MAIN_GRAPH,
        startDestination = Route.HOME
    ) {
        composable(Route.HOME) {
            MainScreen(
                onLogout = {
                    // Navigate ke Splash dan clear session
                    navController.navigate(Route.SPLASH) {
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}
