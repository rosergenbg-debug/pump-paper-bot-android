package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.max

data class GeminiEntryEvidence(
    val signalActive: Boolean,
    val eligible: Boolean,
    val score: Int,
    val groups: Int,
    val signalSource: String,
    val reason: String,
    val blockedReason: String = "",
    val anchorId: Long = 0L
) {
    companion object {
        fun from(
            frame: GeminiMarketFrame,
            impulse: ImpulseSnapshot,
            controlDecision: GeminiHourlyDecision?,
            appEvaluation: AppPaperEvaluation
        ): GeminiEntryEvidence {
            val snapshot = frame.snapshot
            val impulseFresh = impulse.candleTime > 0L &&
                snapshot.lastSync - impulse.candleTime in 0L..20L * 60L * 1000L
            val appReady = GeminiAppReadinessPolicy.isReady(appEvaluation)
            val geminiPositive = controlDecision != null &&
                controlDecision.requestedAction != "SELL" &&
                controlDecision.directionScore >= 20 &&
                controlDecision.confidence >= 55
            val signalActive = appReady || geminiPositive

            val spotStrong = listOfNotNull(
                frame.spotTakerBuyPercent?.let { it >= 51.0 },
                impulse.spotTakerRatio?.takeIf { impulseFresh }?.let { it >= 0.51 }
            ).any { it }
            val futuresStrong = listOfNotNull(
                frame.futuresTakerBuyPercent?.let { it >= 51.0 },
                impulse.futuresTakerRatio?.takeIf { impulseFresh }?.let { it >= 0.51 }
            ).any { it }
            val flowStrong = spotStrong && futuresStrong
            val cvdStrong = frame.spotCvdPercent?.let { it > 0.0 } == true &&
                frame.futuresCvdPercent?.let { it > 0.0 } == true
            val momentumStrong = frame.pump1hPercent?.let { it >= 0.15 } == true &&
                frame.pump3hPercent?.let { it >= -0.50 } != false
            val bookStrong = snapshot.bookImbalance?.let { it >= 0.05 } == true
            val broadMarketWeak = frame.btc1hPercent?.let { it < 0.0 } == true &&
                frame.sol1hPercent?.let { it < 0.0 } == true
            val broadMarketSupport = !broadMarketWeak

            val score =
                (if (signalActive) 2 else 0) +
                    (if (flowStrong) 2 else if (spotStrong || futuresStrong) 1 else 0) +
                    (if (cvdStrong) 2 else 0) +
                    (if (momentumStrong) 1 else 0) +
                    (if (bookStrong) 1 else 0) +
                    (if (broadMarketSupport) 1 else 0)
            val groups = listOf(
                signalActive,
                spotStrong || futuresStrong,
                cvdStrong,
                momentumStrong,
                bookStrong,
                broadMarketSupport
            ).count { it }
            val blocked = when {
                snapshot.lateEntryBlocked -> "цена уже высоко — поздний вход запрещён"
                snapshot.marketGateActive -> "PUMP, BTC и SOL перегреты — цену не догоняем"
                snapshot.rapidDrop.active && !snapshot.rapidDrop.recoveryConfirmed ->
                    "резкое падение ещё не подтвердило разворот"
                broadMarketWeak -> "BTC и SOL одновременно снижаются"
                !momentumStrong -> "рост PUMP ещё не подтверждён закрытым движением"
                !flowStrong && !cvdStrong -> "нет согласованного подтверждения покупателей или CVD"
                score < 5 || groups < 3 -> "недостаточно независимых подтверждений входа"
                else -> ""
            }
            val source = buildList {
                if (appReady) add("APP ${appEvaluation.readinessScore}/100")
                if (geminiPositive) add(
                    "Gemini ${controlDecision?.directionScore}/100, уверенность ${controlDecision?.confidence}/100"
                )
            }.joinToString(" + ").ifBlank { "сигнала входа нет" }
            val facts = buildList {
                if (flowStrong) add("покупатели сильнее одновременно в spot и futures")
                else if (spotStrong) add("усилился spot‑поток покупателей")
                else if (futuresStrong) add("усилился futures‑поток покупателей")
                if (cvdStrong) add("spot и futures CVD положительные")
                if (momentumStrong) add("PUMP подтвердил движение вверх")
                if (bookStrong) add("в стакане перевес покупателей")
                if (broadMarketSupport) add("BTC/SOL не дают совместного медвежьего запрета")
            }
            val evidenceText = facts.joinToString("; ").ifBlank { "рыночных подтверждений пока нет" }
            val anchorId = max(
                if (appReady) appEvaluation.candleTime else 0L,
                if (geminiPositive) controlDecision?.id ?: 0L else 0L
            )
            return GeminiEntryEvidence(
                signalActive = signalActive,
                eligible = signalActive && blocked.isBlank(),
                score = score,
                groups = groups,
                signalSource = source,
                reason = "$source: $evidenceText",
                blockedReason = blocked,
                anchorId = anchorId
            )
        }
    }
}

