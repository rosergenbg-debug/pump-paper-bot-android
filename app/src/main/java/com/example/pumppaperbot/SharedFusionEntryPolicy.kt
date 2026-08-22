package com.example.pumppaperbot

import android.content.Context

/**
 * V5.22 single source of truth for entry timing shared by Fusion and Pump Machine.
 *
 * Both paper accounts consume the same short-lived market observation but keep their own
 * FusionStabilityState (confirmations, cooldowns and later exits). This means the entry brain
 * is physically one implementation without forcing the two accounts to remain synchronized
 * after one of them exits earlier.
 */
data class SharedFusionEntryObservation(
    val frame: FusionFlowFrame?,
    val shockReady: Boolean,
    val sampledAt: Long,
    val sampleBucket: Long
)

data class SharedFusionEntryDecision(
    val action: String?,
    val nextState: FusionStabilityState,
    val reason: String
)

object SharedFusionEntryObservationStore {
    private const val SAMPLE_MILLIS = 15_000L
    private var cachedBucket = Long.MIN_VALUE
    private var cached: SharedFusionEntryObservation? = null

    @Synchronized
    fun snapshot(context: Context, now: Long = System.currentTimeMillis()): SharedFusionEntryObservation {
        val bucket = now / SAMPLE_MILLIS
        cached?.takeIf { cachedBucket == bucket }?.let { return it }

        val breathing = LiveMarketBreathingStore.snapshot(context, now)
        val shock = ShockReboundStore.state(context)
        return SharedFusionEntryObservation(
            frame = FusionFlowPolicy.frame(breathing),
            shockReady = shock.fresh(now) && shock.ready,
            sampledAt = now,
            sampleBucket = bucket
        ).also {
            cachedBucket = bucket
            cached = it
        }
    }
}

object SharedFusionEntryPolicy {
    fun evaluate(
        previous: FusionStabilityState,
        observation: SharedFusionEntryObservation,
        now: Long
    ): SharedFusionEntryDecision {
        if (previous.cooldownUntil > now) {
            val leftSeconds = ((previous.cooldownUntil - now + 999L) / 1000L).coerceAtLeast(1L)
            return SharedFusionEntryDecision(
                null,
                previous.copy(
                    entryStreak = 0,
                    entryCandidateAt = 0L,
                    exitStreak = 0,
                    exitArmedAt = 0L,
                    exitArmedBid = 0.0,
                    peakBid = 0.0,
                    profitDefenseArmed = false
                ),
                "COOLDOWN: общий Fusion-вход заблокирован ещё ${leftSeconds}с после собственного выхода"
            )
        }

        if (observation.shockReady) {
            return SharedFusionEntryDecision(
                "BUY",
                previous.copy(
                    entryStreak = 0,
                    entryCandidateAt = 0L,
                    exitStreak = 0,
                    exitArmedAt = 0L,
                    exitArmedBid = 0.0,
                    peakBid = 0.0,
                    profitDefenseArmed = false,
                    cooldownUntil = 0L
                ),
                "SHOCK_REBOUND_ENTRY: общий Fusion-entry engine получил подтверждённый быстрый отскок"
            )
        }

        val frame = observation.frame
        val buy = frame?.buySignal == true
        if (!buy) {
            return SharedFusionEntryDecision(
                null,
                previous.copy(
                    entryStreak = 0,
                    entryCandidateAt = 0L,
                    exitStreak = 0,
                    exitArmedAt = 0L,
                    exitArmedBid = 0.0,
                    peakBid = 0.0,
                    profitDefenseArmed = false,
                    cooldownUntil = 0L
                ),
                "WAIT: общий Fusion-вход сейчас/5/15/30 ещё не собран"
            )
        }

        val candidateAt = if (previous.entryStreak > 0 && previous.entryCandidateAt > 0L) {
            previous.entryCandidateAt
        } else now
        val streak = (previous.entryStreak + 1).coerceAtMost(FusionStabilityPolicy.ENTRY_CONFIRMATIONS)
        val confirmedByTime = now - candidateAt >= FusionStabilityPolicy.ENTRY_CONFIRM_MIN_MILLIS
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

        return if (streak >= FusionStabilityPolicy.ENTRY_CONFIRMATIONS && confirmedByTime) {
            SharedFusionEntryDecision(
                "BUY",
                next,
                "ENTRY_CONFIRMED: общий Fusion-entry engine — сейчас/5/15/30 подтверждены минимум двумя наблюдениями и 60с"
            )
        } else {
            val elapsed = (now - candidateAt).coerceAtLeast(0L)
            val left = ((FusionStabilityPolicy.ENTRY_CONFIRM_MIN_MILLIS - elapsed).coerceAtLeast(0L) + 999L) / 1000L
            SharedFusionEntryDecision(
                null,
                next,
                if (frame?.strongBuy == true) {
                    "ENTRY_ARMED_STRONG ${streak}/${FusionStabilityPolicy.ENTRY_CONFIRMATIONS}: общий сильный BUY; подтверждаем ещё ${left}с"
                } else {
                    "ENTRY_ARMED ${streak}/${FusionStabilityPolicy.ENTRY_CONFIRMATIONS}: общий BUY подтверждается; ещё ${left}с"
                }
            )
        }
    }
}
