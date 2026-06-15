package com.example.insightku.feature.settings.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.insightku.core.data.repository.MonitorableApp
import com.example.insightku.core.notification.BankNotificationListenerService
import com.example.insightku.core.ui.theme.Dimens

/**
 * Onboarding Deteksi Otomatis — membangun kepercayaan SEBELUM meminta izin.
 *
 * Urutan: value → kontrak transparansi (apa yg dibaca vs TIDAK) → pilih aplikasi
 * → izin Notification Access → baterai (opsional, jujur). Bahasa manusia, tanpa
 * istilah teknis (NotificationListenerService/background/unrestricted).
 */
private enum class OnboardingStep { VALUE, TRANSPARENCY, PICK_APPS, PERMISSION, BATTERY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoDetectionOnboardingScreen(
    onFinish: () -> Unit,
    viewModel: BankWhitelistViewModel = hiltViewModel()
) {
    var step by remember { mutableStateOf(OnboardingStep.VALUE) }
    val apps by viewModel.apps.collectAsState()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    var listenerEnabled by remember { mutableStateOf(false) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listenerEnabled = BankNotificationListenerService.isEnabled(context)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { listenerEnabled = BankNotificationListenerService.isEnabled(context) }

    fun back() {
        step = when (step) {
            OnboardingStep.VALUE        -> { onFinish(); return }
            OnboardingStep.TRANSPARENCY -> OnboardingStep.VALUE
            OnboardingStep.PICK_APPS    -> OnboardingStep.TRANSPARENCY
            OnboardingStep.PERMISSION   -> OnboardingStep.PICK_APPS
            OnboardingStep.BATTERY      -> OnboardingStep.PERMISSION
        }
    }

    Scaffold(
        containerColor = SettingsPalette.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    TextButton(onClick = onFinish) {
                        Text("Lewati", color = SettingsPalette.textMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SettingsPalette.background,
                    navigationIconContentColor = SettingsPalette.textPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SettingsPalette.background)
                .padding(padding)
                .padding(horizontal = Dimens.ScreenHorizontalPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing)
        ) {
            when (step) {
                OnboardingStep.VALUE -> StepValue { step = OnboardingStep.TRANSPARENCY }
                OnboardingStep.TRANSPARENCY -> StepTransparency { step = OnboardingStep.PICK_APPS }
                OnboardingStep.PICK_APPS -> StepPickApps(
                    apps = apps,
                    onToggle = { pkg, allowed -> viewModel.setAllowed(pkg, allowed) },
                    onNext = { step = OnboardingStep.PERMISSION }
                )
                OnboardingStep.PERMISSION -> StepPermission(
                    granted = listenerEnabled,
                    onOpenSettings = { BankNotificationListenerService.openSettings(context) },
                    onNext = { step = OnboardingStep.BATTERY }
                )
                OnboardingStep.BATTERY -> StepBattery(onDone = onFinish)
            }
        }
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = SettingsPalette.textPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = SettingsPalette.textMuted)
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(Dimens.ButtonHeightPrimary),
        shape = RoundedCornerShape(Dimens.ButtonRadius),
        colors = ButtonDefaults.buttonColors(containerColor = SettingsPalette.Purple)
    ) { Text(text, fontWeight = FontWeight.SemiBold, color = Color.White) }
}

@Composable
private fun StepValue(onNext: () -> Unit) {
    StepHeader(
        "Belanja, langsung siap dicatat",
        "Saat ada notifikasi transaksi dari aplikasi bank-mu, InsighKu menyiapkan draft yang tinggal kamu cek. Kamu tetap yang memutuskan apa yang disimpan."
    )
    Spacer(Modifier.height(Dimens.PaddingMedium))
    PrimaryButton("Mulai", onNext)
}

@Composable
private fun StepTransparency(onNext: () -> Unit) {
    StepHeader("Apa yang kami lakukan — dan tidak", "Transparansi penuh sebelum kamu memberi izin apa pun.")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        color = SettingsPalette.card,
        border = BorderStroke(1.dp, SettingsPalette.cardBorder)
    ) {
        Column(Modifier.padding(Dimens.CardInnerPaddingLarge), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TransparencyRow(true,  "Membaca notifikasi dari aplikasi yang kamu pilih")
            TransparencyRow(true,  "Mengambil nominal & nama toko untuk draft")
            TransparencyRow(true,  "Memproses semuanya di HP-mu saja")
            TransparencyRow(false, "Membaca SMS, chat, atau aplikasi lain")
            TransparencyRow(false, "Mengakses rekening atau saldo bank")
            TransparencyRow(false, "Mengirim isi notifikasi ke server")
            TransparencyRow(false, "Menyimpan transaksi tanpa izinmu")
        }
    }
    Spacer(Modifier.height(Dimens.PaddingMedium))
    PrimaryButton("Saya mengerti", onNext)
}

