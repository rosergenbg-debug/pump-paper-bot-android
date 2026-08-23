package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PumpMachine2PolicyTest {
    private val feeRate = 0.0025
    private val entryCost = 1000.0
    private val buyFee = entryCost * feeRate
    private val amount = (entryCost - buyFee) / 1.0
    private val portfolio = FusionSimPortfolio(
        cashEur = 0.0,
        pumpAmount = amount,
        entryPrice = 1.0,
        entryCostEur = entryCost
    )

    @Test
    fun `pm2 waits below two percent net before timeout`() {
        val decision = PumpMachine2Policy.evaluate(
            portfolio = portfolio,
            previous = FusionStabilityState(),
            frame = null,
            bid = 1.020,
            feeRate = feeRate,
            now = 1_000_000L,
            shockReady = false,
            shockFailed = false,
            shockEntry = false,
            positionAgeMillis = 10L * 60L * 1000L
        )

        assertNull(decision.action)
        assertTrue(decision.tradeNetPercent < 2.0)
    }

    @Test
    fun `pm2 exits when executable net reaches two percent`() {
        val decision = PumpMachine2Policy.evaluate(
            portfolio = portfolio,
            previous = FusionStabilityState(),
            frame = null,
            bid = 1.026,
            feeRate = feeRate,
            now = 1_000_000L,
            shockReady = false,
            shockFailed = false,
            shockEntry = false,
            positionAgeMillis = 10L * 60L * 1000L
        )

        assertEquals(2.0, PumpMachine2Policy.TAKE_PROFIT_NET_PERCENT, 0.0)
        assertEquals("EXIT", decision.action)
        assertTrue(decision.tradeNetPercent >= 2.0)
        assertTrue(decision.reason.startsWith("V526_TAKE_PROFIT_PM2"))
    }
}
