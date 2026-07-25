package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

data class GeminiPaperTrade(
    val time: Long,
    val action: String,
    val price: Double,
    val amount: Double,
    val fee: Double,
    val score: Int,
    val confidence: Int,
    val reason: String
) {
    fun toJson(): JSONObject = JSONObject()
        .put("time", time).put("action", action).put("price", price)
        .put("amount", amount).put("fee", fee).put("score", score)
        .put("confidence", confidence).put("reason", reason)

    companion object {
        fun fromJson(value: JSONObject) = GeminiPaperTrade(
            value.optLong("time"), value.optString("action"), value.optDouble("price"),
            value.optDouble("amount"), value.optDouble("fee"), value.optInt("score"),
            value.optInt("confidence"), value.optString("reason")
        )
    }
}

data class GeminiPaperPortfolio(
    val cashEur: Double = START_BALANCE,
    val pumpAmount: Double = 0.0,
    val entryPrice: Double = 0.0,
    val lastDecisionId: Long = 0L,
    val trades: List<GeminiPaperTrade> = emptyList()
) {
    fun value(price: Double): Double = cashEur + pumpAmount * max(0.0, price)
    fun profit(price: Double): Double = value(price) - START_BALANCE
    fun profitPercent(price: Double): Double = profit(price) / START_BALANCE * 100.0
    val inPosition: Boolean get() = pumpAmount > 0.0

    companion object {
        const val START_BALANCE = 1000.0
    }
}

/**
 * Independent live shadow account. It never changes StrategyV2 BUY/SELL decisions.
 * One Gemini answer may cause at most one operation.
 */
object GeminiPaperTrader {
    const val FEE_RATE = 0.0015
    const val BUY_SCORE = 55
    const val BUY_CONFIDENCE = 60
    const val SELL_SCORE = -35
    const val STOP_LOSS_PERCENT = -4.5
    const val TAKE_PROFIT_PERCENT = 6.0

    fun evaluate(
        current: GeminiPaperPortfolio,
        price: Double,
        decisionId: Long,
        score: Int,
        confidence: Int,
        lateEntryRisk: Int,
        now: Long = System.currentTimeMillis()
    ): GeminiPaperPortfolio {
        if (price <= 0.0 || decisionId <= 0L) return current
        val move = if (current.inPosition) (price / current.entryPrice - 1.0) * 100.0 else 0.0
        val priceExitReason = when {
            current.inPosition && move <= STOP_LOSS_PERCENT -> "Защитный стоп ${"%.2f".format(move)}%"
            current.inPosition && move >= TAKE_PROFIT_PERCENT -> "Фиксация прибыли ${"%.2f".format(move)}%"
            else -> null
        }
        if (decisionId == current.lastDecisionId && priceExitReason == null) return current
        val marked = current.copy(lastDecisionId = decisionId)
        if (!current.inPosition) {
            if (score < BUY_SCORE || confidence < BUY_CONFIDENCE || lateEntryRisk > 65) return marked
            val fee = current.cashEur * FEE_RATE
            val amount = (current.cashEur - fee) / price
            return marked.copy(
                cashEur = 0.0,
                pumpAmount = amount,
                entryPrice = price,
                trades = addTrade(current.trades, GeminiPaperTrade(
                    now, "BUY", price, amount, fee, score, confidence,
                    "Gemini ≥ $BUY_SCORE, уверенность ≥ $BUY_CONFIDENCE, поздний вход допустим"
                ))
            )
        }

        val reason = when {
            priceExitReason != null -> priceExitReason
            score <= SELL_SCORE && confidence >= 55 -> "Gemini развернулся вниз"
            else -> return marked
        }
        val gross = current.pumpAmount * price
        val fee = gross * FEE_RATE
        return marked.copy(
            cashEur = gross - fee,
            pumpAmount = 0.0,
            entryPrice = 0.0,
            trades = addTrade(current.trades, GeminiPaperTrade(
                now, "SELL", price, current.pumpAmount, fee, score, confidence, reason
            ))
        )
    }

    private fun addTrade(old: List<GeminiPaperTrade>, trade: GeminiPaperTrade) =
        (old + trade).takeLast(100)
}

object GeminiPaperStore {
    private const val PREFS = "gemini_paper_v34"
    private const val KEY = "portfolio"

    fun load(context: Context): GeminiPaperPortfolio {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return GeminiPaperPortfolio()
        return runCatching {
            val json = JSONObject(raw)
            val tradesJson = json.optJSONArray("trades") ?: JSONArray()
            GeminiPaperPortfolio(
                cashEur = json.optDouble("cashEur", GeminiPaperPortfolio.START_BALANCE),
                pumpAmount = json.optDouble("pumpAmount"),
                entryPrice = json.optDouble("entryPrice"),
                lastDecisionId = json.optLong("lastDecisionId"),
                trades = (0 until tradesJson.length()).map {
                    GeminiPaperTrade.fromJson(tradesJson.getJSONObject(it))
                }
            )
        }.getOrDefault(GeminiPaperPortfolio())
    }

    fun evaluate(context: Context, market: LiveSnapshot, gemini: GeminiDiagnostics): GeminiPaperPortfolio {
        val updated = GeminiPaperTrader.evaluate(
            load(context), market.lastPrice, gemini.lastSuccess, gemini.directionScore,
            gemini.confidence, market.lateEntryRisk
        )
        save(context, updated)
        return updated
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun save(context: Context, value: GeminiPaperPortfolio) {
        val json = JSONObject()
            .put("cashEur", value.cashEur).put("pumpAmount", value.pumpAmount)
            .put("entryPrice", value.entryPrice).put("lastDecisionId", value.lastDecisionId)
            .put("trades", JSONArray(value.trades.map { it.toJson() }))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, json.toString()).apply()
    }
}
