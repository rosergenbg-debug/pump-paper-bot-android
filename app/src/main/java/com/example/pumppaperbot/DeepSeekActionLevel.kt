package com.example.pumppaperbot

import android.content.Context

enum class DeepSeekActionPhase { ENTRY, EXIT }

enum class DeepSeekActionBand { RED, YELLOW, GREEN }

data class DeepSeekActionLevel(
    val phase: DeepSeekActionPhase,
    val level: Int,
    val band: DeepSeekActionBand,
    val label: String,
    val detail: String,
    val intensive: Boolean = false,
    val proPreferred: Boolean = false
)

data class DeepSeekEntryLevelEvidence(
    val freshAi: Boolean,
    val aiAction: String,
    val aiDirection: Int,
    val aiConfidence: Int,
    val aiEntryReadiness: Int,
    val appReadiness: Int,
    val hardVeto: Boolean,
    val microFresh: Boolean,
    val microPhase: String,
    val pumpBuyerPercent60s: Double,
    val pumpChange60sPercent: Double,
    val bitcoinBuyerPercent60s: Double,
    val bitcoinChange60sPercent: Double,
    val breathing5m: Int? = null,
    val breathing15m: Int? = null,
    val breathing30m: Int? = null,
    val breathing60m: Int? = null,
    val appBuySignal: Boolean = false,
    val appSellSignal: Boolean = false
)

data class DeepSeekExitLevelEvidence(
    val deepSeekDanger: Int,
    val exitAdvised: Boolean,
    val localSellSignal: Boolean,
    val rapidDrop: Boolean,
    val localGuardCritical: Boolean,
    val directionScore: Int,
    val microFresh: Boolean,
    val pumpBuyerPercent60s: Double,
    val pumpChange60sPercent: Double
)

/**
 * A display and scheduling layer only. It combines already available evidence but never executes a trade.
 */
object DeepSeekActionLevelPolicy {
    const val INTENSIVE_INTERVAL_MILLIS = 2L * 60L * 1000L
    const val APPROACHING_LEVEL = 7
    const val READY_LEVEL = 9

    fun entry(evidence: DeepSeekEntryLevelEvidence): DeepSeekActionLevel {
        val microPressure = evidence.microFresh && evidence.microPhase in setOf(
            "PRESSURE", "IGNITION", "CONFIRMATION"
        )
        val fused = AppLedHybridPolicy.entry(AppLedEntryEvidence(
            aiFresh = evidence.freshAi,
            aiAction = evidence.aiAction,
            aiDirection = evidence.aiDirection,
            aiConfidence = evidence.aiConfidence,
            aiReadiness = evidence.aiEntryReadiness,
            appReadiness = evidence.appReadiness,
            appBuySignal = evidence.appBuySignal,
            appSellSignal = evidence.appSellSignal,
            hardVeto = evidence.hardVeto,
            microFresh = evidence.microFresh,
            pumpBuyerPercent60s = evidence.pumpBuyerPercent60s,
            pumpChange60sPercent = evidence.pumpChange60sPercent,
            bitcoinBuyerPercent60s = evidence.bitcoinBuyerPercent60s,
            bitcoinChange60sPercent = evidence.bitcoinChange60sPercent,
            breathing5m = evidence.breathing5m,
            breathing15m = evidence.breathing15m,
            breathing30m = evidence.breathing30m,
            breathing60m = evidence.breathing60m
        ))
        return entryResult(
            fused.level,
            fused.reason,
            intensive = fused.level >= APPROACHING_LEVEL || microPressure || evidence.appReadiness >= 70,
            proPreferred = fused.level >= READY_LEVEL && evidence.aiAction.uppercase() == "BUY"
        )
    }

