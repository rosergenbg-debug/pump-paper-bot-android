package com.example.pumppaperbot

import android.content.Context
import kotlin.math.roundToInt

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
    val breathing15m: Int? = null
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
    const val INTENSIVE_INTERVAL_MILLIS = 60_000L
    const val APPROACHING_LEVEL = 7
    const val READY_LEVEL = 9

    fun entry(evidence: DeepSeekEntryLevelEvidence): DeepSeekActionLevel {
        if (evidence.hardVeto) {
            return DeepSeekActionLevel(
                DeepSeekActionPhase.ENTRY, 1, DeepSeekActionBand.RED,
                "НЕ ВХОДИТЬ", "Защитный запрет: резкое падение ещё не подтвердило восстановление."
            )
        }

        val microPressure = evidence.microFresh && evidence.microPhase in setOf(
            "PRESSURE", "IGNITION", "CONFIRMATION"
        )
        val microStrong = microPressure && evidence.pumpBuyerPercent60s >= 55.0 &&
            evidence.pumpChange60sPercent >= 0.03
        val appStrong = evidence.appReadiness >= 70
        val sustainedPump = (evidence.breathing5m ?: 0) >= 15 &&
            (evidence.breathing15m ?: 0) >= 10
        val stableStrong = (evidence.breathing5m ?: 0) >= 25 &&
            (evidence.breathing15m ?: 0) >= 20
        // Bitcoin is a regime/risk filter, not a requirement that PUMP copy every 60-second move.
        // A short BTC dip may be a normal lead/lag divergence when PUMP flow stays independently strong.
        val bitcoinWeak = evidence.microFresh && !sustainedPump && !microStrong && (
            evidence.bitcoinBuyerPercent60s < 40.0 && evidence.bitcoinChange60sPercent <= -0.20
        )
        val aiStrong = evidence.freshAi && evidence.aiDirection >= 35 && evidence.aiConfidence >= 55

        if (!evidence.freshAi) {
            val localLevel = if (microStrong && appStrong && !bitcoinWeak) 4 else if (microPressure) 3 else 1
            return entryResult(
                localLevel,
                "DeepSeek ещё не дал свежую оценку; локальная движуха используется только как повод усилить наблюдение.",
                intensive = microPressure || appStrong,
                proPreferred = false
            )
        }

        val aiLevel = evidence.aiEntryReadiness.coerceIn(1, 10)
        val directionLevel = (evidence.aiDirection.coerceIn(0, 100) / 10.0).roundToInt()
        val confidenceLevel = (evidence.aiConfidence.coerceIn(0, 100) / 10.0).roundToInt()
        val appLevel = (evidence.appReadiness.coerceIn(0, 100) / 10.0).roundToInt()
        val microLevel = if (evidence.microFresh) {
            (((evidence.pumpBuyerPercent60s - 45.0) / 2.0).coerceIn(0.0, 8.0) +
                if (microPressure) 2.0 else 0.0).roundToInt().coerceIn(0, 10)
        } else 0
        val stableLevel = (((evidence.breathing5m ?: 0) + (evidence.breathing15m ?: 0)) / 20.0)
            .roundToInt().coerceIn(0, 10)
        val localWeight = if (evidence.microFresh) 0.10 else 0.0
        val raw = (
            aiLevel * 0.35 + directionLevel * 0.15 + confidenceLevel * 0.15 +
                appLevel * 0.10 + microLevel * localWeight + stableLevel * 0.15
            ).roundToInt().coerceIn(1, 10)

        val independentlyConfirmed = aiStrong && (appStrong || microStrong || stableStrong) && !bitcoinWeak
        var level = raw
        level = when {
            evidence.aiDirection <= 0 -> minOf(level, 3)
            evidence.aiConfidence < 55 -> minOf(level, 5)
            bitcoinWeak -> minOf(level, 5)
            !independentlyConfirmed -> minOf(level, 7)
            else -> level
        }
        if (evidence.aiAction == "BUY" && independentlyConfirmed) {
            level = maxOf(level, if (evidence.aiDirection >= 70 && evidence.aiConfidence >= 70) 10 else 9)
        } else if (independentlyConfirmed) {
            level = maxOf(level, 6)
        }

        val detail = when {
            level >= READY_LEVEL && evidence.aiAction == "BUY" ->
                "DeepSeek и независимые рыночные данные подтвердили вход; проверьте цену перед решением."
            level >= READY_LEVEL ->
                "Вход близко: DeepSeek, локальный алгоритм и поток покупателей согласуются."
            level >= APPROACHING_LEVEL ->
                "Вход приближается: устойчивость 5–15 минут улучшается, но подтверждение ещё не полное."
            else -> "DeepSeek пока не видит достаточно согласованных оснований для входа."
        }
        return entryResult(
            level,
            detail,
            intensive = level >= APPROACHING_LEVEL || microPressure || appStrong,
            proPreferred = level >= READY_LEVEL
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
            ?: state.lastBreathingScore.takeIf { state.lastSuccess > 0L }
        return entry(DeepSeekEntryLevelEvidence(
            freshAi = DeepSeekPrimaryPolicy.isFreshSignal(state, now),
            aiAction = state.action,
            aiDirection = state.direction,
            aiConfidence = state.confidence,
            aiEntryReadiness = state.entryReadiness,
            appReadiness = snapshot.readinessScore.coerceAtLeast(0),
            // The 30-minute late/overheat flags belong to APP. They remain visible context,
            // but must not freeze the independent intrabar DeepSeek circuit.
            hardVeto = snapshot.rapidDrop.active && !snapshot.rapidDrop.recoveryConfirmed,
            microFresh = microFresh,
            microPhase = micro.phase,
            pumpBuyerPercent60s = micro.aggressiveBuyPercent60s,
            pumpChange60sPercent = micro.priceChange60sPercent,
            bitcoinBuyerPercent60s = micro.bitcoinAggressiveBuyPercent60s,
            bitcoinChange60sPercent = micro.bitcoinPriceChange60sPercent,
            breathing5m = horizon(5),
            breathing15m = horizon(15)
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
    fun shouldNotify(action: String, userPositionOpen: Boolean): Boolean =
        action != "BUY" || !userPositionOpen
}

internal object AlertDeliveryPolicy {
    fun shouldRing(withinSchedule: Boolean, urgentPersonalExit: Boolean): Boolean =
        withinSchedule || urgentPersonalExit
}
