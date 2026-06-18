package com.example.insightku.feature.settings.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Spa
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.feature.home.domain.MerchantMemory
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.InsightTone
import com.example.insightku.core.ui.theme.LocalResponsiveDimens
import com.example.insightku.core.ui.theme.VisualDensity
import com.example.insightku.core.utils.CurrencyUtils
import com.example.insightku.feature.settings.presentation.SettingsViewModel

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onNavigateToNotificationDebug: (() -> Unit)? = null,
    onNavigateToBankWhitelist: (() -> Unit)? = null,
    onNavigateToAutoDetectionOnboarding: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.userEmail) {
        if (!uiState.isLoading && uiState.userEmail.isEmpty()) onLogout()
    }

    if (uiState.showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = { viewModel.onEvent(SettingsEvent.ConfirmLogout) },
            onDismiss = { viewModel.onEvent(SettingsEvent.HideLogoutDialog) }
        )
    }

    val dimens = LocalResponsiveDimens.current
    val onEvent = viewModel::onEvent

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsPalette.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = dimens.screenHorizontalPadding,
            end = dimens.screenHorizontalPadding,
            top = Dimens.PaddingExtraLarge,
            bottom = Dimens.ContentBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing)
    ) {
        item { IdentityHeader(uiState) }
        item { AppearanceSection(uiState, onEvent) }
        item { AccentSection(uiState, onEvent) }
        item { ComfortSection(uiState, onEvent) }
        item { VisualDensitySection(uiState, onEvent) }
        item { InsightToneSection(uiState, onEvent) }
        item { HabitGoalSection(uiState, onEvent) }
        item { CurrencySection(uiState, onEvent) }
        item { SmartCaptureSection(uiState, onEvent, onNavigateToBankWhitelist, onNavigateToAutoDetectionOnboarding) }
        if (onNavigateToNotificationDebug != null) {
            item {
                SettingsSurface {
                    Row(
                        Modifier.fillMaxWidth().clickable { onNavigateToNotificationDebug() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = null,
                            tint     = Color(0xFF7C4DFF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Notification Debug", style = MaterialTheme.typography.bodyLarge, color = SettingsPalette.textPrimary)
                            Text("Lihat log notifikasi bank yang diterima", style = MaterialTheme.typography.bodySmall, color = SettingsPalette.textMuted)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SettingsPalette.textMuted)
                    }
                }
            }
        }
        item { AccountSection(uiState, onEvent) }
        item { AppInfoFooter() }
    }
}

// ── Shared shells ───────────────────────────────────────────────────────────────

@Composable
private fun SettingsSurface(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        color = SettingsPalette.card,
        border = BorderStroke(1.dp, SettingsPalette.cardBorder),
        tonalElevation = 0.dp,
        shadowElevation = Dimens.ElevationSmall
    ) {
        Column(Modifier.padding(Dimens.CardInnerPaddingLarge), content = content)
    }
}

@Composable
private fun SectionLabel(icon: ImageVector, title: String, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                .background(SettingsPalette.tint(SettingsPalette.Purple)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = SettingsPalette.Purple, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(Dimens.PaddingMedium))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SettingsPalette.textPrimary)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SettingsPalette.textMuted)
        }
    }
}

@Composable
private fun ToneSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = SettingsPalette.Purple,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = SettingsPalette.textMuted.copy(alpha = 0.4f)
        )
    )
}

// ── 1. Identity header ────────────────────────────────────────────────────────

@Composable
private fun IdentityHeader(uiState: SettingsUiState) {
    Column {
        Text(
            if (uiState.userName.isNotBlank()) "Hey, ${uiState.userName.substringBefore(' ')}" else "Your space",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = SettingsPalette.textPrimary
        )
        Spacer(Modifier.height(Dimens.PaddingSmall))
        Text(
            "Shape how InsightKu feels and how it gets to know you 💜",
            style = MaterialTheme.typography.bodyMedium,
            color = SettingsPalette.textMuted
        )
    }
}

