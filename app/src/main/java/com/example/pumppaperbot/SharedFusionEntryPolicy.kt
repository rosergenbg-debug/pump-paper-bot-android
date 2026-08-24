package com.example.pumppaperbot

import android.content.Context

/**
 * V5.22 single source of truth for entry timing shared by Fusion and Pump Machine.
 *
 * Fusion, PM2 and PM3 consume the same short-lived market observation through separate
 * persisted stability states. Since V5.28, PM2 and PM3 use the same strict eligibility
 * criteria but independently confirm, execute, cool down and re-enter.
 */
data class SharedFusionEntryObservation(
    val frame: FusionFlowFrame?,
    val shockReady: Boolean,
    val sampledAt: Long,
    val sampleBucket: Long,
    val breathing: LiveMarketBreathingSnapshot? = null,
    val micro: MicroImpulseSnapshot? = null,
    val executionAsk: Double = 0.0,
    val bookBidNotional: Double? = null,
    val bookAskNotional: Double? = null,
    val bookSpreadPercent: Double? = null,
    val capitalFlow: CapitalFlowProxy = CapitalFlowProxy()
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
        val micro = MicroImpulseStore.state(context).takeIf {
            it.connected && DeepSeekFreshMarketContext.isFresh(
                it.updatedAt, now, DeepSeekFreshMarketContext.MICRO_MAX_AGE
            )
        }
        val market = PumpBotEngine.snapshot(context)
        val fusionMarket = BitpandaFusionStore.state(context)
        val impulse = ImpulseRadarStore.state(context)
        return SharedFusionEntryObservation(
            frame = FusionFlowPolicy.frame(breathing),
            shockReady = shock.fresh(now) && shock.ready,
            sampledAt = now,
            sampleBucket = bucket,
            breathing = breathing,
            micro = micro,
            executionAsk = fusionMarket.ask.takeIf { fusionMarket.fresh(now) } ?: 0.0,
            bookBidNotional = market.bookBidNotional,
            bookAskNotional = market.bookAskNotional,
            bookSpreadPercent = market.spreadPercent,
            capitalFlow = CapitalFlowProxyPolicy.evaluate(impulse, breathing, now)
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
                    entryAnchorAsk = 0.0,
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
            val capital = CapitalParticipationGate.evaluate(observation)
            if (!capital.allowed) {
                return SharedFusionEntryDecision(
                    null,
                    previous.copy(entryStreak = 0, entryCandidateAt = 0L, entryAnchorAsk = 0.0),
                    "SHOCK_CAPITAL_WAIT: ${capital.reason}"
                )
            }
            return SharedFusionEntryDecision(
                "BUY",
                previous.copy(
                    entryStreak = 0,
                    entryCandidateAt = 0L,
                    entryAnchorAsk = 0.0,
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
                    entryAnchorAsk = 0.0,
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

        val capital = CapitalParticipationGate.evaluate(observation)
        if (!capital.allowed) {
            return SharedFusionEntryDecision(
                null,
                previous.copy(entryStreak = 0, entryCandidateAt = 0L, entryAnchorAsk = 0.0),
                "CAPITAL_WAIT: ${capital.reason}"
            )
        }

        val candidateAt = if (previous.entryStreak > 0 && previous.entryCandidateAt > 0L) {
            previous.entryCandidateAt
        } else now
        val anchorAsk = if (previous.entryStreak > 0 && previous.entryAnchorAsk > 0.0) {
            previous.entryAnchorAsk
        } else observation.executionAsk
        val streak = (previous.entryStreak + 1).coerceAtMost(FusionStabilityPolicy.ENTRY_CONFIRMATIONS)
        val confirmedByTime = now - candidateAt >= FusionStabilityPolicy.ENTRY_CONFIRM_MIN_MILLIS
        val next = previous.copy(
            entryStreak = streak,
            entryCandidateAt = candidateAt,
            entryAnchorAsk = anchorAsk,
            exitStreak = 0,
            exitArmedAt = 0L,
            exitArmedBid = 0.0,
            peakBid = 0.0,
            profitDefenseArmed = false,
            cooldownUntil = 0L
        )

        val acceptance = CapitalParticipationGate.priceAcceptance(anchorAsk, observation.executionAsk)
        return if (streak >= FusionStabilityPolicy.ENTRY_CONFIRMATIONS && confirmedByTime && acceptance.allowed) {
            SharedFusionEntryDecision(
                "BUY",
                next,
                "ENTRY_CONFIRMED: сейчас/5/15/30, крупные BUY, стакан и принятие цены подтверждены; ${capital.reason}; ${acceptance.reason}"
            )
        } else {
            val elapsed = (now - candidateAt).coerceAtLeast(0L)
            val left = ((FusionStabilityPolicy.ENTRY_CONFIRM_MIN_MILLIS - elapsed).coerceAtLeast(0L) + 999L) / 1000L
            SharedFusionEntryDecision(
                null,
                next,
                if (frame?.strongBuy == true) {
                    "ENTRY_ARMED_STRONG ${streak}/${FusionStabilityPolicy.ENTRY_CONFIRMATIONS}: сильный BUY и капитал есть; ещё ${left}с; ${acceptance.reason}"
                } else {
                    "ENTRY_ARMED ${streak}/${FusionStabilityPolicy.ENTRY_CONFIRMATIONS}: BUY и капитал подтверждаются; ещё ${left}с; ${acceptance.reason}"
                }
            )
        }
    }
}
