package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekPaperPolicyTest {
    @Test fun `DeepSeek owns buy and exit actions`() {
        assertEquals("BUY", DeepSeekPaperPolicy.recommendation(
            DeepSeekPrimaryState(action = "BUY", direction = 75, confidence = 80)
        ).action)
        assertEquals("SELL", DeepSeekPaperPolicy.recommendation(
            DeepSeekPrimaryState(action = "EXIT", direction = -70, confidence = 85)
        ).action)
    }

    @Test fun `watch and hold never execute a trade`() {
        assertEquals("HOLD", DeepSeekPaperPolicy.recommendation(
            DeepSeekPrimaryState(action = "WATCH")
        ).action)
        assertEquals("HOLD", DeepSeekPaperPolicy.recommendation(
            DeepSeekPrimaryState(action = "HOLD")
        ).action)
    }

    @Test fun `accepted DeepSeek response is applied exactly once`() {
        val state = DeepSeekPrimaryState(lastSuccess = 2_000L, action = "BUY")
        assertTrue(DeepSeekPaperPolicy.isNewDecision(state, GeminiPaperPortfolio(lastDecisionId = 1_000L)))
        assertFalse(DeepSeekPaperPolicy.isNewDecision(state, GeminiPaperPortfolio(lastDecisionId = 2_000L)))
    }
}
