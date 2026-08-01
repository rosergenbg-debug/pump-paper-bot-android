package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

data class GeminiPaperTrade(
    val time: Long,
    val decisionId: Long,
    val action: String,
    val price: Double,
    val amount: Double,
    val fee: Double,
    val score: Int,
    val confidence: Int,
    val reason: String,
    val pnlEur: Double = 0.0,
    val methodVersion: Int = 1
) {
    fun toJson(): JSONObject = JSONObject()
        .put("time", time)
        .put("decisionId", decisionId)
        .put("action", action)
        .put("price", price)
        .put("amount", amount)
        .put("fee", fee)
        .put("score", score)
        .put("confidence", confidence)
        .put("reason", reason)
        .put("pnlEur", pnlEur)
        .put("methodVersion", methodVersion)

    companion object {
        fun fromJson(value: JSONObject) = GeminiPaperTrade(
            time = value.optLong("time"),
            decisionId = value.optLong("decisionId"),
            action = value.optString("action"),
            price = value.optDouble("price"),
            amount = value.optDouble("amount"),
            fee = value.optDouble("fee"),
            score = value.optInt("score"),
            confidence = value.optInt("confidence"),
            reason = value.optString("reason"),
            pnlEur = value.optDouble("pnlEur"),
            methodVersion = value.optInt("methodVersion", 1)
        )
    }
}

data class GeminiHourlyDecision(
    val id: Long,
    val decidedAt: Long,
    val candleTime: Long,
    val price: Double,
    val requestedAction: String,
    val execution: String,
    val directionScore: Int,
    val confidence: Int,
    val horizonHours: Int,
    val reason: String,
    val risks: List<String>,
    val model: String,
    val positionAfter: Boolean,
    val portfolioValueAfter: Double,
    val requestSentAt: Long = 0L,
    val responseReceivedAt: Long = 0L,
    val executionQuoteAt: Long = 0L,
    val evaluationVersion: Int = 1,
    val evaluatedAt: Long = 0L,
    val evaluatedReturnPercent: Double? = null,
    val peakReturnPercent: Double? = null,
    val directionCorrect: Boolean? = null,
    val surgeOpportunity: Boolean? = null,
    val surgeCaptured: Boolean? = null
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("decidedAt", decidedAt)
        .put("candleTime", candleTime)
        .put("price", price)
        .put("requestedAction", requestedAction)
        .put("execution", execution)
        .put("directionScore", directionScore)
        .put("confidence", confidence)
        .put("horizonHours", horizonHours)
        .put("reason", reason)
        .put("risks", JSONArray(risks))
        .put("model", model)
        .put("positionAfter", positionAfter)
        .put("portfolioValueAfter", portfolioValueAfter)
        .put("requestSentAt", requestSentAt)
        .put("responseReceivedAt", responseReceivedAt)
        .put("executionQuoteAt", executionQuoteAt)
        .put("evaluationVersion", evaluationVersion)
        .put("evaluatedAt", evaluatedAt)
        .apply {
            evaluatedReturnPercent?.let { put("evaluatedReturnPercent", it) }
            peakReturnPercent?.let { put("peakReturnPercent", it) }
            directionCorrect?.let { put("directionCorrect", it) }
            surgeOpportunity?.let { put("surgeOpportunity", it) }
            surgeCaptured?.let { put("surgeCaptured", it) }
        }

    companion object {
        fun fromJson(value: JSONObject): GeminiHourlyDecision {
            fun nullableDouble(name: String): Double? =
                if (value.has(name) && !value.isNull(name)) value.optDouble(name) else null
            fun nullableBoolean(name: String): Boolean? =
                if (value.has(name) && !value.isNull(name)) value.optBoolean(name) else null
            val risksJson = value.optJSONArray("risks") ?: JSONArray()
            return GeminiHourlyDecision(
                id = value.optLong("id"),
                decidedAt = value.optLong("decidedAt"),
                candleTime = value.optLong("candleTime"),
                price = value.optDouble("price"),
                requestedAction = value.optString("requestedAction", "HOLD"),
                execution = value.optString("execution", "Наблюдение"),
                directionScore = value.optInt("directionScore"),
                confidence = value.optInt("confidence"),
                horizonHours = value.optInt("horizonHours", 1),
                reason = value.optString("reason"),
                risks = (0 until risksJson.length()).mapNotNull {
                    risksJson.optString(it).trim().takeIf(String::isNotBlank)
                },
                model = value.optString("model"),
                positionAfter = value.optBoolean("positionAfter"),
                portfolioValueAfter = value.optDouble(
                    "portfolioValueAfter",
                    GeminiPaperPortfolio.START_BALANCE
                ),
                requestSentAt = value.optLong("requestSentAt"),
                responseReceivedAt = value.optLong(
                    "responseReceivedAt",
                    value.optLong("decidedAt")
                ),
                executionQuoteAt = value.optLong("executionQuoteAt"),
                evaluationVersion = value.optInt("evaluationVersion", 1),
                evaluatedAt = value.optLong("evaluatedAt"),
                evaluatedReturnPercent = nullableDouble("evaluatedReturnPercent"),
                peakReturnPercent = nullableDouble("peakReturnPercent"),
                directionCorrect = nullableBoolean("directionCorrect"),
                surgeOpportunity = nullableBoolean("surgeOpportunity"),
                surgeCaptured = nullableBoolean("surgeCaptured")
            )
        }

        const val CAUSAL_EVALUATION_VERSION = 2
    }
}

