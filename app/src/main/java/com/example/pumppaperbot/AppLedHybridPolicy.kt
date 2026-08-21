package com.example.pumppaperbot

import kotlin.math.abs
import kotlin.math.roundToInt

data class AppLedEntryEvidence(
    val aiFresh: Boolean,
    val aiAction: String,
    val aiDirection: Int,
    val aiConfidence: Int,
    val aiReadiness: Int,
    val appReadiness: Int,
    val appBuySignal: Boolean,
    val appSellSignal: Boolean,
    val hardVeto: Boolean,
    val microFresh: Boolean,
    val pumpBuyerPercent60s: Double,
    val pumpChange60sPercent: Double,
    val bitcoinBuyerPercent60s: Double,
    val bitcoinChange60sPercent: Double,
    val breathing5m: Int?,
    val breathing15m: Int?,
    val breathing30m: Int?,
    val breathing60m: Int?
)

data class AppLedEntryDecision(
    val level: Int,
    val tradeAction: String,
    val reason: String,
    val appLed: Boolean,
    val reboundConfirmed: Boolean,
    val structuralWeakness: Boolean,
    val appConfirmedEntry: Boolean,
    val independentDeepSeekSetup: Boolean
)

data class AppLedExitEvidence(
    val modelRequestsExit: Boolean,
    val appExitSignal: Boolean,
    val rapidDropUnrecovered: Boolean,
    val currentReturnPercent: Double,
    val positionAgeMillis: Long,
    val microFresh: Boolean,
    val pumpBuyerPercent15s: Double,
    val pumpBuyerPercent60s: Double,
    val pumpBuyerPercent5m: Double,
    val pumpChange60sPercent: Double,
    val breathing5m: Int?,
    val breathing15m: Int?,
    val breathing30m: Int?,
    val breathing60m: Int?
)

data class AppLedExitDecision(
    val allowExit: Boolean,
    val dangerCap: Int,
    val reason: String,
    val emergency: Boolean = false,
    val appConfirmedExit: Boolean = false,
    val independentDeepSeekSetup: Boolean = false
)

data class DeepSeekPersistenceDecision(
    val entryStreak: Int,
    val exitStreak: Int,
    val lastEvaluationAt: Long,
    val confirmIndependentBuy: Boolean,
    val confirmIndependentExit: Boolean
)

/**
 * V5.10 keeps APP as context but restores an actually executable independent DeepSig lane.
 * A strong locally confirmed DeepSig setup may trade after one AI evaluation; the separate
 * trade-verification AI call is still mandatory before the virtual order is applied.
 */
object AppLedHybridPolicy {
    const val MIN_ORDINARY_HOLD_MILLIS = 20L * 60L * 1000L
    private const val HARD_STOP_PERCENT = -5.0
    private const val PROFIT_FADE_RETURN_PERCENT = 1.0

