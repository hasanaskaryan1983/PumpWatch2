package com.pumpwatch.app.presentation.stats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pumpwatch.app.domain.SimulatedTrade
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TradeRow(trade: SimulatedTrade, modifier: Modifier = Modifier) {
    val pnlPercent = trade.closedPnlPercent ?: 0.0
    val pnlColor = if (pnlPercent >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
    val dateFormat = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(2f)) {
            Text(
                text = trade.symbol.uppercase(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = trade.direction.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier.weight(1.5f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = dateFormat.format(Date(trade.entryTimeMillis)),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = trade.exitReason?.name?.replace("_", " ") ?: "Open",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = if (pnlPercent >= 0) "+%.2f%%".format(pnlPercent) else "%.2f%%".format(pnlPercent),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = pnlColor
            )
            Text(
                text = "${(trade.closedProfitAmount ?: 0.0).toInt()} USDT",
                style = MaterialTheme.typography.bodySmall,
                color = pnlColor
            )
        }
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}
