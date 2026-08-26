package com.example.pumppaperbot

/**
 * V5.37 keeps the 15-second market observation shared, but decides fast-cycle eligibility
 * independently for every Pump Machine profile. A responsive profile must not depend on the
 * stricter PUMP_3 profile becoming a candidate first.
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
    fun evaluate(observation: SharedFusionEntryObservation): PumpFastCandidatePlanV537 =
        PumpFastCandidatePlanV537(
            pump3 = PumpProfitEngineV526.isFastCandidate(PumpProfitModeV526.PUMP_3, observation),
            pump2 = PumpProfitEngineV526.isFastCandidate(PumpProfitModeV526.PUMP_2, observation),
            retest = PumpProfitEngineV526.isFastCandidate(PumpProfitModeV526.PUMP_RETEST, observation),
            safe = PumpProfitEngineV526.isFastCandidate(PumpProfitModeV526.PUMP_SAFE, observation)
        )
}