@Composable
private fun TransparencyRow(positive: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val tint = if (positive) SettingsPalette.IncomeGreen else SettingsPalette.ExpenseRed
        Box(
            modifier = Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (positive) Icons.Default.Check else Icons.Default.Close, null, tint = tint, modifier = Modifier.size(15.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = SettingsPalette.textPrimary)
    }
}

@Composable
private fun StepPickApps(
    apps: List<MonitorableApp>,
    onToggle: (String, Boolean) -> Unit,
    onNext: () -> Unit
) {
    val installed = apps.filter { it.isInstalled }
    StepHeader(
        "Pilih aplikasi yang dipantau",
        if (installed.isEmpty())
            "Kami belum mendeteksi aplikasi keuangan yang didukung di HP-mu. Kamu bisa mengatur ini nanti di Pengaturan."
        else
            "Ini aplikasi keuangan yang terpasang di HP-mu. Nyalakan yang ingin dipantau."
    )
    installed.forEach { app ->
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.CardRadius),
            color = SettingsPalette.card,
            border = BorderStroke(1.dp, SettingsPalette.cardBorder)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(Dimens.CardInnerPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(app.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = SettingsPalette.textPrimary)
                Switch(
                    checked = app.isAllowed,
                    onCheckedChange = { onToggle(app.packageName, it) },
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
    Spacer(Modifier.height(Dimens.PaddingMedium))
    PrimaryButton("Lanjut", onNext)
}

@Composable
private fun StepPermission(
    granted: Boolean,
    onOpenSettings: () -> Unit,
    onNext: () -> Unit
) {
    IconBadge(Icons.Default.NotificationsActive)
    StepHeader(
        "Satu izin terakhir",
        "Android akan menampilkan daftar panjang dengan bahasa teknis. Cari InsighKu, lalu nyalakan. Itu saja."
    )
    if (granted) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.CardRadius),
            color = SettingsPalette.IncomeGreen.copy(alpha = 0.12f)
        ) {
            Row(Modifier.padding(Dimens.CardInnerPadding), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Check, null, tint = SettingsPalette.IncomeGreen, modifier = Modifier.size(18.dp))
                Text("Izin sudah diberikan", color = SettingsPalette.textPrimary, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(Dimens.PaddingMedium))
        PrimaryButton("Lanjut", onNext)
    } else {
        PrimaryButton("Buka pengaturan izin", onOpenSettings)
        TextButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("Nanti saja", color = SettingsPalette.textMuted)
        }
    }
}

@Composable
private fun StepBattery(onDone: () -> Unit) {
    IconBadge(Icons.Default.Shield)
    StepHeader(
        "Agar deteksi tetap berjalan",
        "Beberapa HP mematikan aplikasi di latar belakang untuk menghemat baterai. Kalau deteksi sering meleset, mengizinkan InsighKu berjalan di latar membantu. Fitur tetap jalan tanpa ini — yang terlewat selalu bisa kamu catat manual."
    )
    Spacer(Modifier.height(Dimens.PaddingMedium))
    PrimaryButton("Selesai", onDone)
}

@Composable
private fun IconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(SettingsPalette.tint(SettingsPalette.Purple)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = SettingsPalette.Purple, modifier = Modifier.size(30.dp))
    }
}
