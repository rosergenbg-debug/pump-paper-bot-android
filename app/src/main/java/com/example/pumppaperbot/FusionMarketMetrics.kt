package com.example.pumppaperbot

/**
 * Real Bitpanda Fusion executable top-of-book spread.
 * This is NOT a trading fee, and is independent of the current account tier.
 */
internal object FusionMarketMetrics {
    fun spreadPercent(bid: Double, ask: Double): Double {
        require(bid.isFinite() && ask.isFinite() && bid > 0.0 && ask >= bid)
        return (ask - bid) / ((ask + bid) / 2.0) * 100.0
    }

    fun spreadEuro(bid: Double, ask: Double): Double {
        require(bid.isFinite() && ask.isFinite() && bid > 0.0 && ask >= bid)
        return ask - bid
    }
}