// ── 2. Appearance + live preview ──────────────────────────────────────────────

@Composable
private fun AppearanceSection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsSurface {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(
                if (uiState.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                "Appearance",
                if (uiState.isDarkMode) "Dark and easy on the eyes" else "Light and airy"
            )
            Spacer(Modifier.weight(1f))
            ToneSwitch(uiState.isDarkMode) { onEvent(SettingsEvent.OnThemeChange(it)) }
        }
        Spacer(Modifier.height(Dimens.PaddingLarge))
        // Live mini-preview — a tiny mock card that recolors with the toggle.
        val previewBg by animateColorAsState(if (uiState.isDarkMode) Color(0xFF1A1030) else Color.White, label = "previewBg")
        val previewText by animateColorAsState(if (uiState.isDarkMode) Color(0xFFEDE9FE) else Color(0xFF1A1A2E), label = "previewText")
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
            color = previewBg,
            border = BorderStroke(1.dp, SettingsPalette.cardBorder)
        ) {
            Row(Modifier.padding(Dimens.CardInnerPadding), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(SettingsPalette.Purple))
                Spacer(Modifier.width(Dimens.PaddingMedium))
                Column {
                    Text("Preview", style = MaterialTheme.typography.labelSmall, color = SettingsPalette.textMuted)
                    Text("This is how cards will look", style = MaterialTheme.typography.bodyMedium, color = previewText)
                }
            }
        }
    }
}

// ── 3. Comfort mode ────────────────────────────────────────────────────────────

@Composable
private fun ComfortSection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsSurface {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(Icons.Default.Spa, "Comfort mode", "Softer motion, calmer pace")
            Spacer(Modifier.weight(1f))
            ToneSwitch(uiState.comfortMode) { onEvent(SettingsEvent.OnComfortModeToggle(it)) }
        }
        AnimatedVisibility(uiState.comfortMode, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Text(
                "Animations across the app are gentled — things settle softly instead of sliding in.",
                style = MaterialTheme.typography.bodySmall,
                color = SettingsPalette.Purple,
                modifier = Modifier.padding(top = Dimens.PaddingMedium)
            )
        }
    }
}

// ── 4. How insights talk to you + live preview ─────────────────────────────────

@Composable
private fun InsightToneSection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsSurface {
        SectionLabel(Icons.Default.AutoAwesome, "How insights talk to you", "Pick the voice that feels right")
        Spacer(Modifier.height(Dimens.PaddingLarge))
        // Segmented pills — not a tab bar, an in-place voice picker.
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)) {
            InsightTone.entries.forEach { tone ->
                val selected = uiState.insightTone == tone
                val bg by animateColorAsState(if (selected) SettingsPalette.Purple else SettingsPalette.tint(SettingsPalette.Purple), label = "tonebg")
                Surface(
                    Modifier.weight(1f).clickable { onEvent(SettingsEvent.OnInsightToneChange(tone)) },
                    shape = RoundedCornerShape(50),
                    color = bg
                ) {
                    Text(
                        tone.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Color.White else SettingsPalette.Purple,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.PaddingMedium)
                    )
                }
            }
        }
        Spacer(Modifier.height(Dimens.PaddingLarge))
        // Live preview — the SAME insight rendered in the chosen tone.
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
            color = SettingsPalette.cardElevated
        ) {
            Row(Modifier.padding(Dimens.CardInnerPadding), verticalAlignment = Alignment.Top) {
                Text("🔮", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(Dimens.PaddingMedium))
                Column {
                    Text("Sounds like", style = MaterialTheme.typography.labelSmall, color = SettingsPalette.textMuted)
                    Spacer(Modifier.height(2.dp))
                    Text(uiState.insightTone.previewSentence, style = MaterialTheme.typography.bodyMedium, color = SettingsPalette.textPrimary)
                }
            }
        }
    }
}

private val InsightTone.label: String
    get() = when (this) {
        InsightTone.GENTLE -> "Gentle"
        InsightTone.WARM -> "Warm"
        InsightTone.DIRECT -> "Direct"
    }

