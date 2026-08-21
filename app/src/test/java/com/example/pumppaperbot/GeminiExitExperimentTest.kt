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
        currentReturn: Double = 1.0,
        pullback: Double = if (priceWeak) 1.0 else 0.1,
        adaptivePullback: Double = 0.7,
        microWeak: Boolean = false,
        breathing5m: Int? = -20,
        breathing15m: Int? = -25,
        breathing20m: Int? = -18,
        breathing30m: Int? = -20,
        breathing60m: Int? = -15
    ) = GeminiExitEvidence(
        score = score,
        groups = groups,
        pullbackPercent = pullback,
        adaptivePullbackPercent = adaptivePullback,
        currentReturnPercent = currentReturn,
        spotFlowWeak = spotWeak,
        futuresFlowWeak = futuresWeak,
        cvdWeak = cvdWeak,
        marketWeak = groups >= 4,
        derivativesWeak = false,
        bookWeak = false,
        directionWeak = score >= 7,
        priceWeak = priceWeak,
        microWeak = microWeak,
        reason = "тестовые независимые признаки",
        appExitSignal = false,
        breathing5m = breathing5m,
        breathing15m = breathing15m,
        breathing20m = breathing20m,
        breathing30m = breathing30m,
        breathing60m = breathing60m
    )

    @Test fun `large profit alone is never an automatic experiment exit`() {
        val state = GeminiExitExperimentState(portfolio = bought().copy(positionPeakPrice = 1.10))
        val evidence = evidence(
            score = 6,
            groups = 3,
            spotWeak = true,
            futuresWeak = false,
            cvdWeak = false,
            priceWeak = true,
            currentReturn = 9.0,
            pullback = 0.9,
            microWeak = false,
            breathing5m = 15,
            breathing15m = 12,
            breathing20m = 10,
            breathing30m = 5,
            breathing60m = 3
        )

        val result = GeminiExitExperimentEngine.evaluate(state, evidence, 1.09, 20L, 1_000_000L)

        assertEquals(null, result.executedTrade)
        assertTrue(result.state.portfolio.inPosition)
        assertEquals(0, result.state.dangerStreak)
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

    @Test fun `moderate 5 15 20 reversal exits after two separate checks`() {
        val state = GeminiExitExperimentState(initializedAt = 1L, portfolio = bought())
        val first = GeminiExitExperimentEngine.evaluate(
            state,
            evidence(score = 5, groups = 3),
            price = 1.01,
            decisionId = 11L,
            now = 1_000_000L
        )
        assertTrue(first.state.portfolio.inPosition)
        assertEquals(1, first.state.dangerStreak)
        assertEquals(null, first.executedTrade)

        val second = GeminiExitExperimentEngine.evaluate(
            first.state,
            evidence(score = 5, groups = 3),
            price = 1.005,
            decisionId = 11L,
            now = 1_120_000L
        )
        assertFalse(second.state.portfolio.inPosition)
        assertEquals("SELL", second.executedTrade?.action)
        assertTrue(second.state.lastReason.contains("ДВУМЯ ПРОВЕРКАМИ"))
    }

    @Test fun `strong 5 15 20 reversal can exit on first check after ten minute hold`() {
        val result = GeminiExitExperimentEngine.evaluate(
            GeminiExitExperimentState(initializedAt = 1L, portfolio = bought()),
            evidence(
                score = 6,
                groups = 4,
                priceWeak = true,
                currentReturn = 0.2,
                breathing5m = -24,
                breathing15m = -20,
                breathing20m = -16,
                breathing30m = 8,
                breathing60m = 10
            ),
            price = 0.995,
            decisionId = 11L,
            now = 700_000L
        )

        assertFalse(result.state.portfolio.inPosition)
        assertEquals("SELL", result.executedTrade?.action)
        assertTrue(result.state.lastReason.contains("СИЛЬНЫЙ РАЗВОРОТ 5/15/20"))
    }

    @Test fun `positive 30 and 60 do not block confirmed 5 15 20 exit`() {
        val result = GeminiExitExperimentEngine.evaluate(
            GeminiExitExperimentState(initializedAt = 1L, portfolio = bought()),
            evidence(
                score = 7,
                groups = 4,
                priceWeak = true,
                breathing5m = -22,
                breathing15m = -18,
                breathing20m = -14,
                breathing30m = 25,
                breathing60m = 30
            ),
            price = 0.996,
            decisionId = 11L,
            now = 700_000L
        )

        assertFalse(result.state.portfolio.inPosition)
        assertTrue(result.state.lastReason.contains("30/60 пока только фон"))
    }

    @Test fun `profit protection exits after roughly one percent pullback when 5 15 20 deteriorate`() {
        val result = GeminiExitExperimentEngine.evaluate(
            GeminiExitExperimentState(
                initializedAt = 1L,
                portfolio = bought().copy(positionPeakPrice = 1.04)
            ),
            evidence(
                score = 4,
                groups = 3,
                spotWeak = true,
                futuresWeak = true,
                cvdWeak = false,
                currentReturn = 2.9,
                pullback = 1.05,
                microWeak = false,
                breathing5m = -14,
                breathing15m = -12,
                breathing20m = -10,
                breathing30m = 5,
                breathing60m = 8
            ),
            price = 1.029,
            decisionId = 11L,
            now = 700_000L
        )

        assertFalse(result.state.portfolio.inPosition)
        assertEquals("SELL", result.executedTrade?.action)
        assertTrue(result.state.lastReason.contains("ЗАЩИТА ПРИБЫЛИ"))
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
                priceWeak = true,
                breathing5m = 5,
                breathing15m = 4,
                breathing20m = 3
            ),
            price = 1.01,
            decisionId = 11L,
            now = 700_000L
        )
        assertTrue(result.state.portfolio.inPosition)
        assertEquals(null, result.executedTrade)
    }

    @Test fun `adaptive two point five to three percent loss is emergency backstop`() {
        val result = GeminiExitExperimentEngine.evaluate(
            GeminiExitExperimentState(initializedAt = 1L, portfolio = bought()),
            evidence(
                score = 0,
                groups = 0,
                spotWeak = false,
                futuresWeak = false,
                cvdWeak = false,
                currentReturn = -2.6,
                adaptivePullback = 0.7,
                breathing5m = 0,
                breathing15m = 0,
                breathing20m = 0
            ),
            price = 0.974,
            decisionId = 11L,
            now = 120_000L
        )
        assertFalse(result.state.portfolio.inPosition)
        assertTrue(result.state.lastReason.contains("АВАРИЙНАЯ СТРАХОВКА"))
    }
}
