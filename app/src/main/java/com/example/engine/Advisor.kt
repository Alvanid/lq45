package com.example.engine

import kotlin.math.abs

data class AIRecommendation(
    val status: String, // "STRONG BUY", "BUY", "HOLD", "SELL", "STRONG SELL"
    val buyScore: Int, // 0 to 100
    val sellScore: Int, // 0 to 100
    val riskLevel: String, // "Low", "Medium", "High"
    val confidenceLevel: String, // "High", "Medium", "Low"
    val reason: String
)

object Advisor {

    fun generateRecommendation(
        currentPrice: Double,
        predPrice: Double, // tomorrow's prediction
        rsi: Double,
        macd: Double,
        signal: Double,
        sma20: Double?,
        sma50: Double?
    ): AIRecommendation {
        val priceDiffPct = ((predPrice - currentPrice) / currentPrice) * 100.0
        val isMacdBullish = macd > signal
        val isAboveSmas = (sma20 != null && currentPrice > sma20) && (sma50 != null && currentPrice > sma50)

        var buyScore = 30
        var sellScore = 30
        val reasonsList = mutableListOf<String>()

        // Analyze price momentum
        if (priceDiffPct > 5.0) {
            buyScore += 30
            reasonsList.add("Model neural network memproyeksikan kenaikan harga signifikan (+${String.format("%.2f", priceDiffPct)}%).")
        } else if (priceDiffPct > 1.5) {
            buyScore += 15
            reasonsList.add("Proyeksi model mengarah ke kenaikan moderat (+${String.format("%.2f", priceDiffPct)}%).")
        } else if (priceDiffPct < -5.0) {
            sellScore += 30
            reasonsList.add("Proyeksi model mendeteksi koreksi bearish tajam (${String.format("%.2f", priceDiffPct)}%).")
        } else if (priceDiffPct < -1.5) {
            sellScore += 15
            reasonsList.add("Tren jangka pendek diprediksi sedikit terkoreksi (${String.format("%.2f", priceDiffPct)}%).")
        } else {
            reasonsList.add("Prakiraan harga berada pada fase konsolidasi sideways (${String.format("%.2f", priceDiffPct)}%).")
        }

        // Analyze RSI
        if (rsi < 30.0) {
            buyScore += 30
            sellScore -= 20
            reasonsList.add("RSI ($rsi) berada di zona oversold (jenuh jual), indikasi teknikal pembalikan arah naik.")
        } else if (rsi > 70.0) {
            sellScore += 35
            buyScore -= 20
            reasonsList.add("RSI ($rsi) berada di zona overbought (jenuh beli), waspada resiko profit taking.")
        } else {
            reasonsList.add("RSI stabil di tingkat netral ($rsi).")
        }

        // Analyze MACD
        if (isMacdBullish) {
            buyScore += 15
            reasonsList.add("Kombinasi MACD mendeteksi momentum crossover bullish (MACD di atas Signal).")
        } else {
            sellScore += 15
            reasonsList.add("Kombinasi MACD mendeteksi pergerakan bearish (MACD di bawah Signal).")
        }

        // Analyze SMAs
        if (isAboveSmas) {
            buyScore += 10
            reasonsList.add("Saham berada dalam uptrend sehat di atas indikator rata-rata pergerakan SMA20 dan SMA50.")
        } else if (sma20 != null && currentPrice < sma20) {
            sellScore += 10
            reasonsList.add("Harga di bawah SMA20, mengindikasikan tekanan jual jangka pendek mendominasi.")
        }

        // Clip scores
        buyScore = buyScore.coerceIn(5, 95)
        sellScore = sellScore.coerceIn(5, 95)

        // Evaluate Status
        val status = when {
            buyScore >= 75 -> "STRONG BUY"
            buyScore >= 55 -> "BUY"
            sellScore >= 75 -> "STRONG SELL"
            sellScore >= 55 -> "SELL"
            else -> "HOLD"
        }

        // Confidence
        val confidenceLevel = when {
            buyScore >= 70 || sellScore >= 70 -> "High"
            buyScore >= 45 || sellScore >= 45 -> "Medium"
            else -> "Low"
        }

        // Risk Level calculation
        val riskLevel = when {
            rsi > 75.0 || rsi < 25.0 -> "High"
            abs(priceDiffPct) > 7.0 -> "High"
            abs(priceDiffPct) > 3.0 -> "Medium"
            else -> "Low"
        }

        val reasonText = reasonsList.joinToString("\n• ", prefix = "• ")

        return AIRecommendation(
            status = status,
            buyScore = buyScore,
            sellScore = sellScore,
            riskLevel = riskLevel,
            confidenceLevel = confidenceLevel,
            reason = reasonText
        )
    }
}
