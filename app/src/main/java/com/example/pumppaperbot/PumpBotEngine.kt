package com.example.pumppaperbot

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PumpCandle(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long,
    val quoteVolume: Double = 0.0,
    val tradeCount: Int = 0,
    val takerBuyVolume: Double = 0.0
)

data class TradeEvent(
    val time: Long,
    val action: String,
    val price: Double,
    val amount: Double,
    val fee: Double,
    val equity: Double,
    val pnl: Double,
    val coins: Double,
    val reason: String = ""
)

data class BacktestResult(
    val assetName: String,
    val symbol: String,
    val strategyName: String,
    val equity: Double,
    val profit: Double,
    val profitPercent: Double,
    val totalFees: Double,
    val trades: List<TradeEvent>,
    val firstCandleTime: Long,
    val lastCandleTime: Long,
    val roundTrips: Int,
    val winRatePercent: Double,
    val maxDrawdownPercent: Double,
    val stopCount: Int
) {
    companion object {
        fun empty(startTime: Long = 0L) = BacktestResult(
            "PUMP", "PUMP/EUR", "Профиль 4 этапа", PumpBotEngine.startBalance,
            0.0, 0.0, 0.0, emptyList(), startTime, startTime, 0, 0.0, 0.0, 0
        )
    }
}

data class ChartBundle(
    val candles: List<PumpCandle>,
    val fast: List<Double?>,
    val slow: List<Double?>,
    val trades: List<TradeEvent>,
    val subtitle: String,
    val readinessScore: Int = 0,
    val trendReadiness: Int = 0,
    val shockReadiness: Int = 0,
    val aggressive: Boolean = false,
    val showReadinessGauge: Boolean = true
)

data class LiveSnapshot(
    val running: Boolean,
    val waitMode: String,
    val buyRsi: Double,
    val lastSync: Long,
    val lastCandle: Long,
    val lastPrice: Double,
    val lastRsi: Double,
    val lastEma200: Double,
    val fundingRate: Double,
    val strategyMode: String,
    val aggressive: Boolean,
    val readinessScore: Int,
    val trendReadiness: Int,
    val shockReadiness: Int,
    val partialTaken: Boolean,
    val buySignal: Boolean,
    val sellSignal: Boolean,
    val signalAction: String,
    val signalReason: String,
    val entryPrice: Double,
    val highestClose: Double,
    val chart: ChartBundle
)

data class CoinOption(val name: String, val symbol: String)

object PumpBotEngine {
    const val appVersionName = "1.7"
    const val startBalance = 1000.0
    const val feeRate = 0.0015
    const val slippage = 0.0005
    const val rsiPeriod = 14
    const val defaultBuyRsi = 45.0
    const val sellRsi = 62.0
    const val emaFastPeriod = 50
    const val emaSlowPeriod = 200
    const val stopLoss = StrategyV2.PRICE_STOP
    const val trailingStop = 0.04
    const val pumpSymbol = "PUMPUSDT"
    const val btcSymbol = "BTCUSDT"
    const val ethSymbol = "ETHUSDT"
    const val solSymbol = "SOLUSDT"
    const val eurSymbol = "EURUSDT"
    const val uniqueWorkName = "pump_rsi_risk_periodic_monitor"

