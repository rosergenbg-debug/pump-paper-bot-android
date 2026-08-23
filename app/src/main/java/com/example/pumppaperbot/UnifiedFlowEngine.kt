package com.example.pumppaperbot

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * V5.15 single source of truth for every multi-minute PUMP flow horizon.
 *
 * Raw MicroImpulse samples are 60-second rolling observations sampled more often than once
 * per minute. Summing all of them would count the same trades several times. We therefore
 * keep exactly one observation per completed wall-clock minute and build every 5/15/20/30+
 * horizon from those non-overlapping minute buckets.
 *
 * V5.15 keeps V5.14's scoring math but removes the allocation-heavy groupBy/prefix scans from
 * the hot path. Window selection now walks backwards only across the requested horizon.
 */
internal data class UnifiedMinuteFlow(
    val minuteKey: Long,
    val at: Long,
    val priceUsdt: Double,
    val buyerPercent: Double,
    val buyNotional: Double,
    val sellNotional: Double,
    val bookImbalance: Double?,
    val bitcoinBuyerPercent: Double,
    val bitcoinChange60sPercent: Double,
    val priceChange60sPercent: Double,
    val source: LiveBreathingSample
)

internal data class UnifiedWindowFlow(
    val minutes: Int,
    val score: Int?,
    val rawScore: Int?,
    val priceChangePercent: Double?,
    val buyerPercent: Double?,
    val persistencePercent: Int,
    val minuteCount: Int,
    val requiredMinutes: Int,
    val ready: Boolean
)

internal object UnifiedFlowEngine {
    const val NEUTRAL_BAND = 8
    const val CONFIRM_BAND = 6
    const val STRONG_MOVE = 28

    /** Uses only completed wall-clock minutes. */
    fun completedMinuteBuckets(
        samples: List<LiveBreathingSample>,
        now: Long
    ): List<UnifiedMinuteFlow> {
        if (samples.isEmpty()) return emptyList()
        val currentMinute = now / 60_000L
        val ordered = samples.asSequence()
            .filter { it.at > 0L && it.at / 60_000L < currentMinute }
            .sortedBy { it.at }

        val result = ArrayList<UnifiedMinuteFlow>()
        var minuteKey = Long.MIN_VALUE
        var latestInMinute: LiveBreathingSample? = null

        fun flush() {
            val sample = latestInMinute ?: return
            result += toBucket(minuteKey, sample)
        }

        ordered.forEach { sample ->
            val key = sample.at / 60_000L
            if (key != minuteKey) {
                flush()
                minuteKey = key
            }
            latestInMinute = sample
        }
        flush()
        return result
    }

    fun representativeSamplesFromBuckets(
        buckets: List<UnifiedMinuteFlow>
    ): List<LiveBreathingSample> = buckets.map { bucket ->
        bucket.source.copy(
            at = bucket.at,
            priceUsdt = bucket.priceUsdt,
            pumpBuyerPercent = bucket.buyerPercent,
            pumpBuyNotional60s = bucket.buyNotional,
            pumpSellNotional60s = bucket.sellNotional,
            bookImbalance = bucket.bookImbalance
        )
    }

    fun window(
        buckets: List<UnifiedMinuteFlow>,
        minutes: Int,
        requireFullCoverage: Boolean = true
    ): UnifiedWindowFlow = windowAt(
        buckets = buckets,
        endIndex = buckets.lastIndex,
        minutes = minutes,
        requireFullCoverage = requireFullCoverage,
        stabilize = true
    )

    fun windowAt(
        buckets: List<UnifiedMinuteFlow>,
        endIndex: Int,
        minutes: Int,
        requireFullCoverage: Boolean,
        stabilize: Boolean = true
    ): UnifiedWindowFlow {
        val required = requiredMinutes(minutes)
        if (minutes <= 0 || endIndex !in buckets.indices) {
            return UnifiedWindowFlow(minutes, null, null, null, null, 0, 0, required, false)
        }
        val raw = rawWindow(buckets, endIndex, minutes, requireFullCoverage)
        if (raw.rawScore == null) return raw
        if (!stabilize) return raw.copy(score = raw.rawScore)

        val previous = ArrayList<Int>(2)
        for (back in 1..2) {
            val index = endIndex - back
            if (index >= 0) {
                rawWindow(buckets, index, minutes, requireFullCoverage = false).rawScore?.let(previous::add)
            }
        }
        val stable = stabilize(raw.rawScore, previous, minutes)
        return raw.copy(score = stable)
    }

    fun historyScore(
        buckets: List<UnifiedMinuteFlow>,
        endIndex: Int,
        minutes: Int
    ): Int {
        if (endIndex !in buckets.indices) return 0
        val startIndex = windowStartIndex(buckets, endIndex, minutes)
        val count = endIndex - startIndex + 1
        if (count < minimumHistoryMinutes(minutes)) return 0
        return windowAt(
            buckets = buckets,
            endIndex = endIndex,
            minutes = minutes,
            requireFullCoverage = false,
            stabilize = true
        ).score ?: 0
    }

