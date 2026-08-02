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
        microWeak = false,
        reason = "тестовые независимые признаки"
    )

    @Test fun `experiment protects a strong profit peak when live flow confirms pullback`() {
        val state = GeminiExitExperimentState(portfolio = bought().copy(positionPeakPrice = 1.10))
        val evidence = evidence(
            score = 6,
            groups = 3,
            spotWeak = true,
            futuresWeak = false,
            cvdWeak = false,
            priceWeak = true,
            currentReturn = 9.0
        ).copy(pullbackPercent = 0.9, microWeak = true)

        val result = GeminiExitExperimentEngine.evaluate(state, evidence, 1.09, 20L, 1_000_000L)

        assertEquals("SELL", result.executedTrade?.action)
        assertTrue(result.state.lastReason.contains("ЗАЩИТА ВЗЯТОГО ВЕРХА"))
    }

    private fun entryEvidence(
        active: Boolean = true,
        eligible: Boolean = true,
        score: Int = 7,
        groups: Int = 4,
        blocked: String = ""
    ) = GeminiEntryEvidence(
        signalActive = active,
        eligible = eligible,
        score = score,
        groups = groups,
        signalSource = "APP 99/100",
        reason = "APP 99/100: покупатели и CVD подтверждают вход",
        blockedReason = blocked,
        anchorId = 12L
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

    @Test fun `experiment can enter earlier on a confirmed independent signal`() {
        val initial = GeminiExitExperimentState(initializedAt = 1L)
        val result = GeminiExitExperimentEngine.considerEntry(
            initial,
            entryEvidence(),
            price = 1.0,
            decisionId = 12L,
            now = 120_000L
        )

        assertTrue(result.state.portfolio.inPosition)
        assertEquals("BUY", result.executedTrade?.action)
        assertEquals("BUY", result.state.lastSignal)
        assertTrue(result.executedTrade?.reason?.contains("РАННИЙ ВХОД") == true)
    }

    @Test fun `preparatory signal without market confirmation stays visible but does not buy`() {
        val result = GeminiExitExperimentEngine.considerEntry(
            GeminiExitExperimentState(initializedAt = 1L),
            entryEvidence(
                eligible = false,
                score = 3,
                groups = 2,
                blocked = "нет согласованного подтверждения покупателей или CVD"
            ),
            price = 1.0,
            decisionId = 12L,
            now = 120_000L
        )

        assertFalse(result.state.portfolio.inPosition)
        assertEquals(null, result.executedTrade)
        assertEquals("ENTRY_BLOCKED", result.state.lastSignal)
        assertTrue(result.state.lastReason.contains("НЕ ВЫПОЛНЕН"))
    }

    @Test fun `late entry veto overrides otherwise strong evidence`() {
        val result = GeminiExitExperimentEngine.considerEntry(
            GeminiExitExperimentState(initializedAt = 1L),
            entryEvidence(eligible = false, blocked = "цена уже высоко — поздний вход запрещён"),
            price = 1.0,
            decisionId = 12L,
            now = 120_000L
        )

        assertFalse(result.state.portfolio.inPosition)
        assertTrue(result.state.lastReason.contains("поздний вход запрещён"))
    }

    @Test fun `same signal cannot reopen experiment after an early exit`() {
        val alreadyUsed = GeminiExitExperimentState(
            initializedAt = 1L,
            lastEntryAnchorId = 12L
        )
        val result = GeminiExitExperimentEngine.considerEntry(
            alreadyUsed,
            entryEvidence(),
            price = 1.0,
            decisionId = 12L,
            now = 240_000L
        )

        assertFalse(result.state.portfolio.inPosition)
        assertEquals(null, result.executedTrade)
        assertTrue(result.state.lastReason.contains("сигнал уже использовался"))
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
