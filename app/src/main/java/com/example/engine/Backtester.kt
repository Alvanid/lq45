package com.example.engine

import kotlin.math.abs
import kotlin.math.sqrt

data class BacktestMetrics(
    val initialBalance: Double,
    val finalBalance: Double,
    val totalProfit: Double,
    val returnPct: Double,
    val winRate: Double, // percentage, e.g. 62.5
    val profitFactor: Double,
    val sharpeRatio: Double,
    val maxDrawdown: Double, // percentage, e.g. 12.4%
    val totalTrades: Int,
    val listTrades: List<TradeLog>
)

data class TradeLog(
    val type: String, // "BUY" or "SELL"
    val dateMillis: Long,
    val price: Double,
    val shares: Int,
    val profitEarned: Double?, // populated on SELL
    val returnPct: Double?    // populated on SELL
)

object Backtester {

    /**
     * Simulates trading a single stock based on automatic technical indicators:
     * BUY: RSI < 30 OR (MACD crossovers above Signal Line)
     * SELL: RSI > 70 OR (MACD crossovers below Signal Line)
     */
    fun runBacktest(
        indicators: List<TechnicalIndicators>,
        initialBalance: Double = 100_000_000.0
    ): BacktestMetrics {
        if (indicators.size < 60) {
            return BacktestMetrics(initialBalance, initialBalance, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, emptyList())
        }

        var balance = initialBalance
        var shares = 0
        var buyPrice = 0.0
        var buyDateMillis = 0L

        val trades = mutableListOf<TradeLog>()
        val portfolioHistory = mutableListOf<Double>()
        
        var totalGain = 0.0
        var totalLoss = 0.0
        var winningTrades = 0
        var losingTrades = 0

        // Skip first 50 entries to allow indicator stabilizer
        for (i in 50 until indicators.size) {
            val current = indicators[i]
            val prev = indicators[i - 1]
            val price = current.candle.closePrice

            val rsi = current.rsi14 ?: 50.0
            val macd = current.macd ?: 0.0
            val signal = current.signal ?: 0.0
            val prevMacd = prev.macd ?: 0.0
            val prevSignal = prev.signal ?: 0.0

            val macdCrossUp = prevMacd <= prevSignal && macd > signal
            val macdCrossDown = prevMacd >= prevSignal && macd < signal

            // BUY Signal
            if (shares == 0) {
                if (rsi < 30 || macdCrossUp) {
                    // Buy maximum possible shares (integer rounding of 100-share lots or individual shares)
                    // Let's trade standard individual shares for simplicity in backtester
                    val sharesToBuy = (balance / price).toInt()
                    if (sharesToBuy > 0) {
                        shares = sharesToBuy
                        buyPrice = price
                        buyDateMillis = current.candle.dateMillis
                        balance -= shares * price
                        trades.add(TradeLog("BUY", buyDateMillis, price, shares, null, null))
                    }
                }
            }
            // SELL Signal
            else {
                if (rsi > 70 || macdCrossDown || i == indicators.size - 1) { // force exit at end
                    val revenue = shares * price
                    val profit = revenue - (shares * buyPrice)
                    val rPct = (profit / (shares * buyPrice)) * 100.0

                    balance += revenue
                    
                    if (profit >= 0) {
                        totalGain += profit
                        winningTrades++
                    } else {
                        totalLoss += abs(profit)
                        losingTrades++
                    }

                    trades.add(TradeLog("SELL", current.candle.dateMillis, price, shares, profit, rPct))
                    shares = 0
                }
            }

            // Track portfolio equity curve
            val currentEquity = balance + (shares * price)
            portfolioHistory.add(currentEquity)
        }

        // Calculate final metrics
        val finalBalance = balance + (shares * (indicators.last().candle.closePrice))
        val totalProfit = finalBalance - initialBalance
        val returnPct = (totalProfit / initialBalance) * 100.0

        val totalSellTrades = winningTrades + losingTrades
        val winRate = if (totalSellTrades > 0) (winningTrades.toDouble() / totalSellTrades) * 100.0 else 0.0
        val profitFactor = if (totalLoss > 0.0) totalGain / totalLoss else if (totalGain > 0.0) 99.9 else 1.0

        // Sharpe Ratio logic: monthly/daily standards
        val returns = mutableListOf<Double>()
        for (k in 1 until portfolioHistory.size) {
            val r = (portfolioHistory[k] - portfolioHistory[k - 1]) / portfolioHistory[k - 1]
            returns.add(r)
        }
        val avgReturn = if (returns.isNotEmpty()) returns.average() else 0.0
        var variance = 0.0
        if (returns.isNotEmpty()) {
            val mean = avgReturn
            var sum = 0.0
            for (r in returns) {
                sum += (r - mean) * (r - mean)
            }
            variance = sum / returns.size
        }
        val stdDev = if (variance > 0.0) sqrt(variance) else 1e-6
        // Daily to Annual Sharpe Ratio adjustment (daily risk free component assumed 0)
        val sharpeRatio = if (stdDev > 1e-6) (avgReturn / stdDev) * sqrt(252.0) else 0.0

        // Max Drawdown percentage
        var maxEquity = initialBalance
        var maxDd = 0.0
        for (eq in portfolioHistory) {
            if (eq > maxEquity) {
                maxEquity = eq
            }
            val dd = (maxEquity - eq) / maxEquity * 100.0
            if (dd > maxDd) {
                maxDd = dd
            }
        }

        return BacktestMetrics(
            initialBalance = initialBalance,
            finalBalance = finalBalance,
            totalProfit = totalProfit,
            returnPct = returnPct,
            winRate = winRate,
            profitFactor = profitFactor,
            sharpeRatio = sharpeRatio,
            maxDrawdown = maxDd,
            totalTrades = trades.size,
            listTrades = trades
        )
    }
}
