package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiExitExperimentTest {
    private fun recommendation() = GeminiHourlyRecommendation(
        action = "BUY",
        directionScore = 70,
        confidence = 80,
        horizonHours = 1,
        reason = "Контрольный вход",
        risks = emptyList(),
        model = "test"
    )

    private fun bought(): GeminiPaperPortfolio = GeminiPaperTrader.applyDecision(
        GeminiPaperPortfolio(),
        price = 1.0,
        decisionId = 10L,
        candleTime = 100L,
        recommendation = recommendation(),
        now = 100L
    )

    private fun evidence(
        score: Int,
        groups: Int,
        spotWeak: Boolean = true,
        futuresWeak: Boolean = true,
        cvdWeak: Boolean = true,
        priceWeak: Boolean = false,
        currentReturn: Double = 1.0
    ) = GeminiExitEvidence(
        score = score,
        groups = groups,
        pullbackPercent = if (priceWeak) 1.0 else 0.1,
        adaptivePullbackPercent = 0.7,
        currentReturnPercent = currentReturn,
        spotFlowWeak = spotWeak,
        futuresFlowWeak = futuresWeak,
        cvdWeak = cvdWeak,
        marketWeak = groups >= 4,
        derivativesWeak = false,
        bookWeak = false,
        directionWeak = score >= 7,
        priceWeak = priceWeak,
        reason = "тестовые независимые признаки"
    )

    @Test fun `experiment starts from an exact control portfolio checkpoint`() {
        val control = bought()
        val state = GeminiExitExperimentEngine.bootstrap(null, control, 500L)

        assertEquals(control, state.portfolio)
        assertEquals(10L, state.lastControlDecisionId)
        assertEquals(500L, state.initializedAt)
    }

    @Test fun `experiment mirrors a later Gemini buy but not its sell`() {
        val initial = GeminiExitExperimentState(initializedAt = 1L, lastControlDecisionId = 1L)
        val buy = GeminiPaperTrade(
            time = 200L,
            decisionId = 2L,
            action = "BUY",
            price = 1.0,
            amount = 100.0,
            fee = 1.0,
            score = 70,
            confidence = 80,
            reason = "Gemini вошёл"
        )
        val mirrored = GeminiExitExperimentEngine.mirrorControlTrade(initial, buy)
        assertTrue(mirrored.state.portfolio.inPosition)
        assertEquals("BUY", mirrored.executedTrade?.action)

        val controlSell = buy.copy(time = 300L, decisionId = 3L, action = "SELL")
        val ignoredSell = GeminiExitExperimentEngine.mirrorControlTrade(mirrored.state, controlSell)
        assertTrue(ignoredSell.state.portfolio.inPosition)
        assertEquals(null, ignoredSell.executedTrade)
        assertEquals(3L, ignoredSell.state.lastControlDecisionId)
    }

    @Test fun `moderate reversal must survive two separate checks`() {
        val state = GeminiExitExperimentState(initializedAt = 1L, portfolio = bought())
        val first = GeminiExitExperimentEngine.evaluate(
            state,
            evidence(score = 5, groups = 3),
            price = 1.01,
            decisionId = 11L,
            now = 120_000L
        )
        assertTrue(first.state.portfolio.inPosition)
        assertEquals(1, first.state.dangerStreak)
        assertEquals(null, first.executedTrade)

        val second = GeminiExitExperimentEngine.evaluate(
            first.state,
            evidence(score = 5, groups = 3),
            price = 1.005,
            decisionId = 11L,
            now = 240_000L
        )
        assertFalse(second.state.portfolio.inPosition)
        assertEquals("SELL", second.executedTrade?.action)
    }

    @Test fun `one isolated indicator cannot close the experiment`() {
        val result = GeminiExitExperimentEngine.evaluate(
            GeminiExitExperimentState(initializedAt = 1L, portfolio = bought()),
            evidence(
                score = 2,
                groups = 1,
                spotWeak = false,
                futuresWeak = false,
                cvdWeak = false,
                priceWeak = true
            ),
            price = 1.01,
            decisionId = 11L,
            now = 120_000L
        )
        assertTrue(result.state.portfolio.inPosition)
        assertEquals(null, result.executedTrade)
    }

    @Test fun `five percent loss remains only an emergency backstop`() {
        val result = GeminiExitExperimentEngine.evaluate(
            GeminiExitExperimentState(initializedAt = 1L, portfolio = bought()),
            evidence(
                score = 0,
                groups = 0,
                spotWeak = false,
                futuresWeak = false,
                cvdWeak = false,
                currentReturn = -5.1
            ),
            price = 0.949,
            decisionId = 11L,
            now = 120_000L
        )
        assertFalse(result.state.portfolio.inPosition)
        assertTrue(result.state.lastReason.contains("АВАРИЙНАЯ СТРАХОВКА"))
    }
}
