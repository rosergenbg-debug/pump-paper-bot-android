package com.example.pumppaperbot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V322AuditFixesTest {
    @Test
    fun `ordinary alerts start at 0615 every day`() {
        assertFalse(AlertSchedule.isMinuteAllowed(AlertSchedule.MODE_WORK, 375, 1380, 374))
        assertTrue(AlertSchedule.isMinuteAllowed(AlertSchedule.MODE_WORK, 375, 1380, 375))
        assertFalse(AlertSchedule.isMinuteAllowed(AlertSchedule.MODE_WORK, 375, 1380, 1380))
        assertTrue(AlertSchedule.isMinuteAllowed(AlertSchedule.MODE_ALWAYS, 375, 1380, 120))
    }

    @Test
    fun `Gemini experiment reads readiness from independent APP evaluation`() {
        val appReady = AppPaperEvaluation(
            candleTime = 123L,
            price = 1.0,
            action = "WAIT",
            reason = "APP ждёт последнее подтверждение",
            strategyMode = StrategyV2.MODE_TREND,
            highestClose = 1.0,
            readinessScore = 99
        )
        val appNotReady = appReady.copy(readinessScore = 98)
        val appAlreadySelling = appReady.copy(action = StrategyV2.ACTION_SELL)

        assertTrue(GeminiAppReadinessPolicy.isReady(appReady))
        assertFalse(GeminiAppReadinessPolicy.isReady(appNotReady))
        assertFalse(GeminiAppReadinessPolicy.isReady(appAlreadySelling))
    }
}
