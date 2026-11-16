package com.example.insightku.feature.settings.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.insightku.R
import com.example.insightku.core.notification.BankNotificationListenerService
import com.example.insightku.core.ui.components.bottomsheet.SafeBottomSheet
import com.example.insightku.core.ui.theme.AccentPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.InsightTone
import com.example.insightku.core.ui.theme.LocalResponsiveDimens
import com.example.insightku.core.ui.theme.VisualDensity
import com.example.insightku.core.utils.CurrencyUtils
import com.example.insightku.feature.home.domain.MerchantMemory

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onNavigateToNotificationDebug: (() -> Unit)? = null,
    onNavigateToBankWhitelist: (() -> Unit)? = null,
    onNavigateToAutoDetectionOnboarding: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
        contentPadding = PaddingValues(
            start = dimens.screenHorizontalPadding,
            end = dimens.screenHorizontalPadding,
            top = Dimens.PaddingExtraLarge,
            // Floating bottom nav (~64dp) + gesture bar; keep the last row reachable.
            bottom = Dimens.ContentBottomPadding + Dimens.BottomNavHeight
        )
    ) {
        item { IdentityHeader() }

        item { GroupHeading(stringResource(R.string.settings_group_appearance)) }
        item { AppearanceSection(uiState, onEvent) }
        item { AccentSection(uiState, onEvent) }
        item { VisualDensitySection(uiState, onEvent) }
        item { ComfortSection(uiState, onEvent) }

        item { Spacer(Modifier.height(Dimens.SectionSpacing)) }
        item { GroupHeading(stringResource(R.string.settings_group_language_money)) }
        item { LanguageSection(uiState, onEvent) }
        item { CurrencySection(uiState, onEvent) }
        item { InsightToneSection(uiState, onEvent) }

        item { Spacer(Modifier.height(Dimens.SectionSpacing)) }
        item { GroupHeading(stringResource(R.string.settings_group_habits_capture)) }
        item { HabitGoalSection(uiState, onEvent) }
        item { SmartCaptureSection(uiState, onEvent, onNavigateToBankWhitelist, onNavigateToAutoDetectionOnboarding) }
        if (onNavigateToNotificationDebug != null) {
            item {
                SettingsRow(
                    title = stringResource(R.string.settings_notification_debug),
                    subtitle = stringResource(R.string.settings_notification_debug_desc),
                    leadingIcon = Icons.Default.BugReport,
                    onClick = onNavigateToNotificationDebug,
                    trailing = { TrailingChevron() }
                )
            }
        }

        item { Spacer(Modifier.height(Dimens.SectionSpacing)) }
        item { GroupHeading(stringResource(R.string.account)) }
        item { AccountSection(uiState, onEvent) }
        item { AppInfoFooter() }
    }
}

// ── Layout primitives ───────────────────────────────────────────────────────────
//
// One continuous editorial list: no card per setting. Groups are separated by
// whitespace, rows inside a group by a single hairline. Every row shares one
// 56dp baseline so labels, values, icons and toggles line up column-perfect.

private val RowHeight = 56.dp
private val IconColumnWidth = 32.dp

@Composable
private fun GroupHeading(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = SettingsPalette.Purple,
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(bottom = Dimens.PaddingMedium)
    )
}

@Composable
private fun Hairline() {
    HorizontalDivider(
        color = SettingsPalette.cardBorder.copy(alpha = 0.7f),
        thickness = 0.5.dp
    )
}

/** Shared row: label at start, trailing control pinned to the end, never overlapping. */
@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowHeight)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = Dimens.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = SettingsPalette.textMuted,
                modifier = Modifier.size(Dimens.IconSizeMedium)
            )
            Spacer(Modifier.width(Dimens.PaddingMedium))
        } else {
            Spacer(Modifier.width(IconColumnWidth))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = SettingsPalette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SettingsPalette.textMuted,
                    maxLines = 2
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(Dimens.PaddingMedium))
            Box(contentAlignment = Alignment.CenterEnd) { trailing() }
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) { content() }
}

