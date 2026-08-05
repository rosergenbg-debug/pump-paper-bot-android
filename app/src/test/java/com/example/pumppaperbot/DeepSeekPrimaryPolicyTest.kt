package com.example.pumppaperbot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekPrimaryPolicyTest {
    @Test fun `primary DeepSeek runs without an open position`() {
        assertTrue(DeepSeekPrimaryPolicy.shouldRun(
            DeepSeekPrimaryState(), hasMarketData = true, force = false, now = 1_000L
        ))
    }

    @Test fun `primary DeepSeek waits two minutes between paid calls`() {
        val state = DeepSeekPrimaryState(lastAttempt = 1_000L)
        assertFalse(DeepSeekPrimaryPolicy.shouldRun(
            state, hasMarketData = true, force = false,
            now = 1_000L + DeepSeekPrimaryPolicy.INTERVAL - 1L
        ))
        assertTrue(DeepSeekPrimaryPolicy.shouldRun(
            state, hasMarketData = true, force = false,
            now = 1_000L + DeepSeekPrimaryPolicy.INTERVAL
        ))
    }

    @Test fun `five euro cost level is informational and detected without blocking`() {
        assertFalse(DeepSeekCostWarningPolicy.warningReached(4.999))
        assertTrue(DeepSeekCostWarningPolicy.warningReached(5.0))
    }

    @Test fun `rejected buy cannot execute but remains visible for diagnosis`() {
        assertTrue(DeepSeekTradeVerificationPolicy.finalAction("BUY", false, false) == "WATCH")
        assertTrue(DeepSeekTradeVerificationPolicy.acceptedDirection("BUY", false, 82) == 82)
        assertTrue(DeepSeekTradeVerificationPolicy.acceptedConfidence("BUY", false, 91) == 91)
    }

    @Test fun `rejected exit holds the open position but preserves warning direction`() {
        assertTrue(DeepSeekTradeVerificationPolicy.finalAction("EXIT", false, true) == "HOLD")
        assertTrue(DeepSeekTradeVerificationPolicy.acceptedDirection("EXIT", false, -74) == -74)
    }

    @Test fun `material signal change starts primary DeepSeek before interval`() {
        val state = DeepSeekPrimaryState(lastAttempt = 1_000L)
        assertTrue(DeepSeekPrimaryPolicy.shouldRun(
            state, hasMarketData = true, force = false, now = 2_000L, materialChange = true
        ))
    }

    @Test fun `yellow entry support permits a one minute paid cadence`() {
        val state = DeepSeekPrimaryState(lastAttempt = 1_000L)
        assertFalse(DeepSeekPrimaryPolicy.shouldRun(
            state,
            hasMarketData = true,
            force = false,
            now = 1_000L + DeepSeekActionLevelPolicy.INTENSIVE_INTERVAL_MILLIS - 1L,
            intervalMillis = DeepSeekActionLevelPolicy.INTENSIVE_INTERVAL_MILLIS
        ))
        assertTrue(DeepSeekPrimaryPolicy.shouldRun(
            state,
            hasMarketData = true,
            force = false,
            now = 1_000L + DeepSeekActionLevelPolicy.INTENSIVE_INTERVAL_MILLIS,
            intervalMillis = DeepSeekActionLevelPolicy.INTENSIVE_INTERVAL_MILLIS
        ))
    }

    @Test fun `Gemini routine waits two hours unless position is open`() {
        val lastSuccess = 1_000L
        assertFalse(GeminiRoutinePolicy.allowed(
            lastSuccess, positionOpen = false, force = false,
            now = lastSuccess + GeminiRoutinePolicy.NORMAL_INTERVAL - 1L
        ))
        assertTrue(GeminiRoutinePolicy.allowed(
            lastSuccess, positionOpen = true, force = false, now = lastSuccess + 1L
        ))
    }

    @Test fun `Gemini keeps half for routine work and releases all capacity to position advice`() {
        assertTrue(GeminiRequestBudget.activeLimit(false) == GeminiRequestBudget.NORMAL_REQUESTS_PER_DAY)
        assertTrue(GeminiRequestBudget.activeLimit(true) == GeminiRequestBudget.NORMAL_REQUESTS_PER_DAY)
        assertTrue(
            GeminiRequestBudget.activeLimit(positionOpen = true, positionPriority = true) ==
                GeminiRequestBudget.MAX_REQUESTS_PER_DAY
        )
        assertTrue(GeminiRequestBudget.NORMAL_REQUESTS_PER_DAY == 12)
        assertTrue(GeminiRequestBudget.MAX_REQUESTS_PER_DAY - GeminiRequestBudget.NORMAL_REQUESTS_PER_DAY == 13)
    }
}
