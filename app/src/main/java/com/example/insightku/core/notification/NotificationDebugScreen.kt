package com.example.insightku.core.notification

import android.content.ComponentName
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.i18n.DateFormatter
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import java.util.*

// ── Raw notification (all packages, no filter) ───────────────────────────────

data class RawNotificationEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = ""
)

// ── Parsed transaction debug entry ───────────────────────────────────────────

data class NotificationDebugEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String = "",
    val bankName: String = "",
    val rawTitle: String = "",
    val rawContent: String = "",
    val parsedAmount: Double? = null,
    val parsedMerchant: String? = null,
    val parsedType: TransactionType? = null,
    val parsedCategory: String? = null,
    val parseSuccess: Boolean = false,
    val transactionCreated: Boolean = false,
    val txId: String? = null,
    val errorMessage: String? = null
)

// ── Shared in-memory log ──────────────────────────────────────────────────────

object NotificationDebugLog {
    // Thread-safe backing store — written from any thread (binder, IO, main)
    private val _rawEntries    = java.util.concurrent.CopyOnWriteArrayList<RawNotificationEntry>()
    private val _entries       = java.util.concurrent.CopyOnWriteArrayList<NotificationDebugEntry>()
    private val _serviceEvents = java.util.concurrent.CopyOnWriteArrayList<String>()

    // Compose-observable snapshots — always a copy, refreshed on main thread
    private val _rawState      = mutableStateListOf<RawNotificationEntry>()
    private val _entriesState  = mutableStateListOf<NotificationDebugEntry>()
    private val _eventsState   = mutableStateListOf<String>()

    // Getters read from Compose state (reactive) — always in sync via syncToState()
    val rawEntries:    List<RawNotificationEntry>  get() = _rawState.toList()
    val entries:       List<NotificationDebugEntry> get() = _entriesState.toList()
    val serviceEvents: List<String>                 get() = _eventsState.toList()

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    /** Sync backing store → Compose state on main thread. Safe to call from any thread. */
    private fun syncToState() {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            // Already on main thread — update directly, no post needed
            _rawState.apply { clear(); addAll(_rawEntries.take(50)) }
            _entriesState.apply { clear(); addAll(_entries.take(20)) }
            _eventsState.apply { clear(); addAll(_serviceEvents.take(30)) }
        } else {
            mainHandler.post {
                _rawState.apply { clear(); addAll(_rawEntries.take(50)) }
                _entriesState.apply { clear(); addAll(_entries.take(20)) }
                _eventsState.apply { clear(); addAll(_serviceEvents.take(30)) }
            }
        }
    }

    fun addRaw(entry: RawNotificationEntry) {
        _rawEntries.add(0, entry)
        while (_rawEntries.size > 50) _rawEntries.removeLastOrNull()
        syncToState()
    }

    fun add(entry: NotificationDebugEntry) {
        _entries.add(0, entry)
        while (_entries.size > 20) _entries.removeLastOrNull()
        syncToState()
    }

    fun recordServiceEvent(msg: String) {
        android.util.Log.d("BankService", "recordServiceEvent: $msg")
        _serviceEvents.add(0, msg)
        while (_serviceEvents.size > 30) _serviceEvents.removeLastOrNull()
        syncToState()
    }

    fun clearAll() {
        _rawEntries.clear(); _entries.clear(); _serviceEvents.clear()
        syncToState()
    }
}

// ── Debug Screen ──────────────────────────────────────────────────────────────

