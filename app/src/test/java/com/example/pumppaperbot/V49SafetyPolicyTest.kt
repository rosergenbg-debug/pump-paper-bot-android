package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V49SafetyPolicyTest {
    @Test fun `paper execution applies symmetric adverse slippage`() {
        assertEquals(100.05, PaperExecutionPolicy.executionPrice(100.0, "BUY"), 0.000001)
        assertEquals(99.95, PaperExecutionPolicy.executionPrice(100.0, "SELL"), 0.000001)
        assertEquals(99.95, PaperExecutionPolicy.executionPrice(100.0, StrategyV2.ACTION_SELL_HALF), 0.000001)
    }

    @Test fun `app reserve cycle accepts a fresh signal but never chases price`() {
        val now = 2_000_000L
        val evaluation = AppPaperEvaluation(
            candleTime = now - 15L * 60L * 1000L,
            price = 100.0,
            action = "BUY",
            reason = "test",
            strategyMode = StrategyV2.MODE_TREND,
            highestClose = 100.0
        )
        val accepted = PaperExecutionPolicy.prepareAppEvaluation(evaluation, 100.5, now)
        val chased = PaperExecutionPolicy.prepareAppEvaluation(evaluation, 102.0, now)
        val stale = PaperExecutionPolicy.prepareAppEvaluation(
            evaluation.copy(candleTime = now - PaperExecutionPolicy.APP_MAX_DECISION_AGE_MILLIS - 1L),
            100.0,
            now
        )

        assertEquals("BUY", accepted.action)
        assertEquals(100.5 * (1.0 + PumpBotEngine.slippage), accepted.price, 0.000001)
        assertEquals("WAIT", chased.action)
        assertTrue(chased.reason.contains("не догоняем"))
        assertEquals("WAIT", stale.action)
    }

    @Test fun `old DeepSeek trade recommendation is neutralized`() {
        val now = 5_000_000L
        val stale = DeepSeekPrimaryState(
            lastSuccess = now - PaperExecutionPolicy.MAX_DECISION_AGE_MILLIS - 1L,
            action = "BUY",
            direction = 80,
            confidence = 90
        )
        val fresh = stale.copy(lastSuccess = now - 1_000L)

        assertEquals("HOLD", DeepSeekPaperPolicy.executableRecommendation(stale, now).action)
        assertEquals("BUY", DeepSeekPaperPolicy.executableRecommendation(fresh, now).action)
    }

    @Test fun `entry reminder is bounded by time repeat count and chase protection`() {
        val now = 10_000_000L
        val reminder = EntryAlertReminder("APP", "1", now - 1_000L, 100.0, now - 1_000L, 0)

        assertTrue(EntryAlertReminderPolicy.shouldKeep(reminder, 101.0, now))
        assertFalse(EntryAlertReminderPolicy.shouldKeep(reminder, 101.6, now))
        assertFalse(EntryAlertReminderPolicy.shouldKeep(reminder.copy(repeats = 2), 100.0, now))
        assertFalse(EntryAlertReminderPolicy.shouldKeep(
            reminder.copy(signalAt = now - EntryAlertReminderPolicy.MAX_SIGNAL_AGE_MILLIS - 1L),
            100.0,
            now
        ))
    }

    @Test fun `Gemini position advisor is immediate then quota aware by cadence`() {
        val now = 20_000_000L
        val empty = GeminiPositionAdvisorState()
        assertTrue(GeminiPositionAdvisorPolicy.shouldRun(empty, true, 100L, false, false, now))
        val routine = empty.copy(positionEntryTime = 100L, lastAttempt = now)
        assertFalse(GeminiPositionAdvisorPolicy.shouldRun(routine, true, 100L, false, false, now))
        assertTrue(GeminiPositionAdvisorPolicy.shouldRun(
            routine.copy(lastAttempt = now - GeminiPositionAdvisorPolicy.REGULAR_INTERVAL),
            true, 100L, false, false, now
        ))
        assertTrue(GeminiPositionAdvisorPolicy.shouldRun(routine, true, 100L, false, true, now))
        assertFalse(GeminiPositionAdvisorPolicy.shouldRun(empty, false, 0L, true, true, now))
    }
}
