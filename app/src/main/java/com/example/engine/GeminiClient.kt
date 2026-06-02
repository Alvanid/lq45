package com.example.engine

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

interface GeminiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: GeminiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiService::class.java)
    }
}

object GeminiAdvisor {

    /**
     * Calls Gemini-3.5-flash model to get Indonesian Stock Analysis
     */
    suspend fun analyzeStockWithAI(
        symbol: String,
        fullName: String,
        currentPrice: Double,
        pred1Day: Double,
        pred7Days: Double,
        pred30Days: Double,
        rsi: Double,
        macd: Double,
        signal: Double,
        trend: String,
        accuracyR2: Double
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("GeminiAdvisor", "Gemini API Key is placeholder. Generating offline local AI summary instead.")
            return generateOfflineAISummary(symbol, fullName, currentPrice, pred1Day, pred7Days, rsi, trend, accuracyR2)
        }

        val prompt = """
            Anda adalah Analis Kuantitatif, Analis Teknikal Saham senior, dan Peneliti Keuangan di Bursa Efek Indonesia.
            Berikan analisis mendalam profesional dalam BAHASA INDONESIA untuk saham $fullName ($symbol).
            
            Statistik Saat Ini:
            - Harga Saat Ini: Rp ${String.format("%,.2f", currentPrice)}
            - Proyeksi Hari Esok (Model ANN): Rp ${String.format("%,.2f", pred1Day)}
            - Proyeksi 7 Hari Mendatang: Rp ${String.format("%,.2f", pred7Days)}
            - Proyeksi 30 Hari Mendatang: Rp ${String.format("%,.2f", pred30Days)}
            - Indikator RSI (14): ${String.format("%.2f", rsi)}
            - MACD Line: ${String.format("%.4f", macd)}, Signal Line: ${String.format("%.4f", signal)}
            - Tren Prediksi ANN: $trend
            - Akurasi R² Model: ${String.format("%.2f%%", accuracyR2 * 100.0)}
            
            Buat laporan ringkas dan terstruktur (sekitar 3-4 paragraf) dengan poin-poin tebalkan berpola:
            1. **Evaluasi Sentimen Pasar & Teknikal**: Analisis RSI, MACD, dan level harga terhadap rata-rata pergerakan tren.
            2. **Ulasan Proyeksi Jaringan Saraf Tiruan (ANN)**: Berikan pandangan apakah tren Bullish/Bearish yang diproyeksikan logis sesuai dinamika sektor bisnisnya.
            3. **Rekomendasi Strategis (Strong Buy, Buy, Hold, Sell, Strong Sell)**: Berikan saran aksi beli/jual bertingkat, area level masuk (Entry Level), area batas kerugian (Stop Loss), dan target harga realistis (Take Profit). Incorporate risk assessment berdasarkan akurasi model.
            
            Tulis dengan gaya bahasa Bloomberg Terminal, Stockbit Pro, profesional, ilmiah, elegan, lugas, tanpa mengada-ada.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = "Anda adalah asisten analisis keuangan saham pro di Bursa Efek Indonesia."))
            )
        )

        return try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            val generatedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!generatedText.isNullOrBlank()) {
                generatedText
            } else {
                "Gagal menerima respons AI. Mohon cek kuota API."
            }
        } catch (e: Exception) {
            Log.e("GeminiAdvisor", "Gemini API call failed: ${e.message}")
            generateOfflineAISummary(symbol, fullName, currentPrice, pred1Day, pred7Days, rsi, trend, accuracyR2)
        }
    }

    private fun generateOfflineAISummary(
        symbol: String,
        fullName: String,
        currentPrice: Double,
        pred1Day: Double,
        pred7Days: Double,
        rsi: Double,
        trend: String,
        accuracyR2: Double
    ): String {
        val changePct = ((pred1Day - currentPrice) / currentPrice) * 100.0
        val rsiZone = when {
            rsi < 30.0 -> "Oversold (Jenuh Jual)"
            rsi > 70.0 -> "Overbought (Jenuh Beli)"
            else -> "Netral Konsolidatif"
        }

        return """
            ⚠️ **Mode Lokal Aktif (Kunci API Gemini tidak terkonfigurasi di AI Studio Secrets)**
            
            Berikut ringkasan analisis keuangan kuantitatif otomatis untuk **$fullName ($symbol)**:
            
            1. **Kondisi Teknikal Pasar**:
               Saham saat ini diperdagangkan di tingkat Rp ${String.format("%,.2f", currentPrice)} dengan indikator RSI ${String.format("%.2f", rsi)} menunjukkan zona **$rsiZone**. Momentum pergerakan mengindikasikan status pasar yang relatif stabil.
               
            2. **Prakiraan Jaringan Saraf Tiruan (ANN)**:
               Berdasarkan sliding window 60 hari terakhir, model Backpropagation ANN memproyeksikan pergerakan berskala **$trend** dengan target estimasi besok sebesar Rp ${String.format("%,.2f", pred1Day)} (${String.format("%+.2f%%", changePct)}). Model ini dilatih pada SQLite lokal dengan tingkat R² akurasi historis sekitar ${String.format("%.2f%%", accuracyR2 * 100.0)}.
               
            3. **Rekomendasi Aksi**:
               • **Rekomendasi**: ${if (changePct > 3.0 && rsi < 55) "ACCUMULATE BUY" else if (changePct < -3.0 || rsi > 70) "TAKE PROFIT / SELL" else "HOLD / WAIT & SEE"}
               • **Entry Zone**: Rp ${String.format("%,.0f", currentPrice * 0.98)} - Rp ${String.format("%,.0f", currentPrice)}
               • **Target Take Profit**: Rp ${String.format("%,.0f", pred7Days)} (Proyeksi 1 Minggu)
               • **Batas Stop Loss**: Rp ${String.format("%,.0f", currentPrice * 0.94)} (Resiko Maksimal 6%)
        """.trimIndent()
    }
}
