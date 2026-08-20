package com.example.pumppaperbot

import android.content.Context
import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Shadow-only, second-level market observer. It never creates a BUY or SELL.
 * PUMP/USDT is used for order-flow ratios; the Gemini paper account remains PUMP/EUR.
 */
class MicroImpulseStream(context: Context) : WebSocketListener() {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private val trades = ArrayDeque<MicroTrade>()
    private val bitcoinTrades = ArrayDeque<MicroTrade>()
    private var socket: WebSocket? = null
    private var stopped = true
    private var bestBid = 0.0
    private var bestAsk = 0.0
    private var bidQuantity = 0.0
    private var askQuantity = 0.0
    private var lastSavedAt = 0L
    private var connectedAt = 0L
    private var ignitionAt = 0L
    private var ignitionPrice = 0.0
    private var bitcoinPrice = 0.0

    @Synchronized
    fun start() {
        if (!stopped && socket != null) return
        stopped = false
        connect()
    }

    @Synchronized
    fun stop() {
        stopped = true
        handler.removeCallbacksAndMessages(null)
        socket?.close(1000, "monitor stopped")
        socket = null
        client.dispatcher.executorService.shutdown()
        MicroImpulseStore.markDisconnected(appContext)
    }

    @Synchronized
    private fun connect() {
        if (stopped) return
        socket?.cancel()
        val request = Request.Builder().url(STREAM_URL).build()
        socket = client.newWebSocket(request, this)
        MicroImpulseStore.markConnecting(appContext)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        connectedAt = System.currentTimeMillis()
        ignitionAt = 0L
        ignitionPrice = 0.0
        MicroImpulseStore.markConnected(appContext)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        runCatching {
            val root = JSONObject(text)
            val data = root.optJSONObject("data") ?: root
            val stream = root.optString("stream").lowercase()
            when {
                stream.startsWith("pumpusdt@aggtrade") -> recordTrade(data)
                stream.startsWith("pumpusdt@bookticker") -> recordBook(data)
                stream.startsWith("btcusdt@aggtrade") -> recordBitcoinTrade(data)
                stream.isBlank() && data.optString("e") == "aggTrade" -> recordTrade(data)
                stream.isBlank() && data.optString("e") == "bookTicker" -> recordBook(data)
            }
        }.onFailure {
            MicroImpulseStore.markError(appContext, it.message ?: "ошибка разбора WebSocket")
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        MicroImpulseStore.markError(appContext, t.message ?: "WebSocket отключён")
        scheduleReconnect()
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        MicroImpulseStore.markDisconnected(appContext)
        scheduleReconnect()
    }

    @Synchronized
    private fun scheduleReconnect() {
        socket = null
        if (stopped) return
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ connect() }, RECONNECT_MILLIS)
    }

    @Synchronized
    private fun recordTrade(data: JSONObject) {
        val price = data.optString("p").toDoubleOrNull() ?: return
        val quantity = data.optString("q").toDoubleOrNull() ?: return
        val at = data.optLong("T", System.currentTimeMillis())
        if (price <= 0.0 || quantity <= 0.0) return
        // Binance m=false means the buyer was the aggressive taker.
        trades.addLast(MicroTrade(at, price, price * quantity, aggressiveBuy = !data.optBoolean("m", true)))
        evaluate(at, price)
    }

    @Synchronized
    private fun recordBook(data: JSONObject) {
        bestBid = data.optString("b").toDoubleOrNull() ?: bestBid
        bestAsk = data.optString("a").toDoubleOrNull() ?: bestAsk
        bidQuantity = data.optString("B").toDoubleOrNull() ?: bidQuantity
        askQuantity = data.optString("A").toDoubleOrNull() ?: askQuantity
        val price = when {
            bestBid > 0.0 && bestAsk > 0.0 -> (bestBid + bestAsk) / 2.0
            trades.isNotEmpty() -> trades.peekLast().price
            else -> 0.0
        }
        if (price > 0.0) evaluate(System.currentTimeMillis(), price)
    }

    @Synchronized
    private fun recordBitcoinTrade(data: JSONObject) {
        val price = data.optString("p").toDoubleOrNull() ?: return
        val quantity = data.optString("q").toDoubleOrNull() ?: return
        val at = data.optLong("T", System.currentTimeMillis())
        if (price <= 0.0 || quantity <= 0.0) return
        bitcoinPrice = price
        bitcoinTrades.addLast(
            MicroTrade(at, price, price * quantity, aggressiveBuy = !data.optBoolean("m", true))
        )
        val cutoff = at - HISTORY_MILLIS
        while (bitcoinTrades.isNotEmpty() && bitcoinTrades.peekFirst().at < cutoff) {
            bitcoinTrades.removeFirst()
        }
    }

    private fun evaluate(now: Long, currentPrice: Double) {
        val cutoff = now - HISTORY_MILLIS
        while (trades.isNotEmpty() && trades.peekFirst().at < cutoff) trades.removeFirst()
        while (bitcoinTrades.isNotEmpty() && bitcoinTrades.peekFirst().at < cutoff) bitcoinTrades.removeFirst()
        if (now - lastSavedAt < SAVE_INTERVAL_MILLIS) return
        lastSavedAt = now

        val five = trades.filter { it.at >= now - 5_000L }
        val fifteen = trades.filter { it.at >= now - 15_000L }
        val sixty = trades.filter { it.at >= now - 60_000L }
        val fiveMinutes = trades.toList()
        val buy5 = five.filter { it.aggressiveBuy }.sumOf { it.notional }
        val sell5 = five.filterNot { it.aggressiveBuy }.sumOf { it.notional }
        val buy15 = fifteen.filter { it.aggressiveBuy }.sumOf { it.notional }
        val sell15 = fifteen.filterNot { it.aggressiveBuy }.sumOf { it.notional }
        val buy60 = sixty.filter { it.aggressiveBuy }.sumOf { it.notional }
        val sell60 = sixty.filterNot { it.aggressiveBuy }.sumOf { it.notional }
        val buy5m = fiveMinutes.filter { it.aggressiveBuy }.sumOf { it.notional }
        val sell5m = fiveMinutes.filterNot { it.aggressiveBuy }.sumOf { it.notional }
        val buyRatio5 = ratio(buy5, sell5)
        val buyRatio15 = ratio(buy15, sell15)
        val buyRatio60 = ratio(buy60, sell60)
        val buyRatio5m = ratio(buy5m, sell5m)
        val expectedFiveSecondTrades = max(sixty.size / 12.0, fiveMinutes.size / 60.0).coerceAtLeast(1.0)
        val tradeAcceleration = five.size / expectedFiveSecondTrades
        val oldPrice = sixty.firstOrNull()?.price ?: currentPrice
        val change60 = if (oldPrice > 0.0) (currentPrice / oldPrice - 1.0) * 100.0 else 0.0
        val spread = if (bestBid > 0.0 && bestAsk > bestBid) {
            (bestAsk - bestBid) / ((bestAsk + bestBid) / 2.0) * 100.0
        } else null
        val bookTotal = bestBid * bidQuantity + bestAsk * askQuantity
        val bookImbalance = if (bookTotal > 0.0) {
            (bestBid * bidQuantity - bestAsk * askQuantity) / bookTotal
        } else null
        val btc15 = bitcoinTrades.filter { it.at >= now - 15_000L }
        val btc60 = bitcoinTrades.filter { it.at >= now - 60_000L }
        val btcBuy15 = btc15.filter { it.aggressiveBuy }.sumOf { it.notional }
        val btcSell15 = btc15.filterNot { it.aggressiveBuy }.sumOf { it.notional }
        val btcBuy60 = btc60.filter { it.aggressiveBuy }.sumOf { it.notional }
        val btcSell60 = btc60.filterNot { it.aggressiveBuy }.sumOf { it.notional }
        val btcOldPrice = btc60.firstOrNull()?.price ?: bitcoinPrice
        val btcChange60 = if (btcOldPrice > 0.0 && bitcoinPrice > 0.0) {
            (bitcoinPrice / btcOldPrice - 1.0) * 100.0
        } else 0.0

        val warmedUp = connectedAt > 0L && now - connectedAt >= WARMUP_MILLIS
        val ignition = warmedUp && five.size >= 8 && tradeAcceleration >= 2.5 &&
            buyRatio5 >= 0.62 && buyRatio15 >= 0.58 && change60 >= 0.15
        if (ignition && ignitionAt == 0L) {
            ignitionAt = now
            ignitionPrice = currentPrice
        }
        if (ignitionAt > 0L && now - ignitionAt > CONFIRMATION_WINDOW_MILLIS) {
            ignitionAt = 0L
            ignitionPrice = 0.0
        }
        val confirming = ignitionAt > 0L && now - ignitionAt <= CONFIRMATION_WINDOW_MILLIS &&
            currentPrice >= ignitionPrice * 0.999 && buyRatio15 >= 0.55
        val pressure = five.size >= 4 && tradeAcceleration >= 1.5 &&
            buyRatio15 >= 0.56 && change60 > -0.10
        val phase = when {
            !warmedUp -> "WARMING UP"
            confirming && now - ignitionAt >= 10_000L -> "CONFIRMATION"
            ignition -> "IGNITION"
            pressure -> "PRESSURE"
            else -> "CALM"
        }
        val score = (
            (tradeAcceleration.coerceIn(0.0, 4.0) / 4.0 * 35.0) +
                ((buyRatio15 - 0.5).coerceIn(0.0, 0.3) / 0.3 * 35.0) +
                (change60.coerceIn(0.0, 1.0) * 20.0) +
                ((bookImbalance ?: 0.0).coerceIn(0.0, 0.5) / 0.5 * 10.0)
            ).toInt().coerceIn(0, 100)
        val largeFlow = LargeFlowFingerprintPolicy.evaluate(trades.toList(), now, currentPrice)

        val snapshot = MicroImpulseSnapshot(
                connected = true,
                phase = phase,
                score = score,
                updatedAt = now,
                priceUsdt = currentPrice,
                trades5s = five.size,
                trades60s = sixty.size,
                tradeAcceleration = tradeAcceleration,
                aggressiveBuyPercent5s = buyRatio5 * 100.0,
                aggressiveBuyPercent15s = buyRatio15 * 100.0,
                aggressiveBuyPercent60s = buyRatio60 * 100.0,
                aggressiveBuyPercent5m = buyRatio5m * 100.0,
                buyNotional60s = buy60,
                sellNotional60s = sell60,
                priceChange60sPercent = change60,
                spreadPercent = spread,
                topBookImbalance = bookImbalance,
                bitcoinPriceUsdt = bitcoinPrice,
                bitcoinAggressiveBuyPercent15s = ratio(btcBuy15, btcSell15) * 100.0,
                bitcoinAggressiveBuyPercent60s = ratio(btcBuy60, btcSell60) * 100.0,
                bitcoinPriceChange60sPercent = btcChange60,
                largeFlow = largeFlow,
                error = ""
            )
        MicroImpulseStore.save(appContext, snapshot)
        LiveMarketBreathingStore.append(appContext, snapshot)
    }

    private fun ratio(buy: Double, sell: Double): Double {
        val total = buy + sell
        return if (total > 0.0) buy / total else 0.5
    }

    private companion object {
        const val STREAM_URL = "wss://stream.binance.com:9443/stream?streams=pumpusdt@aggTrade/pumpusdt@bookTicker/btcusdt@aggTrade"
        const val HISTORY_MILLIS = 300_000L
        const val SAVE_INTERVAL_MILLIS = 15_000L
        const val WARMUP_MILLIS = 60_000L
        const val CONFIRMATION_WINDOW_MILLIS = 3L * 60L * 1000L
        const val RECONNECT_MILLIS = 5_000L
    }
}

