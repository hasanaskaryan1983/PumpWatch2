package com.pumpwatch.app.ui.backtest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.pumpwatch.app.domain.BacktestResult
import com.pumpwatch.app.domain.CoinTrack

private val dayOptions = listOf(1, 7, 14, 30, 90)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BacktestScreen(
    candidateCoins: List<CoinTrack>,
    uiState: BacktestUiState,
    onBack: () -> Unit,
    onRun: (List<String>, Int) -> Unit
) {
    val selected = remember { mutableStateListOf<String>() }
    var days by remember { mutableStateOf(7) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بک‌تست استراتژی") },
                navigationIcon = { TextButton(onClick = onBack) { Text("بازگشت") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            Text(
                "روی داده‌های گذشته همون منطق تشخیص پامپ و استاپ شناور رو اجرا می‌کنه تا ببینی " +
                    "قبلاً چند درصد موفق بوده. برای بازه بیشتر از ۱ روز، داده‌ی CoinGecko ساعتی می‌شه، " +
                    "پس نتیجه یه تقریبه نه دقیق ۱۰۰٪.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall
            )

            if (candidateCoins.isEmpty()) {
                Text(
                    "اول از داشبورد یک‌بار «بروزرسانی» بزن تا لیست کوین‌های رتبه انتخابی بارگذاری بشه.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text("بازه زمانی:", modifier = Modifier.padding(horizontal = 16.dp))
            Row(Modifier.padding(horizontal = 16.dp)) {
                dayOptions.forEach { d ->
                    FilterChip(
                        selected = days == d,
                        onClick = { days = d },
                        label = { Text("${d}روز") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Text(
                "کوین‌ها (${selected.size} انتخاب‌شده):",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(Modifier.weight(1f)) {
                items(candidateCoins, key = { it.id }) { coin ->
                    val checked = coin.id in selected
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = {
                            if (it) selected.add(coin.id) else selected.remove(coin.id)
                        })
                        Text("${coin.name} (${coin.symbol.uppercase()})")
                    }
                }

                if (uiState.combined != null) {
                    item { ResultSummary(uiState.combined) }
                }
            }

            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }

            if (uiState.isRunning) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                Text(uiState.progressText, modifier = Modifier.padding(16.dp))
            }

            Button(
                onClick = { onRun(selected.toList(), days) },
                enabled = !uiState.isRunning && selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) { Text("اجرای بک‌تست") }
        }
    }
}

@Composable
private fun ResultSummary(result: BacktestResult) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("نتیجه ترکیبی همه کوین‌های انتخاب‌شده", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("تعداد کل معاملات: ${result.totalTrades}")
        Text("موفق: ${result.winCount} | ناموفق: ${result.lossCount}")
        Text("نرخ موفقیت: %.1f%%".format(result.winRatePercent))
        val avgColor = if (result.avgPnlPercent >= 0) Color(0xFF16C784) else Color(0xFFEA3943)
        Text("میانگین سود/زیان هر معامله: %+.2f%%".format(result.avgPnlPercent), color = avgColor)
        Text("بهترین معامله: %+.2f%%".format(result.bestTradePercent))
        Text("بدترین معامله: %+.2f%%".format(result.worstTradePercent))
        Spacer(Modifier.height(8.dp))
        Text(
            "توجه: این محاسبه ساده جمع درصدهاست، بدون احتساب کارمزد، اسلیپیج یا " +
                "نقدشوندگی واقعی بازار — فقط راهنمای اولیه برای سنجش منطق استراتژیه.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
