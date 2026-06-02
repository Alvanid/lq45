package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.StockRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var stockRepository: StockRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Instantiate repository
        stockRepository = StockRepository(applicationContext)

        // Prepopulate SQLite candles on background thread
        lifecycleScope.launch {
            stockRepository.checkAndPrepopulate()
        }

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf("login") }
                var sessionUser by remember { mutableStateOf("") }
                var sessionRole by remember { mutableStateOf("") }

                Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                    when (screen) {
                        "login" -> {
                            LoginScreen(
                                onLoginSuccess = { username, role ->
                                    sessionUser = username
                                    sessionRole = role
                                    currentScreen = "dashboard"
                                }
                            )
                        }
                        "dashboard" -> {
                            DashboardScreen(
                                repository = stockRepository,
                                currentUser = sessionUser,
                                currentUserRole = sessionRole,
                                onNavigateTo = { targetRoute ->
                                    currentScreen = targetRoute
                                }
                            )
                        }
                        "watchlist" -> {
                            WatchlistScreen(
                                repository = stockRepository,
                                onNavigateBack = { currentScreen = "dashboard" }
                            )
                        }
                        "backtesting" -> {
                            BacktestingScreen(
                                repository = stockRepository,
                                onNavigateBack = { currentScreen = "dashboard" }
                            )
                        }
                        "portfolio" -> {
                            PortfolioScreen(
                                repository = stockRepository,
                                onNavigateBack = { currentScreen = "dashboard" }
                            )
                        }
                        "ai_advisor" -> {
                            AIAdvisorScreen(
                                repository = stockRepository,
                                onNavigateBack = { currentScreen = "dashboard" }
                            )
                        }
                        "training" -> {
                            TrainingScreen(
                                repository = stockRepository,
                                onNavigateBack = { currentScreen = "dashboard" }
                            )
                        }
                        "exports" -> {
                            ExportsScreen(
                                repository = stockRepository,
                                onNavigateBack = { currentScreen = "dashboard" }
                            )
                        }
                    }
                }
            }
        }
    }
}
