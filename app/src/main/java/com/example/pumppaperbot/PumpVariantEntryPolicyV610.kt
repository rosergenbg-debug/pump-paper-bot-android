package com.example.pumppaperbot

import kotlin.math.max

/**
 * V6.1 entry logic for the two variant Pump Machines.
 *
 * These lanes are deliberately NOT aliases of the ordinary impulse gate:
 * - RETEST first arms on an early impulse, then survives the expected pullback and evaluates
 *   the rebound on its own short-horizon evidence.
 * - SAFE looks for a clean, sustained continuation with low execution/absorption risk. APP is
 *   supportive evidence, not a binary lock that can leave the account permanently idle.
 *
 * Hard market/data safety remains local and deterministic. V6 execution intelligence stays
 * shadow-only and is not consulted here.
 */
data class PumpVariantEntryAssessmentV610(
    val score: Int,
    val threshold: Int,
    val allowed: Boolean,
    val nearCandidate: Boolean,
    val hardVeto: Boolean,
    val strongLocal: Boolean,
    val reason: String
)

object PumpVariantEntryPolicyV610 {
    private const val MAX_COMMON_SPREAD = 0.50
    private const val MAX_SAFE_SPREAD = 0.35

    /**
     * RETEST is armed only by a genuine early impulse. Once armed, callers must NOT require this
     * seed gate to remain true during the pullback; doing so was the V5.x self-cancellation bug.
     */
    fun retestSeed(observation: SharedFusionEntryObservation): PumpVariantEntryAssessmentV610 {
        val base = AdaptiveBreathEntryPolicy.evaluate(PumpProfitModeV526.PUMP_RETEST, observation)
        return PumpVariantEntryAssessmentV610(
            score = base.score,
            threshold = base.threshold,
            allowed = base.allowed,
            nearCandidate = base.nearCandidate,
            hardVeto = base.hardVeto,
            strongLocal = base.allowed && base.score >= base.threshold + 8,
            reason = "RETEST seed: ${base.reason}"
        )
    }

    fun retestRebound(
        observation: SharedFusionEntryObservation,
        pullbackPercent: Double,
        reboundPercent: Double
    ): PumpVariantEntryAssessmentV610 {
        commonSafety(observation, MAX_COMMON_SPREAD, maxAbsorption = 84)?.let { return it }
        val breathing = observation.breathing!!
        val breath = breathing.buyerBreath
        val frame = observation.frame!!
        val micro = observation.micro!!
        if (breath.phase in setOf(
                BuyerBreathPhase.SELLER_TAKEOVER,
                BuyerBreathPhase.EXHAUSTION,
                BuyerBreathPhase.STALE
            )) {
            return veto(30 + observation.entryTuning.retestScoreOffset, "RETEST: продавцы/выдыхание не допускают rebound-вход")
        }
        if (pullbackPercent < 0.18) {
            return wait(0, 30 + observation.entryTuning.retestScoreOffset, "RETEST: откат ещё только ${fmt(pullbackPercent)}%")
        }
        if (pullbackPercent > 0.90) {
            return veto(30 + observation.entryTuning.retestScoreOffset, "RETEST: откат ${fmt(pullbackPercent)}% глубже рабочего диапазона")
        }

        val total5 = micro.buyNotional5m + micro.sellNotional5m
        val imbalance = if (total5 > 0.0) {
            (micro.buyNotional5m - micro.sellNotional5m) / total5
        } else (micro.aggressiveBuyPercent5m - 50.0) / 50.0
        val book = bookImbalance(observation)
        val absorptionPenalty = ((breath.absorptionRisk - 45).coerceAtLeast(0) * 0.20)
        val score = (
            frame.instant.coerceIn(-20, 20) * 0.80 +
                frame.score5m.coerceIn(-20, 20) * 0.35 +
                frame.score15m.coerceIn(-20, 20) * 0.12 +
                (micro.aggressiveBuyPercent15s - 50.0).coerceIn(-20.0, 30.0) * 0.70 +
                (micro.aggressiveBuyPercent60s - 50.0).coerceIn(-20.0, 25.0) * 0.30 +
                imbalance.coerceIn(-1.0, 1.0) * 11.0 +
                book.coerceIn(-1.0, 1.0) * 6.0 +
                reboundPercent.coerceIn(0.0, 0.60) * 28.0 - absorptionPenalty
            ).toInt().coerceIn(-100, 100)
        val threshold = (30 + observation.entryTuning.retestScoreOffset).coerceIn(24, 40)
        val reboundVisible = reboundPercent >= 0.10
        val shortFlowRecovered = frame.instant >= 3 && frame.score5m >= -3 &&
            micro.aggressiveBuyPercent15s >= 53.0 && micro.aggressiveBuyPercent60s >= 50.0
        val mediumNotBroken = frame.score15m >= -12 && micro.aggressiveBuyPercent5m >= 50.0
        val allowed = reboundVisible && shortFlowRecovered && mediumNotBroken && score >= threshold
        val near = reboundVisible && shortFlowRecovered && score >= threshold - 6
        val strong = allowed && score >= threshold + 7 && frame.instant >= 6 &&
            micro.aggressiveBuyPercent15s >= 57.0 && breath.absorptionRisk <= 60
        val reason = "RETEST rebound: score $score/$threshold; pullback=${fmt(pullbackPercent)}%, " +
            "rebound=${fmt(reboundPercent)}%; мгн/5/15=${frame.instant}/${frame.score5m}/${frame.score15m}; " +
            "buyers15s/60s/5m=${fmt(micro.aggressiveBuyPercent15s)}/${fmt(micro.aggressiveBuyPercent60s)}/${fmt(micro.aggressiveBuyPercent5m)}; " +
            "стакан=${signed(book * 100)}%; поглощение=${breath.absorptionRisk}"
        return PumpVariantEntryAssessmentV610(score, threshold, allowed, near, false, strong, reason)
    }