    private const val prefsName = "PumpSignalV17"
    private const val algorithmVersion = 17
    private const val keyVersion = "algorithm_version"
    private const val keyRunning = "running"
    private const val keyWaitMode = "wait_mode"
    private const val keyBuyRsi = "buy_rsi"
    private const val keyLastSync = "last_sync"
    private const val keyLastCandle = "last_candle"
    private const val keyLastPrice = "last_price"
    private const val keyLastRsi = "last_rsi"
    private const val keyLastEma200 = "last_ema200"
    private const val keyFundingRate = "funding_rate"
    private const val keyAggressive = "aggressive"
    private const val keyReadinessScore = "readiness_score"
    private const val keyTrendReadiness = "trend_readiness"
    private const val keyShockReadiness = "shock_readiness"
    private const val keyStrategyMode = "strategy_mode"
    private const val keyPendingMode = "pending_mode"
    private const val keyEntryTime = "entry_time"
    private const val keyPartialTaken = "partial_taken"
    private const val keyPartialCandle = "partial_candle"
    private const val keyBuySignal = "buy_signal"
    private const val keySellSignal = "sell_signal"
    private const val keySignalAction = "signal_action"
    private const val keySignalReason = "signal_reason"
    private const val keyEntryPrice = "entry_price"
    private const val keyHighestClose = "highest_close"
    private const val keyLastAlertKey = "last_alert_key"
    private const val keyMarketJson = "market_json"
    private const val keyEurJson = "eur_json"
    private const val keyBtcJson = "btc_json"
    private const val keyEthJson = "eth_json"
    private const val keySolJson = "sol_json"
    private const val keyFuturesJson = "futures_json"
    private const val keyPremiumJson = "premium_json"
    private const val keyFundingJson = "funding_json"

    val coinOptions = listOf(CoinOption("PUMP", "PUMPUSDT"))

    fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    fun ensureInitialized(context: Context) {
        val p = prefs(context)
        if (p.getInt(keyVersion, 0) != algorithmVersion) reset(context)
    }

    fun reset(context: Context) {
        prefs(context).edit()
            .clear()
            .putInt(keyVersion, algorithmVersion)
            .putBoolean(keyRunning, false)
            .putString(keyWaitMode, "BUY")
            .putDouble(keyBuyRsi, defaultBuyRsi)
            .putLong(keyLastSync, 0L)
            .putLong(keyLastCandle, 0L)
            .putDouble(keyLastPrice, 0.0)
            .putDouble(keyLastRsi, 0.0)
            .putDouble(keyLastEma200, 0.0)
            .putDouble(keyFundingRate, 0.0)
            .putBoolean(keyAggressive, false)
            .putInt(keyReadinessScore, 0)
            .putInt(keyTrendReadiness, 0)
            .putInt(keyShockReadiness, 0)
            .putString(keyStrategyMode, StrategyV2.MODE_NONE)
            .putString(keyPendingMode, StrategyV2.MODE_NONE)
            .putLong(keyEntryTime, 0L)
            .putBoolean(keyPartialTaken, false)
            .putLong(keyPartialCandle, 0L)
            .putBoolean(keyBuySignal, false)
            .putBoolean(keySellSignal, false)
            .putString(keySignalAction, "WAIT")
            .putString(keySignalReason, "Нажмите ЗАПУСТИТЬ")
            .putDouble(keyEntryPrice, 0.0)
            .putDouble(keyHighestClose, 0.0)
            .putString(keyLastAlertKey, "")
            .putString(keyMarketJson, "")
            .putString(keyEurJson, "")
            .putString(keyBtcJson, "")
            .putString(keyEthJson, "")
            .putString(keySolJson, "")
            .putString(keyFuturesJson, "")
            .putString(keyPremiumJson, "")
            .putString(keyFundingJson, "")
            .apply()
    }

    fun setRunning(context: Context, running: Boolean) {
        ensureInitialized(context)
        prefs(context).edit()
            .putBoolean(keyRunning, running)
            .putLong(keyLastSync, System.currentTimeMillis())
            .apply()
    }

    fun isRunning(context: Context): Boolean {
        ensureInitialized(context)
        return prefs(context).getBoolean(keyRunning, false)
    }

    fun setWaitMode(context: Context, mode: String) {
        ensureInitialized(context)
        val clean = if (mode == "SELL") "SELL" else "BUY"
        prefs(context).edit()
            .putString(keyWaitMode, clean)
            .putString(keyLastAlertKey, "")
            .apply()
    }

    fun setBuyRsi(context: Context, value: Double) {
        ensureInitialized(context)
        prefs(context).edit()
            .putDouble(keyBuyRsi, normalizeBuyRsi(value))
            .putString(keyLastAlertKey, "")
            .apply()
    }

