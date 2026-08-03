package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V411PositionSupportPolicyTest {
    private val chart = ChartBundle(emptyList(), emptyList(), emptyList(), emptyList(), "test")

    @Test
    fun `profit from two percent selects one minute Pro support`() {
        val plan = PositionSupervisorPolicy.supportPlan(
            snapshot = snapshot(price = 102.5),
            state = PositionSupervisionState(),
            guard = PersonalPositionGuardState(entryTime = 10L, entryPrice = 100.0, peakPrice = 102.5),
            micro = MicroImpulseSnapshot(connected = true, updatedAt = 1_000_000L),
            forceCritical = false,
            now = 1_000_000L
        )

        assertEquals(PositionSupervisorPolicy.PRO_MODEL, plan.model)
        assertEquals(PositionSupervisorPolicy.PRO_RECHECK_INTERVAL, plan.intervalMillis)
        assertFalse(plan.maxReasoning)
        assertTrue(plan.tier.contains("УСИЛЕННЫЙ"))
    }

    @Test
    fun `profit from four percent selects maximum Pro reasoning`() {
        val plan = PositionSupervisorPolicy.supportPlan(
            snapshot = snapshot(price = 104.5),
            state = PositionSupervisionState(),
            guard = PersonalPositionGuardState(entryTime = 10L, entryPrice = 100.0, peakPrice = 104.5),
            micro = MicroImpulseSnapshot(connected = true, updatedAt = 1_000_000L),
            forceCritical = false,
            now = 1_000_000L
        )

        assertEquals(PositionSupervisorPolicy.PRO_MODEL, plan.model)
        assertTrue(plan.maxReasoning)
        assertTrue(plan.tier.contains("PRO MAX"))
    }

    @Test
    fun `seller reversal after profitable peak becomes emergency`() {
        val plan = PositionSupervisorPolicy.supportPlan(
            snapshot = snapshot(price = 102.8),
            state = PositionSupervisionState(),
            guard = PersonalPositionGuardState(entryTime = 10L, entryPrice = 100.0, peakPrice = 104.0),
            micro = MicroImpulseSnapshot(
                connected = true,
                updatedAt = 1_000_000L,
                aggressiveBuyPercent60s = 42.0,
                priceChange60sPercent = -0.25,
                topBookImbalance = -0.20
            ),
            forceCritical = false,
            now = 1_000_000L
        )

        assertTrue(plan.maxReasoning)
        assertTrue(plan.tier.contains("АВАРИЙНЫЙ"))
        assertTrue(plan.pullbackPercent > 1.0)
    }

    @Test
    fun `routine position remains Flash while foreground market stays two minutes`() {
        val plan = PositionSupervisorPolicy.supportPlan(
            snapshot = snapshot(price = 101.0),
            state = PositionSupervisionState(),
            guard = PersonalPositionGuardState(entryTime = 10L, entryPrice = 100.0, peakPrice = 101.0),
            micro = MicroImpulseSnapshot(),
            forceCritical = false,
            now = 1_000_000L
        )

        assertEquals(PositionSupervisorPolicy.FLASH_MODEL, plan.model)
        assertEquals(PositionSupervisorPolicy.FLASH_INTERVAL, plan.intervalMillis)
        assertEquals(
            PositionSupervisorPolicy.FOREGROUND_NORMAL_INTERVAL,
            PositionSupervisorPolicy.foregroundCycleInterval(plan)
        )
    }

    private fun snapshot(price: Double) = LiveSnapshot(
        running = true,
        waitMode = "SELL",
        buyRsi = 45.0,
        lastSync = 1_000_000L,
        lastCandle = 900_000L,
        lastPrice = price,
        lastRsi = 55.0,
        lastEma200 = 90.0,
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
        entryTime = 10L,
        highestClose = price,
        chart = chart,
        livePrice = price,
        livePriceAt = 1_000_000L
    )
}
