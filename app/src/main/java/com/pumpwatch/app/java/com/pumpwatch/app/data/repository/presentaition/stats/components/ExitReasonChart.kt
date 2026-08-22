package com.pumpwatch.app.presentation.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import com.pumpwatch.app.domain.ExitReason

@Composable
fun ExitReasonPieChart(
    exitReasons: Map<ExitReason, Int>,
    modifier: Modifier = Modifier
) {
    val total = exitReasons.values.sum()
    if (total == 0) {
        Box(modifier.fillMaxSize()) {
            androidx.compose.material3.Text(
                "No trades yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val colors = listOf(
        Color(0xFF2196F3), // TRAILING_STOP
        Color(0xFF4CAF50), // TAKE_PROFIT
        Color(0xFFFF9800), // REVERSAL_DETECTED
        Color(0xFFF44336), // HARD_STOP_LOSS
        Color(0xFF9C27B0), // TIMEOUT
        Color(0xFF607D8B)  // MANUAL
    )

    val startAngles = mutableListOf<Float>()
    var currentAngle = 0f
    exitReasons.values.forEach { count ->
        startAngles.add(currentAngle)
        currentAngle += (count.toFloat() / total) * 360f
    }

    Canvas(modifier = modifier.size(200.dp)) {
        val diameter = minOf(size.width, size.height)
        val topLeft = Offset(
            (size.width - diameter) / 2,
            (size.height - diameter) / 2
        )

        exitReasons.entries.forEachIndexed { index, entry ->
            val sweepAngle = (entry.value.toFloat() / total) * 360f
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngles[index],
                sweepAngle = sweepAngle,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                useCenter = true,
                style = Fill
            )
        }
    }
}
