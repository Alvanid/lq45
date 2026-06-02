package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}

@Dao
interface StockCandleDao {
    @Query("SELECT DISTINCT symbol FROM stock_candles")
    fun getAllSymbolsFlow(): Flow<List<String>>

    @Query("SELECT * FROM stock_candles WHERE symbol = :symbol ORDER BY dateMillis ASC")
    fun getCandlesFlow(symbol: String): Flow<List<StockCandle>>

    @Query("SELECT * FROM stock_candles WHERE symbol = :symbol ORDER BY dateMillis ASC")
    suspend fun getCandlesList(symbol: String): List<StockCandle>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandles(candles: List<StockCandle>)

    @Query("DELETE FROM stock_candles WHERE symbol = :symbol")
    suspend fun deleteCandlesBySymbol(symbol: String)

    @Query("SELECT COUNT(*) FROM stock_candles")
    suspend fun getTotalCandleCount(): Int

    @Query("SELECT COUNT(DISTINCT symbol) FROM stock_candles")
    suspend fun getUniqueSymbolCount(): Int
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getWatchlistFlow(): Flow<List<WatchlistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: WatchlistItem)

    @Delete
    suspend fun removeFromWatchlist(item: WatchlistItem)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE symbol = :symbol)")
    fun isInWatchlistFlow(symbol: String): Flow<Boolean>
}

@Dao
interface PortfolioDao {
    @Query("SELECT * FROM portfolio_transactions ORDER BY purchaseDate DESC")
    fun getTransactionsFlow(): Flow<List<PortfolioTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTransaction(transaction: PortfolioTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: PortfolioTransaction)

    @Query("DELETE FROM portfolio_transactions")
    suspend fun clearPortfolio()
}

@Dao
interface ModelPerformanceDao {
    @Query("SELECT * FROM model_performances WHERE symbol = :symbol LIMIT 1")
    fun getPerformanceFlow(symbol: String): Flow<ModelPerformance?>

    @Query("SELECT * FROM model_performances WHERE symbol = :symbol LIMIT 1")
    suspend fun getPerformance(symbol: String): ModelPerformance?

    @Query("SELECT * FROM model_performances")
    fun getAllPerformancesFlow(): Flow<List<ModelPerformance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerformance(performance: ModelPerformance)
}

@Dao
interface PredictionDao {
    @Query("SELECT * FROM predictions WHERE symbol = :symbol LIMIT 1")
    fun getPredictionFlow(symbol: String): Flow<Prediction?>

    @Query("SELECT * FROM predictions WHERE symbol = :symbol LIMIT 1")
    suspend fun getPrediction(symbol: String): Prediction?

    @Query("SELECT * FROM predictions")
    fun getAllPredictionsFlow(): Flow<List<Prediction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: Prediction)
}