internal object GeminiAppReadinessPolicy {
    fun isReady(evaluation: AppPaperEvaluation): Boolean =
        evaluation.candleTime > 0L &&
            evaluation.action != StrategyV2.ACTION_SELL &&
            evaluation.action != StrategyV2.ACTION_SELL_HALF &&
            evaluation.readinessScore >= 99
}

data class GeminiExitEvidence(
    val score: Int,
    val groups: Int,
    val pullbackPercent: Double,
    val adaptivePullbackPercent: Double,
    val currentReturnPercent: Double,
    val spotFlowWeak: Boolean,
    val futuresFlowWeak: Boolean,
    val cvdWeak: Boolean,
    val marketWeak: Boolean,
    val derivativesWeak: Boolean,
    val bookWeak: Boolean,
    val directionWeak: Boolean,
    val priceWeak: Boolean,
    val reason: String
) {
    companion object {
        fun from(
            portfolio: GeminiPaperPortfolio,
            price: Double,
            frame: GeminiMarketFrame,
            impulse: ImpulseSnapshot
        ): GeminiExitEvidence {
            val peak = max(max(portfolio.positionPeakPrice, portfolio.entryPrice), price)
            val pullback = if (peak > 0.0) (1.0 - price / peak) * 100.0 else 0.0
            val currentReturn = if (portfolio.entryPrice > 0.0) {
                (price / portfolio.entryPrice - 1.0) * 100.0
            } else {
                0.0
            }
            val ranges = frame.snapshot.chart.candles.takeLast(24).mapNotNull { candle ->
                candle.close.takeIf { it > 0.0 }?.let {
                    (candle.high - candle.low).coerceAtLeast(0.0) / it * 100.0
                }
            }.sorted()
            val typicalRange = when {
                ranges.isEmpty() -> 0.8
                ranges.size % 2 == 1 -> ranges[ranges.size / 2]
                else -> (ranges[ranges.size / 2 - 1] + ranges[ranges.size / 2]) / 2.0
            }
            val adaptivePullback = (typicalRange * 0.65).coerceIn(0.35, 1.80)
            val impulseFresh = impulse.candleTime > 0L &&
                frame.snapshot.lastSync - impulse.candleTime in 0L..20L * 60L * 1000L

            val spotFlowWeak = listOfNotNull(
                frame.spotTakerBuyPercent?.let { it < 49.0 },
                impulse.spotTakerRatio?.takeIf { impulseFresh }?.let { it < 0.49 }
            ).any { it }
            val futuresFlowWeak = listOfNotNull(
                frame.futuresTakerBuyPercent?.let { it < 49.0 },
                impulse.futuresTakerRatio?.takeIf { impulseFresh }?.let { it < 0.49 }
            ).any { it }
            val cvdWeak = frame.spotCvdPercent?.let { it < 0.0 } == true &&
                frame.futuresCvdPercent?.let { it < 0.0 } == true
            val marketWeak = frame.btc1hPercent?.let { it < 0.0 } == true &&
                frame.sol1hPercent?.let { it < 0.0 } == true
            val derivativesWeak = frame.openInterestChange10mPercent?.let { it < 0.0 } == true &&
                (frame.pump1hPercent?.let { it < 0.0 } == true ||
                    frame.futuresCvdPercent?.let { it < 0.0 } == true)
            val bookWeak = frame.snapshot.bookImbalance?.let { it < -0.08 } == true
            val directionWeak = frame.snapshot.directionScore <= -20
            val priceWeak = pullback >= adaptivePullback

            val weighted =
                (if (priceWeak) 2 else 0) +
                    (if (spotFlowWeak) 1 else 0) +
                    (if (futuresFlowWeak) 1 else 0) +
                    (if (cvdWeak) 2 else 0) +
                    (if (marketWeak) 1 else 0) +
                    (if (derivativesWeak) 1 else 0) +
                    (if (bookWeak) 1 else 0) +
                    (if (directionWeak) 2 else 0)
            val groupCount = listOf(
                priceWeak,
                spotFlowWeak || futuresFlowWeak,
                cvdWeak,
                marketWeak,
                derivativesWeak,
                bookWeak,
                directionWeak
            ).count { it }
            val facts = buildList {
                if (priceWeak) add(String.format(Locale.GERMANY, "откат %.2f%% при норме шума %.2f%%", pullback, adaptivePullback))
                if (spotFlowWeak && futuresFlowWeak) add("покупатели ослабли одновременно в spot и futures")
                else if (spotFlowWeak) add("ослаб spot-поток покупателей")
                else if (futuresFlowWeak) add("ослаб futures-поток покупателей")
                if (cvdWeak) add("spot и futures CVD развернулись вниз")
                if (marketWeak) add("BTC и SOL одновременно снижаются")
                if (derivativesWeak) add("open interest сокращается вместе со слабостью цены/фьючерсов")
                if (bookWeak) add("в стакане перевес продавцов")
                if (directionWeak) add("рыночное направление отрицательное")
            }
            return GeminiExitEvidence(
                score = weighted,
                groups = groupCount,
                pullbackPercent = pullback,
                adaptivePullbackPercent = adaptivePullback,
                currentReturnPercent = currentReturn,
                spotFlowWeak = spotFlowWeak,
                futuresFlowWeak = futuresFlowWeak,
                cvdWeak = cvdWeak,
                marketWeak = marketWeak,
                derivativesWeak = derivativesWeak,
                bookWeak = bookWeak,
                directionWeak = directionWeak,
                priceWeak = priceWeak,
                reason = if (facts.isEmpty()) "подтверждённых признаков разворота нет" else facts.joinToString("; ")
            )
        }
    }
}