    fun entry(evidence: AppLedEntryEvidence): AppLedEntryDecision {
        if (evidence.hardVeto) {
            return AppLedEntryDecision(
                level = 1,
                tradeAction = "WATCH",
                reason = "Защитный запрет: резкое падение ещё не подтвердило восстановление.",
                appLed = true,
                reboundConfirmed = false,
                structuralWeakness = true,
                appConfirmedEntry = false,
                independentDeepSeekSetup = false
            )
        }

        val five = evidence.breathing5m ?: 0
        val fifteen = evidence.breathing15m ?: 0
        val thirty = evidence.breathing30m ?: 0
        val sixty = evidence.breathing60m ?: 0
        val microBuyers = evidence.microFresh &&
            evidence.pumpBuyerPercent60s >= 54.0 && evidence.pumpChange60sPercent >= 0.03
        val sustainedTurn = five >= 15 && fifteen >= 5
        val strongTurn = five >= 25 && fifteen >= 15
        val localEntryConfirmed = strongTurn || (sustainedTurn && microBuyers)
        val reboundConfirmed = microBuyers && (sustainedTurn || evidence.appReadiness >= 70) || strongTurn
        val structuralWeakness = fifteen <= -20 && thirty <= -15 && sixty <= -10
        val bitcoinSystemicWeak = evidence.microFresh &&
            evidence.bitcoinBuyerPercent60s < 38.0 && evidence.bitcoinChange60sPercent <= -0.25 &&
            structuralWeakness
        val bitcoinSideways = evidence.microFresh &&
            abs(evidence.bitcoinChange60sPercent) <= 0.10 &&
            evidence.bitcoinBuyerPercent60s in 44.0..56.0

        // V5.9 defined this normalizer and tested it, but the live entry path never used it.
        // V5.10 wires it into the real gate: a 9/10 WATCH/HOLD with strong direction,
        // confidence and confirmed 5/15-minute flow becomes a genuine BUY candidate.
        val effectiveAiAction = if (evidence.aiFresh) {
            DeepSeekTradeIntentPolicy.normalize(
                modelAction = evidence.aiAction,
                positionOpen = false,
                entryReadiness = evidence.aiReadiness,
                direction = evidence.aiDirection,
                confidence = evidence.aiConfidence,
                hardVeto = evidence.hardVeto,
                locallyConfirmed = localEntryConfirmed
            )
        } else {
            evidence.aiAction
        }

        val appLevel = (evidence.appReadiness.coerceIn(0, 100) / 10.0).roundToInt()
        val flowLevel = when {
            strongTurn -> 8
            reboundConfirmed -> 7
            five >= 10 || microBuyers -> 6
            else -> 1
        }
        var level = maxOf(appLevel, flowLevel).coerceIn(1, 10)

        // DeepSeek adjusts the stable APP baseline instead of replacing it.
        val aiAdjustment = when {
            !evidence.aiFresh -> 0
            effectiveAiAction == "BUY" && evidence.aiReadiness >= 8 &&
                evidence.aiDirection >= 55 && evidence.aiConfidence >= 60 -> 2
            evidence.aiDirection >= 25 && evidence.aiConfidence >= 55 -> 1
            evidence.aiReadiness <= 3 && evidence.aiDirection <= -60 && evidence.aiConfidence >= 70 -> -1
            else -> 0
        }
        level = (level + aiAdjustment).coerceIn(1, 10)

        if (evidence.appReadiness >= 70 && reboundConfirmed) level = maxOf(level, 7)
        if (bitcoinSideways && reboundConfirmed && !structuralWeakness) level = maxOf(level, 7)
        if (bitcoinSystemicWeak) level = minOf(level, 5)
        if (structuralWeakness && !reboundConfirmed) level = minOf(level, 4)

        val appConfirmedEntry = (evidence.appBuySignal || evidence.appReadiness >= 99) &&
            reboundConfirmed && !bitcoinSystemicWeak
        // Keep this threshold aligned with GeminiExecutionPolicy.MIN_BUY_CONFIDENCE (65),
        // otherwise a candidate could pass the DeepSig gate and still be silently downgraded
        // to HOLD at the final paper-execution layer.
        val independentDeepSeekSetup = evidence.aiFresh && effectiveAiAction == "BUY" &&
            evidence.aiReadiness >= 8 && evidence.aiDirection >= 55 && evidence.aiConfidence >= 65 &&
            localEntryConfirmed && !structuralWeakness && !bitcoinSystemicWeak && !evidence.appSellSignal
        // APP may open immediately when its own stable phase is confirmed. DeepSig's
        // independent lane is now executable after this strong setup plus the existing
        // separate trade-verification call.
        val tradeAction = if (appConfirmedEntry) "BUY" else "WATCH"
        if (independentDeepSeekSetup) level = maxOf(level, 8)
        if (tradeAction == "BUY") {
            val highestConfirmation = appConfirmedEntry && strongTurn
            level = maxOf(level, if (highestConfirmation) 10 else 9)
        }

        val reason = when {
            tradeAction == "BUY" && appConfirmedEntry ->
                "APP подтвердил вход, а поток покупателей и движение 5–15 минут согласуются."
            independentDeepSeekSetup && effectiveAiAction != evidence.aiAction ->
                "DeepSig дал высокий вход 9/10, а 5–15 минут подтвердили разворот; осторожный WATCH повышен до проверки BUY."
            independentDeepSeekSetup ->
                "DeepSig самостоятельно подтвердил устойчивый вход, а 5–15 минут поддерживают движение; запускается проверка BUY."
            structuralWeakness && !reboundConfirmed ->
                "Средний фон ещё слабый; одного зелёного микродвижения недостаточно."
            evidence.appReadiness >= 70 && reboundConfirmed ->
                "APP близок к входу, а покупатели подтверждают восстановление; вход приближается."
            bitcoinSideways && reboundConfirmed ->
                "Bitcoin в боковике, а PUMP набирает собственный покупательский импульс."
            else -> "APP ещё не подтвердил фазу входа; DeepSig продолжает наблюдение."
        }
        return AppLedEntryDecision(
            level = level,
            tradeAction = tradeAction,
            reason = reason,
            appLed = appLevel >= flowLevel || evidence.appBuySignal,
            reboundConfirmed = reboundConfirmed,
            structuralWeakness = structuralWeakness,
            appConfirmedEntry = appConfirmedEntry,
            independentDeepSeekSetup = independentDeepSeekSetup
        )
    }

