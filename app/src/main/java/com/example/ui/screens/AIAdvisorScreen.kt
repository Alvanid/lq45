package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.engine.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAdvisorScreen(
    repository: StockRepository,
    onNavigateBack: () -> Unit
) {
    var selectedSymbol by remember { mutableStateOf("BBCA.JK") }
    val allSymbols by repository.allSymbols.collectAsState(initial = emptyList())
    val candlesState = repository.getCandles(selectedSymbol).collectAsState(initial = emptyList())
    val predictionState = repository.getPrediction(selectedSymbol).collectAsState(initial = null)
    val performanceState = repository.getModelPerformance(selectedSymbol).collectAsState(initial = null)

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // AI Analysis status
    var aiReportMarkdown by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }

    // Run programmatic recommendations calculations
    val calculatedAdvisorData = remember(candlesState.value, predictionState.value) {
        val candles = candlesState.value
        val pred = predictionState.value
        val indicators = IndicatorsCalculator.calculate(candles)
        val lastIdx = indicators.lastIndex
        if (lastIdx >= 0 && pred != null) {
            val lastInd = indicators[lastIdx]
            val rsi = lastInd.rsi14 ?: 50.0
            val macd = lastInd.macd ?: 0.0
            val sig = lastInd.signal ?: 0.0
            val sma20 = lastInd.sma20
            val sma50 = lastInd.sma50

            Advisor.generateRecommendation(
                currentPrice = candles.last().closePrice,
                predPrice = pred.pred1Day,
                rsi = rsi,
                macd = macd,
                signal = sig,
                sma20 = sma20,
                sma50 = sma50
            )
        } else {
            AIRecommendation("HOLD", 50, 50, "Low", "Low", "• Menghitung indikator momentum...")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI REKOMENDASI & SENTIMEN SAHAM", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
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
                .verticalScroll(scrollState)
        ) {
            // STOCK EXPLORER SELECTOR
            Text("Pilih Saham untuk Analisis Taktis", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val safeSymbols = if (allSymbols.isEmpty()) listOf("BBCA.JK", "BBRI.JK", "BMRI.JK", "TLKM.JK", "GOTO.JK") else allSymbols
                safeSymbols.take(5).forEach { sym ->
                    val selected = sym == selectedSymbol
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) PrimaryNeon else DarkSurface)
                            .border(1.dp, if (selected) AccentCyan else DarkBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                selectedSymbol = sym
                                aiReportMarkdown = "" // Reset analysis on change
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(sym.removeSuffix(".JK"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // SCORE CARDS OVERVIEW (Status, buy, sell, risk)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Recommendation Badge Code
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Rekomendasi Algoritmik", color = TextSecondary, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        val status = calculatedAdvisorData.status
                        val col = when {
                            status.contains("STRONG BUY") -> ProfitGreen
                            status.contains("BUY") -> ProfitGreen.copy(alpha = 0.8f)
                            status.contains("STRONG SELL") -> LossRed
                            status.contains("SELL") -> LossRed.copy(alpha = 0.8f)
                            else -> NeutralOrange
                        }
                        Text(
                            text = status,
                            color = col,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Buy vs Sell Score meters
                Card(
                    modifier = Modifier.weight(1.3f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Beli: ${calculatedAdvisorData.buyScore}%", color = ProfitGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("Jual: ${calculatedAdvisorData.sellScore}%", color = LossRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // Progress bar meter
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        ) {
                            Box(modifier = Modifier.weight(calculatedAdvisorData.buyScore.toFloat()).fillMaxHeight().background(ProfitGreen))
                            Box(modifier = Modifier.weight(calculatedAdvisorData.sellScore.toFloat()).fillMaxHeight().background(LossRed))
                        }
                    }
                }
            }

            // COMPOSITE PROGRAMMATIC REASONS TEXTBOX
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Analisis Indikator Kuantitatif Terpadu",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Text(
                        text = calculatedAdvisorData.reason,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // GEMINI AI DEEP ANALYSER CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Asisten Gemini Pro AI",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Run AI analysis button
                        Button(
                            onClick = {
                                if (isAnalyzing) return@Button
                                isAnalyzing = true
                                coroutineScope.launch {
                                    val candles = candlesState.value
                                    val indicators = IndicatorsCalculator.calculate(candles)
                                    val lastInd = indicators.lastOrNull()
                                    val rsiVal = lastInd?.rsi14 ?: 50.0
                                    val macdVal = lastInd?.macd ?: 0.0
                                    val sigVal = lastInd?.signal ?: 0.0
                                    
                                    val pred = predictionState.value
                                    val accuracyR2 = performanceState.value?.r2 ?: 0.85
                                    val meta = StockCatalog.getBySymbol(selectedSymbol)

                                    aiReportMarkdown = GeminiAdvisor.analyzeStockWithAI(
                                        symbol = selectedSymbol,
                                        fullName = meta?.fullName ?: selectedSymbol,
                                        currentPrice = candles.lastOrNull()?.closePrice ?: 1000.0,
                                        pred1Day = pred?.pred1Day ?: 1000.0,
                                        pred7Days = pred?.pred7Days ?: 1100.0,
                                        pred30Days = pred?.pred30Days ?: 1200.0,
                                        rsi = rsiVal,
                                        macd = macdVal,
                                        signal = sigVal,
                                        trend = pred?.trend ?: "Sideways",
                                        accuracyR2 = accuracyR2
                                    )
                                    isAnalyzing = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Generate AI Report", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (aiReportMarkdown.isEmpty()) {
                        Text(
                            "Klik tombol di atas untuk mengirim data teknikal saham ke model LLM Gemini dan menghasilkan laporan komprehensif investasi profesional berstandar Bloomberg.",
                            color = TextHint,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBackground)
                                .border(1.dp, DarkBorder)
                                .padding(12.dp)
                        ) {
                            // Display analyzed markdown report
                            Text(
                                text = aiReportMarkdown,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
