package com.example.insightku.core.i18n

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Manages dynamic locale switching across the application.
 *
 * Uses the AndroidX AppCompat per-app language API (API 33+ on Android 13+,
 * backported via AppCompatDelegate for older devices). This approach:
 *   - Applies immediately without Activity recreation
 *   - Persists across app restarts (handled by AppCompat internally for API 33+)
 *   - Works with Compose string resources automatically
 */
object LocaleHelper {

    /** Supported languages in the application. */
    val SUPPORTED_LOCALES = listOf(
        SupportedLocale(code = "id", displayName = "Bahasa Indonesia", flag = "🇮🇩"),
        SupportedLocale(code = "en", displayName = "English", flag = "🇺🇸")
    )

    /**
     * Apply a locale by language code.
     *
     * @param languageCode ISO 639-1 code (e.g. "id", "en")
     */
    fun applyLocale(languageCode: String) {
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /**
     * Get the current effective language code.
     *
     * Returns the user-selected language if set, otherwise falls back
     * to the device language if supported, then English.
     */
    fun getCurrentLanguageCode(context: Context): String {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (!currentLocales.isEmpty) {
            return currentLocales[0]?.language ?: "en"
        }
        // No explicit selection — detect device language
        val deviceLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        val deviceCode = deviceLocale?.language ?: "en"
        return if (SUPPORTED_LOCALES.any { it.code == deviceCode }) deviceCode else "en"
    }

    /**
     * Wrap a [Context] with the current application locale.
     *
     * Use this in non-Composable contexts (Services, Workers, Notifications)
     * to ensure string resources are resolved in the correct locale.
     */
    fun wrapContext(context: Context): Context {
        val localeCode = getCurrentLanguageCode(context)
        val locale = Locale.forLanguageTag(localeCode)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    /**
     * Get the display name for a language code in the current locale.
     */
    fun getDisplayName(languageCode: String): String {
        val locale = Locale.forLanguageTag(languageCode)
        return locale.getDisplayLanguage(locale).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }
    }
}

/**
 * Represents a supported locale with metadata for the language picker UI.
 */
data class SupportedLocale(
    val code: String,
    val displayName: String,
    val flag: String
)
