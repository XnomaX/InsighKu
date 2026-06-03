package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.example.insightku.data.local.preferences.UserPreferencesDataStore
import com.example.insightku.data.repository.TransactionRepository
import com.example.insightku.domain.usecase.auth.LogoutUseCase
import com.example.insightku.domain.usecase.learning.CategoryMemory
import com.example.insightku.ui.components.settings.InputMode
import com.example.insightku.ui.components.settings.SettingsEvent
import com.example.insightku.ui.components.settings.SettingsUiState
import com.example.insightku.ui.theme.InsightTone
import com.example.insightku.ui.theme.VisualDensity
import com.example.insightku.utils.ErrorBus
import com.example.insightku.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SettingsViewModel — orchestrates the Settings screen and persists EVERY preference to DataStore.
 *
 * Design note: the previous version had a class of bug where some toggles updated UI state but were
 * never written to DataStore (e.g. budget alerts), so they silently reset on restart. Every event
 * here both updates state AND persists, so no control is ever inert.
 *
 * Category learning is genuinely real: [CategoryMemory] derives merchant→category associations from
 * the user's actual transactions; forgotten merchants are filtered out via DataStore.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
    private val errorBus: ErrorBus,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val sessionManager: SessionManager,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val categoryMemory = CategoryMemory()

    init {
        onEvent(SettingsEvent.LoadSettings)
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.LoadSettings              -> loadSettings()
            is SettingsEvent.ShowLogoutDialog          -> _uiState.update { it.copy(showLogoutDialog = true) }
            is SettingsEvent.HideLogoutDialog          -> _uiState.update { it.copy(showLogoutDialog = false) }
            is SettingsEvent.ConfirmLogout             -> logout()
            is SettingsEvent.ClearError                -> _uiState.update { it.copy(error = null) }
            is SettingsEvent.OnThemeChange             -> persist({ it.copy(isDarkMode = event.isDarkMode) }) { preferencesDataStore.setDarkMode(event.isDarkMode) }
            is SettingsEvent.OnComfortModeToggle       -> persist({ it.copy(comfortMode = event.enabled) }) { preferencesDataStore.setComfortMode(event.enabled) }
            is SettingsEvent.OnInsightToneChange       -> persist({ it.copy(insightTone = event.tone) }) { preferencesDataStore.setInsightTone(event.tone.key) }
            is SettingsEvent.OnAccentChange            -> persist({ it.copy(accentColor = parseHex(event.hex)) }) { preferencesDataStore.setAccentColor(event.hex) }
            is SettingsEvent.OnVisualDensityChange     -> persist({ it.copy(visualDensity = event.density) }) { preferencesDataStore.setVisualDensity(event.density.key) }
            is SettingsEvent.OnHideAmountsToggle       -> persist({ it.copy(hideAmounts = event.hidden) }) { preferencesDataStore.setHideAmounts(event.hidden) }
            is SettingsEvent.OnHabitGoalChange         -> persist({ it.copy(habitGoal = event.goal) }) { preferencesDataStore.setStreakGoal(event.goal) }
            is SettingsEvent.OnCurrencyChange          -> persist({ it.copy(currencyCode = event.currencyCode) }) { preferencesDataStore.setCurrencyCode(event.currencyCode) }
            is SettingsEvent.OnDefaultInputChange      -> persist({ it.copy(defaultInputMode = event.mode) }) { preferencesDataStore.setDefaultInputMode(event.mode.name.lowercase()) }
            is SettingsEvent.OnPushNotificationsToggle -> persist({ it.copy(pushNotificationsEnabled = event.enabled) }) { preferencesDataStore.setNotificationEnabled(event.enabled) }
            is SettingsEvent.OnSmartCaptureToggle      -> persist({ it.copy(smartCaptureEnabled = event.enabled) }) { preferencesDataStore.setSmartCaptureEnabled(event.enabled) }
            is SettingsEvent.OnCategoryLearningToggle  -> persist({ it.copy(categoryLearningEnabled = event.enabled) }) { preferencesDataStore.setCategoryLearningEnabled(event.enabled) }
            is SettingsEvent.OnForgetMemory            -> forgetMemory(event.merchant)
        }
    }

    /** Update UI state immediately (responsive) and persist to DataStore (durable). */
    private fun persist(reducer: (SettingsUiState) -> SettingsUiState, write: suspend () -> Unit) {
        _uiState.update(reducer)
        viewModelScope.launch { write() }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val (userEmail, userName, _) = sessionManager.getUserData()

            // Reactive: re-emit whenever any preference, the transaction list, or the forgotten set
            // changes. combine caps at a manageable arity, so personalization prefs and learning are
            // grouped into two combines and merged.
            val prefsFlow = combine(
                preferencesDataStore.currencyCode,
                preferencesDataStore.isDarkMode,
                preferencesDataStore.defaultInputMode,
                preferencesDataStore.notificationEnabled,
                preferencesDataStore.comfortMode
            ) { currency, dark, input, notif, comfort ->
                arrayOf(currency, dark, input, notif, comfort)
            }

            val personalizationFlow = combine(
                preferencesDataStore.insightTone,
                preferencesDataStore.streakGoal,
                preferencesDataStore.smartCaptureEnabled,
                preferencesDataStore.categoryLearningEnabled
            ) { tone, goal, smart, learning ->
                arrayOf(tone, goal, smart, learning)
            }

            // Learned memories, with forgotten merchants filtered out.
            val memoriesFlow = combine(
                transactionRepository.getAllTransactions(),
                preferencesDataStore.forgottenMerchants,
                preferencesDataStore.categoryLearningEnabled
            ) { txns, forgotten, learningOn ->
                if (!learningOn) emptyList()
                else categoryMemory.derive(txns).filter { it.merchant.trim().lowercase() !in forgotten }
            }

            // Global appearance/behavior personalization (accent, density, privacy).
            val globalFlow = combine(
                preferencesDataStore.accentColor,
                preferencesDataStore.visualDensity,
                preferencesDataStore.hideAmounts
            ) { accent, density, hide ->
                arrayOf(accent, density, hide)
            }

            combine(prefsFlow, personalizationFlow, memoriesFlow, globalFlow) { prefs, personalization, memories, global ->
                arrayOf(prefs, personalization, memories, global)
            }.collect { merged ->
                @Suppress("UNCHECKED_CAST")
                val prefs = merged[0] as Array<Any>
                @Suppress("UNCHECKED_CAST")
                val personalization = merged[1] as Array<Any>
                @Suppress("UNCHECKED_CAST")
                val memories = merged[2] as List<com.example.insightku.domain.usecase.learning.MerchantMemory>
                @Suppress("UNCHECKED_CAST")
                val global = merged[3] as Array<Any>
                val inputMode = prefs[2] as String
                _uiState.update {
                    it.copy(
                        isLoading                = false,
                        currencyCode             = prefs[0] as String,
                        isDarkMode               = prefs[1] as Boolean,
                        defaultInputMode         = if (inputMode == "ocr") InputMode.OCR else InputMode.MANUAL,
                        pushNotificationsEnabled = prefs[3] as Boolean,
                        comfortMode              = prefs[4] as Boolean,
                        insightTone              = InsightTone.fromKey(personalization[0] as String),
                        habitGoal                = personalization[1] as Int,
                        smartCaptureEnabled      = personalization[2] as Boolean,
                        categoryLearningEnabled  = personalization[3] as Boolean,
                        learnedMemories          = memories,
                        accentColor              = parseHex(global[0] as String),
                        visualDensity            = VisualDensity.fromKey(global[1] as String),
                        hideAmounts              = global[2] as Boolean,
                        userEmail                = userEmail,
                        userName                 = userName
                    )
                }
            }
        }
    }

    private fun forgetMemory(merchant: String) {
        // Optimistic removal; the memoriesFlow will also reflect it once DataStore emits.
        _uiState.update { state ->
            state.copy(learnedMemories = state.learnedMemories.filterNot { it.merchant.equals(merchant, ignoreCase = true) })
        }
        viewModelScope.launch { preferencesDataStore.forgetMerchant(merchant) }
    }

    /** Parse a "#RRGGBB" hex into a Compose Color, falling back to brand purple on any malformed input. */
    private fun parseHex(hex: String): Color =
        runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color(0xFF7C4DFF))

    private fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showLogoutDialog = false) }
            try {
                logoutUseCase().getOrThrow()
                _uiState.value = SettingsUiState(userEmail = "", userName = "", isLoading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = SettingsUiState(userEmail = "", userName = "", isLoading = false)
            }
        }
    }
}
