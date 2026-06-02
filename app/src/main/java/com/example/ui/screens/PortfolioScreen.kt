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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.engine.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    repository: StockRepository,
    onNavigateBack: () -> Unit
) {
    val transactions by repository.portfolioTransactions.collectAsState(initial = emptyList())
    val allPredictions by repository.allPredictions.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    // Add transaction sheet form state
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedSymbol by remember { mutableStateOf("BBCA.JK") }
    var buyPriceText by remember { mutableStateOf("") }
    var lotsText by remember { mutableStateOf("") }

    val initialCapital = 100_000_000.0 // static simulated capital

    // Compute portfolio statistics based on active predictions prices
    val totalInvested = remember(transactions) {
        transactions.sumOf { it.buyPrice * it.lots * 100.0 }
    }

    val currentEquityValue = remember(transactions, allPredictions) {
        transactions.sumOf { tx ->
            val livePrice = allPredictions.firstOrNull { it.symbol == tx.symbol }?.currentPrice ?: tx.buyPrice
            livePrice * tx.lots * 100.0
        }
    }

    val cashRemaining = initialCapital - totalInvested
    val totalPortfolioValue = cashRemaining + currentEquityValue
    val roiPct = ((totalPortfolioValue - initialCapital) / initialCapital) * 100.0
    val roiProfitVal = totalPortfolioValue - initialCapital

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PORTFOLIO SIMULATOR PRO", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Simulate Trade", tint = AccentCyan)
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
            // EQUITY SUMMARY HEADER
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Nilai Portfolio Simulator", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rp ${String.format("%,.0f", totalPortfolioValue)}",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (roiProfitVal >= 0) ProfitGreen.copy(alpha = 0.2f) else LossRed.copy(alpha = 0.2f))
                                .border(1.dp, if (roiProfitVal >= 0) ProfitGreen else LossRed, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${if (roiProfitVal >= 0) "+" else ""}${String.format("%.2f%%", roiPct)}",
                                color = if (roiProfitVal >= 0) ProfitGreen else LossRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Divider(color = DarkBorder)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Total ROI", color = TextSecondary, fontSize = 9.sp)
                            Text(
                                text = "Rp ${String.format("%+,.0f", roiProfitVal)}",
                                color = if (roiProfitVal >= 0) ProfitGreen else LossRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("Kas Terpakai (Beli)", color = TextSecondary, fontSize = 9.sp)
                            Text(
                                text = "Rp ${String.format("%,.0f", totalInvested)}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("Saldo Kas Kosong", color = TextSecondary, fontSize = 9.sp)
                            Text(
                                text = "Rp ${String.format("%,.0f", cashRemaining.coerceAtLeast(0.0))}",
                                color = AccentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Text(
                "Daftar Kepemilikan Saham Aktif",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // ACTIVE INVESTMENTS LIST
            if (transactions.isEmpty()) {
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
                        "Simulator Kosong. Tambahkan simulasi transaksi Beli dengan mengklik ikon '+' di pojok kanan atas.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions) { tx ->
                        val meta = StockCatalog.getBySymbol(tx.symbol)
                        val liveStats = allPredictions.firstOrNull { it.symbol == tx.symbol }

                        val currentPrice = liveStats?.currentPrice ?: tx.buyPrice
                        val investedValue = tx.buyPrice * tx.lots * 100.0
                        val currentLiveValue = currentPrice * tx.lots * 100.0
                        val profitVal = currentLiveValue - investedValue
                        val profitPct = (profitVal / investedValue) * 100.0

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, DarkBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text(
                                        text = meta?.shortName ?: tx.symbol.removeSuffix(".JK"),
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${tx.lots} Lot (Rp ${String.format("%,.0f", tx.buyPrice)}/Lbr)",
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1.2f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "Rp ${String.format("%,.0f", currentLiveValue)}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Rp ${String.format("%+,.0f", profitVal)} (${String.format("%+.2f%%", profitPct)})",
                                        color = if (profitVal >= 0) ProfitGreen else LossRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.removeTransaction(tx)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Sell", tint = LossRed.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // SIMULATED ADD TRANSACTION DIALOG SHEET
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("SIMULASIKAN PEMBELIAN SAHAM", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Symbol selector
                    Column {
                        Text("Kode Saham (LQ45)", color = TextSecondary, fontSize = 10.sp)
                        Row {
                            listOf("BBCA.JK", "BBRI.JK", "BMRI.JK", "TLKM.JK", "GOTO.JK").forEach { s ->
                                val selected = s == selectedSymbol
                                Box(
                                    modifier = Modifier
                                        .padding(end = 6.dp, top = 4.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (selected) PrimaryNeon else DarkBorder)
                                        .clickable {
                                            selectedSymbol = s
                                            val metaPrice = StockCatalog.getBySymbol(s)?.basePrice ?: 1000.0
                                            buyPriceText = metaPrice.toInt().toString()
                                        }
                                        .padding(6.dp)
                                ) {
                                    Text(s.removeSuffix(".JK"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    TextField(
                        value = buyPriceText,
                        onValueChange = { buyPriceText = it },
                        label = { Text("Harga Beli per Lembar Sembari", fontSize = 11.sp) },
                        colors = TextFieldDefaults.colors(focusedContainerColor = DarkSurfacePressed, unfocusedContainerColor = DarkSurfacePressed),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextField(
                        value = lotsText,
                        onValueChange = { lotsText = it },
                        label = { Text("Jumlah Lot (1 Lot = 100 Lembar)", fontSize = 11.sp) },
                        colors = TextFieldDefaults.colors(focusedContainerColor = DarkSurfacePressed, unfocusedContainerColor = DarkSurfacePressed),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val buyPrice = buyPriceText.toDoubleOrNull() ?: 1000.0
                        val lots = lotsText.toIntOrNull() ?: 1
                        coroutineScope.launch {
                            repository.addTransaction(selectedSymbol, buyPrice, lots)
                            showAddDialog = false
                            buyPriceText = ""
                            lotsText = ""
                        }
                    }
                ) {
                    Text("SIMPAN TRANSAKSI", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("KEMBALI", color = TextSecondary, fontSize = 12.sp)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }
}
