package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
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
    val pnlEur: Double = 0.0
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
            pnlEur = value.optDouble("pnlEur")
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
                evaluatedReturnPercent = nullableDouble("evaluatedReturnPercent"),
                peakReturnPercent = nullableDouble("peakReturnPercent"),
                directionCorrect = nullableBoolean("directionCorrect"),
                surgeOpportunity = nullableBoolean("surgeOpportunity"),
                surgeCaptured = nullableBoolean("surgeCaptured")
            )
        }
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
    val maxDrawdownPercent: Double = 0.0
) {
    fun value(price: Double): Double = cashEur + pumpAmount * max(0.0, price)
    fun profit(price: Double): Double = value(price) - START_BALANCE
    fun profitPercent(price: Double): Double = profit(price) / START_BALANCE * 100.0
    val inPosition: Boolean get() = pumpAmount > 0.0
    val closedTrades: Int get() = trades.count { it.action == "SELL" }
    val winningTrades: Int get() = trades.count { it.action == "SELL" && it.pnlEur > 0.0 }
    val winRatePercent: Double
        get() = if (closedTrades > 0) winningTrades.toDouble() / closedTrades * 100.0 else 0.0
    val evaluatedHours: Int get() = decisions.count { it.directionCorrect != null }
    val correctDirections: Int get() = decisions.count { it.directionCorrect == true }
    val directionAccuracyPercent: Double
        get() = if (evaluatedHours > 0) correctDirections.toDouble() / evaluatedHours * 100.0 else 0.0
    val surgeOpportunities: Int get() = decisions.count { it.surgeOpportunity == true }
    val capturedSurges: Int get() = decisions.count { it.surgeCaptured == true }
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

data class GeminiHourOutcome(
    val decisionId: Long,
    val closePrice: Double,
    val highPrice: Double
)

/**
 * Pure paper-account logic. It never reads or changes StrategyV2 state.
 * A decision id is the UTC hour of a fully closed market hour.
 */
object GeminiPaperTrader {
    const val FEE_RATE = 0.0015
    private const val SURGE_THRESHOLD_PERCENT = 3.0
    private const val MIN_DIRECTION_MOVE_PERCENT = 0.25

    fun applyDecision(
        current: GeminiPaperPortfolio,
        price: Double,
        decisionId: Long,
        candleTime: Long,
        recommendation: GeminiHourlyRecommendation,
        now: Long = System.currentTimeMillis()
    ): GeminiPaperPortfolio {
        if (price <= 0.0 || decisionId <= 0L || decisionId <= current.lastDecisionId) return current

        val working = current
        var cash = working.cashEur
        var amount = working.pumpAmount
        var entry = working.entryPrice
        var totalFees = working.totalFeesEur
        var trades = working.trades
        val normalizedAction = recommendation.action.uppercase().let {
            if (it in setOf("BUY", "HOLD", "SELL")) it else "HOLD"
        }
        val execution = when {
            normalizedAction == "BUY" && !working.inPosition -> {
                val fee = cash * FEE_RATE
                amount = (cash - fee) / price
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
                        score = recommendation.directionScore,
                        confidence = recommendation.confidence,
                        reason = recommendation.reason
                    )
                )
                "КУПЛЕНО на все свободные €"
            }
            normalizedAction == "SELL" && working.inPosition -> {
                val soldAmount = amount
                val gross = soldAmount * price
                val fee = gross * FEE_RATE
                val buyFee = trades.lastOrNull { it.action == "BUY" }?.fee ?: 0.0
                val pnl = gross - fee - (soldAmount * entry + buyFee)
                cash = gross - fee
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
                        score = recommendation.directionScore,
                        confidence = recommendation.confidence,
                        reason = recommendation.reason,
                        pnlEur = pnl
                    )
                )
                "ПРОДАНО полностью"
            }
            normalizedAction == "BUY" -> "УЖЕ В PUMP — позиция сохранена"
            normalizedAction == "SELL" -> "НЕТ PUMP — остались наличные"
            working.inPosition -> "ДЕРЖИМ PUMP"
            else -> "ЖДЁМ В НАЛИЧНЫХ"
        }

        val valueAfter = cash + amount * price
        val peak = max(working.peakValueEur, valueAfter)
        val drawdown = if (peak > 0.0) (1.0 - valueAfter / peak) * 100.0 else 0.0
        val maxDrawdown = max(working.maxDrawdownPercent, drawdown)
        val decision = GeminiHourlyDecision(
            id = decisionId,
            decidedAt = now,
            candleTime = candleTime,
            price = price,
            requestedAction = normalizedAction,
            execution = execution,
            directionScore = recommendation.directionScore.coerceIn(-100, 100),
            confidence = recommendation.confidence.coerceIn(0, 100),
            horizonHours = recommendation.horizonHours.coerceIn(1, 6),
            reason = recommendation.reason.take(1000),
            risks = recommendation.risks.map { it.take(300) }.take(5),
            model = recommendation.model.take(80),
            positionAfter = amount > 0.0,
            portfolioValueAfter = valueAfter
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
            maxDrawdownPercent = maxDrawdown
        )
    }

    fun gradeCompletedHours(
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
                evaluatedReturnPercent = closeMove,
                peakReturnPercent = peakMove,
                directionCorrect = predictedDirection == actualDirection,
                surgeOpportunity = opportunity,
                surgeCaptured = opportunity && previous.positionAfter
            )
        }
        return if (changed) current.copy(decisions = decisions) else current
    }

    private fun addTrade(old: List<GeminiPaperTrade>, trade: GeminiPaperTrade): List<GeminiPaperTrade> =
        (old + trade).takeLast(200)
}

