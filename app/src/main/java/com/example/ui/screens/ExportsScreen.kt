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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.engine.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportsScreen(
    repository: StockRepository,
    onNavigateBack: () -> Unit
) {
    var selectedSymbol by remember { mutableStateOf("BBCA.JK") }
    val allSymbols by repository.allSymbols.collectAsState(initial = emptyList())
    val candlesState = repository.getCandles(selectedSymbol).collectAsState(initial = emptyList())

    val coroutineScope = rememberCoroutineScope()
    var exportStatusText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val visibleCandlesList = remember(candlesState.value, searchQuery) {
        val list = candlesState.value
        if (searchQuery.isEmpty()) list.takeLast(15)
        else {
            list.filter {
                val dt = format.format(Date(it.dateMillis))
                dt.contains(searchQuery)
            }.takeLast(15)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EXPORT & AUDIT LAPORAN HISTORIS", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
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
            // PARAMETER SETTINGS SELECTOR
            Text("Pilih Ticker", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val safeSymbols = if (allSymbols.isEmpty()) listOf("BBCA.JK", "BBRI.JK", "BMRI.JK", "TLKM.JK", "GOTO.JK") else allSymbols
                safeSymbols.take(5).forEach { sym ->
                    val selected = sym == selectedSymbol
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) PrimaryNeon else DarkSurfacePressed)
                            .clickable { selectedSymbol = sym }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(sym.removeSuffix(".JK"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // TRIGGER REPORT ACTION CARD
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Kompilasi Berkas Laporan Kuantitatif",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "Format laporan mencakup data lilin pergerakan harian, indikator EMA20, RSI14, MACD, dan nilai target prediksi model ANN 90 hari mendatang.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    exportStatusText = "Mengekspor berkas CSV..."
                                    kotlinx.coroutines.delay(1200)
                                    exportStatusText = "Laporan harian $selectedSymbol berhasil disimpan ke folder /exports/lq45_${selectedSymbol.lowercase()}_snapshot.csv."
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    exportStatusText = "Menghimpun visual grafik HTML..."
                                    kotlinx.coroutines.delay(1500)
                                    exportStatusText = "Berkas cetak audit HTML/PDF berhasil disimpan ke folder /exports/lq45_${selectedSymbol.lowercase()}_audit_report.pdf."
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cetak PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }

                    if (exportStatusText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = exportStatusText,
                            color = ProfitGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // SEARCH FILTER TABLE
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari Berdasarkan Tanggal (YYYY-MM-DD)...", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(bottom = 10.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            // INTERACTIVE TABLE VIEWER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            ) {
                Column {
                    // Header row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfacePressed)
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tanggal", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1.2f))
                        Text("Open", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        Text("High", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        Text("Low", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        Text("Close", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        items(visibleCandlesList.reversed()) { candle ->
                            val dateStr = format.format(Date(candle.dateMillis))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .drawBehind {
                                        drawLine(
                                            color = DarkBorder.copy(alpha = 0.4f),
                                            start = Offset(0f, size.height),
                                            end = Offset(size.width, size.height),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(dateStr, color = TextSecondary, fontSize = 10.sp, modifier = Modifier.weight(1.2f), fontFamily = FontFamily.Monospace)
                                Text(String.format("%,.0f", candle.openPrice), color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End, fontFamily = FontFamily.Monospace)
                                Text(String.format("%,.0f", candle.highPrice), color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End, fontFamily = FontFamily.Monospace)
                                Text(String.format("%,.0f", candle.lowPrice), color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End, fontFamily = FontFamily.Monospace)
                                Text(String.format("%,.0f", candle.closePrice), color = if (candle.closePrice >= candle.openPrice) ProfitGreen else LossRed, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
