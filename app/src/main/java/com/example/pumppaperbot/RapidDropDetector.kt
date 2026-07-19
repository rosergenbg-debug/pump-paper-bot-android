package com.example.pumppaperbot

import kotlin.math.max

data class RapidDropState(
    val active: Boolean,
    val dropPercent: Double = 0.0,
    val severityBand: Int = 0,
    val peakPrice: Double = 0.0,
    val currentPrice: Double = 0.0,
    val peakTime: Long = 0L,
    val windowMinutes: Int = 0,
    val reboundPercent: Double = 0.0,
    val recoveryConfirmed: Boolean = false
) {
    companion object {
        fun none() = RapidDropState(false)
    }
}

/**
 * Detects an exceptional fall from the highest PUMP/EUR price seen during the
 * previous 24 hours.  A live ticker is used when available; closed 30-minute
 * candles remain the fallback, so an API failure cannot invent a crash.
 *
 * The detector is deliberately not a buy signal.  Recovery is marked only
 * after a visible rebound, a green closed candle and buyer-dominated spot flow.
 */
object RapidDropDetector {
    const val DROP_THRESHOLD_PERCENT = 25.0
    const val RESET_THRESHOLD_PERCENT = 20.0
    const val LOOKBACK_BARS = 48
    private const val RECOVERY_PERCENT = 3.0
    private const val BUYER_FLOW_MIN = 0.52
    const val SAME_LEVEL_COOLDOWN_MILLIS = 12L * 60L * 60L * 1000L

    fun detect(candles: List<PumpCandle>, livePrice: Double? = null): RapidDropState {
        if (candles.size < 3) return RapidDropState.none()
        val recent = candles.takeLast(LOOKBACK_BARS.coerceAtMost(candles.size))
        val current = livePrice?.takeIf { it.isFinite() && it > 0.0 }
            ?: recent.last().close.takeIf { it.isFinite() && it > 0.0 }
            ?: return RapidDropState.none()
        val peakIndex = recent.indices.maxByOrNull { recent[it].high } ?: return RapidDropState.none()
        val peak = recent[peakIndex].high
        if (!peak.isFinite() || peak <= 0.0 || current >= peak) return RapidDropState.none()
        val drop = (1.0 - current / peak) * 100.0
        if (drop < DROP_THRESHOLD_PERCENT) return RapidDropState.none()

        val afterPeak = recent.subList(peakIndex, recent.size)
        val low = minOf(afterPeak.minOf { it.low }, current)
        val rebound = if (low > 0.0) max(0.0, (current / low - 1.0) * 100.0) else 0.0
        val latest = recent.last()
        val previous = recent[recent.lastIndex - 1]
        val flowCandles = recent.takeLast(3)
        val totalVolume = flowCandles.sumOf { it.volume }
        val buyerShare = if (totalVolume > 0.0) {
            flowCandles.sumOf { it.takerBuyVolume } / totalVolume
        } else 0.0
        val recovery = rebound >= RECOVERY_PERCENT &&
            latest.close > latest.open &&
            latest.close > previous.close &&
            buyerShare >= BUYER_FLOW_MIN

        return RapidDropState(
            active = true,
            dropPercent = drop,
            severityBand = severityBand(drop),
            peakPrice = peak,
            currentPrice = current,
            peakTime = recent[peakIndex].closeTime,
            windowMinutes = ((recent.last().closeTime - recent[peakIndex].closeTime) / 60_000L)
                .toInt().coerceIn(0, 24 * 60),
            reboundPercent = rebound,
            recoveryConfirmed = recovery
        )
    }

    internal fun severityBand(dropPercent: Double): Int = when {
        dropPercent >= 80.0 -> 80
        dropPercent >= 65.0 -> 65
        dropPercent >= 50.0 -> 50
        dropPercent >= 35.0 -> 35
        dropPercent >= DROP_THRESHOLD_PERCENT -> 25
        else -> 0
    }

    internal fun shouldNotify(currentBand: Int, lastBand: Int, elapsedMillis: Long): Boolean {
        if (currentBand < 25) return false
        if (lastBand < 25) return true
        return currentBand > lastBand || elapsedMillis >= SAME_LEVEL_COOLDOWN_MILLIS
    }
}