data class GeminiExperimentState(
    val enabled: Boolean,
    val status: String,
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
    val portfolio: GeminiPaperPortfolio
)

object GeminiPaperStore {
    private const val PREFS = "gemini_paper_v34"
    private const val KEY_PORTFOLIO = "portfolio"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_STATUS = "status"
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

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun state(context: Context): GeminiExperimentState {
        resetUsageDayIfNeeded(context)
        val p = prefs(context)
        return GeminiExperimentState(
            enabled = p.getBoolean(KEY_ENABLED, true),
            status = p.getString(KEY_STATUS, "ЖДЁМ ЗАКРЫТИЯ ЧАСА").orEmpty(),
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
            portfolio = loadPortfolio(p.getString(KEY_PORTFOLIO, null))
        )
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_STATUS, if (enabled) "ЖДЁМ ЗАКРЫТИЯ ЧАСА" else "ВЫКЛЮЧЕН")
            .apply()
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
            .putString(KEY_ERROR, "")
            .apply()
    }

    fun markApiRequest(context: Context, model: String, now: Long = System.currentTimeMillis()) {
        resetUsageDayIfNeeded(context, now)
        val p = prefs(context)
        p.edit()
            .putString(KEY_ACTIVE_MODEL, model.take(80))
            .putString(KEY_STATUS, "GEMINI АНАЛИЗИРУЕТ")
            .putInt(KEY_REQUESTS, p.getInt(KEY_REQUESTS, 0) + 1)
            .apply()
    }

    fun saveSuccess(
        context: Context,
        portfolio: GeminiPaperPortfolio,
        model: String,
        promptTokens: Int,
        outputTokens: Int,
        totalTokens: Int,
        now: Long = System.currentTimeMillis()
    ) {
        val p = prefs(context)
        p.edit()
            .putString(KEY_PORTFOLIO, portfolioToJson(portfolio).toString())
            .putLong(KEY_LAST_SUCCESS, now)
            .putString(KEY_STATUS, "РАБОТАЕТ")
            .putString(KEY_MODEL, model.take(80))
            .putString(KEY_ACTIVE_MODEL, model.take(80))
            .putString(KEY_ERROR, "")
            .putInt(KEY_PROMPT_TOKENS, p.getInt(KEY_PROMPT_TOKENS, 0) + promptTokens)
            .putInt(KEY_OUTPUT_TOKENS, p.getInt(KEY_OUTPUT_TOKENS, 0) + outputTokens)
            .putInt(KEY_TOTAL_TOKENS, p.getInt(KEY_TOTAL_TOKENS, 0) + totalTokens)
            .apply()
    }

    fun saveFailure(context: Context, error: String, now: Long = System.currentTimeMillis()) {
        prefs(context).edit()
            .putLong(KEY_LAST_FAILURE, now)
            .putString(KEY_STATUS, "ОШИБКА")
            .putString(KEY_ERROR, error.take(500))
            .apply()
    }

    fun markWaiting(context: Context, status: String) {
        prefs(context).edit().putString(KEY_STATUS, status.take(120)).apply()
    }

    fun savePortfolio(context: Context, portfolio: GeminiPaperPortfolio) {
        prefs(context).edit()
            .putString(KEY_PORTFOLIO, portfolioToJson(portfolio).toString())
            .apply()
    }

    fun reset(context: Context) {
        prefs(context).edit().clear().putBoolean(KEY_ENABLED, true).apply()
    }

    private fun loadPortfolio(raw: String?): GeminiPaperPortfolio {
        if (raw.isNullOrBlank()) return GeminiPaperPortfolio()
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
                maxDrawdownPercent = json.optDouble("maxDrawdownPercent")
            )
        }.getOrDefault(GeminiPaperPortfolio())
    }

    private fun portfolioToJson(value: GeminiPaperPortfolio): JSONObject = JSONObject()
        .put("cashEur", value.cashEur)
        .put("pumpAmount", value.pumpAmount)
        .put("entryPrice", value.entryPrice)
        .put("lastDecisionId", value.lastDecisionId)
        .put("trades", JSONArray(value.trades.map { it.toJson() }))
        .put("decisions", JSONArray(value.decisions.map { it.toJson() }))
        .put("totalFeesEur", value.totalFeesEur)
        .put("peakValueEur", value.peakValueEur)
        .put("maxDrawdownPercent", value.maxDrawdownPercent)

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