data class GeminiPaperPortfolio(
    val cashEur: Double = START_BALANCE,
    val pumpAmount: Double = 0.0,
    val entryPrice: Double = 0.0,
    val lastDecisionId: Long = 0L,
    val trades: List<GeminiPaperTrade> = emptyList(),
    val decisions: List<GeminiHourlyDecision> = emptyList(),
    val totalFeesEur: Double = 0.0,
    val peakValueEur: Double = START_BALANCE,
    val maxDrawdownPercent: Double = 0.0,
    val causalPeakValueEur: Double = 0.0,
    val causalMaxDrawdownPercent: Double = 0.0,
    val positionPeakPrice: Double = 0.0
) {
    fun value(price: Double): Double = cashEur + pumpAmount * max(0.0, price)
    fun profit(price: Double): Double = value(price) - START_BALANCE
    fun profitPercent(price: Double): Double = profit(price) / START_BALANCE * 100.0
    val inPosition: Boolean get() = pumpAmount > 0.0
    val closedTrades: Int
        get() = trades.count {
            it.action == "SELL" && it.methodVersion >= GeminiHourlyDecision.CAUSAL_EVALUATION_VERSION
        }
    val winningTrades: Int
        get() = trades.count {
            it.action == "SELL" &&
                it.methodVersion >= GeminiHourlyDecision.CAUSAL_EVALUATION_VERSION &&
                it.pnlEur > 0.0
        }
    val winRatePercent: Double
        get() = if (closedTrades > 0) winningTrades.toDouble() / closedTrades * 100.0 else 0.0
    val evaluatedHours: Int
        get() = decisions.count {
            it.evaluationVersion >= GeminiHourlyDecision.CAUSAL_EVALUATION_VERSION &&
                it.directionCorrect != null
        }
    val correctDirections: Int
        get() = decisions.count {
            it.evaluationVersion >= GeminiHourlyDecision.CAUSAL_EVALUATION_VERSION &&
                it.directionCorrect == true
        }
    val directionAccuracyPercent: Double
        get() = if (evaluatedHours > 0) correctDirections.toDouble() / evaluatedHours * 100.0 else 0.0
    val surgeOpportunities: Int
        get() = decisions.count {
            it.evaluationVersion >= GeminiHourlyDecision.CAUSAL_EVALUATION_VERSION &&
                it.surgeOpportunity == true
        }
    val capturedSurges: Int
        get() = decisions.count {
            it.evaluationVersion >= GeminiHourlyDecision.CAUSAL_EVALUATION_VERSION &&
                it.surgeCaptured == true
        }
    val surgeCapturePercent: Double
        get() = if (surgeOpportunities > 0) capturedSurges.toDouble() / surgeOpportunities * 100.0 else 0.0

    companion object {
        const val START_BALANCE = 1000.0
    }
}

data class GeminiHourlyRecommendation(
    val action: String,
    val directionScore: Int,
    val confidence: Int,
    val horizonHours: Int,
    val reason: String,
    val risks: List<String>,
    val model: String
)

data class GeminiPendingDecision(
    val hourId: Long,
    val candleTime: Long,
    val recommendation: GeminiHourlyRecommendation,
    val requestSentAt: Long,
    val responseReceivedAt: Long,
    val promptTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int
) {
    fun toJson(): JSONObject = JSONObject()
        .put("hourId", hourId)
        .put("candleTime", candleTime)
        .put("requestSentAt", requestSentAt)
        .put("responseReceivedAt", responseReceivedAt)
        .put("promptTokens", promptTokens)
        .put("outputTokens", outputTokens)
        .put("totalTokens", totalTokens)
        .put("recommendation", JSONObject()
            .put("action", recommendation.action)
            .put("directionScore", recommendation.directionScore)
            .put("confidence", recommendation.confidence)
            .put("horizonHours", recommendation.horizonHours)
            .put("reason", recommendation.reason)
            .put("risks", JSONArray(recommendation.risks))
            .put("model", recommendation.model)
        )

    companion object {
        fun fromJson(value: JSONObject): GeminiPendingDecision? {
            val recommendation = value.optJSONObject("recommendation") ?: return null
            val risks = recommendation.optJSONArray("risks") ?: JSONArray()
            val hourId = value.optLong("hourId")
            val responseReceivedAt = value.optLong("responseReceivedAt")
            if (hourId <= 0L || responseReceivedAt <= 0L) return null
            return GeminiPendingDecision(
                hourId = hourId,
                candleTime = value.optLong("candleTime"),
                recommendation = GeminiHourlyRecommendation(
                    action = recommendation.optString("action", "HOLD"),
                    directionScore = recommendation.optInt("directionScore").coerceIn(-100, 100),
                    confidence = recommendation.optInt("confidence").coerceIn(0, 100),
                    horizonHours = recommendation.optInt("horizonHours", 1).coerceIn(1, 6),
                    reason = recommendation.optString("reason").take(1000),
                    risks = (0 until risks.length()).mapNotNull {
                        risks.optString(it).trim().takeIf(String::isNotBlank)
                    }.take(5),
                    model = recommendation.optString("model").take(80)
                ),
                requestSentAt = value.optLong("requestSentAt"),
                responseReceivedAt = responseReceivedAt,
                promptTokens = value.optInt("promptTokens"),
                outputTokens = value.optInt("outputTokens"),
                totalTokens = value.optInt("totalTokens")
            )
        }
    }
}

data class GeminiHourOutcome(
    val decisionId: Long,
    val evaluatedAt: Long,
    val closePrice: Double,
    val highPrice: Double
)

internal object GeminiEvaluationWindow {
    private const val HOUR_MILLIS = 60L * 60L * 1000L

    fun targetAt(decision: GeminiHourlyDecision): Long =
        decision.responseReceivedAt +
            decision.horizonHours.coerceIn(1, 6) * HOUR_MILLIS
}

/**
 * Pure paper-account logic. It never reads or changes StrategyV2 state.
 * A decision id is the UTC hour of a fully closed market hour.
 */
object GeminiPaperTrader {
    const val FEE_RATE = 0.0015
    internal const val TRADE_RETENTION_MILLIS = 183L * 24L * 60L * 60L * 1000L
    private const val MAX_RETAINED_TRADES = 5_000
    private const val SURGE_THRESHOLD_PERCENT = 3.0
    private const val MIN_DIRECTION_MOVE_PERCENT = 0.25