data class GeminiExitExperimentState(
    val initializedAt: Long = 0L,
    val portfolio: GeminiPaperPortfolio = GeminiPaperPortfolio(),
    val lastControlDecisionId: Long = 0L,
    val dangerStreak: Int = 0,
    val lastEvaluationAt: Long = 0L,
    val lastScore: Int = 0,
    val lastGroups: Int = 0,
    val lastReason: String = "Ждём первую проверку рынка",
    val lastSignal: String = "WAIT",
    val adaptivePullbackPercent: Double = 0.0,
    val lastPhase: String = "ENTRY",
    val lastEntryAnchorId: Long = 0L
)

data class GeminiExitEvaluationResult(
    val state: GeminiExitExperimentState,
    val executedTrade: GeminiPaperTrade? = null
)

internal object GeminiExitExperimentEngine {
    private const val MIN_EVALUATION_GAP_MILLIS = 60_000L
    private const val EMERGENCY_LOSS_PERCENT = 5.0

    fun bootstrap(
        state: GeminiExitExperimentState?,
        control: GeminiPaperPortfolio,
        now: Long
    ): GeminiExitExperimentState = state ?: GeminiExitExperimentState(
        initializedAt = now,
        portfolio = control,
        lastControlDecisionId = control.trades.maxOfOrNull { it.decisionId } ?: 0L,
        lastEntryAnchorId = control.trades.lastOrNull { it.action == "BUY" }?.decisionId ?: 0L,
        lastReason = "Эксперимент начат с точной копии текущего портфеля Gemini"
    )

