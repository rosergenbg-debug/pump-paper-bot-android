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

    @Test fun `primary DeepSeek waits ten minutes between paid calls`() {
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

    @Test fun `Gemini keeps half of daily capacity for an open position`() {
        assertTrue(GeminiRequestBudget.activeLimit(false) * 2 == GeminiRequestBudget.activeLimit(true))
        assertTrue(GeminiRequestBudget.activeLimit(false) == GeminiRequestBudget.NORMAL_REQUESTS_PER_DAY)
        assertTrue(GeminiRequestBudget.activeLimit(true) == GeminiRequestBudget.MAX_REQUESTS_PER_DAY)
    }
}
