package com.example.insightku.feature.home.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.core.data.model.DraftConfidence
import com.example.insightku.core.data.model.DraftTransaction
import com.example.insightku.core.data.model.DraftType
import com.example.insightku.core.data.model.TransactionType
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.*

@Composable
fun DraftInboxSection(
    drafts: List<DraftTransaction>,
    onOpenDraft: (DraftTransaction) -> Unit,
    onDismissDraft: (DraftTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (drafts.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val visible = if (expanded) drafts else drafts.take(2)
    val hiddenCount = (drafts.size - visible.size).coerceAtLeast(0)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.draft_inbox_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textPrimary
            )
            Text(
                stringResource(R.string.draft_inbox_subtitle, drafts.size),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(Dimens.CardRadius),
            color    = AppPalette.card,
            border   = BorderStroke(1.dp, AppPalette.cardBorder)
        ) {
            Column {
                visible.forEachIndexed { index, draft ->
                    if (draft.draftType == DraftType.AUTO_ALLOCATION) {
                        AllocationDraftInboxRow(
                            draft    = draft,
                            onOpen   = { onOpenDraft(draft) },
                            onDismiss = { onDismissDraft(draft) }
                        )
                    } else {
                        DraftInboxRow(
                            draft    = draft,
                            onOpen   = { onOpenDraft(draft) },
                            onDismiss = { onDismissDraft(draft) }
                        )
                    }
                    if (index < visible.lastIndex || hiddenCount > 0) {
                        HorizontalDivider(color = AppPalette.cardBorder, thickness = 1.dp)
                    }
                }
                if (hiddenCount > 0 && !expanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.draft_inbox_more, hiddenCount),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = NavPurple
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllocationDraftInboxRow(
    draft: DraftTransaction,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDismiss()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ExpenseRed.copy(alpha = 0.10f))
                    .padding(horizontal = Dimens.CardInnerPadding),
                contentAlignment = Alignment.CenterEnd
            ) {                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.draft_delete_description), tint = ExpenseRed, modifier = Modifier.size(22.dp))
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppPalette.card)
                .clickable(onClick = onOpen)
                .padding(Dimens.CardInnerPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Allocation icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SuccessColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Savings,
                    contentDescription = null,
                    tint = SuccessColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${NumberFormatter.formatCurrency(draft.allocationAmount ?: draft.amountGuess)} → ${draft.goalName ?: draft.merchantGuess}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textPrimary,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = buildString {
                            append(draft.sourceAccountName ?: stringResource(R.string.draft_account_label))
                            append(" · ")
                            append(draft.triggerDescription ?: stringResource(R.string.draft_auto_allocation))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted,
                        maxLines = 1
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AppPalette.textMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftInboxRow(
    draft: DraftTransaction,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDismiss()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ExpenseRed.copy(alpha = 0.10f))
                    .padding(horizontal = Dimens.CardInnerPadding),
                contentAlignment = Alignment.CenterEnd
            ) {                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.draft_delete_description), tint = ExpenseRed, modifier = Modifier.size(22.dp))
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppPalette.card)
                .clickable(onClick = onOpen)
                .padding(Dimens.CardInnerPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ConfidenceRing(confidence = draft.confidence, isIncome = draft.typeGuess == TransactionType.INCOME)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${NumberFormatter.formatCurrency(draft.amountGuess)} · ${draft.merchantGuess.ifBlank { draft.bankName }}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textPrimary,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = buildString {
                            append(draft.bankName)
                            append(" · ")
                            append(if (draft.typeGuess == TransactionType.INCOME) stringResource(R.string.draft_possible_income) else stringResource(R.string.draft_possible_expense))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted,
                        maxLines = 1
                    )
                    if (draft.confidence == DraftConfidence.LOW) {
                        Text("· ${stringResource(R.string.draft_needs_check)}", style = MaterialTheme.typography.bodySmall, color = WarningYellow, maxLines = 1)
                    }
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AppPalette.textMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ConfidenceRing(
    confidence: DraftConfidence,
    isIncome: Boolean
) {
    val base = if (isIncome) IncomeGreen else NavPurple
    val sweep = when (confidence) {
        DraftConfidence.HIGH   -> 360f
        DraftConfidence.MEDIUM -> 200f
        DraftConfidence.LOW    -> 90f
    }
    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(40.dp)) {
            drawArc(
                color = base.copy(alpha = 0.18f),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )
            drawArc(
                color = base,
                startAngle = -90f, sweepAngle = sweep, useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        Icon(
            imageVector = if (isIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
            contentDescription = null,
            tint = base,
            modifier = Modifier.size(16.dp)
        )
    }
}
