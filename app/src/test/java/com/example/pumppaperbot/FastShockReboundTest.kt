package com.example.pumppaperbot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FastShockReboundTest {
    private fun observation(
        at: Long,
        drawdown: Double = 3.8,
        rebound: Double = 0.9,
        change15: Double = 0.25,
        change60: Double = -0.20,
        buy5: Double = 63.0,
        buy15: Double = 59.0,
        buy60: Double = 52.0,
        activity: Double = 1.5,
        book: Double = 0.05
    ) = ShockReboundObservation(
        at = at,
        price = 1.0,
        drawdown3mPercent = drawdown,
        rebound3mPercent = rebound,
        change15sPercent = change15,
        change60sPercent = change60,
        buyer5sPercent = buy5,
        buyer15sPercent = buy15,
        buyer60sPercent = buy60,
        tradeAcceleration = activity,
        moneyActivityRatio = activity,
        bookImbalance = book
    )

    @Test fun `shock never buys while knife is still falling`() {
        val result = ShockReboundPolicy.update(
            ShockReboundState(),
            observation(
                at = 1_000L,
                rebound = 0.1,
                change15 = -0.8,
                change60 = -2.4,
                buy5 = 30.0,
                buy15 = 34.0,
                buy60 = 38.0
            )
        )
        assertTrue(result.active)
        assertFalse(result.ready)
    }

    @Test fun `shock rebound needs two observations separated by fifteen seconds`() {
        val first = ShockReboundPolicy.update(ShockReboundState(), observation(at = 10_000L))
        assertTrue(first.active)
        assertFalse(first.ready)
        val tooSoon = ShockReboundPolicy.update(first, observation(at = 20_000L))
        assertFalse(tooSoon.ready)
        val confirmed = ShockReboundPolicy.update(first, observation(at = 26_000L))
        assertTrue(confirmed.ready)
    }

    @Test fun `confirmed rebound fails when fast sellers retake control`() {
        val first = ShockReboundPolicy.update(ShockReboundState(), observation(at = 10_000L))
        val ready = ShockReboundPolicy.update(first, observation(at = 26_000L))
        assertTrue(ready.ready)
        val failed = ShockReboundPolicy.update(
            ready,
            observation(
                at = 42_000L,
                rebound = 0.2,
                change15 = -0.6,
                change60 = -2.0,
                buy5 = 34.0,
                buy15 = 40.0,
                buy60 = 42.0
            )
        )
        assertTrue(failed.failed)
        assertFalse(failed.ready)
    }
}
