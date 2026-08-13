package com.pumpwatch.app.ui.crash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun CrashScreen(crashText: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current

    Scaffold { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("اپ دفعه قبل کرش کرد", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "این متن رو کپی کن و برای دیباگ بفرست — دقیقاً همون خطاییه که باعث بسته " +
                    "شدن اپ شد.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))

            Card(Modifier.weight(1f)) {
                Text(
                    crashText,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(16.dp))
            Row {
                Button(
                    onClick = { clipboard.setText(AnnotatedString(crashText)) },
                    modifier = Modifier.weight(1f)
                ) { Text("کپی متن خطا") }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text("متوجه شدم، ادامه بده") }
            }
        }
    }
}
