package com.pumpwatch.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pumpwatch.app.data.local.LanguageStore
import com.pumpwatch.app.data.local.ModeStore
import com.pumpwatch.app.domain.MarketMode
import com.pumpwatch.app.presentation.ModeSelectScreen

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Handle permission result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply saved language
        val languageStore = LanguageStore(applicationContext)
        LanguageStore.applyLanguage(languageStore.getSavedLanguage())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val modeStore = remember { ModeStore(applicationContext) }
            var modeChosen by remember { mutableStateOf(modeStore.hasChosen()) }

            if (!modeChosen) {
                ModeSelectScreen { chosenMode ->
                    modeStore.save(chosenMode)
                    modeChosen = true
                }
            } else {
                // Simple main screen
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "🚀 PumpWatch",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Mode: ${modeStore.current().emoji} ${modeStore.current().label}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.height(32.dp))
                            Button(
                                onClick = { /* TODO: Navigate to dashboard */ },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Dashboard (Coming Soon)")
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    // Switch mode
                                    val newMode = if (modeStore.current() == MarketMode.SPOT) 
                                        MarketMode.FUTURES else MarketMode.SPOT
                                    modeStore.save(newMode)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Switch Mode")
                            }
                        }
                    }
                }
            }
        }
    }
}
