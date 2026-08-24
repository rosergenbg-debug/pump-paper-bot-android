package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidityReleaseShadowPolicyTest {
    private fun samples(
        now: Long,
        oldBuy: Double = 100_000.0,
        oldSell: Double = 100_000.0,
        newBuy: Double = 95_000.0,
        newSell: Double = 55_000.0,
        oldBid: Double = 90_000.0,
        newBid: Double = 88_000.0,
        oldAsk: Double = 110_000.0,
        newAsk: Double = 82_000.0,
        oldPrice: Double = 1.0,
        newPrice: Double = 1.002
    ): List<LiquidityReleaseSample> = listOf(
        LiquidityReleaseSample(now - 105_000L, oldBuy, oldSell, oldBid, oldAsk, oldPrice),
        LiquidityReleaseSample(now - 90_000L, oldBuy, oldSell, oldBid, oldAsk, oldPrice),
        LiquidityReleaseSample(now - 75_000L, oldBuy, oldSell, oldBid, oldAsk, oldPrice),
        LiquidityReleaseSample(now - 60_000L, oldBuy, oldSell, oldBid, oldAsk, oldPrice),
        LiquidityReleaseSample(now - 30_000L, newBuy, newSell, newBid, newAsk, newPrice),
        LiquidityReleaseSample(now - 15_000L, newBuy, newSell, newBid, newAsk, newPrice),
        LiquidityReleaseSample(now, newBuy, newSell, newBid, newAsk, newPrice)
    )

    @Test
    fun `release requires persistence instead of one disappearing ask snapshot`() {
        val now = 2_000_000L
        val first = LiquidityReleaseShadowPolicy.evaluate(samples(now), LiquidityReleaseRuntime(), now)
        assertEquals(LiquidityReleaseState.ASK_RETREAT, first.state)
        assertTrue(first.runtime.candidateSince > 0L)

        val later = now + 45_000L
        val confirmed = LiquidityReleaseShadowPolicy.evaluate(samples(later), first.runtime, later)
        assertEquals(LiquidityReleaseState.LIQUIDITY_RELEASED, confirmed.state)
        assertTrue(confirmed.persistenceSeconds >= 45L)
    }

    @Test
    fun `large two-sided turnover remains balance when outflow does not shrink`() {
        val now = 3_000_000L
        val result = LiquidityReleaseShadowPolicy.evaluate(
            samples(now, oldBuy = 500_000.0, oldSell = 500_000.0, newBuy = 520_000.0,
                newSell = 510_000.0, oldAsk = 300_000.0, newAsk = 305_000.0),
            LiquidityReleaseRuntime(),
            now
        )
        assertEquals(LiquidityReleaseState.BALANCE, result.state)
    }

    @Test
    fun `returned sellers cancel a recent release`() {
        val now = 4_000_000L
        val result = LiquidityReleaseShadowPolicy.evaluate(
            samples(now, oldSell = 100_000.0, newSell = 135_000.0, oldAsk = 100_000.0, newAsk = 120_000.0),
            LiquidityReleaseRuntime(
                candidateSince = now - 90_000L,
                releasedAt = now - 60_000L,
                previousState = LiquidityReleaseState.LIQUIDITY_RELEASED
            ),
            now
        )
        assertEquals(LiquidityReleaseState.FALSE_RELEASE, result.state)
        assertEquals(0L, result.runtime.releasedAt)
    }

    @Test
    fun `insufficient history fails closed as warming up`() {
        val now = 5_000_000L
        val result = LiquidityReleaseShadowPolicy.evaluate(samples(now).takeLast(2), LiquidityReleaseRuntime(), now)
        assertEquals(LiquidityReleaseState.WARMING_UP, result.state)
        assertTrue(result.shadowOnly)
    }
}
