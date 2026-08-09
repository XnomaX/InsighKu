package com.example.insightku

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.insightku.core.data.local.preferences.UserPreferencesDataStore
import com.example.insightku.core.i18n.LocaleHelper
import com.example.insightku.core.notification.NotificationTransactionData
import com.example.insightku.core.ui.components.InsightKuApp
import com.example.insightku.core.ui.theme.InsightKuTheme
import com.example.insightku.core.ui.theme.InsightTone
import com.example.insightku.core.ui.theme.VisualDensity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
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

    // Theme-only prefs — avoids constructing SettingsViewModel (+ LogoutUseCase → Room) on first frame.
    private data class ThemePrefs(
        val isDarkMode: Boolean = false,
        val currencyCode: String = "IDR",
        val comfortMode: Boolean = false,
        val insightTone: InsightTone = InsightTone.WARM,
        val accentColorHex: String = "#7C4DFF",
        val visualDensity: VisualDensity = VisualDensity.COMFORTABLE,
        val hideAmounts: Boolean = false,
    )

    private val themePrefs by lazy {
        // Typed combine tops out at 5 flows; use Array form like SettingsViewModel.
        combine<Any, ThemePrefs>(
            preferencesDataStore.isDarkMode,
            preferencesDataStore.currencyCode,
            preferencesDataStore.comfortMode,
            preferencesDataStore.insightTone,
            preferencesDataStore.accentColor,
            preferencesDataStore.visualDensity,
            preferencesDataStore.hideAmounts,
        ) { values ->
            ThemePrefs(
                isDarkMode = values[0] as Boolean,
                currencyCode = values[1] as String,
                comfortMode = values[2] as Boolean,
                insightTone = InsightTone.fromKey(values[3] as String),
                accentColorHex = values[4] as String,
                visualDensity = VisualDensity.fromKey(values[5] as String),
                hideAmounts = values[6] as Boolean,
            )
        }.stateIn(
            scope = lifecycleScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemePrefs(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate — bridges process start → first Compose frame.
        val splashScreen = installSplashScreen()
        // Auth gate is Compose SplashScreen; do not hold system splash for routing.
        splashScreen.setKeepOnScreenCondition { false }
        // Instant remove — default scale-out makes system→Compose read as two screens.
        splashScreen.setOnExitAnimationListener { provider -> provider.remove() }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

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
            val theme by themePrefs.collectAsStateWithLifecycle()
            InsightKuMainApp(
                intent = intentState,
                allocationDraftId = allocDraftId,
                theme = theme,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Update state — triggers recomposition of the EXISTING tree, not a new one
        currentIntent.value = intent
        currentAllocationDraftId.value = extractAllocationDraftId(intent)
    }

    @Composable
    private fun InsightKuMainApp(
        intent: Intent?,
        allocationDraftId: String? = null,
        theme: ThemePrefs,
    ) {
        val notificationData = remember(intent?.data) {
            extractNotificationData(intent)
        }

        InsightKuTheme(
            darkTheme = theme.isDarkMode,
            currencyCode = theme.currencyCode,
            comfortMode = theme.comfortMode,
            insightTone = theme.insightTone,
            accent = Color(theme.accentColorHex.toColorInt()),
            visualDensity = theme.visualDensity,
            hideAmounts = theme.hideAmounts,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                InsightKuApp(
                    notificationData = notificationData,
                    allocationDraftId = allocationDraftId,
                )
            }
        }
    }

    private fun extractNotificationData(intent: Intent?): NotificationTransactionData? {
        val data = intent?.data ?: return null
        if (data.scheme != "insightku" || data.host != "add-transaction") return null
        val amount = data.getQueryParameter("amount")?.toDoubleOrNull() ?: return null
        val title = data.getQueryParameter("title") ?: ""
        val bankName = data.getQueryParameter("bankName") ?: ""
        val type = data.getQueryParameter("type") ?: ""
        val timestamp = data.getQueryParameter("timestamp")?.toLongOrNull()
            ?: System.currentTimeMillis()
        val description = data.getQueryParameter("description") ?: ""
        val draftId = data.getQueryParameter("draftId")
        return NotificationTransactionData(
            amount = amount,
            title = title,
            bankName = bankName,
            typeHint = type,
            timestamp = timestamp,
            description = description,
            draftId = draftId,
        )
    }

    private fun extractAllocationDraftId(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (data.scheme != "insightku" || data.host != "allocation-draft") return null
        return data.getQueryParameter("draftId")
    }
}