@Composable
private fun TrailingChevron() {
    Icon(
        Icons.Default.ChevronRight,
        contentDescription = null,
        tint = SettingsPalette.textMuted,
        modifier = Modifier.size(Dimens.IconSizeMedium)
    )
}

@Composable
private fun TrailingValue(value: String, chevron: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = SettingsPalette.Purple,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (chevron) {
            Spacer(Modifier.width(Dimens.PaddingSmall))
            TrailingChevron()
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
            uncheckedTrackColor = SettingsPalette.textMuted.copy(alpha = 0.35f)
        )
    )
}

/** Thin, borderless preview strip — a single accent rule instead of a nested card. */
@Composable
private fun PreviewStrip(
    label: String,
    body: String,
    leading: @Composable (() -> Unit)? = null
) {
    val bodyColor by animateColorAsState(SettingsPalette.textPrimary, label = "previewText")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.CornerRadiusSmall))
            .background(SettingsPalette.tint(SettingsPalette.Purple))
            .padding(horizontal = Dimens.PaddingLarge, vertical = Dimens.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(SettingsPalette.Purple)
        )
        Spacer(Modifier.width(Dimens.PaddingMedium))
        if (leading != null) {
            leading()
            Spacer(Modifier.width(Dimens.PaddingMedium))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = SettingsPalette.textMuted)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = bodyColor)
        }
    }
}

// ── 1. Identity header ────────────────────────────────────────────────────────

@Composable
private fun IdentityHeader() {
    Text(
        stringResource(R.string.settings_identity_subtitle),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = SettingsPalette.textPrimary,
        modifier = Modifier.padding(bottom = Dimens.PaddingExtraLarge)
    )
}

