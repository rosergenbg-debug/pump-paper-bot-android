package com.example.pumppaperbot

/**
 * V6.2 primary-PM market lifecycle model.
 *
 * This is intentionally not another score. AdaptiveBreathEntryPolicy already scores flow quality.
 * This layer answers three structural questions that a weighted score cannot answer safely:
 * 1) is a fast green burst only the first bounce inside a still deeply negative medium trend?
 * 2) is an in-position pullback a transient shakeout or a structurally failed impulse?
 * 3) after one stopped false start, has a genuinely new aligned regime appeared strongly enough
 *    to re-arm normal confirmation without waiting out the whole stale cooldown?
 *
 * It is pure/deterministic and does not place trades.
 */
enum class PrimaryImpulseRegimeV620 {
    ALIGNED,
    REVERSAL_SEED,
    REPAIRING_REVERSAL,
    BURST_NEEDS_ACCEPTANCE,
    BROKEN
}

data class PrimaryImpulseAssessmentV620(
    val regime: PrimaryImpulseRegimeV620,
    val keepFastTracking: Boolean,
    val blockPrimaryEntry: Boolean,
    val allowStrongShortcut: Boolean,
    val reason: String
)

object PrimaryImpulseLifecycleV620 {
    private const val LOSS_RESCUE_WINDOW_MILLIS = 12L * 60L * 1000L

    fun assess(observation: SharedFusionEntryObservation): PrimaryImpulseAssessmentV620 {
        val frame = observation.frame
            ?: return PrimaryImpulseAssessmentV620(
                PrimaryImpulseRegimeV620.BROKEN, false, true, false, "нет flow frame"
            )
        val micro = observation.micro
            ?: return PrimaryImpulseAssessmentV620(
                PrimaryImpulseRegimeV620.BROKEN, false, true, false, "нет micro tape"
            )
        val breath = observation.breathing?.buyerBreath
            ?: return PrimaryImpulseAssessmentV620(
                PrimaryImpulseRegimeV620.BROKEN, false, true, false, "нет buyer-breath"
            )

        val shortTurnedUp = frame.instant >= 8 && frame.score5m >= 2 &&
            micro.aggressiveBuyPercent15s >= 55.0 && micro.aggressiveBuyPercent60s >= 52.0
        val deepMediumDebt = frame.score15m <= -20 && frame.score30m <= -20
        if (deepMediumDebt && shortTurnedUp) {
            return PrimaryImpulseAssessmentV620(
                PrimaryImpulseRegimeV620.REVERSAL_SEED,
                keepFastTracking = true,
                blockPrimaryEntry = true,
                allowStrongShortcut = false,
                reason = "короткий BUY-разворот уже виден, но 15/30м ещё глубоко отрицательны (${frame.score15m}/${frame.score30m}); наблюдаем ремонт, не покупаем первый отскок"
            )
        }

        val repairing = shortTurnedUp &&
            (frame.score15m < -8 || frame.score30m < -12) &&
            !deepMediumDebt
        if (repairing) {
            return PrimaryImpulseAssessmentV620(
                PrimaryImpulseRegimeV620.REPAIRING_REVERSAL,
                keepFastTracking = true,
                blockPrimaryEntry = false,
                allowStrongShortcut = false,
                reason = "средний поток ещё ремонтируется (${frame.score15m}/${frame.score30m}); обычное профильное подтверждение обязательно"
            )
        }

        // A vertical one-minute burst is not automatically bad. It simply loses the V6.1
        // shortcut that let strict PM confirm in 15s and chase a wider price interval.
        val verticalBurst = micro.priceChange60sPercent >= 0.33 &&
            (frame.instant >= 30 || micro.tradeAcceleration >= 1.50) &&
            micro.aggressiveBuyPercent15s >= 70.0
        if (verticalBurst) {
            return PrimaryImpulseAssessmentV620(
                PrimaryImpulseRegimeV620.BURST_NEEDS_ACCEPTANCE,
                keepFastTracking = true,
                blockPrimaryEntry = false,
                allowStrongShortcut = false,
                reason = "вертикальный 60с burst ${fmt(micro.priceChange60sPercent)}%; не запрещаем импульс, но убираем ускоренное/chase-подтверждение"
            )
        }

        val aligned = frame.score15m >= -8 && frame.score30m >= -12
        return PrimaryImpulseAssessmentV620(
            regime = if (aligned) PrimaryImpulseRegimeV620.ALIGNED else PrimaryImpulseRegimeV620.REPAIRING_REVERSAL,
            keepFastTracking = shortTurnedUp || frame.instant >= 5,
            blockPrimaryEntry = false,
            allowStrongShortcut = aligned,
            reason = if (aligned) {
                "короткий и средний поток не конфликтуют"
            } else {
                "средний поток неоднозначен; используем обычное подтверждение"
            }
        )
    }

