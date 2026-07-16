package com.example.pumppaperbot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrategyV2MarketGateTest {
    @Test
    fun blocksOnlyWhenPumpBtcAndSolAreAllHot() {
        val pump = candles(finalJump = 1.10, hotVolume = true)
        val btc = candles(finalJump = 1.08, hotVolume = true)
        val sol = candles(finalJump = 1.09, hotVolume = true)

        val gate = StrategyV2.marketOverheatGateAt(pump.lastIndex, pump, btc, sol)

        assertTrue(gate.active)
        assertTrue(gate.historySamples >= StrategyV2.MARKET_GATE_MIN_HISTORY)
    }

    @Test
    fun keepsEntryOpenWhenOneMarketIsNotHot() {
        val pump = candles(finalJump = 1.10, hotVolume = true)
        val btc = candles(finalJump = 1.08, hotVolume = true)
        val sol = candles(finalJump = 1.0, hotVolume = false)

        val gate = StrategyV2.marketOverheatGateAt(pump.lastIndex, pump, btc, sol)

        assertFalse(gate.active)
    }

    @Test
    fun doesNotGuessWithoutMinimumPastHistory() {
        val pump = candles(count = 300, finalJump = 1.10, hotVolume = true)
        val btc = candles(count = 300, finalJump = 1.08, hotVolume = true)
        val sol = candles(count = 300, finalJump = 1.09, hotVolume = true)

        val gate = StrategyV2.marketOverheatGateAt(pump.lastIndex, pump, btc, sol)

        assertFalse(gate.active)
    }

    private fun candles(
        count: Int = 370,
        finalJump: Double,
        hotVolume: Boolean
    ): List<PumpCandle> {
        val start = 1_700_000_000_000L
        val interval = 30L * 60L * 1000L
        val closes = MutableList(count) { index -> 100.0 + index * 0.01 }
        if (finalJump > 1.0) {
            val old = closes[count - 3]
            closes[count - 2] = old * (1.0 + (finalJump - 1.0) * 0.45)
            closes[count - 1] = old * finalJump
        }
        return closes.mapIndexed { index, close ->
            val volume = if (hotVolume && index >= count - 2) 5_000.0 else 1_000.0
            PumpCandle(
                openTime = start + index * interval,
                open = close,
                high = close,
                low = close,
                close = close,
                volume = volume,
                closeTime = start + (index + 1) * interval - 1L,
                quoteVolume = close * volume,
                tradeCount = 100,
                takerBuyVolume = volume * 0.55
            )
        }
    }
}