    fun setAggressive(context: Context, aggressive: Boolean) {
        ensureInitialized(context)
        prefs(context).edit()
            .putBoolean(keyAggressive, aggressive)
            .putString(keyLastAlertKey, "")
            .apply()
    }

    fun isAggressive(context: Context): Boolean {
        ensureInitialized(context)
        return prefs(context).getBoolean(keyAggressive, false)
    }

    fun confirmBought(context: Context) {
        val s = snapshot(context)
        val confirmedMode = when {
            s.strategyMode == StrategyV2.MODE_EXHAUSTION -> StrategyV2.MODE_EXHAUSTION
            s.strategyMode == StrategyV2.MODE_SHOCK -> StrategyV2.MODE_SHOCK
            s.strategyMode == StrategyV2.MODE_TREND -> StrategyV2.MODE_TREND
            s.shockReadiness > s.trendReadiness -> StrategyV2.MODE_EXHAUSTION
            else -> StrategyV2.MODE_TREND
        }
        prefs(context).edit()
            .putString(keyWaitMode, "SELL")
            .putDouble(keyEntryPrice, if (s.lastPrice > 0.0) s.lastPrice else s.entryPrice)
            .putDouble(keyHighestClose, if (s.lastPrice > 0.0) s.lastPrice else s.highestClose)
            .putLong(keyEntryTime, s.lastCandle)
            .putString(keyStrategyMode, confirmedMode)
            .putBoolean(keyPartialTaken, false)
            .putLong(keyPartialCandle, 0L)
            .putString(keyLastAlertKey, "")
            .apply()
    }

    fun confirmPartialSold(context: Context) {
        val s = snapshot(context)
        prefs(context).edit()
            .putBoolean(keyPartialTaken, true)
            .putLong(keyPartialCandle, s.lastCandle)
            .putString(keyLastAlertKey, "")
            .apply()
    }

    fun confirmSold(context: Context) {
        prefs(context).edit()
            .putString(keyWaitMode, "BUY")
            .putDouble(keyEntryPrice, 0.0)
            .putDouble(keyHighestClose, 0.0)
            .putLong(keyEntryTime, 0L)
            .putString(keyStrategyMode, StrategyV2.MODE_NONE)
            .putBoolean(keyPartialTaken, false)
            .putLong(keyPartialCandle, 0L)
            .putString(keyLastAlertKey, "")
            .apply()
    }

    fun klineUrl(symbol: String, interval: String, limit: Int = 1000): String {
        val encoded = URLEncoder.encode(symbol, "UTF-8")
        return "https://data-api.binance.vision/api/v3/klines?symbol=$encoded&interval=$interval&limit=$limit"
    }

    fun historicalKlineUrl(symbol: String, interval: String, start: Long, end: Long): String {
        val encoded = URLEncoder.encode(symbol, "UTF-8")
        return "https://data-api.binance.vision/api/v3/klines?symbol=$encoded&interval=$interval&startTime=$start&endTime=$end&limit=1000"
    }

    fun fundingUrl(symbol: String = pumpSymbol, start: Long? = null, end: Long? = null): String {
        val encoded = URLEncoder.encode(symbol, "UTF-8")
        val range = if (start != null && end != null) "&startTime=$start&endTime=$end" else ""
        return "https://fapi.binance.com/fapi/v1/fundingRate?symbol=$encoded$range&limit=1000"
    }

    fun futuresKlineUrl(symbol: String = pumpSymbol, interval: String = "30m", limit: Int = 1000): String {
        val encoded = URLEncoder.encode(symbol, "UTF-8")
        return "https://fapi.binance.com/fapi/v1/klines?symbol=$encoded&interval=$interval&limit=$limit"
    }

    fun historicalFuturesKlineUrl(symbol: String, interval: String, start: Long, end: Long): String {
        val encoded = URLEncoder.encode(symbol, "UTF-8")
        return "https://fapi.binance.com/fapi/v1/klines?symbol=$encoded&interval=$interval&startTime=$start&endTime=$end&limit=1500"
    }

