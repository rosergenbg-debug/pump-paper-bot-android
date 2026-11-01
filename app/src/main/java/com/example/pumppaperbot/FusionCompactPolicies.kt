package com.example.pumppaperbot

import kotlin.math.max

/**
 * Net change relative to total funds spent buying PUMP, with both venue fees
 * reflected in EUR, using the executable Fusion bid for selling.
 */
internal object FusionPositionFees {
    const val RATE = 0.0025
    fun pnlPercent(entryAsk: Double, currentBid: Double, fee: Double = RATE): Double {
        require(entryAsk > 0.0 && currentBid > 0.0 && fee >= 0.0 && fee < 1.0)
        return ((currentBid * (1.0 - fee)) / (entryAsk * (1.0 + fee)) - 1.0) * 100.0
    }
}

internal data class FusionFlowLine(val minutes: Int, val buyUsdt: Double, val sellUsdt: Double) {
    val netUsdt: Double get() = buyUsdt - sellUsdt
    val buyRatio: Float get() =
        if (buyUsdt + sellUsdt <= 0.0) 0f
        else (buyUsdt / (buyUsdt + sellUsdt)).toFloat().coerceIn(0f, 1f)
}

internal data class FusionFlowViewModel(
    val rows: List<FusionFlowLine>,
    val fresh: Boolean,
    val secondsCovered: Long,
    val status: String
) {
    companion object {
        fun from(micro: MicroImpulseSnapshot, now: Long): FusionFlowViewModel {
            // A saved old snapshot must never masquerade as live zero-flow market activity.
            val fresh = micro.connected && micro.updatedAt in 1L..now &&
                now - micro.updatedAt <= 25_000L
            val rows = listOf(
                FusionFlowLine(1, micro.buyNotional60s, micro.sellNotional60s),
                FusionFlowLine(5, micro.buyNotional5m, micro.sellNotional5m),
                FusionFlowLine(15, micro.buyNotional15m, micro.sellNotional15m),
                FusionFlowLine(30, micro.buyNotional30m, micro.sellNotional30m),
                FusionFlowLine(60, micro.buyNotional60m, micro.sellNotional60m)
            )
            val coverage = max(0L, micro.flowHistorySeconds)
            val status = when {
                !fresh -> "Поток Binance недоступен или устарел"
                coverage < 60L -> "Накопление истории • менее 1 минуты"
                coverage < 3600L -> "Живой поток • история набирается (${coverage / 60} мин)"
                else -> "Живой поток • 60 минут истории"
            }
            return FusionFlowViewModel(rows, fresh, coverage, status)
        }
    }
}
