package com.example.insightku.core.navigation

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.core.ui.components.ErrorSnackbar
import com.example.insightku.core.ui.components.RootViewModel

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.insightku.core.ui.components.splash.SplashScreen

import com.example.insightku.core.notification.NotificationTransactionData

@Composable
fun RootNavGraph(notificationData: NotificationTransactionData? = null) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    /**
     * PERBAIKAN KRITIS: Ganti viewModel() → hiltViewModel()
     *
     * SEBELUMNYA (salah):
     * `val rootViewModel: RootViewModel = viewModel()`
     * `viewModel()` membuat instance baru yang dikelola Compose runtime (ViewModelStoreOwner Activity).
     * Sementara Hilt membuat instance berbeda lewat RootViewModelModule yang sudah dihapus.
     * Dua instance berbeda → LoginViewModel dan ViewModel lain emit error ke instance A,
     * tapi RootNavGraph mengobservasi instance B → snackbar tidak pernah muncul.
     *
     * SEKARANG (benar):
     * `hiltViewModel()` mendapatkan instance yang dikelola Hilt dengan lifecycle Activity.
     * Semua ViewModel yang inject ErrorBus mengirim error ke ErrorBus (Singleton),
     * RootViewModel mengobservasi ErrorBus dan expose ke sini via globalError StateFlow.
     * Satu instance, satu sumber kebenaran.
     */
    val rootViewModel: RootViewModel = hiltViewModel()
    val globalError by rootViewModel.globalError.collectAsState()

    LaunchedEffect(globalError) {
        globalError?.let {
            snackbarHostState.showSnackbar(it)
            rootViewModel.clearGlobalError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // WindowInsets.None: kita kelola sendiri di tiap screen/graph
        // agar tidak double-padding antara Scaffold dan content
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Route.SPLASH,
            route = "root_graph",
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Splash Screen — Protected Route Controller
            composable(Route.SPLASH) {
                SplashScreen(
                    onNavigateToAuth = {
                        navController.navigate(Route.AUTH_GRAPH) {
                            popUpTo(Route.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Route.MAIN_GRAPH) {
                            popUpTo(Route.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            // Auth Graph — untuk user yang belum login
            authNavGraph(navController = navController)

            // Main Graph — untuk user yang sudah login
            mainNavGraph(navController = navController, notificationData = notificationData)
        }
    }
}

