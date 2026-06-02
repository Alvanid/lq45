package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.engine.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: StockRepository,
    currentUser: String,
    currentUserRole: String,
    onNavigateTo: (String) -> Unit
) {
    // Current ticker selection
    var selectedSymbol by remember { mutableStateOf("BBCA.JK") }
    val candlesState = repository.getCandles(selectedSymbol).collectAsState(initial = emptyList())
    val predictionState = repository.getPrediction(selectedSymbol).collectAsState(initial = null)
    val performanceState = repository.getModelPerformance(selectedSymbol).collectAsState(initial = null)
    
    val allPredictions by repository.allPredictions.collectAsState(initial = emptyList())
    val allSymbols by repository.allSymbols.collectAsState(initial = emptyList())
    val isWatchlistedState = repository.isWatchlisted(selectedSymbol).collectAsState(initial = false)

    // Chart display settings
    var showMa20 by remember { mutableStateOf(true) }
    var showMa50 by remember { mutableStateOf(false) }
    var showBollinger by remember { mutableStateOf(false) }
    var selectedSubPanel by remember { mutableStateOf("VOLUME") } // VOLUME, RSI, MACD

    val coroutineScope = rememberCoroutineScope()
    var alertLogs = remember {
        mutableStateListOf(
            "BERITA: Volume perdagangan BBRI melonjak di atas rata-rata harian.",
            "ALERT: Sinyal Stochastic RSI BBCA meluncur ke daerah oversold.",
            "ALERT: Model ANN mendeteksi sinyal breakout bullish dari GOTO.",
            "BERITA: Bursa Efek Indonesia mencatat penguatan indeks LQ45 +0.8%"
        )
    }

    // Dynamic fear & greed calculation based on RSI of loaded predictions
    val fearScore = remember(allPredictions) {
        if (allPredictions.isEmpty()) 54
        else {
            val avgConfidence = allPredictions.map { it.confidenceScore }.average()
            val baseScore = (avgConfidence * 100.0).toInt().coerceIn(10, 90)
            baseScore
        }
    }

    val bullCount = remember(allPredictions) {
        allPredictions.count { it.trend == "Bullish" }
    }
    val bearCount = remember(allPredictions) {
        allPredictions.count { it.trend == "Bearish" }
    }

    val indicators = remember(candlesState.value) {
        IndicatorsCalculator.calculate(candlesState.value)
    }

    // Trigger on-demand sync
    var isSyncing by remember { mutableStateOf(false) }
    var syncResultMsg by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x331E3A8A), // Blue 900 20%
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SYSTEM STATUS: LIVE",
                            fontSize = 10.sp,
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LQ45 PRO ",
                                fontSize = 20.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "ANN",
                                fontSize = 20.sp,
                                color = PrimaryNeon,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Secure active card info
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfacePressed)
                                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "$currentUser ($currentUserRole)",
                                color = AccentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Notification Badge Icon
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(DarkSurfacePressed)
                                .border(1.dp, DarkBorder, RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔔", fontSize = 16.sp)
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(LossRed)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-4).dp, y = 4.dp)
                            )
                        }

                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(PrimaryNeon)
                                .clickable { onNavigateTo("login") },
                            contentAlignment = Alignment.Center
                        ) {
                            val initials = if (currentUser.isNotEmpty()) currentUser.take(2).uppercase() else "JD"
                            Text(
                                text = initials,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Elegant horizontal bar mimicking dynamic real-time prices & news alerts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(PrimaryNeon.copy(alpha = 0.15f))
                    .border(1.dp, PrimaryNeon.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = alertLogs.lastOrNull() ?: "Menara pengawas sentimen aktif.",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
                .immersiveBackground()
        ) {
            // SIDEBAR COMPONENT (Bloomberg Desktop style)
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .background(DarkSurface)
                    .drawBehind {
                        drawLine(
                            color = DarkBorder,
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                SidebarIcon(Icons.Default.Dashboard, "DASHBOARD", true) {}
                SidebarIcon(Icons.Default.Favorite, "WATCHLIST", false) { onNavigateTo("watchlist") }
                SidebarIcon(Icons.Default.Analytics, "DIAGNOSTIC", false) { onNavigateTo("training") }
                SidebarIcon(Icons.Default.SmartToy, "ADVISOR", false) { onNavigateTo("ai_advisor") }
                SidebarIcon(Icons.Default.Work, "PORTFOLIO", false) { onNavigateTo("portfolio") }
                SidebarIcon(Icons.Default.SettingsBackupRestore, "BACKTEST", false) { onNavigateTo("backtesting") }
                SidebarIcon(Icons.Default.GetApp, "EXPORTS", false) { onNavigateTo("exports") }
            }

            // MAIN CONTENT ROW: Divided into Details/Graphs & Market Grid
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                // ROW 1: QUICK STATS TITLE CARDS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val meta = StockCatalog.getBySymbol(selectedSymbol)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = meta?.fullName ?: selectedSymbol,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = selectedSymbol,
                                color = AccentCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = meta?.sector ?: "LQ45 Index",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // On-demand Sync Trigger
                    Button(
                        onClick = {
                            if (isSyncing) return@Button
                            isSyncing = true
                            syncResultMsg = ""
                            coroutineScope.launch {
                                try {
                                    val count = repository.syncStockData(selectedSymbol)
                                    syncResultMsg = "Sukses mengambil $count data historis & melatih model!"
                                    alertLogs.add("SYNC: Berhasil memperbarui data historis untuk $selectedSymbol ($count candle).")
                                } catch (e: Exception) {
                                    syncResultMsg = "Gagal mengambil data: ${e.message}"
                                } finally {
                                    isSyncing = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("sync_btn")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Stock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Toggle favorite button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                if (isWatchlistedState.value) {
                                    repository.removeFromWatchlist(selectedSymbol)
                                } else {
                                    repository.addToWatchlist(selectedSymbol)
                                    alertLogs.add("WATCHLIST: Menambahkan $selectedSymbol ke daftar pantau favorit.")
                                }
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isWatchlistedState.value) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Watchlist",
                            tint = if (isWatchlistedState.value) LossRed else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (syncResultMsg.isNotEmpty()) {
                    Text(
                        text = syncResultMsg,
                        color = if (syncResultMsg.startsWith("Sukses")) ProfitGreen else LossRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ROW 2: CHART ROW WITH CHART SELECTOR OVERLAYS
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.3f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Drawing controller toggles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sub panel indicators
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("VOLUME", "RSI", "MACD").forEach { panel ->
                                    val act = selectedSubPanel == panel
                                    Button(
                                        onClick = { selectedSubPanel = panel },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (act) PrimaryNeon.copy(alpha = 0.25f) else Color.Transparent,
                                            contentColor = if (act) AccentCyan else TextSecondary
                                        ),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, if (act) PrimaryNeon else DarkBorder)
                                    ) {
                                        Text(panel, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Moving Average toggle overlays
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = showMa20,
                                    onClick = { showMa20 = !showMa20 },
                                    label = { Text("EMA20", fontSize = 9.sp) },
                                    modifier = Modifier.height(26.dp)
                                )
                                FilterChip(
                                    selected = showMa50,
                                    onClick = { showMa50 = !showMa50 },
                                    label = { Text("EMA50", fontSize = 9.sp) },
                                    modifier = Modifier.height(26.dp)
                                )
                                FilterChip(
                                    selected = showBollinger,
                                    onClick = { showBollinger = !showBollinger },
                                    label = { Text("Bands", fontSize = 9.sp) },
                                    modifier = Modifier.height(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Draw live chart
                        val latestPredictionVal = predictionState.value?.pred1Day
                        CandleChart(
                            indicators = indicators,
                            predictionPricetomorrow = latestPredictionVal,
                            showMa20 = showMa20,
                            showMa50 = showMa50,
                            showBollinger = showBollinger,
                            activeSubPanel = selectedSubPanel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ROW 3: FOOTER ROW SPLIT BETWEEN HEATMAP AND METERS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // LEFT COLUMN: FEAR-GREED + TARGET SCORE CARD
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FearAndGreedMeter(
                                score = fearScore,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            SentimentGauge(
                                bullishCount = if (bullCount == 0) 3 else bullCount,
                                bearishCount = if (bearCount == 0) 1 else bearCount,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }

                    // RIGHT COLUMN: MEMBERS SELECTABLE LQ45 HEATMAP
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                "LQ45 Heatmap (Sentimen Tren Model)",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            val displayGridList = remember(allPredictions) {
                                StockCatalog.stocks.map { m ->
                                    val p = allPredictions.firstOrNull { it.symbol == m.symbol }
                                    Pair(m, p)
                                }
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(62.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(displayGridList) { pair ->
                                    val m = pair.first
                                    val p = pair.second
                                    val isSelected = m.symbol == selectedSymbol
                                    
                                    val boxColor = when (p?.trend) {
                                        "Bullish" -> ProfitGreen.copy(alpha = 0.35f)
                                        "Bearish" -> LossRed.copy(alpha = 0.35f)
                                        else -> PrimaryNeon.copy(alpha = 0.2f)
                                    }
                                    val borderColor = if (isSelected) AccentCyan else DarkBorder

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(boxColor)
                                            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                                            .clickable { selectedSymbol = m.symbol }
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = m.shortName,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val valueText = p?.currentPrice?.let {
                                                if (it >= 1000) "${(it / 1000).toInt()}k" else it.toString()
                                            } ?: "Sync"
                                            Text(
                                                text = valueText,
                                                color = if (p?.trend == "Bullish") ProfitGreen else if (p?.trend == "Bearish") LossRed else TextSecondary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SidebarIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = label,
            tint = if (active) AccentCyan else TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) AccentCyan else TextHint,
            letterSpacing = 0.5.sp
        )
    }
}
