package com.example.pumppaperbot

import kotlin.math.max

/**
 * Relative, volume-scale-independent entry model.
 *
 * A small market may move on small money and a busy market may ignore a large print, therefore
 * absolute USDT thresholds are deliberately not used here. V6.1 keeps the PM1/PM2 architecture
 * intact but corrects two observed timing errors: flat/noisy buyer imbalance is penalised, while a
 * genuinely accelerating move is not hard-rejected merely because the lifecycle label already
 * reached MATURE for a moment. Hard execution/data safety remains local and deterministic.
 */
object AdaptiveBreathEntryPolicy {
    data class Result(
        val score: Int,
        val threshold: Int,
        val allowed: Boolean,
        val nearCandidate: Boolean,
        val hardVeto: Boolean,
        val reason: String
    )

    private data class Profile(
        val threshold: Int,
        val hysteresis: Int,
        val maxMovePercent: Double
    )

    private fun profile(mode: PumpProfitModeV526) = when (mode) {
        PumpProfitModeV526.PUMP_2 -> Profile(25, 7, 1.65)
        // V6.1 gives the stricter PM2 a little more room before anti-chase veto. The old 1.15%
        // ceiling could reject a valid large impulse before its 30s confirmation completed.
        PumpProfitModeV526.PUMP_3 -> Profile(36, 7, 1.35)
        PumpProfitModeV526.PUMP_RETEST -> Profile(31, 7, 1.35)
        PumpProfitModeV526.PUMP_SAFE -> Profile(47, 6, 1.10)
    }