@Composable
fun NotificationDebugScreen(modifier: Modifier = Modifier) {
    val context   = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    var isListenerEnabled by remember { mutableStateOf(false) }
    var rawListenerValue  by remember { mutableStateOf("") }
    var componentNameStr  by remember { mutableStateOf("") }
    var refreshTick       by remember { mutableStateOf(0) }
    var selectedTab       by remember { mutableStateOf(0) } // 0=All, 1=Bank, 2=Service

    fun refresh() {
        val cn = ComponentName(context, BankNotificationListenerService::class.java)
        componentNameStr  = cn.flattenToString()
        rawListenerValue  = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: "(null)"
        isListenerEnabled = rawListenerValue.split(":").any { it.trim() == componentNameStr }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(refreshTick) { refresh() }

    val rawEntries     = NotificationDebugLog.rawEntries
    val bankEntries    = NotificationDebugLog.entries
    val serviceEvents  = NotificationDebugLog.serviceEvents

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppPalette.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // ── Header ────────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BugReport, null, tint = AppPalette.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.notification_debug_title), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = AppPalette.textPrimary)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { refreshTick++ }) {
                Icon(Icons.Default.Refresh, stringResource(R.string.debug_refresh), tint = AppPalette.textMuted)
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Listener status ───────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(if (isListenerEnabled) stringResource(R.string.debug_listener_active) else stringResource(R.string.debug_listener_inactive), isListenerEnabled)
            StatusChip(stringResource(R.string.debug_tab_all, rawEntries.size), rawEntries.isNotEmpty())
            StatusChip(stringResource(R.string.debug_tab_bank, bankEntries.size), bankEntries.any { it.parseSuccess })
        }

        Spacer(Modifier.height(8.dp))

        // ── Diagnostics ───────────────────────────────────────────────────────
        Surface(shape = RoundedCornerShape(10.dp), color = AppPalette.card) {
            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                DiagRow("ComponentName", componentNameStr)
                DiagRow("Match",         if (isListenerEnabled) "YES ✓" else "NO ✗")
                DiagRow("Raw listeners", if (rawListenerValue.length > 80) rawListenerValue.take(80) + "…" else rawListenerValue)
            }
        }

        if (!isListenerEnabled) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = { BankNotificationListenerService.openSettings(context) },
                colors   = ButtonDefaults.buttonColors(containerColor = AppPalette.accent),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.debug_open_notification_settings)) }
        } else {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick  = { BankNotificationListenerService.forceReconnect(context) },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.debug_force_reconnect), color = AppPalette.textMuted, fontSize = 12.sp) }
                OutlinedButton(
                    onClick  = { NotificationDebugLog.clearAll() },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.debug_clear_log), color = AppPalette.textMuted, fontSize = 12.sp) }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Tabs ──────────────────────────────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor   = AppPalette.card,
            contentColor     = AppPalette.accent
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text(stringResource(R.string.debug_tab_all, rawEntries.size), color = if (selectedTab == 0) AppPalette.accent else AppPalette.textMuted,
                    modifier = Modifier.padding(vertical = 10.dp), fontSize = 12.sp)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text(stringResource(R.string.debug_tab_bank, bankEntries.size), color = if (selectedTab == 1) AppPalette.accent else AppPalette.textMuted,
                    modifier = Modifier.padding(vertical = 10.dp), fontSize = 12.sp)
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text(stringResource(R.string.debug_tab_service), color = if (selectedTab == 2) AppPalette.accent else AppPalette.textMuted,
                    modifier = Modifier.padding(vertical = 10.dp), fontSize = 12.sp)
            }
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }) {
                Text(stringResource(R.string.debug_tab_battery), color = if (selectedTab == 3) AppPalette.accent else AppPalette.textMuted,
                    modifier = Modifier.padding(vertical = 10.dp), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        when (selectedTab) {
            // ── Tab 0: All notifications (no filter) ─────────────────────────
            0 -> {
                if (rawEntries.isEmpty()) {
                    EmptyState(stringResource(R.string.debug_empty_raw))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(rawEntries) { RawEntryCard(it) }
                    }
                }
            }
            // ── Tab 1: Bank parsed entries ────────────────────────────────────
            1 -> {
                if (bankEntries.isEmpty()) {
                    EmptyState(stringResource(R.string.debug_empty_bank))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(bankEntries) { BankEntryCard(it) }
                    }
                }
            }
            // ── Tab 2: Service lifecycle events ──────────────────────────────
            2 -> {
                if (serviceEvents.isEmpty()) {
                    EmptyState(stringResource(R.string.debug_empty_service))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(serviceEvents) { event ->
                            Surface(shape = RoundedCornerShape(8.dp), color = AppPalette.card) {
                                Text(event, color = AppPalette.textPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.fillMaxWidth().padding(10.dp))
                            }
                        }
                    }
                }
            }
            // ── Tab 3: Battery & Device fix guide ────────────────────────────
            3 -> BatteryGuideTab(context)
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ── Battery Guide Tab ─────────────────────────────────────────────────────────

