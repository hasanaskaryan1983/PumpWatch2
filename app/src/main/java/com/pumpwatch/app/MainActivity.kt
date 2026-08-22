package com.pumpwatch.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pumpwatch.app.data.local.LanguageStore
import com.pumpwatch.app.data.local.ModeStore
import com.pumpwatch.app.data.repository.TradeRepository
import com.pumpwatch.app.domain.MarketMode
import com.pumpwatch.app.presentation.ModeSelectScreen
import com.pumpwatch.app.ui.auth.AuthLoadingState
import com.pumpwatch.app.ui.auth.AuthScreenState
import com.pumpwatch.app.ui.auth.AuthViewModel
import com.pumpwatch.app.ui.auth.LoginScreen
import com.pumpwatch.app.ui.auth.SetupLoginMethodScreen
import com.pumpwatch.app.ui.backtest.BacktestScreen
import com.pumpwatch.app.ui.backtest.BacktestViewModel
import com.pumpwatch.app.ui.crash.CrashLogStore
import com.pumpwatch.app.ui.crash.CrashScreen
import com.pumpwatch.app.ui.dashboard.DashboardScreen
import com.pumpwatch.app.ui.dashboard.DashboardViewModel
import com.pumpwatch.app.ui.detail.CoinDetailScreen
import com.pumpwatch.app.ui.settings.SettingsScreen
import com.pumpwatch.app.ui.settings.SettingsViewModel
import com.pumpwatch.app.ui.theme.PumpWatchTheme
import com.pumpwatch.app.ui.trades.TradesScreen
import com.pumpwatch.app.ui.trades.TradesViewModel
import com.pumpwatch.app.worker.MonitoringWorker

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            MonitoringWorker.schedule(applicationContext)
        }
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
            PumpWatchTheme {
                val modeStore = remember { ModeStore(applicationContext) }
                var modeChosen by remember { mutableStateOf(modeStore.hasChosen()) }
                var crashText by remember { mutableStateOf<String?>(null) }

                if (!modeChosen) {
                    // Show mode selection first
                    ModeSelectScreen { chosenMode ->
                        modeStore.save(chosenMode)
                        modeChosen = true
                    }
                } else if (crashText != null) {
                    CrashScreen(
                        crashText = crashText,
                        onDismiss = {
                            CrashLogStore.clear(applicationContext)
                            crashText = null
                        }
                    )
                } else {
                    val authVm: AuthViewModel = viewModel()
                    val screenState by authVm.screenState.collectAsState()
                    val authError by authVm.authError.collectAsState()

                    LaunchedEffect(authError) {
                        if (authError != null) {
                            // Show error
                        }
                    }

                    when (screenState) {
                        AuthScreenState.NotLoggedIn -> LoginScreen(
                            onLoginClick = { authVm.login() },
                            onToggleMonitoring = { active ->
                                if (active) {
                                    notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                } else {
                                    MonitoringWorker.cancel(applicationContext)
                                }
                            }
                        )
                        AuthScreenState.LoggedIn -> AppNavHost(
                            onToggleMonitoring = { active ->
                                if (active) {
                                    notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                } else {
                                    MonitoringWorker.cancel(applicationContext)
                                }
                            }
                        )
                        AuthScreenState.Monitoring -> AppNavHost(
                            onToggleMonitoring = { active ->
                                if (active) {
                                    notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                } else {
                                    MonitoringWorker.cancel(applicationContext)
                                }
                            }
                        )
                        AuthScreenState.Loading -> {
                            // Loading
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AppNavHost(onToggleMonitoring: (Boolean) -> Unit) {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "dashboard") {
            composable("dashboard") {
                val vm: DashboardViewModel = viewModel()
                val state by vm.uiState.collectAsState()

                DashboardScreen(
                    state = state,
                    onToggleMonitoring = onToggleMonitoring,
                    onRefresh = vm::refreshCoins,
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenTrades = { navController.navigate("trades") },
                    onOpenBacktest = { navController.navigate("backtest") },
                    onOpenCoin = { coinId -> navController.navigate("coin/$coinId") }
                )
            }

            composable("backtest") {
                val vm: BacktestViewModel = viewModel()
                val coins by vm.candidateCoins.collectAsState()
                val state by vm.uiState.collectAsState()

                BacktestScreen(
                    candidateCoins = coins,
                    uiState = state,
                    onBack = { navController.popBackStack() },
                    onRun = vm::run
                )
            }

            composable("trades") {
                val vm: TradesViewModel = viewModel()
                val trades by vm.trades.collectAsState()
                val tradeSettings by vm.settings.collectAsState()

                TradesScreen(
                    trades = trades,
                    settings = tradeSettings,
                    onBack = { navController.popBackStack() },
                    onSaveSettings = vm::saveSettings
                )
            }

            composable("settings") {
                val vm: SettingsViewModel = viewModel()
                val settings by vm.settings.collectAsState()

                SettingsScreen(
                    current = settings,
                    onBack = { navController.popBackStack() },
                    onSave = vm::save
                )
            }

            composable("coin/{coinId}") { backStackEntry ->
                val coinId = backStackEntry.arguments?.getString("coinId") ?: ""
                val vm: DashboardViewModel = viewModel()
                val state by vm.uiState.collectAsState()
                val coin = state.coins.find { it.id == coinId }
                val signal = state.signals[coinId]

                CoinDetailScreen(
                    coin = coin,
                    signal = signal,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
