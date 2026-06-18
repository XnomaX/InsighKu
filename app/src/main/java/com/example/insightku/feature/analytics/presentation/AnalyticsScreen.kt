package com.example.insightku.feature.analytics.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.core.ui.theme.Dimens

/**
 * Analytics Screen - Period Navigation Foundation
 *
 * This screen provides the structural foundation for analytics with period-based navigation.
 * Currently shows only the period selector with empty content containers.
 *
 * Future implementations will add:
 * - Weekly analytics content
 * - Monthly analytics content
 * - Annual analytics content
 */
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF9FE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = Dimens.ScreenHorizontalPadding)
        ) {
            // Subtle spacing below status bar
            Spacer(modifier = Modifier.height(16.dp))

            // Period Selector
            PeriodSelector(
                selectedPeriod = uiState.selectedPeriod,
                onPeriodSelected = { period ->
                    viewModel.onEvent(AnalyticsEvent.SelectPeriod(period))
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.SectionSpacing))

            // Content Container - responds to selected period
            AnalyticsContentContainer(
                selectedPeriod = uiState.selectedPeriod
            )
        }
    }
}

@Composable
private fun AnalyticsContentContainer(
    selectedPeriod: com.example.insightku.feature.analytics.domain.AnalyticsPeriodType
) {
    // Empty container that reserves layout space for future content
    // Content will be added based on selected period in future implementations
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(Color.Transparent)
    ) {
        // Future: Weekly, Monthly, Annual analytics content will be placed here
        // Each period can have its own independent analytics module
    }
}
