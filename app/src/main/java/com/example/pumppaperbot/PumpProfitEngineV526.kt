package com.example.pumppaperbot

import kotlin.math.max

enum class PumpProfitModeV526 { PUMP_2, PUMP_3, PUMP_RETEST, PUMP_SAFE }

data class PumpProfitPositionDecisionV526(
    val action: String?,
    val nextState: FusionStabilityState,
    val reason: String?,
    val tradeNetPercent: Double,
    val peakNetPercent: Double
)

/**
 * PM paper execution/risk engine.
 *
 * V6.2 corrects two different units that had been mixed in the old early-risk path:
 * TP/hard-stop/breakeven remain NET-after-fees economics, while an early adverse PRICE move is
 * measured from executable entry ask to current bid before fees. This prevents the normal ~0.5%
 * round-trip fee floor from masquerading as a market drawdown. Structural seller failure remains
 * able to exit earlier, but a single transient seller phase is not enough on its own.
 */
object PumpProfitEngineV526 {
    private data class Config(
        val name: String,
        val takeProfitNet: Double,
        val hardStopNet: Double,
        val breakevenTriggerNet: Double,
        val breakevenLockNet: Double,
        val earlyAdversePricePercent: Double,
        val givebackArmNet: Double,
        val maxGivebackNet: Double,
        val softHoldMillis: Long,
        val hardHoldMillis: Long,
        val timeoutKeepNet: Double,
        val minInstant: Int,
        val min5m: Int,
        val min15m: Int,
        val min30m: Int,
        val minBuyer5m: Double,
        val minActivityRatio: Double,
        val maxAbsorptionRisk: Int,
        val minEfficiency: Int,
        val minCapitalActivityRatio: Double,
        val minFiveMinuteNotionalUsdt: Double,
        val minBuySellNotionalRatio: Double,
        val maxEarlyMovePercent: Double,
        val confirmationMillis: Long,
        val maxConfirmationRisePercent: Double
    )

    private val PM2 = Config(
        name = "PM2",
        takeProfitNet = 2.00,
        hardStopNet = -1.10,
        breakevenTriggerNet = 0.85,
        breakevenLockNet = 0.10,
        earlyAdversePricePercent = -0.45,
        givebackArmNet = 1.25,
        maxGivebackNet = 0.55,
        softHoldMillis = 20L * 60L * 1000L,
        hardHoldMillis = 30L * 60L * 1000L,
        timeoutKeepNet = 0.30,
        minInstant = 8,
        min5m = 2,
        min15m = -4,
        min30m = -8,
        minBuyer5m = 56.0,
        minActivityRatio = 1.05,
        maxAbsorptionRisk = 62,
        minEfficiency = -25,
        minCapitalActivityRatio = 1.15,
        minFiveMinuteNotionalUsdt = 250_000.0,
        minBuySellNotionalRatio = 1.12,
        maxEarlyMovePercent = 1.45,
        confirmationMillis = 15_000L,
        maxConfirmationRisePercent = 0.40
    )

    private val PM3 = Config(
        name = "PM3",
        takeProfitNet = 3.00,
        hardStopNet = -1.30,
        breakevenTriggerNet = 1.25,
        breakevenLockNet = 0.15,
        earlyAdversePricePercent = -0.55,
        givebackArmNet = 2.00,
        maxGivebackNet = 0.75,
        softHoldMillis = 35L * 60L * 1000L,
        hardHoldMillis = 50L * 60L * 1000L,
        timeoutKeepNet = 0.45,
        minInstant = 10,
        min5m = 4,
        min15m = 4,
        min30m = 2,
        minBuyer5m = 59.0,
        minActivityRatio = 1.10,
        maxAbsorptionRisk = 58,
        minEfficiency = -15,
        minCapitalActivityRatio = 1.15,
        minFiveMinuteNotionalUsdt = 250_000.0,
        minBuySellNotionalRatio = 1.12,
        maxEarlyMovePercent = 0.90,
        confirmationMillis = 30_000L,
        maxConfirmationRisePercent = 0.35
    )

    private val PM_RETEST = PM2.copy(
        name = "PM RETEST",
        confirmationMillis = 15_000L,
        maxConfirmationRisePercent = 0.50
    )

    private val PM_SAFE = Config(
        name = "PM SAFE",
        takeProfitNet = 1.15,
        hardStopNet = -0.75,
        breakevenTriggerNet = 0.55,
        breakevenLockNet = 0.05,
        earlyAdversePricePercent = -0.30,
        givebackArmNet = 0.80,
        maxGivebackNet = 0.32,
        softHoldMillis = 15L * 60L * 1000L,
        hardHoldMillis = 25L * 60L * 1000L,
        timeoutKeepNet = 0.18,
        minInstant = 14,
        min5m = 8,
        min15m = 2,
        min30m = -1,
        minBuyer5m = 61.0,
        minActivityRatio = 1.15,
        maxAbsorptionRisk = 52,
        minEfficiency = -8,
        minCapitalActivityRatio = 1.20,
        minFiveMinuteNotionalUsdt = 350_000.0,
        minBuySellNotionalRatio = 1.18,
        maxEarlyMovePercent = 1.25,
        confirmationMillis = 30_000L,
        maxConfirmationRisePercent = 0.30
    )

