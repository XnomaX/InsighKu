package com.example.insightku.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

@Composable
fun RootNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.AUTH_GRAPH,
        route = "root_graph"
    ) {
        authNavGraph(navController = navController)
        mainNavGraph(navController = navController)
    }
}
