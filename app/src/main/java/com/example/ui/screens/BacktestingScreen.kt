package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.engine.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BacktestingScreen(
    repository: StockRepository,
    onNavigateBack: () -> Unit
) {
    var selectedSymbol by remember { mutableStateOf("BBCA.JK") }
    val allSymbols by repository.allSymbols.collectAsState(initial = emptyList())
    val candlesState = repository.getCandles(selectedSymbol).collectAsState(initial = emptyList())

    var initialBalanceText by remember { mutableStateOf("100000000") }
    var backtestResult by remember { mutableStateOf<BacktestMetrics?>(null) }
    var isSimulating by remember { mutableStateOf(false) }

    val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BACKTESTING STRATEGI ALGORITMIK", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
                .immersiveBackground()
                .padding(16.dp)
        ) {
            // PARAMETER SETTING INPUT PANEL
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Konfigurasi Parameter Backtest",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stock selecting spinner-like dropdown
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pilih Saham", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfacePressed)
                                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                    .clickable { }
                                    .padding(horizontal = 12.dp, vertical = 2.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                // Ticker chooser row scroll
                                Row {
                                    val safeSymbols = if (allSymbols.isEmpty()) listOf("BBCA.JK", "BBRI.JK", "BMRI.JK", "TLKM.JK", "GOTO.JK") else allSymbols
                                    safeSymbols.take(5).forEach { sym ->
                                        val selected = sym == selectedSymbol
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 4.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (selected) PrimaryNeon else Color.Transparent)
                                                .clickable { selectedSymbol = sym }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(sym.removeSuffix(".JK"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        // Capital Input
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Modal Awal (Rp)", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                            TextField(
                                value = initialBalanceText,
                                onValueChange = { initialBalanceText = it },
                                modifier = Modifier.height(44.dp).fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurfacePressed,
                                    unfocusedContainerColor = DarkSurfacePressed,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isSimulating = true
                            val capital = initialBalanceText.toDoubleOrNull() ?: 100_000_000.0
                            val calculatedIndicators = IndicatorsCalculator.calculate(candlesState.value)
                            backtestResult = Backtester.runBacktest(calculatedIndicators, capital)
                            isSimulating = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mulai Simulasi Backtest", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // RESULTS VIEWPANEL
            val result = backtestResult
            if (result == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Konfigurasikan parameter di atas lalu klik Mulai Simulasi.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                Text(
                    "Hasil Backtest: ${selectedSymbol.removeSuffix(".JK")}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // High fidelity metric cards in grid
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Profit Bersih",
                        value = "Rp ${String.format("%,.0f", result.totalProfit)}",
                        percent = "${if (result.returnPct >= 0) "+" else ""}${String.format("%.2f%%", result.returnPct)}",
                        isPositive = result.returnPct >= 0,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Win Rate",
                        value = "${String.format("%.1f%%", result.winRate)}",
                        percent = "${result.totalTrades} Transaksi",
                        isPositive = result.winRate >= 50.0,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Profit Factor",
                        value = String.format("%.2f", result.profitFactor),
                        percent = "Faktor Rasio",
                        isPositive = result.profitFactor >= 1.5,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Max Drawdown",
                        value = String.format("%.2f%%", result.maxDrawdown),
                        percent = "Kerugian Maks",
                        isPositive = false, // Lower is better
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Sharpe Ratio",
                        value = String.format("%.2f", result.sharpeRatio),
                        percent = "Risk-Adjusted",
                        isPositive = result.sharpeRatio >= 1.0,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    "Riwayat Logs Transaksi Algoritmik",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Trade transactions logs list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(result.listTrades.reversed()) { trade ->
                        val isBuy = trade.type == "BUY"
                        val dateText = format.format(Date(trade.dateMillis))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurface)
                                .border(1.dp, if (isBuy) PrimaryNeon.copy(alpha = 0.5f) else LossRed.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isBuy) PrimaryNeon else LossRed)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(trade.type, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(dateText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("Shares: ${trade.shares} Lbr", color = TextSecondary, fontSize = 9.sp)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Harga: Rp ${String.format("%,.0f", trade.price)}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    if (!isBuy && trade.profitEarned != null) {
                                        Text(
                                            text = "Profit: Rp ${String.format("%+,.0f", trade.profitEarned)} (${String.format("%+.2f%%", trade.returnPct)})",
                                            color = if (trade.profitEarned >= 0) ProfitGreen else LossRed,
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

@Composable
fun MetricCard(
    title: String,
    value: String,
    percent: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(title, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = percent,
            color = if (title == "Max Drawdown") LossRed else if (isPositive) ProfitGreen else TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
