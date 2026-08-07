package com.example.pumppaperbot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalPositionGuardPolicyTest {
    @Test
    fun `an old rapid drop does not remain critical during confirmed live recovery`() {
        val now = 40_000_000L
        val outcome = PersonalPositionGuardPolicy.evaluate(
            state = PersonalPositionGuardState(
                entryTime = 1L,
                entryPrice = 100.0,
                peakPrice = 101.0,
                lastAlertKey = "РЕЗКОЕ ПАДЕНИЕ",
                lastAlertAt = now - 60_000L,
                criticalActive = true
            ),
            snapshot = snapshot(),
            micro = MicroImpulseSnapshot(
                connected = true,
                updatedAt = now,
                aggressiveBuyPercent15s = 85.0,
                aggressiveBuyPercent60s = 63.0,
                aggressiveBuyPercent5m = 57.2,
                priceChange60sPercent = 0.25
            ),
            price = 100.5,
            now = now
        )

        assertFalse(outcome.state.criticalActive)
        assertTrue(outcome.alertReason.isBlank())
    }

    @Test
    fun `isolated app sell stays warning only`() {
        val now = 41_000_000L
        val outcome = PersonalPositionGuardPolicy.evaluate(
            state = PersonalPositionGuardState(entryTime = 1L, entryPrice = 100.0, peakPrice = 100.0),
            snapshot = snapshot().copy(
                rapidDrop = RapidDropState.none(),
                sellSignal = true,
                signalReason = "закрытая свеча"
            ),
            micro = MicroImpulseSnapshot(connected = true, updatedAt = now),
            price = 99.5,
            now = now
        )

        assertFalse(outcome.state.criticalActive)
        assertTrue(outcome.alertReason.contains("APP"))
    }

    private fun snapshot() = LiveSnapshot(
        running = true,
        waitMode = "SELL",
        buyRsi = 45.0,
        lastSync = 1L,
        lastCandle = 1L,
        lastPrice = 100.5,
        lastRsi = 50.0,
        lastEma200 = 100.0,
        fundingRate = 0.0,
        strategyMode = StrategyV2.MODE_TREND,
        aggressive = false,
        readinessScore = 0,
        trendReadiness = 0,
        shockReadiness = 0,
        partialTaken = false,
        buySignal = false,
        sellSignal = false,
        signalAction = "WAIT",
        signalReason = "test",
        entryPrice = 100.0,
        entryTime = 1L,
        highestClose = 101.0,
        chart = ChartBundle(emptyList(), emptyList(), emptyList(), emptyList(), "test"),
        directionScore = -10,
        rapidDrop = RapidDropState(
            active = true,
            dropPercent = 25.5,
            reboundPercent = 2.0,
            recoveryConfirmed = false
        ),
        livePrice = 100.5,
        livePriceAt = 1L
    )
}
