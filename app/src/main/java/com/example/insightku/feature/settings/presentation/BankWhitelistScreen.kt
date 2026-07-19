package com.example.insightku.feature.settings.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.data.repository.MonitorableApp
import com.example.insightku.core.ui.theme.Dimens

/**
 * Layar Whitelist Bank — kontrol penuh user atas sumber notifikasi yang diproses.
 *
 * "Terpasang di HP-mu" di atas (app didukung yang benar-benar terpasang),
 * "Didukung lainnya" di-collapse default agar tidak menampilkan puluhan bank
 * yang tidak dimiliki user. Toggle per app = allow/disallow di consent layer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankWhitelistScreen(
    onBack: () -> Unit,
    viewModel: BankWhitelistViewModel = hiltViewModel()
) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val installed = apps.filter { it.isInstalled }
    val others    = apps.filter { !it.isInstalled }
    var showOthers by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SettingsPalette.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.whitelist_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SettingsPalette.background,
                    titleContentColor = SettingsPalette.textPrimary,
                    navigationIconContentColor = SettingsPalette.textPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SettingsPalette.background)
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = Dimens.ScreenHorizontalPadding,
                vertical = Dimens.CardSpacing
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing)
        ) {
            item {
                Text(
                    stringResource(R.string.whitelist_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = SettingsPalette.textMuted
                )
            }

            // ── Terpasang di HP-mu ────────────────────────────────────────────
            item {
                Text(
                    stringResource(R.string.whitelist_installed_section),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SettingsPalette.textPrimary
                )
            }
            if (installed.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.whitelist_installed_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = SettingsPalette.textMuted
                    )
                }
            } else {
                items(installed, key = { it.packageName }) { app ->
                    AppRow(app = app, onToggle = { viewModel.setAllowed(app.packageName, it) })
                }
            }

            // ── Didukung lainnya (collapsed) ──────────────────────────────────
            if (others.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.CardRadius))
                            .clickable { showOthers = !showOthers }
                            .padding(vertical = Dimens.PaddingSmall),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.whitelist_other_section, others.size),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SettingsPalette.textPrimary
                        )
                        Icon(Icons.Default.ExpandMore, contentDescription = null, tint = SettingsPalette.textMuted)
                    }
                }
                if (showOthers) {
                    items(others, key = { it.packageName }) { app ->
                        AppRow(app = app, onToggle = { viewModel.setAllowed(app.packageName, it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    app: MonitorableApp,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        color = SettingsPalette.card,
        border = BorderStroke(1.dp, SettingsPalette.cardBorder),
        tonalElevation = 0.dp,
        shadowElevation = Dimens.ElevationSmall
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.CardInnerPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(SettingsPalette.tint(SettingsPalette.Purple)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = SettingsPalette.Purple, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(app.displayName, style = MaterialTheme.typography.bodyLarge, color = SettingsPalette.textPrimary)
                Text(
                    if (app.isAllowed) stringResource(R.string.whitelist_monitored) else stringResource(R.string.whitelist_not_monitored),
                    style = MaterialTheme.typography.bodySmall,
                    color = SettingsPalette.textMuted
                )
            }
            Switch(
                checked = app.isAllowed,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SettingsPalette.Purple,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = SettingsPalette.textMuted.copy(alpha = 0.4f)
                )
            )
        }
    }
}
