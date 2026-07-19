package com.example.insightku.feature.settings.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.insightku.R
import com.example.insightku.core.ui.theme.Dimens

@Composable
fun LogoutConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape           = RoundedCornerShape(Dimens.BottomSheetRadius),
            color           = MaterialTheme.colorScheme.surface,
            tonalElevation  = 6.dp,
            shadowElevation = 0.dp,
            modifier        = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier            = Modifier.padding(Dimens.CardInnerPaddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
            ) {
                // ── Icon ─────────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onErrorContainer,
                        modifier           = Modifier.size(28.dp)
                    )
                }

                // ── Copy ──────────────────────────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
                ) {
                    Text(
                        text       = stringResource(R.string.settings_logout_title),
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        textAlign  = TextAlign.Center
                    )
                    Text(
                        text      = stringResource(R.string.settings_logout_message),
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // ── Actions ───────────────────────────────────────────────────
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
                ) {
                    Button(
                        onClick  = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.ButtonHeightPrimary),
                        shape  = RoundedCornerShape(Dimens.ButtonRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor   = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            modifier           = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(Dimens.PaddingMedium))
                        Text(stringResource(R.string.settings_logout_button), fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.ButtonHeightSecondary),
                        shape  = RoundedCornerShape(Dimens.ButtonRadius),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            stringResource(R.string.settings_logout_stay),
                            fontWeight = FontWeight.Medium,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