    fun safeContinuation(
        observation: SharedFusionEntryObservation,
        appSupport: Boolean
    ): PumpVariantEntryAssessmentV610 {
        commonSafety(observation, MAX_SAFE_SPREAD, maxAbsorption = 77)?.let { return it }
        val breath = observation.breathing!!.buyerBreath
        val frame = observation.frame!!
        val micro = observation.micro!!
        if (breath.phase !in setOf(
                BuyerBreathPhase.IGNITION,
                BuyerBreathPhase.EXPANSION,
                BuyerBreathPhase.MATURE
            )) {
            return veto(39 + observation.entryTuning.safeScoreOffset, "SAFE: нужна живая фаза роста, сейчас ${breath.phase}")
        }
        val move = max(0.0, max(breath.moveSincePhaseStartPercent ?: 0.0, breath.priceChange5mPercent ?: 0.0))
        val maxMove = (1.60 - observation.entryTuning.chaseTighteningBps / 100.0).coerceAtLeast(0.90)
        if (move > maxMove) {
            return veto(39 + observation.entryTuning.safeScoreOffset, "SAFE: движение уже ${fmt(move)}%, поздно входить даже в continuation")
        }

        val total5 = micro.buyNotional5m + micro.sellNotional5m
        val imbalance = if (total5 > 0.0) {
            (micro.buyNotional5m - micro.sellNotional5m) / total5
        } else (micro.aggressiveBuyPercent5m - 50.0) / 50.0
        val book = bookImbalance(observation)
        val activity = breath.activityRatio?.let { ((it - 1.0) * 5.0).coerceIn(-4.0, 7.0) } ?: 0.0
        val efficiency = ((breath.efficiencyScore ?: 0) / 12.0).coerceIn(-6.0, 7.0)
        val phaseBonus = when (breath.phase) {
            BuyerBreathPhase.IGNITION -> 5.0
            BuyerBreathPhase.EXPANSION -> 7.0
            BuyerBreathPhase.MATURE -> 2.0
            else -> 0.0
        }
        val appBonus = if (appSupport) 6.0 else 0.0
        val absorptionPenalty = ((breath.absorptionRisk - 40).coerceAtLeast(0) * 0.28)
        val score = (
            frame.instant.coerceIn(-20, 20) * 0.65 +
                frame.score5m.coerceIn(-20, 20) * 0.45 +
                frame.score15m.coerceIn(-20, 20) * 0.25 +
                frame.score30m.coerceIn(-20, 20) * 0.10 +
                (micro.aggressiveBuyPercent60s - 50.0).coerceIn(-20.0, 25.0) * 0.35 +
                imbalance.coerceIn(-1.0, 1.0) * 13.0 +
                book.coerceIn(-1.0, 1.0) * 7.0 + activity + efficiency + phaseBonus + appBonus - absorptionPenalty
            ).toInt().coerceIn(-100, 100)
        val threshold = (39 + observation.entryTuning.safeScoreOffset).coerceIn(33, 48)
        val directionalFloor = frame.instant >= 5 && frame.score5m >= 2 && frame.score15m >= -2 &&
            micro.aggressiveBuyPercent60s >= 52.0 && micro.aggressiveBuyPercent5m >= 53.0
        val sellerPressure = imbalance < -0.16 && frame.instant < 7
        if (sellerPressure) return veto(threshold, "SAFE: выполненные продажи всё ещё слишком сильны")
        val allowed = directionalFloor && score >= threshold
        val near = directionalFloor && score >= threshold - 6
        val spread = observation.bookSpreadPercent ?: micro.spreadPercent ?: 0.0
        val strong = allowed && score >= threshold + 7 && breath.absorptionRisk <= 55 &&
            spread <= 0.25 && micro.aggressiveBuyPercent60s >= 56.0
        val reason = "SAFE continuation: score $score/$threshold${if (appSupport) " + APP" else ""}; " +
            "мгн/5/15/30=${frame.instant}/${frame.score5m}/${frame.score15m}/${frame.score30m}; " +
            "buyers60s/5m=${fmt(micro.aggressiveBuyPercent60s)}/${fmt(micro.aggressiveBuyPercent5m)}; " +
            "стакан=${signed(book * 100)}%; phase=${breath.phase}; absorption=${breath.absorptionRisk}"
        return PumpVariantEntryAssessmentV610(score, threshold, allowed, near, false, strong, reason)
    }