    fun applyDecision(
        current: GeminiPaperPortfolio,
        price: Double,
        decisionId: Long,
        candleTime: Long,
        recommendation: GeminiHourlyRecommendation,
        now: Long = System.currentTimeMillis(),
        requestSentAt: Long = now,
        responseReceivedAt: Long = now,
        executionQuoteAt: Long = now
    ): GeminiPaperPortfolio {
        if (price <= 0.0 || decisionId <= 0L || decisionId <= current.lastDecisionId) return current

        val working = current
        var cash = working.cashEur
        var amount = working.pumpAmount
        var entry = working.entryPrice
        var totalFees = working.totalFeesEur
        var trades = working.trades
        val effectiveRecommendation = GeminiExecutionPolicy.apply(recommendation, working.inPosition)
        val normalizedAction = effectiveRecommendation.action.uppercase().let {
            if (it in setOf("BUY", "HOLD", "SELL")) it else "HOLD"
        }
        val execution = when {
            normalizedAction == "BUY" && !working.inPosition && working.cashEur > 0.01 -> {
                val allocation = cash
                val fee = allocation * FEE_RATE
                amount = (allocation - fee) / price
                cash = 0.0
                entry = price
                totalFees += fee
                trades = addTrade(
                    trades,
                    GeminiPaperTrade(
                        time = now,
                        decisionId = decisionId,
                        action = "BUY",
                        price = price,
                        amount = amount,
                        fee = fee,
                        score = effectiveRecommendation.directionScore,
                        confidence = effectiveRecommendation.confidence,
                        reason = effectiveRecommendation.reason,
                        methodVersion = GeminiHourlyDecision.CAUSAL_EVALUATION_VERSION
                    )
                )
                "КУПЛЕНО на весь свободный остаток"
            }
            normalizedAction == "SELL" && working.inPosition -> {
                val soldAmount = amount
                val gross = soldAmount * price
                val fee = gross * FEE_RATE
                val matchingBuy = trades.lastOrNull { it.action == "BUY" }
                val buyFee = matchingBuy?.fee ?: 0.0
                val pnl = gross - fee - (soldAmount * entry + buyFee)
                cash += gross - fee
                amount = 0.0
                entry = 0.0
                totalFees += fee
                trades = addTrade(
                    trades,
                    GeminiPaperTrade(
                        time = now,
                        decisionId = decisionId,
                        action = "SELL",
                        price = price,
                        amount = soldAmount,
                        fee = fee,
                        score = effectiveRecommendation.directionScore,
                        confidence = effectiveRecommendation.confidence,
                        reason = effectiveRecommendation.reason,
                        pnlEur = pnl,
                        methodVersion = if (
                            matchingBuy?.methodVersion == GeminiHourlyDecision.CAUSAL_EVALUATION_VERSION
                        ) {
                            GeminiHourlyDecision.CAUSAL_EVALUATION_VERSION
                        } else {
                            1
                        }
                    )
                )
                "ПРОДАНО полностью"
            }
            normalizedAction == "BUY" && working.inPosition -> "УЖЕ В PUMP — позиция сохранена"
            normalizedAction == "BUY" -> "НЕТ СВОБОДНЫХ € — покупка не выполнена"
            normalizedAction == "SELL" -> "НЕТ PUMP — остались наличные"
            working.inPosition -> "ДЕРЖИМ PUMP"
            else -> "ЖДЁМ В НАЛИЧНЫХ"
        }

        val valueAfter = cash + amount * price
        val peak = max(working.peakValueEur, valueAfter)
        val drawdown = if (peak > 0.0) (1.0 - valueAfter / peak) * 100.0 else 0.0
        val maxDrawdown = max(working.maxDrawdownPercent, drawdown)
        val causalPeak = if (working.causalPeakValueEur > 0.0) {
            max(working.causalPeakValueEur, valueAfter)
        } else {
            valueAfter
        }
        val causalDrawdown = if (causalPeak > 0.0) {
            (1.0 - valueAfter / causalPeak) * 100.0
        } else {
            0.0
        }
        val causalMaxDrawdown = max(working.causalMaxDrawdownPercent, causalDrawdown)
        val decision = GeminiHourlyDecision(
            id = decisionId,
            decidedAt = now,
            candleTime = candleTime,
            price = price,
            requestedAction = normalizedAction,
            execution = execution,
            directionScore = effectiveRecommendation.directionScore.coerceIn(-100, 100),
            confidence = effectiveRecommendation.confidence.coerceIn(0, 100),
            horizonHours = effectiveRecommendation.horizonHours.coerceIn(1, 6),
            reason = effectiveRecommendation.reason.take(1000),
            risks = effectiveRecommendation.risks.map { it.take(300) }.take(5),
            model = effectiveRecommendation.model.take(80),
            positionAfter = amount > 0.0,
            portfolioValueAfter = valueAfter,
            requestSentAt = requestSentAt,
            responseReceivedAt = responseReceivedAt,
            executionQuoteAt = executionQuoteAt,
            evaluationVersion = GeminiHourlyDecision.CAUSAL_EVALUATION_VERSION
        )
        return working.copy(
            cashEur = cash,
            pumpAmount = amount,
            entryPrice = entry,
            lastDecisionId = decisionId,
            trades = trades,
            decisions = (working.decisions + decision).takeLast(336),
            totalFeesEur = totalFees,
            peakValueEur = peak,
            maxDrawdownPercent = maxDrawdown,
            causalPeakValueEur = causalPeak,
            causalMaxDrawdownPercent = causalMaxDrawdown,
            positionPeakPrice = when {
                amount <= 0.0 -> 0.0
                working.inPosition -> max(working.positionPeakPrice, price)
                else -> price
            }
        )
    }

