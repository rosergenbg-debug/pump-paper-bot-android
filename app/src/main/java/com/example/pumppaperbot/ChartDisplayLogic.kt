package com.example.pumppaperbot

import kotlin.math.max
import kotlin.math.roundToInt

internal data class ChartTooltipPosition(val left: Float, val top: Float)

internal data class TradeFocusWindow(val visibleBars: Int, val endExclusive: Int)

internal fun tradeFocusWindow(startIndex: Int, endIndex: Int, totalBars: Int): TradeFocusWindow {
    if (totalBars <= 0) return TradeFocusWindow(0, 0)
    val safeStart = startIndex.coerceIn(0, totalBars - 1)
    val safeEnd = endIndex.coerceIn(safeStart, totalBars - 1)
    val span = safeEnd - safeStart + 1
    val sidePadding = max(5, (span * 0.14).roundToInt())
    val visible = (span + sidePadding * 2).coerceIn(minOf(24, totalBars), minOf(240, totalBars))
    val preferredStart = safeStart - sidePadding
    val maxStart = max(0, totalBars - visible)
    val windowStart = preferredStart.coerceIn(0, maxStart)
    val endExclusive = (windowStart + visible).coerceAtMost(totalBars)
    return TradeFocusWindow(visible, endExclusive)
}

internal fun nextChartVisibleBarLimit(current: Int): Int = when (current) {
    in 0..30 -> 120
    in 31..60 -> 30
    in 61..120 -> 60
    else -> 120
}

internal fun chartZoomActionText(limit: Int): String = when (limit) {
    in 0..30 -> "ВЕРНУТЬ ОБЗОР"
    in 31..60 -> "ЕЩЁ ×2"
    else -> "УВЕЛИЧИТЬ ×2"
}

internal fun chartTooltipPosition(
    touchX: Float,
    touchY: Float,
    plotLeft: Float,
    plotRight: Float,
    plotTop: Float,
    plotBottom: Float,
    tooltipWidth: Float,
    tooltipHeight: Float,
    clearance: Float
): ChartTooltipPosition {
    val preferredLeft = if (touchX >= (plotLeft + plotRight) / 2f) {
        touchX - tooltipWidth - clearance
    } else {
        touchX + clearance
    }
    val preferredTop = if (touchY >= (plotTop + plotBottom) / 2f) {
        touchY - tooltipHeight - clearance
    } else {
        touchY + clearance
    }
    return ChartTooltipPosition(
        left = preferredLeft.coerceIn(plotLeft, max(plotLeft, plotRight - tooltipWidth)),
        top = preferredTop.coerceIn(plotTop, max(plotTop, plotBottom - tooltipHeight))
    )
}
