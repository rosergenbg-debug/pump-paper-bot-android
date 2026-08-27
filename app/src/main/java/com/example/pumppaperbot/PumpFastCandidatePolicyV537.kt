package com.example.pumppaperbot

/**
 * One shared 15-second observation, but independent fast-cycle eligibility for every PM profile.
 *
 * V6.1 also keeps the variant lanes warm while a plausible retest/continuation is developing.
 * The old implementation asked the ordinary impulse gate again during a pullback, which could
 * remove RETEST from the fast path precisely when it needed to observe the rebound.
 */
data class PumpFastCandidatePlanV537(
    val pump3: Boolean,
    val pump2: Boolean,
    val retest: Boolean,
    val safe: Boolean
) {
    val any: Boolean get() = pump3 || pump2 || retest || safe
}

object PumpFastCandidatePolicyV537 {
    fun evaluate(observation: SharedFusionEntryObservation): PumpFastCandidatePlanV537 {
        val frame = observation.frame
        val breath = observation.breathing?.buyerBreath
        val micro = observation.micro
        val ordinaryRetest = PumpProfitEngineV526.isFastCandidate(PumpProfitModeV526.PUMP_RETEST, observation)
        val ordinarySafe = PumpProfitEngineV526.isFastCandidate(PumpProfitModeV526.PUMP_SAFE, observation)

        // Bounded warm tracking, not trading authority. It only asks the local variant store to
        // re-evaluate more frequently; the store still requires its persisted seed/pullback state.
        val retestWarm = breath?.fresh == true && micro?.connected == true && frame != null &&
            breath.phase in setOf(BuyerBreathPhase.IGNITION, BuyerBreathPhase.EXPANSION, BuyerBreathPhase.MATURE) &&
            frame.score15m >= -8 && frame.score5m >= -8 &&
            micro.aggressiveBuyPercent5m >= 48.0 &&
            (breath.moveSincePhaseStartPercent ?: 0.0) <= 1.80
        val safeWarm = breath?.fresh == true && micro?.connected == true && frame != null &&
            breath.phase in setOf(BuyerBreathPhase.IGNITION, BuyerBreathPhase.EXPANSION, BuyerBreathPhase.MATURE) &&
            frame.instant >= 2 && frame.score5m >= -2 && micro.aggressiveBuyPercent60s >= 50.0

        return PumpFastCandidatePlanV537(
            pump3 = PumpProfitEngineV526.isFastCandidate(PumpProfitModeV526.PUMP_3, observation),
            pump2 = PumpProfitEngineV526.isFastCandidate(PumpProfitModeV526.PUMP_2, observation),
            retest = ordinaryRetest || retestWarm,
            safe = ordinarySafe || safeWarm
        )
    }
}
