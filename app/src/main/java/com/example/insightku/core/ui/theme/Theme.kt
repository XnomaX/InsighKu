package com.example.insightku.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A1080),
    onPrimaryContainer = Color(0xFFE8DDFF),
)

@Composable
fun InsightKuTheme(
    darkTheme: Boolean = false,
    currencyCode: String = "IDR",
    comfortMode: Boolean = false,
    insightTone: InsightTone = InsightTone.WARM,
    accent: Color = Color(0xFF7C4DFF),
    visualDensity: VisualDensity = VisualDensity.COMFORTABLE,
    hideAmounts: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalComfortMode provides comfortMode,
        LocalInsightTone provides insightTone,
        LocalAccent provides accent,
        LocalVisualDensity provides visualDensity,
        LocalHideAmounts provides hideAmounts,
        LocalCurrencyCode provides currencyCode
    ) {
        ProvideResponsiveDimens {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography,
                shapes = Shapes,
                content = content
            )
        }
    }
}
