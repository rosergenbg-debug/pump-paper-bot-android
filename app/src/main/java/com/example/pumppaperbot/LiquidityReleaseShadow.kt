package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.roundToInt

enum class LiquidityReleaseState {
    WARMING_UP,
    SELLERS_STRENGTHEN,
    BALANCE,
    SELLERS_EXHAUSTING,
    ASK_RETREAT,
    LIQUIDITY_RELEASED,
    FALSE_RELEASE
}

data class LiquidityReleaseSample(
    val at: Long,
    val buy60s: Double,
    val sell60s: Double,
    val bidNotional: Double,
    val askNotional: Double,
    val price: Double
) {
    fun toJson() = JSONObject()
        .put("at", at).put("buy60s", buy60s).put("sell60s", sell60s)
        .put("bidNotional", bidNotional).put("askNotional", askNotional).put("price", price)

    companion object {
        fun fromJson(value: JSONObject) = LiquidityReleaseSample(
            at = value.optLong("at"),
            buy60s = value.optDouble("buy60s"),
            sell60s = value.optDouble("sell60s"),
            bidNotional = value.optDouble("bidNotional"),
            askNotional = value.optDouble("askNotional"),
            price = value.optDouble("price")
        )
    }
}

data class LiquidityReleaseRuntime(
    val candidateSince: Long = 0L,
    val releasedAt: Long = 0L,
    val previousState: LiquidityReleaseState = LiquidityReleaseState.WARMING_UP
)

data class LiquidityReleaseSnapshot(
    val at: Long = 0L,
    val state: LiquidityReleaseState = LiquidityReleaseState.WARMING_UP,
    val score: Int = 0,
    val confidence: Int = 0,
    val sellDecayPercent: Double = 0.0,
    val buyHoldPercent: Double = 0.0,
    val askThinningPercent: Double = 0.0,
    val bidHoldPercent: Double = 0.0,
    val priceResponsePercent: Double = 0.0,
    val persistenceSeconds: Long = 0L,
    val price: Double = 0.0,
    val buy60sUsdt: Double = 0.0,
    val sell60sUsdt: Double = 0.0,
    val bidNotionalUsdt: Double = 0.0,
    val askNotionalUsdt: Double = 0.0,
    val shadowOnly: Boolean = true,
    val reason: String = "Накопление истории наблюдений.",
    val runtime: LiquidityReleaseRuntime = LiquidityReleaseRuntime()
) {
    fun toJson() = JSONObject()
        .put("at", at).put("state", state.name).put("score", score).put("confidence", confidence)
        .put("sell_decay_percent", sellDecayPercent).put("buy_hold_percent", buyHoldPercent)
        .put("ask_thinning_percent", askThinningPercent).put("bid_hold_percent", bidHoldPercent)
        .put("price_response_percent", priceResponsePercent).put("persistence_seconds", persistenceSeconds)
        .put("price", price).put("buy_60s_usdt", buy60sUsdt).put("sell_60s_usdt", sell60sUsdt)
        .put("bid_notional_usdt", bidNotionalUsdt).put("ask_notional_usdt", askNotionalUsdt)
        .put("shadow_only", shadowOnly).put("reason", reason)
        .put("candidate_since", runtime.candidateSince).put("released_at", runtime.releasedAt)

    companion object {
        fun fromJson(value: JSONObject?) = if (value == null) LiquidityReleaseSnapshot() else LiquidityReleaseSnapshot(
            at = value.optLong("at"),
            state = runCatching { LiquidityReleaseState.valueOf(value.optString("state")) }
                .getOrDefault(LiquidityReleaseState.WARMING_UP),
            score = value.optInt("score"),
            confidence = value.optInt("confidence"),
            sellDecayPercent = value.optDouble("sell_decay_percent"),
            buyHoldPercent = value.optDouble("buy_hold_percent"),
            askThinningPercent = value.optDouble("ask_thinning_percent"),
            bidHoldPercent = value.optDouble("bid_hold_percent"),
            priceResponsePercent = value.optDouble("price_response_percent"),
            persistenceSeconds = value.optLong("persistence_seconds"),
            price = value.optDouble("price"),
            buy60sUsdt = value.optDouble("buy_60s_usdt"),
            sell60sUsdt = value.optDouble("sell_60s_usdt"),
            bidNotionalUsdt = value.optDouble("bid_notional_usdt"),
            askNotionalUsdt = value.optDouble("ask_notional_usdt"),
            shadowOnly = value.optBoolean("shadow_only", true),
            reason = value.optString("reason", "Накопление истории наблюдений."),
            runtime = LiquidityReleaseRuntime(
                candidateSince = value.optLong("candidate_since"),
                releasedAt = value.optLong("released_at"),
                previousState = runCatching { LiquidityReleaseState.valueOf(value.optString("state")) }
                    .getOrDefault(LiquidityReleaseState.WARMING_UP)
            )
        )
    }
}