    fun mirrorControlTrade(
        state: GeminiExitExperimentState,
        trade: GeminiPaperTrade
    ): GeminiExitEvaluationResult {
        if (trade.decisionId <= state.lastControlDecisionId) return GeminiExitEvaluationResult(state)
        if (trade.action != "BUY" || state.portfolio.inPosition) {
            return GeminiExitEvaluationResult(state.copy(lastControlDecisionId = trade.decisionId))
        }
        val recommendation = GeminiHourlyRecommendation(
            action = "BUY",
            directionScore = trade.score,
            confidence = trade.confidence,
            horizonHours = 1,
            reason = "Тот же вход, что у контрольного Gemini. ${trade.reason}",
            risks = emptyList(),
            model = "gemini-exit-experiment"
        )
        val bought = GeminiPaperTrader.applyDecision(
            current = state.portfolio,
            price = trade.price,
            decisionId = trade.decisionId,
            candleTime = trade.time,
            recommendation = recommendation,
            now = trade.time,
            requestSentAt = trade.time,
            responseReceivedAt = trade.time,
            executionQuoteAt = trade.time
        )
        val executed = if (bought.trades.size > state.portfolio.trades.size) {
            bought.trades.lastOrNull()?.takeIf { it.action == "BUY" }
        } else {
            null
        }
        return GeminiExitEvaluationResult(
            state.copy(
                portfolio = bought,
                lastControlDecisionId = trade.decisionId,
                dangerStreak = 0,
                lastSignal = if (executed != null) "BUY" else state.lastSignal,
                lastReason = if (executed != null) "Вход скопирован у контрольного Gemini" else state.lastReason,
                lastPhase = "ENTRY",
                lastEntryAnchorId = if (executed != null) {
                    max(state.lastEntryAnchorId, trade.decisionId)
                } else {
                    state.lastEntryAnchorId
                }
            ),
            executed
        )
    }

    fun considerEntry(
        state: GeminiExitExperimentState,
        evidence: GeminiEntryEvidence,
        price: Double,
        decisionId: Long,
        now: Long
    ): GeminiExitEvaluationResult {
        val marked = GeminiPaperTrader.markToMarket(state.portfolio, price)
        if (marked.inPosition) return GeminiExitEvaluationResult(state.copy(portfolio = marked))
        val alreadyUsed = evidence.anchorId > 0L && evidence.anchorId <= state.lastEntryAnchorId
        val statusReason = when {
            !evidence.signalActive -> "В евро; ждём подписанный сигнал APP 99/100 или свежий положительный Gemini"
            alreadyUsed -> "РАННИЙ ВХОД НЕ ВЫПОЛНЕН: этот же сигнал уже использовался для входа; ждём новую закрытую свечу или новое решение Gemini. ${evidence.reason}"
            !evidence.eligible -> "РАННИЙ ВХОД НЕ ВЫПОЛНЕН: ${evidence.blockedReason}. ${evidence.reason}"
            else -> "РАННИЙ ВХОД ПОДТВЕРЖДЁН: ${evidence.reason}. Оценка ${evidence.score}/9, групп ${evidence.groups}/6."
        }
        if (!evidence.eligible || alreadyUsed) {
            return GeminiExitEvaluationResult(
                state.copy(
                    portfolio = marked,
                    dangerStreak = 0,
                    lastEvaluationAt = now,
                    lastScore = evidence.score,
                    lastGroups = evidence.groups,
                    lastReason = statusReason,
                    lastSignal = if (evidence.signalActive) "ENTRY_BLOCKED" else "WAIT",
                    lastPhase = "ENTRY"
                )
            )
        }
        val bought = GeminiPaperTrader.applyExperimentalEntry(
            current = marked,
            price = price,
            decisionId = decisionId,
            score = evidence.score,
            reason = statusReason,
            now = now
        )
        val trade = if (bought.trades.size > marked.trades.size) {
            bought.trades.lastOrNull()?.takeIf { it.action == "BUY" }
        } else {
            null
        }
        return GeminiExitEvaluationResult(
            state.copy(
                portfolio = bought,
                dangerStreak = 0,
                lastEvaluationAt = now,
                lastScore = evidence.score,
                lastGroups = evidence.groups,
                lastReason = statusReason,
                lastSignal = if (trade != null) "BUY" else "ENTRY_BLOCKED",
                lastPhase = "ENTRY",
                lastEntryAnchorId = if (trade != null) {
                    max(state.lastEntryAnchorId, evidence.anchorId)
                } else {
                    state.lastEntryAnchorId
                }
            ),
            trade
        )
    }

