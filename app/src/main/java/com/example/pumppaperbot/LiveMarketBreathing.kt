package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.ArrayDeque
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class LiveBreathingSample(
    val at: Long,
    val priceUsdt: Double,
    val pumpBuyerPercent: Double,
    val pumpChange60sPercent: Double,
    val bookImbalance: Double?,
    val bitcoinBuyerPercent: Double,
    val bitcoinChange60sPercent: Double,
    val pumpBuyNotional60s: Double = 0.0,
    val pumpSellNotional60s: Double = 0.0,
    val pumpTrades60s: Int = 0,
    val tradeAcceleration: Double = 0.0,
    val bitcoinPriceUsdt: Double = 0.0
)

data class LiveBreathingHorizon(
    val minutes: Int,
    val score: Int?,
    val priceChangePercent: Double?,
    val buyerPercent: Double?,
    val persistencePercent: Int,
    val samples: Int
)

/** One point of the unified completed-minute flow surface. */
data class LiveFlowWavePoint(
    val at: Long,
    val score15m: Int,
    val score30m: Int,
    val score60m: Int,
    val score180m: Int,
    val score360m: Int,
    val score5m: Int = score15m,
    val score20m: Int = (score15m + score30m) / 2
) {
    fun score(minutes: Int): Int = when (minutes) {
        5 -> score5m
        15 -> score15m
        20 -> score20m
        30 -> score30m
        60 -> score60m
        180 -> score180m
        else -> score360m
    }

    fun composite(): Int = (
        score15m * 0.28 + score30m * 0.24 + score60m * 0.22 +
            score180m * 0.16 + score360m * 0.10
        ).roundToInt().coerceIn(-100, 100)
}

data class LiveFlowWave(
    val points: List<LiveFlowWavePoint> = emptyList(),
    val state: String = "НАКАПЛИВАЕМ ЖИВОЙ ПОТОК",
    val guidance: String = "Ждём устойчивые данные 15–30 минут.",
    val staleSeconds: Long = 0L
) {
    val latest: LiveFlowWavePoint? get() = points.lastOrNull()

    fun currentJson(): JSONObject = JSONObject().apply {
        put("state", state)
        put("guidance", guidance)
        put("stale_seconds", staleSeconds)
        latest?.let {
            put("score_15m", it.score15m)
            put("score_5m", it.score5m)
            put("score_20m", it.score20m)
            put("score_30m", it.score30m)
            put("score_1h", it.score60m)
            put("score_3h", it.score180m)
            put("score_6h", it.score360m)
            put("composite", it.composite())
        }
    }
}

data class LiveMarketBreathingSnapshot(
    val updatedAt: Long = 0L,
    val fresh: Boolean = false,
    val historyMinutes: Int = 0,
    val instantScore: Int? = null,
    val normalScore: Int? = null,
    val experimentScore: Int? = null,
    val regime: String = "НАКАПЛИВАЕМ ИСТОРИЮ",
    val horizons: List<LiveBreathingHorizon> = emptyList(),
    val flowWave: LiveFlowWave = LiveFlowWave(),
    val buyerBreath: BuyerBreathSnapshot = BuyerBreathSnapshot()
) {
    fun toJson(): JSONObject = JSONObject()
        .put("updated_at", updatedAt)
        .put("fresh", fresh)
        .put("history_minutes", historyMinutes)
        .put("instant_score", instantScore ?: JSONObject.NULL)
        .put("normal_deepseek_score", normalScore ?: JSONObject.NULL)
        .put("experiment_score", experimentScore ?: JSONObject.NULL)
        .put("normal_experiment_gap_max", LiveMarketBreathingAnalyzer.MAX_EXPERIMENT_GAP)
        .put("regime", regime)
        .put("continuous_flow_wave", flowWave.currentJson())
        .put("buyer_breath_cycle", buyerBreath.toJson())
        .put("horizons", JSONArray(horizons.map { horizon ->
            JSONObject()
                .put("minutes", horizon.minutes)
                .put("score", horizon.score ?: JSONObject.NULL)
                .put("price_change_pct", horizon.priceChangePercent ?: JSONObject.NULL)
                .put("aggressive_buy_pct", horizon.buyerPercent ?: JSONObject.NULL)
                .put("direction_persistence_pct", horizon.persistencePercent)
                .put("samples", horizon.samples)
        }))
}

