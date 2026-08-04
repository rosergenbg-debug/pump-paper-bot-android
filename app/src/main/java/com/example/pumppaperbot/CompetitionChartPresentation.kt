package com.example.pumppaperbot

/**
 * Keeps the four-account comparison aligned with the current market interval.
 *
 * The strategy intentionally stores closed 30-minute candles. A manual trade can happen inside
 * the still-open interval, though, so the comparison needs a display-only live edge; otherwise a
 * fresh BUY/SELL marker sits beyond the last closed candle and remains invisible for up to 30
 * minutes.
 */
internal object CompetitionChartPresentation {
    fun withLiveEdge(
        closedCandles: List<PumpCandle>,
        livePrice: Double,
        now: Long
    ): List<PumpCandle> = ChartSpeedPresentation.withLiveEdge(
        closedCandles,
        ChartInterval.THIRTY_MINUTES,
        livePrice.takeIf { it.isFinite() && it > 0.0 },
        now
    )
}

internal fun competitionConnectorLaneY(
    positive: Boolean,
    entryY: Float,
    exitY: Float,
    top: Float,
    bottom: Float,
    clearance: Float
): Float = if (positive) {
    (minOf(entryY, exitY) - clearance).coerceAtLeast(top)
} else {
    (maxOf(entryY, exitY) + clearance).coerceAtMost(bottom)
}
