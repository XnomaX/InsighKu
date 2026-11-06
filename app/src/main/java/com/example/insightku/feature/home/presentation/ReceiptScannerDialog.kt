package com.example.insightku.feature.home.presentation

import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.insightku.R
import com.example.insightku.core.data.model.Account
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.LocalAccent

/**
 * MediaStore-based capture needs API 29+: `VOLUME_EXTERNAL_PRIMARY` and
 * `RELATIVE_PATH` are both Q additions, and below Q writing to MediaStore
 * requires WRITE_EXTERNAL_STORAGE, which this app deliberately does not
 * request. Below Q the scanner offers the gallery picker only.
 */
private val CAMERA_CAPTURE_SUPPORTED = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

@Composable
fun ReceiptScannerDialog(
    isOpen: Boolean,
    isIncome: Boolean,
    uiState: ScannerUiState,
    accounts: List<Account> = emptyList(),
    onDismiss: () -> Unit,
    onImageSelected: (Uri, fromCamera: Boolean) -> Unit,
    onConfirm: (TransactionFormData) -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingPhotoUri
        if (saved && uri != null) onImageSelected(uri, true)
        else onRetry()
    }
    val pickFromGallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) onImageSelected(uri, false) else onRetry()
    }
    val requestCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val uri = pendingPhotoUri
        if (granted && uri != null) takePhoto.launch(uri)
        else onRetry()
    }

    fun launchCamera() {
        // Guard: the API-29 surface below is unreachable because the button is
        // hidden below Q (CAMERA_CAPTURE_SUPPORTED). Kept as a hard check so the
        // invariant holds even if another call site is added later.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            onRetry()
            return
        }

        // Capture straight into MediaStore instead of a FileProvider URI.
        //
        // The FileProvider + cross-app grant approach fails on this device's
        // ROM (LineageOS-based): queryIntentActivities() returns 0 even with a
        // <queries> manifest entry, getPackageInfo() throws NameNotFoundException
        // for the camera app, and grantUriPermission(by-name) is a silent no-op
        // for packages not visible to us (Android 11+ package-visibility gate).
        // Intent FLAG_GRANT_* + ClipData grants are also ignored by this ROM.
        //
        // MediaStore avoids all of that: it is the system media provider, the
        // launched camera app already has media-write access, so no per-URI
        // grant from us is required. We delete the row after the scan (see
        // ReceiptScanRepository) since receipts carry financial data.
        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "receipt_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/InsightKu")
            // Do NOT set IS_PENDING=1 here: a pending row is owned by this
            // app, so the camera app (different uid) cannot write it without
            // a per-URI grant — and per-URI grants are broken on this ROM.
            // Leaving it non-pending lets a camera app with media-write
            // access update the row. We still delete it after the scan.
        }
        val uri = resolver.insert(collection, values)
        if (uri == null) {
            onRetry()
            return
        }
        pendingPhotoUri = uri

        val granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) takePhoto.launch(uri) else requestCamera.launch(android.Manifest.permission.CAMERA)
    }

    if (isOpen) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = GlassSurface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp), clip = false)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    ScannerHeader(isIncome = isIncome, onClose = onDismiss)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .padding(top = 16.dp, bottom = 20.dp)
                    ) {
                        val defaultAccountId = accounts.firstOrNull { it.isDefault }?.id
                            ?: accounts.firstOrNull()?.id ?: ""
                        when (uiState.state) {
                            ScanState.READY -> ReadyToScanContent(
                                isIncome = isIncome,
                                onTakePhoto = ::launchCamera,
                                showTakePhoto = CAMERA_CAPTURE_SUPPORTED,
                                onGallery = {
                                    pickFromGallery.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                            )
                            ScanState.PROCESSING -> ProcessingContent(
                                isIncome = isIncome,
                                text = stringResource(R.string.scanner_processing)
                            )
                            ScanState.SCANNED -> ScannedContent(
                                formData = uiState.form,
                                isIncome = isIncome,
                                accounts = accounts,
                                defaultAccountId = defaultAccountId,
                                onCancel = onDismiss,
                                onSubmit = { edited -> onConfirm(edited) }
                            )
                            ScanState.ERROR -> ErrorContent(
                                offline = uiState.offline,
                                detail = uiState.error,
                                onRetry = onRetry,
                                onDismiss = onDismiss
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Header ──────────────────────────────────────────────────────────────────

@Composable
private fun ScannerHeader(isIncome: Boolean, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPalette.card)
            .padding(start = 18.dp, end = 8.dp, top = 16.dp, bottom = 16.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text(
                text = stringResource(if (isIncome) R.string.scanner_title_income else R.string.scanner_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textPrimary
            )
            Text(
                text = stringResource(R.string.scanner_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(36.dp)
                .clip(CircleShape)
                .background(AppPalette.cardBorder)
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.close),
                tint = AppPalette.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppPalette.cardBorder)
    )
}

// ─── Ready ───────────────────────────────────────────────────────────────────

@Composable
fun ReadyToScanContent(
    isIncome: Boolean,
    onTakePhoto: () -> Unit,
    onGallery: () -> Unit,
    // MediaStore capture is API 29+ only (see launchCamera). Below that the
    // button is hidden so users are routed to the gallery picker, which the
    // AndroidX photo-picker backport supports on every API we target.
    showTakePhoto: Boolean = true
) {
    val accent = if (isIncome) IncomeGreen else LocalAccent.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(accent.copy(alpha = 0.1f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(if (isIncome) R.string.scanner_scan_income_doc else R.string.scanner_scan_receipt),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppPalette.textPrimary
        )
        if (showTakePhoto) {
            Text(
                stringResource(if (isIncome) R.string.scanner_point_camera_income else R.string.scanner_point_camera_receipt),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        // Below API 29 the helper line is omitted rather than re-worded: the
        // camera-specific copy would not apply, and the generic fallback
        // (scanner_subtitle) is already rendered by ScannerHeader above, which
        // would show the same sentence twice.
        Spacer(Modifier.height(8.dp))
        if (showTakePhoto) {
            PillButton(
                text = stringResource(R.string.scanner_take_photo),
                icon = Icons.Default.CameraAlt,
                onClick = onTakePhoto,
                containerColor = accent
            )
        }
        GhostPillButton(
            text = stringResource(R.string.scanner_upload_gallery),
            icon = Icons.Default.Upload,
            onClick = onGallery,
            modifier = Modifier.padding(top = if (showTakePhoto) 8.dp else 0.dp)
        )
    }
}

// ─── Processing ──────────────────────────────────────────────────────────────

@Composable
fun ProcessingContent(isIncome: Boolean, text: String) {
    // Make the wait state unmissable: large spinner + an indeterminate
    // progress bar so the user knows inference (~10-15s on CPU) is actually
    // running, not frozen.
    val accent = if (isIncome) IncomeGreen else LocalAccent.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(accent.copy(alpha = 0.1f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = accent,
                strokeWidth = 5.dp,
                modifier = Modifier.size(44.dp)
            )
        }
        Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = AppPalette.textPrimary)
        Text(
            stringResource(if (isIncome) R.string.scanner_extracting_income else R.string.scanner_extracting_transaction),
            style = MaterialTheme.typography.bodySmall,
            color = AppPalette.textMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            color = accent,
            trackColor = accent.copy(alpha = 0.15f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )
    }
}

// ─── Scanned (editable form, aligned with AddTransaction's ManualForm) ───────

@Composable
fun ScannedContent(
    formData: TransactionFormData,
    isIncome: Boolean,
    accounts: List<Account>,
    defaultAccountId: String,
    onCancel: () -> Unit,
    onSubmit: (TransactionFormData) -> Unit
) {
    // Local editable copy seeded from the scan result; resets on a new scan.
    // Preselect the default account so the form is valid without extra taps,
    // matching AddTransaction's behaviour.
    var form by remember(formData, defaultAccountId) {
        mutableStateOf(formData.copy(accountId = formData.accountId.ifBlank { defaultAccountId }))
    }
    val categories = if (isIncome) listOf(
        stringResource(R.string.scanner_cat_salary),
        stringResource(R.string.scanner_cat_freelance),
        stringResource(R.string.scanner_cat_gift)
    ) else listOf(
        stringResource(R.string.scanner_cat_food),
        stringResource(R.string.scanner_cat_transport),
        stringResource(R.string.scanner_cat_shopping)
    )

    val accent = if (isIncome) IncomeGreen else LocalAccent.current
    val isFormValid = form.amount.isNotBlank() &&
        form.amount.toDoubleOrNull() != null &&
        form.accountId.isNotBlank()
    val saveBgColor = when {
        !isFormValid -> AppPalette.placeholder
        isIncome -> AppPalette.success
        else -> AppPalette.accent
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Success banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(IncomeGreen.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(if (isIncome) R.string.scanner_income_scanned else R.string.scanner_receipt_scanned),
                color = IncomeGreen,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Amount — mirrors AddTransaction's AmountHeroCard
        AmountHeroCard(
            amount = form.amount,
            isIncome = isIncome,
            onAmountChange = { form = form.copy(amount = it) },
            onImeAction = {},
            onFocusChange = {}
        )

        // Merchant / income source
        FormSectionCard {
            FormSectionTitle(stringResource(R.string.transaction_details_section))
            Spacer(Modifier.height(12.dp))
            FinanceField(
                icon = if (isIncome) Icons.Default.Work else Icons.Default.Store,
                value = form.merchant,
                onValueChange = { form = form.copy(merchant = it) },
                placeholder = stringResource(if (isIncome) R.string.income_source else R.string.merchant_store)
            )
        }

        // Category chips
        FormSectionCard {
            FormSectionTitle(stringResource(R.string.category))
            Spacer(Modifier.height(12.dp))
            ScannerCategoryChips(
                categories = categories,
                selected = form.category,
                onSelect = { form = form.copy(category = it) },
                accent = accent
            )
        }

        // Account — every transaction must belong to an account (mirrors
        // AddTransaction's AccountChipSelector).
        FormSectionCard {
            FormSectionTitle(stringResource(R.string.account))
            Spacer(Modifier.height(12.dp))
            AccountChipSelector(
                accounts = accounts,
                selectedAccountId = form.accountId,
                onAccountSelected = { form = form.copy(accountId = it) }
            )
        }

        // Date & note
        FormSectionCard {
            FormSectionTitle(stringResource(R.string.date_and_note))
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = AppPalette.background,
                    border = BorderStroke(1.dp, AppPalette.cardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accent.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = DateFormatter.formatNumericDate(form.dateMillis),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = AppPalette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                FinanceField(
                    icon = Icons.AutoMirrored.Filled.Notes,
                    value = form.description,
                    onValueChange = { form = form.copy(description = it) },
                    placeholder = stringResource(R.string.note_placeholder),
                    singleLine = false,
                    modifier = Modifier.height(80.dp)
                )
            }
        }

        // Save button — pill, mirrors AddTransaction's save button
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(saveBgColor)
                .clickable(enabled = isFormValid) { onSubmit(form) },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (isFormValid) Color.White else AppPalette.textMuted,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(if (isIncome) R.string.add_income else R.string.add_expense),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isFormValid) Color.White else AppPalette.textMuted
                )
            }
        }

        // Cancel ghost button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(50.dp))
                .border(1.dp, AppPalette.cardBorder, RoundedCornerShape(50.dp))
                .clickable { onCancel() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = AppPalette.textMuted, modifier = Modifier.size(16.dp))
                Text(
                    stringResource(R.string.cancel),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = AppPalette.textMuted
                )
            }
        }
    }
}

@Composable
private fun ScannerCategoryChips(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val isSelected = category == selected
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(category) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) accent.copy(alpha = 0.12f) else AppPalette.background,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) accent else AppPalette.cardBorder
                )
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) accent else AppPalette.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

// ─── Error ───────────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(
    offline: Boolean,
    detail: String?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(AppPalette.error.copy(alpha = 0.1f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.WifiOff, contentDescription = null, tint = AppPalette.error, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(if (offline) R.string.scanner_error_offline else R.string.scanner_error_generic),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppPalette.textPrimary,
            textAlign = TextAlign.Center
        )
        if (!offline && !detail.isNullOrBlank()) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted,
                modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(20.dp))
        PillButton(
            text = stringResource(R.string.try_again),
            onClick = onRetry,
            containerColor = AppPalette.accent
        )
        GhostPillButton(
            text = stringResource(R.string.cancel),
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            onClick = onDismiss,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// ─── Shared pill buttons (mirror AddTransaction's save / back buttons) ───────

@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(containerColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun GhostPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(50.dp))
            .border(1.dp, AppPalette.cardBorder, RoundedCornerShape(50.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = AppPalette.textMuted, modifier = Modifier.size(16.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppPalette.textMuted
            )
        }
    }
}
