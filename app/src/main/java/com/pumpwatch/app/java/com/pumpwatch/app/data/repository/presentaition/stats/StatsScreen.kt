package com.pumpwatch.app.presentation.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pumpwatch.app.presentation.stats.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 Performance Dashboard") }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is StatsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is StatsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = "❌ ${state.message}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is StatsUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --- Overview Cards ---
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OverviewCard(
                                title = "Win Rate",
                                value = "%.1f%%".format(state.stats.winRatePercent),
                                valueColor = if (state.stats.winRatePercent > 50) Color(0xFF4CAF50) else Color(0xFFF44336),
                                modifier = Modifier.weight(1f)
                            )
                            OverviewCard(
                                title = "Profit Factor",
                                value = if (state.stats.profitFactor.isInfinite()) "" else "%.2f".format(state.stats.profitFactor),
                                valueColor = if (state.stats.profitFactor >= 1.5) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OverviewCard(
                                title = "Total PnL",
                                value = "${if (state.stats.totalProfit >= 0) "+" else ""}$${"%.2f".format(state.stats.totalProfit)}",
                                valueColor = if (state.stats.totalProfit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                                modifier = Modifier.weight(1f)
                            )
                            OverviewCard(
                                title = "Trades",
                                value = "${state.stats.totalTrades}",
                                subtitle = "${state.stats.winningTrades}W / ${state.stats.losingTrades}L",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // --- Equity Curve ---
                    item {
                        Text(
                            "Equity Curve",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            EquityCurveChart(
                                equityPoints = state.equityCurve,
                                initialCapital = 1000.0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(16.dp)
                            )
                        }
                    }

                    // --- Exit Reason Distribution ---
                    if (state.stats.totalTrades > 0) {
                        item {
                            Text(
                                "Exit Reason Distribution",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                ) {
                                    ExitReasonPieChart(
                                        exitReasons = state.trades
                                            .filter { it.exitReason != null }
                                            .groupBy { it.exitReason!! }
                                            .mapValues { it.value.size },
                                        modifier = Modifier.size(200.dp)
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    // Legend
                                    state.trades
                                        .filter { it.exitReason != null }
                                        .groupBy { it.exitReason!! }
                                        .mapValues { it.value.size }
                                        .entries
                                        .sortedByDescending { it.value }
                                        .forEach { (reason, count) ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    reason.name.replace("_", " "),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    "$count (${count * 100 / state.stats.totalTrades}%)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                }
                            }
                        }
                    }

                    // --- Recent Trades ---
                    if (state.trades.isNotEmpty()) {
                        item {
                            Text(
                                "Recent Trades (${state.trades.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(state.trades.takeLast(20).reversed()) { trade ->
                            TradeRow(trade = trade)
                        }
                    }

                    // --- Empty State ---
                    if (state.trades.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    "No trades yet. Start monitoring to see your performance.",
                                    modifier = Modifier.padding(24.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
