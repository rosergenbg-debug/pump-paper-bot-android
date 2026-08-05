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
    val bitcoinChange60sPercent: Double
)

data class LiveBreathingHorizon(
    val minutes: Int,
    val score: Int?,
    val priceChangePercent: Double?,
    val buyerPercent: Double?,
    val persistencePercent: Int,
    val samples: Int
)

data class LiveMarketBreathingSnapshot(
    val updatedAt: Long = 0L,
    val fresh: Boolean = false,
    val historyMinutes: Int = 0,
    val instantScore: Int? = null,
    val normalScore: Int? = null,
    val experimentScore: Int? = null,
    val regime: String = "НАКАПЛИВАЕМ ИСТОРИЮ",
    val horizons: List<LiveBreathingHorizon> = emptyList()
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
 * Converts noisy 15-second public trade/book snapshots into persistent multi-horizon context.
 * It never executes a trade. The experiment remains faster, but cannot differ from the normal
 * DeepSeek breathing score by more than 15 points.
 */
object LiveMarketBreathingAnalyzer {
    const val MAX_EXPERIMENT_GAP = 15
    const val MAX_LIVE_AGE_MILLIS = 90_000L
    private val windows = intArrayOf(5, 15, 30, 60, 360)

    fun analyze(samples: List<LiveBreathingSample>, now: Long): LiveMarketBreathingSnapshot {
        val valid = samples.asSequence()
            .filter { it.at in (now - RollingCsvRetention.RETENTION_MILLIS)..now }
            .filter { it.priceUsdt > 0.0 && it.pumpBuyerPercent.isFinite() }
            .sortedBy { it.at }
            .toList()
        if (valid.isEmpty()) return LiveMarketBreathingSnapshot()

        val latest = valid.last()
        val fresh = now >= latest.at && now - latest.at <= MAX_LIVE_AGE_MILLIS
        val instant = instantScore(latest)
        val horizons = windows.map { minutes -> horizon(valid, latest, minutes) }
        val weighted = listOf(0.25, 0.25, 0.20, 0.18, 0.12)
            .zip(horizons)
            .filter { it.second.score != null }
        val normal = if (weighted.isEmpty()) null else (
            weighted.sumOf { (weight, horizon) -> weight * horizon.score!! } /
                weighted.sumOf { it.first }
            ).roundToInt().coerceIn(-100, 100)
        val experiment = normal?.let { stable ->
            val faster = (stable * 0.55 + instant * 0.45).roundToInt()
            faster.coerceIn(stable - MAX_EXPERIMENT_GAP, stable + MAX_EXPERIMENT_GAP)
                .coerceIn(-100, 100)
        }
        val regime = when {
            !fresh -> "ИСТОРИЯ УСТАРЕЛА — ЖДЁМ ЖИВОЙ ПОТОК"
            normal == null -> "НАКАПЛИВАЕМ ИСТОРИЮ"
            normal >= 35 && horizons.count { (it.score ?: 0) >= 20 } >= 2 -> "УСТОЙЧИВОЕ ДАВЛЕНИЕ ПОКУПАТЕЛЕЙ"
            normal <= -35 && horizons.count { (it.score ?: 0) <= -20 } >= 2 -> "УСТОЙЧИВОЕ ДАВЛЕНИЕ ПРОДАВЦОВ"
            abs(normal) < 15 -> "РЫНОЧНЫЙ ШУМ / БОКОВИК"
            normal > 0 -> "УМЕРЕННОЕ УЛУЧШЕНИЕ"
            else -> "УМЕРЕННОЕ УХУДШЕНИЕ"
        }
        val historyMinutes = ((latest.at - valid.first().at).coerceAtLeast(0L) / 60_000L)
            .toInt().coerceAtMost(24 * 60)
        return LiveMarketBreathingSnapshot(
            updatedAt = latest.at,
            fresh = fresh,
            historyMinutes = historyMinutes,
            instantScore = instant,
            normalScore = normal.takeIf { fresh },
            experimentScore = experiment.takeIf { fresh },
            regime = regime,
            horizons = horizons
        )
    }

    internal fun instantScore(sample: LiveBreathingSample): Int {
        val buyer = ((sample.pumpBuyerPercent - 50.0) * 4.0).coerceIn(-100.0, 100.0)
        val price = (sample.pumpChange60sPercent * 125.0).coerceIn(-100.0, 100.0)
        val book = ((sample.bookImbalance ?: 0.0) * 100.0).coerceIn(-100.0, 100.0)
        val bitcoin = (
            (sample.bitcoinBuyerPercent - 50.0) * 2.0 + sample.bitcoinChange60sPercent * 80.0
            ).coerceIn(-100.0, 100.0)
        return (buyer * 0.48 + price * 0.32 + book * 0.10 + bitcoin * 0.10)
            .roundToInt().coerceIn(-100, 100)
    }

    private fun horizon(
        all: List<LiveBreathingSample>,
        latest: LiveBreathingSample,
        minutes: Int
    ): LiveBreathingHorizon {
        val selected = all.filter { it.at >= latest.at - minutes * 60_000L }
        if (selected.size < 2) return LiveBreathingHorizon(minutes, null, null, null, 0, selected.size)
        val buyer = median(selected.map { it.pumpBuyerPercent })
        val first = selected.first()
        val change = if (first.priceUsdt > 0.0) (latest.priceUsdt / first.priceUsdt - 1.0) * 100.0 else 0.0
        val book = median(selected.mapNotNull { it.bookImbalance }) ?: 0.0
        val bitcoinBuyer = median(selected.map { it.bitcoinBuyerPercent }) ?: 50.0
        val directionSamples = selected.map(::instantScore)
        val dominantPositive = directionSamples.count { it >= 5 } >= directionSamples.count { it <= -5 }
        val persistent = directionSamples.count { if (dominantPositive) it >= 0 else it <= 0 }
        val persistence = (persistent * 100.0 / directionSamples.size).roundToInt()
        val priceScale = when (minutes) {
            5 -> 0.75
            15 -> 1.25
            30 -> 1.75
            60 -> 2.50
            else -> 6.0
        }
        val buyerScore = (((buyer ?: 50.0) - 50.0) * 4.0).coerceIn(-100.0, 100.0)
        val priceScore = (change / priceScale * 100.0).coerceIn(-100.0, 100.0)
        val bookScore = (book * 100.0).coerceIn(-100.0, 100.0)
        val btcScore = ((bitcoinBuyer - 50.0) * 2.0).coerceIn(-100.0, 100.0)
        val persistenceFactor = (0.65 + persistence.coerceIn(50, 100) / 100.0 * 0.35)
        val score = ((buyerScore * 0.44 + priceScore * 0.38 + bookScore * 0.08 + btcScore * 0.10) *
            persistenceFactor).roundToInt().coerceIn(-100, 100)
        return LiveBreathingHorizon(minutes, score, change, buyer, persistence, selected.size)
    }

    private fun median(values: List<Double>): Double? {
        val sorted = values.filter(Double::isFinite).sorted()
        if (sorted.isEmpty()) return null
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
    }
}

object LiveMarketBreathingStore {
    private const val FILE_NAME = "pump_live_breathing_v415.csv"
    private const val PRUNE_INTERVAL_MILLIS = 60L * 60L * 1000L
    private val samples = ArrayDeque<LiveBreathingSample>()
    private var initialized = false
    private var lastPrunedAt = 0L

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
            bitcoinChange60sPercent = micro.bitcoinPriceChange60sPercent
        )
        samples.addLast(sample)
        trim(sample.at)
        runCatching {
            val file = File(context.filesDir, FILE_NAME)
            if (sample.at - lastPrunedAt >= PRUNE_INTERVAL_MILLIS) {
                RollingCsvRetention.prune(file, sample.at)
                lastPrunedAt = sample.at
            }
            val newFile = !file.exists() || file.length() == 0L
            file.appendText(buildString {
                if (newFile) append("observed_at_ms,price_usdt,pump_buy_pct,pump_change_60s_pct,book_imbalance,btc_buy_pct,btc_change_60s_pct\n")
                append(sample.at).append(',')
                append(number(sample.priceUsdt)).append(',')
                append(number(sample.pumpBuyerPercent)).append(',')
                append(number(sample.pumpChange60sPercent)).append(',')
                append(sample.bookImbalance?.let(::number).orEmpty()).append(',')
                append(number(sample.bitcoinBuyerPercent)).append(',')
                append(number(sample.bitcoinChange60sPercent)).append('\n')
            }, Charsets.UTF_8)
        }
    }

    @Synchronized
    fun snapshot(context: Context, now: Long = System.currentTimeMillis()): LiveMarketBreathingSnapshot {
        ensureLoaded(context)
        trim(now)
        return LiveMarketBreathingAnalyzer.analyze(samples.toList(), now)
    }

    @Synchronized
    internal fun resetForTests() {
        samples.clear()
        initialized = false
        lastPrunedAt = 0L
    }

    private fun ensureLoaded(context: Context) {
        if (initialized) return
        initialized = true
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return
        runCatching {
            file.useLines(Charsets.UTF_8) { lines ->
                lines.drop(1).mapNotNull(::parse).forEach(samples::addLast)
            }
        }
        trim(System.currentTimeMillis())
    }

    private fun trim(now: Long) {
        val cutoff = now - RollingCsvRetention.RETENTION_MILLIS
        while (samples.isNotEmpty() && samples.peekFirst().at < cutoff) samples.removeFirst()
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
            bitcoinChange60sPercent = values[6].toDoubleOrNull() ?: return null
        )
    }

    private fun number(value: Double): String = String.format(Locale.US, "%.8f", value)
}
