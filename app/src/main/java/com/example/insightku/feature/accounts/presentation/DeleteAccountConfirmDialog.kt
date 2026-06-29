package com.example.insightku.feature.accounts.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.ExpenseRed

/**
 * Delete account confirmation dialog.
 * Used by both AccountDetailSheet and AccountRow in AccountsScreen,
 * and by EditAccountDialog.
 */
@Composable
fun DeleteAccountConfirmDialog(
    accountName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(Dimens.BottomSheetRadius),
            color = AppPalette.card,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Delete Account?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary
                )

                Text(
                    text = "Are you sure you want to delete \"$accountName\"? This action cannot be undone and all transactions associated with this account will also be permanently deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppPalette.textMuted,
                    lineHeight = 22.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onDismiss() },
                        shape = RoundedCornerShape(Dimens.ButtonRadiusSmall),
                        color = AppPalette.cardElevated,
                        border = BorderStroke(1.dp, AppPalette.cardBorder)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = AppPalette.textMuted
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onConfirm() },
                        shape = RoundedCornerShape(Dimens.ButtonRadiusSmall),
                        color = ExpenseRed
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Delete",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}