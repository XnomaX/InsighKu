package com.example.insightku.ui

import androidx.compose.runtime.Composable
import com.example.insightku.navigation.RootNavGraph
import dagger.hilt.android.AndroidEntryPoint

@Composable
fun InsightKuApp() {
    // Menggunakan RootNavGraph yang sudah terintegrasi dengan Protected Route
    RootNavGraph()
}
