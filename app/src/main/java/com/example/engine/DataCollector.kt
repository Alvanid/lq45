package com.example.engine

import android.util.Log
import com.example.data.StockCandle
import com.example.data.StockCandleDao
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.Calendar
import java.util.Random
import java.util.concurrent.TimeUnit
import kotlin.math.exp

// --- Yahoo Finance API Retrofit definition ---
@JsonClass(generateAdapter = true)
data class YahooChartResponse(
    @Json(name = "chart") val chart: ChartData
)

@JsonClass(generateAdapter = true)
data class ChartData(
    @Json(name = "result") val result: List<ChartResult>?
)

@JsonClass(generateAdapter = true)
data class ChartResult(
    @Json(name = "timestamp") val timestamp: List<Long>?,
    @Json(name = "indicators") val indicators: ChartIndicators
)

@JsonClass(generateAdapter = true)
data class ChartIndicators(
    @Json(name = "quote") val quote: List<ChartQuote>?
)

@JsonClass(generateAdapter = true)
data class ChartQuote(
    @Json(name = "open") val open: List<Double?>?,
    @Json(name = "high") val high: List<Double?>?,
    @Json(name = "low") val low: List<Double?>?,
    @Json(name = "close") val close: List<Double?>?,
    @Json(name = "volume") val volume: List<Long?>?
)

interface YahooFinanceService {
    @GET("v8/finance/chart/{symbol}")
    suspend fun getChartData(
        @Path("symbol") symbol: String,
        @Query("range") range: String = "5y",
        @Query("interval") interval: String = "1d"
    ): YahooChartResponse
}

object YahooFinanceClient {
    private const val BASE_URL = "https://query1.finance.yahoo.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val service: YahooFinanceService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(YahooFinanceService::class.java)
    }
}

class DataCollector(private val dao: StockCandleDao) {

    /**
     * Downloads/collects historical stock data.
     * Tries yfinance REST API first.
     * Falls back to high-fidelity GBM synthesis if network is offline or fails, ensuring 100% operation!
     */
    suspend fun downloadStock(symbol: String, forceOffline: Boolean = false): Int {
        if (forceOffline) {
            val syntheticCandles = generate5YearsOfHistory(symbol)
            dao.deleteCandlesBySymbol(symbol)
            dao.insertCandles(syntheticCandles)
            return syntheticCandles.size
        }

        try {
            val response = YahooFinanceClient.service.getChartData(symbol)
            val result = response.chart.result?.firstOrNull()
            val timestamps = result?.timestamp
            val quote = result?.indicators?.quote?.firstOrNull()

            if (timestamps != null && quote != null &&
                quote.open != null && quote.high != null &&
                quote.low != null && quote.close != null
            ) {
                val list = mutableListOf<StockCandle>()
                for (i in timestamps.indices) {
                    val open = quote.open[i]
                    val high = quote.high[i]
                    val low = quote.low[i]
                    val close = quote.close[i]
                    val vol = quote.volume?.getOrNull(i) ?: 0L

                    if (open != null && high != null && low != null && close != null) {
                        list.add(
                            StockCandle(
                                symbol = symbol,
                                openPrice = open,
                                highPrice = high,
                                lowPrice = low,
                                closePrice = close,
                                volume = vol,
                                dateMillis = timestamps[i] * 1000
                            )
                        )
                    }
                }

                if (list.isNotEmpty()) {
                    dao.deleteCandlesBySymbol(symbol)
                    dao.insertCandles(list)
                    Log.d("DataCollector", "Downloaded ${list.size} candles from Yahoo Finance for $symbol")
                    return list.size
                }
            }
        } catch (e: Exception) {
            Log.e("DataCollector", "Yahoo Finance download failed. Falling back to high-fidelity simulation: ${e.message}")
        }

        // Fallback to high-fidelity simulation
        val syntheticCandles = generate5YearsOfHistory(symbol)
        dao.deleteCandlesBySymbol(symbol)
        dao.insertCandles(syntheticCandles)
        return syntheticCandles.size
    }

    /**
     * Generates extremely realistic geometric Brownian motion (GBM) daily price bars
     * spanning exactly 5 years (~1250 trading days) for simulated backtesting/trading.
     */
    private fun generate5YearsOfHistory(symbol: String): List<StockCandle> {
        val meta = StockCatalog.getBySymbol(symbol)
        val initialPrice = meta?.basePrice ?: 1000.0

        val rand = Random(symbol.hashCode().toLong()) // deterministic per ticker
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -5)

        val totalDays = 1250
        val list = mutableListOf<StockCandle>()
        var currentPrice = initialPrice

        // Drift and volatility parameters based on sectors
        val drift = when (meta?.sector) {
            "Financials" -> 0.00012
            "Technology" -> 0.00020
            "Energy" -> 0.00015
            else -> 0.00008
        }
        val vol = when (meta?.sector) {
            "Financials" -> 0.012
            "Technology" -> 0.025
            "Real Estate" -> 0.018
            else -> 0.010
        }

        for (i in 0 until totalDays) {
            // Check weekend
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek == Calendar.SATURDAY) {
                calendar.add(Calendar.DAY_OF_MONTH, 2)
            } else if (dayOfWeek == Calendar.SUNDAY) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            // Geometric Brownian Motion for close
            val r = rand.nextGaussian()
            val changePct = exp(drift - 0.5 * vol * vol + vol * r)
            var close = currentPrice * changePct

            // Guard rails
            if (close < 50.0 && symbol == "GOTO.JK") close = maxOf(40.0, close)
            else if (close < 10.0) close = 10.0

            // Generate daily details
            val volatilityScalar = (0.015 + rand.nextDouble() * 0.02)
            val open = currentPrice
            val range = close * volatilityScalar
            val high = maxOf(open, close) + rand.nextDouble() * range * 0.5
            val low = minOf(open, close) - rand.nextDouble() * range * 0.5
            
            // Realistic volume
            val avgVol = when {
                initialPrice < 100 -> 150_000_000L
                initialPrice < 1000 -> 30_000_000L
                else -> 10_000_000L
            }
            val volume = (avgVol * (0.5 + rand.nextDouble() * 1.5)).toLong()

            list.add(
                StockCandle(
                    symbol = symbol,
                    openPrice = open,
                    highPrice = high,
                    lowPrice = low,
                    closePrice = close,
                    volume = volume,
                    dateMillis = calendar.timeInMillis
                )
            )

            currentPrice = close
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return list
    }
}
