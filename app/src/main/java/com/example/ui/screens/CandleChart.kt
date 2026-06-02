package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.TechnicalIndicators
import com.example.ui.theme.*
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

@Composable
fun CandleChart(
    indicators: List<TechnicalIndicators>,
    predictionPricetomorrow: Double?,
    showMa20: Boolean = true,
    showMa50: Boolean = false,
    showBollinger: Boolean = false,
    activeSubPanel: String = "VOLUME", // "VOLUME", "RSI", "MACD"
    modifier: Modifier = Modifier
) {
    if (indicators.size < 10) {
        Box(modifier = modifier.fillMaxSize()) {
            Text("Tidak cukup data untuk memplot grafik.", color = TextSecondary)
        }
        return
    }

    // Capture visible portion of the data
    val displayedCandlesCount = 45
    val visibleData = remember(indicators) {
        indicators.takeLast(displayedCandlesCount)
    }

    // Touch interaction status
    var activeCrosshairsIdx by remember { mutableStateOf<Int?>(null) }
    var touchLocationX by remember { mutableStateOf(0f) }

    val selectedIndicator = activeCrosshairsIdx?.let { visibleData.getOrNull(it) }

    Column(modifier = modifier) {
        // OHLC info bar at the top of the chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val target = selectedIndicator ?: visibleData.last()
            val o = target.candle.openPrice
            val h = target.candle.highPrice
            val l = target.candle.lowPrice
            val c = target.candle.closePrice
            val diff = c - o
            val diffPct = (diff / o) * 100.0
            val clr = if (diff >= 0) ProfitGreen else LossRed

            Text(
                "O: ${String.format("%,.1f", o)}",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "H: ${String.format("%,.1f", h)}",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "L: ${String.format("%,.1f", l)}",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "C: ${String.format("%,.1f", c)}",
                color = clr,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${if (diffPct >= 0) "+" else ""}${String.format("%.2f%%", diffPct)}",
                color = clr,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Main Candlestick plot
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(visibleData) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val sectionWidth = size.width / displayedCandlesCount
                            val idx = (offset.x / sectionWidth).toInt().coerceIn(0, displayedCandlesCount - 1)
                            activeCrosshairsIdx = idx
                            touchLocationX = offset.x
                        },
                        onDragEnd = {
                            activeCrosshairsIdx = null
                        },
                        onDragCancel = {
                            activeCrosshairsIdx = null
                        },
                        onDrag = { change, dragAmount ->
                            touchLocationX += dragAmount.x
                            val sectionWidth = size.width / displayedCandlesCount
                            val idx = (touchLocationX / sectionWidth).toInt().coerceIn(0, displayedCandlesCount - 1)
                            activeCrosshairsIdx = idx
                        }
                    )
                }
                .pointerInput(visibleData) {
                    detectTapGestures(
                        onPress = { offset ->
                            val sectionWidth = size.width / displayedCandlesCount
                            val idx = (offset.x / sectionWidth).toInt().coerceIn(0, displayedCandlesCount - 1)
                            activeCrosshairsIdx = idx
                            tryAwaitRelease()
                            activeCrosshairsIdx = null
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height

            // Calculate min and max price inside the visible chunk to scale beautifully
            var maxPrice = visibleData.maxOf { it.candle.highPrice }
            var minPrice = visibleData.minOf { it.candle.lowPrice }

            if (showBollinger) {
                maxPrice = max(maxPrice, visibleData.mapNotNull { it.bbandUpper }.maxOrNull() ?: maxPrice)
                minPrice = min(minPrice, visibleData.mapNotNull { it.bbandLower }.minOrNull() ?: minPrice)
            }

            maxPrice *= 1.01  // add padding margins
            minPrice *= 0.99
            val priceRange = maxPrice - minPrice

            val candleWidth = width / displayedCandlesCount
            val barSpacing = candleWidth * 0.15f

            // Draw grid guidelines
            val horizontalLines = 4
            for (lineIdx in 0..horizontalLines) {
                val lineY = (height / horizontalLines) * lineIdx
                val representedPrice = maxPrice - (priceRange / horizontalLines) * lineIdx
                drawLine(
                    color = DarkBorder.copy(alpha = 0.5f),
                    start = Offset(0f, lineY),
                    end = Offset(width, lineY),
                    strokeWidth = 1f
                )
            }

            // Draw candles
            visibleData.forEachIndexed { index, data ->
                val x = index * candleWidth
                val oY = (height - ((data.candle.openPrice - minPrice) / priceRange) * height).toFloat()
                val hY = (height - ((data.candle.highPrice - minPrice) / priceRange) * height).toFloat()
                val lY = (height - ((data.candle.lowPrice - minPrice) / priceRange) * height).toFloat()
                val cY = (height - ((data.candle.closePrice - minPrice) / priceRange) * height).toFloat()

                val isGreen = data.candle.closePrice >= data.candle.openPrice
                val candleColor = if (isGreen) ProfitGreen else LossRed

                // Wick
                drawLine(
                    color = candleColor,
                    start = Offset(x + candleWidth / 2f, hY),
                    end = Offset(x + candleWidth / 2f, lY),
                    strokeWidth = 2f
                )

                // Candle Body rect
                val bodyTop = min(oY, cY)
                val bodyBottom = max(oY, cY)
                val bodyHeight = max(bodyBottom - bodyTop, 2f)

                drawRect(
                    color = candleColor,
                    topLeft = Offset(x + barSpacing, bodyTop),
                    size = Size(candleWidth - barSpacing * 2f, bodyHeight)
                )

                // Optional lines overlay
                if (showMa20 && index > 0) {
                    val prevData = visibleData[index - 1]
                    if (data.sma20 != null && prevData.sma20 != null) {
                        val prevY = (height - ((prevData.sma20 - minPrice) / priceRange) * height).toFloat()
                        val currY = (height - ((data.sma20 - minPrice) / priceRange) * height).toFloat()
                        drawLine(
                            color = PrimaryNeon,
                            start = Offset((index - 1) * candleWidth + candleWidth / 2f, prevY),
                            end = Offset(x + candleWidth / 2f, currY),
                            strokeWidth = 3f
                        )
                    }
                }

                if (showMa50 && index > 0) {
                    val prevData = visibleData[index - 1]
                    if (data.sma50 != null && prevData.sma50 != null) {
                        val prevY = (height - ((prevData.sma50 - minPrice) / priceRange) * height).toFloat()
                        val currY = (height - ((data.sma50 - minPrice) / priceRange) * height).toFloat()
                        drawLine(
                            color = NeutralOrange,
                            start = Offset((index - 1) * candleWidth + candleWidth / 2f, prevY),
                            end = Offset(x + candleWidth / 2f, currY),
                            strokeWidth = 3f
                        )
                    }
                }

                // Bollinger bands shading/lines
                if (showBollinger && index > 0) {
                    val prevData = visibleData[index - 1]
                    if (data.bbandUpper != null && data.bbandLower != null &&
                        prevData.bbandUpper != null && prevData.bbandLower != null
                    ) {
                        val prevUY = (height - ((prevData.bbandUpper - minPrice) / priceRange) * height).toFloat()
                        val currUY = (height - ((data.bbandUpper - minPrice) / priceRange) * height).toFloat()
                        drawLine(
                            color = AccentCyan.copy(alpha = 0.6f),
                            start = Offset((index - 1) * candleWidth + candleWidth / 2f, prevUY),
                            end = Offset(x + candleWidth / 2f, currUY),
                            strokeWidth = 1.5f
                        )

                        val prevLY = (height - ((prevData.bbandLower - minPrice) / priceRange) * height).toFloat()
                        val currLY = (height - ((data.bbandLower - minPrice) / priceRange) * height).toFloat()
                        drawLine(
                            color = AccentCyan.copy(alpha = 0.6f),
                            start = Offset((index - 1) * candleWidth + candleWidth / 2f, prevLY),
                            end = Offset(x + candleWidth / 2f, currLY),
                            strokeWidth = 1.5f
                        )
                    }
                }
            }

            // Tomorrow's Prediction overlay dot & outline circle
            if (predictionPricetomorrow != null) {
                val predY = (height - ((predictionPricetomorrow - minPrice) / priceRange) * height).toFloat()
                drawCircle(
                    color = AccentCyan,
                    radius = 8f,
                    center = Offset(width - candleWidth / 2f, predY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 12f,
                    center = Offset(width - candleWidth / 2f, predY),
                    style = Stroke(2f)
                )
            }

            // Crosshair overlay lines rendering
            activeCrosshairsIdx?.let { idx ->
                val lineX = idx * candleWidth + candleWidth / 2f
                // Vertical crosshair
                drawLine(
                    color = TextSecondary.copy(alpha = 0.7f),
                    start = Offset(lineX, 0f),
                    end = Offset(lineX, height),
                    strokeWidth = 2f
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Technical Sub panels plot (Volume, RSI, MACD details)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val barWidth = width / displayedCandlesCount

                when (activeSubPanel) {
                    "VOLUME" -> {
                        val maxVolume = visibleData.maxOf { it.candle.volume }.coerceAtLeast(1)
                        val spacing = barWidth * 0.15f

                        visibleData.forEachIndexed { idx, data ->
                            val isGreen = data.candle.closePrice >= data.candle.openPrice
                            val barColor = if (isGreen) ProfitGreen.copy(alpha = 0.45f) else LossRed.copy(alpha = 0.45f)
                            val barHeight = ((data.candle.volume.toDouble() / maxVolume) * height).toFloat()
                            val topLeftY = height - barHeight

                            drawRect(
                                color = barColor,
                                topLeft = Offset(idx * barWidth + spacing, topLeftY),
                                size = Size(barWidth - spacing * 2f, barHeight)
                            )
                        }
                    }

                    "RSI" -> {
                        // Drawing Guidelines for RSI
                        val y30 = height - (30f / 100f) * height
                        val y70 = height - (70f / 100f) * height
                        drawLine(color = LossRed.copy(alpha = 0.4f), start = Offset(0f, y70), end = Offset(width, y70), strokeWidth = 2f)
                        drawLine(color = ProfitGreen.copy(alpha = 0.4f), start = Offset(0f, y30), end = Offset(width, y30), strokeWidth = 2f)

                        // Relative Strength Index trace line
                        val path = Path()
                        var first = true

                        visibleData.forEachIndexed { idx, data ->
                            val rsiVal = data.rsi14
                            if (rsiVal != null) {
                                val pointX = idx * barWidth + barWidth / 2f
                                val pointY = (height - (rsiVal / 100f) * height).toFloat()
                                if (first) {
                                    path.moveTo(pointX, pointY)
                                    first = false
                                } else {
                                    path.lineTo(pointX, pointY)
                                }
                            }
                        }

                        drawPath(
                            path = path,
                            color = AccentCyan,
                            style = Stroke(width = 3.5f)
                        )
                    }

                    "MACD" -> {
                        // Graph MACD Line, Signal Line, and Histograms
                        val nonNullMacd = visibleData.mapNotNull { it.macd }
                        val nonNullSignal = visibleData.mapNotNull { it.signal }
                        val nonNullHist = visibleData.mapNotNull { it.hist }

                        val maxVal = maxOf(
                            nonNullMacd.maxOrNull()?.absoluteValue ?: 1.0,
                            maxOf(
                                nonNullSignal.maxOrNull()?.absoluteValue ?: 1.0,
                                nonNullHist.maxOrNull()?.absoluteValue ?: 1.0
                            )
                        )
                        val scale = height / (maxVal * 2.0) // center on half height

                        // Zero line
                        val centerY = height / 2f
                        drawLine(color = DarkBorder, start = Offset(0f, centerY), end = Offset(width, centerY), strokeWidth = 1f)

                        // 1. Histograms
                        visibleData.forEachIndexed { idx, data ->
                            val hist = data.hist
                            if (hist != null) {
                                val barHeight = (hist * scale).toFloat()
                                val barColor = if (hist >= 0) ProfitGreen.copy(alpha = 0.6f) else LossRed.copy(alpha = 0.6f)
                                val spacing = barWidth * 0.2f

                                drawRect(
                                    color = barColor,
                                    topLeft = Offset(
                                        idx * barWidth + spacing,
                                        if (hist >= 0) centerY - barHeight else centerY
                                    ),
                                    size = Size(barWidth - spacing * 2f, barHeight.absoluteValue)
                                )
                            }
                        }

                        // 2. MACD (Blue) and Signal (Amber) lines
                        val pathMacd = Path()
                        val pathSignal = Path()
                        var firstMacd = true
                        var firstSignal = true

                        visibleData.forEachIndexed { idx, data ->
                            val mac = data.macd
                            val sig = data.signal
                            val pointX = idx * barWidth + barWidth / 2f

                            if (mac != null) {
                                val pointY = centerY - (mac * scale).toFloat()
                                if (firstMacd) {
                                    pathMacd.moveTo(pointX, pointY)
                                    firstMacd = false
                                } else {
                                    pathMacd.lineTo(pointX, pointY)
                                }
                            }

                            if (sig != null) {
                                val pointY = centerY - (sig * scale).toFloat()
                                if (firstSignal) {
                                    pathSignal.moveTo(pointX, pointY)
                                    firstSignal = false
                                } else {
                                    pathSignal.lineTo(pointX, pointY)
                                }
                            }
                        }

                        drawPath(path = pathMacd, color = PrimaryNeon, style = Stroke(width = 3f))
                        drawPath(path = pathSignal, color = NeutralOrange, style = Stroke(width = 3f))
                    }
                }

                // Vertical Crosshair on sub-panel
                activeCrosshairsIdx?.let { idx ->
                    val lineX = idx * barWidth + barWidth / 2f
                    drawLine(
                        color = TextSecondary.copy(alpha = 0.7f),
                        start = Offset(lineX, 0f),
                        end = Offset(lineX, height),
                        strokeWidth = 2f
                    )
                }
            }
        }
    }
}