// ── 2. Language picker ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    var showSheet by remember { mutableStateOf(false) }
    val currentLang = uiState.appLanguage

    SettingsGroup {
        SettingsRow(
            title = stringResource(R.string.settings_language),
            subtitle = stringResource(R.string.settings_language_desc),
            leadingIcon = Icons.Default.Language,
            onClick = { showSheet = true },
            trailing = {
                TrailingValue(
                    when (currentLang) {
                        AppLanguage.ID -> "🇮🇩 Indonesia"
                        AppLanguage.EN -> "🇺🇸 English"
                    }
                )
            }
        )
        Hairline()
    }

    if (showSheet) {
        LanguageBottomSheet(
            currentLanguage = currentLang,
            onSelect = { lang ->
                onEvent(SettingsEvent.OnLanguageChange(lang))
                showSheet = false
            },
            onDismiss = { showSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageBottomSheet(
    currentLanguage: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val Purple = SettingsPalette.Purple

    SafeBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = Dimens.BottomSheetRadius, topEnd = Dimens.BottomSheetRadius),
        containerColor = SettingsPalette.card,
        contentWindowInsets = WindowInsets(0, 8, 0, 8),
        tonalElevation = 0.dp,
        dragHandle = { SheetDragHandle() }
    ) {
        Column(modifier = Modifier.padding(bottom = Dimens.PaddingExtraLarge)) {
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SettingsPalette.textPrimary,
                modifier = Modifier.padding(
                    horizontal = Dimens.CardInnerPaddingLarge,
                    vertical = Dimens.PaddingLarge
                )
            )

            AppLanguage.entries.forEach { language ->
                val isSelected = language == currentLanguage
                val rowBg = if (isSelected) SettingsPalette.tint(Purple) else Color.Transparent

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = RowHeight)
                        .background(rowBg)
                        .clickable { onSelect(language) }
                        .padding(
                            horizontal = Dimens.CardInnerPaddingLarge,
                            vertical = Dimens.PaddingLarge
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                ) {
                    Text(
                        text = when (language) {
                            AppLanguage.ID -> "🇮🇩"
                            AppLanguage.EN -> "🇺🇸"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (language) {
                                AppLanguage.ID -> "Bahasa Indonesia"
                                AppLanguage.EN -> "English"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Purple else SettingsPalette.textPrimary
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

@Composable
private fun SheetDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(Dimens.BottomSheetHandleWidth)
                .height(Dimens.BottomSheetHandleHeight)
                .clip(RoundedCornerShape(Dimens.BottomSheetHandleRadius))
                .background(SettingsPalette.cardBorder)
        )
    }
}

// ── 3. Appearance + live preview ──────────────────────────────────────────────

@Composable
private fun AppearanceSection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsGroup {
        SettingsRow(
            title = stringResource(R.string.appearance),
            subtitle = if (uiState.isDarkMode) stringResource(R.string.settings_appearance_desc_dark) else stringResource(R.string.settings_appearance_desc_light),
            leadingIcon = if (uiState.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
            trailing = { ToneSwitch(uiState.isDarkMode) { onEvent(SettingsEvent.OnThemeChange(it)) } }
        )
        PreviewStrip(
            label = stringResource(R.string.settings_preview),
            body = stringResource(R.string.settings_preview_desc),
            leading = {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(SettingsPalette.Purple)
                )
            }
        )
        Spacer(Modifier.height(Dimens.PaddingSmall))
        Hairline()
    }
}

// ── 4. Comfort mode ────────────────────────────────────────────────────────────

@Composable
private fun ComfortSection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsGroup {
        SettingsRow(
            title = stringResource(R.string.settings_comfort_mode),
            subtitle = stringResource(R.string.settings_comfort_mode_desc),
            trailing = { ToneSwitch(uiState.comfortMode) { onEvent(SettingsEvent.OnComfortModeToggle(it)) } }
        )
        AnimatedVisibility(uiState.comfortMode, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Text(
                stringResource(R.string.settings_comfort_body_detail),
                style = MaterialTheme.typography.bodySmall,
                color = SettingsPalette.Purple,
                modifier = Modifier.padding(start = IconColumnWidth, bottom = Dimens.PaddingMedium)
            )
        }
    }
}

// ── 5. How insights talk to you + live preview ─────────────────────────────────

@Composable
private fun InsightToneSection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsGroup {
        SettingsRow(
            title = stringResource(R.string.settings_insight_tone),
            subtitle = stringResource(R.string.settings_insight_tone_desc),
            leadingIcon = Icons.Default.AutoAwesome
        )
        Column(Modifier.padding(start = IconColumnWidth)) {
            PillSelector(
                options = InsightTone.entries.map { it to it.label() },
                selected = uiState.insightTone,
                onSelect = { onEvent(SettingsEvent.OnInsightToneChange(it)) }
            )
            Spacer(Modifier.height(Dimens.PaddingMedium))
            PreviewStrip(
                label = stringResource(R.string.settings_sounds_like),
                body = uiState.insightTone.previewSentence()
            )
            Spacer(Modifier.height(Dimens.PaddingLarge))
        }
    }
}

@Composable
private fun InsightTone.label(): String = when (this) {
    InsightTone.GENTLE -> stringResource(R.string.insight_tone_gentle)
    InsightTone.WARM -> stringResource(R.string.insight_tone_warm)
    InsightTone.DIRECT -> stringResource(R.string.insight_tone_direct)
}

/** A single insight shown three ways, so the preview teaches the difference instantly. */
@Composable
private fun InsightTone.previewSentence(): String = when (this) {
    InsightTone.GENTLE -> stringResource(R.string.insight_tone_preview_gentle)
    InsightTone.WARM -> stringResource(R.string.insight_tone_preview_warm)
    InsightTone.DIRECT -> stringResource(R.string.insight_tone_preview_direct)
}

// ── Reusable pill selector ───────────────────────────────────────────────────────

@Composable
private fun <T> PillSelector(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            val bg by animateColorAsState(
                if (isSelected) SettingsPalette.Purple else SettingsPalette.tint(SettingsPalette.Purple),
                label = "pillbg"
            )
            Surface(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
                    .clickable { onSelect(value) },
                shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
                color = bg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else SettingsPalette.Purple,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = Dimens.PaddingSmall)
                    )
                }
            }
        }
    }
}