/** A single insight shown three ways, so the preview teaches the difference instantly. */
private val InsightTone.previewSentence: String
    get() = when (this) {
        InsightTone.GENTLE -> "You've leaned into weekends a little more lately — and that's perfectly okay."
        InsightTone.WARM -> "Weekends are where your money likes to go loud."
        InsightTone.DIRECT -> "Most of your spending happens on weekends."
    }

// ── Reusable pill selector ───────────────────────────────────────────────────────

/** A generic 3-way segmented pill selector matching the insight-tone pattern. */
@Composable
private fun <T> PillSelector(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            val bg by animateColorAsState(
                if (isSelected) SettingsPalette.Purple else SettingsPalette.tint(SettingsPalette.Purple),
                label = "pillbg"
            )
            Surface(
                Modifier.weight(1f).clickable { onSelect(value) },
                shape = RoundedCornerShape(50),
                color = bg
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else SettingsPalette.Purple,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.PaddingMedium)
                )
            }
        }
    }
}

// ── Accent picker ────────────────────────────────────────────────────────────────

private val ACCENT_PRESETS = listOf(
    "#7C4DFF" to "Purple",
    "#7C3AED" to "Violet",
    "#4A90E2" to "Blue",
    "#10B981" to "Green",
    "#F59E0B" to "Amber",
    "#EF4444" to "Coral"
)

@Composable
private fun AccentSection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsSurface {
        SectionLabel(Icons.Default.Palette, "Accent color", "Recolors the whole app instantly")
        Spacer(Modifier.height(Dimens.PaddingLarge))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)) {
            ACCENT_PRESETS.forEach { (hex, _) ->
                val color = Color(android.graphics.Color.parseColor(hex))
                val isSelected = uiState.accentColor.value == color.value
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(color)
                        .clickable { onEvent(SettingsEvent.OnAccentChange(hex)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ── Visual density ──────────────────────────────────────────────────────────────

@Composable
private fun VisualDensitySection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsSurface {
        SectionLabel(Icons.Default.Dashboard, "Visual density", "How tightly content is packed")
        Spacer(Modifier.height(Dimens.PaddingLarge))
        PillSelector(
            options = listOf(
                VisualDensity.COMFORTABLE to "Comfortable",
                VisualDensity.COZY to "Cozy",
                VisualDensity.COMPACT to "Compact"
            ),
            selected = uiState.visualDensity,
            onSelect = { onEvent(SettingsEvent.OnVisualDensityChange(it)) }
        )
    }
}

// ── 5. Habit goal stepper ───────────────────────────────────────────────────────
@Composable
private fun HabitGoalSection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsSurface {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(Icons.Default.LocalFireDepartment, "Your habit goal", "Days a week you want to check in")
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepperButton(Icons.Default.Remove, enabled = uiState.habitGoal > 1) {
                    onEvent(SettingsEvent.OnHabitGoalChange((uiState.habitGoal - 1).coerceAtLeast(1)))
                }
                Text(
                    "${uiState.habitGoal}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SettingsPalette.textPrimary,
                    modifier = Modifier.width(40.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                StepperButton(Icons.Default.Add, enabled = uiState.habitGoal < 7) {
                    onEvent(SettingsEvent.OnHabitGoalChange((uiState.habitGoal + 1).coerceAtMost(7)))
                }
            }
        }
    }
}

@Composable
private fun StepperButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else 0.3f
    Box(
        Modifier.size(36.dp).clip(CircleShape)
            .background(SettingsPalette.tint(SettingsPalette.Purple).copy(alpha = 0.12f * alpha))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = SettingsPalette.Purple.copy(alpha = alpha), modifier = Modifier.size(18.dp)) }
}

