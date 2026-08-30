package com.example.pumppaperbot

import kotlin.math.max
import kotlin.math.min

internal data class MainChartPriceWindow(
    val minPrice: Double,
    val maxPrice: Double
) {
    val span: Double get() = (maxPrice - minPrice).coerceAtLeast(0.0000000001)
}

data class MainChartFlowScores(
    val instant: Int?,
    val fiveMinutes: Int?,
    val fifteenMinutes: Int?,
    val thirtyMinutes: Int?,
    val fresh: Boolean
)

/** Presentation only: indicators never decide the main chart's vertical price scale. */
internal object MainChartViewportPolicy {
    fun candleWindow(
        candles: List<PumpCandle>,
        verticalShiftFraction: Float = 0f
    ): MainChartPriceWindow? {
        if (candles.isEmpty()) return null
        val guides = RangeGuidePolicy.levels(candles.last().close)
        val candleMin = candles.minOf { it.low }
        val candleMax = candles.maxOf { it.high }
        val rawMin = min(candleMin, guides?.outerLower ?: candleMin)
        val rawMax = max(candleMax, guides?.outerUpper ?: candleMax)
        if (!rawMin.isFinite() || !rawMax.isFinite() || rawMin <= 0.0 || rawMax < rawMin) return null
        val rawSpan = max(rawMax - rawMin, rawMax * 0.006)
        val padding = rawSpan * 0.12
        val baseMin = rawMin - padding
        val baseMax = rawMax + padding
        val span = baseMax - baseMin
        val shift = span * verticalShiftFraction.coerceIn(-1.35f, 1.35f)
        val shiftedMin = (baseMin + shift).coerceAtLeast(rawMax * 0.000001)
        return MainChartPriceWindow(shiftedMin, shiftedMin + span)
    }
}

internal object MainChartFlowPresentation {
    fun from(snapshot: LiveMarketBreathingSnapshot): MainChartFlowScores {
        fun score(minutes: Int): Int? = snapshot.horizons.firstOrNull { it.minutes == minutes }?.score
        return if (snapshot.fresh) {
            MainChartFlowScores(
                instant = snapshot.instantScore,
                fiveMinutes = score(5),
                fifteenMinutes = score(15),
                thirtyMinutes = score(30),
                fresh = true
            )
        } else {
            MainChartFlowScores(null, null, null, null, fresh = false)
        }
    }
}