    fun applyProtectiveExit(
        current: GeminiPaperPortfolio,
        price: Double,
        decisionId: Long,
        reason: String,
        now: Long = System.currentTimeMillis()
    ): GeminiPaperPortfolio {
        if (!current.inPosition || price <= 0.0 || decisionId <= 0L) return current
        val soldAmount = current.pumpAmount
        val gross = soldAmount * price
        val fee = gross * FEE_RATE
        val matchingBuy = current.trades.lastOrNull { it.action == "BUY" }
        val buyFee = matchingBuy?.fee ?: 0.0
        val pnl = gross - fee - (soldAmount * current.entryPrice + buyFee)
        val trade = GeminiPaperTrade(
            time = now,
            decisionId = decisionId,
            action = "SELL",
            price = price,
            amount = soldAmount,
            fee = fee,
            score = -100,
            confidence = 100,
            reason = reason,
            pnlEur = pnl,
            methodVersion = GeminiHourlyDecision.CAUSAL_EVALUATION_VERSION
        )
        val cash = current.cashEur + gross - fee
        return current.copy(
            cashEur = cash,
            pumpAmount = 0.0,
            entryPrice = 0.0,
            lastDecisionId = max(current.lastDecisionId, decisionId),
            trades = addTrade(current.trades, trade),
            totalFeesEur = current.totalFeesEur + fee,
            peakValueEur = max(current.peakValueEur, cash),
            causalPeakValueEur = max(current.causalPeakValueEur, cash),
            positionPeakPrice = 0.0
        )
    }

    fun applyExperimentalEntry(
        current: GeminiPaperPortfolio,
        price: Double,
        decisionId: Long,
        score: Int,
        reason: String,
        now: Long = System.currentTimeMillis()
    ): GeminiPaperPortfolio {
        if (current.inPosition || current.cashEur <= 0.01 || price <= 0.0 || decisionId <= 0L) {
            return current
        }
        val allocation = current.cashEur
        val fee = allocation * FEE_RATE
        val amount = (allocation - fee) / price
        val trade = GeminiPaperTrade(
            time = now,
            decisionId = decisionId,
            action = "BUY",
            price = price,
            amount = amount,
            fee = fee,
            score = score.coerceIn(0, 100),
            confidence = 100,
            reason = reason,
            methodVersion = GeminiHourlyDecision.CAUSAL_EVALUATION_VERSION
        )
        return current.copy(
            cashEur = 0.0,
            pumpAmount = amount,
            entryPrice = price,
            trades = addTrade(current.trades, trade),
            totalFeesEur = current.totalFeesEur + fee,
            positionPeakPrice = price
        )
    }

    fun gradeCompletedHorizons(
        current: GeminiPaperPortfolio,
        outcomes: List<GeminiHourOutcome>
    ): GeminiPaperPortfolio {
        if (current.decisions.none { it.evaluatedReturnPercent == null }) return current
        val byDecision = outcomes.associateBy { it.decisionId }
        var changed = false
        val decisions = current.decisions.map { previous ->
            if (previous.evaluatedReturnPercent != null || previous.price <= 0.0) {
                return@map previous
            }
            if (previous.evaluationVersion < GeminiHourlyDecision.CAUSAL_EVALUATION_VERSION) {
                return@map previous
            }
            val outcome = byDecision[previous.id] ?: return@map previous
            if (outcome.closePrice <= 0.0 || outcome.highPrice <= 0.0) return@map previous
            val closeMove = (outcome.closePrice / previous.price - 1.0) * 100.0
            val peakMove = (outcome.highPrice / previous.price - 1.0) * 100.0
            val predictedDirection = when {
                previous.directionScore >= 10 -> 1
                previous.directionScore <= -10 -> -1
                else -> 0
            }
            val actualDirection = when {
                closeMove >= MIN_DIRECTION_MOVE_PERCENT -> 1
                closeMove <= -MIN_DIRECTION_MOVE_PERCENT -> -1
                else -> 0
            }
            val opportunity = peakMove >= SURGE_THRESHOLD_PERCENT
            changed = true
            previous.copy(
                evaluatedAt = outcome.evaluatedAt,
                evaluatedReturnPercent = closeMove,
                peakReturnPercent = peakMove,
                directionCorrect = predictedDirection == actualDirection,
                surgeOpportunity = opportunity,
                surgeCaptured = opportunity && previous.positionAfter
            )
        }
        return if (changed) current.copy(decisions = decisions) else current
    }

    fun markToMarket(
        current: GeminiPaperPortfolio,
        price: Double
    ): GeminiPaperPortfolio {
        if (price <= 0.0 || !price.isFinite()) return current
        val value = current.value(price)
        val peak = max(current.peakValueEur, value)
        val drawdown = if (peak > 0.0) (1.0 - value / peak) * 100.0 else 0.0
        val maxDrawdown = max(current.maxDrawdownPercent, drawdown)
        val causalPeak = if (current.causalPeakValueEur > 0.0) {
            max(current.causalPeakValueEur, value)
        } else {
            value
        }
        val causalDrawdown = if (causalPeak > 0.0) (1.0 - value / causalPeak) * 100.0 else 0.0
        val causalMaxDrawdown = max(current.causalMaxDrawdownPercent, causalDrawdown)
        val positionPeakPrice = if (current.inPosition) {
            max(max(current.positionPeakPrice, current.entryPrice), price)
        } else {
            0.0
        }
        return if (peak != current.peakValueEur ||
            maxDrawdown != current.maxDrawdownPercent ||
            causalPeak != current.causalPeakValueEur ||
            causalMaxDrawdown != current.causalMaxDrawdownPercent ||
            positionPeakPrice != current.positionPeakPrice
        ) {
            current.copy(
                peakValueEur = peak,
                maxDrawdownPercent = maxDrawdown,
                causalPeakValueEur = causalPeak,
                causalMaxDrawdownPercent = causalMaxDrawdown,
                positionPeakPrice = positionPeakPrice
            )
        } else {
            current
        }
    }

    private fun addTrade(old: List<GeminiPaperTrade>, trade: GeminiPaperTrade): List<GeminiPaperTrade> {
        val cutoff = trade.time - TRADE_RETENTION_MILLIS
        return (old + trade)
            .filter { it.time >= cutoff }
            .takeLast(MAX_RETAINED_TRADES)
    }
}

/**
 * A mild deterministic guard against low-conviction Gemini BUYs.
 * It stays independent from StrategyV2 and never creates a BUY by itself.
 */
internal object GeminiExecutionPolicy {
    const val MIN_BUY_DIRECTION = 30
    const val MIN_BUY_CONFIDENCE = 65

