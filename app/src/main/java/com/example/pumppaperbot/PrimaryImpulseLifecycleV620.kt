package com.example.pumppaperbot

/**
 * V6.2 primary-PM market lifecycle model.
 *
 * This is intentionally not another score. AdaptiveBreathEntryPolicy already scores flow quality.
 * This layer answers two structural questions that a weighted score cannot answer safely:
 * 1) is a fast green burst only the first bounce inside a still deeply negative medium trend?
 * 2) is an in-position pullback a transient shakeout or a structurally failed impulse?
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
