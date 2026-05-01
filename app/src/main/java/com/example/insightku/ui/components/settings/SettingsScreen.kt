
package com.example.insightku.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.R
import com.example.insightku.ui.theme.Dimens
import com.example.insightku.ui.theme.LocalResponsiveDimens
import com.example.insightku.utils.CurrencyUtils
import com.example.insightku.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.userEmail) {
        if (!uiState.isLoading && uiState.userEmail.isEmpty()) {
            onLogout()
        }
    }

    if (uiState.showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = { viewModel.onEvent(SettingsEvent.ConfirmLogout) },
            onDismiss = { viewModel.onEvent(SettingsEvent.HideLogoutDialog) }
        )
    }

    val dimens = LocalResponsiveDimens.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        SettingsHeader()

        Column(
            modifier = Modifier
                .padding(horizontal = dimens.screenHorizontalPadding)
                .offset(y = (-32).dp),
            verticalArrangement = Arrangement.spacedBy(dimens.itemSpacing)
        ) {
            AppearanceSection(uiState, viewModel::onEvent)
            CurrencySection(uiState, viewModel::onEvent)
            TransactionInputSection(uiState, viewModel::onEvent)
            WhatsAppIntegrationSection(uiState, viewModel::onEvent)
            NotificationsSection(uiState, viewModel::onEvent)
            SecuritySection(uiState, viewModel::onEvent)
            AccountSection(uiState, viewModel::onEvent)
            AppInfoSection()
        }
    }
}

@Composable
private fun SettingsHeader() {
    val purpleGradient = Brush.verticalGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = purpleGradient)
            .padding(Dimens.PaddingExtraLarge)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
            Text(
                text = stringResource(R.string.settings_subtitle),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.8f)
                )
            )
        }
    }
}

@Composable
private fun AppearanceSection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsCard(
        title = stringResource(R.string.appearance),
        icon = if (uiState.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode
    ) {
        SettingSwitchItem(
            label = stringResource(R.string.dark_mode),
            description = stringResource(R.string.dark_mode_desc),
            checked = uiState.isDarkMode,
            onCheckedChange = { onEvent(SettingsEvent.OnThemeChange(it)) }
        )
    }
}

@Composable
private fun TransactionInputSection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsCard(title = stringResource(R.string.transaction_input), icon = Icons.Default.Edit) {
        SettingSelectItem(
            label = stringResource(R.string.default_input_mode),
            description = stringResource(R.string.default_input_mode_desc),
            selectedValue = uiState.defaultInputMode,
            options = mapOf(
                InputMode.OCR to stringResource(R.string.input_mode_ocr),
                InputMode.MANUAL to stringResource(R.string.input_mode_manual)
            ),
            onSelectionChange = { onEvent(SettingsEvent.OnDefaultInputChange(it)) }
        )
    }
}

@Composable
private fun CurrencySection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    val currencyOptions = CurrencyUtils.SUPPORTED_CURRENCIES.associate { it.code to it.displayName }
    SettingsCard(title = "Currency", icon = Icons.Default.AttachMoney) {
        SettingSelectItem(
            label = "Default Currency",
            description = "Pilih mata uang yang digunakan di seluruh aplikasi",
            selectedValue = uiState.currencyCode,
            options = currencyOptions,
            onSelectionChange = { onEvent(SettingsEvent.OnCurrencyChange(it)) }
        )
    }
}

@Composable
private fun WhatsAppIntegrationSection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsCard(title = stringResource(R.string.whatsapp_integration), icon = Icons.AutoMirrored.Filled.Message) {
        SettingSwitchItem(
            label = stringResource(R.string.whatsapp_bot),
            description = if (uiState.whatsappEnabled) stringResource(R.string.whatsapp_bot_desc_enabled) else stringResource(R.string.whatsapp_bot_desc_disabled),
            checked = uiState.whatsappEnabled,
            onCheckedChange = { onEvent(SettingsEvent.OnWhatsAppToggle(it)) }
        )
    }
}

@Composable
private fun NotificationsSection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsCard(title = stringResource(R.string.notifications), icon = Icons.Default.Notifications) {
        SettingSwitchItem(
            label = stringResource(R.string.push_notifications),
            description = stringResource(R.string.push_notifications_desc),
            checked = uiState.pushNotificationsEnabled,
            onCheckedChange = { onEvent(SettingsEvent.OnPushNotificationsToggle(it)) }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingMedium))
        SettingSwitchItem(
            label = stringResource(R.string.budget_alerts),
            description = stringResource(R.string.budget_alerts_desc),
            checked = uiState.budgetAlertsEnabled,
            onCheckedChange = { onEvent(SettingsEvent.OnBudgetAlertsToggle(it)) }
        )
    }
}

@Composable
private fun SecuritySection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsCard(title = stringResource(R.string.security), icon = Icons.Default.Shield) {
        SettingSwitchItem(
            label = stringResource(R.string.biometric_auth),
            description = stringResource(R.string.biometric_auth_desc),
            checked = uiState.biometricEnabled,
            onCheckedChange = { onEvent(SettingsEvent.OnBiometricToggle(it)) }
        )
    }
}

@Composable
private fun AccountSection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    SettingsCard(title = stringResource(R.string.account), icon = Icons.Default.Person) {
        SettingItem(
            label = stringResource(R.string.current_user),
            description = uiState.userEmail
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingMedium))
        Button(
            onClick = { onEvent(SettingsEvent.ShowLogoutDialog) },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.logout_button))
                Spacer(modifier = Modifier.width(Dimens.PaddingMedium))
                Text(stringResource(R.string.logout_button))
            }
        }
    }
}

@Composable
private fun AppInfoSection() {
    Card(
        shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.PaddingExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.app_info_version), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
            Text(
                text = stringResource(R.string.app_info_made_with_love),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Reusable Components
@Composable
fun SettingsCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(Dimens.PaddingLarge)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(Dimens.PaddingMedium))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
            content()
        }
    }
}

@Composable
fun SettingSwitchItem(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SettingSelectItem(
    label: String,
    description: String,
    selectedValue: T,
    options: Map<T, String>,
    onSelectionChange: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = options[selectedValue] ?: "",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (value, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            onSelectionChange(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingItem(label: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LogoutConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.logout_dialog_title))
                Spacer(Modifier.width(Dimens.PaddingMedium))
                Text(stringResource(R.string.logout_dialog_title))
            }
        },
        text = { Text(stringResource(R.string.logout_dialog_message)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(stringResource(R.string.logout_dialog_confirm)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.logout_dialog_cancel)) }
        }
    )
}
