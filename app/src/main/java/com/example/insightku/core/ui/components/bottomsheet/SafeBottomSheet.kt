package com.example.insightku.core.ui.components.bottomsheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent

/**
 * SafeBottomSheet — satu-satunya wrapper Bottom Sheet di InsightKu.
 *
 * - Menjaga background sheet tidak menembus status bar (guardTop = statusBar + 24dp).
 * - Menyediakan footer aksi yang tetap terlihat saat konten discroll / keyboard muncul.
 * - Semua inset (termasuk IME) dikelola di sini, bukan di tiap pemanggil.
 *
 * Pemanggil cukup: `SafeBottomSheet(onDismissRequest = ...) { SheetHeader(...); ... }`
 * dan, bila ada aksi utama, `footer = { SheetPrimaryButton(...) }`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    shape: Shape = RoundedCornerShape(topStart = Dimens.BottomSheetRadius, topEnd = Dimens.BottomSheetRadius),
    containerColor: Color = AppPalette.card,
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = Color.Black.copy(alpha = 0.32f),
    contentWindowInsets: WindowInsets = WindowInsets.safeDrawing,
    dragHandle: @Composable (() -> Unit)? = { DefaultDragHandle() },
    footer: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val resolvedContentColor = contentColorFor(containerColor)

    val view = LocalView.current
    val density = LocalDensity.current
    val windowInsetsCompat = remember { WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets) }
    val statusBarPx = remember { windowInsetsCompat.getInsets(WindowInsetsCompat.Type.statusBars()).top }

    val guardTop = remember { with(density) { statusBarPx.toDp() + 24.dp } }
    val guardedInsets = remember(contentWindowInsets, guardTop) {
        WindowInsets(top = guardTop).union(contentWindowInsets)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        shape = shape,
        containerColor = Color.Transparent,
        contentColor = resolvedContentColor,
        tonalElevation = tonalElevation,
        scrimColor = scrimColor,
        contentWindowInsets = { guardedInsets },
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(containerColor)
                .imePadding()
        ) {
            if (dragHandle != null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    dragHandle()
                }
            }
            if (footer != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) { content() }
                Column(modifier = Modifier.fillMaxWidth()) { footer() }
            } else {
                content()
            }
        }
    }
}

/** Satu-satunya drag handle: 40 × 4dp, hairline netral. */
@Composable
fun DefaultDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.PaddingMedium, bottom = Dimens.PaddingMedium),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(Dimens.BottomSheetHandleWidth)
                .height(Dimens.BottomSheetHandleHeight)
                .clip(RoundedCornerShape(Dimens.BottomSheetHandleRadius))
                .background(AppPalette.cardBorder)
        )
    }
}

/** Header tenang: judul + satu kalimat konteks. Tanpa chip, tanpa divider. */
@Composable
fun SheetHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Dimens.BottomSheetContentPadding,
                vertical = Dimens.PaddingLarge
            ),
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = AppPalette.textPrimary
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted
            )
        }
    }
}

/** Aksi utama: aksen + teks gelap (kontras 4.5:1), 52dp, radius token. */
@Composable
fun SheetPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.ButtonHeightPrimary),
        shape = RoundedCornerShape(Dimens.ButtonRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = LocalAccent.current,
            contentColor = AppPalette.textPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(Dimens.IconSizeMedium))
            Spacer(Modifier.width(Dimens.PaddingMedium))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
    }
}

/** Aksi sekunder: outline hairline, teks primer, tinggi & radius sama. */
@Composable
fun SheetSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.ButtonHeightPrimary),
        shape = RoundedCornerShape(Dimens.ButtonRadius),
        border = BorderStroke(1.dp, AppPalette.cardBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppPalette.textPrimary)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(Dimens.IconSizeMedium))
            Spacer(Modifier.width(Dimens.PaddingMedium))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
    }
}

/** Aksi ketiga / cancel: text button 48dp target. */
@Composable
fun SheetTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(Dimens.ButtonHeightSecondary)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = AppPalette.textMuted)
    }
}