// ── Accent picker ────────────────────────────────────────────────────────────────

private val ACCENT_PRESETS = listOf(
    AccentPalette.DEFAULT_HEX to R.string.settings_accent_apricot,
    "#E7B84B" to R.string.settings_accent_honey,
    "#7FAE92" to R.string.settings_accent_sage,
    "#79AFCB" to R.string.settings_accent_sky,
    "#A898CF" to R.string.settings_accent_lilac
)

@Composable
private fun AccentSection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsGroup {
        SettingsRow(
            title = stringResource(R.string.settings_accent_color),
            subtitle = stringResource(R.string.settings_accent_color_desc),
            leadingIcon = Icons.Default.Palette
        )
        Row(
            Modifier.padding(start = IconColumnWidth, bottom = Dimens.PaddingLarge),
            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
        ) {
            ACCENT_PRESETS.forEach { (hex, labelRes) ->
                val label = stringResource(labelRes)
                val color = Color(hex.toColorInt())
                val isSelected = uiState.accentColorHex.equals(hex, ignoreCase = true)
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color)
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton
                        ) { onEvent(SettingsEvent.OnAccentChange(hex)) }
                        .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(Dimens.IconSizeMedium))
                }
            }
        }
        Hairline()
    }
}

// ── Visual density ──────────────────────────────────────────────────────────────

@Composable
private fun VisualDensitySection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsGroup {
        SettingsRow(
            title = stringResource(R.string.settings_visual_density),
            subtitle = stringResource(R.string.settings_visual_density_desc)
        )
        Column(Modifier.padding(start = IconColumnWidth, bottom = Dimens.PaddingLarge)) {
            PillSelector(
                options = listOf(
                    VisualDensity.COMFORTABLE to stringResource(R.string.settings_density_comfortable),
                    VisualDensity.COZY to stringResource(R.string.settings_density_cozy),
                    VisualDensity.COMPACT to stringResource(R.string.settings_density_compact)
                ),
                selected = uiState.visualDensity,
                onSelect = { onEvent(SettingsEvent.OnVisualDensityChange(it)) }
            )
        }
        Hairline()
    }
}

// ── 6. Habit goal stepper ───────────────────────────────────────────────────────

@Composable
private fun HabitGoalSection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsGroup {
        SettingsRow(
            title = stringResource(R.string.settings_habit_goal),
            subtitle = stringResource(R.string.settings_habit_goal_desc),
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepperButton(Icons.Default.Remove, enabled = uiState.habitGoal > 1) {
                        onEvent(SettingsEvent.OnHabitGoalChange((uiState.habitGoal - 1).coerceAtLeast(1)))
                    }
                    Text(
                        "${uiState.habitGoal}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SettingsPalette.textPrimary,
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.Center
                    )
                    StepperButton(Icons.Default.Add, enabled = uiState.habitGoal < 7) {
                        onEvent(SettingsEvent.OnHabitGoalChange((uiState.habitGoal + 1).coerceAtMost(7)))
                    }
                }
            }
        )
        Hairline()
    }
}

@Composable
private fun StepperButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else 0.3f
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(SettingsPalette.tint(SettingsPalette.Purple).copy(alpha = 0.12f * alpha))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = SettingsPalette.Purple.copy(alpha = alpha), modifier = Modifier.size(18.dp)) }
}

