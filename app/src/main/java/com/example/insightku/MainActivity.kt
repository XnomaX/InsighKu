package com.example.insightku

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.example.insightku.core.data.local.preferences.UserPreferencesDataStore
import com.example.insightku.core.i18n.LocaleHelper
import com.example.insightku.core.notification.NotificationTransactionData
import com.example.insightku.core.ui.components.InsightKuApp
import com.example.insightku.core.ui.theme.InsightKuTheme
import com.example.insightku.core.utils.enableHighestRefreshRate
import com.example.insightku.feature.settings.presentation.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var preferencesDataStore: UserPreferencesDataStore

    // MutableState so onNewIntent can update it and trigger recomposition
    // without calling setContent() again (which creates a conflicting second tree).
    private val currentIntent = mutableStateOf<Intent?>(null)
    private val currentAllocationDraftId = mutableStateOf<String?>(null)

    // System clamps high refresh under battery saver; re-request when it toggles.
    private val powerSaveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            enableHighestRefreshRate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Prefer highest Display.Mode BEFORE setContent so the first Compose
        // frames already run at 90/120Hz on capable devices.
        enableHighestRefreshRate()

        // Apply the user's persisted language choice on startup. Requires
        // AppCompatActivity — AppCompatDelegate.setApplicationLocales() is a
        // visual no-op on a plain ComponentActivity (nothing applies the locale
        // to resources, so stringResource never switches).
        lifecycleScope.launch {
            LocaleHelper.applyLocale(preferencesDataStore.appLanguage.first())
        }

        currentIntent.value = intent
        currentAllocationDraftId.value = extractAllocationDraftId(intent)

        setContent {
            val intentState by currentIntent
            val allocDraftId by currentAllocationDraftId
            InsightKuMainApp(intent = intentState, allocationDraftId = allocDraftId)
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        ContextCompat.registerReceiver(
            this,
            powerSaveReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onResume() {
        super.onResume()
        // Re-apply after pause (battery saver exit, fold/unfold, multi-window).
        enableHighestRefreshRate()
    }

    override fun onStop() {
        try {
            unregisterReceiver(powerSaveReceiver)
        } catch (_: IllegalArgumentException) {
            // already unregistered
        }
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Update state — triggers recomposition of the EXISTING tree, not a new one
        currentIntent.value = intent
        currentAllocationDraftId.value = extractAllocationDraftId(intent)
    }
}

@Composable
private fun InsightKuMainApp(intent: Intent?, allocationDraftId: String? = null) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val notificationData = remember(intent?.data) {
        extractNotificationData(intent)
    }

    InsightKuTheme(
        darkTheme     = settingsState.isDarkMode,
        currencyCode  = settingsState.currencyCode,
        comfortMode   = settingsState.comfortMode,
        insightTone   = settingsState.insightTone,
        accent        = Color(android.graphics.Color.parseColor(settingsState.accentColorHex)),
        visualDensity = settingsState.visualDensity,
        hideAmounts   = settingsState.hideAmounts
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = MaterialTheme.colorScheme.background
        ) {
            InsightKuApp(notificationData = notificationData, allocationDraftId = allocationDraftId)
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

private fun extractAllocationDraftId(intent: Intent?): String? {
    val data = intent?.data ?: return null
    if (data.scheme != "insightku" || data.host != "allocation-draft") return null
    return data.getQueryParameter("draftId")
}