    fun premiumKlineUrl(symbol: String = pumpSymbol, interval: String = "30m", limit: Int = 1000): String {
        val encoded = URLEncoder.encode(symbol, "UTF-8")
        return "https://fapi.binance.com/fapi/v1/premiumIndexKlines?symbol=$encoded&interval=$interval&limit=$limit"
    }

    fun historicalPremiumKlineUrl(symbol: String, interval: String, start: Long, end: Long): String {
        val encoded = URLEncoder.encode(symbol, "UTF-8")
        return "https://fapi.binance.com/fapi/v1/premiumIndexKlines?symbol=$encoded&interval=$interval&startTime=$start&endTime=$end&limit=1500"
    }

    fun syncMarket(
        context: Context,
        pumpJson: String,
        eurJson: String,
        btcJson: String,
        ethJson: String,
        solJson: String,
        futuresJson: String,
        premiumJson: String,
        fundingJson: String
    ) {
        ensureInitialized(context)
        val candles = StrategyV2.synthesizeEur(parseCandles(pumpJson), parseCandles(eurJson))
        val btc = parseCandles(btcJson)
        val eth = parseCandles(ethJson)
        val sol = parseCandles(solJson)
        val futures = parseCandles(futuresJson)
        val premium = parseCandles(premiumJson)
        val funding = parseFunding(fundingJson)
        val p = prefs(context)
        val evaluation = evaluateLive(
            candles = candles,
            btcCandles = btc,
            ethCandles = eth,
            solCandles = sol,
            futuresCandles = futures,
            premiumCandles = premium,
            funding = funding,
            waitMode = p.getString(keyWaitMode, "BUY").orEmpty(),
            entryPrice = p.getDouble(keyEntryPrice, 0.0),
            entryTime = p.getLong(keyEntryTime, 0L),
            positionMode = p.getString(keyStrategyMode, StrategyV2.MODE_NONE).orEmpty(),
            partialTaken = p.getBoolean(keyPartialTaken, false),
            partialCandle = p.getLong(keyPartialCandle, 0L),
            storedHighest = p.getDouble(keyHighestClose, 0.0),
            aggressive = p.getBoolean(keyAggressive, false)
        )

        val editor = p.edit()
            .putString(keyMarketJson, pumpJson)
            .putString(keyEurJson, eurJson)
            .putString(keyBtcJson, btcJson)
            .putString(keyEthJson, ethJson)
            .putString(keySolJson, solJson)
            .putString(keyFuturesJson, futuresJson)
            .putString(keyPremiumJson, premiumJson)
            .putString(keyFundingJson, fundingJson)
            .putLong(keyLastSync, System.currentTimeMillis())
            .putLong(keyLastCandle, evaluation.lastCandle)
            .putDouble(keyLastPrice, evaluation.lastPrice)
            .putDouble(keyLastRsi, evaluation.lastRsi)
            .putDouble(keyLastEma200, evaluation.lastEma200)
            .putDouble(keyFundingRate, evaluation.fundingRate)
            .putInt(keyReadinessScore, evaluation.readinessScore)
            .putInt(keyTrendReadiness, evaluation.trendReadiness)
            .putInt(keyShockReadiness, evaluation.shockReadiness)
            .putString(keyPendingMode, evaluation.strategyMode)
            .putBoolean(keyBuySignal, evaluation.buySignal)
            .putBoolean(keySellSignal, evaluation.sellSignal)
            .putString(keySignalAction, evaluation.signalAction)
            .putString(keySignalReason, evaluation.signalReason)
            .putDouble(keyHighestClose, evaluation.highestClose)
        if (kotlin.math.abs(evaluation.readinessScore) < 90) editor.putString(keyLastAlertKey, "")
        editor.apply()
    }