    private fun commonSafety(
        observation: SharedFusionEntryObservation,
        maxSpread: Double,
        maxAbsorption: Int
    ): PumpVariantEntryAssessmentV610? {
        val breathing = observation.breathing ?: return veto(0, "нет live breathing snapshot")
        val frame = observation.frame ?: return veto(0, "быстрый flow frame ещё не готов")
        val micro = observation.micro ?: return veto(0, "нет свежей реальной ленты сделок")
        if (!breathing.fresh || !breathing.buyerBreath.fresh || !micro.connected) {
            return veto(0, "лента сделок устарела")
        }
        if (micro.flowHistorySeconds < 60L) return veto(0, "нужно хотя бы 60 секунд живых сделок")
        if (observation.executionAsk <= 0.0) return veto(0, "нет исполнимой цены ask")
        val spread = observation.bookSpreadPercent ?: micro.spreadPercent
        if (spread != null && spread > maxSpread) return veto(0, "спред ${fmt(spread)}% слишком широк")
        if (breathing.buyerBreath.absorptionRisk >= maxAbsorption) {
            return veto(0, "поглощение ${breathing.buyerBreath.absorptionRisk}/100 слишком высоко")
        }
        if (frame.instant <= -12 && frame.score5m <= -8) {
            return veto(0, "быстрый поток уже захвачен продавцами")
        }
        return null
    }

    private fun bookImbalance(observation: SharedFusionEntryObservation): Double {
        val bid = observation.bookBidNotional ?: return 0.0
        val ask = observation.bookAskNotional ?: return 0.0
        return if (bid + ask > 0.0) (bid - ask) / (bid + ask) else 0.0
    }

    private fun veto(threshold: Int, reason: String) = PumpVariantEntryAssessmentV610(
        score = -100,
        threshold = threshold,
        allowed = false,
        nearCandidate = false,
        hardVeto = true,
        strongLocal = false,
        reason = reason
    )

    private fun wait(score: Int, threshold: Int, reason: String) = PumpVariantEntryAssessmentV610(
        score = score,
        threshold = threshold,
        allowed = false,
        nearCandidate = score >= threshold - 6,
        hardVeto = false,
        strongLocal = false,
        reason = reason
    )

    private fun fmt(value: Double) = String.format(java.util.Locale.GERMANY, "%.2f", value)
    private fun signed(value: Double) = String.format(java.util.Locale.GERMANY, "%+.1f", value)
}