/**
 * V5.15 keeps V5.14's completed-minute flow semantics but makes the hot path cheap.
 * Multi-minute horizons are stable for the whole forming wall-clock minute; only Instant
 * is allowed to react every few seconds. The store caches the heavy minute snapshot.
 */
object LiveMarketBreathingAnalyzer {
    const val MAX_EXPERIMENT_GAP = 15
    const val MAX_LIVE_AGE_MILLIS = 90_000L
    private val windows = intArrayOf(5, 15, 20, 30, 60, 360)

    fun analyze(samples: List<LiveBreathingSample>, now: Long): LiveMarketBreathingSnapshot {
        val valid = samples.asSequence()
            .filter { it.at in (now - RollingCsvRetention.RETENTION_MILLIS)..now }
            .filter { it.priceUsdt > 0.0 && it.pumpBuyerPercent.isFinite() }
            .sortedBy { it.at }
            .toList()
        if (valid.isEmpty()) return LiveMarketBreathingSnapshot()

        val latest = valid.last()
        val fresh = isFresh(latest, now)
        val instant = instantScore(latest)
        val minuteBuckets = UnifiedFlowEngine.completedMinuteBuckets(valid, now)
        val horizons = windows.map { minutes ->
            val flow = UnifiedFlowEngine.window(
                buckets = minuteBuckets,
                minutes = minutes,
                requireFullCoverage = true
            )
            LiveBreathingHorizon(
                minutes = minutes,
                score = flow.score,
                priceChangePercent = flow.priceChangePercent,
                buyerPercent = flow.buyerPercent,
                persistencePercent = flow.persistencePercent,
                samples = flow.minuteCount
            )
        }

        val normalWeights = mapOf(5 to 0.25, 15 to 0.25, 30 to 0.20, 60 to 0.18, 360 to 0.12)
        val weighted = horizons.mapNotNull { horizon ->
            normalWeights[horizon.minutes]?.let { weight -> weight to horizon }
        }.filter { it.second.score != null }
        val normal = if (weighted.isEmpty()) null else (
            weighted.sumOf { (weight, horizon) -> weight * horizon.score!! } /
                weighted.sumOf { it.first }
            ).roundToInt().coerceIn(-100, 100)
        val experiment = experimentScore(normal, instant)
        val regime = regime(fresh, normal, horizons)
        val historyMinutes = ((latest.at - valid.first().at).coerceAtLeast(0L) / 60_000L)
            .toInt().coerceAtMost(24 * 60)

        val buyerBreathInput = UnifiedFlowEngine.representativeSamplesFromBuckets(minuteBuckets)
            .takeIf { it.size >= 2 } ?: valid
        val buyerBreath = BuyerBreathCycleAnalyzer.analyze(buyerBreathInput, horizons, fresh)
        val flowWave = continuousFlowFromBuckets(minuteBuckets, latest.at, now)

        return LiveMarketBreathingSnapshot(
            updatedAt = latest.at,
            fresh = fresh,
            historyMinutes = historyMinutes,
            instantScore = instant,
            normalScore = normal.takeIf { fresh },
            experimentScore = experiment.takeIf { fresh },
            regime = regime,
            horizons = horizons,
            flowWave = flowWave,
            buyerBreath = buyerBreath
        )
    }

