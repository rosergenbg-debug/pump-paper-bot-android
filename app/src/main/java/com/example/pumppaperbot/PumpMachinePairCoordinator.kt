package com.example.pumppaperbot

import android.content.Context
import org.json.JSONObject

data class PumpPairEntryDirective(val decision: SharedFusionEntryDecision)

data class PumpMachinePairSyncResult(
    val pump3: PumpMachineSyncResult,
    val pump2: PumpMachine2SyncResult
)

/**
 * V5.27 fair-comparison coordinator.
 *
 * PM2 and PM3 share one persisted entry state, one observation, one timestamp and one
 * executable Bitpanda ask. After either account exits, it stays in EUR until the other
 * account finishes the same market episode. Their exits remain fully independent.
 */
object PumpMachinePairCoordinator {
    private const val PREFS = "pump_machine_pair_entry_v527"
    private const val STABILITY = "stability"

    @Synchronized
    fun sync(context: Context, now: Long = System.currentTimeMillis()): PumpMachinePairSyncResult {
        val pump3Before = PumpMachineStore.state(context)
        val pump2Before = PumpMachine2Store.state(context)
        val bothFlat = !pump3Before.inPosition && !pump2Before.inPosition
        val oneStillRunning = pump3Before.inPosition.xor(pump2Before.inPosition)

        val directive = when {
            bothFlat -> {
                val pairPrevious = pairStability(context)
                val pump3Previous = PumpMachineStore.stability(context)
                val pump2Previous = PumpMachine2Store.stability(context)
                val previous = pairPrevious.copy(
                    cooldownUntil = maxOf(
                        pairPrevious.cooldownUntil,
                        pump3Previous.cooldownUntil,
                        pump2Previous.cooldownUntil
                    ),
                    lastExitAt = maxOf(
                        pairPrevious.lastExitAt,
                        pump3Previous.lastExitAt,
                        pump2Previous.lastExitAt
                    ),
                    lastLossExitAt = maxOf(
                        pairPrevious.lastLossExitAt,
                        pump3Previous.lastLossExitAt,
                        pump2Previous.lastLossExitAt
                    ),
                    lossExitStreak = maxOf(
                        pairPrevious.lossExitStreak,
                        pump3Previous.lossExitStreak,
                        pump2Previous.lossExitStreak
                    )
                )
                val observation = SharedFusionEntryObservationStore.snapshot(context, now)
                val shared = PumpProfitEngineV526.evaluateEntry(
                    PumpProfitModeV526.PUMP_3,
                    previous,
                    observation,
                    now
                )
                val named = shared.copy(
                    reason = shared.reason.replace("V526_PM3", "V527_PAIR")
                        .replace("V526 PM3", "V527 PAIR")
                )
                val persisted = if (named.action == "BUY") {
                    named.nextState.copy(entryStreak = 0, entryCandidateAt = 0L)
                } else named.nextState
                savePairStability(context, persisted)
                PumpPairEntryDirective(named)
            }
            oneStillRunning -> {
                val waiting = SharedFusionEntryDecision(
                    action = null,
                    nextState = pairStability(context).copy(
                        entryStreak = 0,
                        entryCandidateAt = 0L
                    ),
                    reason = "V527_PAIR_WAIT: один Pump-счёт ещё ведёт общую сделку; новый вход начнётся только вместе"
                )
                savePairStability(context, waiting.nextState)
                PumpPairEntryDirective(waiting)
            }
            else -> null
        }

        // Both stores read the same already-refreshed Bitpanda snapshot and the same `now`.
        val pump3 = PumpMachineStore.sync(context, now, directive)
        val pump2 = PumpMachine2Store.sync(context, now, directive)
        return PumpMachinePairSyncResult(pump3, pump2)
    }

    private fun pairStability(context: Context): FusionStabilityState {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(STABILITY, null)
        return runCatching { FusionStabilityState.fromJson(JSONObject(raw.orEmpty())) }
            .getOrDefault(FusionStabilityState())
    }

    private fun savePairStability(context: Context, value: FusionStabilityState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(STABILITY, value.toJson().toString())
            .apply()
    }
}
