package com.example.pumppaperbot

import android.content.Context
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs

/**
 * V6.0 shadow-only execution intelligence.
 *
 * The existing Pump strategies keep full trading authority. This layer only measures whether
 * Binance-led flow agrees with the real Bitpanda Fusion execution book and how much gross move is
 * required to overcome current round-trip fees, spread and estimated depth slippage.
 */
data class FusionBookMetricsV600(
    val top3BidEur: Double = 0.0,
    val top3AskEur: Double = 0.0,
    val top5BidEur: Double = 0.0,
    val top5AskEur: Double = 0.0,
    val imbalance3: Double? = null,
    val imbalance5: Double? = null,
    val microprice: Double? = null,
    val micropriceBiasBps: Double? = null,
    val buySlippage500Bps: Double? = null,
    val sellSlippage500Bps: Double? = null
)

data class FusionBookMemoryV600(
    val at: Long = 0L,
    val top5BidEur: Double = 0.0,
    val top5AskEur: Double = 0.0,
    val spreadBps: Double = 0.0,
    val microprice: Double = 0.0
) {
    fun toJson() = JSONObject()
        .put("at", at)
        .put("top5BidEur", top5BidEur)
        .put("top5AskEur", top5AskEur)
        .put("spreadBps", spreadBps)
        .put("microprice", microprice)

    companion object {
        fun fromJson(value: JSONObject?): FusionBookMemoryV600 {
            if (value == null) return FusionBookMemoryV600()
            return FusionBookMemoryV600(
                at = value.optLong("at"),
                top5BidEur = value.optDouble("top5BidEur"),
                top5AskEur = value.optDouble("top5AskEur"),
                spreadBps = value.optDouble("spreadBps"),
                microprice = value.optDouble("microprice")
            )
        }
    }
}

