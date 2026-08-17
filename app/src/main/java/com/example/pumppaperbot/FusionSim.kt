package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

data class FusionSimTrade(
    val time: Long,
    val decisionId: Long,
    val action: String,
    val price: Double,
    val amount: Double,
    val feeEur: Double,
    val pnlEur: Double,
    val reason: String
) {
    fun toJson(): JSONObject = JSONObject().put("time", time).put("decisionId", decisionId)
        .put("action", action).put("price", price).put("amount", amount)
        .put("feeEur", feeEur).put("pnlEur", pnlEur).put("reason", reason)

    companion object {
        fun fromJson(j: JSONObject) = FusionSimTrade(
            j.optLong("time"), j.optLong("decisionId"), j.optString("action"),
            j.optDouble("price"), j.optDouble("amount"), j.optDouble("feeEur"),
            j.optDouble("pnlEur"), j.optString("reason")
        )
    }
}

data class FusionSimDecision(
    val time: Long,
    val decisionId: Long,
    val requestedAction: String,
    val result: String,
    val venuePrice: Double,
    val reason: String
) {
    fun toJson(): JSONObject = JSONObject().put("time", time).put("decisionId", decisionId)
        .put("requestedAction", requestedAction).put("result", result)
        .put("venuePrice", venuePrice).put("reason", reason)

    companion object {
        fun fromJson(j: JSONObject) = FusionSimDecision(
            j.optLong("time"), j.optLong("decisionId"), j.optString("requestedAction"),
            j.optString("result"), j.optDouble("venuePrice"), j.optString("reason")
        )
    }
}

data class FusionSimPortfolio(
    val cashEur: Double = START_BALANCE,
    val pumpAmount: Double = 0.0,
    val entryPrice: Double = 0.0,
    val entryCostEur: Double = 0.0,
    val lastDecisionId: Long = 0L,
    val totalFeesEur: Double = 0.0,
    val peakValueEur: Double = START_BALANCE,
    val maxDrawdownPercent: Double = 0.0,
    val trades: List<FusionSimTrade> = emptyList(),
    val decisions: List<FusionSimDecision> = emptyList()
) {
    val inPosition: Boolean get() = pumpAmount > 0.0
    fun value(price: Double): Double = cashEur + pumpAmount * max(0.0, price)
    fun profit(price: Double): Double = value(price) - START_BALANCE
    companion object { const val START_BALANCE = 1000.0 }
}

internal object FusionSimTrader {
    fun apply(
        current: FusionSimPortfolio,
        decisionId: Long,
        requestedAction: String,
        bid: Double,
        ask: Double,
        feeRate: Double,
        reason: String,
        now: Long
    ): FusionSimPortfolio {
        if (decisionId <= current.lastDecisionId || bid <= 0.0 || ask < bid) return current
        val action = requestedAction.uppercase()
        var cash = current.cashEur
        var amount = current.pumpAmount
        var entry = current.entryPrice
        var entryCost = current.entryCostEur
        var fees = current.totalFeesEur
        var trades = current.trades
        var result = "НАБЛЮДЕНИЕ"
        var venuePrice = (bid + ask) / 2.0
        when {
            action == "BUY" && !current.inPosition && cash > 0.01 -> {
                venuePrice = ask
                val fee = cash * feeRate.coerceIn(0.0, 0.02)
                amount = (cash - fee) / ask
                entryCost = cash
                cash = 0.0
                entry = ask
                fees += fee
                result = "ВИРТУАЛЬНО КУПИЛ ПО ASK"
                trades = (trades + FusionSimTrade(now, decisionId, "BUY", ask, amount, fee, 0.0, reason)).takeLast(5000)
            }
            (action == "SELL" || action == "EXIT") && current.inPosition -> {
                venuePrice = bid
                val gross = amount * bid
                val fee = gross * feeRate.coerceIn(0.0, 0.02)
                val pnl = gross - fee - entryCost
                cash = gross - fee
                fees += fee
                result = "ВИРТУАЛЬНО ПРОДАЛ ПО BID"
                trades = (trades + FusionSimTrade(now, decisionId, "SELL", bid, amount, fee, pnl, reason)).takeLast(5000)
                amount = 0.0
                entry = 0.0
                entryCost = 0.0
            }
            action == "BUY" -> result = "BUY ПРОПУЩЕН: ПОЗИЦИЯ УЖЕ ОТКРЫТА"
            action == "SELL" || action == "EXIT" -> result = "SELL ПРОПУЩЕН: ПОЗИЦИИ НЕТ"
        }
        val value = cash + amount * bid
        val peak = max(current.peakValueEur, value)
        val drawdown = if (peak > 0.0) (1.0 - value / peak) * 100.0 else 0.0
        val decision = FusionSimDecision(now, decisionId, action, result, venuePrice, reason.take(800))
        return current.copy(
            cashEur = cash, pumpAmount = amount, entryPrice = entry, entryCostEur = entryCost,
            lastDecisionId = decisionId, totalFeesEur = fees, peakValueEur = peak,
            maxDrawdownPercent = max(current.maxDrawdownPercent, drawdown), trades = trades,
            decisions = (current.decisions + decision).takeLast(9000)
        )
    }
}

