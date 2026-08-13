package com.pumpwatch.app.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pumpwatch.app.domain.CoinTrack
import com.pumpwatch.app.domain.PumpSignal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinDetailScreen(coin: CoinTrack?, signal: PumpSignal?, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(coin?.name ?: "جزئیات") },
                navigationIcon = { TextButton(onClick = onBack) { Text("بازگشت") } }
            )
        }
    ) { padding ->
        if (coin == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("داده‌ای موجود نیست")
            }
            return@Scaffold
        }

        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("قیمت فعلی: $${"%,.4f".format(coin.history.lastOrNull()?.price ?: 0.0)}")
            Spacer(Modifier.height(16.dp))

            Sparkline(prices = coin.history.map { it.price }, modifier = Modifier.fillMaxWidth().height(120.dp))

            Spacer(Modifier.height(24.dp))
            if (signal != null) {
                Text("ریز سیگنال‌های فعلی:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("تغییر قیمت در بازه: %+.2f%%".format(signal.priceChangePercent))
                Text("رشد حجم نسبت به میانگین: %+.1f%%".format(signal.volumeGrowthPercent))
                Text("تیک‌های صعودی پیاپی: ${signal.consecutiveUpTicks}")
                Text("تعداد سیگنال‌های فعال: ${signal.firedSignalCount} از ۳")
                Text("امتیاز ترکیبی: ${signal.score} / 100")
                if (signal.isPump) {
                    Spacer(Modifier.height(8.dp))
                    Text("⚠️ این کوین در حال حاضر شرایط پامپ ترکیبی را دارد", color = Color(0xFFEA3943))
                }
            } else {
                Text("در حال حاضر سیگنال فعالی برای این کوین ثبت نشده است.")
            }
        }
    }
}

@Composable
private fun Sparkline(prices: List<Double>, modifier: Modifier = Modifier) {
    if (prices.size < 2) {
        Box(modifier) { Text("داده کافی برای رسم نمودار نیست") }
        return
    }
    val min = prices.min()
    val max = prices.max()
    val range = (max - min).takeIf { it > 0 } ?: 1.0

    Canvas(modifier = modifier) {
        val stepX = size.width / (prices.size - 1)
        val points = prices.mapIndexed { index, price ->
            val x = index * stepX
            val y = size.height - ((price - min) / range * size.height).toFloat()
            Offset(x, y)
        }
        for (i in 0 until points.size - 1) {
            drawLine(
                color = Color(0xFF16C784),
                start = points[i],
                end = points[i + 1],
                strokeWidth = 4f
            )
        }
    }
}
