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
    val bitcoinChange60sPercent: Double
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
    const val PREPARE_LEVEL = 5
    const val READY_LEVEL = 8

    fun entry(evidence: DeepSeekEntryLevelEvidence): DeepSeekActionLevel {
        if (evidence.hardVeto) {
            return DeepSeekActionLevel(
                DeepSeekActionPhase.ENTRY, 1, DeepSeekActionBand.RED,
                "НЕ ВХОДИТЬ", "Защитный запрет: поздний вход, перегрев или падение ещё не подтверждено."
            )
        }

        val microPressure = evidence.microFresh && evidence.microPhase in setOf(
            "PRESSURE", "IGNITION", "CONFIRMATION"
        )
        val microStrong = microPressure && evidence.pumpBuyerPercent60s >= 55.0 &&
            evidence.pumpChange60sPercent >= 0.03
        val appStrong = evidence.appReadiness >= 70
        val bitcoinWeak = evidence.microFresh && (
            evidence.bitcoinBuyerPercent60s < 43.0 || evidence.bitcoinChange60sPercent <= -0.18
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
        val localWeight = if (evidence.microFresh) 0.15 else 0.0
        val raw = (
            aiLevel * 0.35 + directionLevel * 0.20 + confidenceLevel * 0.15 +
                appLevel * (0.15 + (0.15 - localWeight)) + microLevel * localWeight
            ).roundToInt().coerceIn(1, 10)

        val independentlyConfirmed = aiStrong && (appStrong || microStrong) && !bitcoinWeak
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
            level >= PREPARE_LEVEL ->
                "Подготовьтесь: условия улучшаются, но полного подтверждения входа ещё нет."
            else -> "DeepSeek пока не видит достаточно согласованных оснований для входа."
        }
        return entryResult(
            level,
            detail,
            intensive = level >= PREPARE_LEVEL || microPressure || appStrong,
            proPreferred = level >= READY_LEVEL
        )
    }

    fun exit(evidence: DeepSeekExitLevelEvidence): DeepSeekActionLevel {
        val sellerTakeover = evidence.microFresh && (
            evidence.pumpBuyerPercent60s < 46.0 || evidence.pumpChange60sPercent <= -0.18
        )
        var level = evidence.deepSeekDanger.coerceIn(1, 10)
        if (sellerTakeover && evidence.directionScore <= -25) level = maxOf(level, 7)
        if (evidence.exitAdvised || evidence.localSellSignal || evidence.rapidDrop || evidence.localGuardCritical) {
            level = maxOf(level, 9)
        }
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
            DeepSeekActionBand.YELLOW -> "Риск растёт: следите за стаканом, исполненными продажами и Bitcoin."
            DeepSeekActionBand.RED -> "DeepSeek или локальная защита видят серьёзную опасность; проверьте продажу сейчас."
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
        now: Long = System.currentTimeMillis()
    ): DeepSeekActionLevel {
        val microFresh = micro.connected && DeepSeekFreshMarketContext.isFresh(
            micro.updatedAt, now, DeepSeekFreshMarketContext.MICRO_MAX_AGE
        )
        return entry(DeepSeekEntryLevelEvidence(
            freshAi = DeepSeekPrimaryPolicy.isFreshSignal(state, now),
            aiAction = state.action,
            aiDirection = state.direction,
            aiConfidence = state.confidence,
            aiEntryReadiness = state.entryReadiness,
            appReadiness = snapshot.readinessScore.coerceAtLeast(0),
            hardVeto = snapshot.lateEntryBlocked || snapshot.marketGateActive ||
                (snapshot.rapidDrop.active && !snapshot.rapidDrop.recoveryConfirmed),
            microFresh = microFresh,
            microPhase = micro.phase,
            pumpBuyerPercent60s = micro.aggressiveBuyPercent60s,
            pumpChange60sPercent = micro.priceChange60sPercent,
            bitcoinBuyerPercent60s = micro.bitcoinAggressiveBuyPercent60s,
            bitcoinChange60sPercent = micro.bitcoinPriceChange60sPercent
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
            localGuardCritical = guard.forceCriticalAi,
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
            in 1..4 -> DeepSeekActionBand.RED
            in 5..7 -> DeepSeekActionBand.YELLOW
            else -> DeepSeekActionBand.GREEN
        }
        val label = when (band) {
            DeepSeekActionBand.RED -> "НЕ ВХОДИТЬ"
            DeepSeekActionBand.YELLOW -> "ПОДГОТОВИТЬСЯ"
            DeepSeekActionBand.GREEN -> "ПРОВЕРИТЬ ВХОД"
        }
        return DeepSeekActionLevel(
            DeepSeekActionPhase.ENTRY, safe, band, label, detail, intensive, proPreferred
        )
    }
}

internal object DeepSeekActionLevelAlertPolicy {
    const val NONE = 0
    const val PREPARE = 1
    const val READY = 2

    fun band(level: Int): Int = when {
        level >= DeepSeekActionLevelPolicy.READY_LEVEL -> READY
        level >= DeepSeekActionLevelPolicy.PREPARE_LEVEL -> PREPARE
        else -> NONE
    }

    fun next(previousBand: Int, level: Int): Int {
        val current = band(level)
        return if (current > previousBand) current else NONE
    }
}

object DeepSeekActionLevelAlertStore {
    private const val PREFS = "deepseek_action_level_alerts_v413"
    private const val KEY_BAND = "entry_band"

    @Synchronized
    fun sync(context: Context, level: DeepSeekActionLevel, state: DeepSeekPrimaryState) {
        if (level.phase != DeepSeekActionPhase.ENTRY ||
            PumpBotEngine.snapshot(context).waitMode == "SELL"
        ) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val currentBand = DeepSeekActionLevelAlertPolicy.band(level.level)
        val previousBand = prefs.getInt(KEY_BAND, DeepSeekActionLevelAlertPolicy.NONE)
        if (currentBand == DeepSeekActionLevelAlertPolicy.NONE) {
            prefs.edit().putInt(KEY_BAND, currentBand).apply()
            return
        }
        val next = DeepSeekActionLevelAlertPolicy.next(previousBand, level.level)
        if (next == DeepSeekActionLevelAlertPolicy.NONE || !AlertSchedule.isAllowedNow(context)) return
        val delivered = runCatching {
            PumpAlert.showDeepSeekActionLevel(context, level, state)
        }.isSuccess
        if (delivered) prefs.edit().putInt(KEY_BAND, currentBand).commit()
    }
}

internal object VirtualTradeAlertPolicy {
    fun shouldNotify(action: String, userPositionOpen: Boolean): Boolean =
        action != "BUY" || !userPositionOpen
}
