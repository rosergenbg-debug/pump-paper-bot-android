package com.example.pumppaperbot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class V322AuditFixesTest {
    @Test
    fun `work mode preparation rings only on configured work days from 0615 to 2300`() {
        assertFalse(AlertSchedule.isMomentAllowed(AlertSchedule.MODE_WORK, 375, 1380, Calendar.MONDAY, 374))
        assertTrue(AlertSchedule.isMomentAllowed(AlertSchedule.MODE_WORK, 375, 1380, Calendar.MONDAY, 375))
        assertFalse(AlertSchedule.isMomentAllowed(AlertSchedule.MODE_WORK, 375, 1380, Calendar.MONDAY, 1380))
        assertTrue(AlertSchedule.isMomentAllowed(AlertSchedule.MODE_WORK, 375, 1380, Calendar.TUESDAY, 900))
        assertFalse(AlertSchedule.isMomentAllowed(AlertSchedule.MODE_WORK, 375, 1380, Calendar.WEDNESDAY, 900))
        assertTrue(AlertSchedule.isMomentAllowed(AlertSchedule.MODE_WORK, 375, 1380, Calendar.THURSDAY, 900))
        assertTrue(AlertSchedule.isMomentAllowed(AlertSchedule.MODE_WORK, 375, 1380, Calendar.FRIDAY, 900))
        assertFalse(AlertSchedule.isMomentAllowed(AlertSchedule.MODE_WORK, 375, 1380, Calendar.SATURDAY, 900))
    }

    @Test
    fun `daily and always modes are honored`() {
        assertTrue(AlertSchedule.isMomentAllowed(AlertSchedule.MODE_DAILY, 375, 1380, Calendar.SUNDAY, 900))
        assertFalse(AlertSchedule.isMomentAllowed(AlertSchedule.MODE_DAILY, 375, 1380, Calendar.SUNDAY, 374))
        assertTrue(AlertSchedule.isMomentAllowed(AlertSchedule.MODE_ALWAYS, 375, 1380, Calendar.SUNDAY, 30))
    }

    @Test
    fun `executed trades ring every day in daytime and always in 24 hour mode`() {
        assertTrue(AlertSchedule.isExecutedTradeMomentAllowed(AlertSchedule.MODE_WORK, 375, 1380, 900))
        assertTrue(AlertSchedule.isExecutedTradeMomentAllowed(AlertSchedule.MODE_DAILY, 375, 1380, 900))
        assertFalse(AlertSchedule.isExecutedTradeMomentAllowed(AlertSchedule.MODE_WORK, 375, 1380, 30))
        assertTrue(AlertSchedule.isExecutedTradeMomentAllowed(AlertSchedule.MODE_ALWAYS, 375, 1380, 30))
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
