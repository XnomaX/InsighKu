package com.example.insightku.core.utils

import android.app.Activity
import android.os.Build
import android.view.Surface

/**
 * Requests the highest supported display mode (90/120Hz+) when available.
 * No-ops safely on 60Hz-only devices or when the system clamps the rate
 * (battery saver / thermal). Re-call from onResume / power-save changes.
 *
 * Caveat: high Hz only helps if frame time stays under budget
 * (~8.3ms @ 120Hz, ~11.1ms @ 90Hz). Profile with:
 * Developer Options → Profile GPU Rendering / Layout Inspector.
 * Heavy Compose recomposition still looks janky at 120Hz.
 */
fun Activity.enableHighestRefreshRate() {
    try {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        } ?: return

        // Prefer same resolution as current mode so we don't drop to a
        // lower-res high-Hz mode some OEMs advertise.
        val current = display.mode
        val best = display.supportedModes
            .filter {
                it.physicalWidth == current.physicalWidth &&
                    it.physicalHeight == current.physicalHeight
            }
            .maxByOrNull { it.refreshRate }
            ?: display.supportedModes.maxByOrNull { it.refreshRate }
            ?: return

        // preferredDisplayModeId is the reliable OEM path (API 23+).
        val attrs = window.attributes
        if (attrs.preferredDisplayModeId != best.modeId) {
            attrs.preferredDisplayModeId = best.modeId
            window.attributes = attrs
        }

        // API 30+: hint the compositor for this window's surface.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.setFrameRate(
                best.refreshRate,
                Surface.FRAME_RATE_COMPATIBILITY_DEFAULT,
            )
        }
    } catch (_: Exception) {
        // Device/OEM quirk — leave system default (typically 60Hz).
        try {
            val attrs = window.attributes
            attrs.preferredDisplayModeId = 0 // 0 = no preference
            window.attributes = attrs
        } catch (_: Exception) {
            // ignore
        }
    }
}
