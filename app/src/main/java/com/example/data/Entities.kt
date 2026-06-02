package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val passwordHash: String, // SHA-256 string
    val role: String, // "Admin", "Investor", "Guest"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stock_candles")
data class StockCandle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String, // e.g., "BBCA.JK"
    val openPrice: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val closePrice: Double,
    val volume: Long,
    val dateMillis: Long // Unix epoch millis representing the day
)

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val symbol: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "portfolio_transactions")
data class PortfolioTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val buyPrice: Double,
    val lots: Int, // 1 lot = 100 shares
    val purchaseDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "model_performances")
data class ModelPerformance(
    @PrimaryKey val symbol: String, // PK to have 1 entry per stock
    val mse: Double,
    val rmse: Double,
    val mae: Double,
    val mape: Double,
    val r2: Double,
    val trainedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "predictions")
data class Prediction(
    @PrimaryKey val symbol: String,
    val currentPrice: Double,
    val pred1Day: Double,
    val pred7Days: Double,
    val pred30Days: Double,
    val pred90Days: Double,
    val confidenceScore: Double, // 0.0 to 1.0
    val trend: String, // "Bullish", "Bearish", "Sideways"
    val forecastTrendHex: String, // UI helper
    val createdAt: Long = System.currentTimeMillis()
)
