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
    val closeTime: Long
)

data class TradeEvent(
    val time: Long,
    val action: String,
    val price: Double,
    val amount: Double,
    val fee: Double,
    val equity: Double,
    val pnl: Double,
    val coins: Double
)

data class BacktestResult(
    val assetName: String,
    val symbol: String,
    val buyRsi: Double,
    val equity: Double,
    val profit: Double,
    val profitPercent: Double,
    val totalFees: Double,
    val trades: List<TradeEvent>,
    val firstCandleTime: Long,
    val lastCandleTime: Long
)

data class ChartBundle(
    val candles: List<PumpCandle>,
    val fast: List<Double?>,
    val slow: List<Double?>,
    val trades: List<TradeEvent>,
    val subtitle: String
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
    const val appVersionName = "1.1"
    const val startBalance = 1000.0
    const val feeRate = 0.0015
    const val slippage = 0.0005
    const val rsiPeriod = 14
    const val defaultBuyRsi = 35.0
    const val sellRsi = 62.0
    const val emaFastPeriod = 50
    const val emaSlowPeriod = 200
    const val stopLoss = 0.08
    const val trailingStop = 0.04
    const val pumpSymbol = "PUMPUSDT"
    const val uniqueWorkName = "pump_rsi_risk_periodic_monitor"

    private const val prefsName = "PumpRsiRiskBotV10"
    private const val algorithmVersion = 10
    private const val keyVersion = "algorithm_version"
    private const val keyRunning = "running"
    private const val keyWaitMode = "wait_mode"
    private const val keyBuyRsi = "buy_rsi"
    private const val keyLastSync = "last_sync"
    private const val keyLastCandle = "last_candle"
    private const val keyLastPrice = "last_price"
    private const val keyLastRsi = "last_rsi"
    private const val keyLastEma200 = "last_ema200"
    private const val keyBuySignal = "buy_signal"
    private const val keySellSignal = "sell_signal"
    private const val keySignalAction = "signal_action"
    private const val keySignalReason = "signal_reason"
    private const val keyEntryPrice = "entry_price"
    private const val keyHighestClose = "highest_close"
    private const val keyLastAlertKey = "last_alert_key"
    private const val keyMarketJson = "market_json"

    val coinOptions = listOf(
        CoinOption("PUMP", "PUMPUSDT"),
        CoinOption("Bitcoin", "BTCUSDT"),
        CoinOption("Ethereum", "ETHUSDT"),
        CoinOption("Solana", "SOLUSDT"),
        CoinOption("BNB", "BNBUSDT"),
        CoinOption("XRP", "XRPUSDT"),
        CoinOption("Dogecoin", "DOGEUSDT"),
        CoinOption("Cardano", "ADAUSDT"),
        CoinOption("TRON", "TRXUSDT"),
        CoinOption("Avalanche", "AVAXUSDT")
    )

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
            .putBoolean(keyBuySignal, false)
            .putBoolean(keySellSignal, false)
            .putString(keySignalAction, "WAIT")
            .putString(keySignalReason, "Нажмите ЗАПУСТИТЬ")
            .putDouble(keyEntryPrice, 0.0)
            .putDouble(keyHighestClose, 0.0)
            .putString(keyLastAlertKey, "")
            .putString(keyMarketJson, "")
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

    fun confirmBought(context: Context) {
        val s = snapshot(context)
        prefs(context).edit()
            .putString(keyWaitMode, "SELL")
            .putDouble(keyEntryPrice, if (s.lastPrice > 0.0) s.lastPrice else s.entryPrice)
            .putDouble(keyHighestClose, if (s.lastPrice > 0.0) s.lastPrice else s.highestClose)
            .putString(keyLastAlertKey, "")
            .apply()
    }

    fun confirmSold(context: Context) {
        prefs(context).edit()
            .putString(keyWaitMode, "BUY")
            .putDouble(keyEntryPrice, 0.0)
            .putDouble(keyHighestClose, 0.0)
            .putString(keyLastAlertKey, "")
            .apply()
    }

    fun klineUrl(symbol: String, interval: String, limit: Int = 500): String {
        val encoded = URLEncoder.encode(symbol, "UTF-8")
        return "https://data-api.binance.vision/api/v3/klines?symbol=$encoded&interval=$interval&limit=$limit"
    }

    fun historicalKlineUrl(symbol: String, interval: String, start: Long, end: Long): String {
        val encoded = URLEncoder.encode(symbol, "UTF-8")
        return "https://data-api.binance.vision/api/v3/klines?symbol=$encoded&interval=$interval&startTime=$start&endTime=$end&limit=1000"
    }

    fun syncPump(context: Context, json: String) {
        ensureInitialized(context)
        val candles = parseCandles(json)
        val p = prefs(context)
        val evaluation = evaluateLive(
            candles = candles,
            waitMode = p.getString(keyWaitMode, "BUY").orEmpty(),
            buyRsi = selectedBuyRsi(p),
            entryPrice = p.getDouble(keyEntryPrice, 0.0),
            storedHighest = p.getDouble(keyHighestClose, 0.0)
        )

        p.edit()
            .putString(keyMarketJson, json)
            .putLong(keyLastSync, System.currentTimeMillis())
            .putLong(keyLastCandle, evaluation.lastCandle)
            .putDouble(keyLastPrice, evaluation.lastPrice)
            .putDouble(keyLastRsi, evaluation.lastRsi)
            .putDouble(keyLastEma200, evaluation.lastEma200)
            .putBoolean(keyBuySignal, evaluation.buySignal)
            .putBoolean(keySellSignal, evaluation.sellSignal)
            .putString(keySignalAction, evaluation.signalAction)
            .putString(keySignalReason, evaluation.signalReason)
            .putDouble(keyHighestClose, evaluation.highestClose)
            .apply()
    }

    fun snapshot(context: Context): LiveSnapshot {
        ensureInitialized(context)
        val p = prefs(context)
        val candles = parseSavedCandles(p.getString(keyMarketJson, "").orEmpty())
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
                "RSI ${buyRsi.toInt()} на 30-минутных свечах. Желтая EMA50 / фиолетовая EMA200"
            )
        )
    }

    fun alertKey(snapshot: LiveSnapshot): String {
        return "${snapshot.signalAction}:${snapshot.lastCandle}:${snapshot.buyRsi.toInt()}"
    }

    fun shouldAlert(context: Context, snapshot: LiveSnapshot): Boolean {
        if (!snapshot.running) return false
        if (snapshot.signalAction != snapshot.waitMode) return false
        if (snapshot.signalAction != "BUY" && snapshot.signalAction != "SELL") return false
        val key = alertKey(snapshot)
        return key.isNotBlank() && key != prefs(context).getString(keyLastAlertKey, "")
    }

    fun markAlerted(context: Context, snapshot: LiveSnapshot) {
        prefs(context).edit().putString(keyLastAlertKey, alertKey(snapshot)).apply()
    }

    fun backtest(asset: CoinOption, candles: List<PumpCandle>, startTime: Long, buyThreshold: Double): BacktestResult {
        val buyRsi = normalizeBuyRsi(buyThreshold)
        if (candles.isEmpty()) {
            return BacktestResult(asset.name, asset.symbol, buyRsi, startBalance, 0.0, 0.0, 0.0, emptyList(), 0L, 0L)
        }

        val closes = candles.map { it.close }
        val rsi = rsi(closes, rsiPeriod)
        val ema200 = ema(closes, emaSlowPeriod)
        var cash = startBalance
        var coins = 0.0
        var entryPrice = 0.0
        var positionCost = 0.0
        var highestClose = 0.0
        val trades = ArrayList<TradeEvent>()
        val first = maxOf(emaSlowPeriod + 1, rsiPeriod + 1)

        for (i in first until candles.size) {
            val candle = candles[i]
            if (candle.closeTime < startTime) continue
            val rsiNow = value(rsi, i)
            val emaNow = value(ema200, i)
            val buy = coins <= 0.0 && rsiNow <= buyRsi && candle.close > emaNow
            val stop = coins > 0.0 &&
                (candle.close <= entryPrice * (1 - stopLoss) || candle.close <= highestClose * (1 - trailingStop))
            val sell = coins > 0.0 && (rsiNow >= sellRsi || candle.close < emaNow || stop)

            if (buy) {
                val buyPrice = candle.close * (1 + slippage)
                val amount = cash
                val fee = amount * feeRate
                coins = (amount - fee) / buyPrice
                cash = 0.0
                entryPrice = buyPrice
                positionCost = amount
                highestClose = candle.close
                trades.add(TradeEvent(candle.closeTime, "BUY", buyPrice, amount, fee, coins * buyPrice, 0.0, coins))
            } else if (sell) {
                val sellPrice = candle.close * (1 - slippage)
                val gross = coins * sellPrice
                val fee = gross * feeRate
                cash = gross - fee
                val pnl = cash - positionCost
                trades.add(TradeEvent(candle.closeTime, "SELL", sellPrice, gross, fee, cash, pnl, 0.0))
                coins = 0.0
                entryPrice = 0.0
                positionCost = 0.0
                highestClose = 0.0
            } else if (coins > 0.0) {
                highestClose = maxOf(highestClose, candle.close)
            }
        }

        val lastPrice = candles.lastOrNull()?.close ?: 0.0
        val equity = if (coins > 0.0 && lastPrice > 0.0) coins * lastPrice * (1 - feeRate) else cash
        val profit = equity - startBalance
        return BacktestResult(
            assetName = asset.name,
            symbol = asset.symbol,
            buyRsi = buyRsi,
            equity = equity,
            profit = profit,
            profitPercent = profit / startBalance * 100.0,
            totalFees = trades.sumOf { it.fee },
            trades = trades,
            firstCandleTime = candles.firstOrNull()?.openTime ?: 0L,
            lastCandleTime = candles.lastOrNull()?.closeTime ?: 0L
        )
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
                        closeTime = closeTime
                    )
                )
            }
        }
        return candles.distinctBy { it.closeTime }.sortedBy { it.closeTime }
    }

    private data class LiveEvaluation(
        val lastCandle: Long,
        val lastPrice: Double,
        val lastRsi: Double,
        val lastEma200: Double,
        val buySignal: Boolean,
        val sellSignal: Boolean,
        val signalAction: String,
        val signalReason: String,
        val highestClose: Double
    )

    private fun evaluateLive(
        candles: List<PumpCandle>,
        waitMode: String,
        buyRsi: Double,
        entryPrice: Double,
        storedHighest: Double
    ): LiveEvaluation {
        if (candles.size < emaSlowPeriod + 5) {
            return LiveEvaluation(0L, 0.0, 0.0, 0.0, false, false, "WAIT", "Ждем достаточно 30-минутных свечей", storedHighest)
        }

        val closes = candles.map { it.close }
        val rsi = rsi(closes, rsiPeriod)
        val ema200 = ema(closes, emaSlowPeriod)
        val i = candles.lastIndex
        val candle = candles[i]
        val rsiNow = value(rsi, i)
        val emaNow = value(ema200, i)
        val trendOk = candle.close > emaNow
        val highest = if (waitMode == "SELL") maxOf(storedHighest, candle.close) else storedHighest
        val stop = waitMode == "SELL" && entryPrice > 0.0 &&
            (candle.close <= entryPrice * (1 - stopLoss) || candle.close <= highest * (1 - trailingStop))
        val buy = trendOk && rsiNow <= buyRsi
        val sell = rsiNow >= sellRsi || !trendOk || stop

        val action = when {
            waitMode == "BUY" && buy -> "BUY"
            waitMode == "SELL" && sell -> "SELL"
            else -> "WAIT"
        }
        val reason = when (action) {
            "BUY" -> String.format(Locale.US, "ПОКУПКА: RSI %.1f <= %.0f и цена выше EMA200", rsiNow, buyRsi)
            "SELL" -> when {
                stop -> "ПРОДАЖА: сработала защита стоп/трейлинг"
                rsiNow >= sellRsi -> String.format(Locale.US, "ПРОДАЖА: RSI %.1f >= %.0f", rsiNow, sellRsi)
                !trendOk -> "ПРОДАЖА: цена ушла ниже EMA200"
                else -> "ПРОДАЖА: есть сигнал"
            }
            else -> String.format(Locale.US, "ЖДЕМ: RSI %.1f, EMA200 %.8f", rsiNow, emaNow)
        }

        return LiveEvaluation(candle.closeTime, candle.close, rsiNow, emaNow, buy, sell, action, reason, highest)
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
        return if (value <= 30.0) 30.0 else 35.0
    }

    private fun SharedPreferences.Editor.putDouble(key: String, value: Double): SharedPreferences.Editor {
        return putLong(key, java.lang.Double.doubleToRawLongBits(value))
    }

    private fun SharedPreferences.getDouble(key: String, default: Double): Double {
        return if (contains(key)) java.lang.Double.longBitsToDouble(getLong(key, 0L)) else default
    }
}
