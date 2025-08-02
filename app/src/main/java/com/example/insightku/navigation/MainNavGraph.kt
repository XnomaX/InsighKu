package com.example.insightku.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation

fun NavGraphBuilder.mainNavGraph(navController: NavHostController) {
    navigation(
        route = Route.MAIN_GRAPH,
        startDestination = Route.HOME
    ) {
        composable(Route.HOME) {
            HomeScreen()
        }
        // Tambahkan layar lain di dalam main graph di sini
        // composable(Route.PROFILE) { ProfileScreen() }
    }
}

@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Home Screen")
    }
}
