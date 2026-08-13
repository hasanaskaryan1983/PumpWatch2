package com.pumpwatch.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PumpGreen = Color(0xFF16C784)
private val PumpRed = Color(0xFFEA3943)
private val DarkBg = Color(0xFF0E1116)

private val DarkColors = darkColorScheme(
    primary = PumpGreen,
    error = PumpRed,
    background = DarkBg,
    surface = Color(0xFF171B22)
)

private val LightColors = lightColorScheme(
    primary = PumpGreen,
    error = PumpRed
)

@Composable
fun PumpWatchTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
