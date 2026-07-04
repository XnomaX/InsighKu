package com.example.insightku.feature.budgeting.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.LocalAccent

/**
 * Placeholder for GoalDetailScreen navigation.
 * TODO: Implement proper goal detail with ViewModel to fetch goal by ID
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreenPlaceholder(
    goalId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppPalette.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = AppPalette.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Goal Detail",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Placeholder content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Goal Detail",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Goal ID: $goalId",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppPalette.textMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Full goal detail screen with contributions, progress, and actions will be shown here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                }
            }
        }
    }
}

/**
 * Placeholder for BudgetDetailScreen navigation.
 * TODO: Implement proper budget detail with ViewModel to fetch budget by ID
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailScreenPlaceholder(
    budgetId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppPalette.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = AppPalette.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Budget Detail",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Placeholder content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Budget Detail",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Budget ID: $budgetId",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppPalette.textMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Full budget detail screen with spending breakdown and transactions will be shown here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.textMuted
                    )
                }
            }
        }
    }
}