/** Pure, causal detector. It observes only samples available at [now] and has no trading authority. */
object LiquidityReleaseShadowPolicy {
    private const val CONFIRM_MILLIS = 45_000L
    private const val FALSE_RELEASE_WINDOW = 2L * 60L * 1000L

    fun evaluate(
        samples: List<LiquidityReleaseSample>,
        previous: LiquidityReleaseRuntime,
        now: Long
    ): LiquidityReleaseSnapshot {
        val available = samples.filter { it.at <= now && it.buy60s >= 0.0 && it.sell60s >= 0.0 &&
            it.bidNotional > 0.0 && it.askNotional > 0.0 && it.price > 0.0 }
        val recent = available.filter { now - it.at in 0L..30_000L }
        val baseline = available.filter { now - it.at in 60_000L..120_000L }
        if (recent.size < 2 || baseline.size < 2) {
            val last = available.lastOrNull()
            return LiquidityReleaseSnapshot(
                at = now,
                state = LiquidityReleaseState.WARMING_UP,
                confidence = ((available.size * 100) / 8).coerceIn(0, 75),
                price = last?.price ?: 0.0,
                buy60sUsdt = last?.buy60s ?: 0.0,
                sell60sUsdt = last?.sell60s ?: 0.0,
                bidNotionalUsdt = last?.bidNotional ?: 0.0,
                askNotionalUsdt = last?.askNotional ?: 0.0,
                reason = "Нужно не менее 60–120 секунд свежей ленты и стакана.",
                runtime = previous.copy(candidateSince = 0L, previousState = LiquidityReleaseState.WARMING_UP)
            )
        }

        fun average(items: List<LiquidityReleaseSample>, value: (LiquidityReleaseSample) -> Double) =
            items.map(value).average().coerceAtLeast(0.000001)
        fun changeDown(old: Double, current: Double) = (old - current) / old * 100.0
        fun ratio(current: Double, old: Double) = current / old * 100.0

        val oldSell = average(baseline) { it.sell60s }
        val newSell = average(recent) { it.sell60s }
        val oldBuy = average(baseline) { it.buy60s }
        val newBuy = average(recent) { it.buy60s }
        val oldAsk = average(baseline) { it.askNotional }
        val newAsk = average(recent) { it.askNotional }
        val oldBid = average(baseline) { it.bidNotional }
        val newBid = average(recent) { it.bidNotional }
        val oldPrice = average(baseline) { it.price }
        val newPrice = average(recent) { it.price }

        val sellDecay = changeDown(oldSell, newSell)
        val buyHold = ratio(newBuy, oldBuy)
        val askThin = changeDown(oldAsk, newAsk)
        val bidHold = ratio(newBid, oldBid)
        val priceResponse = (newPrice - oldPrice) / oldPrice * 100.0

        val score = (
            (sellDecay.coerceIn(-30.0, 45.0) / 45.0 * 35.0) +
                (askThin.coerceIn(-20.0, 35.0) / 35.0 * 25.0) +
                ((buyHold - 65.0).coerceIn(-25.0, 55.0) / 55.0 * 15.0) +
                ((bidHold - 65.0).coerceIn(-25.0, 55.0) / 55.0 * 15.0) +
                ((priceResponse + 0.08).coerceIn(-0.30, 0.55) / 0.55 * 10.0)
            ).roundToInt().coerceIn(-100, 100)
        val releaseCandidate = sellDecay >= 18.0 && askThin >= 6.0 && buyHold >= 72.0 &&
            bidHold >= 70.0 && priceResponse >= -0.08 && score >= 45
        val falseRelease = previous.releasedAt > 0L && now - previous.releasedAt <= FALSE_RELEASE_WINDOW &&
            (sellDecay < -8.0 || askThin < -4.0 || newSell > oldSell * 1.15)
        val candidateSince = if (releaseCandidate) {
            previous.candidateSince.takeIf { it > 0L } ?: now
        } else 0L
        val persistence = if (candidateSince > 0L) (now - candidateSince).coerceAtLeast(0L) else 0L

        val state = when {
            falseRelease -> LiquidityReleaseState.FALSE_RELEASE
            releaseCandidate && persistence >= CONFIRM_MILLIS -> LiquidityReleaseState.LIQUIDITY_RELEASED
            askThin >= 8.0 && sellDecay >= 5.0 -> LiquidityReleaseState.ASK_RETREAT
            sellDecay >= 18.0 && buyHold >= 72.0 -> LiquidityReleaseState.SELLERS_EXHAUSTING
            sellDecay < -15.0 || askThin < -10.0 -> LiquidityReleaseState.SELLERS_STRENGTHEN
            else -> LiquidityReleaseState.BALANCE
        }
        val releasedAt = when (state) {
            LiquidityReleaseState.LIQUIDITY_RELEASED -> previous.releasedAt.takeIf { it > 0L } ?: now
            LiquidityReleaseState.FALSE_RELEASE -> 0L
            else -> previous.releasedAt.takeIf { now - it <= FALSE_RELEASE_WINDOW } ?: 0L
        }
        val confidence = (35 + baseline.size * 8 + recent.size * 8 +
            (persistence / 1_000L).coerceAtMost(25L)).toInt().coerceIn(0, 100)
        val reason = when (state) {
            LiquidityReleaseState.SELLERS_STRENGTHEN -> "Продажи или ask-глубина снова растут; освобождение не подтверждено."
            LiquidityReleaseState.BALANCE -> "Приток и отток пока взаимно гасятся; устойчивого дисбаланса нет."
            LiquidityReleaseState.SELLERS_EXHAUSTING -> "Агрессивные продажи уменьшаются, а покупки не исчезают; ждём ухода ask."
            LiquidityReleaseState.ASK_RETREAT -> "Продажи слабеют и ask-глубина уходит; ждём устойчивость 45 секунд и ответ цены."
            LiquidityReleaseState.LIQUIDITY_RELEASED -> "Продажи ослабли, ask не восстановился, bid удерживается и цена принимает движение."
            LiquidityReleaseState.FALSE_RELEASE -> "После видимого ухода ask продавцы или sell-поток вернулись — прежний сигнал отменён."
            LiquidityReleaseState.WARMING_UP -> "Накопление истории наблюдений."
        }
        return LiquidityReleaseSnapshot(
            at = now,
            state = state,
            score = score,
            confidence = confidence,
            sellDecayPercent = sellDecay,
            buyHoldPercent = buyHold,
            askThinningPercent = askThin,
            bidHoldPercent = bidHold,
            priceResponsePercent = priceResponse,
            persistenceSeconds = persistence / 1_000L,
            price = newPrice,
            buy60sUsdt = newBuy,
            sell60sUsdt = newSell,
            bidNotionalUsdt = newBid,
            askNotionalUsdt = newAsk,
            reason = reason,
            runtime = LiquidityReleaseRuntime(candidateSince, releasedAt, state)
        )
    }
}

