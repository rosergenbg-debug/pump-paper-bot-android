package com.example.pumppaperbot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class V322AuditFixesTest {
    @Test
    fun `ordinary alerts ring only on configured work days from 0615 to 2300`() {
        assertFalse(AlertSchedule.isMomentAllowed(375, 1380, Calendar.MONDAY, 374))
        assertTrue(AlertSchedule.isMomentAllowed(375, 1380, Calendar.MONDAY, 375))
        assertFalse(AlertSchedule.isMomentAllowed(375, 1380, Calendar.MONDAY, 1380))
        assertTrue(AlertSchedule.isMomentAllowed(375, 1380, Calendar.TUESDAY, 900))
        assertFalse(AlertSchedule.isMomentAllowed(375, 1380, Calendar.WEDNESDAY, 900))
        assertTrue(AlertSchedule.isMomentAllowed(375, 1380, Calendar.THURSDAY, 900))
        assertTrue(AlertSchedule.isMomentAllowed(375, 1380, Calendar.FRIDAY, 900))
        assertFalse(AlertSchedule.isMomentAllowed(375, 1380, Calendar.SATURDAY, 900))
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