    fun evaluate(mode: PumpProfitModeV526, observation: SharedFusionEntryObservation): Result {
        val base = profile(mode)
        val tuning = observation.entryTuning
        val p = base.copy(
            threshold = (base.threshold + tuning.scoreOffset(mode)).coerceIn(18, 60),
            maxMovePercent = (base.maxMovePercent - tuning.chaseTighteningBps / 100.0)
                .coerceAtLeast(0.45)
        )
        val breathing = observation.breathing
            ?: return veto(p, "нет live breathing snapshot")
        val frame = observation.frame
            ?: return veto(p, "быстрый flow frame ещё не готов")
        val micro = observation.micro
            ?: return veto(p, "нет свежей реальной ленты сделок")
        if (!breathing.fresh || !breathing.buyerBreath.fresh || !micro.connected) {
            return veto(p, "лента сделок устарела")
        }
        if (micro.flowHistorySeconds < 60L) return veto(p, "нужно хотя бы 60 секунд живых сделок")
        if (observation.executionAsk <= 0.0) return veto(p, "нет исполнимой цены ask")
        val spread = observation.bookSpreadPercent ?: micro.spreadPercent
        if (spread != null && spread > 0.50) return veto(p, "спред ${fmt(spread)}% слишком широк")

        val breath = breathing.buyerBreath
        val move = max(0.0, max(breath.moveSincePhaseStartPercent ?: 0.0, breath.priceChange5mPercent ?: 0.0))
        if (move > p.maxMovePercent) return veto(p, "движение уже ${fmt(move)}%: поздно догонять")
        val absorptionLimit = (90 - tuning.absorptionTightening).coerceAtLeast(76)
        if (breath.absorptionRisk >= absorptionLimit) return veto(p, "поглощение ${breath.absorptionRisk}/100")

        val primaryMode = mode == PumpProfitModeV526.PUMP_2 || mode == PumpProfitModeV526.PUMP_3
        val matureReacceleration = primaryMode && breath.phase == BuyerBreathPhase.MATURE &&
            frame.instant >= if (mode == PumpProfitModeV526.PUMP_2) 8 else 10 &&
            frame.instant >= frame.score5m - 1 &&
            micro.aggressiveBuyPercent15s >= 56.0 &&
            micro.aggressiveBuyPercent60s >= 53.0 &&
            micro.priceChange60sPercent >= 0.08 &&
            breath.absorptionRisk <= 62
        if (breath.phase !in setOf(BuyerBreathPhase.IGNITION, BuyerBreathPhase.EXPANSION, BuyerBreathPhase.SHOCK) &&
            !matureReacceleration
        ) {
            return veto(p, "новый вход запрещён в фазе ${breath.phase}: нужен новый разгон, а не зрелая/тихая волна")
        }

        val fastDecelerating = frame.instant + tuning.decelerationGap < frame.score5m
        val mediumDecelerating = frame.score5m + tuning.decelerationGap < frame.score15m
        val severeDeceleration =
            (frame.instant + tuning.decelerationGap * 2 < frame.score5m && micro.aggressiveBuyPercent15s < 49.0) ||
                (frame.score5m + tuning.decelerationGap * 2 < frame.score15m && micro.aggressiveBuyPercent60s < 50.0)
        if (severeDeceleration) {
            return veto(
                p,
                "поток резко разваливается: мгн/5/15=${frame.instant}/${frame.score5m}/${frame.score15m}, покупатели 15с/60с=${fmt(micro.aggressiveBuyPercent15s)}/${fmt(micro.aggressiveBuyPercent60s)}"
            )
        }

        val total5 = micro.buyNotional5m + micro.sellNotional5m
        val tradeImbalance = if (total5 > 0.0) {
            (micro.buyNotional5m - micro.sellNotional5m) / total5
        } else (micro.aggressiveBuyPercent5m - 50.0) / 50.0
        val bid = observation.bookBidNotional
        val ask = observation.bookAskNotional
        val bookImbalance = if (bid != null && ask != null && bid + ask > 0.0) (bid - ask) / (bid + ask) else 0.0
        val activity = breath.activityRatio?.let { ((it - 1.0) * 6.0).coerceIn(-5.0, 8.0) } ?: 0.0
        val efficiency = ((breath.efficiencyScore ?: 0) / 10.0).coerceIn(-7.0, 7.0)
        val curvature = ((frame.instant - frame.score5m) * 0.55).coerceIn(-10.0, 10.0)
        val phase = when (breath.phase) {
            BuyerBreathPhase.IGNITION -> 7.0
            BuyerBreathPhase.EXPANSION -> 5.0
            BuyerBreathPhase.MATURE -> if (matureReacceleration) 1.0 else -10.0
            BuyerBreathPhase.QUIET -> -12.0
            BuyerBreathPhase.SHOCK -> 2.0
            BuyerBreathPhase.EXHAUSTION -> -8.0
            BuyerBreathPhase.SELLER_TAKEOVER -> -12.0
            BuyerBreathPhase.STALE -> -20.0
        }
        val absorptionPenalty = ((breath.absorptionRisk - 45).coerceAtLeast(0) * 0.22)

        // V6.1 noise correction: buyer imbalance without price response is not enough. This is a
        // soft penalty rather than a veto so a very strong early flow can still become a candidate.
        val response5m = breath.priceChange5mPercent ?: 0.0
        val response60s = micro.priceChange60sPercent
        val flatNoisePenalty = when {
            response5m < 0.02 && response60s < 0.02 -> 10.0
            response5m < 0.05 && response60s < 0.04 -> 6.0
            else -> 0.0
        }
        val realMoveBonus = when {
            response60s >= 0.25 -> 4.0
            response60s >= 0.12 -> 2.0
            else -> 0.0
        }
        val decelerationPenalty = (if (fastDecelerating) 5.0 else 0.0) +
            (if (mediumDecelerating) 3.0 else 0.0)

        val score = (
            frame.instant.coerceIn(-20, 20) * 0.62 +
                frame.score5m.coerceIn(-20, 20) * 0.42 +
                frame.score15m.coerceIn(-20, 20) * 0.18 +
                frame.score30m.coerceIn(-20, 20) * 0.08 +
                curvature + tradeImbalance.coerceIn(-1.0, 1.0) * 18.0 +
                bookImbalance.coerceIn(-1.0, 1.0) * 8.0 + activity + efficiency + phase +
                realMoveBonus - flatNoisePenalty - decelerationPenalty - absorptionPenalty
            ).toInt().coerceIn(-100, 100)

        val sellerDominance = tradeImbalance < -0.28 && frame.instant < 4 && frame.score5m < 2
        if (sellerDominance) return veto(p, "исполненные продажи доминируют, быстрый поток не восстанавливается", score)
        val allowed = score >= p.threshold
        val near = score >= p.threshold - p.hysteresis
        val direction = "мгн/5/15/30=${frame.instant}/${frame.score5m}/${frame.score15m}/${frame.score30m}"
        val evidence = "дисбаланс сделок=${fmtSigned(tradeImbalance * 100)}%, стакан=${fmtSigned(bookImbalance * 100)}%, активность=${breath.activityRatio?.let(::fmt) ?: "нет базы"}, цена60с=${fmtSigned(response60s)}%"
        val timing = buildString {
            if (flatNoisePenalty > 0.0) append("; шум−${flatNoisePenalty.toInt()}")
            if (decelerationPenalty > 0.0) append("; торможение−${decelerationPenalty.toInt()}")
            if (realMoveBonus > 0.0) append("; движение+${realMoveBonus.toInt()}")
            if (matureReacceleration) append("; MATURE re-acceleration")
        }
        return Result(
            score, p.threshold, allowed, near, false,
            "$direction; $evidence; фаза=${breath.phase}; поглощение=${breath.absorptionRisk}$timing; tuning r${tuning.revision}"
        )
    }

    private fun veto(profile: Profile, reason: String, score: Int = -100) =
        Result(score, profile.threshold, false, false, true, reason)

    private fun fmt(value: Double) = String.format(java.util.Locale.GERMANY, "%.2f", value)
    private fun fmtSigned(value: Double) = String.format(java.util.Locale.GERMANY, "%+.1f", value)
}
