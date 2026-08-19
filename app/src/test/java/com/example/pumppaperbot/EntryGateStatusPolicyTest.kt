package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryGateStatusPolicyTest {
    @Test fun `deepsig candidate exposes two step confirmation`() {
        val result = EntryGateStatusPolicy.deepSig(
            DeepSeekPrimaryState(
                proposedAction = "BUY",
                executionStatus = "кандидат входа",
                verificationSummary = "ждём вторую независимую оценку",
                independentEntryConfirmStreak = 1
            ),
            GeminiPaperPortfolio()
        )

        assertEquals("ПОДТВЕРЖДАЕТ ВХОД", result.state)
        assertEquals(1, result.confirmations)
        assertEquals(2, result.confirmationsRequired)
    }

    @Test fun `deepsigx shows exact blocker and three step gate`() {
        val result = EntryGateStatusPolicy.deepSigX(GeminiExitExperimentState(
            lastSignal = "ENTRY_BLOCKED",
            lastReason = "не хватило spot и futures подтверждения",
            entryConfirmStreak = 1
        ))

        assertEquals("ВХОД ЗАБЛОКИРОВАН", result.state)
        assertEquals(3, result.confirmationsRequired)
        assertTrue(result.reason.contains("не хватило"))
    }

    @Test fun `fusion reports missing read only venue instead of vague wait`() {
        val result = EntryGateStatusPolicy.fusion(
            FusionSimPortfolio(),
            FusionMarketSnapshot(configured = false),
            DeepSeekPrimaryState(action = "BUY"),
            100L
        )

        assertTrue(result.reason.contains("ключ Bitpanda"))
    }
}
