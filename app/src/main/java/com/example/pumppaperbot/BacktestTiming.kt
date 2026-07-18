package com.example.pumppaperbot

internal data class BacktestHoldWindow(
    val lastSignalIndex: Int,
    val executionIndex: Int
)

/**
 * A 24-hour hold on 30-minute candles observes exactly 48 closed candles and
 * executes at the open immediately after them. The same rule gives 96 candles
 * for 48 hours.
 */
internal fun backtestHoldWindow(entryIndex: Int, holdBars: Int, lastIndex: Int): BacktestHoldWindow {
    require(entryIndex in 0..lastIndex)
    require(holdBars > 0)
    val lastSignal = minOf(entryIndex + holdBars - 1, lastIndex - 1)
        .coerceAtLeast(entryIndex)
    return BacktestHoldWindow(lastSignal, minOf(lastSignal + 1, lastIndex))
}