    /** Cheap refresh used between completed-minute boundaries. */
    internal fun refreshLive(
        stable: LiveMarketBreathingSnapshot,
        latest: LiveBreathingSample,
        now: Long
    ): LiveMarketBreathingSnapshot {
        val fresh = isFresh(latest, now)
        val instant = instantScore(latest)
        val experiment = experimentScore(stable.normalScore, instant).takeIf { fresh }
        val staleMillis = (now - latest.at).coerceAtLeast(0L)
        return stable.copy(
            updatedAt = latest.at,
            fresh = fresh,
            instantScore = instant,
            experimentScore = experiment,
            flowWave = stable.flowWave.copy(staleSeconds = staleMillis / 1_000L)
        )
    }

    internal fun continuousFlow(samples: List<LiveBreathingSample>, now: Long): LiveFlowWave {
        if (samples.isEmpty()) return LiveFlowWave()
        val latestSampleAt = samples.maxOfOrNull { it.at } ?: return LiveFlowWave()
        return continuousFlowFromBuckets(
            UnifiedFlowEngine.completedMinuteBuckets(samples, now),
            latestSampleAt,
            now
        )
    }

    /**
     * Build only the six-hour visible path. Earlier minute buckets stay available to the
     * 180/360-minute windows, but we no longer recalculate invisible historical endpoints.
     */
    private fun continuousFlowFromBuckets(
        allBuckets: List<UnifiedMinuteFlow>,
        latestSampleAt: Long,
        now: Long
    ): LiveFlowWave {
        val warmupCutoff = now - 12L * 60L * 60L * 1_000L
        val buckets = allBuckets.dropWhile { it.at < warmupCutoff }
        if (buckets.isEmpty()) {
            val stale = (now - latestSampleAt).coerceAtLeast(0L)
            return LiveFlowWave(staleSeconds = stale / 1_000L)
        }

        val firstVisibleIndex = (buckets.size - 361).coerceAtLeast(0)
        val allPoints = ArrayList<LiveFlowWavePoint>(buckets.size - firstVisibleIndex + 1)
        for (index in firstVisibleIndex..buckets.lastIndex) {
            val bucket = buckets[index]
            allPoints += LiveFlowWavePoint(
                at = bucket.at,
                score5m = UnifiedFlowEngine.historyScore(buckets, index, 5),
                score15m = UnifiedFlowEngine.historyScore(buckets, index, 15),
                score20m = UnifiedFlowEngine.historyScore(buckets, index, 20),
                score30m = UnifiedFlowEngine.historyScore(buckets, index, 30),
                score60m = UnifiedFlowEngine.historyScore(buckets, index, 60),
                score180m = UnifiedFlowEngine.historyScore(buckets, index, 180),
                score360m = UnifiedFlowEngine.historyScore(buckets, index, 360)
            )
        }

        val staleMillis = (now - latestSampleAt).coerceAtLeast(0L)
        if (staleMillis >= 30_000L && allPoints.isNotEmpty()) {
            allPoints += allPoints.last().copy(at = now)
        }
        val stateAndGuidance = flowGuidance(allPoints, staleMillis)
        return LiveFlowWave(
            points = allPoints.takeLast(361),
            state = stateAndGuidance.first,
            guidance = stateAndGuidance.second,
            staleSeconds = staleMillis / 1_000L
        )
    }

    internal fun flowPulse(sample: LiveBreathingSample): Double =
        UnifiedFlowEngine.samplePulse(sample)