// ── 7. Currency ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencySection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    var showSheet by remember { mutableStateOf(false) }
    val selected = CurrencyUtils.getOption(uiState.currencyCode)

    SettingsGroup {
        SettingsRow(
            title = stringResource(R.string.settings_currency),
            subtitle = stringResource(R.string.settings_currency_desc),
            leadingIcon = Icons.Default.Payments,
            onClick = { showSheet = true },
            trailing = { TrailingValue("${selected.flag} ${selected.code}") }
        )
        Hairline()
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
    val Purple = SettingsPalette.Purple

    SafeBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = Dimens.BottomSheetRadius, topEnd = Dimens.BottomSheetRadius),
        containerColor = SettingsPalette.card,
        contentWindowInsets = WindowInsets(0, 8, 0, 8),
        tonalElevation = 0.dp,
        dragHandle = { SheetDragHandle() }
    ) {
        Column(modifier = Modifier.padding(bottom = Dimens.PaddingExtraLarge)) {
            Text(
                text = stringResource(R.string.settings_choose_currency),
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
                        .heightIn(min = RowHeight)
                        .background(rowBg)
                        .clickable { onSelect(option.code) }
                        .padding(
                            horizontal = Dimens.CardInnerPaddingLarge,
                            vertical = Dimens.PaddingMedium
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                ) {
                    Text(text = option.flag, style = MaterialTheme.typography.titleMedium)
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

// ── 8. Smart capture (honest consent + REAL category learning) ───────────────────

@Composable
private fun SmartCaptureSection(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onNavigateToBankWhitelist: (() -> Unit)? = null,
    onNavigateToAutoDetectionOnboarding: (() -> Unit)? = null
) {
    SettingsGroup {
        SettingsRow(
            title = stringResource(R.string.settings_smart_capture),
            subtitle = stringResource(R.string.settings_smart_capture_desc),
            trailing = { ToneSwitch(uiState.smartCaptureEnabled) { onEvent(SettingsEvent.OnSmartCaptureToggle(it)) } }
        )
        Text(
            stringResource(R.string.settings_smart_capture_body_long),
            style = MaterialTheme.typography.bodySmall,
            color = SettingsPalette.textMuted,
            modifier = Modifier.padding(start = IconColumnWidth, bottom = Dimens.PaddingLarge)
        )
        Hairline()

        SettingsRow(
            title = stringResource(R.string.settings_learn_categories),
            subtitle = stringResource(R.string.settings_learn_categories_desc),
            trailing = { ToneSwitch(uiState.categoryLearningEnabled) { onEvent(SettingsEvent.OnCategoryLearningToggle(it)) } }
        )

        AnimatedVisibility(
            visible = uiState.categoryLearningEnabled && uiState.learnedMemories.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(Modifier.padding(start = IconColumnWidth, bottom = Dimens.PaddingLarge)) {
                Text(
                    stringResource(R.string.settings_learn_categories_hint),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SettingsPalette.Purple
                )
                Spacer(Modifier.height(Dimens.PaddingMedium))
                uiState.learnedMemories.forEach { memory ->
                    MemoryRow(memory) { onEvent(SettingsEvent.OnForgetMemory(memory.merchant)) }
                }
            }
        }

        if (uiState.categoryLearningEnabled && uiState.learnedMemories.isEmpty()) {
            Text(
                stringResource(R.string.settings_learn_categories_empty_detail),
                style = MaterialTheme.typography.bodySmall,
                color = SettingsPalette.textMuted,
                modifier = Modifier.padding(start = IconColumnWidth, bottom = Dimens.PaddingLarge)
            )
        }
        Hairline()

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
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    var isListenerEnabled by remember { mutableStateOf(false) }
    fun refreshPermission() {
        isListenerEnabled = BankNotificationListenerService.isEnabled(context)
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshPermission()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { refreshPermission() }

    Column {
        SettingsRow(
            title = stringResource(R.string.settings_bank_notification),
            subtitle = stringResource(R.string.settings_bank_notification_desc),
            trailing = {
                ToneSwitch(
                    checked = uiState.bankNotificationEnabled && isListenerEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !isListenerEnabled) {
                            // Permission not granted — open settings instead of toggling
                            BankNotificationListenerService.openSettings(context)
                        } else {
                            onEvent(SettingsEvent.OnBankNotificationToggle(enabled))
                        }
                    }
                )
            }
        )

        when {
            !isListenerEnabled -> {
                // Permission prompt is the one place a filled panel is warranted:
                // it carries a real action, not a setting row.
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.CornerRadiusSmall))
                        .background(SettingsPalette.tint(SettingsPalette.Purple))
                        .padding(Dimens.PaddingLarge)
                ) {
                    Text(
                        stringResource(R.string.settings_notification_access_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SettingsPalette.textPrimary
                    )
                    Spacer(Modifier.height(Dimens.PaddingSmall))
                    Text(
                        stringResource(R.string.settings_notification_access_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = SettingsPalette.textMuted
                    )
                    Spacer(Modifier.height(Dimens.PaddingMedium))
                    Button(
                        onClick = {
                            if (onNavigateToAutoDetectionOnboarding != null) {
                                onNavigateToAutoDetectionOnboarding()
                            } else {
                                BankNotificationListenerService.openSettings(context)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsPalette.Purple)
                    ) {
                        Text(stringResource(R.string.settings_notification_access_button))
                    }
                }
            }
            isListenerEnabled && uiState.bankNotificationEnabled -> {
                Text(
                    "✓ ${stringResource(R.string.settings_notification_active)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SettingsPalette.textMuted,
                    modifier = Modifier.padding(start = IconColumnWidth, bottom = Dimens.PaddingMedium)
                )
            }
            else -> {
                Text(
                    stringResource(R.string.settings_notification_inactive),
                    style = MaterialTheme.typography.bodySmall,
                    color = SettingsPalette.textMuted,
                    modifier = Modifier.padding(start = IconColumnWidth, bottom = Dimens.PaddingMedium)
                )
            }
        }

        if (onNavigateToBankWhitelist != null) {
            Hairline()
            SettingsRow(
                title = stringResource(R.string.settings_monitored_apps),
                subtitle = stringResource(R.string.settings_monitored_apps_desc),
                onClick = onNavigateToBankWhitelist,
                trailing = { TrailingChevron() }
            )
        }
    }
}

@Composable
private fun MemoryRow(memory: MerchantMemory, onForget: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(vertical = Dimens.PaddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                memory.merchant,
                style = MaterialTheme.typography.bodyMedium,
                color = SettingsPalette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "usually ${memory.category} · ${memory.timesSeen}/${memory.totalForMerchant} times",
                style = MaterialTheme.typography.bodySmall,
                color = SettingsPalette.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(Dimens.PaddingMedium))
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SettingsPalette.tint(SettingsPalette.textMuted))
                .clickable(onClick = onForget),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Close, "Forget ${memory.merchant}", tint = SettingsPalette.textMuted, modifier = Modifier.size(16.dp)) }
    }
}

// ── 9. Account ───────────────────────────────────────────────────────────────────

@Composable
private fun AccountSection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsGroup {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    uiState.userName.ifBlank { stringResource(R.string.settings_signed_in) },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = SettingsPalette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    uiState.userEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = SettingsPalette.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Hairline()
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = RowHeight)
                .clickable { onEvent(SettingsEvent.ShowLogoutDialog) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = SettingsPalette.ExpenseRed,
                modifier = Modifier.size(Dimens.IconSizeMedium)
            )
            Spacer(Modifier.width(Dimens.PaddingMedium))
            Text(
                stringResource(R.string.settings_log_out),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = SettingsPalette.ExpenseRed,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AppInfoFooter() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = Dimens.PaddingExtraLarge, bottom = Dimens.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = SettingsPalette.textMuted
        )
        Text(
            stringResource(R.string.app_info_made_with_love),
            style = MaterialTheme.typography.bodySmall,
            color = SettingsPalette.textMuted
        )
    }
}
