package com.example.insightku

import android.content.Intent
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.core.notification.NotificationTransactionData
import com.example.insightku.core.ui.components.InsightKuApp
import com.example.insightku.core.ui.theme.InsightKuTheme
import com.example.insightku.feature.settings.presentation.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // MutableState so onNewIntent can update it and trigger recomposition
    // without calling setContent() again (which creates a conflicting second tree).
    private val currentIntent = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        currentIntent.value = intent

        setContent {
            val intentState by currentIntent
            InsightKuMainApp(intent = intentState)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Update state — triggers recomposition of the EXISTING tree, not a new one
        currentIntent.value = intent
    }
}

@Composable
private fun InsightKuMainApp(intent: Intent?) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settingsState by settingsViewModel.uiState.collectAsState()

    val notificationData = remember(intent?.data) {
        extractNotificationData(intent)
    }

    InsightKuTheme(
        darkTheme     = settingsState.isDarkMode,
        currencyCode  = settingsState.currencyCode,
        comfortMode   = settingsState.comfortMode,
        insightTone   = settingsState.insightTone,
        accent        = settingsState.accentColor,
        visualDensity = settingsState.visualDensity,
        hideAmounts   = settingsState.hideAmounts
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = MaterialTheme.colorScheme.background
        ) {
            InsightKuApp(notificationData = notificationData)
        }
    }
}

private fun extractNotificationData(intent: Intent?): NotificationTransactionData? {
    val data = intent?.data ?: return null
    if (data.scheme != "insightku" || data.host != "add-transaction") return null
    val amount      = data.getQueryParameter("amount")?.toDoubleOrNull() ?: return null
    val title       = data.getQueryParameter("title")       ?: ""
    val bankName    = data.getQueryParameter("bankName")    ?: ""
    val type        = data.getQueryParameter("type")        ?: ""
    val timestamp   = data.getQueryParameter("timestamp")?.toLongOrNull() ?: System.currentTimeMillis()
    val description = data.getQueryParameter("description") ?: ""
    val draftId     = data.getQueryParameter("draftId")
    return NotificationTransactionData(
        amount      = amount,
        title       = title,
        bankName    = bankName,
        typeHint    = type,
        timestamp   = timestamp,
        description = description,
        draftId     = draftId
    )
}