    fun snapshot(context: Context): LiveSnapshot {
        ensureInitialized(context)
        val p = prefs(context)
        val candles = StrategyV2.synthesizeEur(
            parseSavedCandles(p.getString(keyMarketJson, "").orEmpty()),
            parseSavedCandles(p.getString(keyEurJson, "").orEmpty())
        )
        val closes = candles.map { it.close }
        val fast = ema(closes, emaFastPeriod)
        val slow = ema(closes, emaSlowPeriod)
        val buyRsi = selectedBuyRsi(p)
        return LiveSnapshot(
            running = p.getBoolean(keyRunning, false),
            waitMode = p.getString(keyWaitMode, "BUY").orEmpty().ifBlank { "BUY" },
            buyRsi = buyRsi,
            lastSync = p.getLong(keyLastSync, 0L),
            lastCandle = p.getLong(keyLastCandle, 0L),
            lastPrice = p.getDouble(keyLastPrice, 0.0),
            lastRsi = p.getDouble(keyLastRsi, 0.0),
            lastEma200 = p.getDouble(keyLastEma200, 0.0),
            fundingRate = p.getDouble(keyFundingRate, 0.0),
            strategyMode = if (p.getString(keyWaitMode, "BUY") == "SELL") {
                p.getString(keyStrategyMode, StrategyV2.MODE_NONE).orEmpty()
            } else {
                p.getString(keyPendingMode, StrategyV2.MODE_NONE).orEmpty()
            },
            aggressive = p.getBoolean(keyAggressive, false),
            readinessScore = p.getInt(keyReadinessScore, 0),
            trendReadiness = p.getInt(keyTrendReadiness, 0),
            shockReadiness = p.getInt(keyShockReadiness, 0),
            partialTaken = p.getBoolean(keyPartialTaken, false),
            buySignal = p.getBoolean(keyBuySignal, false),
            sellSignal = p.getBoolean(keySellSignal, false),
            signalAction = p.getString(keySignalAction, "WAIT").orEmpty(),
            signalReason = p.getString(keySignalReason, "Сигнала нет").orEmpty(),
            entryPrice = p.getDouble(keyEntryPrice, 0.0),
            highestClose = p.getDouble(keyHighestClose, 0.0),
            chart = ChartBundle(
                candles,
                fast,
                slow,
                emptyList(),
                "PUMP/EUR, 30 минут. Желтая EMA50 / фиолетовая EMA200",
                p.getInt(keyReadinessScore, 0),
                p.getInt(keyTrendReadiness, 0),
                p.getInt(keyShockReadiness, 0),
                p.getBoolean(keyAggressive, false)
            )
        )
    }

    fun alertKey(context: Context, snapshot: LiveSnapshot): String {
        val band = if (kotlin.math.abs(snapshot.readinessScore) >= 100) 100 else 99
        val profile = if (snapshot.aggressive) "AGGRESSIVE" else "CAREFUL"
        return "${snapshot.waitMode}:$band:$profile:${snapshot.strategyMode}${AlertSchedule.alertKeySuffix(context)}"
    }

    fun shouldAlert(context: Context, snapshot: LiveSnapshot): Boolean {
        if (!snapshot.running) return false
        val expected = (snapshot.waitMode == "BUY" && snapshot.readinessScore >= 99) ||
            (snapshot.waitMode == "SELL" && snapshot.readinessScore <= -99)
        if (expected && !AlertSchedule.isAllowedNow(context)) {
            AlertSchedule.rememberBlocked(context, snapshot)
            return false
        }
        if (AlertSchedule.isAllowedNow(context)) {
            when (AlertSchedule.resolvePending(context, snapshot)) {
                DelayedSignalState.POSSIBLE -> {
                    val delayedKey = alertKey(context, snapshot)
                    return delayedKey != prefs(context).getString(keyLastAlertKey, "")
                }
                DelayedSignalState.MISSED -> return false
                DelayedSignalState.NONE -> Unit
            }
        }
        if (!expected) return false
        val key = alertKey(context, snapshot)
        return key.isNotBlank() && key != prefs(context).getString(keyLastAlertKey, "")
    }