    private fun cfg(mode: PumpProfitModeV526): Config = when (mode) {
        PumpProfitModeV526.PUMP_2 -> PM2
        PumpProfitModeV526.PUMP_3 -> PM3
        PumpProfitModeV526.PUMP_RETEST -> PM_RETEST
        PumpProfitModeV526.PUMP_SAFE -> PM_SAFE
    }

    private fun entryCfg(mode: PumpProfitModeV526): Config = cfg(mode)

    private fun resetEntry(previous: FusionStabilityState, keepCooldown: Boolean = true) = previous.copy(
        entryStreak = 0,
        entryCandidateAt = 0L,
        entryAnchorAsk = 0.0,
        exitStreak = 0,
        exitArmedAt = 0L,
        exitArmedBid = 0.0,
        peakBid = 0.0,
        profitDefenseArmed = false,
        cooldownUntil = if (keepCooldown) previous.cooldownUntil else 0L
    )

    fun isFastCandidate(mode: PumpProfitModeV526, observation: SharedFusionEntryObservation): Boolean {
        if (observation.shockReady) return shockPermitted(observation)
        val result = AdaptiveBreathEntryPolicy.evaluate(mode, observation)
        if (result.allowed) return true
        if (mode !in setOf(PumpProfitModeV526.PUMP_2, PumpProfitModeV526.PUMP_3)) return false
        if (result.hardVeto || !result.nearCandidate) return false
        return PrimaryImpulseLifecycleV620.assess(observation).keepFastTracking
    }

    fun entryGateResult(
        mode: PumpProfitModeV526,
        observation: SharedFusionEntryObservation
    ): AdaptiveBreathEntryPolicy.Result = AdaptiveBreathEntryPolicy.evaluate(mode, observation)