// ── 6. Currency ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencySection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    var showSheet by remember { mutableStateOf(false) }
    val selected = CurrencyUtils.getOption(uiState.currencyCode)

    SettingsSurface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSheet = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel(Icons.Default.Payments, "Currency", "Used everywhere amounts appear")
            Spacer(Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
            ) {
                Text(
                    text = "${selected.flag} ${selected.code}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SettingsPalette.Purple
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = SettingsPalette.textMuted,
                    modifier = Modifier.size(Dimens.IconSizeMedium)
                )
            }
        }
    }

    if (showSheet) {
        CurrencyBottomSheet(
            currentCode = uiState.currencyCode,
            onSelect = { code ->
                onEvent(SettingsEvent.OnCurrencyChange(code))
                showSheet = false
            },
            onDismiss = { showSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyBottomSheet(
    currentCode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val Purple = SettingsPalette.Purple

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = Dimens.BottomSheetRadius, topEnd = Dimens.BottomSheetRadius),
        containerColor = SettingsPalette.card,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = Dimens.PaddingMedium, bottom = Dimens.PaddingSmall)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SettingsPalette.cardBorder)
            )
        }
    ) {
        Column(modifier = Modifier.padding(bottom = Dimens.PaddingExtraLarge)) {
            Text(
                text = "Choose currency",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SettingsPalette.textPrimary,
                modifier = Modifier.padding(
                    horizontal = Dimens.CardInnerPaddingLarge,
                    vertical = Dimens.PaddingLarge
                )
            )

            CurrencyUtils.SUPPORTED_CURRENCIES.forEach { option ->
                val isSelected = option.code == currentCode
                val rowBg = if (isSelected) SettingsPalette.tint(Purple) else Color.Transparent

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                        .clickable { onSelect(option.code) }
                        .padding(
                            horizontal = Dimens.CardInnerPaddingLarge,
                            vertical = Dimens.PaddingLarge
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                ) {
                    Text(
                        text = option.flag,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = option.region,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Purple else SettingsPalette.textPrimary
                        )
                        Text(
                            text = "${option.code} · ${option.symbol}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SettingsPalette.textMuted
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Purple,
                            modifier = Modifier.size(Dimens.IconSizeMedium)
                        )
                    }
                }
            }
        }
    }
}

// ── 7. Smart capture (honest consent + REAL category learning) ───────────────────

@Composable
private fun SmartCaptureSection(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onNavigateToBankWhitelist: (() -> Unit)? = null,
    onNavigateToAutoDetectionOnboarding: (() -> Unit)? = null
) {
    SettingsSurface {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(Icons.Default.AutoAwesome, "Smart capture", "Let the app learn your habits")
            Spacer(Modifier.weight(1f))
            ToneSwitch(uiState.smartCaptureEnabled) { onEvent(SettingsEvent.OnSmartCaptureToggle(it)) }
        }
        Text(
            "When on, InsightKu quietly learns from how you log things to make tracking faster. It only ever uses your own transactions, and you can pause it anytime.",
            style = MaterialTheme.typography.bodySmall,
            color = SettingsPalette.textMuted,
            modifier = Modifier.padding(top = Dimens.PaddingMedium)
        )

        Spacer(Modifier.height(Dimens.PaddingLarge))
        androidx.compose.material3.HorizontalDivider(color = SettingsPalette.cardBorder)
        Spacer(Modifier.height(Dimens.PaddingLarge))

        // The genuinely-real control: category learning + transparency.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Learn my categories", style = MaterialTheme.typography.bodyLarge, color = SettingsPalette.textPrimary)
                Text("Suggests a category when you log a place you've logged before", style = MaterialTheme.typography.bodySmall, color = SettingsPalette.textMuted)
            }
            ToneSwitch(uiState.categoryLearningEnabled) { onEvent(SettingsEvent.OnCategoryLearningToggle(it)) }
        }

        AnimatedVisibility(
            visible = uiState.categoryLearningEnabled && uiState.learnedMemories.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(Modifier.padding(top = Dimens.PaddingLarge)) {
                Text("What I've picked up so far", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SettingsPalette.Purple)
                Spacer(Modifier.height(Dimens.PaddingMedium))
                uiState.learnedMemories.forEach { memory ->
                    MemoryRow(memory) { onEvent(SettingsEvent.OnForgetMemory(memory.merchant)) }
                }
            }
        }

        if (uiState.categoryLearningEnabled && uiState.learnedMemories.isEmpty()) {
            Text(
                "Nothing learned yet — once you log a place a couple of times, it'll show up here.",
                style = MaterialTheme.typography.bodySmall,
                color = SettingsPalette.textMuted,
                modifier = Modifier.padding(top = Dimens.PaddingMedium)
            )
        }

        Spacer(Modifier.height(Dimens.PaddingMedium))
        androidx.compose.material3.HorizontalDivider(color = SettingsPalette.cardBorder)
        Spacer(Modifier.height(Dimens.PaddingMedium))

        // ── Bank notification auto-capture ────────────────────────────────────
        BankNotificationSection(uiState, onEvent, onNavigateToBankWhitelist, onNavigateToAutoDetectionOnboarding)
    }
}

