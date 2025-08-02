/*
package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    
    // Theme settings
    private val _theme = MutableStateFlow("light")
    val theme: StateFlow<String> = _theme.asStateFlow()
    
    // Input mode settings
    private val _defaultInputMode = MutableStateFlow("ocr")
    val defaultInputMode: StateFlow<String> = _defaultInputMode.asStateFlow()
    
    // WhatsApp integration
    private val _whatsappEnabled = MutableStateFlow(false)
    val whatsappEnabled: StateFlow<Boolean> = _whatsappEnabled.asStateFlow()
    
    // Notification settings
    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()
    
    private val _budgetAlertsEnabled = MutableStateFlow(true)
    val budgetAlertsEnabled: StateFlow<Boolean> = _budgetAlertsEnabled.asStateFlow()
    
    // Security settings
    private val _biometricsEnabled = MutableStateFlow(false)
    val biometricsEnabled: StateFlow<Boolean> = _biometricsEnabled.asStateFlow()
    
    // User info
    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Simulate loading from SharedPreferences or DataStore
                // In real implementation, use DataStore or SharedPreferences
                
                // Load theme
                _theme.value = "light" // Default or loaded value
                
                // Load input mode
                _defaultInputMode.value = "ocr" // Default or loaded value
                
                // Load WhatsApp setting
                _whatsappEnabled.value = false // Default or loaded value
                
                // Load notification settings
                _notificationsEnabled.value = true // Default or loaded value
                _budgetAlertsEnabled.value = true // Default or loaded value
                
                // Load security settings
                _biometricsEnabled.value = false // Default or loaded value
                
                // Load user info
                _userEmail.value = "user@insightku.com" // Default or loaded value
                
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateTheme(newTheme: String) {
        viewModelScope.launch {
            _theme.value = newTheme
            // Save to SharedPreferences or DataStore
            saveSettingToStorage("theme", newTheme)
        }
    }
    
    fun updateDefaultInputMode(mode: String) {
        viewModelScope.launch {
            _defaultInputMode.value = mode
            saveSettingToStorage("defaultInputMode", mode)
        }
    }
    
    fun updateWhatsAppEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _whatsappEnabled.value = enabled
            saveSettingToStorage("whatsappEnabled", enabled.toString())
        }
    }
    
    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _notificationsEnabled.value = enabled
            saveSettingToStorage("notificationsEnabled", enabled.toString())
        }
    }
    
    fun updateBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _budgetAlertsEnabled.value = enabled
            saveSettingToStorage("budgetAlertsEnabled", enabled.toString())
        }
    }
    
    fun updateBiometricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _biometricsEnabled.value = enabled
            saveSettingToStorage("biometricsEnabled", enabled.toString())
        }
    }
    
    fun updateUserEmail(email: String) {
        viewModelScope.launch {
            _userEmail.value = email
            saveSettingToStorage("userEmail", email)
        }
    }
    
    private suspend fun saveSettingToStorage(key: String, value: String) {
        // Implement saving to SharedPreferences or DataStore
        // For now, just simulate the save operation
        try {
            // SharedPreferences or DataStore save operation
            // Example: dataStore.edit { preferences ->
            //     preferences[stringPreferencesKey(key)] = value
            // }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun resetToDefaults() {
        viewModelScope.launch {
            _theme.value = "light"
            _defaultInputMode.value = "ocr"
            _whatsappEnabled.value = false
            _notificationsEnabled.value = true
            _budgetAlertsEnabled.value = true
            _biometricsEnabled.value = false
            
            // Save all defaults
            saveSettingToStorage("theme", "light")
            saveSettingToStorage("defaultInputMode", "ocr")
            saveSettingToStorage("whatsappEnabled", "false")
            saveSettingToStorage("notificationsEnabled", "true")
            saveSettingToStorage("budgetAlertsEnabled", "true")
            saveSettingToStorage("biometricsEnabled", "false")
        }
    }
    
    fun exportSettings(): Map<String, Any> {
        return mapOf(
            "theme" to _theme.value,
            "defaultInputMode" to _defaultInputMode.value,
            "whatsappEnabled" to _whatsappEnabled.value,
            "notificationsEnabled" to _notificationsEnabled.value,
            "budgetAlertsEnabled" to _budgetAlertsEnabled.value,
            "biometricsEnabled" to _biometricsEnabled.value,
            "userEmail" to _userEmail.value
        )
    }
    
    fun importSettings(settings: Map<String, Any>) {
        viewModelScope.launch {
            settings["theme"]?.let { updateTheme(it as String) }
            settings["defaultInputMode"]?.let { updateDefaultInputMode(it as String) }
            settings["whatsappEnabled"]?.let { updateWhatsAppEnabled(it as Boolean) }
            settings["notificationsEnabled"]?.let { updateNotificationsEnabled(it as Boolean) }
            settings["budgetAlertsEnabled"]?.let { updateBudgetAlertsEnabled(it as Boolean) }
            settings["biometricsEnabled"]?.let { updateBiometricsEnabled(it as Boolean) }
            settings["userEmail"]?.let { updateUserEmail(it as String) }
        }
    }
}*/