    fun samplePulse(sample: LiveBreathingSample): Double {
        val total = sample.pumpBuyNotional60s + sample.pumpSellNotional60s
        val buyerPercent = if (total > 0.0) {
            sample.pumpBuyNotional60s / total * 100.0
        } else sample.pumpBuyerPercent
        val executedFlow = ((buyerPercent - 50.0) * 4.0).coerceIn(-100.0, 100.0)
        val priceResponse = (sample.pumpChange60sPercent * 110.0).coerceIn(-100.0, 100.0)
        val book = ((sample.bookImbalance ?: 0.0) * 100.0).coerceIn(-100.0, 100.0)
        val btcRegime = (
            (sample.bitcoinBuyerPercent - 50.0) * 1.5 +
                sample.bitcoinChange60sPercent * 55.0
            ).coerceIn(-100.0, 100.0)
        return (
            executedFlow * 0.62 + priceResponse * 0.25 + book * 0.08 + btcRegime * 0.05
            ).coerceIn(-100.0, 100.0)
    }

    fun requiredMinutes(minutes: Int): Int = when (minutes) {
        5 -> 4
        15 -> 12
        20 -> 16
        30 -> 25
        60 -> 50
        180 -> 150
        360 -> 300
        else -> (minutes * 0.82).roundToInt().coerceAtLeast(2)
    }

    private fun minimumHistoryMinutes(minutes: Int): Int = when (minutes) {
        5 -> 4
        15 -> 12
        20 -> 16
        30 -> 25
        60 -> 45
        180 -> 90
        360 -> 120
        else -> maxOf(4, (minutes * 0.50).roundToInt())
    }

    private fun rawWindow(
        buckets: List<UnifiedMinuteFlow>,
        endIndex: Int,
        minutes: Int,
        requireFullCoverage: Boolean
    ): UnifiedWindowFlow {
        val required = requiredMinutes(minutes)
        val startIndex = windowStartIndex(buckets, endIndex, minutes)
        val selected = buckets.subList(startIndex, endIndex + 1)
        val ready = selected.size >= required
        val minimum = if (requireFullCoverage) required else minOf(4, minutes.coerceAtLeast(2))
        if (selected.size < minimum) {
            return UnifiedWindowFlow(
                minutes, null, null, null, null, 0,
                selected.size, required, false
            )
        }

        var totalBuy = 0.0
        var totalSell = 0.0
        var notionalBuckets = 0
        var buyerFallbackSum = 0.0
        val bookValues = ArrayList<Double>(selected.size)
        val btcBuyerValues = ArrayList<Double>(selected.size)
        val btcChangeValues = ArrayList<Double>(selected.size)
        val minutePulses = DoubleArray(selected.size)

        selected.forEachIndexed { index, bucket ->
            totalBuy += bucket.buyNotional
            totalSell += bucket.sellNotional
            if (bucket.buyNotional + bucket.sellNotional > 0.0) notionalBuckets++
            buyerFallbackSum += bucket.buyerPercent
            bucket.bookImbalance?.takeIf(Double::isFinite)?.let(bookValues::add)
            if (bucket.bitcoinBuyerPercent.isFinite()) btcBuyerValues += bucket.bitcoinBuyerPercent
            if (bucket.bitcoinChange60sPercent.isFinite()) btcChangeValues += bucket.bitcoinChange60sPercent
            minutePulses[index] = bucketPulse(bucket)
        }

        val totalNotional = totalBuy + totalSell
        val buyerPercent = if (totalNotional > 0.0 && notionalBuckets * 5 >= selected.size * 3) {
            totalBuy / totalNotional * 100.0
        } else {
            buyerFallbackSum / selected.size
        }.coerceIn(0.0, 100.0)

        val first = selected.first()
        val last = selected.last()
        val firstMinuteFactor = 1.0 + first.priceChange60sPercent / 100.0
        val startPrice = if (
            first.priceUsdt > 0.0 && firstMinuteFactor.isFinite() &&
            firstMinuteFactor > 0.20 && firstMinuteFactor < 5.0
        ) first.priceUsdt / firstMinuteFactor else first.priceUsdt
        val priceChange = if (startPrice > 0.0 && last.priceUsdt > 0.0) {
            (last.priceUsdt / startPrice - 1.0) * 100.0
        } else 0.0

        val book = medianMutable(bookValues) ?: 0.0
        val bitcoinBuyer = medianMutable(btcBuyerValues) ?: 50.0
        val bitcoinChange = medianMutable(btcChangeValues) ?: 0.0

        var positive = 0
        var negative = 0
        minutePulses.forEach { pulse ->
            if (pulse >= CONFIRM_BAND) positive++
            if (pulse <= -CONFIRM_BAND) negative++
        }
        val dominantPositive = positive >= negative
        var persistent = 0
        minutePulses.forEach { pulse ->
            if (if (dominantPositive) pulse >= 0.0 else pulse <= 0.0) persistent++
        }
        val persistence = (persistent * 100.0 / minutePulses.size).roundToInt().coerceIn(0, 100)

        val buyerScore = ((buyerPercent - 50.0) * 4.0).coerceIn(-100.0, 100.0)
        val priceScore = (priceChange / priceScale(minutes) * 100.0).coerceIn(-100.0, 100.0)
        val bookScore = (book * 100.0).coerceIn(-100.0, 100.0)
        val btcScore = (
            (bitcoinBuyer - 50.0) * 1.5 + bitcoinChange * 55.0
            ).coerceIn(-100.0, 100.0)
        val persistenceFactor = 0.78 + persistence.coerceIn(50, 100) / 100.0 * 0.22
        val rawScore = (
            (buyerScore * 0.62 + priceScore * 0.25 + bookScore * 0.08 + btcScore * 0.05) *
                persistenceFactor
            ).roundToInt().coerceIn(-100, 100)

        return UnifiedWindowFlow(
            minutes = minutes,
            score = rawScore,
            rawScore = rawScore,
            priceChangePercent = priceChange,
            buyerPercent = buyerPercent,
            persistencePercent = persistence,
            minuteCount = selected.size,
            requiredMinutes = required,
            ready = ready
        )
    }

