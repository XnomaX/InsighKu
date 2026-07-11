package com.example.insightku.core.ui.components.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * 1. Constrains sheet height so it never reaches the status bar
 * 2. Manages lifecycle correctly to prevent stuck states
 * 3. Synchronizes scrim with sheet visibility
 *
 * Usage: Replace `ModalBottomSheet` with `SafeBottomSheet` everywhere.
 * The `content` lambda is identical — all existing UI is preserved unchanged.
 *
 * ## Height Constraint
 * Tall Bottom Sheets stop expanding ~24dp below the status bar.
 * The content inside becomes scrollable via ModalBottomSheet's internal scroll.
 * Small Bottom Sheets continue using their natural height.
 *
 * ## Lifecycle Management
 * ModalBottomSheet calls onDismissRequest BEFORE the hide animation completes.
 * This wrapper defers the callback until the animation completes via
 * sheetState.isVisible observation, preventing stuck states.
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
    val maxHeight = remember {
        val windowInsetsCompat = WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets)
        val statusBarPx = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.statusBars()).top
        val screenHeightPx = view.rootView.height
        val safeMarginPx = with(density) { 24.dp.toPx() }
        with(density) { (screenHeightPx - statusBarPx - safeMarginPx).toDp() }
    }

    // Track whether a dismiss is pending (animation in progress)
    var pendingDismiss by remember { mutableStateOf(false) }

    LaunchedEffect(sheetState.isVisible, pendingDismiss) {
        if (!sheetState.isVisible && pendingDismiss) {
            pendingDismiss = false
            onDismissRequest()
        } else if (sheetState.isVisible) {
            pendingDismiss = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            pendingDismiss = true
        },
        sheetState = sheetState,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = resolvedContentColor,
        tonalElevation = tonalElevation,
        scrimColor = scrimColor,
        contentWindowInsets = { contentWindowInsets },
        dragHandle = dragHandle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
        ) {
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
