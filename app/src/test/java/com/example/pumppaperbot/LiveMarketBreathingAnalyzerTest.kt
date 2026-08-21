package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LiveMarketBreathingAnalyzerTest {
    private fun sample(
        at: Long,
        buyer: Double = 60.0,
        price: Double = 1.0,
        change60s: Double = 0.05,
        buyNotional: Double = 600.0,
        sellNotional: Double = 400.0,
        book: Double = 0.05,
        bitcoinBuyer: Double = 50.0,
        bitcoinChange: Double = 0.0
    ) = LiveBreathingSample(
        at = at,
        priceUsdt = price,
        pumpBuyerPercent = buyer,
        pumpChange60sPercent = change60s,
        bookImbalance = book,
        bitcoinBuyerPercent = bitcoinBuyer,
        bitcoinChange60sPercent = bitcoinChange,
        pumpBuyNotional60s = buyNotional,
        pumpSellNotional60s = sellNotional
    )

    private fun completedMinutes(
        startMinute: Long,
        count: Int,
        buyer: Double = 60.0,
        buyNotional: Double = 600.0,
        sellNotional: Double = 400.0,
        priceStep: Double = 0.0005,
        change60s: Double = 0.05,
        book: Double = 0.05
    ): List<LiveBreathingSample> = (0 until count).map { index ->
        sample(
            at = (startMinute + index) * 60_000L + 45_000L,
            buyer = buyer,
            price = 1.0 + index * priceStep,
            change60s = change60s,
            buyNotional = buyNotional,
            sellNotional = sellNotional,
            book = book
        )
    }

    @Test fun `thirty minute bar is not fabricated from a few minutes`() {
        val now = 200L * 60_000L + 30_000L
        val result = LiveMarketBreathingAnalyzer.analyze(
            completedMinutes(196L, 4),
            now
        )
        val thirty = result.horizons.first { it.minutes == 30 }

        assertNull(thirty.score)
        assertEquals(4, thirty.samples)
    }

    @Test fun `twenty minute bar uses real twenty minute scale and becomes available`() {
        val now = 300L * 60_000L + 30_000L
        val neutralFlowWithPriceRise = completedMinutes(
            startMinute = 270L,
            count = 30,
            buyer = 50.0,
            buyNotional = 500.0,
            sellNotional = 500.0,
            priceStep = 0.00055,
            change60s = 0.055,
            book = 0.0
        )
        val result = LiveMarketBreathingAnalyzer.analyze(neutralFlowWithPriceRise, now)
        val twenty = result.horizons.first { it.minutes == 20 }

        assertNotNull(twenty.score)
        assertTrue((twenty.priceChangePercent ?: 0.0) > 0.8)
        assertTrue((twenty.score ?: 0) > 10)
    }

    @Test fun `forming current minute cannot rewrite fifteen and thirty minute bars`() {
        val minute = 400L
        val history = completedMinutes(minute - 40L, 40)
        val earlyCurrent = sample(
            at = minute * 60_000L + 10_000L,
            buyer = 80.0,
            change60s = 0.8,
            buyNotional = 800.0,
            sellNotional = 200.0,
            book = 0.5
        )
        val violentLateCurrent = sample(
            at = minute * 60_000L + 40_000L,
            buyer = 10.0,
            change60s = -1.2,
            buyNotional = 100.0,
            sellNotional = 900.0,
            book = -0.8
        )

        val early = LiveMarketBreathingAnalyzer.analyze(
            history + earlyCurrent,
            minute * 60_000L + 20_000L
        )
        val late = LiveMarketBreathingAnalyzer.analyze(
            history + earlyCurrent + violentLateCurrent,
            minute * 60_000L + 50_000L
        )

        assertTrue((late.instantScore ?: 0) < 0)
        assertEquals(
            early.horizons.first { it.minutes == 15 }.score,
            late.horizons.first { it.minutes == 15 }.score
        )
        assertEquals(
            early.horizons.first { it.minutes == 30 }.score,
            late.horizons.first { it.minutes == 30 }.score
        )
    }

    @Test fun `neutral micro advantage stays in dead zone instead of changing colour`() {
        val now = 500L * 60_000L + 30_000L
        val quiet = completedMinutes(
            startMinute = 480L,
            count = 20,
            buyer = 51.0,
            buyNotional = 510.0,
            sellNotional = 490.0,
            priceStep = 0.0,
            change60s = 0.0,
            book = 0.0
        )
        val result = LiveMarketBreathingAnalyzer.analyze(quiet, now)

        assertEquals(0, result.horizons.first { it.minutes == 15 }.score)
    }

    @Test fun `single violent tick cannot overturn sustained positive breathing`() {
        val minute = 600L
        val history = completedMinutes(minute - 40L, 40)
        val violent = sample(
            at = minute * 60_000L + 30_000L,
            buyer = 8.0,
            change60s = -1.4,
            buyNotional = 80.0,
            sellNotional = 920.0,
            book = -0.9
        )
        val result = LiveMarketBreathingAnalyzer.analyze(
            history + violent,
            minute * 60_000L + 35_000L
        )

        assertTrue((result.instantScore ?: 0) < 0)
        assertTrue((result.horizons.first { it.minutes == 15 }.score ?: 0) > 0)
        assertTrue((result.horizons.first { it.minutes == 30 }.score ?: 0) > 0)
        assertTrue((result.normalScore ?: 0) > 0)
    }

    @Test fun `one opposite completed minute cannot flip thirty minute direction`() {
        val now = 700L * 60_000L + 30_000L
        val positive = completedMinutes(659L, 40)
        val oneBadMinute = sample(
            at = 699L * 60_000L + 45_000L,
            buyer = 3.0,
            price = 1.019,
            change60s = -2.0,
            buyNotional = 30.0,
            sellNotional = 970.0,
            book = -1.0
        )
        val result = LiveMarketBreathingAnalyzer.analyze(
            positive.dropLast(1) + oneBadMinute,
            now
        )

        assertTrue((result.horizons.first { it.minutes == 30 }.score ?: 0) >= 0)
    }

    @Test fun `fusion waits for genuine thirty minute coverage`() {
        val now = 800L * 60_000L + 30_000L
        val shortHistory = completedMinutes(780L, 20)
        val fullHistory = completedMinutes(760L, 40)

        val shortFrame = FusionFlowPolicy.frame(
            LiveMarketBreathingAnalyzer.analyze(shortHistory, now)
        )
        val fullFrame = FusionFlowPolicy.frame(
            LiveMarketBreathingAnalyzer.analyze(fullHistory, now)
        )

        assertNull(shortFrame)
        assertNotNull(fullFrame)
        assertTrue(fullFrame!!.buySignal)
    }

    @Test fun `experiment stays nervous but never exceeds fifteen point gap`() {
        val minute = 900L
        val history = completedMinutes(minute - 40L, 40, buyer = 55.0)
        val current = sample(
            at = minute * 60_000L + 20_000L,
            buyer = 82.0,
            change60s = 0.9,
            buyNotional = 820.0,
            sellNotional = 180.0,
            book = 0.4
        )
        val result = LiveMarketBreathingAnalyzer.analyze(
            history + current,
            minute * 60_000L + 25_000L
        )

        assertNotNull(result.normalScore)
        assertNotNull(result.experimentScore)
        assertTrue(abs(result.experimentScore!! - result.normalScore!!) <= 15)
        assertTrue(result.experimentScore!! >= result.normalScore!!)
    }

    @Test fun `insufficient fresh history does not invent a normal score`() {
        val now = 30L * 60L * 60L * 1000L
        val old = sample(
            at = now - RollingCsvRetention.RETENTION_MILLIS - 1L,
            buyer = 5.0,
            price = 2.0,
            change60s = -2.0,
            buyNotional = 50.0,
            sellNotional = 950.0,
            book = -1.0
        )
        val current = listOf(
            sample(now - 15_000L, buyer = 60.0),
            sample(now - 5_000L, buyer = 60.0)
        )

        val result = LiveMarketBreathingAnalyzer.analyze(listOf(old) + current, now)

        assertEquals(0, result.historyMinutes)
        assertNull(result.normalScore)
        assertNull(result.horizons.first { it.minutes == 30 }.score)
    }

    @Test fun `paused feed preserves last fixed window instead of fake decay`() {
        val lastMinute = 1_000L
        val samples = completedMinutes(lastMinute - 90L, 90)
        val freshNow = lastMinute * 60_000L + 10_000L
        val pausedNow = (lastMinute + 5L) * 60_000L + 10_000L
        val fresh = LiveMarketBreathingAnalyzer.analyze(samples, freshNow).flowWave
        val paused = LiveMarketBreathingAnalyzer.analyze(samples, pausedNow).flowWave

        assertTrue((fresh.latest?.score15m ?: 0) > 0)
        assertEquals(fresh.latest?.score15m, paused.latest?.score15m)
        assertTrue(paused.state.contains("ПРИОСТАНОВЛЕНА"))
    }

    @Test fun `bitcoin context is capped and cannot manufacture pump wave`() {
        val pulse = LiveMarketBreathingAnalyzer.flowPulse(
            sample(
                at = 1L,
                buyer = 50.0,
                change60s = 0.0,
                buyNotional = 500.0,
                sellNotional = 500.0,
                book = 0.0,
                bitcoinBuyer = 100.0,
                bitcoinChange = 10.0
            )
        )

        assertTrue(abs(pulse) <= 5.01)
    }
}
