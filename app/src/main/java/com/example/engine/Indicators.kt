package com.example.engine

import com.example.data.StockCandle
import kotlin.math.sqrt

data class TechnicalIndicators(
    val candle: StockCandle,
    val sma20: Double?,
    val sma50: Double?,
    val ema20: Double?,
    val ema50: Double?,
    val rsi14: Double?,
    val macd: Double?,
    val signal: Double?,
    val hist: Double?,
    val bbandUpper: Double?,
    val bbandMiddle: Double?,
    val bbandLower: Double?,
    val atr: Double?
)

object IndicatorsCalculator {

    fun calculate(candles: List<StockCandle>): List<TechnicalIndicators> {
        val size = candles.size
        if (size == 0) return emptyList()

        val closes = candles.map { it.closePrice }
        val highs = candles.map { it.highPrice }
        val lows = candles.map { it.lowPrice }

        val sma20 = calculateSMA(closes, 20)
        val sma50 = calculateSMA(closes, 50)
        val ema20 = calculateEMA(closes, 20)
        val ema50 = calculateEMA(closes, 50)
        val rsi14 = calculateRSI(closes, 14)
        val (macdLine, signalLine, histLine) = calculateMACD(closes, 12, 26, 9)
        val (bbUpper, bbMiddle, bbLower) = calculateBollingerBands(closes, 20, 2.0)
        val atr = calculateATR(highs, lows, closes, 14)

        return candles.mapIndexed { idx, candle ->
            TechnicalIndicators(
                candle = candle,
                sma20 = sma20.getOrNull(idx),
                sma50 = sma50.getOrNull(idx),
                ema20 = ema20.getOrNull(idx),
                ema50 = ema50.getOrNull(idx),
                rsi14 = rsi14.getOrNull(idx),
                macd = macdLine.getOrNull(idx),
                signal = signalLine.getOrNull(idx),
                hist = histLine.getOrNull(idx),
                bbandUpper = bbUpper.getOrNull(idx),
                bbandMiddle = bbMiddle.getOrNull(idx),
                bbandLower = bbLower.getOrNull(idx),
                atr = atr.getOrNull(idx)
            )
        }
    }

    private fun calculateSMA(values: List<Double>, period: Int): List<Double?> {
        val result = arrayOfNulls<Double>(values.size)
        var sum = 0.0
        for (i in values.indices) {
            sum += values[i]
            if (i >= period) {
                sum -= values[i - period]
            }
            if (i >= period - 1) {
                result[i] = sum / period
            }
        }
        return result.toList()
    }

    private fun calculateEMA(values: List<Double>, period: Int): List<Double?> {
        val result = arrayOfNulls<Double>(values.size)
        if (values.isEmpty()) return result.toList()

        val k = 2.0 / (period + 1)
        var ema = values[0]
        result[0] = ema

        for (i in 1 until values.size) {
            ema = values[i] * k + ema * (1 - k)
            result[i] = ema
        }
        return result.toList()
    }

    private fun calculateRSI(values: List<Double>, period: Int): List<Double?> {
        val result = arrayOfNulls<Double>(values.size)
        if (values.size <= period) return result.toList()

        var avgGain = 0.0
        var avgLoss = 0.0

        // Inisialisasi RSI pertama
        for (i in 1..period) {
            val change = values[i] - values[i - 1]
            if (change > 0) {
                avgGain += change
            } else {
                avgLoss += -change
            }
        }
        avgGain /= period
        avgLoss /= period

        val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
        result[period] = 100.0 - (100.0 / (1.0 + rs))

        for (i in (period + 1) until values.size) {
            val change = values[i] - values[i - 1]
            var gain = 0.0
            var loss = 0.0
            if (change > 0) gain = change else loss = -change

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period

            val currentRs = if (avgLoss == 0.0) Double.MAX_VALUE else avgGain / avgLoss
            result[i] = 100.0 - (100.0 / (1.0 + currentRs))
        }
        return result.toList()
    }

    private fun calculateMACD(
        values: List<Double>,
        fastPeriod: Int,
        slowPeriod: Int,
        signalPeriod: Int
    ): Triple<List<Double?>, List<Double?>, List<Double?>> {
        val macdLine = arrayOfNulls<Double>(values.size)
        val signalLine = arrayOfNulls<Double>(values.size)
        val histLine = arrayOfNulls<Double>(values.size)

        val fastEma = calculateEMA(values, fastPeriod)
        val slowEma = calculateEMA(values, slowPeriod)

        for (i in values.indices) {
            val fast = fastEma[i]
            val slow = slowEma[i]
            if (fast != null && slow != null) {
                macdLine[i] = fast - slow
            }
        }

        // Hitung Signal Line (EMA dari MACD Line)
        val macdListNonNull = macdLine.map { it ?: 0.0 }
        val signalEma = calculateEMA(macdListNonNull, signalPeriod)

        for (i in values.indices) {
            if (macdLine[i] != null && i >= slowPeriod - 1) {
                signalLine[i] = signalEma[i]
                val sig = signalLine[i]
                val mac = macdLine[i]
                if (sig != null && mac != null) {
                    histLine[i] = mac - sig
                }
            }
        }

        return Triple(macdLine.toList(), signalLine.toList(), histLine.toList())
    }

    private fun calculateBollingerBands(
        values: List<Double>,
        period: Int,
        multiplier: Double
    ): Triple<List<Double?>, List<Double?>, List<Double?>> {
        val upper = arrayOfNulls<Double>(values.size)
        val middle = arrayOfNulls<Double>(values.size)
        val lower = arrayOfNulls<Double>(values.size)

        val sma = calculateSMA(values, period)

        for (i in values.indices) {
            val mid = sma[i]
            if (mid != null) {
                middle[i] = mid
                // Hitung std dev
                var sumSqDiff = 0.0
                for (j in (i - period + 1)..i) {
                    val diff = values[j] - mid
                    sumSqDiff += diff * diff
                }
                val stdDev = sqrt(sumSqDiff / period)
                upper[i] = mid + multiplier * stdDev
                lower[i] = mid - multiplier * stdDev
            }
        }

        return Triple(upper.toList(), middle.toList(), lower.toList())
    }

    private fun calculateATR(
        highs: List<Double>,
        lows: List<Double>,
        closes: List<Double>,
        period: Int
    ): List<Double?> {
        val atr = arrayOfNulls<Double>(closes.size)
        if (closes.size <= period) return atr.toList()

        val tr = arrayOfNulls<Double>(closes.size)
        tr[0] = highs[0] - lows[0]
        for (i in 1 until closes.size) {
            val hL = highs[i] - lows[i]
            val hC = kotlin.math.abs(highs[i] - closes[i - 1])
            val lC = kotlin.math.abs(lows[i] - closes[i - 1])
            tr[i] = maxOf(hL, maxOf(hC, lC))
        }

        var sumTr = 0.0
        for (i in 0 until period) {
            sumTr += tr[i] ?: 0.0
        }
        atr[period - 1] = sumTr / period

        for (i in period until closes.size) {
            val currentAtr = (atr[i - 1]!! * (period - 1) + (tr[i] ?: 0.0)) / period
            atr[i] = currentAtr
        }

        return atr.toList()
    }
}
