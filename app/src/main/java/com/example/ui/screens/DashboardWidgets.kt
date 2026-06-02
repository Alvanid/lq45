package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FearAndGreedMeter(
    score: Int, // 0 to 100
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Fear & Greed Index", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val center = Offset(width / 2f, height)
                val radius = width / 2f - 10f

                // Semicircle gauge spectrum
                drawArc(
                    brush = Brush.horizontalGradient(
                        colors = listOf(LossRed, NeutralOrange, ProfitGreen)
                    ),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 12f, cap = StrokeCap.Round)
                )

                // Pointer needle calculation
                val angleRad = Math.toRadians(180.0 + (score.toDouble() / 100.0) * 180.0)
                val needleLength = radius - 15f
                val needleEnd = Offset(
                    (center.x + needleLength * cos(angleRad)).toFloat(),
                    (center.y + needleLength * sin(angleRad)).toFloat()
                )

                drawLine(
                    color = Color.White,
                    start = Offset(center.x, center.y - 2f),
                    end = needleEnd,
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )

                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = center
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = score.toString(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                val sentimentText = when {
                    score < 30 -> "EXTREME FEAR"
                    score < 45 -> "FEAR"
                    score < 55 -> "NEUTRAL"
                    score < 75 -> "GREED"
                    else -> "EXTREME GREED"
                }
                val sentimentColor = when {
                    score < 45 -> LossRed
                    score < 55 -> NeutralOrange
                    else -> ProfitGreen
                }
                Text(
                    text = sentimentText,
                    color = sentimentColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun SentimentGauge(
    bullishCount: Int,
    bearishCount: Int,
    modifier: Modifier = Modifier
) {
    val total = (bullishCount + bearishCount).coerceAtLeast(1)
    val bullPct = (bullishCount.toFloat() / total * 100f).toInt()
    val bearPct = 100 - bullPct

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text("Market Sentimen", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Bullish: $bullishCount", color = ProfitGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bearish: $bearishCount", color = LossRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.TrendingDown, contentDescription = null, tint = LossRed, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Segmented indicator bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
        ) {
            Box(
                modifier = Modifier
                    .weight(bullPct.coerceAtLeast(1).toFloat())
                    .fillMaxHeight()
                    .background(ProfitGreen)
            )
            Box(
                modifier = Modifier
                    .weight(bearPct.coerceAtLeast(1).toFloat())
                    .fillMaxHeight()
                    .background(LossRed)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$bullPct% Bullish", color = TextSecondary, fontSize = 10.sp)
            Text("$bearPct% Bearish", color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
fun SimpleTickerRibbon(
    symbols: List<String>,
    onSymbolSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(DarkSurface)
            .border(1.dp, DarkBorder)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        symbols.forEach { sym ->
            val cleanSym = sym.removeSuffix(".JK")
            Row(
                modifier = Modifier
                    .clickable { onSymbolSelected(sym) }
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(cleanSym, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (sym.contains("B")) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = if (sym.contains("B")) ProfitGreen else LossRed,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
