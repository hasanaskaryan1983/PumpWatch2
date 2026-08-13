package com.pumpwatch.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pumpwatch.app.domain.PumpSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(current: PumpSettings, onBack: () -> Unit, onSave: (PumpSettings) -> Unit) {
    var pollInterval by remember(current) { mutableStateOf(current.pollIntervalMinutes.toFloat()) }
    var priceThreshold by remember(current) { mutableStateOf(current.priceChangeThresholdPercent.toFloat()) }
    var volumeThreshold by remember(current) { mutableStateOf(current.volumeGrowthThresholdPercent.toFloat()) }
    var upTicks by remember(current) { mutableStateOf(current.minConsecutiveUpTicks.toFloat()) }
    var windowMinutes by remember(current) { mutableStateOf(current.windowMinutes.toFloat()) }
    var minSignals by remember(current) { mutableStateOf(current.minSignalsRequired.toFloat()) }
    var minRank by remember(current) { mutableStateOf(current.minMarketCapRank.toFloat()) }
    var maxRank by remember(current) { mutableStateOf(current.maxMarketCapRank.toFloat()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات تشخیص") },
                navigationIcon = { TextButton(onClick = onBack) { Text("بازگشت") } }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingSlider("فاصله بررسی (دقیقه)", pollInterval, 2f, 30f, { pollInterval = it })
            SettingSlider("رتبه حداقل مارکت‌کپ (پایین‌تر = بزرگ‌تر)", minRank, 1f, 900f, { v ->
                minRank = v
                if (maxRank < minRank) maxRank = minRank
            })
            SettingSlider("رتبه حداکثر مارکت‌کپ", maxRank, 1f, 1000f, { v ->
                maxRank = v
                if (minRank > maxRank) minRank = maxRank
            })
            SettingSlider("آستانه تغییر قیمت (٪)", priceThreshold, 1f, 30f, { priceThreshold = it })
            SettingSlider("آستانه رشد حجم (٪)", volumeThreshold, 10f, 200f, { volumeThreshold = it })
            SettingSlider("حداقل تیک‌های صعودی پیاپی", upTicks, 1f, 8f, { upTicks = it })
            SettingSlider("بازه زمانی بررسی (دقیقه)", windowMinutes, 5f, 60f, { windowMinutes = it })
            SettingSlider("حداقل تعداد سیگنال‌های همزمان لازم", minSignals, 1f, 3f, { minSignals = it })

            Spacer(Modifier.height(16.dp))
            Text(
                "توضیح: پامپ فقط زمانی هشدار داده می‌شود که حداقل تعداد سیگنال‌های " +
                    "انتخاب‌شده (قیمت، حجم، مومنتوم) همزمان فعال شوند تا هشدارهای اشتباه کم شود.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                onSave(
                    PumpSettings(
                        pollIntervalMinutes = pollInterval.toInt(),
                        priceChangeThresholdPercent = priceThreshold.toDouble(),
                        volumeGrowthThresholdPercent = volumeThreshold.toDouble(),
                        minConsecutiveUpTicks = upTicks.toInt(),
                        windowMinutes = windowMinutes.toInt(),
                        minSignalsRequired = minSignals.toInt(),
                        minMarketCapRank = minRank.toInt(),
                        maxMarketCapRank = maxRank.toInt()
                    )
                )
                onBack()
            }) { Text("ذخیره") }
        }
    }
}

@Composable
private fun SettingSlider(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text("$label: ${value.toInt()}")
        Slider(value = value, onValueChange = onChange, valueRange = min..max)
    }
}
