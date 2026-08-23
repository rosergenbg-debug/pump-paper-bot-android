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
 * V5.26 PM-only execution engine.
 *
 * Design goals:
 *  - enter while buyer breath is IGNITION/EXPANSION, not after every 15/30m bar turns green;
 *  - reject mature/exhausted/absorbed pumps instead of chasing taker-buy at the top;
 *  - use the existing 15-second causal observer, never an LLM response, in the execution path;
 *  - measure all exits in true simulated NET PnL after buy fee, sell fee and executable bid/ask;
 *  - prevent dead positions with breakeven, early adverse-flow exits and bounded hold time.
 *
 * This is an experiment intended to improve expectancy. It is not a profit guarantee.
 */
object PumpProfitEngineV526 {
    private data class Config(
        val name: String,
        val takeProfitNet: Double,
        val hardStopNet: Double,
        val breakevenTriggerNet: Double,
        val breakevenLockNet: Double,
        val earlyAdverseNet: Double,
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
        val confirmationMillis: Long
    )

    private val PM2 = Config(
        name = "PM2",
        takeProfitNet = 2.00,
        hardStopNet = -1.10,
        breakevenTriggerNet = 0.85,
        breakevenLockNet = 0.10,
        earlyAdverseNet = -0.45,
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
        confirmationMillis = 12_000L
    )

    private val PM3 = Config(
        name = "PM3",
        takeProfitNet = 3.00,
        hardStopNet = -1.30,
        breakevenTriggerNet = 1.25,
        breakevenLockNet = 0.15,
        earlyAdverseNet = -0.55,
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
        maxEarlyMovePercent = 1.60,
        confirmationMillis = 15_000L
    )

    private val PM_RETEST = PM2.copy(name = "PM RETEST")

    private val PM_SAFE = Config(
        name = "PM SAFE",
        takeProfitNet = 1.15,
        hardStopNet = -0.75,
        breakevenTriggerNet = 0.55,
        breakevenLockNet = 0.05,
        earlyAdverseNet = -0.30,
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
        confirmationMillis = 30_000L
    )

    private fun cfg(mode: PumpProfitModeV526): Config = when (mode) {
        PumpProfitModeV526.PUMP_2 -> PM2
        PumpProfitModeV526.PUMP_3 -> PM3
        PumpProfitModeV526.PUMP_RETEST -> PM_RETEST
        PumpProfitModeV526.PUMP_SAFE -> PM_SAFE
    }

    // V5.28: PM2 and PM3 use the same strict market-quality gate, but each store owns its
    // confirmation, cooldown and execution time. Either flat account may therefore re-enter
    // independently while the other account is still managing an earlier position.
    private fun entryCfg(mode: PumpProfitModeV526): Config = when (mode) {
        PumpProfitModeV526.PUMP_SAFE -> PM_SAFE
        else -> PM3
    }

    private fun resetEntry(previous: FusionStabilityState, keepCooldown: Boolean = true) = previous.copy(
        entryStreak = 0,
        entryCandidateAt = 0L,
        exitStreak = 0,
        exitArmedAt = 0L,
        exitArmedBid = 0.0,
        peakBid = 0.0,
        profitDefenseArmed = false,
        cooldownUntil = if (keepCooldown) previous.cooldownUntil else 0L
    )

    fun isFastCandidate(mode: PumpProfitModeV526, observation: SharedFusionEntryObservation): Boolean {
        if (observation.shockReady) return shockPermitted(observation)
        return entryGate(mode, observation).first
    }

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

        val (candidate, reason) = entryGate(mode, observation)
        if (!candidate) {
            return SharedFusionEntryDecision(null, resetEntry(previous, keepCooldown = false), reason)
        }

