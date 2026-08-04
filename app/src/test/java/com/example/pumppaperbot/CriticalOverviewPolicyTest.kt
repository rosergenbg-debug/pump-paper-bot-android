package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CriticalOverviewPolicyTest {
    @Test
    fun `confirmed buyers and supportive market produce green entry overview`() {
        val result = CriticalOverviewPolicy.evaluate(strongBullish(positionOpen = false, actionLevel = 9))

        assertEquals(CriticalOverviewBand.GREEN, result.band)
        assertTrue(result.overallScore >= 25)
        assertTrue(result.headline.contains("ВХОД"))
    }

    @Test
    fun `hard entry veto remains red despite superficially bullish flow`() {
        val result = CriticalOverviewPolicy.evaluate(
            strongBullish(positionOpen = false, actionLevel = 9).copy(hardEntryVeto = true)
        )

        assertEquals(CriticalOverviewBand.RED, result.band)
        assertTrue(result.overallScore <= -85)
    }

    @Test
    fun `exit danger reverses the DeepSeek contribution and worsening flow is red`() {
        val result = CriticalOverviewPolicy.evaluate(CriticalOverviewEvidence(
            positionOpen = true,
            actionLevel = 9,
            directionScore = -70,
            hardEntryVeto = false,
            rapidDrop = false,
            bookImbalance = -0.55,
            pumpBuyerPercent60s = 31.0,
            pumpPriceChange60sPercent = -0.45,
            spotTakerRatio = 0.34,
            futuresTakerRatio = 0.32,
            bitcoinBuyerPercent60s = 38.0,
            bitcoinPriceChange60sPercent = -0.22,
            openInterestChangePercent = 1.4
        ))

        assertEquals(CriticalOverviewBand.RED, result.band)
        assertTrue(result.overallScore <= -55)
        assertTrue(result.headline.contains("ВЫХОД"))
    }

    @Test
    fun `missing external layers stay unavailable instead of becoming invented certainty`() {
        val result = CriticalOverviewPolicy.evaluate(CriticalOverviewEvidence(
            positionOpen = false,
            actionLevel = 5,
            directionScore = 0,
            hardEntryVeto = false,
            rapidDrop = false,
            bookImbalance = null,
            pumpBuyerPercent60s = null,
            pumpPriceChange60sPercent = null,
            spotTakerRatio = null,
            futuresTakerRatio = null,
            bitcoinBuyerPercent60s = null,
            bitcoinPriceChange60sPercent = null,
            openInterestChangePercent = null
        ))

        assertEquals(CriticalOverviewBand.YELLOW, result.band)
        assertTrue(result.metrics.count { it.score == null } >= 6)
    }

    private fun strongBullish(positionOpen: Boolean, actionLevel: Int) = CriticalOverviewEvidence(
        positionOpen = positionOpen,
        actionLevel = actionLevel,
        directionScore = 72,
        hardEntryVeto = false,
        rapidDrop = false,
        bookImbalance = 0.48,
        pumpBuyerPercent60s = 71.0,
        pumpPriceChange60sPercent = 0.38,
        spotTakerRatio = 0.68,
        futuresTakerRatio = 0.65,
        bitcoinBuyerPercent60s = 61.0,
        bitcoinPriceChange60sPercent = 0.16,
        openInterestChangePercent = 1.2
    )
}