data class ScalpExecutionSnapshotV600(
    val at: Long = 0L,
    val trigger: String = "NONE",
    val agreement: String = "INSUFFICIENT_DATA",
    val executionScore: Int = 0,
    val shadowOnly: Boolean = true,
    val feeTier: String = FusionTradingCosts.FEE_TIER,
    val feeBpsPerSide: Double = FusionTradingCosts.FEE_RATE * 10_000.0,
    val spreadBps: Double = 0.0,
    val buySlippage500Bps: Double? = null,
    val sellSlippage500Bps: Double? = null,
    val costFloorBps: Double? = null,
    val imbalance3: Double? = null,
    val imbalance5: Double? = null,
    val micropriceBiasBps: Double? = null,
    val bidDepthChangePercent: Double? = null,
    val askDepthChangePercent: Double? = null,
    val aggressiveBuy15s: Double = 50.0,
    val aggressiveBuy60s: Double = 50.0,
    val tradeAcceleration: Double = 0.0,
    val priceChange60sPercent: Double = 0.0,
    val instantScore: Int? = null,
    val flow5m: Int? = null,
    val flow15m: Int? = null,
    val flow30m: Int? = null,
    val bid: Double = 0.0,
    val ask: Double = 0.0,
    val reason: String = "V6 ещё не получил достаточные данные"
) {
    fun toJson() = JSONObject()
        .put("at", at)
        .put("trigger", trigger)
        .put("agreement", agreement)
        .put("executionScore", executionScore)
        .put("shadowOnly", shadowOnly)
        .put("feeTier", feeTier)
        .put("feeBpsPerSide", feeBpsPerSide)
        .put("spreadBps", spreadBps)
        .putNullable("buySlippage500Bps", buySlippage500Bps)
        .putNullable("sellSlippage500Bps", sellSlippage500Bps)
        .putNullable("costFloorBps", costFloorBps)
        .putNullable("imbalance3", imbalance3)
        .putNullable("imbalance5", imbalance5)
        .putNullable("micropriceBiasBps", micropriceBiasBps)
        .putNullable("bidDepthChangePercent", bidDepthChangePercent)
        .putNullable("askDepthChangePercent", askDepthChangePercent)
        .put("aggressiveBuy15s", aggressiveBuy15s)
        .put("aggressiveBuy60s", aggressiveBuy60s)
        .put("tradeAcceleration", tradeAcceleration)
        .put("priceChange60sPercent", priceChange60sPercent)
        .putNullable("instantScore", instantScore)
        .putNullable("flow5m", flow5m)
        .putNullable("flow15m", flow15m)
        .putNullable("flow30m", flow30m)
        .put("bid", bid)
        .put("ask", ask)
        .put("reason", reason.take(600))

    fun compactText(): String = buildString {
        append("V6 EXECUTION • $agreement • $executionScore/100")
        append("\nfee ${fmt(feeBpsPerSide)} bp/side • spread ${fmt(spreadBps)} bp")
        costFloorBps?.let { append(" • cost floor ${fmt(it)} bp") }
        append("\nimb5 ${imbalance5?.let { fmtSigned(it * 100.0) } ?: "—"}%")
        append(" • micro ${micropriceBiasBps?.let(::fmtSigned) ?: "—"} bp")
        append(" • Binance buy15 ${fmt(aggressiveBuy15s)}%")
        append("\nSHADOW: не разрешает и не запрещает сделки • $reason")
    }

    companion object {
        fun fromJson(value: JSONObject?): ScalpExecutionSnapshotV600 {
            if (value == null) return ScalpExecutionSnapshotV600()
            return ScalpExecutionSnapshotV600(
                at = value.optLong("at"),
                trigger = value.optString("trigger", "NONE"),
                agreement = value.optString("agreement", "INSUFFICIENT_DATA"),
                executionScore = value.optInt("executionScore"),
                shadowOnly = value.optBoolean("shadowOnly", true),
                feeTier = value.optString("feeTier", FusionTradingCosts.FEE_TIER),
                feeBpsPerSide = value.optDouble("feeBpsPerSide", FusionTradingCosts.FEE_RATE * 10_000.0),
                spreadBps = value.optDouble("spreadBps"),
                buySlippage500Bps = value.nullableDouble("buySlippage500Bps"),
                sellSlippage500Bps = value.nullableDouble("sellSlippage500Bps"),
                costFloorBps = value.nullableDouble("costFloorBps"),
                imbalance3 = value.nullableDouble("imbalance3"),
                imbalance5 = value.nullableDouble("imbalance5"),
                micropriceBiasBps = value.nullableDouble("micropriceBiasBps"),
                bidDepthChangePercent = value.nullableDouble("bidDepthChangePercent"),
                askDepthChangePercent = value.nullableDouble("askDepthChangePercent"),
                aggressiveBuy15s = value.optDouble("aggressiveBuy15s", 50.0),
                aggressiveBuy60s = value.optDouble("aggressiveBuy60s", 50.0),
                tradeAcceleration = value.optDouble("tradeAcceleration"),
                priceChange60sPercent = value.optDouble("priceChange60sPercent"),
                instantScore = value.nullableInt("instantScore"),
                flow5m = value.nullableInt("flow5m"),
                flow15m = value.nullableInt("flow15m"),
                flow30m = value.nullableInt("flow30m"),
                bid = value.optDouble("bid"),
                ask = value.optDouble("ask"),
                reason = value.optString("reason")
            )
        }
    }
}

object ScalpExecutionPolicyV600 {
    const val SAMPLE_NOTIONAL_EUR = 500.0
    private const val MEMORY_MAX_AGE_MILLIS = 90_000L
    private const val SAFETY_BUFFER_BPS = 10.0