    /**
     * One stopped false start must not blind a primary PM to a clearly new aligned ignition.
     * This only waives the remaining time-lock for ONE recent loss. It never creates a BUY:
     * the ordinary adaptive score, hard vetoes, price acceptance and confirmation still run.
     * Two consecutive losses keep the cooldown hard.
     */
    fun cooldownRescueEligible(
        mode: PumpProfitModeV526,
        previous: FusionStabilityState,
        observation: SharedFusionEntryObservation,
        now: Long
    ): Boolean {
        if (mode !in setOf(PumpProfitModeV526.PUMP_2, PumpProfitModeV526.PUMP_3)) return false
        if (previous.cooldownUntil <= now || previous.lastLossExitAt <= 0L) return false
        if (previous.lossExitStreak != 1) return false
        val lossAge = now - previous.lastLossExitAt
        if (lossAge !in 0..LOSS_RESCUE_WINDOW_MILLIS) return false

        val lifecycle = assess(observation)
        if (lifecycle.regime != PrimaryImpulseRegimeV620.ALIGNED) return false
        val frame = observation.frame ?: return false
        val micro = observation.micro ?: return false
        val breath = observation.breathing?.buyerBreath ?: return false
        if (!breath.fresh || !micro.connected) return false
        if (breath.phase !in setOf(BuyerBreathPhase.IGNITION, BuyerBreathPhase.EXPANSION)) return false

        val minInstant = if (mode == PumpProfitModeV526.PUMP_2) 14 else 16
        if (frame.instant < minInstant || frame.score5m < 8 || frame.score15m < 2 || frame.score30m < -2) return false
        if (micro.aggressiveBuyPercent15s < 62.0 || micro.aggressiveBuyPercent60s < 58.0 ||
            micro.aggressiveBuyPercent5m < 55.0
        ) return false
        if (micro.tradeAcceleration < 1.15 || micro.priceChange60sPercent < 0.12) return false
        if (breath.absorptionRisk > 55) return false
        val spread = observation.bookSpreadPercent ?: micro.spreadPercent
        if (spread != null && spread > 0.30) return false

        val move = maxOf(
            breath.moveSincePhaseStartPercent ?: 0.0,
            breath.priceChange5mPercent ?: 0.0
        ).coerceAtLeast(0.0)
        val rescueChaseLimit = if (mode == PumpProfitModeV526.PUMP_2) 1.20 else 0.90
        if (move > rescueChaseLimit) return false
        return true
    }

    /**
     * Structural failure is deliberately harder than a single transient SELLER_TAKEOVER phase.
     * It is used only for position supervision; hard NET stops remain independent.
     */
    fun structuralFailure(observation: SharedFusionEntryObservation?): Boolean {
        observation ?: return false
        val frame = observation.frame ?: return false
        val breath = observation.breathing?.buyerBreath
        val micro = observation.micro
        if (frame.severeExitSignal) return true

        val sellerTapeConfirmed = breath?.phase == BuyerBreathPhase.SELLER_TAKEOVER &&
            frame.instant <= -6 &&
            (micro?.aggressiveBuyPercent15s ?: 50.0) <= 42.0 &&
            (micro?.aggressiveBuyPercent60s ?: 50.0) <= 46.0
        if (sellerTapeConfirmed) return true

        val absorptionBreak = (breath?.absorptionRisk ?: 0) >= 90 && frame.instant <= -8
        return absorptionBreak
    }

    fun ordinaryDeterioration(observation: SharedFusionEntryObservation?): Boolean {
        observation ?: return false
        val frame = observation.frame
        val breath = observation.breathing?.buyerBreath
        return frame?.deteriorationSignal == true ||
            breath?.phase == BuyerBreathPhase.SELLER_TAKEOVER ||
            ((breath?.absorptionRisk ?: 0) >= 85 && (frame?.instant ?: 0) < 0)
    }

    private fun fmt(value: Double) = String.format(java.util.Locale.GERMANY, "%.2f", value)
}