object FusionSimStore {
    private const val PREFS = "fusion_sim_paper_v51"
    private const val PORTFOLIO = "portfolio"
    private const val BACKUP = "portfolio_backup"
    private const val ACTIVATED = "activated"
    private const val ACTIVATION_WATERMARK = "activation_watermark"

    fun state(context: Context): FusionSimPortfolio {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        parse(p.getString(PORTFOLIO, null))?.let { return it }
        val recovered = parse(p.getString(BACKUP, null))
        if (recovered != null) { save(context, recovered); return recovered }
        return FusionSimPortfolio()
    }

    fun activate(context: Context, existingDeepSeekDecision: Long) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.getBoolean(ACTIVATED, false)) {
            p.edit().putBoolean(ACTIVATED, true)
                .putLong(ACTIVATION_WATERMARK, existingDeepSeekDecision.coerceAtLeast(0L)).commit()
            UnifiedResearchLog.record(context, "FUSION_SIM", "START", "Активирован без исполнения старых решений DeepSig")
        }
    }

    @Synchronized
    fun sync(context: Context, deepSeek: DeepSeekPrimaryState, now: Long = System.currentTimeMillis()): FusionSimPortfolio {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.getBoolean(ACTIVATED, false)) activate(context, deepSeek.lastSuccess)
        val current = state(context)
        val watermark = p.getLong(ACTIVATION_WATERMARK, 0L)
        if (deepSeek.lastSuccess <= max(current.lastDecisionId, watermark)) return current
        val market = BitpandaFusionStore.state(context)
        if (!market.fresh(now)) {
            UnifiedResearchLog.record(context, "FUSION_SIM", "WAIT", "Новое решение DeepSig не исполнено: нет свежего read-only стакана Bitpanda")
            return current
        }
        val recommendation = DeepSeekPaperPolicy.executableRecommendation(deepSeek, now)
        val next = FusionSimTrader.apply(
            current, deepSeek.lastSuccess, recommendation.action, market.bid, market.ask,
            market.feeRate, recommendation.reason, now
        )
        if (next != current) {
            save(context, next)
            UnifiedResearchLog.record(context, "FUSION_SIM", "OK", next.decisions.last().result)
        }
        return next
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }

    fun toJson(value: FusionSimPortfolio): JSONObject = JSONObject()
        .put("cashEur", value.cashEur).put("pumpAmount", value.pumpAmount)
        .put("entryPrice", value.entryPrice).put("entryCostEur", value.entryCostEur)
        .put("lastDecisionId", value.lastDecisionId).put("totalFeesEur", value.totalFeesEur)
        .put("peakValueEur", value.peakValueEur).put("maxDrawdownPercent", value.maxDrawdownPercent)
        .put("trades", JSONArray(value.trades.map { it.toJson() }))
        .put("decisions", JSONArray(value.decisions.map { it.toJson() }))

    private fun save(context: Context, value: FusionSimPortfolio) {
        val raw = toJson(value).toString()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(PORTFOLIO, raw).putString(BACKUP, raw).commit()
    }

    private fun parse(raw: String?): FusionSimPortfolio? = if (raw.isNullOrBlank()) null else runCatching {
        val j = JSONObject(raw)
        val trades = j.optJSONArray("trades") ?: JSONArray()
        val decisions = j.optJSONArray("decisions") ?: JSONArray()
        FusionSimPortfolio(
            cashEur = j.optDouble("cashEur", 1000.0), pumpAmount = j.optDouble("pumpAmount"),
            entryPrice = j.optDouble("entryPrice"), entryCostEur = j.optDouble("entryCostEur"),
            lastDecisionId = j.optLong("lastDecisionId"), totalFeesEur = j.optDouble("totalFeesEur"),
            peakValueEur = j.optDouble("peakValueEur", 1000.0),
            maxDrawdownPercent = j.optDouble("maxDrawdownPercent"),
            trades = (0 until trades.length()).mapNotNull { trades.optJSONObject(it)?.let(FusionSimTrade::fromJson) },
            decisions = (0 until decisions.length()).mapNotNull { decisions.optJSONObject(it)?.let(FusionSimDecision::fromJson) }
        )
    }.getOrNull()
}
