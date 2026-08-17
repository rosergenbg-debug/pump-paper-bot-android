package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiExitExperimentTest {
    @Test fun `autonomous DeepSigX ignores APP and DeepSig as entry sources`() {
        val onlyForeign = GeminiEntryEvidence.effectiveSignalSources(
            observedAppReady = true,
            observedDeepSeekPositive = true,
            breathingPositive = false
        )
        val ownSignal = GeminiEntryEvidence.effectiveSignalSources(
            observedAppReady = true,
            observedDeepSeekPositive = true,
            breathingPositive = true
        )

        assertEquals(Triple(false, false, false), onlyForeign)
        assertEquals(Triple(false, false, true), ownSignal)
    }

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
        reason = "тестовые независимые признаки",
        appExitSignal = false,
        breathing5m = -20,
        breathing15m = -25,
        breathing30m = -20,
        breathing60m = -15
    )

    @Test fun `eight percent profit is never an automatic experiment exit`() {
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

        assertEquals(null, result.executedTrade)
        assertTrue(result.state.portfolio.inPosition)
        assertEquals(1, result.state.dangerStreak)
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

    @Test fun `experiment enters only after three confirmed independent cycles`() {
        val initial = GeminiExitExperimentState(initializedAt = 1L)
        val first = GeminiExitExperimentEngine.considerEntry(
            initial,
            entryEvidence(),
            price = 1.0,
            decisionId = 12L,
            now = 120_000L
        )
        assertFalse(first.state.portfolio.inPosition)
        assertEquals("ENTRY_ARMED", first.state.lastSignal)

        val second = GeminiExitExperimentEngine.considerEntry(
            first.state,
            entryEvidence(),
            price = 1.0,
            decisionId = 12L,
            now = 240_000L
        )
        assertFalse(second.state.portfolio.inPosition)
        assertTrue(second.state.lastReason.contains("2/3"))

        val result = GeminiExitExperimentEngine.considerEntry(
            second.state,
            entryEvidence(),
            price = 1.0,
            decisionId = 12L,
            now = 360_000L
        )

        assertTrue(result.state.portfolio.inPosition)
        assertEquals("BUY", result.executedTrade?.action)
        assertEquals("BUY", result.state.lastSignal)
        assertTrue(result.executedTrade?.reason?.contains("РАННИЙ ВХОД") == true)
    }

    @Test fun `fresh DeepSeek timestamps share one stable confirmation window`() {
        val first = GeminiEntryEvidence.stableConfirmationAnchor(
            observedAt = 10L * 60L * 1000L,
            appReady = false,
            appCandleTime = 0L,
            deepSeekPositive = true,
            breathingPositive = true
        )
        val second = GeminiEntryEvidence.stableConfirmationAnchor(
            observedAt = 12L * 60L * 1000L,
            appReady = false,
            appCandleTime = 0L,
            deepSeekPositive = true,
            breathingPositive = true
        )

        assertEquals(first, second)
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
        assertTrue(result.state.lastReason.contains("кадр уже использовался"))
    }

    @Test fun `experiment waits thirty minutes after an exit before reentry`() {
        val control = bought()
        val sold = control.copy(
            pumpAmount = 0.0,
            entryPrice = 0.0,
            trades = control.trades + GeminiPaperTrade(
                time = 1_000_000L,
                decisionId = 11L,
                action = "SELL",
                price = 1.0,
                amount = 100.0,
                fee = 0.15,
                score = 5,
                confidence = 60,
                reason = "контрольный выход"
            )
        )
        val result = GeminiExitExperimentEngine.considerEntry(
            GeminiExitExperimentState(initializedAt = 1L, portfolio = sold),
            entryEvidence().copy(anchorId = 13L),
            price = 1.0,
            decisionId = 13L,
            now = 2_200_000L
        )

        assertFalse(result.state.portfolio.inPosition)
        assertTrue(result.state.lastReason.contains("30-минутная защита"))
    }

    @Test fun `moderate reversal must survive three separate checks after minimum hold`() {
        val state = GeminiExitExperimentState(initializedAt = 1L, portfolio = bought())
        val first = GeminiExitExperimentEngine.evaluate(
            state,
            evidence(score = 5, groups = 3),
            price = 1.01,
            decisionId = 11L,
            now = 2_000_000L
        )
        assertTrue(first.state.portfolio.inPosition)
        assertEquals(1, first.state.dangerStreak)
        assertEquals(null, first.executedTrade)

        val second = GeminiExitExperimentEngine.evaluate(
            first.state,
            evidence(score = 5, groups = 3),
            price = 1.005,
            decisionId = 11L,
            now = 2_120_000L
        )
        assertTrue(second.state.portfolio.inPosition)
        assertEquals(2, second.state.dangerStreak)

        val third = GeminiExitExperimentEngine.evaluate(
            second.state,
            evidence(score = 5, groups = 3),
            price = 1.002,
            decisionId = 11L,
            now = 2_240_000L
        )
        assertFalse(third.state.portfolio.inPosition)
        assertEquals("SELL", third.executedTrade?.action)
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