@Composable
private fun BatteryGuideTab(context: android.content.Context) {
    val steps = listOf(
        BatteryStep(
            title   = stringResource(R.string.debug_battery_step1_title),
            detail  = stringResource(R.string.debug_battery_step1_detail),
            action  = stringResource(R.string.debug_battery_step1_action),
            intent  = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        ),
        BatteryStep(
            title   = stringResource(R.string.debug_battery_step2_title),
            detail  = stringResource(R.string.debug_battery_step2_detail),
            action  = stringResource(R.string.debug_battery_step2_action),
            intent  = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
        ),
        BatteryStep(
            title   = stringResource(R.string.debug_battery_step3_title),
            detail  = stringResource(R.string.debug_battery_step3_detail),
            action  = stringResource(R.string.debug_battery_step3_action),
            intent  = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        ),
        BatteryStep(
            title   = stringResource(R.string.debug_battery_step4_title),
            detail  = stringResource(R.string.debug_battery_step4_detail),
            action  = null,
            intent  = null
        )
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Surface(shape = RoundedCornerShape(10.dp), color = AppPalette.errorChipBg.copy(alpha = 0.3f)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(
                        stringResource(R.string.debug_battery_warning_title),
                        color = AppPalette.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.debug_battery_warning_desc),
                        color = AppPalette.textMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        items(steps) { step ->
            Surface(shape = RoundedCornerShape(10.dp), color = AppPalette.card,
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(step.title, color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(step.detail, color = AppPalette.textMuted,
                        style = MaterialTheme.typography.bodySmall)
                    if (step.action != null && step.intent != null) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    step.intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(step.intent)
                                } catch (e: Exception) {
                                    android.util.Log.e("BankService", "Cannot open settings: ${e.message}")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(step.action, color = AppPalette.accent, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private data class BatteryStep(
    val title: String,
    val detail: String,
    val action: String?,
    val intent: android.content.Intent?
)

@Composable
private fun RawEntryCard(entry: RawNotificationEntry) {
    val timeFmt: (Long) -> String = remember { { ts: Long -> DateFormatter.formatTimeFull(ts) } }
    val isBank   = BankNotificationParser.isSupportedPackage(entry.packageName)
    val bgColor  = if (isBank) AppPalette.successChipBg.copy(alpha = 0.3f) else AppPalette.card
    Surface(shape = RoundedCornerShape(10.dp), color = bgColor, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(entry.packageName, color = if (isBank) AppPalette.success else AppPalette.textMuted,
                    style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isBank) StatusChip("BANK", true)
                    Text(timeFmt(entry.timestamp), color = AppPalette.textMuted,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
            if (entry.title.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                DebugRow("title",   entry.title)
            }
            if (entry.content.isNotBlank()) {
                DebugRow("content", entry.content.take(120))
            }
            if (entry.category.isNotBlank()) {
                DebugRow("cat",     entry.category)
            }
        }
    }
}

@Composable
private fun BankEntryCard(entry: NotificationDebugEntry) {
    val timeFmt: (Long) -> String = remember { { ts: Long -> DateFormatter.formatTimeFull(ts) } }
    val bgColor = if (entry.parseSuccess) AppPalette.successChipBg.copy(alpha = 0.3f) else AppPalette.errorChipBg.copy(alpha = 0.3f)
    Surface(shape = RoundedCornerShape(10.dp), color = bgColor, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(entry.bankName.ifBlank { entry.packageName }, fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary, style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusChip(if (entry.parseSuccess) "OK" else "FAIL", entry.parseSuccess)
                    Text(timeFmt(entry.timestamp), color = AppPalette.textMuted,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(4.dp))
            DebugRow("title",   entry.rawTitle)
            DebugRow("content", entry.rawContent.take(100))
            if (entry.parseSuccess) {
                HorizontalDivider(color = AppPalette.cardBorder, modifier = Modifier.padding(vertical = 4.dp))
                DebugRow("amount",   entry.parsedAmount?.let { NumberFormatter.formatCurrency(it) } ?: "-")
                DebugRow("merchant", entry.parsedMerchant ?: "-")
                DebugRow("type",     entry.parsedType?.name ?: "-")
                DebugRow("category", entry.parsedCategory ?: "-")
                if (entry.transactionCreated) {
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = AppPalette.success, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.debug_draft_created), color = AppPalette.success, style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                entry.errorMessage?.let {
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = AppPalette.error, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(it, color = AppPalette.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = AppPalette.textMuted, style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text("$label:", color = AppPalette.textMuted, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(100.dp))
        Text(value, color = AppPalette.textPrimary, style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace, fontSize = 10.sp)
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text("$label: ", color = AppPalette.textMuted, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(56.dp))
        Text(value, color = AppPalette.textPrimary, style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace, fontSize = 10.sp)
    }
}

@Composable
private fun StatusChip(label: String, ok: Boolean) {
    Surface(shape = RoundedCornerShape(50),
        color = if (ok) AppPalette.successChipBg else AppPalette.errorChipBg) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = if (ok) AppPalette.success else AppPalette.error,
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}
