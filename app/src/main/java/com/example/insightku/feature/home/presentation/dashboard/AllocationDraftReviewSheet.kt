package com.example.insightku.feature.home.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.data.model.DraftTransaction
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.SuccessColor

/**
 * AllocationDraftReviewSheet — Bottom sheet for reviewing auto-allocation drafts.
 *
 * Shows allocation details and allows user to approve or reject.
 * Reuses the Draft Transaction infrastructure as required by P0.2.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllocationDraftReviewSheet(
    draft: DraftTransaction,
    onApprove: (DraftTransaction) -> Unit,
    onReject: (DraftTransaction) -> Unit,
    onDismiss: () -> Unit
) {
    com.example.insightku.core.ui.components.bottomsheet.SafeBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = AppPalette.card,
        contentWindowInsets = WindowInsets(0, 8, 0, 8)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.allocation_review_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.allocation_review_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppPalette.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Allocation Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppPalette.cardElevated),
                border = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.allocation_review_amount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppPalette.textMuted
                        )
                        Text(
                            text = NumberFormatter.formatCurrency(draft.allocationAmount ?: 0.0),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SuccessColor
                        )
                    }

                    HorizontalDivider(color = AppPalette.cardBorder)

                    // Source Account
                    DetailRow(
                        icon = Icons.Outlined.AccountBalance,
                        label = stringResource(R.string.allocation_review_source),
                        value = draft.sourceAccountName ?: stringResource(R.string.allocation_review_unknown)
                    )

                    // Destination Goal
                    DetailRow(
                        icon = Icons.Outlined.Savings,
                        label = stringResource(R.string.allocation_review_destination),
                        value = draft.goalName ?: stringResource(R.string.allocation_review_unknown_goal)
                    )

                    // Trigger
                    DetailRow(
                        icon = Icons.Outlined.AutoAwesome,
                        label = stringResource(R.string.allocation_review_trigger),
                        value = draft.triggerDescription ?: draft.triggerType ?: stringResource(R.string.allocation_review_unknown)
                    )

                    // Execution Time
                    draft.triggerTimestamp?.let { ts ->
                        DetailRow(
                            icon = Icons.Outlined.Schedule,
                            label = "Execution Time",
                            value = DateFormatter.formatShortDateTime(ts)
                        )
                    }

                    // Remaining Balance
                    if (draft.allocationAmount != null) {
                        HorizontalDivider(color = AppPalette.cardBorder)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.allocation_review_after),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppPalette.textMuted
                            )
                            Text(
                                text = stringResource(R.string.allocation_review_balance_decrease, NumberFormatter.formatCurrency(draft.allocationAmount)),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppPalette.textMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Execution Preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SuccessColor.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, SuccessColor.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = SuccessColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.allocation_review_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Reject Button
                OutlinedButton(
                    onClick = { onReject(draft) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppPalette.cardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.allocation_review_reject))
                }

                // Approve Button
                Button(
                    onClick = { onApprove(draft) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.allocation_review_approve))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SuccessColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SuccessColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = AppPalette.textMuted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppPalette.textPrimary
            )
        }
    }
}
