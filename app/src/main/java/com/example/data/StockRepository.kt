package com.example.data

import android.content.Context
import android.util.Log
import com.example.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.sqrt

class StockRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val candleDao = db.stockCandleDao()
    private val watchlistDao = db.watchlistDao()
    private val portfolioDao = db.portfolioDao()
    private val performanceDao = db.modelPerformanceDao()
    private val predictionDao = db.predictionDao()
    private val userDao = db.userDao()

    val allSymbols: Flow<List<String>> = candleDao.getAllSymbolsFlow()
    val watchlist: Flow<List<WatchlistItem>> = watchlistDao.getWatchlistFlow()
    val portfolioTransactions: Flow<List<PortfolioTransaction>> = portfolioDao.getTransactionsFlow()
    val allPerformances: Flow<List<ModelPerformance>> = performanceDao.getAllPerformancesFlow()
    val allPredictions: Flow<List<Prediction>> = predictionDao.getAllPredictionsFlow()

    fun getCandles(symbol: String): Flow<List<StockCandle>> = candleDao.getCandlesFlow(symbol)
    fun isWatchlisted(symbol: String): Flow<Boolean> = watchlistDao.isInWatchlistFlow(symbol)
    fun getModelPerformance(symbol: String): Flow<ModelPerformance?> = performanceDao.getPerformanceFlow(symbol)
    fun getPrediction(symbol: String): Flow<Prediction?> = predictionDao.getPredictionFlow(symbol)

    // Ensure we have some default stocks loaded so the app looks professional on first boot
    suspend fun checkAndPrepopulate() = withContext(Dispatchers.IO) {
        val count = candleDao.getTotalCandleCount()
        if (count == 0 || candleDao.getUniqueSymbolCount() < 5) {
            Log.d("StockRepository", "Database empty. Initializing default stock histories...")
            val collector = DataCollector(candleDao)
            // Pre-simulate top 5 index weights
            collector.downloadStock("BBCA.JK", forceOffline = true)
            collector.downloadStock("BBRI.JK", forceOffline = true)
            collector.downloadStock("BMRI.JK", forceOffline = true)
            collector.downloadStock("TLKM.JK", forceOffline = true)
            collector.downloadStock("GOTO.JK", forceOffline = true)

            // Auto-train their models so metrics are filled
            trainModelForSymbol("BBCA.JK")
            trainModelForSymbol("BBRI.JK")
            trainModelForSymbol("BMRI.JK")
            trainModelForSymbol("TLKM.JK")
            trainModelForSymbol("GOTO.JK")
        }

        // Add admin user if none exists
        if (userDao.getUserCount() == 0) {
            userDao.insertUser(User(username = "admin", passwordHash = "admin123", role = "Admin"))
            userDao.insertUser(User(username = "investor", passwordHash = "investor123", role = "Investor"))
        }
    }

    suspend fun lookupUser(username: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByUsername(username)
    }

    suspend fun registerUser(username: String, role: String): Boolean = withContext(Dispatchers.IO) {
        val existing = userDao.getUserByUsername(username)
        if (existing != null) return@withContext false
        userDao.insertUser(User(username = username, passwordHash = "123456", role = role))
        true
    }

    suspend fun syncStockData(symbol: String): Int = withContext(Dispatchers.IO) {
        val collector = DataCollector(candleDao)
        val count = collector.downloadStock(symbol)
        if (count > 0) {
            trainModelForSymbol(symbol)
        }
        count
    }

    suspend fun addToWatchlist(symbol: String) {
        watchlistDao.addToWatchlist(WatchlistItem(symbol))
    }

    suspend fun removeFromWatchlist(symbol: String) {
        watchlistDao.removeFromWatchlist(WatchlistItem(symbol))
    }

    suspend fun addTransaction(symbol: String, buyPrice: Double, lots: Int) {
        portfolioDao.addTransaction(PortfolioTransaction(symbol = symbol, buyPrice = buyPrice, lots = lots))
    }

    suspend fun removeTransaction(transaction: PortfolioTransaction) {
        portfolioDao.deleteTransaction(transaction)
    }

    suspend fun clearPortfolio() {
        portfolioDao.clearPortfolio()
    }

    /**
     * Train Model: Sliding Window, MinMax normalization, 3-layer Neural Network fit, statistics generation, and forecast saving.
     */
    suspend fun trainModelForSymbol(symbol: String, epochs: Int = 120): PerformanceMetrics = withContext(Dispatchers.IO) {
        val candles = candleDao.getCandlesList(symbol)
        if (candles.size < 70) {
            throw IllegalArgumentException("Data terlalu singkat (Min. 70 records) untuk melatih model.")
        }

        val closePrices = candles.map { it.closePrice }

        // Data cleaning: missing values and outlier check (Z-scores)
        val cleanedPrice = cleanAndFilterPrices(closePrices)

        // Fit MinMaxScaler
        val scaler = MinMaxScaler()
        scaler.fit(cleanedPrice)
        val scaledPrices = scaler.transformList(cleanedPrice)

        // Construct 60-day sliding windows
        val windowSize = 60
        val inputs = mutableListOf<DoubleArray>()
        val targets = mutableListOf<Double>()

        for (i in 0..(scaledPrices.size - windowSize - 1)) {
            val window = DoubleArray(windowSize)
            for (w in 0 until windowSize) {
                window[w] = scaledPrices[i + w]
            }
            inputs.add(window)
            targets.add(scaledPrices[i + windowSize])
        }

        // Initialize and train Backpropagation Neural Network
        val ann = BackpropNeuralNetwork()
        ann.train(inputs, targets, epochs = epochs, learningRate = 0.05)

        // Evaluate model on all samples to generate metrics
        val predictionsScaled = inputs.map { ann.predict(it) }
        val predictionsActual = predictionsScaled.map { scaler.inverseTransform(it) }
        val actualTargets = targets.map { scaler.inverseTransform(it) }

        val metrics = BackpropNeuralNetwork.evaluate(actualTargets, predictionsActual)

        // Save model performance summary
        performanceDao.insertPerformance(
            ModelPerformance(
                symbol = symbol,
                mse = metrics.mse,
                rmse = metrics.rmse,
                mae = metrics.mae,
                mape = metrics.mape,
                r2 = metrics.r2
            )
        )

        // Forecast steps: Day +1, Day +7, Day +30, Day +90
        val lastPrices = scaledPrices.takeLast(60)
        val currentPrice = candles.last().closePrice

        // Day +1 prediction
        val tomorrowInput = lastPrices.toDoubleArray()
        val tomorrowPredScaled = ann.predict(tomorrowInput)
        val tomorrowPred = scaler.inverseTransform(tomorrowPredScaled)

        // Day +7 (1 week) prediction
        val pricesBuffer = lastPrices.toMutableList()
        for (step in 1..7) {
            val stepInput = pricesBuffer.takeLast(60).toDoubleArray()
            val stepPred = ann.predict(stepInput)
            pricesBuffer.add(stepPred)
        }
        val pred7 = scaler.inverseTransform(pricesBuffer.last())

        // Day +30 (1 month) prediction
        pricesBuffer.clear()
        pricesBuffer.addAll(lastPrices)
        for (step in 1..30) {
            val stepInput = pricesBuffer.takeLast(60).toDoubleArray()
            val stepPred = ann.predict(stepInput)
            pricesBuffer.add(stepPred)
        }
        val pred30 = scaler.inverseTransform(pricesBuffer.last())

        // Day +90 (3 months) prediction
        pricesBuffer.clear()
        pricesBuffer.addAll(lastPrices)
        for (step in 1..90) {
            val stepInput = pricesBuffer.takeLast(60).toDoubleArray()
            val stepPred = ann.predict(stepInput)
            pricesBuffer.add(stepPred)
        }
        val pred90 = scaler.inverseTransform(pricesBuffer.last())

        // Trend Determination
        val priceDiff = tomorrowPred - currentPrice
        val trend = when {
            priceDiff > (currentPrice * 0.015) -> "Bullish"
            priceDiff < -(currentPrice * 0.015) -> "Bearish"
            else -> "Sideways"
        }
        val hex = when (trend) {
            "Bullish" -> "#00E676"
            "Bearish" -> "#FF1744"
            else -> "#2979FF"
        }

        // Save predictions summary
        predictionDao.insertPrediction(
            Prediction(
                symbol = symbol,
                currentPrice = currentPrice,
                pred1Day = tomorrowPred,
                pred7Days = pred7,
                pred30Days = pred30,
                pred90Days = pred90,
                confidenceScore = metrics.r2.coerceIn(0.01, 0.99),
                trend = trend,
                forecastTrendHex = hex
            )
        )

        metrics
    }

    private fun cleanAndFilterPrices(prices: List<Double>): List<Double> {
        if (prices.size < 5) return prices
        // Replace zeroes or negative values
        val cleaned = prices.map { if (it <= 0) prices.average() else it }.toMutableList()

        // 1. Missing values forward fill
        for (i in cleaned.indices) {
            if (cleaned[i].isNaN()) {
                cleaned[i] = if (i > 0) cleaned[i - 1] else prices.average()
            }
        }

        // 2. Simple outlier capping (beyond 3.5 standard deviations from median)
        val average = cleaned.average()
        val sumSq = cleaned.map { (it - average) * (it - average) }.sum()
        val stdDev = sqrt(sumSq / cleaned.size)
        
        if (stdDev > 0) {
            for (i in cleaned.indices) {
                val zScore = abs(cleaned[i] - average) / stdDev
                if (zScore > 3.5) {
                    cleaned[i] = average + 3.5 * stdDev * (if (cleaned[i] > average) 1 else -1)
                }
            }
        }

        return cleaned
    }
}
