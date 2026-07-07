package com.example.insightku.feature.home.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.core.data.model.TransactionType
import com.example.insightku.core.ui.components.TransactionCategoryIcon
import com.example.insightku.core.ui.components.TransactionTypePresentation
import com.example.insightku.core.ui.theme.*
import com.example.insightku.feature.home.presentation.TransactionItem
import com.example.insightku.feature.home.presentation.formatCurrencyShort
import kotlin.math.abs

@Composable
fun RecentTransactionsPreview(
    transactions: List<TransactionItem>,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Recent",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary
                )
                Text(
                    "Latest financial activity",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }
            Surface(
                modifier = Modifier.clickable(onClick = onViewAllClick),
                shape    = RoundedCornerShape(50.dp),
                color    = AppPalette.card,
                border   = BorderStroke(1.dp, NavPurple)
            ) {
                Text(
                    "View All",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NavPurple
                )
            }
        }
        if (transactions.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(Dimens.CardRadius),
                color    = AppPalette.card,
                border   = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No recent transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                }
            }
        } else {
            Surface(
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(Dimens.CardRadiusLarge),
                color           = AppPalette.card,
                tonalElevation  = 0.dp,
                shadowElevation = Dimens.ElevationSmall,
                border          = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    transactions.take(5).forEachIndexed { index, tx ->
                        TransactionItemRow(tx, onViewAllClick)
                        if (index < transactions.take(5).lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Dimens.CardInnerPadding)
                                    .height(1.dp)
                                    .background(AppPalette.cardBorder)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionItemRow(transaction: TransactionItem, onClick: () -> Unit) {
    val isSystemType = transaction.transactionType !in setOf(TransactionType.INCOME, TransactionType.EXPENSE)
    val presentation = TransactionTypePresentation.forType(transaction.transactionType)
    
    // Determine amount color and prefix
    val amountColor = if (isSystemType) presentation.color else if (transaction.isIncome) IncomeGreen else AppPalette.textPrimary
    val amountPrefix = if (transaction.isIncome) "+" else if (isSystemType && presentation.color == IncomeGreen) "+" else ""
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Dimens.CardInnerPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TransactionCategoryIcon(
            categoryName = transaction.iconName.ifBlank { transaction.category },
            transactionType = transaction.transactionType
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textPrimary,
                maxLines = 1
            )
            Text(
                text = if (isSystemType) presentation.label else transaction.category,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.textMuted
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$amountPrefix ${formatCurrencyShort(abs(transaction.amount))}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
            Text(
                text = transaction.time,
                style = MaterialTheme.typography.labelSmall,
                color = AppPalette.textMuted
            )
        }
    }
}
