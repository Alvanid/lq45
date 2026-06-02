package com.example.engine

data class StockMetadata(
    val symbol: String, // e.g. "BBCA.JK"
    val shortName: String, // e.g. "BBCA"
    val fullName: String, // e.g. "Bank Central Asia Tbk."
    val sector: String,
    val basePrice: Double // realistic starter price
)

object StockCatalog {
    val stocks = listOf(
        StockMetadata("BBCA.JK", "BBCA", "Bank Central Asia Tbk.", "Financials", 10250.0),
        StockMetadata("BBRI.JK", "BBRI", "Bank Rakyat Indonesia Tbk.", "Financials", 4350.0),
        StockMetadata("BMRI.JK", "BMRI", "Bank Mandiri (Persero) Tbk.", "Financials", 6850.0),
        StockMetadata("BBNI.JK", "BBNI", "Bank Negara Indonesia Tbk.", "Financials", 4800.0),
        StockMetadata("TLKM.JK", "TLKM", "Telkom Indonesia Tbk.", "Infrastructure", 3100.0),
        StockMetadata("ASII.JK", "ASII", "Astra International Tbk.", "Consumer Cyclical", 4600.0),
        StockMetadata("ICBP.JK", "ICBP", "Indofood CBP Sukses Makmur Tbk.", "Consumer Non-Cyclical", 11950.0),
        StockMetadata("INDF.JK", "INDF", "Indofood Sukses Makmur Tbk.", "Consumer Non-Cyclical", 6950.0),
        StockMetadata("UNVR.JK", "UNVR", "Unilever Indonesia Tbk.", "Consumer Non-Cyclical", 2300.0),
        StockMetadata("KLBF.JK", "KLBF", "Kalbe Farma Tbk.", "Healthcare", 1450.0),
        StockMetadata("CPIN.JK", "CPIN", "Charoen Pokphand Indonesia Tbk.", "Basic Materials", 4850.0),
        StockMetadata("ADRO.JK", "ADRO", "Adaro Energy Indonesia Tbk.", "Energy", 2950.0),
        StockMetadata("ANTM.JK", "ANTM", "Aneka Tambang Tbk.", "Basic Materials", 1300.0),
        StockMetadata("MDKA.JK", "MDKA", "Merdeka Copper Gold Tbk.", "Basic Materials", 2250.0),
        StockMetadata("PTBA.JK", "PTBA", "Bukit Asam Tbk.", "Energy", 2500.0),
        StockMetadata("ITMG.JK", "ITMG", "Indo Tambangraya Megah Tbk.", "Energy", 24850.0),
        StockMetadata("PGAS.JK", "PGAS", "Perusahaan Gas Negara Tbk.", "Utilities", 1550.0),
        StockMetadata("AKRA.JK", "AKRA", "AKR Corporindo Tbk.", "Energy", 1600.0),
        StockMetadata("EXCL.JK", "EXCL", "XL Axiata Tbk.", "Infrastructure", 2150.0),
        StockMetadata("ISAT.JK", "ISAT", "Indosat Ooredoo Hutchison Tbk.", "Infrastructure", 2300.0),
        StockMetadata("GOTO.JK", "GOTO", "GoTo Gojek Tokopedia Tbk.", "Technology", 65.0),
        StockMetadata("MAPI.JK", "MAPI", "Mitra Adiperkasa Tbk.", "Consumer Cyclical", 1420.0),
        StockMetadata("ACES.JK", "ACES", "Aspirasi Hidup Indonesia Tbk.", "Consumer Cyclical", 860.0),
        StockMetadata("AMRT.JK", "AMRT", "Sumber Alfaria Trijaya Tbk.", "Consumer Non-Cyclical", 2950.0),
        StockMetadata("ERAA.JK", "ERAA", "Erajaya Swasembada Tbk.", "Consumer Cyclical", 410.0),
        StockMetadata("MEDC.JK", "MEDC", "Medco Energi Internasional Tbk.", "Energy", 1150.0),
        StockMetadata("INKP.JK", "INKP", "Indah Kiat Pulp & Paper Tbk.", "Basic Materials", 7250.0),
        StockMetadata("SMGR.JK", "SMGR", "Semen Indonesia Tbk.", "Basic Materials", 3800.0),
        StockMetadata("INTP.JK", "INTP", "Indocement Tunggal Prakarsa Tbk.", "Basic Materials", 7100.0),
        StockMetadata("JSMR.JK", "JSMR", "Jasa Marga Tbk.", "Infrastructure", 4650.0),
        StockMetadata("PWON.JK", "PWON", "Pakuwon Jati Tbk.", "Real Estate", 390.0),
        StockMetadata("BSDE.JK", "BSDE", "Bumi Serpong Damai Tbk.", "Real Estate", 980.0),
        StockMetadata("CTRA.JK", "CTRA", "Ciputra Development Tbk.", "Real Estate", 1150.0),
        StockMetadata("JPFA.JK", "JPFA", "Japfa Tbk.", "Consumer Non-Cyclical", 1350.0),
        StockMetadata("TOWR.JK", "TOWR", "Sarana Menara Nusantara Tbk.", "Infrastructure", 790.0),
        StockMetadata("TBIG.JK", "TBIG", "Tower Bersama Infrastructure Tbk.", "Infrastructure", 1850.0),
        StockMetadata("HMSP.JK", "HMSP", "Hanjaya Mandala Sampoerna Tbk.", "Consumer Non-Cyclical", 690.0),
        StockMetadata("SIDO.JK", "SIDO", "Industri Jamu dan Farmasi Sido Muncul Tbk.", "Healthcare", 710.0),
        StockMetadata("BRPT.JK", "BRPT", "Barito Pacific Tbk.", "Basic Materials", 980.0),
        StockMetadata("ESSA.JK", "ESSA", "Essa Industries Indonesia Tbk.", "Basic Materials", 810.0),
        StockMetadata("HRUM.JK", "HRUM", "Harum Energy Tbk.", "Energy", 1250.0),
        StockMetadata("UNTR.JK", "UNTR", "United Tractors Tbk.", "Industrial", 21850.0),
        StockMetadata("INCO.JK", "INCO", "Vale Indonesia Tbk.", "Basic Materials", 3450.0),
        StockMetadata("MIKA.JK", "MIKA", "Mitra Keluarga Karyasehat Tbk.", "Healthcare", 2850.0)
    )

    fun getBySymbol(symbol: String): StockMetadata? {
        return stocks.firstOrNull { it.symbol == symbol || it.shortName == symbol }
    }
}