    fun apply(
        recommendation: GeminiHourlyRecommendation,
        alreadyInPosition: Boolean
    ): GeminiHourlyRecommendation {
        if (recommendation.action.uppercase() != "BUY" || alreadyInPosition) return recommendation
        if (recommendation.directionScore >= MIN_BUY_DIRECTION &&
            recommendation.confidence >= MIN_BUY_CONFIDENCE
        ) return recommendation
        return recommendation.copy(
            action = "HOLD",
            reason = "Вход пропущен защитой от ложного BUY: нужно направление не ниже " +
                "$MIN_BUY_DIRECTION/100 и уверенность не ниже $MIN_BUY_CONFIDENCE/100. " +
                recommendation.reason
        )
    }
}

internal object GeminiTradeAlertPolicy {
    fun newlyExecutedTrade(
        before: GeminiPaperPortfolio,
        after: GeminiPaperPortfolio,
        decisionId: Long
    ): GeminiPaperTrade? {
        if (after.trades.size <= before.trades.size) return null
        val trade = after.trades.drop(before.trades.size).lastOrNull {
            it.decisionId == decisionId && it.action in setOf("BUY", "SELL")
        } ?: return null
        return trade
    }
}

data class GeminiExperimentState(
    val enabled: Boolean,
    val status: String,
    val phase: String,
    val cycleSource: String,
    val lastCycleStarted: Long,
    val lastCycleFinished: Long,
    val lastDataReady: Long,
    val dataDurationMillis: Long,
    val nextCheckAt: Long,
    val lastAttempt: Long,
    val lastAttemptHour: Long,
    val lastSuccess: Long,
    val lastFailure: Long,
    val attemptsThisHour: Int,
    val model: String,
    val activeModel: String,
    val error: String,
    val requestsToday: Int,
    val promptTokensToday: Int,
    val outputTokensToday: Int,
    val totalTokensToday: Int,
    val portfolio: GeminiPaperPortfolio,
    val pendingDecision: GeminiPendingDecision?,
    val activity: List<GeminiActivityEvent>
)

data class GeminiActivityEvent(
    val at: Long,
    val stage: String,
    val result: String,
    val detail: String,
    val durationMillis: Long = 0L,
    val model: String = "",
    val hourId: Long = 0L,
    val attempt: Int = 0
) {
    fun toJson(): JSONObject = JSONObject()
        .put("at", at)
        .put("stage", stage)
        .put("result", result)
        .put("detail", detail)
        .put("durationMillis", durationMillis)
        .put("model", model)
        .put("hourId", hourId)
        .put("attempt", attempt)

    companion object {
        fun fromJson(value: JSONObject) = GeminiActivityEvent(
            at = value.optLong("at"),
            stage = value.optString("stage"),
            result = value.optString("result", "INFO"),
            detail = value.optString("detail"),
            durationMillis = value.optLong("durationMillis"),
            model = value.optString("model"),
            hourId = value.optLong("hourId"),
            attempt = value.optInt("attempt")
        )
    }
}

object GeminiCycleGuard {
    private val active = AtomicBoolean(false)

    fun tryEnter(): Boolean = active.compareAndSet(false, true)
    fun exit() {
        active.set(false)
    }
}

