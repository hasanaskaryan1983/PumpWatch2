package com.pumpwatch.app.presentation.backtest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pumpwatch.app.domain.*
import com.pumpwatch.app.presentation.stats.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BacktestScreen(
    viewModel: BacktestViewModel,
    onBack: () -> Unit
) {
    val inputs by viewModel.inputs.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val coins by viewModel.availableCoins.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("🧪 Backtest Lab") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState is BacktestUiState.Result) {
                        IconButton(onClick = {
                            val result = uiState as BacktestUiState.Result
                            val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, result.shareText)
                            }
                            context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Report"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Coin Selection ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Coin", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = inputs.coinSymbol.ifBlank { "Select a coin" },
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            coins.forEach { coin ->
                                DropdownMenuItem(
                                    text = { Text("${coin.symbol.uppercase()} - ${coin.name}") },
                                    onClick = {
                                        viewModel.updateInputs {
                                            copy(coinId = coin.id, coinSymbol = coin.symbol)
                                        }
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- Mode & Days ---
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Mode", style = MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = inputs.mode == MarketMode.SPOT,
                                onClick = { viewModel.updateInputs { copy(mode = MarketMode.SPOT) } },
                                label = { Text("Spot") }
                            )
                            Spacer(Modifier.width(8.dp))
                            FilterChip(
                                selected = inputs.mode == MarketMode.FUTURES,
                                onClick = { viewModel.updateInputs { copy(mode = MarketMode.FUTURES) } },
                                label = { Text("Futures") }
                            )
                        }
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Days", style = MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            listOf(7, 30, 90, 180).forEach { d ->
                                FilterChip(
                                    selected = inputs.days == d,
                                    onClick = { viewModel.updateInputs { copy(days = d) } },
                                    label = { Text("$d") },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- Run Button ---
            Button(
                onClick = { viewModel.runBacktest() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is BacktestUiState.Running && inputs.coinId.isNotBlank()
            ) {
                if (uiState is BacktestUiState.Running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Run Backtest")
            }

            // --- Results ---
            when (val state = uiState) {
                is BacktestUiState.Idle -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "یه ارز انتخاب کن و دکمه Run رو بزن.",
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is BacktestUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            "❌ ${state.message}",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                is BacktestUiState.Result -> {
                    val r = state.result

                    // Stats cards
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OverviewCard(
                            title = "Win Rate",
                            value = "%.1f%%".format(r.winRatePercent),
                            valueColor = if (r.winRatePercent > 50) Color(0xFF4CAF50) else Color(0xFFF44336),
                            modifier = Modifier.weight(1f)
                        )
                        OverviewCard(
                            title = "Profit Factor",
                            value = if (r.profitFactor.isInfinite()) "∞" else "%.2f".format(r.profitFactor),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OverviewCard(
                            title = "Net PnL",
                            value = "${if (r.totalPnlAmount >= 0) "+" else ""}$${"%.2f".format(r.totalPnlAmount)}",
                            valueColor = if (r.totalPnlAmount >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                            modifier = Modifier.weight(1f)
                        )
                        OverviewCard(
                            title = "Max DD",
                            value = "%.1f%%".format(r.maxDrawdownPercent),
                            valueColor = Color(0xFFFF9800),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Exit reasons
                    if (r.exitReasonBreakdown.isNotEmpty()) {
                        Text("Exit Reasons", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                r.exitReasonBreakdown.entries.sortedByDescending { it.value }.forEach { (reason, count) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(reason.name.replace("_", " "), style = MaterialTheme.typography.bodySmall)
                                        Text("$count", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    // Advisor insights
                    if (state.insights.isNotEmpty()) {
                        Text("🧠 Advisor", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        state.insights.forEach { insight ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = when (insight.type) {
                                        InsightType.PROBLEM -> MaterialTheme.colorScheme.errorContainer
                                        InsightType.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                                        InsightType.INFO -> MaterialTheme.colorScheme.primaryContainer
                                    }
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        insight.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(insight.detail, style = MaterialTheme.typography.bodySmall)
                                    insight.suggestion?.let {
                                        Text("💡 $it", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }

                    // Reset button
                    OutlinedButton(
                        onClick = { viewModel.reset() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Run Another Backtest")
                    }
                }

                is BacktestUiState.Running -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.width(16.dp))
                            Text("در حال اجرای بک‌تست...")
                        }
                    }
                }
            }
        }
    }
}