    fun bookMetrics(market: FusionMarketSnapshot): FusionBookMetricsV600 {
        val bids = market.bidLevels
        val asks = market.askLevels
        val top3Bid = notional(bids.take(3))
        val top3Ask = notional(asks.take(3))
        val top5Bid = notional(bids.take(5)).takeIf { it > 0.0 } ?: market.bidDepthEur
        val top5Ask = notional(asks.take(5)).takeIf { it > 0.0 } ?: market.askDepthEur
        val bid1Qty = bids.firstOrNull()?.quantity ?: 0.0
        val ask1Qty = asks.firstOrNull()?.quantity ?: 0.0
        val microprice = if (market.bid > 0.0 && market.ask > 0.0 && bid1Qty + ask1Qty > 0.0) {
            (market.ask * bid1Qty + market.bid * ask1Qty) / (bid1Qty + ask1Qty)
        } else null
        val mid = market.mid.takeIf { it > 0.0 }
        val bias = if (microprice != null && mid != null) (microprice / mid - 1.0) * 10_000.0 else null
        return FusionBookMetricsV600(
            top3BidEur = top3Bid,
            top3AskEur = top3Ask,
            top5BidEur = top5Bid,
            top5AskEur = top5Ask,
            imbalance3 = imbalance(top3Bid, top3Ask),
            imbalance5 = imbalance(top5Bid, top5Ask),
            microprice = microprice,
            micropriceBiasBps = bias,
            buySlippage500Bps = buySlippageBps(asks, SAMPLE_NOTIONAL_EUR, market.ask),
            sellSlippage500Bps = sellSlippageBps(bids, SAMPLE_NOTIONAL_EUR, market.bid)
        )
    }