    private fun windowStartIndex(
        buckets: List<UnifiedMinuteFlow>,
        endIndex: Int,
        minutes: Int
    ): Int {
        val startMinute = buckets[endIndex].minuteKey - minutes + 1L
        var start = endIndex
        while (start > 0 && buckets[start - 1].minuteKey >= startMinute) start--
        return start
    }

    private fun toBucket(minuteKey: Long, sample: LiveBreathingSample): UnifiedMinuteFlow {
        val total = sample.pumpBuyNotional60s + sample.pumpSellNotional60s
        val buyerPercent = if (total > 0.0) {
            sample.pumpBuyNotional60s / total * 100.0
        } else sample.pumpBuyerPercent
        return UnifiedMinuteFlow(
            minuteKey = minuteKey,
            at = sample.at,
            priceUsdt = sample.priceUsdt,
            buyerPercent = buyerPercent.coerceIn(0.0, 100.0),
            buyNotional = sample.pumpBuyNotional60s.coerceAtLeast(0.0),
            sellNotional = sample.pumpSellNotional60s.coerceAtLeast(0.0),
            bookImbalance = sample.bookImbalance,
            bitcoinBuyerPercent = sample.bitcoinBuyerPercent.coerceIn(0.0, 100.0),
            bitcoinChange60sPercent = sample.bitcoinChange60sPercent,
            priceChange60sPercent = sample.pumpChange60sPercent,
            source = sample
        )
    }

    private fun bucketPulse(bucket: UnifiedMinuteFlow): Double {
        val total = bucket.buyNotional + bucket.sellNotional
        val buyerPercent = if (total > 0.0) bucket.buyNotional / total * 100.0 else bucket.buyerPercent
        val executedFlow = ((buyerPercent - 50.0) * 4.0).coerceIn(-100.0, 100.0)
        val price = (bucket.priceChange60sPercent * 110.0).coerceIn(-100.0, 100.0)
        val book = ((bucket.bookImbalance ?: 0.0) * 100.0).coerceIn(-100.0, 100.0)
        val btc = (
            (bucket.bitcoinBuyerPercent - 50.0) * 1.5 +
                bucket.bitcoinChange60sPercent * 55.0
            ).coerceIn(-100.0, 100.0)
        return (executedFlow * 0.62 + price * 0.25 + book * 0.08 + btc * 0.05)
            .coerceIn(-100.0, 100.0)
    }

    private fun stabilize(current: Int, previous: List<Int>, minutes: Int): Int {
        if (abs(current) < NEUTRAL_BAND) return 0
        if (abs(current) >= STRONG_MOVE) return current
        val sign = if (current > 0) 1 else -1
        val requiredConfirmations = if (minutes >= 20) 2 else 1
        val confirmations = previous.count { it * sign >= CONFIRM_BAND }
        return if (confirmations >= requiredConfirmations) current else 0
    }

    private fun priceScale(minutes: Int): Double = when (minutes) {
        5 -> 0.75
        15 -> 1.25
        20 -> 1.50
        30 -> 1.75
        60 -> 2.50
        180 -> 4.25
        360 -> 6.00
        else -> (0.55 + minutes * 0.015).coerceAtLeast(0.75)
    }

    private fun medianMutable(values: MutableList<Double>): Double? {
        if (values.isEmpty()) return null
        values.sort()
        val middle = values.size / 2
        return if (values.size % 2 == 1) values[middle]
        else (values[middle - 1] + values[middle]) / 2.0
    }
}
