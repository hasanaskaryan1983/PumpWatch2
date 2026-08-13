package com.pumpwatch.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pumpwatch.app.domain.CoinTrack
import com.pumpwatch.app.domain.PumpSignal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onToggleMonitoring: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTrades: () -> Unit,
    onOpenBacktest: () -> Unit,
    onOpenCoin: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PumpWatch") },
                actions = {
                    TextButton(onClick = onOpenBacktest) { Text("بک‌تست") }
                    TextButton(onClick = onOpenTrades) { Text("معاملات") }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "تنظیمات")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            MonitoringBar(
                isMonitoring = state.isMonitoring,
                onToggle = onToggleMonitoring,
                onRefresh = onRefresh
            )

            state.error?.let {
                Text(
                    "خطا: $it",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (state.coins.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("برای شروع، پایش را روشن کنید یا دکمه بروزرسانی را بزنید")
                }
            } else {
                LazyColumn {
                    items(state.coins, key = { it.id }) { coin ->
                        CoinRow(coin, state.signals[coin.id], onClick = { onOpenCoin(coin.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun MonitoringBar(isMonitoring: Boolean, onToggle: (Boolean) -> Unit, onRefresh: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (isMonitoring) "پایش فعال است" else "پایش خاموش است", modifier = Modifier.weight(1f))
        Switch(checked = isMonitoring, onCheckedChange = onToggle)
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = onRefresh) { Text("بروزرسانی") }
    }
}

@Composable
private fun CoinRow(coin: CoinTrack, signal: PumpSignal?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("${coin.name} (${coin.symbol.uppercase()})", fontWeight = FontWeight.Bold)
            val price = coin.history.lastOrNull()?.price
            Text(if (price != null) "$${"%,.4f".format(price)}" else "—")
            coin.change24hPercent?.let {
                val color = if (it >= 0) Color(0xFF16C784) else Color(0xFFEA3943)
                Text("24h: %+.2f%%".format(it), color = color)
            }
        }

        if (signal != null && signal.isPump) {
            AssistChip(
                onClick = {},
                label = { Text("پامپ احتمالی (${signal.score})") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0xFFEA3943).copy(alpha = 0.15f)
                )
            )
        }
    }
}