    private fun flowGuidance(
        points: List<LiveFlowWavePoint>,
        staleMillis: Long
    ): Pair<String, String> {
        val latest = points.lastOrNull() ?: return "НАКАПЛИВАЕМ ЖИВОЙ ПОТОК" to
            "Ждём устойчивые данные 15–30 минут."
        if (staleMillis > MAX_LIVE_AGE_MILLIS) {
            return "СВЯЗЬ ПРИОСТАНОВЛЕНА • ПОСЛЕДНЯЯ ВОЛНА ЗАФИКСИРОВАНА" to
                "История сохранена без искусственного затухания; новых решений до восстановления потока не принимать."
        }
        val fiveMinutesAgo = points.lastOrNull {
            it.at <= latest.at - 5L * 60L * 1_000L
        } ?: points.first()
        val short = (latest.score15m + latest.score30m) / 2
        val shortBefore = (fiveMinutesAgo.score15m + fiveMinutesAgo.score30m) / 2
        val slope = short - shortBefore
        val long = (latest.score60m + latest.score180m + latest.score360m) / 3
        val recentPeak = points.takeLast(60).maxOfOrNull { it.composite() } ?: latest.composite()
        return when {
            short >= 20 && slope >= 7 && long >= -10 ->
                "ПОКУПАТЕЛЬСКАЯ ВОЛНА НАБИРАЕТ СИЛУ" to
                    "Зона входа только для проверки: дождаться удержания 15/30 мин и не покупать после вертикального отрыва цены."
            short >= 18 && long >= 8 ->
                "ПОКУПАТЕЛИ УДЕРЖИВАЮТ НЕСКОЛЬКО ГОРИЗОНТОВ" to
                    "Открытую позицию можно сопровождать; новый вход — после отката/ретеста, не по одному проценту покупок."
            recentPeak >= 25 && slope <= -10 && short <= 8 ->
                "КОРОТКАЯ ВОЛНА ВЫДЫХАЕТСЯ" to
                    "Подготовить выход и искать подтверждение продажами, ценой, OI и 15/30/60-минутной слабостью."
            short <= -22 && long <= -8 ->
                "ПРОДАВЦЫ ПЕРЕХВАТЫВАЮТ ПОТОК" to
                    "Новый вход не делать; открытую позицию срочно перепроверить по независимым защитным правилам."
            abs(short) < 12 && abs(long) < 12 ->
                "БОКОВИК • ВОЛНЫ ОКОЛО НУЛЯ" to
                    "Ждать, пока 15/30-минутные линии выйдут из шума и более длинный фон не будет против движения."
            slope > 0 ->
                "ПОТОК ПОСТЕПЕННО УЛУЧШАЕТСЯ" to
                    "Это подготовка, не команда BUY: нужен переход коротких волн выше нуля с реакцией цены."
            else ->
                "ПОТОК ПОСТЕПЕННО СЛАБЕЕТ" to
                    "Не реагировать на один тик; следить, станет ли ослабление общим для 15м, 30м и 1ч."
        }
    }

    internal fun instantScore(sample: LiveBreathingSample): Int {
        val buyer = ((sample.pumpBuyerPercent - 50.0) * 4.0).coerceIn(-100.0, 100.0)
        val price = (sample.pumpChange60sPercent * 125.0).coerceIn(-100.0, 100.0)
        val book = ((sample.bookImbalance ?: 0.0) * 100.0).coerceIn(-100.0, 100.0)
        val bitcoin = (
            (sample.bitcoinBuyerPercent - 50.0) * 2.0 +
                sample.bitcoinChange60sPercent * 80.0
            ).coerceIn(-100.0, 100.0)
        return (
            buyer * 0.48 + price * 0.32 + book * 0.10 + bitcoin * 0.10
            ).roundToInt().coerceIn(-100, 100)
    }

    private fun isFresh(latest: LiveBreathingSample, now: Long): Boolean =
        now >= latest.at && now - latest.at <= MAX_LIVE_AGE_MILLIS

    private fun experimentScore(normal: Int?, instant: Int): Int? = normal?.let { stable ->
        val faster = (stable * 0.55 + instant * 0.45).roundToInt()
        faster.coerceIn(stable - MAX_EXPERIMENT_GAP, stable + MAX_EXPERIMENT_GAP)
            .coerceIn(-100, 100)
    }

