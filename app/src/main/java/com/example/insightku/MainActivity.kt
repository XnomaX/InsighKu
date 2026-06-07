package com.example.insightku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.core.ui.components.InsightKuApp
import com.example.insightku.core.ui.theme.InsightKuTheme
import com.example.insightku.feature.settings.presentation.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Allow status bar to be drawn behind
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            InsightKuMainApp()
        }
    }
}

@Composable
private fun InsightKuMainApp() {
    // SettingsViewModel menyediakan currencyCode dari DataStore agar
    // theme bisa meneruskannya ke LocalCurrencyCode di seluruh composable tree.
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settingsState by settingsViewModel.uiState.collectAsState()

    InsightKuTheme(
        darkTheme = settingsState.isDarkMode,
        currencyCode = settingsState.currencyCode,
        comfortMode = settingsState.comfortMode,
        insightTone = settingsState.insightTone,
        accent = settingsState.accentColor,
        visualDensity = settingsState.visualDensity,
        hideAmounts = settingsState.hideAmounts
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            InsightKuApp()
        }
    }
}