object LiquidityReleaseShadowStore {
    private const val PREFS = "liquidity_release_shadow_v532"
    private const val DIRECTORY = "liquidity_release_shadow_v532"
    private const val RETENTION_MILLIS = 30L * 24L * 60L * 60L * 1000L
    private const val RAW_LOG_GAP = 60_000L
    private const val SAMPLE_RETENTION = 10L * 60L * 1000L

    @Synchronized
    fun observe(context: Context, observation: SharedFusionEntryObservation, now: Long): LiquidityReleaseSnapshot {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = latest(context)
        if (prefs.getLong("last_bucket", Long.MIN_VALUE) == observation.sampleBucket) return old
        val micro = observation.micro
        val valid = micro != null && observation.bookBidNotional != null && observation.bookAskNotional != null &&
            micro.priceUsdt > 0.0
        val samples = loadSamples(prefs, now).toMutableList()
        if (valid) {
            samples += LiquidityReleaseSample(
                at = now,
                buy60s = micro!!.buyNotional60s,
                sell60s = micro.sellNotional60s,
                bidNotional = observation.bookBidNotional!!,
                askNotional = observation.bookAskNotional!!,
                price = micro.priceUsdt
            )
        }
        val trimmed = samples.filter { now - it.at in 0L..SAMPLE_RETENTION }.takeLast(48)
        val result = LiquidityReleaseShadowPolicy.evaluate(trimmed, old.runtime, now)
        val stateChanged = result.state != old.state
        val lastRawAt = prefs.getLong("last_raw_at", 0L)
        prefs.edit()
            .putLong("last_bucket", observation.sampleBucket)
            .putString("samples", JSONArray(trimmed.map { it.toJson() }).toString())
            .putString("latest", result.toJson().toString())
            .putLong("last_raw_at", if (stateChanged || now - lastRawAt >= RAW_LOG_GAP) now else lastRawAt)
            .apply()
        if (stateChanged || now - lastRawAt >= RAW_LOG_GAP) append(context, result)
        if (stateChanged) {
            UnifiedResearchLog.record(
                context,
                "LIQUIDITY_SHADOW",
                result.state.name,
                "shadow_only=true; score=${result.score}; sellDecay=${result.sellDecayPercent}; " +
                    "askThin=${result.askThinningPercent}; buyHold=${result.buyHoldPercent}; ${result.reason}",
                now
            )
        }
        return result
    }