    fun markAlerted(context: Context, snapshot: LiveSnapshot) {
        val key = alertKey(context, snapshot)
        prefs(context).edit().putString(keyLastAlertKey, key).apply()
        if (AlertSchedule.pendingTime(context) > 0L) AlertSchedule.markDelivered(context)
    }

    fun parseCandles(json: String): List<PumpCandle> {
        if (json.isBlank()) return emptyList()
        val rows = JSONArray(json)
        val now = System.currentTimeMillis()
        val candles = ArrayList<PumpCandle>()
        for (i in 0 until rows.length()) {
            val row = rows.getJSONArray(i)
            val closeTime = row.getLong(6)
            if (closeTime < now) {
                candles.add(
                    PumpCandle(
                        openTime = row.getLong(0),
                        open = row.getString(1).toDouble(),
                        high = row.getString(2).toDouble(),
                        low = row.getString(3).toDouble(),
                        close = row.getString(4).toDouble(),
                        volume = row.getString(5).toDouble(),
                        closeTime = closeTime,
                        quoteVolume = row.optString(7, "0").toDoubleOrNull() ?: 0.0,
                        tradeCount = row.optInt(8, 0),
                        takerBuyVolume = row.optString(9, "0").toDoubleOrNull() ?: 0.0
                    )
                )
            }
        }
        return candles.distinctBy { it.closeTime }.sortedBy { it.closeTime }
    }