@Composable
private fun BankNotificationSection(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onNavigateToBankWhitelist: (() -> Unit)? = null,
    onNavigateToAutoDetectionOnboarding: (() -> Unit)? = null
) {
    val context   = androidx.compose.ui.platform.LocalContext.current
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle

    // Re-check permission on every ON_RESUME (user may have just granted it in Settings)
    var isListenerEnabled by remember { mutableStateOf(false) }
    fun refreshPermission() {
        isListenerEnabled = com.example.insightku.core.notification.BankNotificationListenerService
            .isEnabled(context)
    }

    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) refreshPermission()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { refreshPermission() }

    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Baca notifikasi bank",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SettingsPalette.textPrimary
                )
                Text(
                    "Deteksi transaksi dari BCA, SeaBank, GoPay, DANA, OVO, dll.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SettingsPalette.textMuted
                )
            }
            ToneSwitch(
                checked = uiState.bankNotificationEnabled && isListenerEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && !isListenerEnabled) {
                        // Permission not granted — open settings instead of toggling
                        com.example.insightku.core.notification.BankNotificationListenerService
                            .openSettings(context)
                    } else {
                        onEvent(SettingsEvent.OnBankNotificationToggle(enabled))
                    }
                }
            )
        }

        Spacer(Modifier.height(Dimens.PaddingSmall))

        when {
            // Permission NOT granted — show prominent prompt
            !isListenerEnabled -> {
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(Dimens.CornerRadiusMedium),
                    color = SettingsPalette.cardElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(Dimens.CardInnerPadding)) {
                        Text(
                            "Izin Notification Access belum diberikan",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            color = SettingsPalette.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Aplikasi perlu izin membaca notifikasi untuk mendeteksi transaksi dari aplikasi bank secara otomatis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SettingsPalette.textMuted
                        )
                        Spacer(Modifier.height(Dimens.PaddingMedium))
                        Button(
                            onClick = {
                                // Trust-first: arahkan ke onboarding (value → transparansi → izin)
                                // alih-alih langsung melempar user ke layar sistem yang menakutkan.
                                if (onNavigateToAutoDetectionOnboarding != null) {
                                    onNavigateToAutoDetectionOnboarding()
                                } else {
                                    com.example.insightku.core.notification.BankNotificationListenerService
                                        .openSettings(context)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                            containerColor = SettingsPalette.Purple
                            )
                        ) {
                            Text("Aktifkan Notification Access")
                        }
                    }
                }
            }
            // Permission granted AND feature enabled — show active status
            isListenerEnabled && uiState.bankNotificationEnabled -> {
                Text(
                    "✓ Aktif — transaksi dari notifikasi bank akan dideteksi otomatis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SettingsPalette.textMuted
                )
            }
            // Permission granted but feature toggled OFF
            isListenerEnabled && !uiState.bankNotificationEnabled -> {
                Text(
                    "Notification Access sudah diberikan. Aktifkan toggle untuk mulai mendeteksi transaksi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SettingsPalette.textMuted
                )
            }
        }

        // Entry ke layar whitelist — kontrol app mana yang dipantau.
        if (onNavigateToBankWhitelist != null) {
            Spacer(Modifier.height(Dimens.PaddingMedium))
            androidx.compose.material3.HorizontalDivider(color = SettingsPalette.cardBorder)
            Spacer(Modifier.height(Dimens.PaddingMedium))
            Row(
                Modifier.fillMaxWidth().clickable { onNavigateToBankWhitelist() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Aplikasi yang dipantau", style = MaterialTheme.typography.bodyLarge, color = SettingsPalette.textPrimary)
                    Text("Pilih aplikasi keuangan mana yang boleh dibaca", style = MaterialTheme.typography.bodySmall, color = SettingsPalette.textMuted)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SettingsPalette.textMuted)
            }
        }
    }
}

