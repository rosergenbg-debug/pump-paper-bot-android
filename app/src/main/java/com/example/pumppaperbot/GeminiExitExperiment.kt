package com.example.pumppaperbot

import android.content.Context
import org.json.JSONObject
import java.util.Locale
import kotlin.math.max

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
    val adaptivePullbackPercent: Double = 0.0
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
                lastReason = if (executed != null) "Вход скопирован у контрольного Gemini" else state.lastReason
            ),
            executed
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
                    adaptivePullbackPercent = evidence.adaptivePullbackPercent
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
                    adaptivePullbackPercent = evidence.adaptivePullbackPercent
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
                adaptivePullbackPercent = evidence.adaptivePullbackPercent
            ),
            trade
        )
    }
}

object GeminiExitExperimentStore {
    private const val PREFS = "gemini_exit_experiment_v319"
    private const val KEY_STATE = "state"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun state(context: Context): GeminiExitExperimentState? {
        val raw = prefs(context).getString(KEY_STATE, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
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
                adaptivePullbackPercent = json.optDouble("adaptivePullbackPercent")
            )
        }.getOrNull()
    }

    @Synchronized
    fun evaluate(
        context: Context,
        controlPortfolio: GeminiPaperPortfolio,
        frame: GeminiMarketFrame,
        impulse: ImpulseSnapshot,
        now: Long
    ): GeminiExitExperimentState {
        val initial = GeminiExitExperimentEngine.bootstrap(state(context), controlPortfolio, now)
        val marked = GeminiPaperTrader.markToMarket(initial.portfolio, frame.preRequestPrice)
        val evidence = GeminiExitEvidence.from(marked, frame.preRequestPrice, frame, impulse)
        val result = GeminiExitExperimentEngine.evaluate(
            initial.copy(portfolio = marked),
            evidence,
            frame.preRequestPrice,
            frame.hourId,
            now
        )
        save(context, result.state)
        result.executedTrade?.let {
            runCatching { PumpAlert.showGeminiExitExperimentTrade(context, it) }
        }
        return result.state
    }

    @Synchronized
    fun mirrorControlTrade(context: Context, trade: GeminiPaperTrade): GeminiExitExperimentState {
        val current = state(context) ?: GeminiExitExperimentState(initializedAt = trade.time)
        val result = GeminiExitExperimentEngine.mirrorControlTrade(current, trade)
        save(context, result.state)
        result.executedTrade?.let {
            runCatching { PumpAlert.showGeminiExitExperimentTrade(context, it) }
        }
        return result.state
    }

    @Synchronized
    fun reset(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun save(context: Context, state: GeminiExitExperimentState) {
        val json = JSONObject()
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
        prefs(context).edit().putString(KEY_STATE, json.toString()).apply()
    }
}