data class MicroTrade(
    val at: Long,
    val price: Double,
    val notional: Double,
    val aggressiveBuy: Boolean
)

data class MicroImpulseSnapshot(
    val connected: Boolean = false,
    val phase: String = "OFFLINE",
    val score: Int = 0,
    val updatedAt: Long = 0L,
    val priceUsdt: Double = 0.0,
    val trades5s: Int = 0,
    val trades60s: Int = 0,
    val tradeAcceleration: Double = 0.0,
    val aggressiveBuyPercent5s: Double = 50.0,
    val aggressiveBuyPercent15s: Double = 50.0,
    val aggressiveBuyPercent60s: Double = 50.0,
    val aggressiveBuyPercent5m: Double = 50.0,
    val buyNotional60s: Double = 0.0,
    val sellNotional60s: Double = 0.0,
    val priceChange60sPercent: Double = 0.0,
    val spreadPercent: Double? = null,
    val topBookImbalance: Double? = null,
    val bitcoinPriceUsdt: Double = 0.0,
    val bitcoinAggressiveBuyPercent15s: Double = 50.0,
    val bitcoinAggressiveBuyPercent60s: Double = 50.0,
    val bitcoinPriceChange60sPercent: Double = 0.0,
    val largeFlow: LargeFlowFingerprint = LargeFlowFingerprint(),
    val error: String = ""
)

