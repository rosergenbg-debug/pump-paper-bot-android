package com.example.pumppaperbot

import kotlin.math.max

/**
 * Relative, volume-scale-independent entry model.
 *
 * A small market may move on small money and a busy market may ignore a large print, therefore
 * absolute USDT thresholds are deliberately not used here. The score combines normalized trade
 * imbalance, acceleration across flow horizons, book imbalance and price response. Only unsafe
 * execution/data conditions are hard vetoes; ordinary disagreement lowers the score.
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
        PumpProfitModeV526.PUMP_3 -> Profile(36, 7, 1.15)
        PumpProfitModeV526.PUMP_RETEST -> Profile(31, 7, 1.35)
        PumpProfitModeV526.PUMP_SAFE -> Profile(47, 6, 1.10)
    }

    fun evaluate(mode: PumpProfitModeV526, observation: SharedFusionEntryObservation): Result {
        val p = profile(mode)
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
        if (breath.absorptionRisk >= 90) return veto(p, "поглощение ${breath.absorptionRisk}/100")
        if ((breath.phase == BuyerBreathPhase.SELLER_TAKEOVER || breath.phase == BuyerBreathPhase.EXHAUSTION) && frame.instant <= 0) {
            return veto(p, "продавцы перехватывают поток")
        }

        val total5 = micro.buyNotional5m + micro.sellNotional5m
        val tradeImbalance = if (total5 > 0.0) {
            (micro.buyNotional5m - micro.sellNotional5m) / total5
        } else (micro.aggressiveBuyPercent5m - 50.0) / 50.0
        val bid = observation.bookBidNotional
        val ask = observation.bookAskNotional
        val bookImbalance = if (bid != null && ask != null && bid + ask > 0.0) (bid - ask) / (bid + ask) else 0.0
        val activity = breath.activityRatio?.let { ((it - 1.0) * 8.0).coerceIn(-6.0, 10.0) } ?: 0.0
        val efficiency = ((breath.efficiencyScore ?: 0) / 10.0).coerceIn(-7.0, 7.0)
        val curvature = ((frame.instant - frame.score5m) * 0.40).coerceIn(-8.0, 8.0)
        val phase = when (breath.phase) {
            BuyerBreathPhase.IGNITION -> 7.0
            BuyerBreathPhase.EXPANSION -> 5.0
            BuyerBreathPhase.MATURE -> 1.0
            BuyerBreathPhase.QUIET -> -4.0
            BuyerBreathPhase.SHOCK -> 2.0
            BuyerBreathPhase.EXHAUSTION -> -8.0
            BuyerBreathPhase.SELLER_TAKEOVER -> -12.0
            BuyerBreathPhase.STALE -> -20.0
        }
        val absorptionPenalty = ((breath.absorptionRisk - 45).coerceAtLeast(0) * 0.22)
        val score = (
            frame.instant.coerceIn(-20, 20) * 0.70 +
                frame.score5m.coerceIn(-20, 20) * 0.60 +
                frame.score15m.coerceIn(-20, 20) * 0.25 +
                frame.score30m.coerceIn(-20, 20) * 0.15 +
                curvature + tradeImbalance.coerceIn(-1.0, 1.0) * 22.0 +
                bookImbalance.coerceIn(-1.0, 1.0) * 10.0 + activity + efficiency + phase - absorptionPenalty
            ).toInt().coerceIn(-100, 100)

        val sellerDominance = tradeImbalance < -0.28 && frame.instant < 4 && frame.score5m < 2
        if (sellerDominance) return veto(p, "исполненные продажи доминируют, быстрый поток не восстанавливается", score)
        val allowed = score >= p.threshold
        val near = score >= p.threshold - p.hysteresis
        val direction = "мгн/5/15/30=${frame.instant}/${frame.score5m}/${frame.score15m}/${frame.score30m}"
        val evidence = "дисбаланс сделок=${fmtSigned(tradeImbalance * 100)}%, стакан=${fmtSigned(bookImbalance * 100)}%, активность=${breath.activityRatio?.let(::fmt) ?: "нет базы"}"
        return Result(score, p.threshold, allowed, near, false, "$direction; $evidence; фаза=${breath.phase}; поглощение=${breath.absorptionRisk}")
    }

    private fun veto(profile: Profile, reason: String, score: Int = -100) =
        Result(score, profile.threshold, false, false, true, reason)

    private fun fmt(value: Double) = String.format(java.util.Locale.GERMANY, "%.2f", value)
    private fun fmtSigned(value: Double) = String.format(java.util.Locale.GERMANY, "%+.1f", value)
}