    fun exit(evidence: DeepSeekExitLevelEvidence): DeepSeekActionLevel {
        val sellerTakeover = evidence.microFresh && (
            (evidence.pumpBuyerPercent60s < 46.0 && evidence.pumpChange60sPercent <= -0.12) ||
                evidence.pumpChange60sPercent <= -0.30
            )
        var level = evidence.deepSeekDanger.coerceIn(1, 10)
        if (sellerTakeover && evidence.directionScore <= -25) level = maxOf(level, 7)
        if (evidence.localSellSignal) level = maxOf(level, 6)
        if (evidence.rapidDrop && sellerTakeover) level = maxOf(level, 8)
        if (evidence.exitAdvised) level = maxOf(level, 9)
        if (evidence.localGuardCritical) level = 10
        val band = when (level) {
            in 1..4 -> DeepSeekActionBand.GREEN
            in 5..7 -> DeepSeekActionBand.YELLOW
            else -> DeepSeekActionBand.RED
        }
        val label = when (band) {
            DeepSeekActionBand.GREEN -> "ДЕРЖИМ"
            DeepSeekActionBand.YELLOW -> "ГОТОВИМСЯ К ВЫХОДУ"
            DeepSeekActionBand.RED -> "ПРОВЕРИТЬ ВЫХОД"
        }
        val detail = when (band) {
            DeepSeekActionBand.GREEN -> "Согласованного разворота пока нет; наблюдение продолжается."
            DeepSeekActionBand.YELLOW -> "Риск растёт, но выход ещё не подтверждён несколькими группами данных."
            DeepSeekActionBand.RED -> "Выход подтверждён DeepSeek или аварийной локальной защитой; проверьте продажу."
        }
        return DeepSeekActionLevel(
            DeepSeekActionPhase.EXIT, level, band, label, detail,
            intensive = level >= 5, proPreferred = level >= 8
        )
    }

    fun fromMarket(
        snapshot: LiveSnapshot,
        state: DeepSeekPrimaryState,
        micro: MicroImpulseSnapshot,
        now: Long = System.currentTimeMillis(),
        breathing: LiveMarketBreathingSnapshot = LiveMarketBreathingSnapshot()
    ): DeepSeekActionLevel {
        val microFresh = micro.connected && DeepSeekFreshMarketContext.isFresh(
            micro.updatedAt, now, DeepSeekFreshMarketContext.MICRO_MAX_AGE
        )
        fun horizon(minutes: Int) = breathing.horizons.firstOrNull { it.minutes == minutes }?.score
        val shortFallback = state.lastBreathingScore.takeIf { state.lastSuccess > 0L }
        return entry(DeepSeekEntryLevelEvidence(
            freshAi = DeepSeekPrimaryPolicy.isFreshSignal(state, now),
            aiAction = state.modelIntent,
            aiDirection = state.direction,
            aiConfidence = state.confidence,
            aiEntryReadiness = state.entryReadiness,
            appReadiness = snapshot.readinessScore.coerceAtLeast(0),
            appBuySignal = snapshot.buySignal,
            appSellSignal = snapshot.sellSignal,
            // The 30-minute late/overheat flags belong to APP. They remain visible context,
            // but must not freeze the independent intrabar DeepSeek circuit.
            hardVeto = snapshot.rapidDrop.active && !snapshot.rapidDrop.recoveryConfirmed,
            microFresh = microFresh,
            microPhase = micro.phase,
            pumpBuyerPercent60s = micro.aggressiveBuyPercent60s,
            pumpChange60sPercent = micro.priceChange60sPercent,
            bitcoinBuyerPercent60s = micro.bitcoinAggressiveBuyPercent60s,
            bitcoinChange60sPercent = micro.bitcoinPriceChange60sPercent,
            breathing5m = horizon(5) ?: shortFallback,
            breathing15m = horizon(15) ?: shortFallback,
            breathing30m = horizon(30),
            breathing60m = horizon(60)
        ))
    }

    fun fromPosition(
        snapshot: LiveSnapshot,
        state: PositionSupervisionState,
        guard: PersonalPositionGuardState,
        micro: MicroImpulseSnapshot,
        now: Long = System.currentTimeMillis()
    ): DeepSeekActionLevel {
        val microFresh = micro.connected && DeepSeekFreshMarketContext.isFresh(
            micro.updatedAt, now, DeepSeekFreshMarketContext.MICRO_MAX_AGE
        )
        return exit(DeepSeekExitLevelEvidence(
            deepSeekDanger = state.dangerLevel,
            exitAdvised = state.exitAdvised,
            localSellSignal = snapshot.sellSignal,
            rapidDrop = snapshot.rapidDrop.active,
            localGuardCritical = guard.criticalActive,
            directionScore = snapshot.directionScore,
            microFresh = microFresh,
            pumpBuyerPercent60s = micro.aggressiveBuyPercent60s,
            pumpChange60sPercent = micro.priceChange60sPercent
        ))
    }

