package com.example.pumppaperbot

import kotlin.math.max

internal data class ChartTooltipPosition(val left: Float, val top: Float)

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
