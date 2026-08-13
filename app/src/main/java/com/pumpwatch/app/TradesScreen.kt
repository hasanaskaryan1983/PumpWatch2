package com.pumpwatch.app.ui.trades

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pumpwatch.app.domain.ExitReason
import com.pumpwatch.app.domain.SimulatedTrade
import com.pumpwatch.app.domain.TradeSettings
import com.pumpwatch.app.domain.TradeStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradesScreen(
    trades: List<SimulatedTrade>,
    settings: TradeSettings,
    onBack: () -> Unit,
    onSaveSettings: (TradeSettings) -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("معاملات شبیه‌سازی‌شده") },
                navigationIcon = { TextButton(onClick = onBack) { Text("بازگشت") } },
                actions = {
                    TextButton(onClick = { showSettings = !showSettings }) {
                        Text(if (showSettings) "لیست معاملات" else "تنظیمات")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            if (!settings.enabled) {
                Text(
                    "معامله شبیه‌سازی‌شده خاموش است — فقط پایش/هشدار فعاله. " +
                        "از بخش تنظیمات روشنش کن.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (showSettings) {
                TradeSettingsForm(settings, onSaveSettings)
            } else {
                val open = trades.filter { it.status == TradeStatus.OPEN }
                val closed = trades.filter { it.status == TradeStatus.CLOSED }

                LazyColumn {
                    if (open.isNotEmpty()) {
                        item { SectionHeader("باز (${open.size})") }
                        items(open, key = { it.id }) { TradeRow(it) }
                    }
                    if (closed.isNotEmpty()) {
                        item { SectionHeader("تاریخچه (${closed.size})") }
                        items(closed, key = { it.id }) { TradeRow(it) }
                    }
                    if (trades.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("هنوز معامله‌ای ثبت نشده")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun TradeRow(trade: SimulatedTrade) {
    val pnl = trade.closedPnlPercent ?: trade.pnlPercentAt(trade.highestPriceSinceEntry)
    val color = if (pnl >= 0) Color(0xFF16C784) else Color(0xFFEA3943)

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${trade.name} (${trade.symbol.uppercase()})", fontWeight = FontWeight.Bold)
            Text("%+.2f%%".format(pnl), color = color, fontWeight = FontWeight.Bold)
        }
        Text("ورود: $${"%,.4f".format(trade.entryPrice)}")
        if (trade.status == TradeStatus.OPEN) {
            Text("سقف تا الان: $${"%,.4f".format(trade.highestPriceSinceEntry)} | استاپ شناور: $${"%,.4f".format(trade.trailingStopPrice)}")
        } else {
            val reasonText = when (trade.exitReason) {
                ExitReason.TRAILING_STOP -> "برخورد با استاپ شناور"
                ExitReason.REVERSAL_DETECTED -> "تشخیص برگشت روند"
                ExitReason.HARD_STOP_LOSS -> "حد ضرر اولیه"
                else -> "دستی"
            }
            Text("خروج: $${"%,.4f".format(trade.exitPrice ?: 0.0)} | علت: $reasonText")
        }
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun TradeSettingsForm(current: TradeSettings, onSave: (TradeSettings) -> Unit) {
    var enabled by remember(current) { mutableStateOf(current.enabled) }
    var maxConcurrent by remember(current) { mutableStateOf(current.maxConcurrentTrades.toFloat()) }
    var hardStop by remember(current) { mutableStateOf(current.hardStopLossPercent.toFloat()) }
    var trailingStop by remember(current) { mutableStateOf(current.trailingStopPercent.toFloat()) }
    var reversalDrop by remember(current) { mutableStateOf(current.reversalDropPercent.toFloat()) }
    var reversalTicks by remember(current) { mutableStateOf(current.reversalDownTicks.toFloat()) }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("فعال‌سازی معامله شبیه‌سازی‌شده", modifier = Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = { enabled = it })
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "توجه: این فقط شبیه‌سازی داخل اپه، هیچ سفارش واقعی به هیچ صرافی ارسال نمی‌شود.",
            style = MaterialTheme.typography.bodySmall
        )

        SettingSlider("حداکثر معاملات همزمان", maxConcurrent, 1f, 10f) { maxConcurrent = it }
        SettingSlider("حد ضرر اولیه (٪)", hardStop, 2f, 20f) { hardStop = it }
        SettingSlider("فاصله استاپ شناور از سقف (٪)", trailingStop, 2f, 20f) { trailingStop = it }
        SettingSlider("افت ناگهانی از سقف برای تشخیص برگشت (٪)", reversalDrop, 1f, 15f) { reversalDrop = it }
        SettingSlider("تیک‌های نزولی پیاپی برای تشخیص برگشت", reversalTicks, 1f, 6f) { reversalTicks = it }

        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            onSave(
                TradeSettings(
                    enabled = enabled,
                    maxConcurrentTrades = maxConcurrent.toInt(),
                    hardStopLossPercent = hardStop.toDouble(),
                    trailingStopPercent = trailingStop.toDouble(),
                    reversalDropPercent = reversalDrop.toDouble(),
                    reversalDownTicks = reversalTicks.toInt()
                )
            )
        }) { Text("ذخیره") }
    }
}

@Composable
private fun SettingSlider(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text("$label: ${value.toInt()}")
        Slider(value = value, onValueChange = onChange, valueRange = min..max)
    }
}
