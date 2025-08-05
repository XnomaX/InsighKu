package com.example.insightku.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.insightku.ui.components.splash.SplashScreen

@Composable
fun RootNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.SPLASH,
        route = "root_graph"
    ) {
        // Splash Screen as entry point - Protected Route Controller
        composable(Route.SPLASH) {
            SplashScreen(
                onNavigateToAuth = {
                    // Navigate ke AuthGraph jika user belum login
                    navController.navigate(Route.AUTH_GRAPH) {
                        popUpTo(Route.SPLASH) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToHome = {
                    // Navigate ke MainGraph jika user sudah login
                    navController.navigate(Route.MAIN_GRAPH) {
                        popUpTo(Route.SPLASH) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // Auth Graph - untuk user yang belum login
        authNavGraph(navController = navController)

        // Main Graph - untuk user yang sudah login (Protected Routes)
        mainNavGraph(navController = navController)
    }
}