    fun latest(context: Context): LiquidityReleaseSnapshot = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("latest", null)
        LiquidityReleaseSnapshot.fromJson(JSONObject(raw.orEmpty()))
    }.getOrDefault(LiquidityReleaseSnapshot())

    fun exportJson(context: Context, now: Long = System.currentTimeMillis()): JSONObject {
        val cutoff = now - RETENTION_MILLIS
        val all = ArrayList<LiquidityReleaseSnapshot>()
        val dir = File(context.filesDir, DIRECTORY)
        dir.listFiles()?.filter { it.name.endsWith(".ndjson") && it.lastModified() >= cutoff }
            ?.sortedBy { it.name }?.forEach { file ->
                file.useLines(Charsets.UTF_8) { lines -> lines.forEach { line ->
                    runCatching { LiquidityReleaseSnapshot.fromJson(JSONObject(line)) }.getOrNull()
                        ?.takeIf { it.at >= cutoff }?.let(all::add)
                } }
            }
        val compact = ArrayList<LiquidityReleaseSnapshot>()
        all.sortedBy { it.at }.forEach { item ->
            val prior = compact.lastOrNull()
            if (prior == null || item.state != prior.state || item.at - prior.at >= 5L * 60L * 1000L) {
                compact += item
            }
        }
        return JSONObject()
            .put("shadow_only", true)
            .put("trading_authority", false)
            .put("retention_days", 30)
            .put("raw_count", all.size)
            .put("exported_count", compact.size)
            .put("latest", latest(context).toJson())
            .put("history", JSONArray(compact.map { it.toJson() }))
    }

    private fun loadSamples(prefs: android.content.SharedPreferences, now: Long): List<LiquidityReleaseSample> =
        runCatching {
            val values = JSONArray(prefs.getString("samples", "[]"))
            (0 until values.length()).mapNotNull { values.optJSONObject(it)?.let(LiquidityReleaseSample::fromJson) }
                .filter { now - it.at in 0L..SAMPLE_RETENTION }
        }.getOrDefault(emptyList())

    private fun append(context: Context, value: LiquidityReleaseSnapshot) {
        val dir = File(context.filesDir, DIRECTORY).apply { mkdirs() }
        File(dir, "shadow-${value.at / (24L * 60L * 60L * 1000L)}.ndjson")
            .appendText(value.toJson().toString() + "\n", Charsets.UTF_8)
        val cutoff = value.at - RETENTION_MILLIS
        dir.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
    }
}

object LiquidityReleaseShadowText {
    fun describe(value: LiquidityReleaseSnapshot): String {
        val title = when (value.state) {
            LiquidityReleaseState.WARMING_UP -> "НАБЛЮДЕНИЕ ПРОГРЕВАЕТСЯ"
            LiquidityReleaseState.SELLERS_STRENGTHEN -> "ПРОДАВЦЫ УСИЛИВАЮТСЯ"
            LiquidityReleaseState.BALANCE -> "ПРИТОК И ОТТОК В РАВНОВЕСИИ"
            LiquidityReleaseState.SELLERS_EXHAUSTING -> "ПРОДАВЦЫ ВЫДЫХАЮТСЯ"
            LiquidityReleaseState.ASK_RETREAT -> "ASK УХОДИТ — НУЖНО ПОДТВЕРЖДЕНИЕ"
            LiquidityReleaseState.LIQUIDITY_RELEASED -> "ЛИКВИДНОСТЬ ОСВОБОЖДЕНА"
            LiquidityReleaseState.FALSE_RELEASE -> "ЛОЖНОЕ ОСВОБОЖДЕНИЕ — ПРОДАВЦЫ ВЕРНУЛИСЬ"
        }
        return "$title • ${value.score}/100 • уверенность ${value.confidence}/100\n" +
            "Продажи ${signed(value.sellDecayPercent)}% • ask ${signed(value.askThinningPercent)}% • " +
            "покупки удержали ${value.buyHoldPercent.roundToInt()}% • bid ${value.bidHoldPercent.roundToInt()}%\n" +
            "${value.reason}\nТЕНЕВОЙ ТЕСТ: показатель записывается в лог и не может открыть или закрыть сделку."
    }

    private fun signed(value: Double) = String.format(java.util.Locale.GERMANY, "%+.1f", value)
}
