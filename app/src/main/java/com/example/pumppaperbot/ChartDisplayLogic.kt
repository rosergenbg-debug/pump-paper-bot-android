package com.example.pumppaperbot

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