object MicroImpulseStore {
    private const val PREFS = "micro_impulse_shadow_v1"

    fun state(context: Context): MicroImpulseSnapshot {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return MicroImpulseSnapshot(
            connected = p.getBoolean("connected", false),
            phase = p.getString("phase", "OFFLINE").orEmpty(),
            score = p.getInt("score", 0),
            updatedAt = p.getLong("updated_at", 0L),
            priceUsdt = p.double("price", 0.0),
            trades5s = p.getInt("trades_5s", 0),
            trades60s = p.getInt("trades_60s", 0),
            tradeAcceleration = p.double("acceleration", 0.0),
            aggressiveBuyPercent5s = p.double("buy_5s", 50.0),
            aggressiveBuyPercent15s = p.double("buy_15s", 50.0),
            aggressiveBuyPercent60s = p.double("buy_60s", 50.0),
            aggressiveBuyPercent5m = p.double("buy_5m", 50.0),
            buyNotional60s = p.double("buy_notional_60s", 0.0),
            sellNotional60s = p.double("sell_notional_60s", 0.0),
            priceChange60sPercent = p.double("change_60s", 0.0),
            spreadPercent = p.nullableDouble("spread"),
            topBookImbalance = p.nullableDouble("book_imbalance"),
            bitcoinPriceUsdt = p.double("btc_price", 0.0),
            bitcoinAggressiveBuyPercent15s = p.double("btc_buy_15s", 50.0),
            bitcoinAggressiveBuyPercent60s = p.double("btc_buy_60s", 50.0),
            bitcoinPriceChange60sPercent = p.double("btc_change_60s", 0.0),
            largeFlow = runCatching {
                LargeFlowFingerprint.fromJson(JSONObject(p.getString("large_flow_json", null).orEmpty()))
            }.getOrDefault(LargeFlowFingerprint()),
            error = p.getString("error", "").orEmpty()
        )
    }

