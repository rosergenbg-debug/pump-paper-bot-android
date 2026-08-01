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

    @Test fun `primary DeepSeek waits five minutes between paid calls`() {
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

    @Test fun `material signal change starts primary DeepSeek before interval`() {
        val state = DeepSeekPrimaryState(lastAttempt = 1_000L)
        assertTrue(DeepSeekPrimaryPolicy.shouldRun(
            state, hasMarketData = true, force = false, now = 2_000L, materialChange = true
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

    @Test fun `Gemini keeps half of daily capacity for an open position`() {
        assertTrue(GeminiRequestBudget.activeLimit(false) == GeminiRequestBudget.NORMAL_REQUESTS_PER_DAY)
        assertTrue(GeminiRequestBudget.activeLimit(true) == GeminiRequestBudget.MAX_REQUESTS_PER_DAY)
        assertTrue(GeminiRequestBudget.NORMAL_REQUESTS_PER_DAY == 12)
        assertTrue(GeminiRequestBudget.MAX_REQUESTS_PER_DAY - GeminiRequestBudget.NORMAL_REQUESTS_PER_DAY == 13)
    }
}