    fun evaluateEntry(
        mode: PumpProfitModeV526,
        previous: FusionStabilityState,
        observation: SharedFusionEntryObservation,
        now: Long
    ): SharedFusionEntryDecision {
        val c = entryCfg(mode)
        if (previous.cooldownUntil > now) {
            val left = ((previous.cooldownUntil - now + 999L) / 1000L).coerceAtLeast(1L)
            return SharedFusionEntryDecision(null, resetEntry(previous), "V526 ${c.name} COOLDOWN: ещё ${left}с")
        }

        if (observation.shockReady) {
            return if (shockPermitted(observation)) {
                SharedFusionEntryDecision(
                    "BUY",
                    resetEntry(previous, keepCooldown = false),
                    "V526_${c.name}_SHOCK_ENTRY: быстрый rebound подтверждён, перегрев/поглощение не блокируют вход"
                )
            } else {
                SharedFusionEntryDecision(
                    null,
                    resetEntry(previous, keepCooldown = false),
                    "V526_${c.name}_NO_FOMO: shock rebound есть, но поток уже выглядит поглощённым/поздним"
                )
            }
        }

        val gate = AdaptiveBreathEntryPolicy.evaluate(mode, observation)
        if (!gate.allowed) {
            if (!gate.hardVeto && gate.nearCandidate && previous.entryStreak > 0) {
                return SharedFusionEntryDecision(
                    null,
                    previous.copy(exitStreak = 0, exitArmedAt = 0L, exitArmedBid = 0.0),
                    "V620_${c.name}_BREATH_HOLD: оценка ${gate.score}/${gate.threshold}; мягкий переход не обнуляет кандидат; ${gate.reason}"
                )
            }
            return SharedFusionEntryDecision(
                null,
                resetEntry(previous, keepCooldown = false),
                "V620_${c.name}_${if (gate.hardVeto) "SAFETY_WAIT" else "SOFT_WAIT"}: оценка ${gate.score}/${gate.threshold}; ${gate.reason}"
            )
        }

        val candidateAt = if (previous.entryStreak > 0 && previous.entryCandidateAt > 0L) previous.entryCandidateAt else now
        val anchorAsk = if (previous.entryStreak > 0 && previous.entryAnchorAsk > 0.0) previous.entryAnchorAsk else observation.executionAsk
        val streak = (previous.entryStreak + 1).coerceAtMost(2)
        val next = previous.copy(
            entryStreak = streak,
            entryCandidateAt = candidateAt,
            entryAnchorAsk = anchorAsk,
            exitStreak = 0,
            exitArmedAt = 0L,
            exitArmedBid = 0.0,
            peakBid = 0.0,
            profitDefenseArmed = false,
            cooldownUntil = 0L
        )
        val elapsed = (now - candidateAt).coerceAtLeast(0L)
        val priceMove = if (anchorAsk > 0.0) (observation.executionAsk / anchorAsk - 1.0) * 100.0 else 0.0
        val micro = observation.micro
        val primary = mode in setOf(PumpProfitModeV526.PUMP_2, PumpProfitModeV526.PUMP_3)
        val lifecycle = if (primary) PrimaryImpulseLifecycleV620.assess(observation) else null
        val strongImpulse = primary && lifecycle?.allowStrongShortcut != false &&
            gate.score >= gate.threshold + 9 &&
            (observation.frame?.instant ?: 0) >= (if (mode == PumpProfitModeV526.PUMP_2) 10 else 12) &&
            (micro?.aggressiveBuyPercent15s ?: 0.0) >= 57.0 &&
            (micro?.aggressiveBuyPercent60s ?: 0.0) >= 54.0 &&
            (micro?.priceChange60sPercent ?: 0.0) >= 0.10
        val baseConfirmation = if (strongImpulse && mode == PumpProfitModeV526.PUMP_3) 15_000L else c.confirmationMillis
        val confirmationMillis = baseConfirmation + observation.entryTuning.confirmationExtraSeconds * 1_000L
        val strongExtraRise = if (strongImpulse) 0.25 else 0.0
        val maxRise = (c.maxConfirmationRisePercent + strongExtraRise -
            observation.entryTuning.chaseTighteningBps / 100.0).coerceAtLeast(0.15)
        val priceAccepted = priceMove in -0.20..maxRise
        val timingLabel = when {
            lifecycle?.regime == PrimaryImpulseRegimeV620.BURST_NEEDS_ACCEPTANCE -> "burst: обычное подтверждение"
            lifecycle?.regime == PrimaryImpulseRegimeV620.REPAIRING_REVERSAL -> "ремонт разворота: обычное подтверждение"
            strongImpulse -> "сильный импульс"
            else -> "обычное подтверждение"
        }
        return if (streak >= 2 && elapsed >= confirmationMillis && priceAccepted) {
            SharedFusionEntryDecision(
                "BUY",
                next,
                "V620_${c.name}_ADAPTIVE_ENTRY: $timingLabel; дыхание ${gate.score}/${gate.threshold}; цена от якоря ${fmtSigned(priceMove)}%; ${gate.reason}"
            )
        } else {
            val left = ((confirmationMillis - elapsed).coerceAtLeast(0L) + 999L) / 1000L
            SharedFusionEntryDecision(
                null,
                next,
                "V620_${c.name}_BREATH_ARMED ${streak}/2: $timingLabel; оценка ${gate.score}/${gate.threshold}, ещё ${left}с; цена от якоря ${fmtSigned(priceMove)}% ${if (priceAccepted) "допустима" else "вне диапазона −0,20…+${fmt(maxRise)}%"}; ${gate.reason}"
            )
        }
    }

    private fun entryGate(mode: PumpProfitModeV526, observation: SharedFusionEntryObservation): Pair<Boolean, String> {
        val result = AdaptiveBreathEntryPolicy.evaluate(mode, observation)
        return result.allowed to "V620 score=${result.score}/${result.threshold}; ${result.reason}"
    }

    private fun shockPermitted(observation: SharedFusionEntryObservation): Boolean {
        val frame = observation.frame
        val breath = observation.breathing?.buyerBreath ?: return false
        if (frame != null && frame.instant < 4) return false
        if (!breath.fresh) return false
        if (breath.phase == BuyerBreathPhase.SELLER_TAKEOVER || breath.phase == BuyerBreathPhase.EXHAUSTION) return false
        if (breath.absorptionRisk >= 72) return false
        return AdaptiveBreathEntryPolicy.evaluate(PumpProfitModeV526.PUMP_2, observation).let { it.allowed && !it.hardVeto }
    }

