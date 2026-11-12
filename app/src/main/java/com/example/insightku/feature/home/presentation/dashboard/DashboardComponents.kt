package com.example.insightku.feature.home.presentation.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.insightku.core.ui.theme.LocalAccent

// NavPurple — matches bottom nav primary action color exactly. Now driven by the global accent
// (LocalAccent) so the accent picker recolors the dashboard live. Composable getter — used only in
// composable scope (modifiers/tints), never inside a Canvas DrawScope.
internal val NavPurple: Color
    @Composable get() = LocalAccent.current
