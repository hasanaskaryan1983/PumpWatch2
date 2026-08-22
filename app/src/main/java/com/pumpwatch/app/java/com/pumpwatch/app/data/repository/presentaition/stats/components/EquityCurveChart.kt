package com.pumpwatch.app.presentation.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.pumpwatch.app.presentation.stats.EquityPoint
import kotlin.math.min

@Composable
fun EquityCurveChart(
    equityPoints: List<EquityPoint>,
    initialCapital: Double,
    modifier: Modifier = Modifier
) {
    if (equityPoints.isEmpty()) {
        Box(modifier.fillMaxSize()) {
            androidx.compose.material3.Text(
                "No data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val lineColor = if (equityPoints.last().equity >= initialCapital)
        Color(0xFF4CAF50)
    else
        Color(0xFFF44336)

    val maxEquity = equityPoints.maxOf { it.equity }
    val minEquity = equityPoints.minOf { it.equity }
    val range = maxEquity - minEquity
    val padding = if (range > 0) range * 0.1 else 100.0

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val verticalPadding = height * 0.1f
        val chartHeight = height - 2 * verticalPadding

        val points = equityPoints.mapIndexed { index, point ->
            val x = if (equityPoints.size == 1) width / 2 else
                (index.toFloat() / (equityPoints.size - 1)) * width
            val normalizedY = (point.equity - (minEquity - padding)) / 
                ((maxEquity + padding) - (minEquity - padding))
            val y = verticalPadding + chartHeight * (1 - normalizedY)
            Offset(x, y)
        }

        // Draw line
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Draw points
        points.forEach { point ->
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = point
            )
        }
    }
}