    fun evaluate(
        state: GeminiExitExperimentState,
        evidence: GeminiExitEvidence,
        price: Double,
        decisionId: Long,
        now: Long
    ): GeminiExitEvaluationResult {
        val marked = GeminiPaperTrader.markToMarket(state.portfolio, price)
        if (!marked.inPosition) {
            return GeminiExitEvaluationResult(
                state.copy(
                    portfolio = marked,
                    dangerStreak = 0,
                    lastEvaluationAt = now,
                    lastScore = evidence.score,
                    lastGroups = evidence.groups,
                    lastReason = "В евро; ждём следующий фактический вход контрольного Gemini",
                    adaptivePullbackPercent = evidence.adaptivePullbackPercent,
                    lastPhase = "ENTRY"
                )
            )
        }
        if (state.lastEvaluationAt > 0L && now - state.lastEvaluationAt < MIN_EVALUATION_GAP_MILLIS) {
            return GeminiExitEvaluationResult(state.copy(portfolio = marked))
        }
        val structuralWeakness = evidence.priceWeak ||
            (evidence.spotFlowWeak && evidence.futuresFlowWeak) || evidence.cvdWeak
        val dangerous = evidence.score >= 4 && evidence.groups >= 3 && structuralWeakness
        val streak = if (dangerous) state.dangerStreak + 1 else 0
        val emergency = evidence.currentReturnPercent <= -EMERGENCY_LOSS_PERCENT
        val immediateReversal = evidence.score >= 7 && evidence.groups >= 4 && structuralWeakness
        val confirmedReversal = streak >= 2
        val shouldExit = emergency || immediateReversal || confirmedReversal
        val prefix = when {
            emergency -> "АВАРИЙНАЯ СТРАХОВКА −5%"
            immediateReversal -> "СИЛЬНЫЙ РАЗВОРОТ РЫНКА"
            confirmedReversal -> "РАЗВОРОТ ПОДТВЕРЖДЁН ДВУМЯ ПРОВЕРКАМИ"
            dangerous -> "ОПАСНОСТЬ 1/2"
            else -> "ПОЗИЦИЯ УДЕРЖИВАЕТСЯ"
        }
        val reason = "$prefix: ${evidence.reason}. Оценка ${evidence.score}, групп ${evidence.groups}."
        if (!shouldExit) {
            return GeminiExitEvaluationResult(
                state.copy(
                    portfolio = marked,
                    dangerStreak = streak,
                    lastEvaluationAt = now,
                    lastScore = evidence.score,
                    lastGroups = evidence.groups,
                    lastReason = reason,
                    lastSignal = if (dangerous) "DANGER" else "HOLD",
                    adaptivePullbackPercent = evidence.adaptivePullbackPercent,
                    lastPhase = "EXIT"
                )
            )
        }
        val sold = GeminiPaperTrader.applyProtectiveExit(
            current = marked,
            price = price,
            decisionId = decisionId,
            reason = reason,
            now = now
        )
        val trade = if (sold.trades.size > marked.trades.size) {
            sold.trades.lastOrNull()?.takeIf { it.action == "SELL" }
        } else {
            null
        }
        return GeminiExitEvaluationResult(
            state.copy(
                portfolio = sold,
                dangerStreak = 0,
                lastEvaluationAt = now,
                lastScore = evidence.score,
                lastGroups = evidence.groups,
                lastReason = reason,
                lastSignal = "SELL",
                adaptivePullbackPercent = evidence.adaptivePullbackPercent,
                lastPhase = "EXIT"
            ),
            trade
        )
    }
}