    private fun regime(
        fresh: Boolean,
        normal: Int?,
        horizons: List<LiveBreathingHorizon>
    ): String = when {
        !fresh -> "ИСТОРИЯ УСТАРЕЛА — ЖДЁМ ЖИВОЙ ПОТОК"
        normal == null -> "НАКАПЛИВАЕМ ИСТОРИЮ"
        normal >= 35 && horizons.count { (it.score ?: 0) >= 20 } >= 2 ->
            "УСТОЙЧИВОЕ ДАВЛЕНИЕ ПОКУПАТЕЛЕЙ"
        normal <= -35 && horizons.count { (it.score ?: 0) <= -20 } >= 2 ->
            "УСТОЙЧИВОЕ ДАВЛЕНИЕ ПРОДАВЦОВ"
        abs(normal) < 15 -> "РЫНОЧНЫЙ ШУМ / БОКОВИК"
        normal > 0 -> "УМЕРЕННОЕ УЛУЧШЕНИЕ"
        else -> "УМЕРЕННОЕ УХУДШЕНИЕ"
    }
}

object LiveMarketBreathingStore {
    private const val FILE_NAME = "pump_live_breathing_v415.csv"
    private const val PRUNE_INTERVAL_MILLIS = 60L * 60L * 1000L
    private val samples = ArrayDeque<LiveBreathingSample>()
    private var initialized = false
    private var lastPrunedAt = 0L

    // V5.15: the expensive 5/15/20/30/60/360 history changes only when a wall-clock
    // minute closes. UI callers may ask every two seconds, but they now receive a cheap
    // Instant refresh instead of rebuilding hours of history every time.
    private var cachedCurrentMinute = Long.MIN_VALUE
    private var cachedFreshState: Boolean? = null
    private var cachedSnapshot: LiveMarketBreathingSnapshot? = null
    private var cacheInvalidated = true

    @Synchronized
    fun append(context: Context, micro: MicroImpulseSnapshot) {
        if (!micro.connected || micro.updatedAt <= 0L || micro.priceUsdt <= 0.0) return
        ensureLoaded(context)
        if (samples.peekLast()?.at == micro.updatedAt) return
        val sample = LiveBreathingSample(
            at = micro.updatedAt,
            priceUsdt = micro.priceUsdt,
            pumpBuyerPercent = micro.aggressiveBuyPercent60s,
            pumpChange60sPercent = micro.priceChange60sPercent,
            bookImbalance = micro.topBookImbalance,
            bitcoinBuyerPercent = micro.bitcoinAggressiveBuyPercent60s,
            bitcoinChange60sPercent = micro.bitcoinPriceChange60sPercent,
            pumpBuyNotional60s = micro.buyNotional60s,
            pumpSellNotional60s = micro.sellNotional60s,
            pumpTrades60s = micro.trades60s,
            tradeAcceleration = micro.tradeAcceleration,
            bitcoinPriceUsdt = micro.bitcoinPriceUsdt
        )
        samples.addLast(sample)
        if (cachedCurrentMinute != Long.MIN_VALUE && sample.at / 60_000L < cachedCurrentMinute) {
            cacheInvalidated = true
        }
        trim(sample.at)
        runCatching {
            val file = File(context.filesDir, FILE_NAME)
            if (sample.at - lastPrunedAt >= PRUNE_INTERVAL_MILLIS) {
                RollingCsvRetention.prune(file, sample.at)
                lastPrunedAt = sample.at
            }
            val newFile = !file.exists() || file.length() == 0L
            file.appendText(buildString {
                if (newFile) append(
                    "observed_at_ms,price_usdt,pump_buy_pct,pump_change_60s_pct,book_imbalance," +
                        "btc_buy_pct,btc_change_60s_pct,pump_buy_notional_60s,pump_sell_notional_60s," +
                        "pump_trades_60s,trade_acceleration,btc_price_usdt\n"
                )
                append(sample.at).append(',')
                append(number(sample.priceUsdt)).append(',')
                append(number(sample.pumpBuyerPercent)).append(',')
                append(number(sample.pumpChange60sPercent)).append(',')
                append(sample.bookImbalance?.let(::number).orEmpty()).append(',')
                append(number(sample.bitcoinBuyerPercent)).append(',')
                append(number(sample.bitcoinChange60sPercent)).append(',')
                append(number(sample.pumpBuyNotional60s)).append(',')
                append(number(sample.pumpSellNotional60s)).append(',')
                append(sample.pumpTrades60s).append(',')
                append(number(sample.tradeAcceleration)).append(',')
                append(number(sample.bitcoinPriceUsdt)).append('\n')
            }, Charsets.UTF_8)
        }
    }

