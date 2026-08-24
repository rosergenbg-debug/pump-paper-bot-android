package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V519FastIntegrationTest {

    @Test fun `money pressure distinguishes seller force from absorption`() {
        val selling = MicroImpulseSnapshot(
            connected = true,
            updatedAt = 1_000L,
            aggressiveBuyPercent60s = 40.0,
            aggressiveBuyPercent5m = 45.0,
            buyNotional60s = 120.0,
            sellNotional60s = 180.0,
            buyNotional5m = 450.0,
            sellNotional5m = 550.0,
            priceChange60sPercent = -0.20,
            flowHistorySeconds = 300L
        )
        val sellerPressure = FastMoneyPressurePolicy.evaluate(selling)
        assertTrue(sellerPressure.heavySelling)
        assertFalse(sellerPressure.absorptionPossible)
        assertEquals(1.5, sellerPressure.activityRatio60sTo5m ?: 0.0, 0.0001)

        val absorption = FastMoneyPressurePolicy.evaluate(
            selling.copy(priceChange60sPercent = -0.02)
        )
        assertFalse(absorption.heavySelling)
        assertTrue(absorption.absorptionPossible)
    }

    @Test fun `virtual participant trade sounds stop when Serge owns a position`() {
        assertTrue(VirtualTradeAlertPolicy.shouldNotify("BUY", userPositionOpen = false))
        assertFalse(VirtualTradeAlertPolicy.shouldNotify("BUY", userPositionOpen = true))
        assertFalse(VirtualTradeAlertPolicy.shouldNotify("EXIT", userPositionOpen = true))
    }

    @Test fun `position alert has one yellow preparation then immediate critical escalation`() {
        val now = 10_000_000L
        val calm = PositionSupervisionState(dangerLevel = 3)
        assertTrue(PositionAlertPolicy.shouldAlert(
            previous = calm,
            firstExit = false,
            stillExit = false,
            dangerLevel = 7,
            conditionDelta = -1,
            now = now
        ))

        val afterYellow = calm.copy(
            dangerLevel = 7,
            lastAlertAt = now,
            lastAlertDanger = 7,
            lastAlertConditionDelta = -1
        )
        assertFalse(PositionAlertPolicy.shouldAlert(
            previous = afterYellow,
            firstExit = false,
            stillExit = false,
            dangerLevel = 7,
            conditionDelta = -1,
            now = now + 30_000L
        ))
        assertTrue(PositionAlertPolicy.shouldAlert(
            previous = afterYellow,
            firstExit = false,
            stillExit = false,
            dangerLevel = 9,
            conditionDelta = -3,
            now = now + 45_000L
        ))
    }

    @Test fun `confirmed shock rebound can enter before slow horizons turn green but never bypasses cooldown`() {
        val now = 20_000_000L
        val buy = FusionStabilityPolicy.evaluate(
            inPosition = false,
            entryPrice = 0.0,
            previous = FusionStabilityState(),
            frame = null,
            bid = 100.0,
            feeRate = 0.0025,
            now = now,
            shockReady = true,
            entryObservation = capitalReadyObservation(null, now, ask = 1.0, shock = true)
        )
        assertEquals("BUY", buy.action)
        assertTrue(buy.reason.startsWith("SHOCK_REBOUND_ENTRY"))

        val blocked = FusionStabilityPolicy.evaluate(
            inPosition = false,
            entryPrice = 0.0,
            previous = FusionStabilityState(cooldownUntil = now + 60_000L),
            frame = null,
            bid = 100.0,
            feeRate = 0.0025,
            now = now,
            shockReady = true,
            entryObservation = capitalReadyObservation(null, now, ask = 1.0, shock = true)
        )
        assertNull(blocked.action)
        assertTrue(blocked.reason.startsWith("COOLDOWN"))
    }

    @Test fun `failed shock rebound exits shock paper position on next fast observation`() {
        val decision = FusionStabilityPolicy.evaluate(
            inPosition = true,
            entryPrice = 100.0,
            previous = FusionStabilityState(peakBid = 101.0),
            frame = null,
            bid = 100.4,
            feeRate = 0.0025,
            now = 30_000_000L,
            positionAgeMillis = 16_000L,
            shockReady = false,
            shockFailed = true,
            shockEntry = true
        )
        assertEquals("EXIT", decision.action)
        assertTrue(decision.reason.startsWith("SHOCK_REBOUND_FAILED"))
    }

    @Test fun `fast manual position warning needs real seller pressure and escalates once`() {
        val now = 40_000_000L
        val micro = MicroImpulseSnapshot(
            connected = true,
            updatedAt = now,
            aggressiveBuyPercent15s = 42.0,
            aggressiveBuyPercent60s = 40.0,
            aggressiveBuyPercent5m = 45.0,
            buyNotional60s = 120.0,
            sellNotional60s = 180.0,
            buyNotional5m = 450.0,
            sellNotional5m = 550.0,
            priceChange60sPercent = -1.0,
            flowHistorySeconds = 300L
        )
        val shock = ShockReboundState(
            active = true,
            shockAt = now,
            lastObservedAt = now,
            drawdown3mPercent = 3.0,
            rebound3mPercent = 0.1
        )
        val yellow = FastPositionWarningPolicy.evaluate(
            FastPositionWarningState(), true, shock, micro, now
        )
        assertEquals(FastPositionWarningPolicy.PREPARE, yellow.band)
        assertTrue(yellow.shouldAlert)

        val repeated = FastPositionWarningPolicy.evaluate(
            yellow.next, true, shock, micro, now + 15_000L
        )
        assertFalse(repeated.shouldAlert)

        val criticalShock = shock.copy(
            failed = true,
            lastObservedAt = now + 30_000L,
            drawdown3mPercent = 4.5
        )
        val critical = FastPositionWarningPolicy.evaluate(
            yellow.next, true, criticalShock, micro.copy(updatedAt = now + 30_000L), now + 30_000L
        )
        assertEquals(FastPositionWarningPolicy.CRITICAL, critical.band)
        assertTrue(critical.shouldAlert)
    }
}