    fun evaluatePosition(
        mode: PumpProfitModeV526,
        portfolio: FusionSimPortfolio,
        previous: FusionStabilityState,
        observation: SharedFusionEntryObservation?,
        bid: Double,
        feeRate: Double,
        positionAgeMillis: Long
    ): PumpProfitPositionDecisionV526 {
        val c = cfg(mode)
        val tradeNet = tradeNetPercent(portfolio, bid, feeRate)
        val grossPriceMove = priceMovePercent(portfolio.entryPrice, bid)
        val peakBid = max(max(previous.peakBid, bid), portfolio.entryPrice)
        val peakNet = tradeNetPercent(portfolio, peakBid, feeRate)
        val armed = previous.profitDefenseArmed || peakNet >= c.breakevenTriggerNet
        val next = previous.copy(
            entryStreak = 0,
            entryCandidateAt = 0L,
            entryAnchorAsk = 0.0,
            peakBid = peakBid,
            profitDefenseArmed = armed,
            cooldownUntil = 0L
        )
        fun exit(reason: String) = PumpProfitPositionDecisionV526("EXIT", next, reason, tradeNet, peakNet)
        if (tradeNet >= c.takeProfitNet) return exit("V610_TAKE_PROFIT_${c.name}: ${fmtSigned(tradeNet)}% NET; цель ${fmt(c.takeProfitNet)}% NET выполнена")
        if (tradeNet <= c.hardStopNet) return exit("V610_HARD_STOP_${c.name}: ${fmtSigned(tradeNet)}% NET; лимит ${fmtSigned(c.hardStopNet)}%")
        if (armed && tradeNet <= c.breakevenLockNet) return exit("V610_BREAKEVEN_${c.name}: пик ${fmtSigned(peakNet)}% NET; защищаем не менее ${fmtSigned(c.breakevenLockNet)}% NET")

        val structuralFailure = PrimaryImpulseLifecycleV620.structuralFailure(observation)
        val deterioration = PrimaryImpulseLifecycleV620.ordinaryDeterioration(observation)
        val structuralPriceLimit = c.earlyAdversePricePercent * 0.60
        if (positionAgeMillis >= 90_000L && structuralFailure && grossPriceMove <= structuralPriceLimit) {
            return exit(
                "V620_STRUCTURAL_RISK_EXIT_${c.name}: цена ${fmtSigned(grossPriceMove)}% от entry без комиссий, подтверждён структурный слом; NET ${fmtSigned(tradeNet)}%"
            )
        }
        if (positionAgeMillis >= 90_000L && grossPriceMove <= c.earlyAdversePricePercent && deterioration) {
            return exit(
                "V620_EARLY_PRICE_EXIT_${c.name}: цена ${fmtSigned(grossPriceMove)}% от entry без комиссий и поток ухудшился; NET ${fmtSigned(tradeNet)}%; комиссии не считаем падением цены"
            )
        }

        val giveback = peakNet - tradeNet
        if (peakNet >= c.givebackArmNet && giveback >= c.maxGivebackNet && tradeNet > c.breakevenLockNet) {
            return exit("V610_PROFIT_GIVEBACK_${c.name}: пик ${fmtSigned(peakNet)}%, откат ${fmt(giveback)} п.п.; фиксируем ${fmtSigned(tradeNet)}% NET")
        }
        val frame = observation?.frame
        val breath = observation?.breathing?.buyerBreath
        val constructive = frame != null && frame.instant >= 0 && frame.score5m >= 0 && frame.score15m >= -2 &&
            breath?.phase != BuyerBreathPhase.EXHAUSTION && breath?.phase != BuyerBreathPhase.SELLER_TAKEOVER
        if (positionAgeMillis >= c.hardHoldMillis) {
            return exit("V610_HARD_TIMEOUT_${c.name}: позиция живёт ${(positionAgeMillis / 60_000L)} мин; освобождаем капитал")
        }
        if (positionAgeMillis >= c.softHoldMillis && (tradeNet < c.timeoutKeepNet || !constructive)) {
            return exit("V610_TIMEOUT_${c.name}: ${(positionAgeMillis / 60_000L)} мин без достаточного продолжения; NET ${fmtSigned(tradeNet)}%")
        }
        val stateReason = if (armed) {
            "V620_${c.name}_HOLD: BE armed; peak=${fmtSigned(peakNet)}% net, now=${fmtSigned(tradeNet)}% net; price=${fmtSigned(grossPriceMove)}%"
        } else {
            "V620_${c.name}_HOLD: peak=${fmtSigned(peakNet)}% net, now=${fmtSigned(tradeNet)}% net; price=${fmtSigned(grossPriceMove)}%"
        }
        return PumpProfitPositionDecisionV526(null, next, stateReason, tradeNet, peakNet)
    }

    private fun tradeNetPercent(portfolio: FusionSimPortfolio, bid: Double, feeRate: Double): Double {
        if (!portfolio.inPosition || portfolio.entryCostEur <= 0.0 || bid <= 0.0) return 0.0
        val fee = feeRate.coerceIn(0.0, 0.02)
        val netExit = portfolio.pumpAmount * bid * (1.0 - fee)
        return (netExit / portfolio.entryCostEur - 1.0) * 100.0
    }

    internal fun priceMovePercent(entryPrice: Double, bid: Double): Double =
        if (entryPrice > 0.0 && bid > 0.0) (bid / entryPrice - 1.0) * 100.0 else 0.0

    private fun fmt(value: Double): String = String.format(java.util.Locale.GERMANY, "%.2f", value)
    private fun fmtSigned(value: Double): String = String.format(java.util.Locale.GERMANY, "%+.2f", value)
}