    @Synchronized
    fun snapshot(
        context: Context,
        now: Long = System.currentTimeMillis()
    ): LiveMarketBreathingSnapshot {
        ensureLoaded(context)
        trim(now)
        val latest = samples.peekLast() ?: return LiveMarketBreathingSnapshot()
        val currentMinute = now / 60_000L
        val fresh = now >= latest.at && now - latest.at <= LiveMarketBreathingAnalyzer.MAX_LIVE_AGE_MILLIS
        val cached = cachedSnapshot

        if (
            cached == null || cacheInvalidated || cachedCurrentMinute != currentMinute ||
            cachedFreshState != fresh
        ) {
            val rebuilt = LiveMarketBreathingAnalyzer.analyze(samples.toList(), now)
            cachedCurrentMinute = currentMinute
            cachedFreshState = fresh
            cachedSnapshot = rebuilt
            cacheInvalidated = false
            return rebuilt
        }

        return LiveMarketBreathingAnalyzer.refreshLive(cached, latest, now)
    }

    @Synchronized
    internal fun resetForTests() {
        samples.clear()
        initialized = false
        lastPrunedAt = 0L
        invalidateCache()
    }

    private fun ensureLoaded(context: Context) {
        if (initialized) return
        initialized = true
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) {
            invalidateCache()
            return
        }
        runCatching {
            file.useLines(Charsets.UTF_8) { lines ->
                lines.drop(1).mapNotNull(::parse).forEach(samples::addLast)
            }
        }
        trim(System.currentTimeMillis())
        invalidateCache()
    }

    private fun trim(now: Long) {
        val cutoff = now - RollingCsvRetention.RETENTION_MILLIS
        var removed = false
        while (samples.isNotEmpty() && samples.peekFirst().at < cutoff) {
            samples.removeFirst()
            removed = true
        }
        if (removed) cacheInvalidated = true
    }

    private fun invalidateCache() {
        cachedCurrentMinute = Long.MIN_VALUE
        cachedFreshState = null
        cachedSnapshot = null
        cacheInvalidated = true
    }

    private fun parse(line: String): LiveBreathingSample? {
        val values = line.split(',')
        if (values.size < 7) return null
        return LiveBreathingSample(
            at = values[0].toLongOrNull() ?: return null,
            priceUsdt = values[1].toDoubleOrNull() ?: return null,
            pumpBuyerPercent = values[2].toDoubleOrNull() ?: return null,
            pumpChange60sPercent = values[3].toDoubleOrNull() ?: return null,
            bookImbalance = values[4].toDoubleOrNull(),
            bitcoinBuyerPercent = values[5].toDoubleOrNull() ?: return null,
            bitcoinChange60sPercent = values[6].toDoubleOrNull() ?: return null,
            pumpBuyNotional60s = values.getOrNull(7)?.toDoubleOrNull() ?: 0.0,
            pumpSellNotional60s = values.getOrNull(8)?.toDoubleOrNull() ?: 0.0,
            pumpTrades60s = values.getOrNull(9)?.toIntOrNull() ?: 0,
            tradeAcceleration = values.getOrNull(10)?.toDoubleOrNull() ?: 0.0,
            bitcoinPriceUsdt = values.getOrNull(11)?.toDoubleOrNull() ?: 0.0
        )
    }

    private fun number(value: Double): String =
        String.format(Locale.US, "%.8f", value)
}