@Composable
private fun MemoryRow(memory: MerchantMemory, onForget: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = Dimens.PaddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(memory.merchant, style = MaterialTheme.typography.bodyMedium, color = SettingsPalette.textPrimary)
            Text(
                "usually ${memory.category} · ${memory.timesSeen}/${memory.totalForMerchant} times",
                style = MaterialTheme.typography.bodySmall,
                color = SettingsPalette.textMuted
            )
        }
        Box(
            Modifier.size(28.dp).clip(CircleShape)
                .background(SettingsPalette.tint(SettingsPalette.textMuted))
                .clickable(onClick = onForget),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Close, "Forget ${memory.merchant}", tint = SettingsPalette.textMuted, modifier = Modifier.size(14.dp)) }
    }
}

// ── 8. Account ───────────────────────────────────────────────────────────────────

@Composable
private fun AccountSection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsSurface {
        Text(uiState.userName.ifBlank { "Signed in" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = SettingsPalette.textPrimary)
        Text(uiState.userEmail, style = MaterialTheme.typography.bodySmall, color = SettingsPalette.textMuted)
        Spacer(Modifier.height(Dimens.PaddingLarge))
        Surface(
            Modifier.fillMaxWidth().clickable { onEvent(SettingsEvent.ShowLogoutDialog) },
            shape = RoundedCornerShape(50),
            color = Color.Transparent,
            border = BorderStroke(1.dp, SettingsPalette.ExpenseRed.copy(alpha = 0.5f))
        ) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = Dimens.PaddingMedium),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = SettingsPalette.ExpenseRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Dimens.PaddingMedium))
                Text("Log out", color = SettingsPalette.ExpenseRed, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun AppInfoFooter() {
    Column(
        Modifier.fillMaxWidth().padding(top = Dimens.PaddingMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("InsightKu", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SettingsPalette.textMuted)
        Text("Made with care 💜", style = MaterialTheme.typography.bodySmall, color = SettingsPalette.textMuted)
    }
}

@Composable
fun LogoutConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape           = RoundedCornerShape(Dimens.BottomSheetRadius),
            color           = MaterialTheme.colorScheme.surface,
            tonalElevation  = 6.dp,
            shadowElevation = 0.dp,
            modifier        = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier            = Modifier.padding(Dimens.CardInnerPaddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
            ) {
                // ── Icon ─────────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onErrorContainer,
                        modifier           = Modifier.size(28.dp)
                    )
                }

                // ── Copy ──────────────────────────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
                ) {
                    Text(
                        text       = "Log out of InsightKu?",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        textAlign  = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text      = "You'll need to sign in again to access your financial story.",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                // ── Actions ───────────────────────────────────────────────────
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
                ) {
                    Button(
                        onClick  = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.ButtonHeightPrimary),
                        shape  = RoundedCornerShape(Dimens.ButtonRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor   = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            modifier           = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(Dimens.PaddingMedium))
                        Text("Log Out", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.ButtonHeightSecondary),
                        shape  = RoundedCornerShape(Dimens.ButtonRadius),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            "Stay Logged In",
                            fontWeight = FontWeight.Medium,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}





