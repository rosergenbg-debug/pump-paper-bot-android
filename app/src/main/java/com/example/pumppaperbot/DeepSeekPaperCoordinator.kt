package com.example.pumppaperbot

import android.content.Context
import kotlin.math.max

data class DeepSeekPaperOutcome(
    val status: String,
    val executedTrade: GeminiPaperTrade? = null
)

internal object DeepSeekPaperPolicy {
    fun recommendation(state: DeepSeekPrimaryState): GeminiHourlyRecommendation =
        GeminiHourlyRecommendation(
            action = when (state.action.uppercase()) {
                "BUY" -> "BUY"
                "EXIT", "SELL" -> "SELL"
                else -> "HOLD"
            },
            directionScore = state.direction,
            confidence = state.confidence,
            horizonHours = 1,
            reason = buildString {
                append("Решение основного DeepSeek. ")
                append(state.summary)
                if (state.evidence.isNotEmpty()) {
                    append(" Факты: ${state.evidence.joinToString("; ")}.")
                }
            }.take(1000),
            risks = state.risks,
            model = state.model.ifBlank { PositionSupervisorPolicy.FLASH_MODEL }
        )

    fun isNewDecision(state: DeepSeekPrimaryState, portfolio: GeminiPaperPortfolio): Boolean =
        state.lastSuccess > 0L && state.lastSuccess > portfolio.lastDecisionId

    fun executableRecommendation(
        state: DeepSeekPrimaryState,
        now: Long
    ): GeminiHourlyRecommendation {
        val recommendation = recommendation(state)
        if (recommendation.action !in setOf("BUY", "SELL")) return recommendation
        if (PaperExecutionPolicy.isFreshDecision(state.lastSuccess, now)) return recommendation
        return recommendation.copy(
            action = "HOLD",
            directionScore = 0,
            confidence = 0,
            reason = "Торговое решение DeepSeek пропущено: срок действия 12 минут истёк. " +
                "Ждём новый анализ свежего рынка."
        )
    }
}

object DeepSeekTradeOwnership {
    private const val PREFS = "deepseek_paper_owner_v47"
    private const val KEY_ACTIVE = "active"
    private const val KEY_LAST_PROCESSED = "last_processed"

    /** Called before the first V4.7 DeepSeek request so a legacy advisory result is never traded. */
    fun activate(context: Context, legacyLastSuccess: Long) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ACTIVE, false)) {
            GeminiPaperStore.retireLegacyPendingDecision(context)
            prefs.edit()
                .putBoolean(KEY_ACTIVE, true)
                .putLong(KEY_LAST_PROCESSED, legacyLastSuccess.coerceAtLeast(0L))
                .commit()
        }
    }

    fun shouldProcess(
        context: Context,
        state: DeepSeekPrimaryState,
        portfolio: GeminiPaperPortfolio
    ): Boolean {
        activate(context, state.lastSuccess)
        val last = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_PROCESSED, 0L)
        return state.lastSuccess > max(last, portfolio.lastDecisionId)
    }

    fun markProcessed(context: Context, decisionId: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_PROCESSED, decisionId.coerceAtLeast(0L))
            .commit()
    }
}

/**
 * Gives the primary DeepSeek circuit ownership of the legacy AI paper account.
 * The old Gemini preference keys are deliberately retained so balances, positions
 * and trade history survive an in-place update from V4.6.
 */
class DeepSeekPaperCoordinator {
    fun sync(
        context: Context,
        deepSeek: DeepSeekPrimaryState,
        source: String,
        now: Long = System.currentTimeMillis()
    ): DeepSeekPaperOutcome {
        GeminiPaperStore.flushPendingTradeAlerts(context)
        GeminiPaperStore.requireHealthyPortfolio(context)
        val frame = GeminiMarketFrame.from(context)
            ?: return DeepSeekPaperOutcome("ждём полный рыночный кадр")
        val stored = GeminiPaperStore.state(context).portfolio
        var portfolio = GeminiPaperTrader.markToMarket(stored, frame.preRequestPrice)
        if (portfolio != stored) GeminiPaperStore.savePortfolio(context, portfolio)

        var executed: GeminiPaperTrade? = null
        if (DeepSeekTradeOwnership.shouldProcess(context, deepSeek, portfolio)) {
            val recommendation = DeepSeekPaperPolicy.executableRecommendation(deepSeek, now)
            val tradeRequested = recommendation.action == "BUY" || recommendation.action == "SELL"
            val quote = if (tradeRequested) {
                GeminiExecutionQuoteClient().fetch()
            } else {
                GeminiExecutionQuote(frame.preRequestPrice, now)
            }
            val before = GeminiPaperTrader.markToMarket(portfolio, quote.priceEur)
            val executionPrice = if (tradeRequested) {
                PaperExecutionPolicy.executionPrice(quote.priceEur, recommendation.action)
            } else {
                quote.priceEur
            }
            val updated = GeminiPaperTrader.applyDecision(
                current = before,
                price = executionPrice,
                decisionId = deepSeek.lastSuccess,
                candleTime = frame.candleTime,
                recommendation = recommendation,
                now = quote.receivedAt,
                requestSentAt = deepSeek.lastAttempt,
                responseReceivedAt = deepSeek.lastSuccess,
                executionQuoteAt = quote.receivedAt
            )
            executed = GeminiTradeAlertPolicy.newlyExecutedTrade(
                before,
                updated,
                deepSeek.lastSuccess
            )
            GeminiPaperStore.completePending(context, updated, executed, quote.receivedAt)
            DeepSeekTradeOwnership.markProcessed(context, deepSeek.lastSuccess)
            portfolio = updated
            GeminiPaperStore.recordActivity(
                context = context,
                stage = "РЕШЕНИЕ DEEPSEEK",
                result = "OK",
                detail = "$source: ${recommendation.action}; ${recommendation.reason}",
                model = recommendation.model,
                hourId = deepSeek.lastSuccess,
                at = quote.receivedAt
            )
            executed?.let { trade ->
                SignalAttributionStore.record(
                    context,
                    "DEEPSEEK",
                    if (trade.action == "BUY") "ВИРТУАЛЬНЫЙ ВХОД" else "ВИРТУАЛЬНЫЙ ВЫХОД",
                    trade.reason,
                    trade.time,
                    executedTrade = true
                )
                if (!ResearchModePolicy.AUTONOMOUS_PARTICIPANTS) {
                    GeminiExitExperimentStore.mirrorControlTrade(context, trade)
                }
            }
            GeminiPaperStore.flushPendingTradeAlerts(context)
        }

        GeminiExitExperimentStore.evaluate(
            context = context,
            controlPortfolio = portfolio,
            deepSeekDecision = deepSeek,
            frame = frame,
            impulse = ImpulseRadarStore.state(context),
            appEvaluation = PumpBotEngine.evaluateAppPaper(context, AppPaperStore.state(context)),
            now = frame.snapshot.lastSync.takeIf { it > 0L } ?: now
        )
        return DeepSeekPaperOutcome(
            status = executed?.let { if (it.action == "BUY") "купил" else "продал" }
                ?: "решение ${deepSeek.action} обработано",
            executedTrade = executed
        )
    }
}
