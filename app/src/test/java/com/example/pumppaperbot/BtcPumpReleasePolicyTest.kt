package com.example.pumppaperbot

import org.junit.Assert.assertTrue
import org.junit.Test

class BtcPumpReleasePolicyTest {
    @Test
    fun matchingSequenceProducesHighPatternScoreWithoutCallingItProbability() {
        val minute = 60_000L
        val now = 61L * minute
        val samples = (0..60).map { i ->
            val btc = when {
                i <= 15 -> 100_000.0
                i <= 45 -> 100_000.0 * (1.0 + 0.005 * (i - 15) / 30.0)
                else -> 100_500.0 * (1.0 + 0.0002 * (i - 45) / 15.0)
            }
            val pump = when {
                i <= 45 -> 0.0030000 * (1.0 + 0.0004 * maxOf(0, i - 15) / 30.0)
                else -> 0.0030012 * (1.0 + 0.0020 * (i - 45) / 15.0)
            }
            LiveBreathingSample(
                at = (i + 1L) * minute,
                priceUsdt = pump,
                pumpBuyerPercent = if (i >= 56) 55.0 else 48.0,
                pumpChange60sPercent = 0.0,
                bookImbalance = 0.0,
                bitcoinBuyerPercent = 50.0,
                bitcoinChange60sPercent = 0.0,
                bitcoinPriceUsdt = btc
            )
        }

        val result = BtcPumpReleasePolicy.evaluate(samples, now)

        assertTrue(result.fresh)
        assertTrue(result.btcImpulseScore >= 80)
        assertTrue(result.pumpHoldScore >= 70)
        assertTrue(result.btcStableScore >= 70)
        assertTrue(result.releaseScore >= 50)
        assertTrue(result.patternScore >= 70)
        assertTrue(result.detail.contains("НЕ BUY-СИГНАЛ"))
    }

    @Test
    fun flatBitcoinCannotMasqueradeAsReleasePattern() {
        val minute = 60_000L
        val now = 61L * minute
        val samples = (0..60).map { i ->
            LiveBreathingSample(
                at = (i + 1L) * minute,
                priceUsdt = 0.003,
                pumpBuyerPercent = 50.0,
                pumpChange60sPercent = 0.0,
                bookImbalance = 0.0,
                bitcoinBuyerPercent = 50.0,
                bitcoinChange60sPercent = 0.0,
                bitcoinPriceUsdt = 100_000.0
            )
        }

        val result = BtcPumpReleasePolicy.evaluate(samples, now)

        assertTrue(result.btcImpulseScore < 35)
        assertTrue(result.patternScore <= 44)
        assertTrue(result.phase.contains("НЕТ BTC-РАЗГОНА"))
    }
}
