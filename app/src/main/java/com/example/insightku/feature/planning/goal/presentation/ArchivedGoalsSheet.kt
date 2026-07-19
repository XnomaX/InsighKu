package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.feature.planning.goal.domain.model.Goal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedGoalsSheet(
    archivedGoals: List<Goal>,
    onRestore: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppPalette.card,
        shape = RoundedCornerShape(Dimens.BottomSheetRadius),
        dragHandle = {
            Box(
                modifier = Modifier
                    .width(Dimens.BottomSheetHandleWidth)
                    .height(Dimens.BottomSheetHandleHeight)
                    .clip(RoundedCornerShape(Dimens.BottomSheetHandleRadius))
                    .background(AppPalette.cardBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.BottomSheetContentPadding)
                .padding(bottom = Dimens.BottomSheetFooterPadding + 16.dp)
        ) {
            // ── Header ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(AppPalette.cardElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Archive, null, tint = AppPalette.textMuted, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.goals_archived_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.textPrimary
                    )
                    Text(
                        stringResource(R.string.goals_archived_subtitle, archivedGoals.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Close, null, tint = AppPalette.textMuted, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Content ────────────────────────────────────────────────────
            if (archivedGoals.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(AppPalette.cardElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Inbox, null, tint = AppPalette.textMuted, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.goals_no_archived),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppPalette.textMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(archivedGoals, key = { it.id }) { goal ->
                        ArchivedGoalRow(
                            goal = goal,
                            onRestore = { onRestore(goal.id) },
                            onDelete = { onDelete(goal.id) },
                            onClick = { onClick(goal.id) }
                        )
                    }
                }
            }
        }
    }
}