    fun save(context: Context, value: MicroImpulseSnapshot) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("connected", value.connected)
            .putString("phase", value.phase)
            .putInt("score", value.score)
            .putLong("updated_at", value.updatedAt)
            .putDouble("price", value.priceUsdt)
            .putInt("trades_5s", value.trades5s)
            .putInt("trades_60s", value.trades60s)
            .putDouble("acceleration", value.tradeAcceleration)
            .putDouble("buy_5s", value.aggressiveBuyPercent5s)
            .putDouble("buy_15s", value.aggressiveBuyPercent15s)
            .putDouble("buy_60s", value.aggressiveBuyPercent60s)
            .putDouble("buy_5m", value.aggressiveBuyPercent5m)
            .putDouble("buy_notional_60s", value.buyNotional60s)
            .putDouble("sell_notional_60s", value.sellNotional60s)
            .putDouble("change_60s", value.priceChange60sPercent)
            .putNullableDouble("spread", value.spreadPercent)
            .putNullableDouble("book_imbalance", value.topBookImbalance)
            .putDouble("btc_price", value.bitcoinPriceUsdt)
            .putDouble("btc_buy_15s", value.bitcoinAggressiveBuyPercent15s)
            .putDouble("btc_buy_60s", value.bitcoinAggressiveBuyPercent60s)
            .putDouble("btc_change_60s", value.bitcoinPriceChange60sPercent)
            .putString("large_flow_json", value.largeFlow.toJson().toString())
            .putString("error", value.error)
            .apply()
    }

    fun markConnecting(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("connected", false).putString("phase", "CONNECTING").apply()
    }

    fun markConnected(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("connected", true).putString("phase", "CALM").putString("error", "").apply()
    }

    fun markDisconnected(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("connected", false).putString("phase", "OFFLINE").apply()
    }

    fun markError(context: Context, error: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("connected", false).putString("phase", "RECONNECTING")
            .putString("error", error.take(240)).apply()
    }

    private fun android.content.SharedPreferences.double(key: String, default: Double): Double =
        if (contains(key)) java.lang.Double.longBitsToDouble(getLong(key, 0L)) else default

    private fun android.content.SharedPreferences.nullableDouble(key: String): Double? =
        if (contains(key)) java.lang.Double.longBitsToDouble(getLong(key, 0L)) else null

    private fun android.content.SharedPreferences.Editor.putDouble(
        key: String,
        value: Double
    ): android.content.SharedPreferences.Editor = putLong(key, java.lang.Double.doubleToRawLongBits(value))

    private fun android.content.SharedPreferences.Editor.putNullableDouble(
        key: String,
        value: Double?
    ): android.content.SharedPreferences.Editor = if (value == null) remove(key) else putDouble(key, value)
}