    fun evaluate(
        market: FusionMarketSnapshot,
        micro: MicroImpulseSnapshot,
        breathing: LiveMarketBreathingSnapshot,
        previous: FusionBookMemoryV600?,
        trigger: String,
        now: Long
    ): Pair<ScalpExecutionSnapshotV600, FusionBookMemoryV600> {
        val metrics = bookMetrics(market)
        val spreadBps = market.spreadPercent.coerceAtLeast(0.0) * 100.0
        val priorFresh = previous?.takeIf { it.at > 0L && now - it.at in 0..MEMORY_MAX_AGE_MILLIS }
        val bidChange = percentChange(priorFresh?.top5BidEur, metrics.top5BidEur)
        val askChange = percentChange(priorFresh?.top5AskEur, metrics.top5AskEur)
        val feeBps = market.feeRate.coerceAtLeast(0.0) * 10_000.0
        val costFloor = if (market.fresh(now) && metrics.buySlippage500Bps != null && metrics.sellSlippage500Bps != null) {
            feeBps * 2.0 + spreadBps + metrics.buySlippage500Bps + metrics.sellSlippage500Bps + SAFETY_BUFFER_BPS
        } else null
        val flow = breathing.flowWave.latest
        val flow5 = flow?.score5m ?: breathing.horizons.firstOrNull { it.minutes == 5 }?.score
        val flow15 = flow?.score15m ?: breathing.horizons.firstOrNull { it.minutes == 15 }?.score
        val flow30 = flow?.score30m ?: breathing.horizons.firstOrNull { it.minutes == 30 }?.score
        val binancePositive = micro.connected &&
            micro.aggressiveBuyPercent15s >= 56.0 &&
            micro.aggressiveBuyPercent60s >= 53.0 &&
            micro.tradeAcceleration >= 1.20 &&
            micro.priceChange60sPercent > -0.10
        val fusionPositive = (metrics.imbalance5 ?: 0.0) >= 0.08 ||
            (metrics.micropriceBiasBps ?: 0.0) >= 1.5 ||
            (askChange != null && askChange <= -12.0 && (bidChange ?: 0.0) >= -5.0)
        val fusionNegative = (metrics.imbalance5 ?: 0.0) <= -0.12 ||
            (metrics.micropriceBiasBps ?: 0.0) <= -2.0 ||
            (askChange != null && askChange >= 20.0 && (bidChange ?: 0.0) <= 0.0)
        val badExecution = market.fresh(now) && (
            spreadBps > 50.0 ||
                metrics.buySlippage500Bps == null ||
                metrics.sellSlippage500Bps == null ||
                (metrics.buySlippage500Bps ?: 0.0) > 35.0 ||
                (metrics.sellSlippage500Bps ?: 0.0) > 35.0
            )
        val agreement = when {
            !market.fresh(now) || !micro.connected || !breathing.fresh -> "INSUFFICIENT_DATA"
            badExecution -> "BAD_EXECUTION"
            binancePositive && fusionPositive -> "CONFIRMED"
            binancePositive && fusionNegative -> "DIVERGENT"
            binancePositive -> "LEADING"
            fusionPositive && (breathing.instantScore ?: 0) > 0 -> "FUSION_LEADING"
            else -> "NEUTRAL"
        }
        var score = 50.0
        score += (metrics.imbalance5 ?: 0.0).coerceIn(-0.5, 0.5) * 30.0
        score += ((metrics.micropriceBiasBps ?: 0.0) / 5.0).coerceIn(-1.0, 1.0) * 10.0
        if (askChange != null && askChange < 0.0) score += (-askChange / 25.0).coerceIn(0.0, 1.0) * 8.0
        if (bidChange != null && bidChange > 0.0) score += (bidChange / 25.0).coerceIn(0.0, 1.0) * 8.0
        score += ((micro.aggressiveBuyPercent15s - 50.0) / 15.0).coerceIn(-1.0, 1.0) * 8.0
        score -= (spreadBps / 50.0).coerceIn(0.0, 1.5) * 20.0
        score -= (((metrics.buySlippage500Bps ?: 40.0) + (metrics.sellSlippage500Bps ?: 40.0)) / 50.0)
            .coerceIn(0.0, 1.5) * 12.0
        score += when (agreement) {
            "CONFIRMED" -> 15.0
            "LEADING", "FUSION_LEADING" -> 5.0
            "DIVERGENT" -> -20.0
            "BAD_EXECUTION" -> -30.0
            "INSUFFICIENT_DATA" -> -50.0
            else -> 0.0
        }
        val reason = when (agreement) {
            "CONFIRMED" -> "Binance flow и Bitpanda execution-book смотрят в одну сторону"
            "LEADING" -> "Binance уже ускоряется; Bitpanda ещё не дал сильного подтверждения"
            "FUSION_LEADING" -> "Bitpanda book улучшился раньше выраженного Binance-flow"
            "DIVERGENT" -> "Binance BUY-flow есть, но Bitpanda book показывает встречное давление"
            "BAD_EXECUTION" -> "движение может быть, но spread/depth делают исполнение дорогим"
            "INSUFFICIENT_DATA" -> "нет одновременно свежих flow и Bitpanda execution-данных"
            else -> "явного согласованного преимущества исполнения пока нет"
        }
        val snapshot = ScalpExecutionSnapshotV600(
            at = now,
            trigger = trigger.take(120),
            agreement = agreement,
            executionScore = score.toInt().coerceIn(0, 100),
            shadowOnly = true,
            feeTier = market.feeTier,
            feeBpsPerSide = feeBps,
            spreadBps = spreadBps,
            buySlippage500Bps = metrics.buySlippage500Bps,
            sellSlippage500Bps = metrics.sellSlippage500Bps,
            costFloorBps = costFloor,
            imbalance3 = metrics.imbalance3,
            imbalance5 = metrics.imbalance5,
            micropriceBiasBps = metrics.micropriceBiasBps,
            bidDepthChangePercent = bidChange,
            askDepthChangePercent = askChange,
            aggressiveBuy15s = micro.aggressiveBuyPercent15s,
            aggressiveBuy60s = micro.aggressiveBuyPercent60s,
            tradeAcceleration = micro.tradeAcceleration,
            priceChange60sPercent = micro.priceChange60sPercent,
            instantScore = breathing.instantScore,
            flow5m = flow5,
            flow15m = flow15,
            flow30m = flow30,
            bid = market.bid,
            ask = market.ask,
            reason = reason
        )
        val memory = FusionBookMemoryV600(
            at = now,
            top5BidEur = metrics.top5BidEur,
            top5AskEur = metrics.top5AskEur,
            spreadBps = spreadBps,
            microprice = metrics.microprice ?: 0.0
        )
        return snapshot to memory
    }

    internal fun buySlippageBps(levels: List<FusionBookLevel>, quoteEur: Double, bestAsk: Double): Double? {
        if (quoteEur <= 0.0 || bestAsk <= 0.0 || levels.isEmpty()) return null
        var remaining = quoteEur
        var base = 0.0
        levels.forEach { level ->
            if (remaining <= 1e-9) return@forEach
            if (level.price <= 0.0 || level.quantity <= 0.0) return@forEach
            val availableQuote = level.price * level.quantity
            val spend = minOf(remaining, availableQuote)
            base += spend / level.price
            remaining -= spend
        }
        if (remaining > 0.01 || base <= 0.0) return null
        val average = quoteEur / base
        return ((average / bestAsk - 1.0) * 10_000.0).coerceAtLeast(0.0)
    }