object GeminiExitExperimentStore {
    private const val PREFS = "gemini_exit_experiment_v319"
    private const val KEY_STATE = "state"
    private const val KEY_STATE_BACKUP = "state_backup_v322"
    private const val KEY_PENDING_ALERTS = "pending_trade_alerts_v322"
    private const val KEY_STORAGE_ERROR = "state_storage_error_v322"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun state(context: Context): GeminiExitExperimentState? {
        val p = prefs(context)
        val raw = p.getString(KEY_STATE, null) ?: return null
        parseState(raw)?.let { return it }
        val recovered = parseState(p.getString(KEY_STATE_BACKUP, null))
        if (recovered != null) {
            p.edit()
                .putString(KEY_STATE, stateToJson(recovered).toString())
                .remove(KEY_STORAGE_ERROR)
                .commit()
            return recovered
        }
        p.edit().putString(
            KEY_STORAGE_ERROR,
            "Повреждены данные Gemini‑эксперимента; торговля остановлена до восстановления или сброса"
        ).commit()
        return null
    }

    private fun parseState(raw: String?): GeminiExitExperimentState? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            JSONObject(json.getString("portfolio"))
            GeminiExitExperimentState(
                initializedAt = json.optLong("initializedAt"),
                portfolio = GeminiPaperStore.loadPortfolio(json.optString("portfolio")),
                lastControlDecisionId = json.optLong("lastControlDecisionId"),
                dangerStreak = json.optInt("dangerStreak"),
                lastEvaluationAt = json.optLong("lastEvaluationAt"),
                lastScore = json.optInt("lastScore"),
                lastGroups = json.optInt("lastGroups"),
                lastReason = json.optString("lastReason", "Ждём первую проверку рынка"),
                lastSignal = json.optString("lastSignal", "WAIT"),
                adaptivePullbackPercent = json.optDouble("adaptivePullbackPercent"),
                lastPhase = json.optString("lastPhase", "ENTRY"),
                lastEntryAnchorId = json.optLong("lastEntryAnchorId")
            )
        }.getOrNull()
    }

    @Synchronized
    fun evaluate(
        context: Context,
        controlPortfolio: GeminiPaperPortfolio,
        controlDecision: GeminiHourlyDecision?,
        frame: GeminiMarketFrame,
        impulse: ImpulseSnapshot,
        appEvaluation: AppPaperEvaluation,
        now: Long
    ): GeminiExitExperimentState {
        flushPendingAlerts(context)
        val existing = state(context)
        check(prefs(context).getString(KEY_STORAGE_ERROR, "").isNullOrBlank()) {
            prefs(context).getString(KEY_STORAGE_ERROR, "Ошибка хранилища Gemini‑эксперимента")
        }
        var initial = GeminiExitExperimentEngine.bootstrap(existing, controlPortfolio, now)
        controlPortfolio.trades.maxByOrNull { it.decisionId }?.let { latest ->
            val mirrored = GeminiExitExperimentEngine.mirrorControlTrade(initial, latest)
            initial = mirrored.state
            if (mirrored.executedTrade != null) {
                save(context, initial, mirrored.executedTrade)
                flushPendingAlerts(context)
                return initial
            }
        }
        val marked = GeminiPaperTrader.markToMarket(initial.portfolio, frame.preRequestPrice)
        val entryEvidence = GeminiEntryEvidence.from(
            frame,
            impulse,
            controlDecision,
            appEvaluation
        )
        val result = if (!marked.inPosition) {
            GeminiExitExperimentEngine.considerEntry(
                initial.copy(portfolio = marked),
                entryEvidence,
                frame.preRequestPrice,
                frame.hourId,
                now
            )
        } else {
            val exitEvidence = GeminiExitEvidence.from(marked, frame.preRequestPrice, frame, impulse)
            GeminiExitExperimentEngine.evaluate(
                initial.copy(portfolio = marked),
                exitEvidence,
                frame.preRequestPrice,
                frame.hourId,
                now
            )
        }
        save(context, result.state, result.executedTrade)
        flushPendingAlerts(context)
        if (result.executedTrade == null && entryEvidence.signalActive && !marked.inPosition) {
            SignalAttributionStore.record(
                context,
                "GEMINI‑ЭКСПЕРИМЕНТ",
                "СИГНАЛ ВХОДА ЗАБЛОКИРОВАН",
                result.state.lastReason,
                now,
                executedTrade = false
            )
        }
        return result.state
    }

    @Synchronized
    fun mirrorControlTrade(context: Context, trade: GeminiPaperTrade): GeminiExitExperimentState {
        val current = state(context) ?: GeminiExitExperimentState(initializedAt = trade.time)
        check(prefs(context).getString(KEY_STORAGE_ERROR, "").isNullOrBlank()) {
            prefs(context).getString(KEY_STORAGE_ERROR, "Ошибка хранилища Gemini‑эксперимента")
        }
        val result = GeminiExitExperimentEngine.mirrorControlTrade(current, trade)
        save(context, result.state, result.executedTrade)
        flushPendingAlerts(context)
        return result.state
    }

    @Synchronized
    fun reset(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun save(
        context: Context,
        state: GeminiExitExperimentState,
        pendingTrade: GeminiPaperTrade? = null
    ) {
        val json = stateToJson(state)
        val pending = (pendingAlerts(context) + listOfNotNull(pendingTrade))
            .distinctBy(::alertId)
        prefs(context).edit()
            .putString(KEY_STATE, json.toString())
            .putString(KEY_STATE_BACKUP, json.toString())
            .putString(KEY_PENDING_ALERTS, JSONArray(pending.map { it.toJson() }).toString())
            .remove(KEY_STORAGE_ERROR)
            .commit()
    }

    private fun stateToJson(state: GeminiExitExperimentState): JSONObject = JSONObject()
            .put("initializedAt", state.initializedAt)
            .put("portfolio", GeminiPaperStore.portfolioToJson(state.portfolio).toString())
            .put("lastControlDecisionId", state.lastControlDecisionId)
            .put("dangerStreak", state.dangerStreak)
            .put("lastEvaluationAt", state.lastEvaluationAt)
            .put("lastScore", state.lastScore)
            .put("lastGroups", state.lastGroups)
            .put("lastReason", state.lastReason)
            .put("lastSignal", state.lastSignal)
            .put("adaptivePullbackPercent", state.adaptivePullbackPercent)
            .put("lastPhase", state.lastPhase)
            .put("lastEntryAnchorId", state.lastEntryAnchorId)

    private fun pendingAlerts(context: Context): List<GeminiPaperTrade> = runCatching {
        val json = JSONArray(prefs(context).getString(KEY_PENDING_ALERTS, "[]").orEmpty())
        (0 until json.length()).mapNotNull { index ->
            json.optJSONObject(index)?.let(GeminiPaperTrade::fromJson)
        }
    }.getOrDefault(emptyList())

    private fun flushPendingAlerts(context: Context) {
        val pending = pendingAlerts(context)
        if (pending.isEmpty()) return
        var delivered = 0
        for (trade in pending) {
            if (runCatching { PumpAlert.showGeminiExitExperimentTrade(context, trade) }.isFailure) break
            delivered++
        }
        if (delivered > 0) {
            prefs(context).edit().putString(
                KEY_PENDING_ALERTS,
                JSONArray(pending.drop(delivered).map { it.toJson() }).toString()
            ).commit()
        }
    }

    private fun alertId(trade: GeminiPaperTrade): String =
        "${trade.time}:${trade.decisionId}:${trade.action}"
}
