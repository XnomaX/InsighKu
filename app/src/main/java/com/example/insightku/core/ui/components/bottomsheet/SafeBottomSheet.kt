package com.example.insightku.core.ui.components.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.core.view.WindowInsetsCompat
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens

/**
 * SafeBottomSheet — A drop-in wrapper around ModalBottomSheet that:
 *
 * 1. Keeps the sheet background from reaching the status bar when fully expanded
 * 2. Manages lifecycle correctly to prevent stuck states
 * 3. Synchronizes scrim with sheet visibility
 *
 * Usage: Replace `ModalBottomSheet` with `SafeBottomSheet` everywhere.
 * The `content` lambda is identical — all existing UI is preserved unchanged.
 *
 * ## Status Bar Guard
 * ModalBottomSheet's Surface aligns to TopCenter; when content is tall enough to
 * fully expand, the Surface offset → 0 and its background fills behind the status bar.
 * Fix: the Surface is made transparent (`containerColor = Color.Transparent`) and the
 * visible card background is applied to an inner Column that sits inside the guarded
 * top inset (`statusBar + 24dp`), so it can never visually reach the status bar.
 * The drag handle is rendered inside this Column so it shares the card background.
 *
 * ## Lifecycle Management
 * ModalBottomSheet already invokes onDismissRequest only AFTER the hide animation
 * completes, so the callback is passed through unchanged — no extra deferral is
 * needed here.
 *
 * ## Caller Pattern
 * ```kotlin
 * var showSheet by remember { mutableStateOf(false) }
 * if (showSheet) {
 *     SafeBottomSheet(
 *         onDismissRequest = { showSheet = false }
 *     ) { ... }
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(topStart = Dimens.BottomSheetRadius, topEnd = Dimens.BottomSheetRadius),
    containerColor: Color = AppPalette.card,
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = Color.Black.copy(alpha = 0.32f),
    contentWindowInsets: WindowInsets = WindowInsets.safeDrawing,
    dragHandle: @Composable (() -> Unit)? = { DefaultDragHandle() },
    content: @Composable ColumnScope.() -> Unit
) {
    val resolvedContentColor = contentColorFor(containerColor)

    val view = LocalView.current
    val density = LocalDensity.current
    val windowInsetsCompat = remember { WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets) }
    val statusBarPx = remember { windowInsetsCompat.getInsets(WindowInsetsCompat.Type.statusBars()).top }

    // Guard: statusBar + 24dp margin as top inset → anchored-draggable includes it
    // in the sheet's measured height → correct dismiss threshold (no bounce on fling).
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
        // Surface is transparent; the visible card background lives here, inside the
        // guarded top inset, so it can never reach up into the status bar — even when
        // the sheet is fully expanded (offset 0). The drag handle is drawn inside the
        // card so it shares the background.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(containerColor)
        ) {
            if (dragHandle != null) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    dragHandle()
                }
            }
            content()
        }
    }
}

/**
 * Default drag handle matching the InsightKu design system.
 */
@Composable
fun DefaultDragHandle() {
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
                .background(AppPalette.cardBorder)
        )
    }
}