        val candidateAt = if (previous.entryStreak > 0 && previous.entryCandidateAt > 0L) {
            previous.entryCandidateAt
        } else now
        val streak = (previous.entryStreak + 1).coerceAtMost(2)
        val next = previous.copy(
            entryStreak = streak,
            entryCandidateAt = candidateAt,
            exitStreak = 0,
            exitArmedAt = 0L,
            exitArmedBid = 0.0,
            peakBid = 0.0,
            profitDefenseArmed = false,
            cooldownUntil = 0L
        )
        val elapsed = (now - candidateAt).coerceAtLeast(0L)
        return if (streak >= 2 && elapsed >= c.confirmationMillis) {
            SharedFusionEntryDecision(
                "BUY",
                next,
                "V526_${c.name}_EARLY_ENTRY: ранний импульс подтверждён двумя 15с наблюдениями; $reason"
            )
        } else {
            val left = ((c.confirmationMillis - elapsed).coerceAtLeast(0L) + 999L) / 1000L
            SharedFusionEntryDecision(
                null,
                next,
                "V526_${c.name}_ARMED ${streak}/2: ранний импульс есть; защита от одиночного тика ещё ${left}с; $reason"
            )
        }
    }

    private fun entryGate(
        mode: PumpProfitModeV526,
        observation: SharedFusionEntryObservation
    ): Pair<Boolean, String> {
        val c = entryCfg(mode)
        val breathing = observation.breathing
            ?: return false to "V526_${c.name}_WAIT: нет live breathing snapshot"
        val frame = observation.frame
            ?: return false to "V526_${c.name}_WAIT: быстрый flow frame ещё не готов"
        if (!breathing.fresh) return false to "V526_${c.name}_WAIT: live flow устарел"

        val breath = breathing.buyerBreath
        val phase = breath.phase
        if (phase != BuyerBreathPhase.IGNITION && phase != BuyerBreathPhase.EXPANSION) {
            return false to "V526_${c.name}_NO_FOMO: фаза $phase, вход разрешён только IGNITION/EXPANSION"
        }

        val buyer5 = breath.buyerPercent5m
            ?: breathing.horizons.firstOrNull { it.minutes == 5 }?.buyerPercent
            ?: 50.0
        val activity = breath.activityRatio
        val efficiency = breath.efficiencyScore ?: 0
        val move = max(
            0.0,
            max(
                breath.moveSincePhaseStartPercent ?: 0.0,
                breath.priceChange5mPercent ?: 0.0
            )
        )

        if (move > c.maxEarlyMovePercent) {
            return false to "V526_${c.name}_NO_FOMO: движение уже ${fmt(move)}% > ${fmt(c.maxEarlyMovePercent)}%"
        }
        if (breath.absorptionRisk > c.maxAbsorptionRisk) {
            return false to "V526_${c.name}_ABSORPTION: риск ${breath.absorptionRisk}/100"
        }
        if (efficiency < c.minEfficiency) {
            return false to "V526_${c.name}_ABSORPTION: эффективность цены $efficiency слишком слабая"
        }
        if (buyer5 < c.minBuyer5m) {
            return false to "V526_${c.name}_WAIT: buyer5=${fmt(buyer5)}% < ${fmt(c.minBuyer5m)}%"
        }
        if (activity == null) {
            return false to "V526_${c.name}_CAPITAL_WAIT: ещё нет надёжной базы активности; в пустой рынок не входим"
        }
        if (activity < c.minActivityRatio) {
            return false to "V526_${c.name}_WAIT: активность ${fmt(activity)}x < ${fmt(c.minActivityRatio)}x"
        }
        val capital = capitalGate(observation.micro, c)
        if (!capital.first) return false to "V526_${c.name}_CAPITAL_WAIT: ${capital.second}"
        if (frame.instant < c.minInstant || frame.score5m < c.min5m) {
            return false to "V526_${c.name}_WAIT: instant/5m ${frame.instant}/${frame.score5m} ещё недостаточны"
        }
        if (frame.score15m < c.min15m || frame.score30m < c.min30m) {
            return false to "V526_${c.name}_WAIT: старший поток ещё падает слишком быстро (${frame.score15m}/${frame.score30m})"
        }

        return true to "phase=$phase instant/5/15/30=${frame.instant}/${frame.score5m}/${frame.score15m}/${frame.score30m}, buyer5=${fmt(buyer5)}%, activity=${fmt(activity)}x, ${capital.second}, move=${fmt(move)}%, absorption=${breath.absorptionRisk}"
    }

    private fun capitalGate(micro: MicroImpulseSnapshot?, c: Config): Pair<Boolean, String> {
        if (micro == null || micro.flowHistorySeconds < 15L * 60L) {
            return false to "ждём свежую 15-минутную ленту реальных сделок"
        }
        val buy5 = micro.buyNotional5m.coerceAtLeast(0.0)
        val sell5 = micro.sellNotional5m.coerceAtLeast(0.0)
        val total5 = buy5 + sell5
        val total15 = (micro.buyNotional15m + micro.sellNotional15m).coerceAtLeast(total5)
        val priorTenPerFive = ((total15 - total5).coerceAtLeast(0.0) / 2.0)
        if (total5 < c.minFiveMinuteNotionalUsdt) {
            return false to "за 5м прошло лишь ${money(total5)} < ${money(c.minFiveMinuteNotionalUsdt)}"
        }
        if (priorTenPerFive <= 0.0) return false to "нет базы предыдущих 10 минут для проверки притока"
        val acceleration = total5 / priorTenPerFive
        if (acceleration < c.minCapitalActivityRatio) {
            return false to "масса 5м не ускорилась: ${fmt(acceleration)}x < ${fmt(c.minCapitalActivityRatio)}x"
        }
        val sideRatio = buy5 / sell5.coerceAtLeast(1.0)
        if (sideRatio < c.minBuySellNotionalRatio) {
            return false to "деньги не на стороне покупателей: BUY/SELL ${fmt(sideRatio)}x"
        }
        val large = micro.largeFlow
        if (large.mode == LargeFlowMode.SELL_SERIES || large.mode == LargeFlowMode.BUY_ABSORBED) {
            return false to "крупный поток против входа: ${large.mode}"
        }
        val dynamicFloor = max(c.minFiveMinuteNotionalUsdt, large.thresholdUsdt * 8.0)
        val broadCapital = total5 >= dynamicFloor && micro.trades60s >= 40
        val largeBuySeries = large.mode == LargeFlowMode.BUY_SERIES && large.confidence >= 50 &&
            large.largeBuyUsdt > large.largeSellUsdt * 1.20
        if (!broadCapital && !largeBuySeries) {
            return false to "нет подтверждения широкой активностью или серией крупных BUY"
        }
        return true to "капитал 5м ${money(total5)}, ускорение ${fmt(acceleration)}x, BUY/SELL ${fmt(sideRatio)}x"
    }

    private fun shockPermitted(observation: SharedFusionEntryObservation): Boolean {
        val frame = observation.frame
        val breath = observation.breathing?.buyerBreath ?: return false
        if (frame != null && frame.instant < 4) return false
        if (!breath.fresh) return false
        if (breath.phase == BuyerBreathPhase.SELLER_TAKEOVER || breath.phase == BuyerBreathPhase.EXHAUSTION) return false
        if (breath.absorptionRisk >= 72) return false
        if (!capitalGate(observation.micro, PM3).first) return false
        return true
    }

    private fun money(value: Double): String = when {
        value >= 1_000_000.0 -> String.format(java.util.Locale.US, "$%.2fm", value / 1_000_000.0)
        value >= 1_000.0 -> String.format(java.util.Locale.US, "$%.0fk", value / 1_000.0)
        else -> String.format(java.util.Locale.US, "$%.0f", value)
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
        val peakBid = max(max(previous.peakBid, bid), portfolio.entryPrice)
        val peakNet = tradeNetPercent(portfolio, peakBid, feeRate)
        val armed = previous.profitDefenseArmed || peakNet >= c.breakevenTriggerNet
        val next = previous.copy(
            entryStreak = 0,
            entryCandidateAt = 0L,
            peakBid = peakBid,
            profitDefenseArmed = armed,
            cooldownUntil = 0L
        )

        fun exit(reason: String) = PumpProfitPositionDecisionV526(
            "EXIT", next, reason, tradeNet, peakNet
        )

        if (tradeNet >= c.takeProfitNet) {
            return exit("V526_TAKE_PROFIT_${c.name}: ${fmtSigned(tradeNet)}% NET; цель ${fmt(c.takeProfitNet)}% NET выполнена")
        }
        if (tradeNet <= c.hardStopNet) {
            return exit("V526_HARD_STOP_${c.name}: ${fmtSigned(tradeNet)}% NET; лимит ${fmtSigned(c.hardStopNet)}%")
        }
        if (armed && tradeNet <= c.breakevenLockNet) {
            return exit("V526_BREAKEVEN_${c.name}: пик ${fmtSigned(peakNet)}% NET; защищаем не менее ${fmtSigned(c.breakevenLockNet)}% NET")
        }

        val frame = observation?.frame
        val breath = observation?.breathing?.buyerBreath
        val deterioration = frame?.deteriorationSignal == true ||
            breath?.phase == BuyerBreathPhase.SELLER_TAKEOVER ||
            ((breath?.absorptionRisk ?: 0) >= 85 && (frame?.instant ?: 0) < 0)
        if (positionAgeMillis >= 90_000L && tradeNet <= c.earlyAdverseNet && deterioration) {
            return exit("V526_EARLY_RISK_EXIT_${c.name}: ${fmtSigned(tradeNet)}% NET и быстрый поток ухудшился; не ждём полного стопа")
        }

        val giveback = peakNet - tradeNet
        if (peakNet >= c.givebackArmNet && giveback >= c.maxGivebackNet && tradeNet > c.breakevenLockNet) {
            return exit("V526_PROFIT_GIVEBACK_${c.name}: пик ${fmtSigned(peakNet)}%, откат ${fmt(giveback)} п.п.; фиксируем ${fmtSigned(tradeNet)}% NET")
        }

        val constructive = frame != null &&
            frame.instant >= 0 && frame.score5m >= 0 && frame.score15m >= -2 &&
            breath?.phase != BuyerBreathPhase.EXHAUSTION &&
            breath?.phase != BuyerBreathPhase.SELLER_TAKEOVER
        if (positionAgeMillis >= c.hardHoldMillis) {
            return exit("V526_HARD_TIMEOUT_${c.name}: позиция живёт ${(positionAgeMillis / 60_000L)} мин; освобождаем капитал")
        }
        if (positionAgeMillis >= c.softHoldMillis && (tradeNet < c.timeoutKeepNet || !constructive)) {
            return exit("V526_TIMEOUT_${c.name}: ${(positionAgeMillis / 60_000L)} мин без достаточного продолжения; NET ${fmtSigned(tradeNet)}%")
        }

        val stateReason = if (armed) {
            "V526_${c.name}_HOLD: BE armed; peak=${fmtSigned(peakNet)}% net, now=${fmtSigned(tradeNet)}% net"
        } else {
            "V526_${c.name}_HOLD: peak=${fmtSigned(peakNet)}% net, now=${fmtSigned(tradeNet)}% net"
        }
        return PumpProfitPositionDecisionV526(null, next, stateReason, tradeNet, peakNet)
    }

    private fun tradeNetPercent(portfolio: FusionSimPortfolio, bid: Double, feeRate: Double): Double {
        if (!portfolio.inPosition || portfolio.entryCostEur <= 0.0 || bid <= 0.0) return 0.0
        val fee = feeRate.coerceIn(0.0, 0.02)
        val netExit = portfolio.pumpAmount * bid * (1.0 - fee)
        return (netExit / portfolio.entryCostEur - 1.0) * 100.0
    }

    private fun fmt(value: Double): String = String.format(java.util.Locale.GERMANY, "%.2f", value)
    private fun fmtSigned(value: Double): String = String.format(java.util.Locale.GERMANY, "%+.2f", value)
}
