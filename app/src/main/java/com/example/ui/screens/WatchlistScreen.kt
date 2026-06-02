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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
fun WatchlistScreen(
    repository: StockRepository,
    onNavigateBack: () -> Unit
) {
    val watchlistItems by repository.watchlist.collectAsState(initial = emptyList())
    val allPredictions by repository.allPredictions.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DAFTAR PANTAU FAVORIT (WATCHLIST)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
            Text(
                "Grup pemantauan taktis LQ45 terpilih untuk transaksi taktis harian investor.",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (watchlistItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TextHint
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Watchlist Anda Kosong.",
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Tambahkan saham favorit Anda dari halaman Dashboard.",
                            color = TextHint,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(watchlistItems) { watch ->
                        val mVal = StockCatalog.getBySymbol(watch.symbol)
                        val pVal = allPredictions.firstOrNull { it.symbol == watch.symbol }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { },
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, DarkBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Ticker branding
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mVal?.shortName ?: watch.symbol.removeSuffix(".JK"),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = mVal?.fullName ?: "Indeks LQ45",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                // Performance / Price details
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    val actPrice = pVal?.currentPrice ?: mVal?.basePrice ?: 0.0
                                    Text(
                                        text = "Rp ${String.format("%,.0f", actPrice)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    val predValue = pVal?.pred1Day ?: actPrice
                                    val changeValPct = ((predValue - actPrice) / actPrice) * 100.0
                                    val isUp = changeValPct >= 0
                                    val diffColor = if (isUp) ProfitGreen else LossRed

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            tint = diffColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${if (isUp) "+" else ""}${String.format("%.2f%%", changeValPct)}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = diffColor
                                        )
                                        Text(
                                            text = " (ANN)",
                                            fontSize = 9.sp,
                                            color = TextHint
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Remove icon
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.removeFromWatchlist(watch.symbol)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = LossRed.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
