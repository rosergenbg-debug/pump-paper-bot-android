package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HumanFactorVwapTest {
    @Test
    fun strongLiveSetupReachesOneHundred() {
        val candles = (0 until 61).map { i ->
            val base = 100.0
            val strong = i == 60
            val previous = i == 59
            PumpCandle(
                i * 60_000L,
                if (strong) 99.0 else base,
                base * 1.001,
                if (strong) 98.9 else base * .999,
                if (strong) 99.2 else base,
                100.0,
                i * 60_000L + 59_999,
                10_000.0,
                100,
                if (strong) 60.0 else if (previous) 40.0 else 50.0
            )
        }
        val result = HumanFactorVwapPolicy.evaluate(candles)
        assertEquals(100, result.first)
        assertTrue(result.second > 0.0)
    }

    @Test
    fun readinessMovesBeforeFullConfirmation() {
        val ordinary = (0 until 61).map { i ->
            PumpCandle(i * 60_000L, 100.0, 100.1, 99.9, 100.0, 100.0, i * 60_000L + 59_999, 10_000.0, 100, 45.0)
        }
        val warming = ordinary.toMutableList().also { rows ->
            val i = rows.lastIndex
            rows[i] = PumpCandle(i * 60_000L, 99.7, 100.0, 99.5, 99.65, 100.0, i * 60_000L + 59_999, 10_000.0, 100, 49.0)
        }
        val ordinaryScore = HumanFactorVwapPolicy.evaluate(ordinary).first
        val warmingScore = HumanFactorVwapPolicy.evaluate(warming).first
        assertTrue(warmingScore > ordinaryScore)
        assertTrue(warmingScore in 1..99)
    }

    @Test
    fun ordinaryMarketNeverCreatesConfirmation() {
        val candles = (0 until 61).map { i ->
            PumpCandle(i * 60_000L, 100.0, 100.1, 99.9, 100.0, 100.0, i * 60_000L + 59_999, 10_000.0, 100, 45.0)
        }
        assertTrue(HumanFactorVwapPolicy.evaluate(candles).first < HumanFactorVwapPolicy.READY)
    }
}