    private fun entryResult(
        level: Int,
        detail: String,
        intensive: Boolean,
        proPreferred: Boolean
    ): DeepSeekActionLevel {
        val safe = level.coerceIn(1, 10)
        val band = when (safe) {
            in 1..6 -> DeepSeekActionBand.RED
            in 7..8 -> DeepSeekActionBand.YELLOW
            else -> DeepSeekActionBand.GREEN
        }
        val label = when (band) {
            DeepSeekActionBand.RED -> "НЕ ВХОДИТЬ"
            DeepSeekActionBand.YELLOW -> "ВХОД ПРИБЛИЖАЕТСЯ"
            DeepSeekActionBand.GREEN -> "ПРОВЕРИТЬ ВХОД"
        }
        return DeepSeekActionLevel(
            DeepSeekActionPhase.ENTRY, safe, band, label, detail, intensive, proPreferred
        )
    }
}

internal object DeepSeekActionLevelAlertPolicy {
    const val NONE = 0
    const val FIRST_ENTRY_ALERT_LEVEL = 7

    fun band(level: Int): Int = if (level >= FIRST_ENTRY_ALERT_LEVEL) level.coerceAtMost(10) else NONE

    fun next(previousBand: Int, level: Int): Int {
        val current = band(level)
        return if (current >= FIRST_ENTRY_ALERT_LEVEL && current > previousBand) current else NONE
    }
}

object DeepSeekActionLevelAlertStore {
    private const val PREFS = "deepseek_action_level_alerts_v417"
    private const val KEY_BAND = "entry_band"
    private const val KEY_LAST_ALERT_AT = "last_alert_at_v418"
    private const val RESET_LEVEL = 4
    private const val NEW_SERIES_COOLDOWN_MILLIS = 15L * 60L * 1000L

    @Synchronized
    fun sync(context: Context, level: DeepSeekActionLevel, state: DeepSeekPrimaryState) {
        if (level.phase != DeepSeekActionPhase.ENTRY ||
            PumpBotEngine.snapshot(context).waitMode == "SELL"
        ) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val currentBand = DeepSeekActionLevelAlertPolicy.band(level.level)
        val previousBand = prefs.getInt(KEY_BAND, DeepSeekActionLevelAlertPolicy.NONE)
        if (currentBand == DeepSeekActionLevelAlertPolicy.NONE) {
            // Keep the previous peak through ordinary 6↔7 noise. A new alert series is armed
            // only after a real retreat to 4/10 or lower.
            if (level.level <= RESET_LEVEL) prefs.edit().putInt(KEY_BAND, currentBand).apply()
            return
        }
        val next = DeepSeekActionLevelAlertPolicy.next(previousBand, level.level)
        if (next == DeepSeekActionLevelAlertPolicy.NONE) return
        val now = state.lastSuccess.takeIf { it > 0L } ?: System.currentTimeMillis()
        val lastAlertAt = prefs.getLong(KEY_LAST_ALERT_AT, 0L)
        if (previousBand == DeepSeekActionLevelAlertPolicy.NONE && lastAlertAt > 0L &&
            now >= lastAlertAt && now - lastAlertAt < NEW_SERIES_COOLDOWN_MILLIS
        ) {
            // Remember the recovered level so a 7→8 change remains meaningful, but do not
            // restart the same noisy warning series minutes later.
            prefs.edit().putInt(KEY_BAND, currentBand).apply()
            return
        }
        val delivered = runCatching {
            PumpAlert.showDeepSeekActionLevel(context, level, state)
        }.isSuccess
        if (delivered) prefs.edit()
            .putInt(KEY_BAND, currentBand)
            .putLong(KEY_LAST_ALERT_AT, now)
            .commit()
    }
}

internal object VirtualTradeAlertPolicy {
    @Suppress("UNUSED_PARAMETER")
    fun shouldNotify(action: String, userPositionOpen: Boolean): Boolean = true
}

internal object AlertDeliveryPolicy {
    fun shouldRing(
        preparatoryAllowed: Boolean,
        executedTradeAllowed: Boolean,
        executedTrade: Boolean,
        urgentPersonalExit: Boolean
    ): Boolean = when {
        urgentPersonalExit -> true
        executedTrade -> executedTradeAllowed
        else -> preparatoryAllowed
    }
}