object GeminiPaperStore {
    private const val PREFS = "gemini_paper_v34"
    private const val KEY_PORTFOLIO = "portfolio"
    private const val KEY_PORTFOLIO_BACKUP = "portfolio_backup_v322"
    private const val KEY_PENDING_ALERTS = "pending_trade_alerts_v322"
    private const val KEY_STORAGE_ERROR = "portfolio_storage_error_v322"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_STATUS = "status"
    private const val KEY_PHASE = "phase"
    private const val KEY_CYCLE_SOURCE = "cycle_source"
    private const val KEY_LAST_CYCLE_STARTED = "last_cycle_started"
    private const val KEY_LAST_CYCLE_FINISHED = "last_cycle_finished"
    private const val KEY_LAST_DATA_READY = "last_data_ready"
    private const val KEY_DATA_DURATION = "data_duration"
    private const val KEY_NEXT_CHECK_AT = "next_check_at"
    private const val KEY_ACTIVITY = "activity"
    private const val KEY_LAST_ATTEMPT = "last_attempt"
    private const val KEY_LAST_ATTEMPT_HOUR = "last_attempt_hour"
    private const val KEY_LAST_SUCCESS = "last_success"
    private const val KEY_LAST_FAILURE = "last_failure"
    private const val KEY_ATTEMPTS_THIS_HOUR = "attempts_this_hour"
    private const val KEY_MODEL = "model"
    private const val KEY_ACTIVE_MODEL = "active_model"
    private const val KEY_ERROR = "error"
    private const val KEY_USAGE_DAY = "usage_day"
    private const val KEY_REQUESTS = "requests"
    private const val KEY_PROMPT_TOKENS = "prompt_tokens"
    private const val KEY_OUTPUT_TOKENS = "output_tokens"
    private const val KEY_TOTAL_TOKENS = "total_tokens"
    private const val KEY_PENDING_DECISION = "pending_decision_v37"
    private const val KEY_ACTIVITY_MIGRATED = "activity_migrated_v37"
    private const val MAX_LEGACY_ACTIVITY_EVENTS = 1000
    private val activityLock = Any()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun state(
        context: Context,
        includeActivity: Boolean = false
    ): GeminiExperimentState {
        resetUsageDayIfNeeded(context)
        migrateActivityIfNeeded(context)
        val p = prefs(context)
        return GeminiExperimentState(
            enabled = p.getBoolean(KEY_ENABLED, true),
            status = p.getString(KEY_STATUS, "ЖДЁМ ЗАКРЫТИЯ ЧАСА").orEmpty(),
            phase = p.getString(KEY_PHASE, "ОЖИДАНИЕ ПЕРВОГО ЦИКЛА").orEmpty(),
            cycleSource = p.getString(KEY_CYCLE_SOURCE, "").orEmpty(),
            lastCycleStarted = p.getLong(KEY_LAST_CYCLE_STARTED, 0L),
            lastCycleFinished = p.getLong(KEY_LAST_CYCLE_FINISHED, 0L),
            lastDataReady = p.getLong(KEY_LAST_DATA_READY, 0L),
            dataDurationMillis = p.getLong(KEY_DATA_DURATION, 0L),
            nextCheckAt = p.getLong(KEY_NEXT_CHECK_AT, 0L),
            lastAttempt = p.getLong(KEY_LAST_ATTEMPT, 0L),
            lastAttemptHour = p.getLong(KEY_LAST_ATTEMPT_HOUR, 0L),
            lastSuccess = p.getLong(KEY_LAST_SUCCESS, 0L),
            lastFailure = p.getLong(KEY_LAST_FAILURE, 0L),
            attemptsThisHour = p.getInt(KEY_ATTEMPTS_THIS_HOUR, 0),
            model = p.getString(KEY_MODEL, "").orEmpty(),
            activeModel = p.getString(KEY_ACTIVE_MODEL, "").orEmpty(),
            error = p.getString(KEY_ERROR, "").orEmpty(),
            requestsToday = p.getInt(KEY_REQUESTS, 0),
            promptTokensToday = p.getInt(KEY_PROMPT_TOKENS, 0),
            outputTokensToday = p.getInt(KEY_OUTPUT_TOKENS, 0),
            totalTokensToday = p.getInt(KEY_TOTAL_TOKENS, 0),
            portfolio = GeminiResearchStore.applyPortfolioMetrics(
                context,
                loadPortfolioRecovering(context)
            ),
            pendingDecision = loadPendingDecision(p.getString(KEY_PENDING_DECISION, null)),
            activity = if (includeActivity) {
                GeminiResearchStore.activity(context)
            } else {
                emptyList()
            }
        )
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_STATUS, if (enabled) "ЖДЁМ ЗАКРЫТИЯ ЧАСА" else "ВЫКЛЮЧЕН")
            .putString(KEY_PHASE, if (enabled) "ОЖИДАНИЕ ЦИКЛА" else "ВЫКЛЮЧЕН")
            .commit()
        recordActivity(
            context,
            stage = "УПРАВЛЕНИЕ",
            result = if (enabled) "OK" else "WAIT",
            detail = if (enabled) "Gemini включён" else "Gemini выключен"
        )
    }

    fun beginCycle(
        context: Context,
        source: String,
        expectedIntervalMillis: Long,
        now: Long = System.currentTimeMillis()
    ) {
        prefs(context).edit()
            .putString(KEY_PHASE, "СОБИРАЮ РЫНОК И НОВОСТИ")
            .putString(KEY_CYCLE_SOURCE, source.take(80))
            .putLong(KEY_LAST_CYCLE_STARTED, now)
            .putLong(KEY_NEXT_CHECK_AT, now + expectedIntervalMillis.coerceAtLeast(0L))
            .apply()
        recordActivity(
            context,
            stage = "ЦИКЛ",
            result = "START",
            detail = "$source: начата проверка рынка, новостей и часового прогноза",
            at = now
        )
    }

    fun markDataReady(
        context: Context,
        source: String,
        startedAt: Long,
        now: Long = System.currentTimeMillis()
    ) {
        val duration = (now - startedAt).coerceAtLeast(0L)
        prefs(context).edit()
            .putString(KEY_PHASE, "ДАННЫЕ ГОТОВЫ • ПРОВЕРЯЮ ЧАС")
            .putLong(KEY_LAST_DATA_READY, now)
            .putLong(KEY_DATA_DURATION, duration)
            .apply()
        recordActivity(
            context,
            stage = "ДАННЫЕ",
            result = "OK",
            detail = "$source: рынок и новости обновлены",
            durationMillis = duration,
            at = now
        )
    }

    fun finishCycle(
        context: Context,
        source: String,
        startedAt: Long,
        nextCheckAt: Long,
        detail: String,
        now: Long = System.currentTimeMillis()
    ) {
        val duration = (now - startedAt).coerceAtLeast(0L)
        prefs(context).edit()
            .putString(KEY_PHASE, "ОЖИДАНИЕ СЛЕДУЮЩЕЙ ПРОВЕРКИ")
            .putLong(KEY_LAST_CYCLE_FINISHED, now)
            .putLong(KEY_NEXT_CHECK_AT, nextCheckAt)
            .apply()
        recordActivity(
            context,
            stage = "ЦИКЛ",
            result = "OK",
            detail = "$source: $detail",
            durationMillis = duration,
            at = now
        )
    }

    fun failCycle(
        context: Context,
        source: String,
        startedAt: Long,
        nextCheckAt: Long,
        error: String,
        now: Long = System.currentTimeMillis()
    ) {
        val duration = (now - startedAt).coerceAtLeast(0L)
        prefs(context).edit()
            .putString(KEY_PHASE, "ОШИБКА ЦИКЛА • БУДЕТ ПОВТОР")
            .putLong(KEY_LAST_CYCLE_FINISHED, now)
            .putLong(KEY_NEXT_CHECK_AT, nextCheckAt)
            .apply()
        recordActivity(
            context,
            stage = "ЦИКЛ",
            result = "ERROR",
            detail = "$source: ${error.take(500)}",
            durationMillis = duration,
            at = now
        )
    }

    fun recordActivity(
        context: Context,
        stage: String,
        result: String,
        detail: String,
        durationMillis: Long = 0L,
        model: String = "",
        hourId: Long = 0L,
        attempt: Int = 0,
        at: Long = System.currentTimeMillis()
    ) {
        synchronized(activityLock) {
            migrateActivityIfNeeded(context)
            val event = GeminiActivityEvent(
                at = at,
                stage = stage.take(40),
                result = result.take(20),
                detail = detail.take(700),
                durationMillis = durationMillis.coerceAtLeast(0L),
                model = model.take(80),
                hourId = hourId,
                attempt = attempt.coerceAtLeast(0)
            )
            GeminiResearchStore.recordActivity(context, event)
        }
    }

    fun markAttempt(context: Context, hourId: Long, now: Long = System.currentTimeMillis()) {
        resetUsageDayIfNeeded(context, now)
        val p = prefs(context)
        val attempts = if (p.getLong(KEY_LAST_ATTEMPT_HOUR, Long.MIN_VALUE) == hourId) {
            p.getInt(KEY_ATTEMPTS_THIS_HOUR, 0) + 1
        } else {
            1
        }
        p.edit()
            .putLong(KEY_LAST_ATTEMPT, now)
            .putLong(KEY_LAST_ATTEMPT_HOUR, hourId)
            .putInt(KEY_ATTEMPTS_THIS_HOUR, attempts)
            .putString(KEY_STATUS, "GEMINI АНАЛИЗИРУЕТ")
            .putString(KEY_PHASE, "ГОТОВЛЮ ЗАПРОС GEMINI")
            .putString(KEY_ERROR, "")
            .apply()
    }

    fun markApiRequest(context: Context, model: String, now: Long = System.currentTimeMillis()) {
        resetUsageDayIfNeeded(context, now)
        val p = prefs(context)
        p.edit()
            .putString(KEY_ACTIVE_MODEL, model.take(80))
            .putString(KEY_STATUS, "GEMINI АНАЛИЗИРУЕТ")
            .putString(KEY_PHASE, "ЗАПРОС ОТПРАВЛЕН • ЖДУ ОТВЕТ")
            .putInt(KEY_REQUESTS, p.getInt(KEY_REQUESTS, 0) + 1)
            .apply()
    }

    fun savePendingSuccess(
        context: Context,
        pending: GeminiPendingDecision,
        now: Long = System.currentTimeMillis()
    ) {
        val p = prefs(context)
        p.edit()
            .putString(KEY_PENDING_DECISION, pending.toJson().toString())
            .putLong(KEY_LAST_SUCCESS, now)
            .putString(KEY_STATUS, "ЖДЁМ СВЕЖУЮ ЦЕНУ")
            .putString(KEY_PHASE, "ОТВЕТ ПОЛУЧЕН • ФИКСИРУЮ ЦЕНУ")
            .putString(KEY_MODEL, pending.recommendation.model.take(80))
            .putString(KEY_ACTIVE_MODEL, pending.recommendation.model.take(80))
            .putString(KEY_ERROR, "")
            .putInt(KEY_PROMPT_TOKENS, p.getInt(KEY_PROMPT_TOKENS, 0) + pending.promptTokens)
            .putInt(KEY_OUTPUT_TOKENS, p.getInt(KEY_OUTPUT_TOKENS, 0) + pending.outputTokens)
            .putInt(KEY_TOTAL_TOKENS, p.getInt(KEY_TOTAL_TOKENS, 0) + pending.totalTokens)
            .apply()
    }

    fun completePending(
        context: Context,
        portfolio: GeminiPaperPortfolio,
        pendingTrade: GeminiPaperTrade? = null,
        now: Long = System.currentTimeMillis()
    ) {
        GeminiResearchStore.savePortfolioMetrics(context, portfolio)
        val raw = portfolioToJson(portfolio).toString()
        val pending = (pendingTradeAlerts(context) + listOfNotNull(pendingTrade))
            .distinctBy(::alertId)
        prefs(context).edit()
            .putString(KEY_PORTFOLIO, raw)
            .putString(KEY_PORTFOLIO_BACKUP, raw)
            .putString(KEY_PENDING_ALERTS, JSONArray(pending.map { it.toJson() }).toString())
            .remove(KEY_PENDING_DECISION)
            .remove(KEY_STORAGE_ERROR)
            .putLong(KEY_LAST_SUCCESS, now)
            .putString(KEY_STATUS, "РАБОТАЕТ")
            .putString(KEY_PHASE, "РЕШЕНИЕ ИСПОЛНЕНО ПО СВЕЖЕЙ ЦЕНЕ")
            .putString(KEY_ERROR, "")
            .commit()
    }

    fun saveFailure(context: Context, error: String, now: Long = System.currentTimeMillis()) {
        prefs(context).edit()
            .putLong(KEY_LAST_FAILURE, now)
            .putString(KEY_STATUS, "ОШИБКА")
            .putString(KEY_PHASE, "ОШИБКА GEMINI • ОЖИДАНИЕ ПОВТОРА")
            .putString(KEY_ERROR, error.take(500))
            .apply()
    }

    fun markWaiting(context: Context, status: String) {
        prefs(context).edit()
            .putString(KEY_STATUS, status.take(120))
            .putString(KEY_PHASE, status.take(120))
            .apply()
    }

    fun savePortfolio(context: Context, portfolio: GeminiPaperPortfolio) {
        GeminiResearchStore.savePortfolioMetrics(context, portfolio)
        val raw = portfolioToJson(portfolio).toString()
        prefs(context).edit()
            .putString(KEY_PORTFOLIO, raw)
            .putString(KEY_PORTFOLIO_BACKUP, raw)
            .remove(KEY_STORAGE_ERROR)
            .commit()
    }

    fun requireHealthyPortfolio(context: Context) {
        check(prefs(context).getString(KEY_STORAGE_ERROR, "").isNullOrBlank()) {
            prefs(context).getString(KEY_STORAGE_ERROR, "Ошибка хранилища Gemini").orEmpty()
        }
    }

    fun flushPendingTradeAlerts(context: Context) {
        val pending = pendingTradeAlerts(context)
        if (pending.isEmpty()) return
        var delivered = 0
        var failure = ""
        for (trade in pending) {
            val result = runCatching { PumpAlert.showGeminiTrade(context, trade) }
            if (result.isFailure) {
                failure = result.exceptionOrNull()?.message ?: "уведомление не принято Android"
                break
            }
            delivered++
        }
        if (delivered > 0) {
            prefs(context).edit().putString(
                KEY_PENDING_ALERTS,
                JSONArray(pending.drop(delivered).map { it.toJson() }).toString()
            ).commit()
        }
        if (failure.isNotBlank()) {
            recordActivity(
                context,
                "ОЧЕРЕДЬ ЗВОНКОВ",
                "WAIT",
                "Сделка сохранена; звонок будет повторён: $failure"
            )
        }
    }

    fun reset(context: Context) {
        GeminiResearchStore.clear(context)
        prefs(context).edit().clear().putBoolean(KEY_ENABLED, true).apply()
    }

    internal fun loadPortfolio(raw: String?): GeminiPaperPortfolio {
        return parsePortfolio(raw) ?: GeminiPaperPortfolio()
    }

    private fun parsePortfolio(raw: String?): GeminiPaperPortfolio? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            val tradesJson = json.optJSONArray("trades") ?: JSONArray()
            val decisionsJson = json.optJSONArray("decisions") ?: JSONArray()
            GeminiPaperPortfolio(
                cashEur = json.optDouble("cashEur", GeminiPaperPortfolio.START_BALANCE),
                pumpAmount = json.optDouble("pumpAmount"),
                entryPrice = json.optDouble("entryPrice"),
                lastDecisionId = json.optLong("lastDecisionId"),
                trades = (0 until tradesJson.length()).mapNotNull {
                    tradesJson.optJSONObject(it)?.let(GeminiPaperTrade::fromJson)
                },
                decisions = (0 until decisionsJson.length()).mapNotNull {
                    decisionsJson.optJSONObject(it)?.let(GeminiHourlyDecision::fromJson)
                },
                totalFeesEur = json.optDouble("totalFeesEur"),
                peakValueEur = json.optDouble(
                    "peakValueEur",
                    GeminiPaperPortfolio.START_BALANCE
                ),
                maxDrawdownPercent = json.optDouble("maxDrawdownPercent"),
                causalPeakValueEur = json.optDouble("causalPeakValueEur", 0.0),
                causalMaxDrawdownPercent = json.optDouble(
                    "causalMaxDrawdownPercent",
                    0.0
                ),
                positionPeakPrice = json.optDouble("positionPeakPrice", 0.0)
            )
        }.getOrNull()
    }

    private fun loadPortfolioRecovering(context: Context): GeminiPaperPortfolio {
        val p = prefs(context)
        val primary = p.getString(KEY_PORTFOLIO, null)
        parsePortfolio(primary)?.let { return it }
        val recovered = parsePortfolio(p.getString(KEY_PORTFOLIO_BACKUP, null))
        if (recovered != null) {
            p.edit()
                .putString(KEY_PORTFOLIO, portfolioToJson(recovered).toString())
                .remove(KEY_STORAGE_ERROR)
                .commit()
            return recovered
        }
        if (!primary.isNullOrBlank()) {
            p.edit().putString(
                KEY_STORAGE_ERROR,
                "Повреждены основные данные Gemini; торговля остановлена до восстановления или сброса"
            ).commit()
        }
        return GeminiPaperPortfolio()
    }

    private fun pendingTradeAlerts(context: Context): List<GeminiPaperTrade> = runCatching {
        val json = JSONArray(prefs(context).getString(KEY_PENDING_ALERTS, "[]").orEmpty())
        (0 until json.length()).mapNotNull { index ->
            json.optJSONObject(index)?.let(GeminiPaperTrade::fromJson)
        }
    }.getOrDefault(emptyList())

    private fun alertId(trade: GeminiPaperTrade): String =
        "${trade.time}:${trade.decisionId}:${trade.action}"

    private fun loadActivity(raw: String?): List<GeminiActivityEvent> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            (0 until json.length()).mapNotNull {
                json.optJSONObject(it)?.let(GeminiActivityEvent::fromJson)
            }.takeLast(MAX_LEGACY_ACTIVITY_EVENTS)
        }.getOrDefault(emptyList())
    }

    internal fun portfolioToJson(value: GeminiPaperPortfolio): JSONObject = JSONObject()
        .put("cashEur", value.cashEur)
        .put("pumpAmount", value.pumpAmount)
        .put("entryPrice", value.entryPrice)
        .put("lastDecisionId", value.lastDecisionId)
        .put("trades", JSONArray(value.trades.map { it.toJson() }))
        .put("decisions", JSONArray(value.decisions.map { it.toJson() }))
        .put("totalFeesEur", value.totalFeesEur)
        .put("peakValueEur", value.peakValueEur)
        .put("maxDrawdownPercent", value.maxDrawdownPercent)
        .put("causalPeakValueEur", value.causalPeakValueEur)
        .put("causalMaxDrawdownPercent", value.causalMaxDrawdownPercent)
        .put("positionPeakPrice", value.positionPeakPrice)

    private fun loadPendingDecision(raw: String?): GeminiPendingDecision? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            GeminiPendingDecision.fromJson(JSONObject(raw))
        }.getOrNull()
    }

    private fun migrateActivityIfNeeded(context: Context) {
        val p = prefs(context)
        if (p.getBoolean(KEY_ACTIVITY_MIGRATED, false)) return
        synchronized(activityLock) {
            if (p.getBoolean(KEY_ACTIVITY_MIGRATED, false)) return@synchronized
            val legacy = loadActivity(p.getString(KEY_ACTIVITY, null))
            GeminiResearchStore.insertLegacyActivity(context, legacy)
            p.edit()
                .remove(KEY_ACTIVITY)
                .putBoolean(KEY_ACTIVITY_MIGRATED, true)
                .commit()
        }
    }

    private fun resetUsageDayIfNeeded(context: Context, now: Long = System.currentTimeMillis()) {
        val day = now / (24L * 60L * 60L * 1000L)
        val p = prefs(context)
        if (p.getLong(KEY_USAGE_DAY, -1L) == day) return
        p.edit()
            .putLong(KEY_USAGE_DAY, day)
            .putInt(KEY_REQUESTS, 0)
            .putInt(KEY_PROMPT_TOKENS, 0)
            .putInt(KEY_OUTPUT_TOKENS, 0)
            .putInt(KEY_TOTAL_TOKENS, 0)
            .apply()
    }
}
