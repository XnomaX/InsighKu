package com.example.insightku.core.i18n

import android.content.Context
import android.content.res.Configuration
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
     * The app's effective locale: the user-selected per-app language when set,
     * otherwise the device locale. Use for locale-sensitive *formatting* outside
     * a Compose context (ViewModels, domain, workers), where `LocalContext` is
     * unavailable; Compose code keeps using `LocalContext`/`Configuration`.
     * Without this, `NumberFormatter`'s `Locale.getDefault()` default embeds
     * system-locale digits inside app-locale sentences.
     *
     * The returned [Locale] is used as-is by `SimpleDateFormat`/`NumberFormat`, which treat
     * the legacy code "in" as an alias of "id" — but string comparisons do not, so every
     * language gate in this codebase goes through [isIndonesian] rather than comparing
     * `language == "id"` directly.
     */
    fun currentLocale(): Locale {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (!locales.isEmpty) locales[0] ?: Locale.getDefault() else Locale.getDefault()
    }

    /**
     * True when [code] is Indonesian, accepting both the modern ISO 639-1 code and the
     * obsolete one. On API <= 34 a device set to Indonesian can report "in" (see
     * [normalizeLanguageCode]); every language gate in the formatting and copy layers must
     * accept both, or an Indonesian UI silently renders English dates, units and insight
     * sentences. Comparing codes rather than round-tripping a [Locale] keeps this provable
     * on the JVM, where `Locale` constructors convert legacy codes at construction time.
     */
    fun isIndonesianCode(code: String): Boolean = code == "id" || code == "in"

    /** Indonesian regardless of whether the platform reports "id" or the legacy "in". */
    fun isIndonesian(locale: Locale): Boolean = isIndonesianCode(locale.language)


    /**
     * Get the current effective language code (modern ISO 639-1, e.g. "id"/"en").
     *
     * Returns the user-selected language if set, otherwise falls back
     * to the device language if supported, then English.
     */
    fun getCurrentLanguageCode(context: Context): String {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (!currentLocales.isEmpty) {
            return normalizeLanguageCode(currentLocales[0]?.language ?: "en")
        }
        // No explicit selection — detect device language
        val deviceLocale = context.resources.configuration.locales[0]
        val deviceCode = normalizeLanguageCode(deviceLocale?.language ?: "en")
        return if (SUPPORTED_LOCALES.any { it.code == deviceCode }) deviceCode else "en"
    }

    /**
     * Map the pre-Android-15 obsolete language codes to their modern ISO 639-1
     * forms. Android 15 stopped converting Indonesian "id" to the legacy "in"
     * (and Hebrew "he"/"iw", Yiddish "yi"/"ji") for apps targeting API 35+; on
     * API <= 34 a device set to Indonesian reports "in", which never equals the
     * [SUPPORTED_LOCALES] code "id".
     *
     * Without this the device-locale fallback returned "en", so notifications and
     * workers that build their Context through [wrapContext] rendered English for
     * Indonesian-locale users who had never picked a language in-app (the path
     * reached before the first Activity launch calls applyLocale). The mapping is
     * a no-op for codes that are already modern, so it is correct whether or not
     * the platform still applies the legacy conversion.
     */
    internal fun normalizeLanguageCode(code: String): String = when (code) {
        "in" -> "id"   // Indonesian
        "iw" -> "he"   // Hebrew
        "ji" -> "yi"   // Yiddish
        else -> code
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

}

/**
 * Represents a supported locale with metadata for the language picker UI.
 */
data class SupportedLocale(
    val code: String,
    val displayName: String,
    val flag: String
)