    fun parseFunding(json: String): List<FundingPoint> {
        if (json.isBlank()) return emptyList()
        return try {
            val rows = JSONArray(json)
            (0 until rows.length()).mapNotNull { index ->
                val row = rows.optJSONObject(index) ?: return@mapNotNull null
                val time = row.optLong("fundingTime", 0L)
                val rate = row.optString("fundingRate", "").toDoubleOrNull() ?: return@mapNotNull null
                if (time > 0L) FundingPoint(time, rate) else null
            }.distinctBy { it.time }.sortedBy { it.time }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private data class LiveEvaluation(
        val lastCandle: Long,
        val lastPrice: Double,
        val lastRsi: Double,
        val lastEma200: Double,
        val fundingRate: Double,
        val strategyMode: String,
        val buySignal: Boolean,
        val sellSignal: Boolean,
        val signalAction: String,
        val signalReason: String,
        val highestClose: Double,
        val readinessScore: Int,
        val trendReadiness: Int,
        val shockReadiness: Int
    )

    private fun evaluateLive(
        candles: List<PumpCandle>,
        btcCandles: List<PumpCandle>,
        ethCandles: List<PumpCandle>,
        solCandles: List<PumpCandle>,
        futuresCandles: List<PumpCandle>,
        premiumCandles: List<PumpCandle>,
        funding: List<FundingPoint>,
        waitMode: String,
        entryPrice: Double,
        entryTime: Long,
        positionMode: String,
        partialTaken: Boolean,
        partialCandle: Long,
        storedHighest: Double,
        aggressive: Boolean
    ): LiveEvaluation {
        if (candles.size < emaSlowPeriod + 36 || btcCandles.size < emaSlowPeriod + 7) {
            return LiveEvaluation(
                0L, 0.0, 0.0, 0.0, 0.0, StrategyV2.MODE_NONE,
                false, false, "WAIT", "Ждем достаточно свечей PUMP/EUR и BTC", storedHighest,
                0, 0, 0
            )
        }

        val closes = candles.map { it.close }
        val rsi = rsi(closes, rsiPeriod)
        val ema200 = ema(closes, emaSlowPeriod)
        val i = candles.lastIndex
        val candle = candles[i]
        val rsiNow = value(rsi, i)
        val emaNow = value(ema200, i)
        val latestFunding = funding.lastOrNull { it.time <= candle.closeTime }?.rate ?: 0.0

        if (waitMode == "BUY") {
            val entry = StrategyV2.latestEntrySignal(
                candles, btcCandles, funding, aggressive,
                ethCandles, solCandles, futuresCandles, premiumCandles
            )
            val action = if (entry.active) "BUY" else "WAIT"
            val readiness = maxOf(entry.trendReadiness, entry.shockReadiness)
            return LiveEvaluation(
                candle.closeTime, candle.close, entry.rsi, entry.ema200, entry.funding, entry.mode,
                entry.active, false, action, entry.reason, storedHighest,
                if (entry.active) 100 else readiness.coerceIn(0, 99),
                entry.trendReadiness,
                entry.shockReadiness
            )
        }

        if (candle.closeTime <= entryTime || (partialTaken && candle.closeTime <= partialCandle)) {
            return LiveEvaluation(
                candle.closeTime, candle.close, rsiNow, emaNow, latestFunding, positionMode,
                false, false, "WAIT", "Позиция подтверждена. Ждем закрытия следующей свечи", storedHighest,
                0, 0, 0
            )
        }

        val exit = StrategyV2.evaluateExit(
            candle, positionMode, entryPrice, entryTime, partialTaken, storedHighest
        )
        val sell = exit.action == StrategyV2.ACTION_SELL || exit.action == StrategyV2.ACTION_SELL_HALF
        return LiveEvaluation(
            candle.closeTime, candle.close, rsiNow, emaNow, latestFunding, positionMode,
            false, sell, exit.action, exit.reason, exit.highestHigh,
            if (sell) -100 else -exit.readiness.coerceIn(0, 99),
            0,
            0
        )
    }

    private fun parseSavedCandles(json: String): List<PumpCandle> {
        return try {
            parseCandles(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun formatTime(time: Long): String {
        if (time <= 0L) return "-"
        return SimpleDateFormat("dd.MM HH:mm", Locale.GERMAN).format(Date(time))
    }

    fun formatDate(time: Long): String {
        if (time <= 0L) return "-"
        return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN).format(Date(time))
    }

    private fun ema(values: List<Double>, period: Int): List<Double?> {
        val result = MutableList<Double?>(values.size) { null }
        val multiplier = 2.0 / (period + 1.0)
        var previous: Double? = null
        for (i in values.indices) {
            if (i == period - 1) {
                var sum = 0.0
                for (j in 0 until period) sum += values[j]
                previous = sum / period
                result[i] = previous
            } else if (i >= period && previous != null) {
                previous = values[i] * multiplier + previous * (1 - multiplier)
                result[i] = previous
            }
        }
        return result
    }

    private fun rsi(values: List<Double>, period: Int): List<Double?> {
        val result = MutableList<Double?>(values.size) { null }
        var averageGain = 0.0
        var averageLoss = 0.0
        for (i in 1 until values.size) {
            val change = values[i] - values[i - 1]
            val gain = maxOf(change, 0.0)
            val loss = maxOf(-change, 0.0)
            if (i <= period) {
                averageGain += gain
                averageLoss += loss
                if (i == period) {
                    averageGain /= period
                    averageLoss /= period
                    result[i] = if (averageLoss == 0.0) 100.0 else 100.0 - 100.0 / (1.0 + averageGain / averageLoss)
                }
            } else {
                averageGain = (averageGain * (period - 1) + gain) / period
                averageLoss = (averageLoss * (period - 1) + loss) / period
                result[i] = if (averageLoss == 0.0) 100.0 else 100.0 - 100.0 / (1.0 + averageGain / averageLoss)
            }
        }
        return result
    }

    private fun value(values: List<Double?>, index: Int): Double {
        return values.getOrNull(index) ?: 0.0
    }

    private fun selectedBuyRsi(prefs: SharedPreferences): Double {
        return normalizeBuyRsi(prefs.getDouble(keyBuyRsi, defaultBuyRsi))
    }

    private fun normalizeBuyRsi(value: Double): Double {
        return 45.0
    }

    private fun SharedPreferences.Editor.putDouble(key: String, value: Double): SharedPreferences.Editor {
        return putLong(key, java.lang.Double.doubleToRawLongBits(value))
    }

    private fun SharedPreferences.getDouble(key: String, default: Double): Double {
        return if (contains(key)) java.lang.Double.longBitsToDouble(getLong(key, 0L)) else default
    }
}
