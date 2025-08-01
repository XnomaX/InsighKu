package com.example.insightku.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.insightku.ui.components.common.InfoBar
import com.example.insightku.ui.components.common.FloatingAIButton
import com.example.insightku.ui.components.dashboard.StreakCalendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onNavigateToAuth: () -> Unit = {},
    onScanReceipt: () -> Unit = {},
    onAddTransaction: () -> Unit = {},
    onViewAllTransactions: () -> Unit = {},
    onManageRecurring: () -> Unit = {},
    onViewStreakDetails: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    
    // Temporary state until ViewModel is created
    var isBalanceVisible by remember { mutableStateOf(true) }
    val balance = "Rp 1.500.000"
    val recentTransactions = emptyList<Any>()
    val currentStreak = 5

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Info Bar
            InfoBar(
                title = "InsightKu",
                subtitle = "Track your finances with AI",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Balance Card - Simple implementation
            BalanceCard(
                balance = balance,
                isVisible = isBalanceVisible,
                onToggleVisibility = { isBalanceVisible = !isBalanceVisible },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // AI Forecast Panel
            AIForecastPanel(
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Daily Finance Streak
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Finance Streak",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    TextButton(
                        onClick = onViewStreakDetails,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF5A2A82)
                        )
                    ) {
                        Text("View Details")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                StreakCalendar(
                    currentStreak = currentStreak,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Recent Transactions
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    TextButton(
                        onClick = onViewAllTransactions,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF5A2A82)
                        )
                    ) {
                        Text("View All")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Empty state for now
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔍",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No transactions yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add your first transaction to get started!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddTransaction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5A2A82),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Transaction",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Transaction")
                        }
                    }
                }
            }
            
            // Add bottom padding for floating button
            Spacer(modifier = Modifier.height(100.dp))
        }
        
        // Floating AI Scanner Button
        FloatingAIButton(
            onClick = onScanReceipt,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 90.dp)
        )
    }
}

@Composable
private fun BalanceCard(
    balance: String,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF5A2A82)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = if (isVisible) balance else "••••••••",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            TextButton(
                onClick = onToggleVisibility
            ) {
                Text(
                    text = if (isVisible) "Hide" else "Show",
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun AIForecastPanel(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "AI Forecast",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Smart insights coming soon...",
            )
        }
    }
}