    internal fun sellSlippageBps(levels: List<FusionBookLevel>, quoteEur: Double, bestBid: Double): Double? {
        if (quoteEur <= 0.0 || bestBid <= 0.0 || levels.isEmpty()) return null
        val targetBase = quoteEur / bestBid
        var remainingBase = targetBase
        var proceeds = 0.0
        levels.forEach { level ->
            if (remainingBase <= 1e-12) return@forEach
            if (level.price <= 0.0 || level.quantity <= 0.0) return@forEach
            val sold = minOf(remainingBase, level.quantity)
            proceeds += sold * level.price
            remainingBase -= sold
        }
        if (remainingBase > targetBase * 0.0001 || targetBase <= 0.0) return null
        val average = proceeds / targetBase
        return ((1.0 - average / bestBid) * 10_000.0).coerceAtLeast(0.0)
    }

    private fun notional(levels: List<FusionBookLevel>) = levels.sumOf { it.price * it.quantity }

    private fun imbalance(bid: Double, ask: Double): Double? {
        val total = bid + ask
        return if (total > 0.0) (bid - ask) / total else null
    }

    private fun percentChange(previous: Double?, current: Double): Double? {
        if (previous == null || previous <= 0.0 || current < 0.0) return null
        return (current / previous - 1.0) * 100.0
    }
}

object ScalpExecutionIntelligenceStoreV600 {
    private const val PREFS = "scalp_execution_intelligence_v600"
    private const val KEY_STATE = "state"
    private const val KEY_MEMORY = "book_memory"
    private const val KEY_LAST_LOGGED_AGREEMENT = "last_logged_agreement"

    @Synchronized
    fun observe(
        context: Context,
        trigger: String,
        now: Long = System.currentTimeMillis()
    ): ScalpExecutionSnapshotV600 {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = runCatching {
            FusionBookMemoryV600.fromJson(JSONObject(prefs.getString(KEY_MEMORY, "{}").orEmpty()))
        }.getOrDefault(FusionBookMemoryV600())
        val result = ScalpExecutionPolicyV600.evaluate(
            market = BitpandaFusionStore.state(context),
            micro = MicroImpulseStore.state(context),
            breathing = LiveMarketBreathingStore.snapshot(context, now),
            previous = previous,
            trigger = trigger,
            now = now
        )
        prefs.edit()
            .putString(KEY_STATE, result.first.toJson().toString())
            .putString(KEY_MEMORY, result.second.toJson().toString())
            .apply()
        V6ScalpReportStore.append(context, result.first)
        val lastAgreement = prefs.getString(KEY_LAST_LOGGED_AGREEMENT, "").orEmpty()
        if (result.first.agreement != lastAgreement || trigger != "BASELINE") {
            UnifiedResearchLog.record(
                context,
                "V6_EXECUTION_SHADOW",
                result.first.agreement,
                "${result.first.trigger}; score=${result.first.executionScore}; " +
                    "costFloor=${result.first.costFloorBps?.let { String.format(Locale.US, "%.1f", it) } ?: "NA"}bp; " +
                    result.first.reason,
                now
            )
            prefs.edit().putString(KEY_LAST_LOGGED_AGREEMENT, result.first.agreement).apply()
        }
        return result.first
    }

    fun current(context: Context): ScalpExecutionSnapshotV600 = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_STATE, "{}").orEmpty()
        ScalpExecutionSnapshotV600.fromJson(JSONObject(raw))
    }.getOrDefault(ScalpExecutionSnapshotV600())
}

private fun JSONObject.putNullable(key: String, value: Any?): JSONObject =
    put(key, value ?: JSONObject.NULL)

private fun JSONObject.nullableDouble(key: String): Double? =
    if (!has(key) || isNull(key)) null else optDouble(key).takeIf(Double::isFinite)

private fun JSONObject.nullableInt(key: String): Int? =
    if (!has(key) || isNull(key)) null else optInt(key)

private fun fmt(value: Double) = String.format(Locale.US, "%.1f", value)
private fun fmtSigned(value: Double) = String.format(Locale.US, "%+.1f", value)
