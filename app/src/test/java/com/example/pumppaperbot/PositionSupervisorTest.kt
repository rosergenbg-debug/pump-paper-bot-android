package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PositionSupervisorTest {
    @Test
    fun stateJsonRoundTripPreservesExitBaselineAndScale() {
        val original = PositionSupervisionState(
            positionEntryTime = 100L,
            lastAttempt = 200L,
            lastSuccess = 190L,
            model = PositionSupervisorPolicy.PRO_MODEL,
            action = "EXIT",
            exitAdvised = true,
            exitAdvisedAt = 180L,
            exitBaselinePrice = 0.0025,
            exitBaselineDirection = -62,
            exitBaselineRsi = 38.0,
            exitBaselineDanger = 7,
            conditionDelta = -7,
            dangerLevel = 9,
            summary = "Давление продавцов усиливается",
            supportTier = "ЗАЩИТА ПРИБЫЛИ • PRO MAX",
            pnlPercent = 4.5,
            peakPnlPercent = 5.2,
            pullbackPercent = 0.7,
            bookStatus = "продавцы усилились",
            flowStatus = "покупки ослабевают",
            bitcoinStatus = "Bitcoin снижается",
            watchFor = "ускорение продаж",
            promptTokens = 123,
            completionTokens = 45
        )

        val restored = PositionSupervisionState.fromJson(original.toJson())

        assertEquals(original, restored)
    }

    @Test
    fun scalesAreClampedWhenReadingOldOrInvalidData() {
        val restored = PositionSupervisionState.fromJson(
            org.json.JSONObject()
                .put("conditionDelta", -99)
                .put("dangerLevel", 50)
        )

        assertEquals(-10, restored.conditionDelta)
        assertEquals(10, restored.dangerLevel)
    }

    @Test
    fun statusDistinguishesDeteriorationImprovementAndCancellation() {
        val exit = PositionSupervisionState(
            lastSuccess = 1L,
            exitAdvised = true,
            conditionDelta = -6,
            dangerLevel = 8,
            summary = "хуже"
        )
        assertTrue(PositionSupervisorPolicy.statusText(exit).contains("ухудшается"))
        assertTrue(PositionSupervisorPolicy.statusText(exit.copy(conditionDelta = 4)).contains("улучшается"))
        assertTrue(PositionSupervisorPolicy.statusText(exit.copy(action = "CANCEL_EXIT")).contains("ОТМЕНА ВЫХОДА"))
        assertFalse(PositionSupervisorPolicy.statusText(exit.copy(exitAdvised = false)).contains("ВЫХОД РЕКОМЕНДОВАН"))
    }

    @Test
    fun dispatcherUsesProForNewOrCriticalPositionAndFlashForRoutineChecks() {
        val now = 10_000_000L
        val state = PositionSupervisionState(
            positionEntryTime = 100L,
            lastAttempt = now - PositionSupervisorPolicy.FLASH_INTERVAL
        )
        assertEquals(
            PositionSupervisorPolicy.PRO_MODEL,
            PositionSupervisorPolicy.chooseModelForPosition(
                state, true, 200L, critical = false, forceCritical = false, now = now
            )
        )
        assertEquals(
            PositionSupervisorPolicy.PRO_MODEL,
            PositionSupervisorPolicy.chooseModelForPosition(
                state, true, 100L, critical = true, forceCritical = false, now = now
            )
        )
        assertEquals(
            PositionSupervisorPolicy.FLASH_MODEL,
            PositionSupervisorPolicy.chooseModelForPosition(
                state, true, 100L, critical = false, forceCritical = false, now = now
            )
        )
        assertEquals(
            null,
            PositionSupervisorPolicy.chooseModelForPosition(
                state.copy(lastAttempt = now), true, 100L,
                critical = false, forceCritical = false, now = now
            )
        )
    }

    @Test
    fun alertsOnlyOnFirstExitOrMaterialNewDeterioration() {
        val now = 20_000_000L
        val notified = PositionSupervisionState(
            exitAdvised = true,
            dangerLevel = 9,
            conditionDelta = -4,
            lastAlertAt = now - PositionAlertPolicy.MIN_REPEAT_INTERVAL_MILLIS,
            lastAlertDanger = 9,
            lastAlertConditionDelta = -4
        )

        assertFalse(PositionAlertPolicy.shouldAlert(notified, false, true, 9, -5, now))
        assertTrue(PositionAlertPolicy.shouldAlert(notified, false, true, 9, -6, now))
        assertTrue(PositionAlertPolicy.shouldAlert(
            notified.copy(lastAlertAt = now - 1_000L), false, true, 10, -5, now
        ))
        assertFalse(PositionAlertPolicy.shouldAlert(notified, false, false, 3, 2, now))
        assertTrue(PositionAlertPolicy.shouldAlert(PositionSupervisionState(), true, true, 8, 0, now))
    }

    @Test
    fun `rising price with buyers on 15s 60s and 5m rejects contradictory exit ten`() {
        val now = 30_000_000L
        val normalized = PositionExitConfirmationPolicy.normalize(
            result = exitResult(),
            previous = PositionSupervisionState(),
            snapshot = snapshot(direction = -10, book = -0.063),
            micro = MicroImpulseSnapshot(
                connected = true,
                updatedAt = now,
                aggressiveBuyPercent15s = 85.0,
                aggressiveBuyPercent60s = 63.0,
                aggressiveBuyPercent5m = 57.2,
                priceChange60sPercent = 0.25,
                topBookImbalance = -0.063,
                bitcoinAggressiveBuyPercent60s = 38.7,
                bitcoinPriceChange60sPercent = 0.0
            ),
            impulse = ImpulseSnapshot(
                candleTime = now,
                spotTakerRatio = 0.544,
                futuresTakerRatio = 0.438
            ),
            pnlPercent = -0.48,
            now = now
        )

        assertEquals("HOLD", normalized.action)
        assertTrue(normalized.dangerLevel <= 5)
        assertTrue(normalized.summary.contains("отскок"))
    }

    @Test
    fun `fresh recovery cancels an earlier exit instead of leaving it stuck`() {
        val now = 31_000_000L
        val normalized = PositionExitConfirmationPolicy.normalize(
            result = exitResult(),
            previous = PositionSupervisionState(exitAdvised = true, dangerLevel = 9),
            snapshot = snapshot(direction = -10, book = -0.05),
            micro = MicroImpulseSnapshot(
                connected = true,
                updatedAt = now,
                aggressiveBuyPercent15s = 70.0,
                aggressiveBuyPercent60s = 62.0,
                aggressiveBuyPercent5m = 56.0,
                priceChange60sPercent = 0.18
            ),
            impulse = ImpulseSnapshot(candleTime = now, spotTakerRatio = 0.55, futuresTakerRatio = 0.44),
            pnlPercent = -0.4,
            now = now
        )

        assertEquals("CANCEL_EXIT", normalized.action)
        assertTrue(normalized.conditionDelta > 0)
    }

    @Test
    fun `moderate recovery needs two cycles before clearing an earlier exit`() {
        val now = 31_500_000L
        val moderateMicro = MicroImpulseSnapshot(
            connected = true,
            updatedAt = now,
            aggressiveBuyPercent15s = 56.0,
            aggressiveBuyPercent60s = 54.0,
            aggressiveBuyPercent5m = 51.0,
            priceChange60sPercent = 0.05
        )
        val first = PositionExitConfirmationPolicy.normalize(
            result = exitResult(),
            previous = PositionSupervisionState(exitAdvised = true, dangerLevel = 9),
            snapshot = snapshot(direction = -10, book = -0.02),
            micro = moderateMicro,
            impulse = ImpulseSnapshot(candleTime = now, spotTakerRatio = 0.51),
            pnlPercent = -0.3,
            now = now
        )

        assertEquals("HOLD", first.action)
        assertEquals(1, first.conditionDelta)
        assertTrue(first.dangerLevel <= 6)

        val second = PositionExitConfirmationPolicy.normalize(
            result = exitResult(),
            previous = PositionSupervisionState(
                exitAdvised = true,
                dangerLevel = first.dangerLevel,
                exitRecoveryStreak = 1
            ),
            snapshot = snapshot(direction = -10, book = -0.02),
            micro = moderateMicro,
            impulse = ImpulseSnapshot(candleTime = now, spotTakerRatio = 0.51),
            pnlPercent = -0.2,
            now = now
        )

        assertEquals("CANCEL_EXIT", second.action)
        assertTrue(second.conditionDelta >= 2)
    }

    @Test
    fun `single moderate recovery does not hide renewed selling`() {
        val now = 31_750_000L
        val normalized = PositionExitConfirmationPolicy.normalize(
            result = exitResult(),
            previous = PositionSupervisionState(
                exitAdvised = true,
                dangerLevel = 6,
                exitRecoveryStreak = 1
            ),
            snapshot = snapshot(direction = -55, book = -0.16),
            micro = MicroImpulseSnapshot(
                connected = true,
                updatedAt = now,
                aggressiveBuyPercent15s = 41.0,
                aggressiveBuyPercent60s = 43.0,
                aggressiveBuyPercent5m = 45.0,
                priceChange60sPercent = -0.22,
                topBookImbalance = -0.16
            ),
            impulse = ImpulseSnapshot(candleTime = now, spotTakerRatio = 0.44, futuresTakerRatio = 0.45),
            pnlPercent = -1.0,
            now = now
        )

        assertEquals("EXIT", normalized.action)
    }

    @Test
    fun `multi group live selling keeps a real exit confirmed`() {
        val now = 32_000_000L
        val original = exitResult()
        val normalized = PositionExitConfirmationPolicy.normalize(
            result = original,
            previous = PositionSupervisionState(),
            snapshot = snapshot(direction = -55, book = -0.18),
            micro = MicroImpulseSnapshot(
                connected = true,
                updatedAt = now,
                aggressiveBuyPercent15s = 38.0,
                aggressiveBuyPercent60s = 41.0,
                aggressiveBuyPercent5m = 44.0,
                priceChange60sPercent = -0.30,
                topBookImbalance = -0.18,
                bitcoinAggressiveBuyPercent60s = 35.0,
                bitcoinPriceChange60sPercent = -0.20
            ),
            impulse = ImpulseSnapshot(candleTime = now, spotTakerRatio = 0.44, futuresTakerRatio = 0.43),
            pnlPercent = -1.0,
            now = now
        )

        assertEquals(original, normalized)
    }

    private fun exitResult() = SupervisorApiResult(
        action = "EXIT",
        conditionDelta = -10,
        dangerLevel = 10,
        summary = "выход",
        bookStatus = "стакан",
        flowStatus = "поток",
        bitcoinStatus = "Bitcoin",
        watchFor = "условие",
        promptTokens = 1,
        completionTokens = 1,
        repaired = false,
        finishReason = "stop"
    )

    private fun snapshot(direction: Int, book: Double) = LiveSnapshot(
        running = true,
        waitMode = "SELL",
        buyRsi = 45.0,
        lastSync = 1L,
        lastCandle = 1L,
        lastPrice = 100.0,
        lastRsi = 50.0,
        lastEma200 = 100.0,
        fundingRate = 0.0,
        strategyMode = StrategyV2.MODE_TREND,
        aggressive = false,
        readinessScore = -30,
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
        directionScore = direction,
        bookImbalance = book,
        livePrice = 99.52,
        livePriceAt = 1L
    )
}