    fun exit(evidence: AppLedExitEvidence): AppLedExitDecision {
        val five = evidence.breathing5m ?: 0
        val fifteen = evidence.breathing15m ?: 0
        val thirty = evidence.breathing30m ?: 0
        val sixty = evidence.breathing60m ?: 0
        val freshSelling = evidence.microFresh && (
            (evidence.pumpBuyerPercent60s < 46.0 && evidence.pumpChange60sPercent <= -0.10) ||
                (evidence.pumpBuyerPercent5m < 47.0 && evidence.pumpChange60sPercent <= -0.05)
            )
        val strongRecovery = evidence.microFresh &&
            evidence.pumpBuyerPercent15s >= 60.0 &&
            evidence.pumpBuyerPercent60s >= 58.0 &&
            evidence.pumpBuyerPercent5m >= 54.0 &&
            evidence.pumpChange60sPercent >= 0.08
        val structuralWeakness = fifteen <= -20 && thirty <= -15 &&
            (five <= -15 || sixty <= -10)
        // Profit-protection lane: once a trade is already meaningfully positive, do not wait
        // for slow 30/60-minute layers to become deeply negative before accepting a model EXIT.
        // The model still has to request EXIT and the second verifier still has to approve it.
        val profitableFade = evidence.currentReturnPercent >= PROFIT_FADE_RETURN_PERCENT &&
            freshSelling && five <= -10 && fifteen <= -5
        val emergency = evidence.currentReturnPercent <= HARD_STOP_PERCENT ||
            (evidence.rapidDropUnrecovered && freshSelling && five <= -15)
        if (emergency) {
            return AppLedExitDecision(
                true, 10,
                "Аварийная защита подтверждена ценой и устойчивым давлением продавцов.",
                emergency = true,
                independentDeepSeekSetup = evidence.modelRequestsExit
            )
        }
        if (strongRecovery) {
            return AppLedExitDecision(
                false, 5,
                "Выход отклонён: цена и покупатели подтверждают свежий отскок на 15 секундах, 60 секундах и 5 минутах."
            )
        }
        if (evidence.positionAgeMillis in 0 until MIN_ORDINARY_HOLD_MILLIS) {
            return AppLedExitDecision(
                false, 6,
                "Обычный выход отложен: позиция слишком новая, а аварийного разворота нет."
            )
        }
        if (evidence.appExitSignal && (freshSelling || structuralWeakness)) {
            return AppLedExitDecision(
                true, 9,
                "APP подтвердил фазу выхода, и её поддерживает свежий поток либо слабость 15–60 минут.",
                appConfirmedExit = true
            )
        }
        if (evidence.modelRequestsExit && freshSelling && (structuralWeakness || profitableFade)) {
            return AppLedExitDecision(
                true,
                if (profitableFade) 8 else 9,
                if (profitableFade) {
                    "DeepSig защищает уже полученную прибыль: 5–15 минут и свежие продажи подтвердили выдыхание раньше медленных 30–60 минут."
                } else {
                    "DeepSig самостоятельно видит устойчивую слабость 15–60 минут; запускается проверка EXIT."
                },
                independentDeepSeekSetup = true
            )
        }
        return AppLedExitDecision(
            false, 6,
            "DeepSig видит риск, но устойчивый разворот или подтверждённое выдыхание прибыли ещё не сформированы."
        )
    }
}

/**
 * V5.10 removes the redundant second persistence cycle for strong independent DeepSig setups.
 * The independent verification model remains the final gate, so one model response alone still
 * cannot execute a virtual BUY/EXIT.
 */
object DeepSeekPersistencePolicy {
    private const val MIN_CONFIRMATION_GAP_MILLIS = 60_000L
    const val REQUIRED_CONFIRMATIONS = 1

    fun update(
        previousEntryStreak: Int,
        previousExitStreak: Int,
        previousEvaluationAt: Long,
        independentEntrySetup: Boolean,
        independentExitSetup: Boolean,
        now: Long
    ): DeepSeekPersistenceDecision {
        val separateCycle = previousEvaluationAt <= 0L ||
            now - previousEvaluationAt >= MIN_CONFIRMATION_GAP_MILLIS
        val entryStreak = when {
            !independentEntrySetup -> 0
            !separateCycle -> previousEntryStreak
            else -> previousEntryStreak + 1
        }.coerceAtMost(REQUIRED_CONFIRMATIONS)
        val exitStreak = when {
            !independentExitSetup -> 0
            !separateCycle -> previousExitStreak
            else -> previousExitStreak + 1
        }.coerceAtMost(REQUIRED_CONFIRMATIONS)
        return DeepSeekPersistenceDecision(
            entryStreak = entryStreak,
            exitStreak = exitStreak,
            lastEvaluationAt = if (separateCycle) now else previousEvaluationAt,
            confirmIndependentBuy = independentEntrySetup && entryStreak >= REQUIRED_CONFIRMATIONS,
            confirmIndependentExit = independentExitSetup && exitStreak >= REQUIRED_CONFIRMATIONS
        )
    }
}
